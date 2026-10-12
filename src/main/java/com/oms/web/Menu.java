package com.oms.web;

import com.oms.model.Module;
import com.oms.model.User;
import com.oms.model.Role;
import com.oms.security.PermissionPolicy;

import java.util.ArrayList;
import java.util.List;

/** Dựng menu chỉ gồm các mục thuộc quyền của người dùng (S1-06), nhóm theo thiết kế giao diện. */
public final class Menu {

    /** icon là tên biểu tượng trong assets/css/icons.css (class "icon--" + icon), rỗng nếu chưa có thiết kế. */
    public record Item(String label, String url, String icon, boolean active, boolean comingSoon, boolean shortcut) {
        public String getLabel() { return label; }
        public String getUrl() { return url; }
        public String getIcon() { return icon; }
        public boolean isActive() { return active; }
        public boolean isComingSoon() { return comingSoon; }
        /** Hiện thêm ở thanh điều hướng nhanh phía dưới trên điện thoại. */
        public boolean isShortcut() { return shortcut; }
    }

    public record Group(String title, List<Item> items) {
        public String getTitle() { return title; }
        public List<Item> getItems() { return items; }
    }

    private Menu() {
    }

    public static List<Group> build(User user, PermissionPolicy policy, String path) {
        List<Group> groups = new ArrayList<>();
        if (PermissionPolicy.canTakeOrders(user.getRoles())
                || user.getRoles().contains(com.oms.model.Role.WH_MANAGER)) {
            groups.add(new Group("Tr\u1EA3 h\u00E0ng", List.of(item("Tr\u1EA3 h\u00E0ng t\u1EEB \u0111\u1EA1i l\u00FD", "/sales/returns",
                    "clipboard-list", path.equals("/sales/returns"), false))));
        }
        groups.add(new Group("", List.of(item("Trang chủ", "/home", "house", path.equals("/home") || path.equals("/"), false))));

        if (user.getRoles().contains(Role.CUSTOMER)) {
            groups.add(new Group("Cổng đại lý", List.of(
                    item("Đặt hàng", "/portal", "package", path.equals("/portal"), true),
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
            groups.add(new Group("Sản phẩm & bảng giá", items));
        }

        boolean takesOrders = PermissionPolicy.canTakeOrders(user.getRoles()) && policy.canWrite(user.getRoles(), Module.ORDER);
        if (takesOrders) {
            boolean approvals = path.equals("/orders/approvals");
            groups.add(new Group("Bán hàng", List.of(
                    item("Đơn hàng", "/orders", "clipboard-list", path.startsWith("/orders") && !approvals, true),
                    item("Duyệt đơn", "/orders/approvals", "circle-check", approvals, false))));
        }

        if (PermissionPolicy.canTakeOrders(user.getRoles()) || PermissionPolicy.canManageCredit(user.getRoles())) {
            groups.add(new Group("Thu tiền theo tuyến", List.of(
                    item("Thu tiền & nộp công ty", "/finance/route-collections", "wallet", path.equals("/finance/route-collections"), false))));
        }

        if (policy.canRead(user.getRoles(), Module.CUSTOMER_CREDIT) && PermissionPolicy.canBrowseCustomers(user.getRoles())) {
            List<Item> customerItems = new ArrayList<>();
            customerItems.add(item("Đại lý", "/customers", "building-2", path.startsWith("/customers"), true));
            if (policy.canRead(user.getRoles(), Module.INVOICE) && PermissionPolicy.canManageCredit(user.getRoles())) {
                customerItems.add(item("Cảnh báo hạn mức", "/finance/credit-alerts", "shield-check", path.equals("/finance/credit-alerts"), false));
            }
            groups.add(new Group("Đại lý & hạn mức công nợ",
                    customerItems));
        }

        if (policy.canRead(user.getRoles(), Module.INVOICE) && PermissionPolicy.canManageCredit(user.getRoles())) {
            List<Item> financeItems = new ArrayList<>();
            financeItems.add(item("Phân tích tuổi nợ", "/finance/debt-aging", "history", path.equals("/finance/debt-aging"), false));
            if (user.getRoles().contains(Role.ACCOUNTANT) || user.getRoles().contains(Role.ADMIN)) {
                financeItems.add(item("Biên bản đối chiếu", "/finance/reconciliations", "clipboard-list", path.equals("/finance/reconciliations"), false));
                financeItems.add(item("Sổ chi tiết công nợ", "/finance/customer-ledger", "history", path.equals("/finance/customer-ledger"), false));
            }
            groups.add(new Group("Hoá đơn & công nợ", financeItems));
        }

        if (policy.canRead(user.getRoles(), Module.INVENTORY)) {
            groups.add(new Group("Tồn kho & nhập kho",
                    List.of(
                            item("Nhà cung cấp", "/suppliers", "truck", path.startsWith("/suppliers"), false),
                            item("Kho & vị trí lưu trữ", "/inventory/warehouses", "boxes", path.equals("/inventory/warehouses"), false),
                            item("Phiếu nhập kho", "/inventory/receipts", "clipboard-list", path.equals("/inventory/receipts"), false),
                            item("Tồn kho theo SKU", "/inventory/stock", "boxes", path.equals("/inventory/stock"), false),
                            item("Lô & hạn sử dụng", "/inventory/lots", "package", path.equals("/inventory/lots"), false),
                            item("Thẻ kho sản phẩm", "/inventory/stock-card", "history", path.equals("/inventory/stock-card"), false),
                            item("Chuyển kho nội bộ", "/inventory/transfers", "truck", path.equals("/inventory/transfers"), false))));
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
            groups.add(new Group("Quản trị", admin));
        }

        // Các module chưa làm: chưa có thiết kế nên giữ dạng "Sắp có" như trước
        List<Item> upcoming = new ArrayList<>();
        for (Module m : Module.values()) {
            if (m.getUrl() == null && policy.canRead(user.getRoles(), m) && !(m == Module.ORDER && takesOrders)) {
                upcoming.add(new Item(m.getLabel(), null, "", false, true, false));
            }
        }
        if (!upcoming.isEmpty()) {
            groups.add(new Group("Sắp có", upcoming));
        }
        return groups;
    }

    private static Item item(String label, String url, String icon, boolean active, boolean shortcut) {
        return new Item(label, url, icon, active, false, shortcut);
    }
}
