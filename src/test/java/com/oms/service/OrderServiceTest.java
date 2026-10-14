
package com.oms.service;

import com.oms.model.DiscountPolicy;
import com.oms.model.Order;
import com.oms.model.OrderLine;
import com.oms.model.PriceList;
import com.oms.model.Role;
import com.oms.model.User;
import com.oms.support.DatabaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Kiểm thử đơn hàng và tính năng đặt lại đơn S5-02. */
class OrderServiceTest extends DatabaseTest {

    private static final long COCA = 500;
    private static final long SNACK = 501;
    private static final long OLD = 502;
    private static final long LON = 1;
    private static final long THUNG = 2;
    private static final long GOI = 3;

    private final OrderService service = new OrderService();

    private User manager;
    private User rep;
    private User otherRep;
    private User customer;
    private User otherCustomer;
    private long priceListId;

    @BeforeEach
    void setUp() throws Exception {
        manager = user("qlkd", Role.SALES_MANAGER);
        rep = user("nvkd", Role.SALES_REP);
        otherRep = user("nvkd2", Role.SALES_REP);
        customer = user("dal900", Role.CUSTOMER);
        otherCustomer = user("dal901", Role.CUSTOMER);

        customer.setCustomerId(900L);
        otherCustomer.setCustomerId(901L);

        sql("INSERT INTO customers (id, code, name, group_id, sales_rep_id) "
                + "VALUES (900, 'DL900', 'Đại lý thử', 1, " + rep.getId() + ")");
        sql("INSERT INTO customers (id, code, name, group_id, sales_rep_id) "
                + "VALUES (901, 'DL901', 'Đại lý khác', 1, " + otherRep.getId() + ")");

        sql("UPDATE customers SET user_id = " + customer.getId() + " WHERE id = 900");
        sql("UPDATE customers SET user_id = " + otherCustomer.getId() + " WHERE id = 901");

        sql("INSERT INTO customer_delivery_points "
                + "(id, customer_id, address, receiver_name, receiver_phone, is_default) "
                + "VALUES (800, 900, '12 Hàng Bài', 'Chị Lan', '0901234567', FALSE), "
                + "(801, 900, 'Kho Long Biên', 'Anh Hùng', '0912345678', TRUE), "
                + "(802, 901, '5 Nguyễn Trãi', 'Chú Ba', '0933333333', TRUE)");

        sql("INSERT INTO products (id, sku, name, category_id, base_unit_id) "
                + "VALUES (500, 'COCA', 'Coca lon', 3, 1)");
        sql("INSERT INTO products (id, sku, name, category_id, base_unit_id) "
                + "VALUES (501, 'SNACK', 'Snack', 4, 3)");
        sql("INSERT INTO products "
                + "(id, sku, name, category_id, base_unit_id, status) "
                + "VALUES (502, 'OLD', 'Hàng cũ', 3, 1, 'DISCONTINUED')");

        sql("INSERT INTO product_units (product_id, unit_id, factor) "
                + "VALUES (500, 2, 24)");

        PriceList list = new PriceList();
        list.setCode("BG1");
        list.setName("Bảng giá cấp 1");
        list.setCustomerGroupId(1);
        list.setValidFrom(LocalDate.of(2020, 1, 1));

        priceListId = new PriceListService().create(list, manager);
        new PriceListService().saveItem(
                priceListId, COCA, "10.000", "9.000", manager
        );
    }

    @Test
    void createsDraftWithCodeAndDefaultDeliveryPoint() throws Exception {
        long id = service.create(900, rep);

        Order o = service.get(id, rep);
        assertEquals(String.format("DH%06d", id), o.getCode());
        assertEquals("DL900", o.getCustomerCode());
        assertEquals("Đại lý cấp 1", o.getCustomerGroupName());
        assertEquals(801L, o.getDeliveryPointId());
        assertTrue(o.getDeliveryPointText().contains("Kho Long Biên"));
        assertNull(o.getDeliveryDate());
        assertEquals(0, o.getLines().size());
        assertEquals(0, BigDecimal.ZERO.compareTo(o.getPayable()));
        assertEquals(1, count("audit_logs", "action = 'ORDER_CREATE'"));
    }

