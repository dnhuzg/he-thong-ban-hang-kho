package com.oms.service;

import com.oms.model.ImportBatch;
import com.oms.model.Role;
import com.oms.model.User;
import com.oms.model.UserImportRow;
import com.oms.support.DatabaseTest;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** S2-01: nhập người dùng hàng loạt — xem trước, báo lỗi từng dòng, dòng lỗi bị bỏ qua, chỉ Admin. */
class UserImportServiceTest extends DatabaseTest {

    private static final String[] HEADER = {"Họ và tên *", "Tài khoản *", "Email *", "Số điện thoại", "Vai trò *", "Kho", "Địa bàn"};

    private final UserImportService service = new UserImportService();
    private User admin;

    @BeforeEach
    void setUp() throws Exception {
        admin = user("admin", Role.ADMIN);
    }

    @Test
    void previewFlagsEachBadRowWithoutWritingAnything() throws Exception {
        user("da.co", Role.SALES_REP);
        byte[] file = workbook(
                row("An Nguyễn", "an.nguyen", "an@congty.vn", "0901234567", "Nhân viên kinh doanh", "", "Địa bàn A"),
                row("Bình", "binh", "binh@congty.vn", "", "Nhân viên kho", "", ""),
                row("Cường", "da.co", "cuong@congty.vn", "", "Nhân viên kinh doanh", "", ""),
                row("Dũng", "dung", "email-sai", "12", "Chức vụ lạ", "Kho Z", ""),
                row("Em", "an.nguyen", "em@congty.vn", "", "Kế toán công nợ", "", ""),
                row("", "hoa", "hoa@congty.vn", "", "Quản lý kho", "Kho A", ""));

        ImportBatch<UserImportRow> batch = service.preview(file, "nv.xlsx");

        assertEquals(6, batch.getRows().size());
        assertEquals(1, batch.getValidCount());   // chỉ dòng của An hợp lệ
        assertTrue(batch.getRows().get(1).getErrorText().contains("kho"));                // vai trò kho thiếu kho
        assertTrue(batch.getRows().get(2).getErrorText().contains("đã tồn tại"));
        String bad = batch.getRows().get(3).getErrorText();
        assertTrue(bad.contains("Email") && bad.contains("Vai trò") && bad.contains("Kho \"Kho Z\""));
        assertTrue(batch.getRows().get(4).getErrorText().contains("trùng"));
        assertTrue(batch.getRows().get(5).getErrorText().contains("Thiếu họ và tên"));
        assertEquals(2, count("users", "1 = 1"));   // admin và da.co có sẵn: xem trước không ghi gì
    }

    @Test
    void applyCreatesValidAccountsAndSkipsBadRows() throws Exception {
        byte[] file = workbook(
                row("An Nguyễn", "an.nguyen", "an@congty.vn", "0901234567", "Nhân viên kinh doanh; Kế toán công nợ", "", "Địa bàn A"),
                row("Bình", "binh", "binh@congty.vn", "", "Quản lý kho", "Kho A; Kho B", ""),
                row("Cường", "cuong", "sai", "", "Nhân viên kinh doanh", "", ""));

        ImportBatch<UserImportRow> batch = service.preview(file, "nv.xlsx");
        service.apply(batch, admin);

        assertEquals(2, batch.getCreatedCount());
        assertEquals(1, batch.getSkippedCount());
        assertEquals(1, count("users", "username = 'an.nguyen' AND status = 'PENDING' AND must_change_password = TRUE"));
        assertEquals(2, count("user_roles", "user_id = (SELECT id FROM users WHERE username = 'an.nguyen')"));
        assertEquals(2, count("user_warehouses", "user_id = (SELECT id FROM users WHERE username = 'binh')"));
        assertEquals(0, count("users", "username = 'cuong'"));
        assertEquals(2, count("audit_logs", "action = 'USER_CREATE'"));
        assertEquals(1, count("audit_logs", "action = 'USER_IMPORT'"));
        assertThrows(ServiceException.class, () -> service.apply(batch, admin));   // không áp hai lần
    }

    @Test
    void duplicatesInsideTheFileAreFlagged() throws Exception {
        byte[] file = workbook(
                row("An", "anh", "an@congty.vn", "", "Nhân viên kinh doanh", "", ""),
                row("An 2", "ANH", "an2@congty.vn", "", "Nhân viên kinh doanh", "", ""),
                row("An 3", "anh3", "AN@congty.vn", "", "Nhân viên kinh doanh", "", ""));

        ImportBatch<UserImportRow> batch = service.preview(file, "nv.xlsx");

        assertEquals(1, batch.getValidCount());
        assertTrue(batch.getRows().get(1).getErrorText().contains("trùng"));
        assertTrue(batch.getRows().get(2).getErrorText().contains("trùng"));
    }

    @Test
    void onlyAdminCanApply() throws Exception {
        User manager = user("qlkd", Role.SALES_MANAGER);
        ImportBatch<UserImportRow> batch = service.preview(workbook(
                row("An", "anh", "an@congty.vn", "", "Nhân viên kinh doanh", "", "")), "nv.xlsx");

        service.apply(batch, manager);

        assertEquals(1, batch.getSkippedCount());
        assertEquals(0, count("users", "username = 'anh'"));
    }

    @Test
    void rejectsFileMissingRequiredColumn() {
        byte[] file = workbookWith(new String[]{"Họ và tên *", "Tài khoản *"}, new String[]{"An", "an"});
        ServiceException e = assertThrows(ServiceException.class, () -> service.preview(file, "x.xlsx"));
        assertTrue(e.getMessage().contains("Email"));
    }

    @Test
    void templateCanBeReadBackByThePreview() throws Exception {
        ImportBatch<UserImportRow> batch = service.preview(service.template(), "mau.xlsx");
        assertEquals(2, batch.getRows().size());
        assertEquals(2, batch.getValidCount());
    }

    private static String[] row(String... cells) {
        return cells;
    }

    private static byte[] workbook(String[]... rows) {
        return workbookWith(HEADER, rows);
    }

    private static byte[] workbookWith(String[] header, String[]... rows) {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Dữ liệu");
            String[][] all = new String[rows.length + 1][];
            all[0] = header;
            System.arraycopy(rows, 0, all, 1, rows.length);
            for (int r = 0; r < all.length; r++) {
                Row row = sheet.createRow(r);
                for (int c = 0; c < all[r].length; c++) {
                    row.createCell(c).setCellValue(all[r][c]);
                }
            }
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
