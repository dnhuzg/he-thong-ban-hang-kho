package com.oms.service;

import com.oms.model.ImportBatch;
import com.oms.model.ProductImportRow;
import com.oms.model.Role;
import com.oms.model.User;
import com.oms.support.DatabaseTest;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** S2-08: nhập sản phẩm hàng loạt từ Excel — xem trước, báo lỗi từng dòng, SKU có sẵn thì cập nhật. */
class ProductImportServiceTest extends DatabaseTest {

    private static final String[] HEADER = {"Mã SKU *", "Tên sản phẩm *", "Nhóm hàng *", "Đơn vị tính *", "Quy cách", "Giá vốn", "Trạng thái"};

    private final ProductImportService service = new ProductImportService();
    private User manager;

    @BeforeEach
    void setUp() throws Exception {
        manager = user("qlkd", Role.SALES_MANAGER);
    }

    @Test
    void previewFlagsErrorsPerRowAndMarksExistingSku() throws Exception {
        sql("INSERT INTO products (id, sku, name, category_id, base_unit_id) VALUES (500, 'COCA', 'Coca cũ', 3, 1)");
        byte[] file = workbook(HEADER,
                row("coca", "Coca lon 330ml", "Đồ uống › Nước giải khát › Nước ngọt có ga", "Lon", "Thùng 24", "7.200", ""),
                row("MI-1", "Mì Hảo Hảo", "Thực phẩm khô", "Gói", "", "3500", "Đang kinh doanh"),
                row("", "Thiếu SKU", "Thực phẩm khô", "Gói", "", "", ""),
                row("BAD-1", "Nhóm sai", "Không có nhóm", "Gói", "", "", ""),
                row("BAD-2", "Đơn vị sai", "Thực phẩm khô", "Chai", "", "abc", "Tạm ngưng"),
                row("MI-1", "Trùng SKU", "Thực phẩm khô", "Gói", "", "", ""));

        ImportBatch<ProductImportRow> batch = service.preview(file, "sp.xlsx", manager);

        assertEquals(6, batch.getRows().size());
        assertEquals(2, batch.getValidCount());
        assertEquals(4, batch.getErrorCount());
        assertTrue(batch.getRows().get(0).isExisting());
        assertFalse(batch.getRows().get(1).isExisting());
        assertEquals(4, batch.getRows().get(2).getRowNumber());   // dòng Excel: tiêu đề là dòng 1
        assertTrue(batch.getRows().get(4).getErrorText().contains("Đơn vị tính"));
        assertTrue(batch.getRows().get(4).getErrorText().contains("Giá vốn"));
        assertTrue(batch.getRows().get(4).getErrorText().contains("Trạng thái"));
        assertTrue(batch.getRows().get(5).getErrorText().contains("trùng"));
        assertEquals(1, count("products", "1 = 1"));   // xem trước chưa ghi gì
    }

    @Test
    void applyCreatesUpdatesAndSkipsBadRows() throws Exception {
        sql("INSERT INTO products (id, sku, name, category_id, base_unit_id, cost_price) VALUES (500, 'COCA', 'Coca cũ', 3, 1, 5000)");
        byte[] file = workbook(HEADER,
                row("COCA", "Coca lon 330ml", "Nước ngọt có ga", "Lon", "Thùng 24", "7200", "Ngừng kinh doanh"),
                row("MI-1", "Mì Hảo Hảo", "Thực phẩm khô", "Gói", "", "3500", ""),
                row("BAD", "", "Thực phẩm khô", "Gói", "", "", ""));

        ImportBatch<ProductImportRow> batch = service.preview(file, "sp.xlsx", manager);
        service.apply(batch, manager);

        assertEquals(1, batch.getCreatedCount());
        assertEquals(1, batch.getUpdatedCount());
        assertEquals(1, batch.getSkippedCount());
        assertEquals(2, count("products", "1 = 1"));
        assertEquals(1, count("products", "sku = 'COCA' AND name = 'Coca lon 330ml' AND status = 'DISCONTINUED' AND cost_price = 7200"));
        assertEquals(1, count("products", "sku = 'MI-1' AND cost_price = 3500 AND status = 'ACTIVE'"));
        assertEquals(1, count("audit_logs", "action = 'PRODUCT_IMPORT'"));
        assertEquals(1, count("audit_logs", "action = 'PRODUCT_CREATE'"));
        assertEquals(1, count("audit_logs", "action = 'PRODUCT_UPDATE'"));
        assertThrows(ServiceException.class, () -> service.apply(batch, manager));   // không áp hai lần
    }

