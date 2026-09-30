package com.oms.service;

import com.oms.config.AppConfig;
import com.oms.config.Db;
import com.oms.dao.AuditLogDao;
import com.oms.dao.CustomerDao;
import com.oms.dao.LookupDao;
import com.oms.dao.UserDao;
import com.oms.model.NamedItem;
import com.oms.model.PageResult;
import com.oms.model.Role;
import com.oms.model.User;
import com.oms.security.Passwords;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class UserService {

    public static final int PAGE_SIZE = 20;

    private static final Logger LOG = Logger.getLogger(UserService.class.getName());
    private static final Pattern USERNAME = Pattern.compile("^[a-zA-Z0-9._-]{3,50}$");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern PHONE = Pattern.compile("^[0-9 +().-]{8,20}$");

    private final UserDao userDao = new UserDao();
    private final LookupDao lookupDao = new LookupDao();
    private final AuditLogDao auditDao = new AuditLogDao();
    private final MailService mailService = new MailService();
    private final CustomerDao customerDao = new CustomerDao();

    public PageResult<User> search(String keyword, Role role, User.Status status, int page) throws SQLException {
        try (Connection conn = Db.getConnection()) {
            return userDao.search(conn, keyword, role, status, Math.max(1, page), PAGE_SIZE);
        }
    }

    public User get(long id) throws SQLException {
        try (Connection conn = Db.getConnection()) {
            User user = userDao.findById(conn, id);
            if (user == null) {
                throw new NotFoundException();
            }
            return user;
        }
    }

    public Map<User.Status, Long> countByStatus() throws SQLException {
        try (Connection conn = Db.getConnection()) {
            return userDao.countByStatus(conn);
        }
    }

    /** Số đại lý mỗi người đang phụ trách, để hộp thoại khoá cảnh báo bàn giao (S1-10). */
    public Map<Long, Long> customerCounts(List<User> users) throws SQLException {
        try (Connection conn = Db.getConnection()) {
            return customerDao.countBySalesReps(conn, users.stream().map(User::getId).toList());
        }
    }

    public List<String> customerNamesOf(long userId) throws SQLException {
        try (Connection conn = Db.getConnection()) {
            return customerDao.namesBySalesRep(conn, userId);
        }
    }

    /** Đại lý có nhân viên phụ trách đã bị khoá — hiển thị ở trang chủ để bàn giao (S1-10). */
    public List<CustomerDao.HandoverItem> customersNeedingHandover() throws SQLException {
        try (Connection conn = Db.getConnection()) {
            return customerDao.findNeedingHandover(conn);
        }
    }

    public List<NamedItem> warehouses() throws SQLException {
        try (Connection conn = Db.getConnection()) {
            return lookupDao.warehouses(conn);
        }
    }

    public List<NamedItem> areas() throws SQLException {
        try (Connection conn = Db.getConnection()) {
            return lookupDao.areas(conn);
        }
    }

    /** Tạo tài khoản và gửi email kích hoạt kèm mật khẩu tạm (S1-08). Trả về true nếu email đã gửi được. */
    public boolean create(UserForm form, User actor) throws SQLException {
        validate(form, actor);
        if (!USERNAME.matcher(form.getUsername()).matches()) {
            throw new ServiceException("Tài khoản chỉ gồm chữ không dấu, số và . _ - (3–50 ký tự).");
        }
        String tempPassword = Passwords.generateTemporary();
        Db.inTransaction(conn -> {
            if (userDao.existsUsername(conn, form.getUsername())) {
                throw new ServiceException("Tài khoản \"" + form.getUsername() + "\" đã tồn tại. Vui lòng chọn tên tài khoản khác.");
            }
            if (userDao.existsEmail(conn, form.getEmail(), 0)) {
                throw new ServiceException("Email \"" + form.getEmail() + "\" đã được dùng cho tài khoản khác.");
            }
            User user = new User();
            user.setUsername(form.getUsername());
            user.setEmail(form.getEmail());
            user.setFullName(form.getFullName());
            user.setPhone(blankToNull(form.getPhone()));
            user.setPasswordHash(Passwords.hash(tempPassword));
            user.setStatus(User.Status.PENDING);
            user.setMustChangePassword(true);
            long newId = userDao.insert(conn, user);
            saveRelations(conn, newId, form);
            auditDao.log(conn, actor.getId(), "USER_CREATE", "USER", newId, "roles=" + form.getRoles());
            return newId;
        });
        return mailService.send(form.getEmail(), "Kích hoạt tài khoản OMS",
                "Chào " + form.getFullName() + ",\nTài khoản của bạn đã được tạo.\n"
                        + "Tài khoản: " + form.getUsername() + "\nMật khẩu tạm: " + tempPassword + "\n"
                        + "Đăng nhập tại " + AppConfig.get("app.baseUrl") + "/login và đổi mật khẩu ngay lần đầu.");
    }

    public void update(UserForm form, User actor) throws SQLException {
        validate(form, actor);
        Db.inTransaction(conn -> {
            User existing = userDao.findById(conn, form.getId());
            if (existing == null) {
                throw new NotFoundException();
            }
            if (userDao.existsEmail(conn, form.getEmail(), form.getId())) {
                throw new ServiceException("Email \"" + form.getEmail() + "\" đã được dùng cho tài khoản khác.");
            }
            existing.setEmail(form.getEmail());
            existing.setFullName(form.getFullName());
            existing.setPhone(blankToNull(form.getPhone()));
            userDao.updateProfile(conn, existing);
            saveRelations(conn, existing.getId(), form);
            auditDao.log(conn, actor.getId(), "USER_UPDATE", "USER", existing.getId(), "roles=" + form.getRoles());
            return null;
        });
    }

    /** Khoá tài khoản: bắt buộc lý do, thu hồi phiên đang mở (S1-10). */
    public void lock(long userId, String reason, User actor) throws SQLException {
        if (reason == null || reason.isBlank()) {
            throw new ServiceException("Vui lòng nhập lý do khoá tài khoản.");
        }
        if (reason.length() > 500) {
            throw new ServiceException("Lý do khoá tối đa 500 ký tự.");
        }
        if (userId == actor.getId()) {
            throw new ServiceException("Bạn không thể tự khoá tài khoản của chính mình.");
        }
        Db.inTransaction(conn -> {
            if (userDao.findById(conn, userId) == null) {
                throw new NotFoundException();
            }
            userDao.lock(conn, userId, reason.trim(), actor.getId());
            auditDao.log(conn, actor.getId(), "USER_LOCK", "USER", userId, reason.trim());
            return null;
        });
    }

    public void unlock(long userId, User actor) throws SQLException {
        Db.inTransaction(conn -> {
            if (userDao.findById(conn, userId) == null) {
                throw new NotFoundException();
            }
            userDao.unlock(conn, userId);
            auditDao.log(conn, actor.getId(), "USER_UNLOCK", "USER", userId, null);
            return null;
        });
    }

    /** Tạo tài khoản admin khi database chưa có người dùng nào. */
    public void ensureInitialAdmin() throws SQLException {
        String initialPassword = AppConfig.get("admin.initialPassword");
        Db.inTransaction(conn -> {
            if (userDao.count(conn) > 0) {
                return null;
            }
            if (Passwords.validateStrength(initialPassword) != null) {
                LOG.severe("Chưa có người dùng nào và admin.initialPassword trong app.properties chưa hợp lệ "
                        + "(tối thiểu 8 ký tự, có chữ và số). Không tạo được tài khoản admin.");
                return null;
            }
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@oms.local");
            admin.setFullName("Quản trị hệ thống");
            admin.setPasswordHash(Passwords.hash(initialPassword));
            admin.setStatus(User.Status.PENDING);
            admin.setMustChangePassword(true);
            long id = userDao.insert(conn, admin);
            userDao.replaceRoles(conn, id, EnumSet.of(Role.ADMIN));
            auditDao.log(conn, null, "USER_CREATE", "USER", id, "initial admin");
            LOG.info("Đã tạo tài khoản admin ban đầu (đăng nhập bằng admin.initialPassword, sẽ bị yêu cầu đổi mật khẩu).");
            return null;
        });
    }

    private void saveRelations(Connection conn, long userId, UserForm form) throws SQLException {
        userDao.replaceRoles(conn, userId, form.getRoles());
        userDao.replaceWarehouses(conn, userId, form.getWarehouseIds());
        userDao.replaceAreas(conn, userId, form.getAreaIds());
    }

    private void validate(UserForm form, User actor) {
        if (form.getFullName().isBlank()) {
            throw new ServiceException("Vui lòng nhập họ và tên.");
        }
        if (!EMAIL.matcher(form.getEmail()).matches()) {
            throw new ServiceException("Email không hợp lệ.");
        }
        if (!form.getPhone().isBlank() && !PHONE.matcher(form.getPhone()).matches()) {
            throw new ServiceException("Số điện thoại không hợp lệ.");
        }
        if (form.getRoles().isEmpty()) {
            throw new ServiceException("Vui lòng chọn ít nhất một vai trò.");
        }
        boolean needsWarehouse = form.getRoles().stream().anyMatch(Role::requiresWarehouse);
        if (needsWarehouse && form.getWarehouseIds().isEmpty()) {
            throw new ServiceException("Người dùng có vai trò kho phải được gắn với ít nhất một kho.");
        }
        // S1-09: không thể tự thu hồi vai trò quản trị của chính mình
        if (!form.isNew() && form.getId() == actor.getId() && actor.isAdmin() && !form.getRoles().contains(Role.ADMIN)) {
            throw new ServiceException("Bạn không thể tự thu hồi vai trò Quản trị hệ thống của chính mình.");
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    public static class NotFoundException extends ServiceException {
        public NotFoundException() {
            super("Không tìm thấy người dùng.");
        }
    }
}
