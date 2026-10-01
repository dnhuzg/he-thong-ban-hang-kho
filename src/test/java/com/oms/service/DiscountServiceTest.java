package com.oms.service;

import com.oms.config.Db;
import com.oms.model.DiscountPolicy;
import com.oms.model.Role;
import com.oms.model.User;
import com.oms.support.DatabaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** S3-01: chính sách chiết khấu theo sản lượng — bậc theo số lượng, % hoặc tiền trên đơn vị, lấy chính sách có lợi nhất. */
class DiscountServiceTest extends DatabaseTest {

    private static final long COCA = 500;
    private static final long SNACK = 501;
    private static final long DRINKS = 1;      // Đồ uống (cha của Nước giải khát, Nước ngọt có ga)
    private static final long SOFT_DRINKS = 3; // Nước ngọt có ga: nhóm của COCA
    private static final LocalDate TODAY = LocalDate.of(2027, 6, 15);

    private final DiscountService service = new DiscountService();
    private User manager;

    @BeforeEach
    void setUp() throws Exception {
        manager = user("qlkd", Role.SALES_MANAGER);
        sql("INSERT INTO products (id, sku, name, category_id, base_unit_id) VALUES (500, 'COCA', 'Coca lon', 3, 1)");
        sql("INSERT INTO products (id, sku, name, category_id, base_unit_id) VALUES (501, 'SNACK', 'Snack', 4, 1)");
    }

    @Test
    void createsPolicyWithTiersAndAudit() throws Exception {
        long id = service.save(0, policy("Hè 2027", COCA, null, DiscountPolicy.Type.PERCENT),
                rows("100", "3", "500", "5,5", "", ""), manager);

        DiscountPolicy p = service.get(id, manager);
        assertEquals("Hè 2027", p.getName());
        assertEquals(2, p.getTiers().size());
        assertEquals(100, p.getTiers().get(0).getMinQty());
        assertEquals(0, new BigDecimal("5.5").compareTo(p.getTiers().get(1).getValue()));
        assertEquals("Sản phẩm", p.getScopeKind());
        assertTrue(p.getScopeText().contains("COCA"));
        assertEquals(1, count("audit_logs", "action = 'DISCOUNT_CREATE'"));
    }

    @Test
    void categoryPolicyShowsPath() throws Exception {
        long id = service.save(0, policy("Nhóm", null, SOFT_DRINKS, DiscountPolicy.Type.AMOUNT), rows("10", "500"), manager);

        assertEquals("Đồ uống • Nước giải khát • Nước ngọt có ga", service.get(id, manager).getScopeText());
    }

