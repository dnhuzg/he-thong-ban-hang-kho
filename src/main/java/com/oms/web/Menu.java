package com.oms.web;

import com.oms.model.Module;
import com.oms.model.User;
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
        groups.add(new Group("", List.of(item("Trang chủ", "/home", "house", path.equals("/home") || path.equals("/"), false))));

        if (policy.canRead(user.getRoles(), Module.PRODUCT)) {
            boolean categories = path.startsWith("/products/categories");
            boolean prices = path.startsWith("/products/price-lists");
            List<Item> items = new ArrayList<>();
            items.add(item("Sản phẩm", "/products", "package", path.startsWith("/products") && !categories && !prices, true));
            items.add(item("Nhóm hàng", "/products/categories", "layers", categories, false));
            // Đại lý (chỉ xem dữ liệu của mình) chưa có cổng riêng nên không thấy bảng giá của mọi nhóm
            if (!policy.isOwnScopeOnly(user.getRoles(), Module.PRODUCT)) {
                items.add(item("Bảng giá", "/products/price-lists", "tags", prices, true));
            }
            groups.add(new Group("Sản phẩm & bảng giá", items));
        }

        if (policy.canRead(user.getRoles(), Module.CUSTOMER_CREDIT) && PermissionPolicy.canBrowseCustomers(user.getRoles())) {
            groups.add(new Group("Đại lý & hạn mức công nợ",
                    List.of(item("Đại lý", "/customers", "building-2", path.startsWith("/customers"), true))));
        }

        if (policy.canRead(user.getRoles(), Module.INVENTORY)) {
            groups.add(new Group("Tồn kho & nhập kho",
                    List.of(item("Nhà cung cấp", "/suppliers", "truck", path.startsWith("/suppliers"), false))));
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
            if (m.getUrl() == null && policy.canRead(user.getRoles(), m)) {
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
