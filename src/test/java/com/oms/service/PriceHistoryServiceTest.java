package com.oms.service;

import com.oms.dao.PriceHistoryDao;
import com.oms.model.PriceChange;
import com.oms.model.PriceList;
import com.oms.model.Role;
import com.oms.model.User;
import com.oms.support.DatabaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** S3-02: mỗi lần thêm / đổi / bỏ giá ghi một dòng lịch sử (giá cũ, giá mới, người sửa, ngày áp dụng); lịch sử chỉ thêm. */
class PriceHistoryServiceTest extends DatabaseTest {

    private static final LocalDate JAN_1 = LocalDate.of(2027, 1, 1);

    private final PriceListService priceLists = new PriceListService();
    private final PriceHistoryService history = new PriceHistoryService();
    private User manager;
    private User other;

    @BeforeEach
    void setUp() throws Exception {
        manager = user("qlkd", Role.SALES_MANAGER);
        other = user("qlkd2", Role.SALES_MANAGER);
        sql("INSERT INTO products (id, sku, name, category_id, base_unit_id) VALUES (500, 'COCA', 'Coca lon', 3, 1)");
        sql("INSERT INTO products (id, sku, name, category_id, base_unit_id) VALUES (501, 'PEPSI', 'Pepsi lon', 3, 1)");
    }

    @Test
    void recordsAddUpdateAndRemoveWithOldAndNewPrices() throws Exception {
        long id = priceLists.create(header("BG1", JAN_1), manager);
        priceLists.saveItem(id, 500, "10.000", "9.000", manager);
        priceLists.saveItem(id, 500, "10.500", "9.200", other);
        priceLists.deleteItem(id, 500, manager);

        List<PriceChange> changes = history.history(500);
        assertEquals(3, changes.size());
        // mới nhất trước
        PriceChange removed = changes.get(0);
        assertEquals(PriceChange.Type.REMOVE, removed.getType());
        assertEquals(0, new BigDecimal("10500").compareTo(removed.getOldSalePrice()));
        assertNull(removed.getNewSalePrice());
        PriceChange updated = changes.get(1);
        assertEquals(PriceChange.Type.UPDATE, updated.getType());
        assertEquals(0, new BigDecimal("10000").compareTo(updated.getOldSalePrice()));
        assertEquals(0, new BigDecimal("9000").compareTo(updated.getOldFloorPrice()));
        assertEquals(0, new BigDecimal("10500").compareTo(updated.getNewSalePrice()));
        assertEquals(0, new BigDecimal("9200").compareTo(updated.getNewFloorPrice()));
        assertEquals("qlkd2", updated.getChangedByName());
        PriceChange added = changes.get(2);
        assertEquals(PriceChange.Type.ADD, added.getType());
        assertNull(added.getOldSalePrice());
        assertEquals("BG1", added.getPriceListCode());
        assertEquals(1, added.getPriceListVersion());
        assertEquals("01/01/2027", added.getEffectiveText());
    }

    @Test
    void savingSamePricesAddsNoHistory() throws Exception {
        long id = priceLists.create(header("BG1", JAN_1), manager);
        priceLists.saveItem(id, 500, "10.000", "9.000", manager);
        priceLists.saveItem(id, 500, "10.000", "9.000", manager);

        assertEquals(1, history.history(500).size());
    }

    @Test
    void rejectedChangeLeavesNoHistory() throws Exception {
        long id = priceLists.create(header("BG1", JAN_1), manager);
        assertThrows(ServiceException.class, () -> priceLists.saveItem(id, 500, "9.000", "9.500", manager));

        assertEquals(0, history.history(500).size());
    }

    @Test
    void historyIsPerProductAndKeepsChainAcrossVersions() throws Exception {
        long v1 = priceLists.create(header("BG1", JAN_1), manager);
        priceLists.saveItem(v1, 500, "10.000", "9.000", manager);
        priceLists.saveItem(v1, 501, "12.000", "11.000", manager);
        long v2 = priceLists.newVersion(v1, LocalDate.of(2027, 3, 1), manager);
        priceLists.saveItem(v2, 500, "11.000", "9.500", manager);

        List<PriceChange> coca = history.history(500);
        assertEquals(2, coca.size());
        PriceChange latest = coca.get(0);
        assertEquals(2, latest.getPriceListVersion());
        assertEquals("01/03/2027", latest.getEffectiveText());
        // giá cũ của phiên bản 2 là giá đã chép từ phiên bản 1
        assertEquals(0, new BigDecimal("10000").compareTo(latest.getOldSalePrice()));
        assertEquals(1, history.history(501).size());
    }

    @Test
    void unknownProductIsNotFound() {
        assertThrows(PriceHistoryService.NotFoundException.class, () -> history.product(999));
    }

    @Test
    void historyCannotBeEditedOrDeletedThroughTheDao() {
        for (Method m : PriceHistoryDao.class.getDeclaredMethods()) {
            String name = m.getName().toLowerCase();
            assertTrue(!name.startsWith("update") && !name.startsWith("delete") && !name.startsWith("remove"),
                    "DAO lịch sử giá không được có hàm sửa / xoá: " + m.getName());
        }
    }

    private static PriceList header(String code, LocalDate from) {
        PriceList pl = new PriceList();
        pl.setCode(code);
        pl.setName("Bảng giá " + code);
        pl.setCustomerGroupId(1);
        pl.setValidFrom(from);
        return pl;
    }
}
