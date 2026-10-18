package com.oms.web;

import com.oms.model.Module;
import com.oms.model.User;
import com.oms.model.Role;
import com.oms.security.PermissionPolicy;

import java.util.ArrayList;
import java.util.List;

/** Dựng menu chỉ gồm các mục thuộc quyền của người dùng (S1-06), nhóm theo thiết kế giao diện. */
public final class Menu {

    /** icon là tên biểu tượng trong assets/css/icons.css (class "icon--" + icon). */
    public record Item(String label, String url, String icon, boolean active, boolean shortcut) {
        public String getLabel() { return label; }
        public String getUrl() { return url; }
        public String getIcon() { return icon; }
        public boolean isActive() { return active; }
        /** Hiện thêm ở thanh điều hướng nhanh phía dưới trên điện thoại. */
        public boolean isShortcut() { return shortcut; }
    }

    /** Vùng lớn của menu trái (Chung / Nghiệp vụ / Quản trị); tiêu đề vùng in ra ở nhóm đầu tiên của mỗi vùng. */
    static final String GENERAL = "Chung";
    static final String BUSINESS = "Nghiệp vụ";
    static final String ADMIN = "Quản trị";

    public record Group(String section, String title, List<Item> items) {
        public String getSection() { return section; }
        public String getTitle() { return title; }
        public List<Item> getItems() { return items; }
    }

    private Menu() {
    }

