package com.oms.service;

import com.oms.config.Db;
import com.oms.dao.AssignmentDao;
import com.oms.dao.AuditLogDao;
import com.oms.dao.CustomerDao;
import com.oms.dao.LookupDao;
import com.oms.model.Customer;
import com.oms.model.NamedItem;
import com.oms.model.PageResult;
import com.oms.model.User;
import com.oms.security.PermissionPolicy;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Hồ sơ đại lý (S3-03). Admin, Quản lý kinh doanh và Kế toán công nợ sửa được; Nhân viên kinh doanh chỉ xem đại lý
 * mình phụ trách (kiểm ở đây, không chỉ ẩn nút trên giao diện).
 */
public class CustomerService {

    public static final int PAGE_SIZE = 20;
    private static final Pattern CODE = Pattern.compile("^[A-Z0-9][A-Z0-9._-]{1,19}$");
    // Mã số thuế doanh nghiệp Việt Nam: 10 số, chi nhánh / đơn vị phụ thuộc thêm "-" và 3 số
    private static final Pattern TAX_CODE = Pattern.compile("^\\d{10}(-\\d{3})?$");

    private final CustomerDao customerDao = new CustomerDao();
    private final LookupDao lookupDao = new LookupDao();
    private final AssignmentDao assignmentDao = new AssignmentDao();
    private final AuditLogDao auditDao = new AuditLogDao();

    /** Danh sách theo quyền: người chỉ được xem đại lý mình phụ trách luôn bị lọc theo chính họ. */
    public PageResult<Customer> search(String keyword, Long groupId, Long areaId, Long salesRepId,
                                       Customer.Status status, int page, User viewer) throws SQLException {
        requireBrowse(viewer);
        Long own = PermissionPolicy.canManageCustomers(viewer.getRoles()) ? null : viewer.getId();
        try (Connection conn = Db.getConnection()) {
            return customerDao.search(conn, new CustomerDao.Filter(keyword, groupId, areaId, salesRepId, status, own),
                    Math.max(1, page), PAGE_SIZE);
        }
    }

    public Customer get(long id, User viewer) throws SQLException {
        requireBrowse(viewer);
        try (Connection conn = Db.getConnection()) {
            Customer c = customerDao.findById(conn, id);
            if (c == null || !canSee(viewer, c)) {
                throw new NotFoundException();
            }
            return c;
        }
    }

    public List<NamedItem> groups() throws SQLException {
        try (Connection conn = Db.getConnection()) {
            return lookupDao.customerGroups(conn);
        }
    }

    public List<NamedItem> areas() throws SQLException {
        try (Connection conn = Db.getConnection()) {
            return lookupDao.areas(conn);
        }
    }

    public List<NamedItem> salesReps() throws SQLException {
        try (Connection conn = Db.getConnection()) {
            return lookupDao.salesReps(conn);
        }
    }

    /** input.code chỉ dùng khi tạo mới; các trường còn lại được kiểm tra và chuẩn hoá tại đây. */
    public long create(Customer input, User actor) throws SQLException {
        requireManage(actor);
        String code = normalizeCode(input.getCode());
        Customer c = validated(input);
        c.setCode(code);
        if (!PermissionPolicy.canAssignCustomers(actor.getRoles())) {
            c.setSalesRepId(null);
        }
        return Db.inTransaction(conn -> {
            if (customerDao.existsCode(conn, code)) {
                throw new ServiceException("Mã đại lý \"" + code + "\" đã tồn tại. Mỗi đại lý phải có mã riêng.");
            }
            checkReferences(conn, c, true);
            checkTaxCode(conn, c.getTaxCode(), 0);
            long id = customerDao.insert(conn, c);
            if (c.getSalesRepId() != null) {
                assignmentDao.insert(conn, id, null, c.getSalesRepId(), "Phân công khi tạo hồ sơ", actor.getId());
            }
            auditDao.log(conn, actor.getId(), "CUSTOMER_CREATE", "CUSTOMER", id, code, null, describe(customerDao.findById(conn, id)));
            return id;
        });
    }

    public void update(long id, Customer input, User actor) throws SQLException {
        requireManage(actor);
        Customer changes = validated(input);
        Db.inTransaction(conn -> {
            Customer existing = customerDao.findByIdForUpdate(conn, id);
            if (existing == null) {
                throw new NotFoundException();
            }
            if (!PermissionPolicy.canAssignCustomers(actor.getRoles())) {
                changes.setSalesRepId(existing.getSalesRepId());
            }
            boolean repChanged = !Objects.equals(existing.getSalesRepId(), changes.getSalesRepId());
            checkReferences(conn, changes, repChanged);
            checkTaxCode(conn, changes.getTaxCode(), id);
            changes.setId(id);
            String before = describe(existing);
            customerDao.update(conn, changes);
            if (repChanged) {
                assignmentDao.insert(conn, id, existing.getSalesRepId(), changes.getSalesRepId(), "Đổi trong hồ sơ đại lý", actor.getId());
            }
            String after = describe(customerDao.findById(conn, id));
            if (!before.equals(after)) {
                auditDao.log(conn, actor.getId(), "CUSTOMER_UPDATE", "CUSTOMER", id, existing.getCode(), before, after);
            }
            return null;
        });
    }

