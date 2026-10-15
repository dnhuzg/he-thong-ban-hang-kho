package com.oms.service;

import com.oms.model.GoodsReceipt;
import com.oms.model.NamedItem;
import com.oms.model.Role;
import com.oms.model.User;
import com.oms.support.DatabaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoodsReceiptServiceTest extends DatabaseTest {

    private final GoodsReceiptService service = new GoodsReceiptService();
    private User keeper;

    @BeforeEach
    void fixture() throws SQLException {
        sql("""
                CREATE TABLE IF NOT EXISTS inventory_lots (id BIGINT AUTO_INCREMENT PRIMARY KEY, warehouse_id BIGINT,
                    product_id BIGINT, remaining_quantity DECIMAL(14,4), unit_cost DECIMAL(15,2), received_at TIMESTAMP,
                    source_type VARCHAR(30), source_id BIGINT, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)
                """);
        sql("INSERT INTO products (id, sku, name, category_id, base_unit_id, cost_price, status) VALUES "
                + "(1, 'NUOC', 'Nước ngọt', 3, 1, 8000, 'ACTIVE'), (2, 'BANH', 'Bánh', 4, 3, NULL, 'DISCONTINUED')");
        sql("INSERT INTO product_units VALUES (1, 2, 24)");
        sql("INSERT INTO suppliers (id, code, name, status) VALUES (1, 'NCC1', 'Minh Phát', 'ACTIVE'), "
                + "(2, 'NCC2', 'Ngừng', 'INACTIVE')");
        keeper = user("kho", Role.WAREHOUSE);
        keeper.setWarehouses(List.of(new NamedItem(10, "Kho A")));
    }

    private GoodsReceiptService.Form form(Long id, String document, boolean confirm,
                                          GoodsReceiptService.LineInput... lines) {
        return new GoodsReceiptService.Form(id, 1L, document, LocalDate.now(), 10L, null, List.of(lines), confirm);
    }

    private static GoodsReceiptService.LineInput thung(String quantity) {
        return new GoodsReceiptService.LineInput(1L, 2L, new BigDecimal(quantity), "LO-1", LocalDate.now().plusMonths(6));
    }

    private long onHandRows(long quantity) throws SQLException {
        return count("warehouse_inventory", "warehouse_id = 10 AND product_id = 1 AND on_hand = " + quantity);
    }

    @Test
    void draftDoesNotChangeStock() throws SQLException {
        var saved = service.save(form(null, "HD-01", false, thung("2")), keeper);

        assertEquals("DRAFT", saved.status());
        assertEquals("PN" + String.format("%06d", saved.id()), saved.code());
        assertEquals(0, count("warehouse_inventory", "1 = 1"));
        assertEquals(0, count("inventory_lots", "1 = 1"));
        assertEquals(0, count("inventory_movements", "1 = 1"));
        GoodsReceipt receipt = service.page(saved.id(), keeper).current();
        assertEquals(0, new BigDecimal("48").compareTo(receipt.lines().get(0).baseQuantity()));
        assertEquals(0, new BigDecimal("24").compareTo(receipt.lines().get(0).factor()));
    }

    @Test
    void confirmAddsBaseQuantityToStockLotAndMovement() throws SQLException {
        var draft = service.save(form(null, "HD-02", false, thung("1")), keeper);
        var saved = service.save(form(draft.id(), "HD-02", true, thung("2")), keeper);

        assertEquals("CONFIRMED", saved.status());
        assertEquals(1, onHandRows(48));
        assertEquals(1, count("inventory_lots", "warehouse_id = 10 AND product_id = 1 AND remaining_quantity = 48 "
                + "AND unit_cost = 8000 AND source_type = 'GOODS_RECEIPT' AND source_id = " + saved.id()));
        assertEquals(1, count("inventory_movements", "movement_type = 'GOODS_RECEIPT' AND unit_id = 1 "
                + "AND quantity = 48 AND reference_id = " + saved.id()));
        assertEquals(1, count("audit_logs", "action = 'GOODS_RECEIPT_CONFIRM'"));
    }

    @Test
    void confirmedReceiptCannotBeEdited() throws SQLException {
        var saved = service.save(form(null, "HD-03", true, thung("1")), keeper);

        var error = assertThrows(ServiceException.class,
                () -> service.save(form(saved.id(), "HD-03", false, thung("5")), keeper));
        assertTrue(error.getMessage().contains("phiếu điều chỉnh"));
        assertEquals(1, count("warehouse_inventory", "on_hand = 24"));
    }

    @Test
    void sameSupplierDocumentIsRejected() throws SQLException {
        service.save(form(null, "HD-04", false, thung("1")), keeper);

        assertThrows(ServiceException.class, () -> service.save(form(null, "HD-04", false, thung("1")), keeper));
    }

    @Test
    void rejectsInvalidInput() {
        var discontinued = new GoodsReceiptService.LineInput(2L, 3L, BigDecimal.ONE, null, null);
        var undeclaredUnit = new GoodsReceiptService.LineInput(1L, 3L, BigDecimal.ONE, null, null);
        var expired = new GoodsReceiptService.LineInput(1L, 1L, BigDecimal.ONE, "LO", LocalDate.now().minusDays(1));
        var inactiveSupplier = new GoodsReceiptService.Form(null, 2L, "HD-05", LocalDate.now(), 10L, null,
                List.of(thung("1")), false);
        var future = new GoodsReceiptService.Form(null, 1L, "HD-06", LocalDate.now().plusDays(1), 10L, null,
                List.of(thung("1")), false);

        assertThrows(ServiceException.class, () -> service.save(form(null, "HD-05", false, discontinued), keeper));
        assertThrows(ServiceException.class, () -> service.save(form(null, "HD-05", false, undeclaredUnit), keeper));
        assertThrows(ServiceException.class, () -> service.save(form(null, "HD-05", false, expired), keeper));
        assertThrows(ServiceException.class, () -> service.save(form(null, "HD-05", false), keeper));
        assertThrows(ServiceException.class, () -> service.save(inactiveSupplier, keeper));
        assertThrows(ServiceException.class, () -> service.save(future, keeper));
    }

    @Test
    void staffOnlyWorksInAssignedWarehouses() throws SQLException {
        var otherWarehouse = new GoodsReceiptService.Form(null, 1L, "HD-07", LocalDate.now(), 20L, null,
                List.of(thung("1")), false);
        assertThrows(ForbiddenException.class, () -> service.save(otherWarehouse, keeper));

        User admin = user("quantri", Role.ADMIN);
        var saved = service.save(otherWarehouse, admin);
        assertThrows(GoodsReceiptService.NotFoundException.class, () -> service.page(saved.id(), keeper));
        assertTrue(service.page(null, keeper).recent().isEmpty());
        assertEquals(1, service.page(null, keeper).warehouses().size());
        assertNull(service.page(null, keeper).current());
    }

    @Test
    void pageListsOnlyActiveSuppliersAndProductsWithUnits() throws SQLException {
        var page = service.page(null, keeper);

        assertEquals(1, page.suppliers().size());
        assertEquals(1, page.products().size());
        assertEquals(2, page.products().get(0).units().size());
        assertEquals(0, new BigDecimal("24").compareTo(page.products().get(0).units().get(1).factor()));
    }
}