    public static List<Group> build(User user, PermissionPolicy policy, String path) {
        List<Group> groups = new ArrayList<>();
        groups.add(new Group(GENERAL, "", List.of(item("Trang chủ", "/home", "house", path.equals("/home") || path.equals("/"), false))));

        if (user.getRoles().contains(Role.SALES_MANAGER)
                || user.getRoles().contains(Role.ADMIN)) {
            groups.add(new Group(GENERAL, "Điều hành", List.of(
                    item("Dashboard bán hàng & kho",
                            "/reports/management-dashboard",
                            "boxes",
                            path.equals("/reports/management-dashboard"),
                            false),
                    item("Báo cáo doanh số",
                            "/reports/sales",
                            "clipboard-list",
                            path.equals("/reports/sales"),
                            false))));
        }

        if (user.getRoles().contains(Role.CUSTOMER)) {
            groups.add(new Group(BUSINESS, "Cổng đại lý", List.of(
                    item("Đặt hàng", "/portal", "package", path.equals("/portal"), true),
                    item("Đơn hàng của tôi", "/portal/orders", "clipboard-list", path.startsWith("/portal/orders"), true),
                    item("Công nợ của tôi", "/portal/my-debt", "wallet", path.equals("/portal/my-debt"), false),
                    item("Đối chiếu công nợ", "/portal/reconciliations", "clipboard-list", path.equals("/portal/reconciliations"), false))));
        }

        if (policy.canRead(user.getRoles(), Module.PRODUCT)) {
            boolean categories = path.startsWith("/products/categories");
            boolean prices = path.startsWith("/products/price-lists");
            boolean discounts = path.startsWith("/products/discounts");
            List<Item> items = new ArrayList<>();
            items.add(item("Sản phẩm", "/products", "package", path.startsWith("/products") && !categories && !prices && !discounts, true));
            items.add(item("Nhóm hàng", "/products/categories", "layers", categories, false));
            // Đại lý (chỉ xem dữ liệu của mình) chưa có cổng riêng nên không thấy bảng giá của mọi nhóm
            if (!policy.isOwnScopeOnly(user.getRoles(), Module.PRODUCT)) {
                items.add(item("Bảng giá", "/products/price-lists", "tags", prices, true));
            }
            if (PermissionPolicy.canManageDiscounts(user.getRoles())) {
                items.add(item("Chiết khấu", "/products/discounts", "percent", discounts, false));
            }
            groups.add(new Group(BUSINESS, "Sản phẩm & bảng giá", items));
        }

        boolean takesOrders = PermissionPolicy.canTakeOrders(user.getRoles()) && policy.canWrite(user.getRoles(), Module.ORDER);
        List<Item> salesItems = new ArrayList<>();
        if (takesOrders) {
            boolean approvals = path.equals("/orders/approvals");
            salesItems.add(item("Đơn hàng", "/orders", "clipboard-list", path.startsWith("/orders") && !approvals, true));
            salesItems.add(item("Duyệt đơn", "/orders/approvals", "circle-check", approvals, false));
        }
        if (PermissionPolicy.canTakeOrders(user.getRoles()) || user.getRoles().contains(Role.WH_MANAGER)) {
            salesItems.add(item("Trả hàng từ đại lý", "/sales/returns", "clipboard-list", path.equals("/sales/returns"), false));
        }
        if (!salesItems.isEmpty()) {
            groups.add(new Group(BUSINESS, "Bán hàng", salesItems));
        }

        if (PermissionPolicy.canTakeOrders(user.getRoles()) || PermissionPolicy.canManageCredit(user.getRoles())) {
            groups.add(new Group(BUSINESS, "Thu tiền theo tuyến", List.of(
                    item("Thu tiền & nộp công ty", "/finance/route-collections", "wallet", path.equals("/finance/route-collections"), false))));
        }

        if (policy.canRead(user.getRoles(), Module.CUSTOMER_CREDIT) && PermissionPolicy.canBrowseCustomers(user.getRoles())) {
            List<Item> customerItems = new ArrayList<>();
            customerItems.add(item("Đại lý", "/customers", "building-2", path.startsWith("/customers"), true));
            if (policy.canRead(user.getRoles(), Module.INVOICE) && PermissionPolicy.canManageCredit(user.getRoles())) {
                customerItems.add(item("Cảnh báo hạn mức", "/finance/credit-alerts", "shield-check", path.equals("/finance/credit-alerts"), false));
            }
            groups.add(new Group(BUSINESS, "Đại lý & hạn mức công nợ",
                    customerItems));
        }

        List<Item> financeItems = new ArrayList<>();
        boolean accountant = user.getRoles().contains(Role.ACCOUNTANT) || user.getRoles().contains(Role.ADMIN);
        if (policy.canRead(user.getRoles(), Module.INVOICE) && PermissionPolicy.canManageCredit(user.getRoles())) {
            financeItems.add(item("Phân tích tuổi nợ", "/finance/debt-aging", "history", path.equals("/finance/debt-aging"), false));
            if (accountant) {
                financeItems.add(item("Biên bản đối chiếu", "/finance/reconciliations", "clipboard-list", path.equals("/finance/reconciliations"), false));
                financeItems.add(item("Sổ chi tiết công nợ", "/finance/debt-ledger", "history", path.equals("/finance/debt-ledger") || path.equals("/finance/customer-ledger"), false));
            }
        }
        if (accountant) {
            financeItems.add(0, item("Hoá đơn", "/finance/invoices", "clipboard-list", path.startsWith("/finance/invoices"), false));
            financeItems.add(1, item("Ghi nhận thanh toán", "/finance/payments", "wallet", path.startsWith("/finance/payments"), false));
            financeItems.add(item("Giảm trừ công nợ", "/finance/return-credits", "clipboard-list", path.equals("/finance/return-credits"), false));
        }
        if (!financeItems.isEmpty()) {
            groups.add(new Group(BUSINESS, "Hoá đơn & công nợ", financeItems));
        }

        if (policy.canRead(user.getRoles(), Module.INVENTORY)) {
            groups.add(new Group(BUSINESS, "Tồn kho & nhập kho",
                    List.of(
                            item("Nhà cung cấp", "/suppliers", "truck", path.startsWith("/suppliers"), false),
                            item("Kho & vị trí lưu trữ", "/inventory/warehouses", "boxes", path.equals("/inventory/warehouses"), false),
                            item("Phiếu nhập kho", "/inventory/receipts", "clipboard-list", path.equals("/inventory/receipts"), false),
                            item("Tồn kho theo SKU", "/inventory/stock", "boxes", path.equals("/inventory/stock"), false),
                            item("Lô & hạn sử dụng", "/inventory/lots", "package", path.equals("/inventory/lots"), false),
                            item("Thẻ kho sản phẩm", "/inventory/stock-card", "history", path.equals("/inventory/stock-card"), false),
                            item("Kiểm kê kho", "/inventory/counts", "clipboard-list", path.equals("/inventory/counts"), false),
                            item("Điều chỉnh tồn kho", "/inventory/adjustments", "clipboard-list", path.startsWith("/inventory/adjustments"), false),
                            item("Chuyển kho nội bộ", "/inventory/transfers", "truck", path.equals("/inventory/transfers"), false))));
        }

        if (policy.canWrite(user.getRoles(), Module.SHIPMENT)
                && (user.isAdmin() || user.hasRole(Role.WAREHOUSE) || user.hasRole(Role.WH_MANAGER))) {
            groups.add(new Group(BUSINESS, "Xuất kho & giao hàng", List.of(
                    item("Phiếu soạn hàng", "/shipments/picking", "package", path.startsWith("/shipments/picking"), true),
                    item("Phân tuyến giao hàng", "/shipments/trips", "truck", path.startsWith("/shipments/trips"), false),
                    item("Hàng đang giao", "/shipments/in-transit", "history", path.equals("/shipments/in-transit"), false),
                    item("Xuất kho", "/shipments/export", "truck", path.startsWith("/shipments/export"), false))));
        }

        List<Item> admin = new ArrayList<>();
        if (policy.canRead(user.getRoles(), Module.USER_AUDIT)) {
            admin.add(item("Người dùng", "/admin/users", "users", path.startsWith("/admin/users"), true));
        }
        if (PermissionPolicy.canManagePermissions(user.getRoles())) {
            admin.add(item("Vai trò & quyền", "/admin/roles", "shield-check", path.equals("/admin/roles"), false));
        }
        if (policy.canRead(user.getRoles(), Module.USER_AUDIT)) {
            admin.add(item("Nhật ký thao tác", "/admin/audit-logs", "history", path.equals("/admin/audit-logs"), false));
        }
        if (!admin.isEmpty()) {
            groups.add(new Group(ADMIN, "Quản trị", admin));
        }

        return groups;
    }

    private static Item item(String label, String url, String icon, boolean active, boolean shortcut) {
        return new Item(label, url, icon, active, shortcut);
    }
}
