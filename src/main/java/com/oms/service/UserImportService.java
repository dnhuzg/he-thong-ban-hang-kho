package com.oms.service;

import com.oms.config.Db;
import com.oms.dao.AuditLogDao;
import com.oms.dao.LookupDao;
import com.oms.dao.UserDao;
import com.oms.model.ImportBatch;
import com.oms.model.ImportRow.Outcome;
import com.oms.model.NamedItem;
import com.oms.model.Role;
import com.oms.model.User;
import com.oms.model.UserImportRow;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Nhập người dùng hàng loạt từ Excel (S2-01), chỉ Quản trị hệ thống. Xem trước kiểm từng dòng như form tạo
 * tài khoản mà chưa ghi gì; nhập tạo từng tài khoản riêng (dòng lỗi không kéo theo dòng khác) và gửi email
 * kích hoạt kèm mật khẩu tạm như tạo tay.
 */
public class UserImportService {

    public static final int MAX_ROWS = 500;

    private static final List<String> HEADERS = List.of("Họ và tên *", "Tài khoản *", "Email *", "Số điện thoại",
            "Vai trò *", "Kho", "Địa bàn");
    private static final List<String> REQUIRED = List.of("Họ và tên", "Tài khoản", "Email", "Vai trò");

    private final UserService userService = new UserService();
    private final UserDao userDao = new UserDao();
    private final LookupDao lookupDao = new LookupDao();
    private final AuditLogDao auditDao = new AuditLogDao();

    /** Ví dụ dùng tên kho / địa bàn có thật trong hệ thống để tệp mẫu nạp lại được ngay. */
    public byte[] template() throws SQLException {
        String warehouse;
        String area;
        try (Connection conn = Db.getConnection()) {
            warehouse = lookupDao.warehouses(conn).stream().map(NamedItem::getName).findFirst().orElse("");
            area = lookupDao.areas(conn).stream().map(NamedItem::getName).findFirst().orElse("");
        }
        return Excel.template(HEADERS,
                List.of(List.of("Nguyễn Văn An", "an.nguyen", "an.nguyen@congty.vn", "0901234567", "Nhân viên kinh doanh", "", area),
                        List.of("Trần Thị Bình", "binh.tran", "binh.tran@congty.vn", "0912345678", "Nhân viên kho; Quản lý kho", warehouse, "")),
                List.of("Cột có dấu * là bắt buộc. Giữ nguyên dòng tiêu đề; có thể đổi thứ tự cột.",
                        "Tài khoản: 3–50 ký tự chữ không dấu, số và . _ - ; không trùng tài khoản đã có.",
                        "Email: không trùng email của tài khoản khác; hệ thống gửi mật khẩu tạm tới email này.",
                        "Vai trò: ghi tên vai trò (ví dụ \"Nhân viên kinh doanh\"); nhiều vai trò cách nhau bằng dấu ; hoặc dấu phẩy. Vai trò: "
                                + String.join(", ", java.util.Arrays.stream(Role.values()).map(Role::getLabel).toList()) + ".",
                        "Kho, Địa bàn: ghi đúng tên đã có trong hệ thống, nhiều nơi cách nhau bằng dấu ; . Vai trò Nhân viên kho / Quản lý kho bắt buộc có kho.",
                        "Tối đa " + MAX_ROWS + " dòng mỗi tệp. Mỗi tài khoản nhập thành công sẽ nhận email kích hoạt."));
    }

    public ImportBatch<UserImportRow> preview(byte[] data, String fileName) throws SQLException {
        Excel.Table table = Excel.read(data, MAX_ROWS, REQUIRED);
        List<UserImportRow> rows = new ArrayList<>();
        try (Connection conn = Db.getConnection()) {
            Map<String, Role> roles = new HashMap<>();
            for (Role r : Role.values()) {
                roles.put(r.getLabel().toLowerCase(Locale.ROOT), r);
                roles.put(r.getCode().toLowerCase(Locale.ROOT), r);
            }
            Map<String, NamedItem> warehouses = byName(lookupDao.warehouses(conn));
            Map<String, NamedItem> areas = byName(lookupDao.areas(conn));
            Set<String> usernames = new HashSet<>();
            Set<String> emails = new HashSet<>();
            for (Excel.Line line : table.lines()) {
                rows.add(check(conn, line, roles, warehouses, areas, usernames, emails));
            }
        }
        return new ImportBatch<>(fileName, rows);
    }