    @Test
    void validatesPolicyAndTiers() throws Exception {
        assertThrows(ServiceException.class, () -> service.save(0, policy(" ", COCA, null, DiscountPolicy.Type.PERCENT), rows("10", "5"), manager));
        // phải chọn đúng một trong sản phẩm / nhóm hàng
        assertThrows(ServiceException.class, () -> service.save(0, policy("A", null, null, DiscountPolicy.Type.PERCENT), rows("10", "5"), manager));
        assertThrows(ServiceException.class, () -> service.save(0, policy("A", COCA, SOFT_DRINKS, DiscountPolicy.Type.PERCENT), rows("10", "5"), manager));
        assertThrows(ServiceException.class, () -> service.save(0, policy("A", 999L, null, DiscountPolicy.Type.PERCENT), rows("10", "5"), manager));
        assertThrows(ServiceException.class, () -> service.save(0, policy("A", null, 999L, DiscountPolicy.Type.PERCENT), rows("10", "5"), manager));
        DiscountPolicy p = policy("A", COCA, null, DiscountPolicy.Type.PERCENT);
        assertThrows(ServiceException.class, () -> service.save(0, p, rows(), manager));
        assertThrows(ServiceException.class, () -> service.save(0, p, rows("0", "5"), manager));
        assertThrows(ServiceException.class, () -> service.save(0, p, rows("abc", "5"), manager));
        assertThrows(ServiceException.class, () -> service.save(0, p, rows("10", ""), manager));
        assertThrows(ServiceException.class, () -> service.save(0, p, rows("10", "0"), manager));
        assertThrows(ServiceException.class, () -> service.save(0, p, rows("10", "101"), manager));
        assertThrows(ServiceException.class, () -> service.save(0, p, rows("10", "2,555"), manager));
        // trùng số lượng, bậc cao không giảm nhiều hơn
        assertThrows(ServiceException.class, () -> service.save(0, p, rows("10", "3", "10", "4"), manager));
        ServiceException e = assertThrows(ServiceException.class, () -> service.save(0, p, rows("10", "5", "50", "5"), manager));
        assertTrue(e.getMessage().contains("cao hơn"));
        DiscountPolicy amount = policy("B", COCA, null, DiscountPolicy.Type.AMOUNT);
        assertThrows(ServiceException.class, () -> service.save(0, amount, rows("10", "-5"), manager));
        assertThrows(ServiceException.class, () -> service.save(0, amount, rows("10", "0"), manager));
        List<String> tooMany = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            tooMany.add(String.valueOf(i * 10));
            tooMany.add(String.valueOf(i));
        }
        assertThrows(ServiceException.class, () -> service.save(0, p, rows(tooMany.toArray(new String[0])), manager));
        DiscountPolicy reversed = policy("C", COCA, null, DiscountPolicy.Type.PERCENT);
        reversed.setValidFrom(LocalDate.of(2027, 2, 1));
        reversed.setValidTo(LocalDate.of(2027, 1, 1));
        assertThrows(ServiceException.class, () -> service.save(0, reversed, rows("10", "5"), manager));
        assertEquals(0, count("discount_policies", "1 = 1"));
        assertEquals(0, count("discount_tiers", "1 = 1"));
    }

    @Test
    void onlySalesManagerAndAdminCanManage() throws Exception {
        User accountant = user("ketoan", Role.ACCOUNTANT);
        User rep = user("nvkd", Role.SALES_REP);
        User admin = user("admin2", Role.ADMIN);
        long id = service.save(0, policy("A", COCA, null, DiscountPolicy.Type.PERCENT), rows("10", "5"), manager);

        for (User u : List.of(accountant, rep)) {
            assertThrows(ForbiddenException.class, () -> service.list(u));
            assertThrows(ForbiddenException.class, () -> service.get(id, u));
            assertThrows(ForbiddenException.class, () -> service.save(0, policy("B", COCA, null, DiscountPolicy.Type.PERCENT), rows("10", "5"), u));
            assertThrows(ForbiddenException.class, () -> service.delete(id, u));
        }
        assertEquals(1, service.list(admin).size());
        assertEquals(1, count("discount_policies", "1 = 1"));
    }

    @Test
    void updateReplacesTiersAndLogsOnlyRealChanges() throws Exception {
        long id = service.save(0, policy("A", COCA, null, DiscountPolicy.Type.PERCENT), rows("10", "3", "50", "5"), manager);

        service.save(id, policy("A", COCA, null, DiscountPolicy.Type.PERCENT), rows("10", "3", "50", "5"), manager);
        assertEquals(0, count("audit_logs", "action = 'DISCOUNT_UPDATE'"));

        service.save(id, policy("A mới", null, DRINKS, DiscountPolicy.Type.AMOUNT), rows("20", "1000"), manager);
        DiscountPolicy p = service.get(id, manager);
        assertEquals("A mới", p.getName());
        assertEquals(1, p.getTiers().size());
        assertEquals(20, p.getTiers().get(0).getMinQty());
        assertNull(p.getProductId());
        assertEquals(1, count("audit_logs", "action = 'DISCOUNT_UPDATE'"));
        assertThrows(DiscountService.NotFoundException.class,
                () -> service.save(999, policy("X", COCA, null, DiscountPolicy.Type.PERCENT), rows("10", "5"), manager));
    }

    @Test
    void deleteRemovesPolicyAndTiers() throws Exception {
        long id = service.save(0, policy("A", COCA, null, DiscountPolicy.Type.PERCENT), rows("10", "3"), manager);

        service.delete(id, manager);

        assertEquals(0, count("discount_policies", "1 = 1"));
        assertEquals(0, count("discount_tiers", "1 = 1"));
        assertEquals(1, count("audit_logs", "action = 'DISCOUNT_DELETE'"));
        assertThrows(DiscountService.NotFoundException.class, () -> service.delete(id, manager));
    }

    @Test
    void picksHighestReachedTier() throws Exception {
        service.save(0, policy("A", COCA, null, DiscountPolicy.Type.PERCENT), rows("100", "3", "500", "5"), manager);

        assertNull(best(COCA, "99", "10000"));
        assertEquals(0, new BigDecimal("300.00").compareTo(best(COCA, "100", "10000").perUnit()));
        assertEquals(100, best(COCA, "499", "10000").tierMinQty());
        assertEquals(0, new BigDecimal("500.00").compareTo(best(COCA, "500", "10000").perUnit()));
        assertEquals(500, best(COCA, "100000", "10000").tierMinQty());
        assertNull(best(SNACK, "500", "10000"));
    }

    @Test
    void amountPerUnitIsCappedAtTheUnitPrice() throws Exception {
        service.save(0, policy("A", COCA, null, DiscountPolicy.Type.AMOUNT), rows("10", "800"), manager);

        assertEquals(0, new BigDecimal("800").compareTo(best(COCA, "10", "10000").perUnit()));
        assertEquals(0, new BigDecimal("500").compareTo(best(COCA, "10", "500").perUnit()));
    }

    @Test
    void mostFavourablePolicyWinsAndPoliciesDoNotStack() throws Exception {
        service.save(0, policy("SKU 4%", COCA, null, DiscountPolicy.Type.PERCENT), rows("100", "4"), manager);
        long group = service.save(0, policy("Nhóm cha 450đ", null, DRINKS, DiscountPolicy.Type.AMOUNT), rows("100", "450"), manager);
        service.save(0, policy("Nhóm 3%", null, SOFT_DRINKS, DiscountPolicy.Type.PERCENT), rows("100", "3"), manager);

        // giá 10.000: 4% = 400, 450đ, 3% = 300 → chọn 450đ của nhóm cha (nhóm cha áp cho cả nhóm con)
        DiscountService.Applied a = best(COCA, "100", "10000");
        assertEquals(group, a.policyId());
        assertEquals(0, new BigDecimal("450").compareTo(a.perUnit()));
        // giá 20.000: 4% = 800 lớn nhất
        assertEquals("SKU 4%", best(COCA, "100", "20000").policyName());
        // sản phẩm ngoài nhóm Đồ uống không bị ảnh hưởng
        assertNull(best(SNACK, "100", "20000"));
    }

    @Test
    void equalDiscountKeepsTheEarlierPolicy() throws Exception {
        long first = service.save(0, policy("Đầu", COCA, null, DiscountPolicy.Type.AMOUNT), rows("10", "300"), manager);
        service.save(0, policy("Sau", null, SOFT_DRINKS, DiscountPolicy.Type.AMOUNT), rows("10", "300"), manager);

        assertEquals(first, best(COCA, "10", "10000").policyId());
    }

    @Test
    void ignoresInactiveAndOutOfDatePolicies() throws Exception {
        DiscountPolicy paused = policy("Tạm dừng", COCA, null, DiscountPolicy.Type.PERCENT);
        paused.setStatus(DiscountPolicy.Status.INACTIVE);
        service.save(0, paused, rows("10", "9"), manager);
        DiscountPolicy expired = policy("Hết hạn", COCA, null, DiscountPolicy.Type.PERCENT);
        expired.setValidTo(TODAY.minusDays(1));
        service.save(0, expired, rows("10", "9"), manager);
        DiscountPolicy future = policy("Chưa tới", COCA, null, DiscountPolicy.Type.PERCENT);
        future.setValidFrom(TODAY.plusDays(1));
        service.save(0, future, rows("10", "9"), manager);
        DiscountPolicy current = policy("Đang chạy", COCA, null, DiscountPolicy.Type.PERCENT);
        current.setValidFrom(TODAY);
        current.setValidTo(TODAY);
        long currentId = service.save(0, current, rows("10", "1"), manager);

        DiscountService.Applied a = best(COCA, "10", "10000");
        assertNotNull(a);
        assertEquals(currentId, a.policyId());
        assertEquals("Đang chạy", a.policyName());
        // đúng ngày kết thúc vẫn áp dụng; hôm sau chính sách đó hết, chính sách "Chưa tới" bắt đầu
        try (Connection conn = Db.getConnection()) {
            assertEquals(currentId, service.best(conn, COCA, new BigDecimal("10"), new BigDecimal("10000"), TODAY).policyId());
            assertEquals("Chưa tới", service.best(conn, COCA, new BigDecimal("10"), new BigDecimal("10000"), TODAY.plusDays(1)).policyName());
            // ngày hôm qua là ngày cuối của "Hết hạn" nên chính sách đó vẫn được chọn
            assertEquals("Hết hạn", service.best(conn, COCA, new BigDecimal("10"), new BigDecimal("10000"), TODAY.minusDays(1)).policyName());
        }
    }

    @Test
    void invalidArgumentsGiveNoDiscount() throws Exception {
        service.save(0, policy("A", COCA, null, DiscountPolicy.Type.PERCENT), rows("1", "5"), manager);

        assertNull(best(COCA, "0", "10000"));
        assertNull(best(COCA, "10", "0"));
        assertNull(best(999, "10", "10000"));
    }

    private DiscountService.Applied best(long productId, String qty, String price) throws Exception {
        try (Connection conn = Db.getConnection()) {
            return service.best(conn, productId, new BigDecimal(qty), new BigDecimal(price), TODAY);
        }
    }

    private static DiscountPolicy policy(String name, Long productId, Long categoryId, DiscountPolicy.Type type) {
        DiscountPolicy p = new DiscountPolicy();
        p.setName(name);
        p.setProductId(productId);
        p.setCategoryId(categoryId);
        p.setType(type);
        return p;
    }

    /** rows("100", "3", "500", "5") → {{"100","3"},{"500","5"}}. */
    private static List<String[]> rows(String... cells) {
        List<String[]> rows = new ArrayList<>();
        for (int i = 0; i < cells.length; i += 2) {
            rows.add(new String[]{cells[i], cells[i + 1]});
        }
        return rows;
    }
}
