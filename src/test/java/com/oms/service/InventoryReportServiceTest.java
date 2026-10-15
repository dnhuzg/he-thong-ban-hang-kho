package com.oms.service;

import com.oms.model.InventoryReport;
import com.oms.model.InventorySkuAgeRow;
import com.oms.model.InventoryValueRow;
import com.oms.support.DatabaseTest;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class InventoryReportServiceTest extends DatabaseTest {

    private final InventoryReportService service = new InventoryReportService();

    @BeforeEach
    void prepareInventoryFixture() throws Exception {
        sql("INSERT INTO product_categories (id, parent_id, name) VALUES (9001, NULL, 'NhÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³m test')");
        sql("INSERT INTO units (id, code, name) VALUES (9001, 'TESTU', 'ÃƒÆ’Ã¢â‚¬Å¾Ãƒâ€šÃ‚ÂÃƒÆ’Ã¢â‚¬Â Ãƒâ€šÃ‚Â¡n test')");
        sql("""
                INSERT INTO products
                    (id, sku, name, category_id, base_unit_id, cost_price, status)
                VALUES
                    (9001, 'TEST-AGE-1', 'SÃƒÆ’Ã‚Â¡Ãƒâ€šÃ‚ÂºÃƒâ€šÃ‚Â£n phÃƒÆ’Ã‚Â¡Ãƒâ€šÃ‚ÂºÃƒâ€šÃ‚Â©m test chÃƒÆ’Ã‚Â¡Ãƒâ€šÃ‚ÂºÃƒâ€šÃ‚Â­m', 9001, 9001, 100.00, 'ACTIVE'),
                    (9002, 'TEST-AGE-2', 'SÃƒÆ’Ã‚Â¡Ãƒâ€šÃ‚ÂºÃƒâ€šÃ‚Â£n phÃƒÆ’Ã‚Â¡Ãƒâ€šÃ‚ÂºÃƒâ€šÃ‚Â©m test nhanh', 9001, 9001, 50.00, 'ACTIVE')
                """);

        LocalDate today = LocalDate.now();
        sql("INSERT INTO inventory_lots (warehouse_id, product_id, remaining_quantity, unit_cost, received_at) " +
                "VALUES (10, 9001, 10.0000, 100.00, '" + LocalDateTime.now().minusDays(40) + "')");
        sql("INSERT INTO inventory_lots (warehouse_id, product_id, remaining_quantity, unit_cost, received_at) " +
                "VALUES (20, 9001, 5.0000, 100.00, '" + LocalDateTime.now().minusDays(20) + "')");
        sql("INSERT INTO inventory_lots (warehouse_id, product_id, remaining_quantity, unit_cost, received_at) " +
                "VALUES (10, 9002, 20.0000, 50.00, '" + today.minusDays(10).atStartOfDay() + "')");
    }

    @Test
    void groupsStockValueByWarehouseAndCategory() throws Exception {
        InventoryReport report = service.report(null, 9001L);

        assertEquals(2, report.valueRows().size());

        InventoryValueRow khoA = report.valueRows().stream()
                .filter(row -> row.warehouseId() == 10)
                .findFirst()
                .orElseThrow();

        assertEquals(new BigDecimal("30.0000"), khoA.quantity());
        assertEquals(new BigDecimal("2000.000000"), khoA.stockValue());
    }

    @Test
    void calculatesWeightedAverageAgeAndMarksSlowMoving() throws Exception {
        InventoryReport report = service.report(null, 9001L);

        InventorySkuAgeRow slow = report.skuAgeRows().stream()
                .filter(row -> row.sku().equals("TEST-AGE-1"))
                .findFirst()
                .orElseThrow();

        assertEquals(new BigDecimal("15.0000"), slow.quantity());
        assertEquals(new BigDecimal("33.33"), slow.averageAgeDays());
        assertTrue(slow.slowMoving());

        InventorySkuAgeRow fast = report.skuAgeRows().stream()
                .filter(row -> row.sku().equals("TEST-AGE-2"))
                .findFirst()
                .orElseThrow();

        assertEquals(new BigDecimal("20.0000"), fast.quantity());
        assertEquals(new BigDecimal("10.00"), fast.averageAgeDays());
        assertFalse(fast.slowMoving());
    }

    @Test
    void exportsValidXlsx() throws Exception {
        byte[] data = service.exportXlsx(null, 9001L);

        assertTrue(data.length > 4);
        assertEquals((byte) 'P', data[0]);
        assertEquals((byte) 'K', data[1]);

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(data))) {
            assertEquals("\u0047\u0069\u00E1\u0020\u0074\u0072\u1ECB\u0020\u0074\u1ED3\u006E", workbook.getSheetAt(0).getSheetName());
            assertEquals("\u004E\u0067\u00E0\u0079\u0020\u0074\u1ED3\u006E\u0020\u0053\u004B\u0055", workbook.getSheetAt(1).getSheetName());
            assertEquals(3, workbook.getSheetAt(0).getLastRowNum() + 1);
        }
    }
}