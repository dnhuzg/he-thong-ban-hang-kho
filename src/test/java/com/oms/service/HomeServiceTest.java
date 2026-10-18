package com.oms.service;

import com.oms.model.AuditEntry;
import com.oms.model.Order;
import com.oms.model.Role;
import com.oms.model.User;
import com.oms.support.DatabaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HomeServiceTest extends DatabaseTest {

    private static final LocalDate TODAY = LocalDate.now();

    private final HomeService home = new HomeService();
    private User rep, rep2, manager;

    @BeforeEach
    void setup() throws Exception {
        rep = user("rep", Role.SALES_REP);
        rep2 = user("rep2", Role.SALES_REP);
        manager = user("manager", Role.SALES_MANAGER);
        sql("INSERT INTO customers(id,code,name,group_id,sales_rep_id,credit_limit) VALUES"
                + "(900,'DL900','Đại lý của rep',1," + rep.getId() + ",1000000),"
                + "(901,'DL901','Đại lý của rep2',1," + rep2.getId() + ",0)");
        sql("INSERT INTO customers(id,code,name,group_id,sales_rep_id,status,lock_reason) VALUES"
                + "(902,'DL902','Rep bị khoá',1," + rep.getId() + ",'LOCKED','Nợ quá hạn'),"
                + "(903,'DL903','Rep2 bị khoá',1," + rep2.getId() + ",'LOCKED','Ngừng hợp tác')");
        sql("INSERT INTO customers(id,code,name,group_id) VALUES(904,'DL904','Chưa phân công',1)");
    }

    private void order(String code, long customerId, String status, User createdBy) throws Exception {
        order(code, customerId, status, createdBy, null, null, null, false);
    }

    private void order(String code, long customerId, String status, User createdBy, Long warehouseId, String deliveryDate,
                       String reason, boolean customerSubmitted) throws Exception {
        sql("INSERT INTO orders(code,customer_id,status,created_by,warehouse_id,delivery_date,approval_reason,customer_submitted) VALUES('"
                + code + "'," + customerId + ",'" + status + "'," + createdBy.getId() + "," + warehouseId + ","
                + (deliveryDate == null ? "NULL" : "'" + deliveryDate + "'") + ","
                + (reason == null ? "NULL" : "'" + reason + "'") + "," + customerSubmitted + ")");
    }

    private static HomeService.Card card(HomeService.Dashboard d, String key) {
        return d.getCards().stream().filter(c -> c.key().equals(key)).findFirst().orElse(null);
    }

    @Test
    void salesRepSeesOnlyOwnDraftsCustomersAndPortalOrders() throws Exception {
        order("N1", 900, "DRAFT", rep);
        order("N2", 901, "DRAFT", rep2);
        order("N3", 900, "DRAFT", manager);
        order("P1", 900, "PENDING_APPROVAL", rep, null, null, "Đơn đại lý cần nhân viên phụ trách xác nhận", true);
        order("P2", 901, "PENDING_APPROVAL", rep2, null, null, "Đơn đại lý cần nhân viên phụ trách xác nhận", true);

        HomeService.Dashboard d = home.build(rep, TODAY);

        assertEquals(1, card(d, "orders.drafts").count());
        assertEquals("Đại lý tôi phụ trách", card(d, "customers.total").label());
        assertEquals(2, card(d, "customers.total").count());
        assertEquals("Đơn đại lý gửi chờ bạn xác nhận", d.getPendingTitle());
        assertEquals(List.of("P1"), d.getPending().stream().map(OrderWorkflowService.Pending::code).toList());
        assertEquals(1, d.getLockedTotal());
        assertEquals("DL902", d.getLockedCustomers().get(0).getCode());
        assertNull(card(d, "orders.pendingCredit"));
    }

    @Test
    void salesManagerSeesAllPendingUnassignedAndLocked() throws Exception {
        order("P1", 900, "PENDING_APPROVAL", rep, null, null, "Vượt hạn mức công nợ: 5 ₫", false);
        order("P2", 901, "PENDING_APPROVAL", rep2, null, null, "SKU A dưới giá sàn: 5 ₫", false);
        sql("UPDATE users SET status='LOCKED' WHERE id=" + rep2.getId());

        HomeService.Dashboard d = home.build(manager, TODAY);

        assertEquals("Đơn chờ duyệt", d.getPendingTitle());
        assertEquals(2, d.getPending().size());
        HomeService.Card pending = card(d, "orders.pending");
        assertEquals(2, pending.count());
        assertEquals("warning", pending.tone());
        assertEquals("/orders/approvals", pending.url());
        HomeService.Card unassigned = card(d, "customers.unassigned");
        assertEquals(1, unassigned.count());
        assertNull(unassigned.url());
        assertEquals(2, card(d, "customers.locked").count());
        assertEquals(2, d.getHandover().size());
        assertTrue(d.isCanAssign());
        assertTrue(d.getLockedCustomers().isEmpty());
    }

    @Test
    void accountantCountsOverCreditOnlyAndSeesLatestCreditChanges() throws Exception {
        User accountant = user("ketoan", Role.ACCOUNTANT);
        order("P1", 900, "PENDING_APPROVAL", rep, null, null, "Vượt hạn mức công nợ: 5 ₫", false);
        order("P2", 901, "PENDING_APPROVAL", rep2, null, null, "SKU A dưới giá sàn: 5 ₫", false);
        for (int i = 1; i <= 6; i++) {
            sql("INSERT INTO customer_credit_history(customer_id,old_limit,new_limit,old_days,new_days,reason,changed_by,changed_at) "
                    + "VALUES(900," + (i - 1) + "000," + i + "000,0,30,'Lần " + i + "'," + accountant.getId()
                    + ",TIMESTAMP '2026-10-0" + i + " 08:00:00')");
        }

        HomeService.Dashboard d = home.build(accountant, TODAY);

        HomeService.Card overCredit = card(d, "orders.pendingCredit");
        assertEquals(1, overCredit.count());
        assertNull(overCredit.url());
        assertEquals(0, new BigDecimal("1000000").compareTo(card(d, "customers.creditLimit").money()));
        assertEquals("Thanh toán ngay: 4", card(d, "customers.withCredit").note());
        assertEquals(5, d.getCreditChanges().size());
        assertEquals("Lần 6", d.getCreditChanges().get(0).getReason());
        assertEquals("Đại lý của rep", d.getCreditChanges().get(0).getCustomerName());
        assertEquals("06/10/2026 08:00", d.getCreditChanges().get(0).getChangedAtText());
        assertNull(d.getPendingTitle());
        assertThrows(ForbiddenException.class, () -> new CreditService().recentChanges(rep, 5));
    }

    @Test
    void warehouseSeesOnlyOwnWarehousesAndPickQueueByDeliveryDate() throws Exception {
        User keeper = user("kho", Role.WAREHOUSE);
        sql("INSERT INTO user_warehouses VALUES(" + keeper.getId() + ",10)");
        keeper = reload(keeper.getId());
        order("A1", 900, "APPROVED", rep, 10L, "2026-12-20", null, false);
        order("A2", 900, "APPROVED", rep, 10L, null, null, false);
        order("K1", 900, "PICKING", rep, 10L, "2026-12-15", null, false);
        order("S1", 900, "SHIPPED", rep, 10L, "2026-12-01", null, false);
        order("B1", 901, "APPROVED", rep2, 20L, "2026-11-01", null, false);
        sql("INSERT INTO products(id,sku,name,category_id,base_unit_id,status) VALUES"
                + "(500,'H0','Hết hàng',3,1,'ACTIVE'),(501,'NG','Ngừng bán',3,1,'INACTIVE'),"
                + "(502,'GC','Giữ chỗ hết',3,1,'ACTIVE'),(503,'CO','Còn hàng',3,1,'ACTIVE'),(504,'KB','Kho khác',3,1,'ACTIVE')");
        sql("INSERT INTO warehouse_inventory(warehouse_id,product_id,on_hand,reserved) VALUES(10,500,0,0),(10,501,0,0),(10,502,5,5),(10,503,10,2),(20,504,0,0)");

        HomeService.Dashboard d = home.build(keeper, TODAY);

        assertEquals(2, card(d, "wh.approved").count());
        assertEquals(1, card(d, "wh.picking").count());
        assertEquals(1, card(d, "wh.shipped").count());
        sql("UPDATE warehouse_inventory SET min_quantity = 10 WHERE product_id IN (503, 504)");   // 504 thuộc kho khác
        HomeService.Card low = card(home.build(keeper, TODAY), "wh.belowMinimum");
        assertEquals(1, low.count());
        assertEquals("warning", low.tone());
        HomeService.Card out = card(d, "wh.outOfStock");
        assertEquals(2, out.count());
        assertEquals("danger", out.tone());
        assertTrue(d.getCards().stream().allMatch(c -> c.url() == null));
        assertEquals(List.of("K1", "A1", "A2"), d.getPickQueue().stream().map(Order::getCode).toList());
        assertFalse(d.isNoWarehouse());

        User unassigned = user("kho2", Role.WH_MANAGER);
        HomeService.Dashboard none = home.build(unassigned, TODAY);
        assertTrue(none.isNoWarehouse());
        assertTrue(none.getCards().isEmpty());
        assertTrue(none.getPickQueue().isEmpty());
    }

    @Test
    void customerSeesOwnOrdersCreditAndDebt() throws Exception {
        User portal = user("daily", Role.CUSTOMER);
        sql("INSERT INTO customer_accounts VALUES(900," + portal.getId() + ")");
        order("D1", 900, "DRAFT", portal);
        order("P1", 900, "PENDING_APPROVAL", portal);
        order("A1", 900, "APPROVED", portal);
        order("S1", 900, "SHIPPED", portal);
        order("G1", 900, "DELIVERED", portal);
        order("X1", 901, "APPROVED", rep2);
        sql("INSERT INTO customer_receivables(customer_id,document_code,issued_on,outstanding) VALUES(900,'HD1',CURRENT_DATE,200000)");
        sql("INSERT INTO order_credit_reservations VALUES((SELECT id FROM orders WHERE code='A1'),900,300000)");

        HomeService.Dashboard d = home.build(portal, TODAY);

        assertEquals(1, card(d, "portal.pending").count());
        assertEquals(2, card(d, "portal.inProgress").count());
        HomeService.Card remaining = card(d, "portal.creditRemaining");
        assertEquals(0, new BigDecimal("500000").compareTo(remaining.money()));
        assertEquals("primary", remaining.tone());
        assertNull(remaining.note());
        assertEquals(0, new BigDecimal("200000").compareTo(card(d, "portal.debt").money()));
        assertEquals(4, d.getMyOrders().size());
        assertTrue(d.getMyOrders().stream().noneMatch(o -> o.isDraft() || o.getCustomerId() != 900));
        assertTrue(d.getCards().stream().allMatch(c -> c.url() == null));

        User unlinked = user("daily2", Role.CUSTOMER);
        HomeService.Dashboard none = home.build(unlinked, TODAY);
        assertTrue(none.isPortalUnlinked());
        assertTrue(none.getCards().isEmpty());
    }

    @Test
    void multipleRolesDoNotRepeatCards() throws Exception {
        User boss = user("boss", Role.SALES_MANAGER, Role.SALES_REP, Role.ACCOUNTANT);

        HomeService.Dashboard d = home.build(boss, TODAY);

        List<String> keys = d.getCards().stream().map(HomeService.Card::key).toList();
        assertEquals(keys.size(), new HashSet<>(keys).size());
        assertEquals("orders.pending", keys.get(0));
        assertTrue(keys.containsAll(List.of("orders.drafts", "customers.creditLimit", "orders.pendingCredit")));
        assertEquals("Tổng số đại lý", card(d, "customers.total").label());
        assertEquals("Đơn chờ duyệt", d.getPendingTitle());
    }

    @Test
    void activityShowsOnlyOwnLatestFiveWithoutLogin() throws Exception {
        for (int i = 1; i <= 6; i++) {
            sql("INSERT INTO audit_logs(actor_id,action,target_type,target_id,created_at) VALUES(" + rep.getId()
                    + ",'ORDER_SUBMIT','ORDER'," + i + ",TIMESTAMP '2026-10-0" + i + " 08:00:00')");
        }
        sql("INSERT INTO audit_logs(actor_id,action,created_at) VALUES(" + rep.getId() + ",'LOGIN',TIMESTAMP '2026-10-09 08:00:00')");
        sql("INSERT INTO audit_logs(actor_id,action,target_type,created_at) VALUES(" + rep2.getId()
                + ",'ORDER_CREATE','ORDER',TIMESTAMP '2026-10-09 09:00:00')");

        List<AuditEntry> activity = home.build(rep, TODAY).getMyActivity();

        assertEquals(5, activity.size());
        assertTrue(activity.stream().allMatch(e -> "ORDER_SUBMIT".equals(e.getAction())));
        assertEquals(6L, activity.get(0).getTargetId());
        assertEquals("Chốt đơn", activity.get(0).getActionLabel());
    }

    @Test
    void newAuditLabelsAreVietnamese() {
        AuditEntry e = new AuditEntry();
        e.setAction("SHIPMENT_FAILED_RETURNED");
        e.setTargetType("SHIPMENT");
        assertEquals("Giao không thành công, trả về kho", e.getActionLabel());
        assertEquals("Phiếu giao hàng", e.getTargetLabel());
        e.setAction("ORDER_ADD_LAST_PURCHASE");
        assertEquals("Thêm hàng từ lần mua trước", e.getActionLabel());
    }
}