    @Test
    void blankCostKeepsOldCostAndUnchangedRowIsReported() throws Exception {
        sql("INSERT INTO products (id, sku, name, category_id, base_unit_id, cost_price) VALUES (500, 'COCA', 'Coca lon', 3, 1, 5000)");
        byte[] file = workbook(HEADER, row("COCA", "Coca lon", "Nước ngọt có ga", "Lon", "", "", ""));

        ImportBatch<ProductImportRow> batch = service.preview(file, "sp.xlsx", manager);
        service.apply(batch, manager);

        assertEquals(1, batch.getUnchangedCount());
        assertEquals(1, count("products", "sku = 'COCA' AND cost_price = 5000"));
    }

    @Test
    void costColumnIgnoredForUsersWithoutCostPermission() throws Exception {
        User rep = user("nvkd", Role.SALES_REP);
        byte[] file = workbook(HEADER, row("MI-1", "Mì", "Thực phẩm khô", "Gói", "", "3500", ""));

        ImportBatch<ProductImportRow> batch = service.preview(file, "sp.xlsx", rep);
        service.apply(batch, rep);

        assertEquals(1, batch.getNotes().size());
        assertEquals(1, count("products", "sku = 'MI-1' AND cost_price IS NULL"));
    }

    @Test
    void changingBaseUnitIsSkippedWhenConversionsExist() throws Exception {
        sql("INSERT INTO products (id, sku, name, category_id, base_unit_id) VALUES (500, 'COCA', 'Coca', 3, 1)");
        sql("INSERT INTO product_units (product_id, unit_id, factor) VALUES (500, 2, 24)");
        byte[] file = workbook(HEADER, row("COCA", "Coca", "Nước ngọt có ga", "Gói", "", "", ""));

        ImportBatch<ProductImportRow> batch = service.preview(file, "sp.xlsx", manager);
        service.apply(batch, manager);

        assertEquals(1, batch.getSkippedCount());
        assertEquals(1, count("products", "sku = 'COCA' AND base_unit_id = 1"));
    }

    @Test
    void rejectsBadFilesAsAWhole() {
        assertThrows(ServiceException.class, () -> service.preview(new byte[0], "x.xlsx", manager));
        assertThrows(ServiceException.class, () -> service.preview("khong phai excel".getBytes(), "x.xlsx", manager));
        byte[] missingColumn = workbook(new String[]{"Mã SKU *", "Tên sản phẩm *"}, new String[]{"A1", "Tên"});
        ServiceException e = assertThrows(ServiceException.class, () -> service.preview(missingColumn, "x.xlsx", manager));
        assertTrue(e.getMessage().contains("Nhóm hàng"));
    }

    @Test
    void templateCanBeReadBackByThePreview() throws Exception {
        ImportBatch<ProductImportRow> batch = service.preview(service.template(), "mau.xlsx", manager);
        assertEquals(2, batch.getRows().size());
        assertEquals(2, batch.getValidCount());
    }

    private static String[] row(String... cells) {
        return cells;
    }

    private static byte[] workbook(String[] header, String[]... rows) {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Dữ liệu");
            List<String[]> all = new ArrayList<>();
            all.add(header);
            all.addAll(List.of(rows));
            for (int r = 0; r < all.size(); r++) {
                Row row = sheet.createRow(r);
                for (int c = 0; c < all.get(r).length; c++) {
                    row.createCell(c).setCellValue(all.get(r)[c]);
                }
            }
            wb.write(out);
            return out.toByteArray();
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
