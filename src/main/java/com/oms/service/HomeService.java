package com.oms.service;

import com.oms.config.Db;
import com.oms.dao.CustomerDao;
import com.oms.dao.OrderDao;
import com.oms.dao.OrderStockDao;
import com.oms.dao.PortalAccountDao;
import com.oms.model.AuditEntry;
import com.oms.model.CreditChange;
import com.oms.model.Customer;
import com.oms.model.CustomerStats;
import com.oms.model.NamedItem;
import com.oms.model.Order;
import com.oms.model.OrderStats;
import com.oms.model.PageResult;
import com.oms.model.Role;
import com.oms.model.User;
import com.oms.security.PermissionPolicy;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Số liệu trang chủ theo vai trò (trừ Admin, vẫn dùng trang tổng quan tài khoản). Mọi con số tính trong phạm vi
 * người xem được thấy và không đọc giá vốn.
 */
public class HomeService {

    private static final int BLOCK_SIZE = 5;

    /** Getter kiểu bean để JSP đọc được (EL của Tomcat 10.1 chưa hỗ trợ accessor của record). */
    public record Card(String key, String label, long count, BigDecimal money, String unit, String note, String url,
                       String icon, String tone) {
        public String getKey() { return key; }
        public String getLabel() { return label; }
        public long getCount() { return count; }
        public BigDecimal getMoney() { return money; }
        public String getUnit() { return unit; }
        public String getNote() { return note; }
        public String getUrl() { return url; }
        public String getIcon() { return icon; }
        public String getTone() { return tone; }
    }

    public static class Dashboard {
        // Người nhiều vai trò có thể gặp cùng một thẻ ở hai vai trò: giữ thẻ đầu tiên, không lặp
        private final Map<String, Card> cards = new LinkedHashMap<>();
        private List<OrderWorkflowService.Pending> pending = List.of();
        private String pendingTitle;
        private List<CustomerDao.HandoverItem> handover = List.of();
        private boolean canAssign;
        private List<Customer> lockedCustomers = List.of();
        private long lockedTotal;
        private List<CreditChange> creditChanges = List.of();
        private List<Order> pickQueue = List.of();
        private List<Order> myOrders = List.of();
        private List<AuditEntry> myActivity = List.of();
        private boolean noWarehouse;
        private boolean portalUnlinked;

        void add(Card card) {
            cards.putIfAbsent(card.key(), card);
        }

        public List<Card> getCards() { return new ArrayList<>(cards.values()); }
        public List<OrderWorkflowService.Pending> getPending() { return pending; }
        public String getPendingTitle() { return pendingTitle; }
        public List<CustomerDao.HandoverItem> getHandover() { return handover; }
        public boolean isCanAssign() { return canAssign; }
        public List<Customer> getLockedCustomers() { return lockedCustomers; }
        public long getLockedTotal() { return lockedTotal; }
        public List<CreditChange> getCreditChanges() { return creditChanges; }
        public List<Order> getPickQueue() { return pickQueue; }
        public List<Order> getMyOrders() { return myOrders; }
        public List<AuditEntry> getMyActivity() { return myActivity; }
        public boolean isNoWarehouse() { return noWarehouse; }
        public boolean isPortalUnlinked() { return portalUnlinked; }
    }

    private final OrderDao orderDao = new OrderDao();
    private final OrderStockDao stockDao = new OrderStockDao();
    private final CustomerDao customerDao = new CustomerDao();
    private final PortalAccountDao portalDao = new PortalAccountDao();
    private final OrderService orderService = new OrderService();
    private final OrderWorkflowService workflowService = new OrderWorkflowService();
    private final OrderCreditService creditQuotes = new OrderCreditService();
    private final CustomerService customerService = new CustomerService();
    private final CreditService creditService = new CreditService();
    private final UserService userService = new UserService();
    private final AuditService auditService = new AuditService();