    @Test
    void onlySalesRolesTakeOrders() throws Exception {
        User accountant = user("ketoan", Role.ACCOUNTANT);
        User warehouse = user("kho", Role.WAREHOUSE);
        User admin = user("admin2", Role.ADMIN);
        long id = service.create(900, manager);

        for (User u : List.of(accountant, warehouse)) {
            assertThrows(ForbiddenException.class, () -> service.search(null, 1, u));
            assertThrows(ForbiddenException.class, () -> service.create(900, u));
            assertThrows(ForbiddenException.class, () -> service.get(id, u));
            assertThrows(ForbiddenException.class, () -> service.addLine(id, COCA, LON, "1", u));
            assertThrows(ForbiddenException.class, () -> service.delete(id, u));
        }

        assertEquals(1, service.search(null, 1, admin).getTotal());
    }

    @Test
    void salesRepOnlyHandlesOrdersOfOwnCustomers() throws Exception {
        assertThrows(OrderService.NotFoundException.class, () -> service.create(901, rep));
        assertThrows(OrderService.NotFoundException.class, () -> service.create(999, rep));

        long own = service.create(900, rep);
        long others = service.create(901, manager);

        assertEquals(1, service.search(null, 1, rep).getTotal());
        assertEquals(2, service.search(null, 1, manager).getTotal());
        assertThrows(OrderService.NotFoundException.class, () -> service.get(others, rep));
        assertThrows(OrderService.NotFoundException.class, () -> service.addLine(others, COCA, LON, "1", rep));
        assertThrows(OrderService.NotFoundException.class, () -> service.saveHeader(others, null, null, null, rep));
        assertThrows(OrderService.NotFoundException.class, () -> service.delete(others, rep));
        assertThrows(OrderService.NotFoundException.class, () -> service.get(own, otherRep));
        assertEquals(1, count("orders", "id = " + others));
    }

    @Test
    void lockedCustomerCannotStartNewOrderButOpenDraftContinues() throws Exception {
        long id = service.create(900, rep);
        sql("UPDATE customers SET status = 'LOCKED', lock_reason = 'Nợ quá hạn' WHERE id = 900");

        ServiceException e = assertThrows(
                ServiceException.class, () -> service.create(900, rep)
        );
        assertTrue(e.getMessage().contains("khoá giao dịch"));
        assertEquals(1, count("orders", "customer_id = 900"));

        service.addLine(id, COCA, LON, "5", rep);
        Order o = service.get(id, rep);
        assertTrue(o.isCustomerLocked());
        assertEquals("Nợ quá hạn", o.getCustomerLockReason());
        assertEquals(1, o.getLines().size());
    }

    @Test
    void headerKeepsOwnPointDateAndNote() throws Exception {
        long id = service.create(900, rep);
        LocalDate day = LocalDate.now().plusDays(2);

        service.saveHeader(id, 800L, day, "  Giao buổi sáng  ", rep);

        Order o = service.get(id, rep);
        assertEquals(800L, o.getDeliveryPointId());
        assertEquals(day, o.getDeliveryDate());
        assertEquals("Giao buổi sáng", o.getNote());
        assertEquals(1, count("audit_logs", "action = 'ORDER_UPDATE'"));

        service.saveHeader(id, 800L, day, "Giao buổi sáng", rep);
        service.saveHeader(id, 800L, LocalDate.now(), "Giao buổi sáng", rep);
        assertEquals(2, count("audit_logs", "action = 'ORDER_UPDATE'"));
    }

