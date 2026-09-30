package com.oms.service;

import com.oms.config.Db;
import com.oms.model.PriceList;
import com.oms.model.PriceListItem;
import com.oms.model.Role;
import com.oms.model.User;
import com.oms.support.DatabaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** S2-10: bảng giá theo nhóm khách, giá sàn, không chồng hiệu lực, khoá khi có đơn, phiên bản mới. */
class PriceListServiceTest extends DatabaseTest {

    private static final LocalDate JAN_1 = LocalDate.of(2027, 1, 1);

    private final PriceListService service = new PriceListService();
    private User manager;

    @BeforeEach
    void createProducts() throws Exception {
        manager = user("qlkd", Role.SALES_MANAGER);
        sql("INSERT INTO products (id, sku, name, category_id, base_unit_id, cost_price) VALUES (500, 'COCA', 'Coca lon', 3, 1, 8000)");
        sql("INSERT INTO products (id, sku, name, category_id, base_unit_id, status) VALUES (501, 'CU', 'Hàng cũ', 3, 1, 'DISCONTINUED')");
    }

    @Test
    void createsListAndSavesPricesWithAudit() throws Exception {
        long id = service.create(header("bg-dl1", 1, JAN_1, null), manager);
        service.saveItem(id, 500, "10.000", "9.000", manager);
        service.saveItem(id, 500, "10.500", "9.000", manager);

        PriceList pl = service.get(id, manager);
        assertEquals("BG-DL1", pl.getCode());
        assertEquals(1, pl.getItems().size());
        assertEquals(0, new BigDecimal("10500").compareTo(pl.getItems().get(0).getSalePrice()));
        assertEquals(1, count("audit_logs", "action = 'PRICE_LIST_CREATE'"));
        assertEquals(2, count("audit_logs", "action = 'PRICE_ITEM_SAVE'"));
    }

    @Test
    void floorPriceCannotExceedSalePrice() throws Exception {
        long id = service.create(header("BG", 1, JAN_1, null), manager);
        ServiceException e = assertThrows(ServiceException.class, () -> service.saveItem(id, 500, "9000", "9500", manager));
        assertTrue(e.getMessage().contains("Giá sàn"));
        assertThrows(ServiceException.class, () -> service.saveItem(id, 500, "", "9000", manager));
        assertEquals(0, count("price_list_items", "1 = 1"));
    }

    @Test
    void discontinuedProductCannotBeAdded() throws Exception {
        long id = service.create(header("BG", 1, JAN_1, null), manager);
        assertThrows(ServiceException.class, () -> service.saveItem(id, 501, "9000", "8000", manager));
    }

    @Test
    void listsOfSameGroupCannotOverlap() throws Exception {
        service.create(header("BG-A", 1, JAN_1, LocalDate.of(2027, 6, 30)), manager);

        assertThrows(ServiceException.class, () -> service.create(header("BG-B", 1, LocalDate.of(2027, 6, 30), null), manager));
        assertThrows(ServiceException.class, () -> service.create(header("BG-C", 1, LocalDate.of(2026, 12, 1), null), manager));
        service.create(header("BG-D", 1, LocalDate.of(2027, 7, 1), null), manager);   // nối tiếp, không chồng
        service.create(header("BG-E", 2, JAN_1, null), manager);                     // nhóm khác
        assertThrows(ServiceException.class, () -> service.create(header("BG-A", 3, JAN_1, null), manager));   // trùng mã
        assertEquals(3, count("price_lists", "1 = 1"));
    }

    @Test
    void lockedListCannotBeEdited() throws Exception {
        long id = service.create(header("BG", 1, JAN_1, null), manager);
        service.saveItem(id, 500, "10000", "9000", manager);
        try (Connection conn = Db.getConnection()) {
            service.lockForOrder(conn, id);
        }

        assertFalse(service.get(id, manager).isEditable());
        assertThrows(ServiceException.class, () -> service.saveItem(id, 500, "11000", "9000", manager));
        assertThrows(ServiceException.class, () -> service.deleteItem(id, 500, manager));
        assertThrows(ServiceException.class, () -> service.updateHeader(id, header("BG", 1, JAN_1, null), manager));
    }