    /** Tạo từng tài khoản hợp lệ; lỗi phát sinh lúc nhập (trùng do người khác vừa tạo...) chỉ bỏ qua dòng đó. */
    public void apply(ImportBatch<UserImportRow> batch, User actor) throws SQLException {
        if (batch.isApplied()) {
            throw new ServiceException("Tệp này đã được nhập rồi.");
        }
        batch.setApplied(true);
        for (UserImportRow row : batch.getRows()) {
            if (!row.isValid()) {
                row.setOutcome(Outcome.SKIPPED);
                continue;
            }
            UserForm form = new UserForm();
            form.setFullName(row.getFullName());
            form.setUsername(row.getUsername());
            form.setEmail(row.getEmail());
            form.setPhone(row.getPhone());
            form.setRoles(row.getRoles());
            form.setWarehouseIds(row.getWarehouseIds());
            form.setAreaIds(row.getAreaIds());
            try {
                row.setEmailSent(userService.create(form, actor));
                row.setOutcome(Outcome.CREATED);
            } catch (ServiceException e) {
                row.addError(e.getMessage());
                row.setOutcome(Outcome.SKIPPED);
            }
        }
        Db.inTransaction(conn -> {
            auditDao.log(conn, actor.getId(), "USER_IMPORT", "USER", null, batch.getFileName(), null,
                    "Tạo mới: " + batch.getCreatedCount() + "\nBỏ qua: " + batch.getSkippedCount());
            return null;
        });
    }

    public int emailsNotSent(ImportBatch<UserImportRow> batch) {
        return (int) batch.getRows().stream().filter(r -> r.getOutcome() == Outcome.CREATED && !r.isEmailSent()).count();
    }

    private UserImportRow check(Connection conn, Excel.Line line, Map<String, Role> roles, Map<String, NamedItem> warehouses,
                                Map<String, NamedItem> areas, Set<String> usernames, Set<String> emails) throws SQLException {
        UserImportRow row = new UserImportRow();
        row.setRowNumber(line.number());
        row.setFullName(line.get("họ và tên"));
        row.setUsername(line.get("tài khoản"));
        row.setEmail(line.get("email"));
        row.setPhone(line.get("số điện thoại"));
        row.setRoleText(line.get("vai trò"));
        row.setWarehouseText(line.get("kho"));
        row.setAreaText(line.get("địa bàn"));

        if (row.getFullName().isEmpty()) {
            row.addError("Thiếu họ và tên");
        } else if (row.getFullName().length() > 150) {
            row.addError("Họ và tên tối đa 150 ký tự");
        }
        try {
            UserService.requireValidUsername(row.getUsername());
            if (!usernames.add(row.getUsername().toLowerCase(Locale.ROOT))) {
                row.addError("Tài khoản trùng với một dòng phía trên trong tệp");
            } else if (userDao.existsUsername(conn, row.getUsername())) {
                row.addError("Tài khoản \"" + row.getUsername() + "\" đã tồn tại");
            }
        } catch (ServiceException e) {
            row.addError(e.getMessage());
        }
        if (!UserService.isValidEmail(row.getEmail())) {
            row.addError("Email không hợp lệ");
        } else if (!emails.add(row.getEmail().toLowerCase(Locale.ROOT))) {
            row.addError("Email trùng với một dòng phía trên trong tệp");
        } else if (userDao.existsEmail(conn, row.getEmail(), 0)) {
            row.addError("Email \"" + row.getEmail() + "\" đã được dùng cho tài khoản khác");
        }
        try {
            PhoneNumbers.normalize(row.getPhone());
        } catch (ServiceException e) {
            row.addError(e.getMessage());
        }
        for (String name : split(row.getRoleText())) {
            Role role = roles.get(name.toLowerCase(Locale.ROOT));
            if (role == null) {
                row.addError("Vai trò \"" + name + "\" không có trong hệ thống");
            } else {
                row.getRoles().add(role);
            }
        }
        if (row.getRoleText().isEmpty()) {
            row.addError("Thiếu vai trò");
        }
        for (String name : split(row.getWarehouseText())) {
            NamedItem item = warehouses.get(name.toLowerCase(Locale.ROOT));
            if (item == null) {
                row.addError("Kho \"" + name + "\" không có trong hệ thống");
            } else {
                row.getWarehouseIds().add(item.getId());
            }
        }
        for (String name : split(row.getAreaText())) {
            NamedItem item = areas.get(name.toLowerCase(Locale.ROOT));
            if (item == null) {
                row.addError("Địa bàn \"" + name + "\" không có trong hệ thống");
            } else {
                row.getAreaIds().add(item.getId());
            }
        }
        if (row.getRoles().stream().anyMatch(Role::requiresWarehouse) && row.getWarehouseIds().isEmpty()) {
            row.addError("Vai trò kho phải gắn với ít nhất một kho");
        }
        return row;
    }

    private static List<String> split(String text) {
        List<String> parts = new ArrayList<>();
        for (String part : text.split("[;,]")) {
            if (!part.isBlank()) {
                parts.add(part.trim());
            }
        }
        return parts;
    }

    private static Map<String, NamedItem> byName(List<NamedItem> items) {
        Map<String, NamedItem> map = new HashMap<>();
        for (NamedItem item : items) {
            map.put(item.getName().toLowerCase(Locale.ROOT), item);
        }
        return map;
    }
}