    @Test
    void headerRejectsForeignPointPastDateAndLongNote() throws Exception {
        long id = service.create(900, rep);

        assertThrows(ServiceException.class, () -> service.saveHeader(id, 802L, null, null, rep));
        assertThrows(ServiceException.class, () -> service.saveHeader(id, 999L, null, null, rep));
        assertThrows(ServiceException.class, () -> service.saveHeader(id, 800L, LocalDate.now().minusDays(1), null, rep));
        assertThrows(ServiceException.class, () -> service.saveHeader(id, 800L, null, "x".repeat(501), rep));

        service.saveHeader(id, null, null, null, rep);
        assertNull(service.get(id, rep).getDeliveryPointId());
    }

    @Test
    void totalsAndDiscountUseAllLinesOfTheSameProduct() throws Exception {
        DiscountPolicy policy = new DiscountPolicy();
        policy.setName("Coca 5%");
        policy.setProductId(COCA);
        policy.setType(DiscountPolicy.Type.PERCENT);
        new DiscountService().save(
                0, policy, List.<String[]>of(new String[]{"100", "5"}), manager
        );

        long id = service.create(900, rep);
        service.addLine(id, COCA, LON, "10", rep);

        Order o = service.get(id, rep);
        assertEquals(0, new BigDecimal("100000").compareTo(o.getGrossTotal()));
        assertEquals(0, BigDecimal.ZERO.compareTo(o.getDiscountTotal()));
        assertEquals(0, new BigDecimal("100000").compareTo(o.getPayable()));

        service.addLine(id, COCA, THUNG, "5", rep);
        o = service.get(id, rep);

        OrderLine lon = o.getLines().get(0);
        OrderLine thung = o.getLines().get(1);

        assertEquals(0, new BigDecimal("10000.00").compareTo(lon.getUnitPrice()));
        assertEquals(0, new BigDecimal("5000.00").compareTo(lon.getDiscountAmount()));
        assertEquals(0, new BigDecimal("240000.00").compareTo(thung.getUnitPrice()));
        assertEquals(0, new BigDecimal("1200000.00").compareTo(thung.getGrossAmount()));
        assertEquals(0, new BigDecimal("60000.00").compareTo(thung.getDiscountAmount()));
        assertEquals(0, new BigDecimal("1140000.00").compareTo(thung.getNetAmount()));
        assertTrue(thung.getDiscountNote().contains("Coca 5%"));
        assertTrue(thung.isConverted());
        assertFalse(lon.isConverted());
        assertEquals(0, new BigDecimal("1300000").compareTo(o.getGrossTotal()));
        assertEquals(0, new BigDecimal("65000").compareTo(o.getDiscountTotal()));
        assertEquals(0, new BigDecimal("1235000").compareTo(o.getPayable()));
        assertEquals(0, new BigDecimal("120").compareTo(thung.getBaseQuantity()));
        assertEquals(0, new BigDecimal("24").compareTo(thung.getFactor()));
    }

    @Test
    void sameProductAndUnitMergesIntoOneLine() throws Exception {
        long id = service.create(900, rep);

        service.addLine(id, COCA, LON, "5", rep);
        service.addLine(id, COCA, LON, "7", rep);

        Order o = service.get(id, rep);
        assertEquals(1, o.getLines().size());
        assertEquals(12, o.getLines().get(0).getQuantity());
        assertEquals(0, new BigDecimal("120000").compareTo(o.getPayable()));
    }

    @Test
    void rejectsBadLines() throws Exception {
        long id = service.create(900, rep);

        assertThrows(ServiceException.class, () -> service.addLine(id, COCA, LON, "0", rep));
        assertThrows(ServiceException.class, () -> service.addLine(id, COCA, LON, "abc", rep));
        assertThrows(ServiceException.class, () -> service.addLine(id, COCA, LON, "", rep));
        assertThrows(ServiceException.class, () -> service.addLine(id, COCA, LON, "1000001", rep));
        assertThrows(ServiceException.class, () -> service.addLine(id, 999, LON, "1", rep));

        ServiceException noPrice = assertThrows(
                ServiceException.class, () -> service.addLine(id, SNACK, GOI, "1", rep)
        );
        assertTrue(noPrice.getMessage().contains("chưa có giá"));

        assertThrows(ServiceException.class, () -> service.addLine(id, OLD, LON, "1", rep));
        assertThrows(ServiceException.class, () -> service.addLine(id, COCA, GOI, "1", rep));
        assertEquals(0, count("order_lines", "1 = 1"));
    }