    @Test
    void newVersionCopiesItemsAndEndsPreviousVersion() throws Exception {
        long v1 = service.create(header("BG", 1, JAN_1, null), manager);
        service.saveItem(v1, 500, "10000", "9000", manager);
        try (Connection conn = Db.getConnection()) {
            service.lockForOrder(conn, v1);
        }

        long v2 = service.newVersion(v1, LocalDate.of(2027, 3, 1), manager);
        service.saveItem(v2, 500, "12000", "10000", manager);   // bản mới sửa được

        PriceList old = service.get(v1, manager);
        PriceList next = service.get(v2, manager);
        assertEquals(LocalDate.of(2027, 2, 28), old.getValidTo());
        assertEquals(2, next.getVersion());
        assertNull(next.getValidTo());
        assertEquals(0, new BigDecimal("10000").compareTo(old.getItems().get(0).getSalePrice()));   // giá cũ giữ nguyên
        assertEquals(2, service.versions("BG").size());
        assertThrows(ServiceException.class, () -> service.newVersion(v1, LocalDate.of(2027, 4, 1), manager));   // đã có bản mới hơn
        assertThrows(ServiceException.class, () -> service.newVersion(v2, LocalDate.of(2027, 3, 1), manager));   // phải sau ngày bắt đầu
    }

    @Test
    void olderVersionIsReadOnly() throws Exception {
        long v1 = service.create(header("BG", 1, JAN_1, null), manager);
        service.newVersion(v1, LocalDate.of(2027, 3, 1), manager);
        assertFalse(service.get(v1, manager).isEditable());
        assertThrows(ServiceException.class, () -> service.saveItem(v1, 500, "10000", "9000", manager));
    }

    @Test
    void effectivePriceDependsOnOrderDate() throws Exception {
        long v1 = service.create(header("BG", 1, JAN_1, null), manager);
        service.saveItem(v1, 500, "10000", "9000", manager);
        long v2 = service.newVersion(v1, LocalDate.of(2027, 3, 1), manager);
        service.saveItem(v2, 500, "12000", "10000", manager);

        try (Connection conn = Db.getConnection()) {
            PriceListItem feb = service.effectivePrice(conn, 1, 500, LocalDate.of(2027, 2, 15));
            PriceListItem mar = service.effectivePrice(conn, 1, 500, LocalDate.of(2027, 3, 1));
            assertEquals(v1, feb.getPriceListId());
            assertEquals(0, new BigDecimal("9000").compareTo(feb.getFloorPrice()));
            assertEquals(0, new BigDecimal("12000").compareTo(mar.getSalePrice()));
            assertNull(service.effectivePrice(conn, 1, 500, LocalDate.of(2026, 12, 31)));
            assertNull(service.effectivePrice(conn, 2, 500, LocalDate.of(2027, 2, 15)));   // nhóm khác
        }
    }

    @Test
    void marginOnlyForCostPriceViewers() throws Exception {
        long id = service.create(header("BG", 1, JAN_1, null), manager);
        service.saveItem(id, 500, "10000", "9000", manager);

        assertNotNull(service.get(id, manager).getItems().get(0).getMarginPercent());
        User rep = user("nvkd", Role.SALES_REP);
        PriceListItem seenByRep = service.get(id, rep).getItems().get(0);
        assertNull(seenByRep.getCostPrice());
        assertNull(seenByRep.getMarginPercent());
    }

    @Test
    void productInPriceListCannotBeDeleted() throws Exception {
        long id = service.create(header("BG", 1, JAN_1, null), manager);
        service.saveItem(id, 500, "10000", "9000", manager);
        assertThrows(ServiceException.class, () -> new ProductService().delete(500, manager));
        assertEquals(1, count("products", "id = 500"));
    }

    private static PriceList header(String code, long groupId, LocalDate from, LocalDate to) {
        PriceList pl = new PriceList();
        pl.setCode(code);
        pl.setName("Bảng giá " + code);
        pl.setCustomerGroupId(groupId);
        pl.setValidFrom(from);
        pl.setValidTo(to);
        return pl;
    }
}
