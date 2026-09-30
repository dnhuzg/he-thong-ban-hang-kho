package com.oms.service;

import com.oms.config.Db;
import com.oms.model.ProductUnit;
import com.oms.model.Role;
import com.oms.model.User;
import com.oms.support.DatabaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** S2-07: nhiều đơn vị quy đổi / SKU, quy về đơn vị cơ sở, đổi hệ số không làm sai số đã ghi. */
class UnitConversionTest extends DatabaseTest {

    @TempDir
    Path uploads;

    private final UnitConversionService service = new UnitConversionService();
    private User manager;
    private long productId;

    @BeforeEach
    void createCanProduct() throws Exception {
        manager = user("qlkd", Role.SALES_MANAGER);
        sql("INSERT INTO units (id, code, name) VALUES (4, 'LOC', 'Lốc')");
        sql("INSERT INTO products (id, sku, name, category_id, base_unit_id) VALUES (500, 'COCA', 'Coca lon', 3, 1)");
        productId = 500;
    }

    @Test
    void declaresSeveralUnitsForOneSku() throws Exception {
        service.save(productId, 4, "6", manager);    // lốc
        service.save(productId, 2, "24", manager);   // thùng

        List<ProductUnit> units = service.list(productId);
        assertEquals(2, units.size());
        assertEquals("Lốc", units.get(0).getUnitName());
        assertEquals("6", units.get(0).getFactorText());
        assertEquals("24", units.get(1).getFactorText());
        assertEquals(2, count("audit_logs", "action = 'PRODUCT_UNIT_SAVE'"));
    }

    @Test
    void anyUnitIsConvertedToBaseUnit() throws Exception {
        service.save(productId, 2, "24", manager);
        try (Connection conn = Db.getConnection()) {
            var twoCases = service.toBase(conn, productId, 2, new BigDecimal("2"));
            assertEquals(0, new BigDecimal("48").compareTo(twoCases.baseQuantity()));
            var cans = service.toBase(conn, productId, 1, new BigDecimal("5"));   // đơn vị cơ sở: hệ số 1
            assertEquals(0, BigDecimal.ONE.compareTo(cans.factor()));
            assertEquals(0, new BigDecimal("5").compareTo(cans.baseQuantity()));
            assertThrows(ServiceException.class, () -> service.toBase(conn, productId, 4, BigDecimal.ONE));   // chưa khai báo lốc
            assertThrows(ServiceException.class, () -> service.toBase(conn, productId, 2, BigDecimal.ZERO));
        }
    }

    @Test
    void changingFactorDoesNotAlterAlreadyRecordedLines() throws Exception {
        service.save(productId, 2, "24", manager);
        UnitConversionService.Converted recorded;
        try (Connection conn = Db.getConnection()) {
            // Dòng giao dịch lưu bản chụp hệ số + số lượng quy đổi tại thời điểm ghi
            recorded = service.toBase(conn, productId, 2, new BigDecimal("3"));
        }
        service.save(productId, 2, "20", manager);   // nhà cung cấp đổi quy cách thùng

        assertEquals(0, new BigDecimal("72").compareTo(recorded.baseQuantity()));
        assertEquals(0, new BigDecimal("24").compareTo(recorded.factor()));
        try (Connection conn = Db.getConnection()) {
            assertEquals(0, new BigDecimal("60").compareTo(service.toBase(conn, productId, 2, new BigDecimal("3")).baseQuantity()));
        }
        assertTrue(count("audit_logs", "action = 'PRODUCT_UNIT_SAVE' AND old_value = '1 Thùng = 24 Lon' AND new_value = '1 Thùng = 20 Lon'") == 1);
    }

    @Test
    void rejectsBaseUnitAndInvalidFactor() throws Exception {
        assertThrows(ServiceException.class, () -> service.save(productId, 1, "2", manager));
        assertThrows(ServiceException.class, () -> service.save(productId, 2, "0", manager));
        assertThrows(ServiceException.class, () -> service.save(productId, 2, "-1", manager));
        assertThrows(ServiceException.class, () -> service.save(productId, 2, "1.23456", manager));
        service.save(productId, 2, "0,5", manager);
        assertEquals("0.5", service.list(productId).get(0).getFactorText());
    }

    @Test
    void baseUnitCannotChangeWhileConversionsExist() throws Exception {
        service.save(productId, 2, "24", manager);
        ProductForm change = new ProductForm();
        change.setId(productId);
        change.setName("Coca lon");
        change.setCategoryId(3);
        change.setBaseUnitId(3);   // đổi đơn vị cơ sở sang gói
        change.setStatus("ACTIVE");
        ProductService products = new ProductService(new FileStorage(uploads));
        assertThrows(ServiceException.class, () -> products.update(change, null, false, manager));

        service.delete(productId, 2, manager);
        products.update(change, null, false, manager);
        assertEquals(1, count("products", "id = 500 AND base_unit_id = 3"));
    }
}