    /** Chỉ xoá được khi chưa có đơn hàng hay dữ liệu nào tham chiếu (khoá ngoại chặn); ngược lại khoá giao dịch. */
    public void delete(long id, User actor) throws SQLException {
        requireManage(actor);
        try {
            Db.inTransaction(conn -> {
                Customer existing = customerDao.findByIdForUpdate(conn, id);
                if (existing == null) {
                    throw new NotFoundException();
                }
                auditDao.log(conn, actor.getId(), "CUSTOMER_DELETE", "CUSTOMER", id, existing.getCode(), describe(existing), null);
                customerDao.delete(conn, id);
                return null;
            });
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new ServiceException("Đại lý đã phát sinh đơn hàng hoặc dữ liệu liên quan nên không xoá được. "
                    + "Hãy khoá giao dịch với đại lý này.");
        }
    }

    private void requireBrowse(User user) {
        if (!PermissionPolicy.canBrowseCustomers(user.getRoles())) {
            throw new ForbiddenException("Bạn không có quyền xem danh sách đại lý.");
        }
    }

    private void requireManage(User user) {
        if (!PermissionPolicy.canManageCustomers(user.getRoles())) {
            throw new ForbiddenException("Chỉ Quản lý kinh doanh, Kế toán công nợ và Quản trị hệ thống được sửa hồ sơ đại lý.");
        }
    }

    private static boolean canSee(User viewer, Customer c) {
        return PermissionPolicy.canManageCustomers(viewer.getRoles())
                || (c.getSalesRepId() != null && c.getSalesRepId() == viewer.getId());
    }

    /** checkRep = false khi người phụ trách không đổi: nhân viên đã bị khoá vẫn giữ trong hồ sơ cho tới khi được bàn giao. */
    private void checkReferences(Connection conn, Customer c, boolean checkRep) throws SQLException {
        if (c.getGroupId() == null || lookupDao.customerGroups(conn).stream().noneMatch(g -> g.getId() == c.getGroupId())) {
            throw new ServiceException("Vui lòng chọn nhóm khách hàng (nhóm quyết định bảng giá áp dụng).");
        }
        if (c.getAreaId() != null && lookupDao.areas(conn).stream().noneMatch(a -> a.getId() == c.getAreaId())) {
            throw new ServiceException("Khu vực được chọn không tồn tại. Vui lòng tải lại trang.");
        }
        if (checkRep && c.getSalesRepId() != null && lookupDao.salesReps(conn).stream().noneMatch(r -> r.getId() == c.getSalesRepId())) {
            throw new ServiceException("Người phụ trách phải là nhân viên kinh doanh đang hoạt động.");
        }
    }

    private void checkTaxCode(Connection conn, String taxCode, long excludeId) throws SQLException {
        if (taxCode != null && customerDao.existsTaxCode(conn, taxCode, excludeId)) {
            throw new ServiceException("Mã số thuế " + taxCode + " đã được dùng cho đại lý khác.");
        }
    }

    private static Customer validated(Customer in) {
        Customer c = new Customer();
        String name = in.getName() == null ? "" : in.getName().trim();
        if (name.isEmpty()) {
            throw new ServiceException("Vui lòng nhập tên đại lý.");
        }
        if (name.length() > 200) {
            throw new ServiceException("Tên đại lý tối đa 200 ký tự.");
        }
        c.setName(name);
        String tax = in.getTaxCode() == null ? "" : in.getTaxCode().trim();
        if (!tax.isEmpty() && !TAX_CODE.matcher(tax).matches()) {
            throw new ServiceException("Mã số thuế gồm 10 chữ số, chi nhánh thêm \"-\" và 3 số (ví dụ 0101234567-001).");
        }
        c.setTaxCode(tax.isEmpty() ? null : tax);
        c.setPhone(PhoneNumbers.normalize(in.getPhone()));
        c.setGroupId(in.getGroupId());
        c.setAreaId(in.getAreaId());
        c.setSalesRepId(in.getSalesRepId());
        return c;
    }

    static String normalizeCode(String raw) {
        String code = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if (!CODE.matcher(code).matches()) {
            throw new ServiceException("Mã đại lý gồm 2–20 ký tự: chữ không dấu, số và . _ - (tự chuyển thành chữ hoa).");
        }
        return code;
    }

    private static String describe(Customer c) {
        return "Tên: " + c.getName() + "\nMST: " + dash(c.getTaxCode()) + "\nSĐT: " + dash(c.getPhone())
                + "\nNhóm khách hàng: " + dash(c.getGroupName()) + "\nKhu vực: " + dash(c.getAreaName())
                + "\nNgười phụ trách: " + dash(c.getSalesRepName()) + "\nTrạng thái: " + c.getStatus().getLabel();
    }

    private static String dash(String v) {
        return v == null || v.isBlank() ? "—" : v;
    }

    public static class NotFoundException extends ServiceException {
        public NotFoundException() {
            super("Không tìm thấy đại lý.");
        }
    }
}