    @Test
    void updateAndDeleteLines() throws Exception {
        long id = service.create(900, rep);
        service.addLine(id, COCA, LON, "5", rep);
        service.addLine(id, COCA, THUNG, "1", rep);

        List<OrderLine> lines = service.get(id, rep).getLines();
        long lonLine = lines.get(0).getId();
        long thungLine = lines.get(1).getId();

        service.updateLine(id, lonLine, LON, "20", rep);
        assertEquals(20, service.get(id, rep).getLines().get(0).getQuantity());

        assertThrows(ServiceException.class, () -> service.updateLine(id, lonLine, THUNG, "1", rep));
        assertThrows(ServiceException.class, () -> service.updateLine(id, lonLine, LON, "0", rep));
        assertThrows(ServiceException.class, () -> service.updateLine(id, 9999, LON, "1", rep));

        service.updateLine(id, thungLine, THUNG, "2", rep);
        assertEquals(2, service.get(id, rep).getLines().get(1).getQuantity());
        assertEquals(0, new BigDecimal("48").compareTo(service.get(id, rep).getLines().get(1).getBaseQuantity()));
    }

    @Test
    void deleteLineAndOrder() throws Exception {
        long id = service.create(900, rep);
        service.addLine(id, COCA, LON, "5", rep);
        long line = service.get(id, rep).getLines().get(0).getId();

        service.deleteLine(id, line, rep);
        assertEquals(0, service.get(id, rep).getLines().size());
        assertThrows(ServiceException.class, () -> service.deleteLine(id, line, rep));

        service.addLine(id, COCA, LON, "5", rep);
        service.delete(id, rep);

        assertEquals(0, count("orders", "id = " + id));
        assertEquals(0, count("order_lines", "1 = 1"));
        assertEquals(1, count("audit_logs", "action = 'ORDER_DELETE'"));
        assertThrows(OrderService.NotFoundException.class, () -> service.get(id, rep));
    }

    @Test
    void lineWithoutCurrentPriceIsFlaggedAndExcludedFromTotals() throws Exception {
        long id = service.create(900, rep);
        service.addLine(id, COCA, LON, "10", rep);
        sql("DELETE FROM price_list_items");

        Order o = service.get(id, rep);
        assertEquals(1, o.getUnpricedLines());
        assertFalse(o.getLines().get(0).isPriced());
        assertEquals(0, BigDecimal.ZERO.compareTo(o.getPayable()));
    }

    @Test
    void productSearchShowsPriceAndUnitsOnlyForActiveProducts() throws Exception {
        long id = service.create(900, rep);
        Order order = service.get(id, rep);

        List<OrderService.ProductChoice> byName = service.searchProducts(order, "Coca");
        assertEquals(1, byName.size());

        OrderService.ProductChoice coca = byName.get(0);
        assertEquals("COCA", coca.sku());
        assertEquals(0, new BigDecimal("10000").compareTo(coca.basePrice()));
        assertEquals(2, coca.units().size());
        assertEquals("Lon", coca.units().get(0).getName());
        assertTrue(coca.units().get(1).getName().contains("= 24"));

        assertEquals(1, service.searchProducts(order, "SNACK").size());
        assertNull(service.searchProducts(order, "SNACK").get(0).basePrice());
        assertEquals(0, service.searchProducts(order, "OLD").size());
        assertEquals(0, service.searchProducts(order, " ").size());
    }