    public Dashboard build(User user, LocalDate today) throws SQLException {
        Dashboard d = new Dashboard();
        if (user.hasRole(Role.SALES_MANAGER)) {
            salesManager(d, user, today);
        }
        if (user.hasRole(Role.SALES_REP)) {
            salesRep(d, user, today);
        }
        if (user.hasRole(Role.ACCOUNTANT)) {
            accountant(d, user);
        }
        if (user.hasRole(Role.WAREHOUSE) || user.hasRole(Role.WH_MANAGER)) {
            warehouse(d, user);
        }
        if (user.hasRole(Role.CUSTOMER)) {
            customer(d, user);
        }
        d.myActivity = auditService.recentBy(user.getId(), BLOCK_SIZE);
        return d;
    }

    private void salesManager(Dashboard d, User user, LocalDate today) throws SQLException {
        d.pending = workflowService.pending(user);
        d.pendingTitle = "Đơn chờ duyệt";
        CustomerStats customers = customerService.stats(user);
        OrderStats orders = orderService.stats(user, today);
        int pending = d.pending.size();
        d.add(new Card("orders.pending", "Đơn chờ duyệt", pending, null, "đơn", null, "/orders/approvals",
                "circle-check", pending > 0 ? "warning" : "primary"));
        d.add(new Card("orders.thisMonth", "Đơn tháng này", orders.getThisMonth(), null, "đơn", null, "/orders",
                "clipboard-list", "primary"));
        // Chưa có trang lọc đại lý chưa phân công nên thẻ này không bấm được
        d.add(new Card("customers.unassigned", "Đại lý chưa phân công", customers.getUnassigned(), null, "đại lý",
                "Chưa có nhân viên kinh doanh phụ trách", null, "users", customers.getUnassigned() > 0 ? "warning" : ""));
        d.add(lockedCard(customers));
        d.handover = userService.customersNeedingHandover();
        d.canAssign = PermissionPolicy.canAssignCustomers(user.getRoles());
    }

    private void salesRep(Dashboard d, User user, LocalDate today) throws SQLException {
        CustomerStats customers = customerService.stats(user);
        OrderStats orders = orderService.stats(user, today);
        long drafts;
        try (Connection conn = Db.getConnection()) {
            drafts = orderDao.countDraftsCreatedBy(conn, user.getId());
        }
        boolean all = PermissionPolicy.canManageCustomers(user.getRoles());
        d.add(new Card("customers.total", all ? "Tổng số đại lý" : "Đại lý tôi phụ trách", customers.getTotal(), null,
                "đại lý", null, "/customers", "building-2", "primary"));
        d.add(new Card("orders.drafts", "Đơn nháp đang gõ dở", drafts, null, "đơn", "Do bạn tạo, chưa chốt", "/orders",
                "clipboard-list", ""));
        d.add(new Card("orders.thisMonth", "Đơn tháng này", orders.getThisMonth(), null, "đơn", null, "/orders",
                "clipboard-list", "primary"));
        d.add(new Card("orders.dueSoon", "Đơn giao trong 7 ngày", orders.getDueSoon(), null, "đơn", null, "/orders",
                "truck", "primary"));
        if (d.pendingTitle == null) {
            d.pending = workflowService.pending(user);
            d.pendingTitle = "Đơn đại lý gửi chờ bạn xác nhận";
        }
        PageResult<Customer> locked = customerService.search(null, null, null, null, Customer.Status.LOCKED, 1, user);
        d.lockedCustomers = locked.getItems().subList(0, Math.min(BLOCK_SIZE, locked.getItems().size()));
        d.lockedTotal = locked.getTotal();
    }

    private void accountant(Dashboard d, User user) throws SQLException {
        CustomerStats customers = customerService.stats(user);
        long overCredit;
        try (Connection conn = Db.getConnection()) {
            overCredit = orderDao.countPendingOverCredit(conn);
        }
        d.add(new Card("customers.creditLimit", "Tổng hạn mức cấp", 0, customers.getTotalCreditLimit(), null, null,
                "/customers", "wallet", "primary"));
        d.add(new Card("customers.withCredit", "Đại lý được nợ", customers.getWithCredit(), null, "đại lý",
                "Thanh toán ngay: " + customers.getPayNow(), "/customers", "building-2", ""));
        d.add(lockedCard(customers));
        // Kế toán không vào được trang duyệt đơn nên thẻ chỉ để biết, không bấm được
        d.add(new Card("orders.pendingCredit", "Đơn chờ duyệt vì vượt hạn mức", overCredit, null, "đơn", null, null,
                "history", overCredit > 0 ? "warning" : ""));
        d.creditChanges = creditService.recentChanges(user, BLOCK_SIZE);
    }

    private void warehouse(Dashboard d, User user) throws SQLException {
        List<Long> ids = new ArrayList<>();
        for (NamedItem w : user.getWarehouses()) {
            ids.add(w.getId());
        }
        if (ids.isEmpty()) {
            d.noWarehouse = true;
            return;
        }
        try (Connection conn = Db.getConnection()) {
            Map<Order.Status, Long> counts = orderDao.countByStatusInWarehouses(conn, ids);
            long outOfStock = stockDao.countUnavailable(conn, ids);
            d.add(new Card("wh.approved", "Đơn đã duyệt chờ soạn", counts.getOrDefault(Order.Status.APPROVED, 0L), null,
                    "đơn", null, null, "clipboard-list", "primary"));
            d.add(new Card("wh.picking", "Đang soạn", counts.getOrDefault(Order.Status.PICKING, 0L), null, "đơn", null,
                    null, "boxes", "primary"));
            d.add(new Card("wh.shipped", "Đã xuất chưa giao", counts.getOrDefault(Order.Status.SHIPPED, 0L), null, "đơn",
                    null, null, "truck", "primary"));
            d.add(new Card("wh.outOfStock", "Mặt hàng hết tồn khả dụng", outOfStock, null, "SKU",
                    "SKU đã nhập kho, khả dụng = 0", null, "package", outOfStock > 0 ? "danger" : ""));
            d.pickQueue = orderDao.pickQueue(conn, ids, BLOCK_SIZE);
        }
    }

    private void customer(Dashboard d, User user) throws SQLException {
        try (Connection conn = Db.getConnection()) {
            Long customerId = portalDao.customerId(conn, user.getId());
            Customer customer = customerId == null ? null : customerDao.findById(conn, customerId);
            if (customer == null) {
                d.portalUnlinked = true;
                return;
            }
            Map<Order.Status, Long> counts = orderDao.countByStatusForCustomer(conn, customerId);
            long inProgress = counts.getOrDefault(Order.Status.APPROVED, 0L) + counts.getOrDefault(Order.Status.PICKING, 0L)
                    + counts.getOrDefault(Order.Status.SHIPPED, 0L);
            long pending = counts.getOrDefault(Order.Status.PENDING_APPROVAL, 0L);
            OrderCreditService.Quote quote = creditQuotes.evaluate(conn, customer, 0, BigDecimal.ZERO);
            boolean payNow = customer.getCreditLimit().signum() == 0;
            d.add(new Card("portal.pending", "Đơn đang chờ duyệt", pending, null, "đơn", null, null, "history",
                    pending > 0 ? "warning" : ""));
            d.add(new Card("portal.inProgress", "Đơn đang giao", inProgress, null, "đơn", "Đã duyệt · đang soạn · đã xuất kho",
                    null, "truck", "primary"));
            d.add(new Card("portal.creditRemaining", "Hạn mức còn lại", 0, quote.remaining(), null,
                    payNow ? "Thanh toán ngay khi nhận hàng" : null, null, "wallet",
                    quote.remaining().signum() <= 0 ? "danger" : "primary"));
            d.add(new Card("portal.debt", "Dư nợ hiện tại", 0, quote.currentDebt(), null, null, null, "wallet", ""));
            d.myOrders = orderDao.recentForCustomer(conn, customerId, BLOCK_SIZE);
            for (Order o : d.myOrders) {
                o.setLines(orderDao.lines(conn, o.getId()));
                orderService.price(conn, o);
            }
        }
    }

    private static Card lockedCard(CustomerStats customers) {
        return new Card("customers.locked", "Đại lý đang khoá", customers.getLocked(), null, "đại lý",
                "Không tạo được đơn mới cho tới khi mở khoá", "/customers?status=LOCKED", "lock-keyhole", "");
    }
}