    @Test
    void customerCanReorderSelectedLinesWithCurrentPrice() throws Exception {
        long oldOrderId = service.create(900, rep);
        service.addLine(oldOrderId, COCA, LON, "5", rep);
        service.addLine(oldOrderId, COCA, THUNG, "1", rep);

        Order oldOrder = service.get(oldOrderId, rep);
        assertEquals(2, oldOrder.getLines().size());

        OrderLine selectedLine = oldOrder.getLines().get(0);

        // Bảng giá thay đổi sau khi tạo đơn cũ.
        sql("UPDATE price_list_items SET sale_price = 12000, floor_price = 10000 "
                + "WHERE price_list_id = " + priceListId
                + " AND product_id = " + COCA);

        OrderService.ReorderResult result = service.reorder(
                oldOrderId,
                new long[]{selectedLine.getId()},
                customer
        );

        Order newOrder = service.get(result.newOrderId(), customer);

        assertTrue(result.newOrderId() != oldOrderId);
        assertTrue(result.message().contains("bảng giá hiện hành"));
        assertEquals(900L, newOrder.getCustomerId());
        assertEquals(1, newOrder.getLines().size());

        OrderLine newLine = newOrder.getLines().get(0);
        assertEquals(COCA, newLine.getProductId());
        assertEquals(selectedLine.getQuantity(), newLine.getQuantity());
        assertEquals(selectedLine.getUnitId(), newLine.getUnitId());
        assertEquals(0, new BigDecimal("12000.00").compareTo(newLine.getUnitPrice()));

        assertEquals(2, count("orders", "customer_id = 900"));
    }

    @Test
    void customerReorderSkipsDiscontinuedProductAndNotifies() throws Exception {
        long oldOrderId = service.create(900, rep);
        service.addLine(oldOrderId, COCA, LON, "2", rep);

        // Mô phỏng một đơn cũ có dòng hàng đã ngừng kinh doanh.
        sql("INSERT INTO order_lines "
                + "(order_id, product_id, unit_id, quantity, factor, base_quantity) VALUES ("
                + oldOrderId + ", " + OLD + ", " + LON + ", 2, 1, 2)");

        Order oldOrder = service.get(oldOrderId, rep);
        long[] selectedIds = oldOrder.getLines().stream()
                .mapToLong(OrderLine::getId)
                .toArray();

        OrderService.ReorderResult result = service.reorder(
                oldOrderId,
                selectedIds,
                customer
        );

        Order newOrder = service.get(result.newOrderId(), customer);

        assertEquals(1, newOrder.getLines().size());
        assertEquals("COCA", newOrder.getLines().get(0).getSku());
        assertTrue(result.message().contains("OLD"));
        assertTrue(result.message().contains("đã ngừng kinh doanh"));
    }

    @Test
    void customerCannotReorderAnotherCustomersOrder() throws Exception {
        long oldOrderId = service.create(900, rep);
        service.addLine(oldOrderId, COCA, LON, "1", rep);

        Order oldOrder = service.get(oldOrderId, rep);
        long lineId = oldOrder.getLines().get(0).getId();

        assertThrows(
                OrderService.NotFoundException.class,
                () -> service.reorder(
                        oldOrderId,
                        new long[]{lineId},
                        otherCustomer
                )
        );

        assertEquals(1, count("orders", "customer_id = 900"));
    }

    @Test
    void nonCustomerCannotReorderOrder() throws Exception {
        long oldOrderId = service.create(900, rep);
        service.addLine(oldOrderId, COCA, LON, "1", rep);

        long lineId = service.get(oldOrderId, rep).getLines().get(0).getId();

        assertThrows(
                ForbiddenException.class,
                () -> service.reorder(
                        oldOrderId,
                        new long[]{lineId},
                        rep
                )
        );

        assertEquals(1, count("orders", "customer_id = 900"));
    }
}