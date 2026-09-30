package com.oms.web;

import com.oms.model.Module;
import com.oms.model.User;
import com.oms.security.PermissionPolicy;

import java.util.ArrayList;
import java.util.List;

/** Dựng menu chỉ gồm các mục thuộc quyền của người dùng (S1-06). */
public final class Menu {

    public record Item(String label, String url, boolean active, boolean comingSoon) {
        public String getLabel() { return label; }
        public String getUrl() { return url; }
        public boolean isActive() { return active; }
        public boolean isComingSoon() { return comingSoon; }
    }

    public record Group(String title, List<Item> items) {
        public String getTitle() { return title; }
        public List<Item> getItems() { return items; }
    }

    private Menu() {
    }

    public static List<Group> build(User user, PermissionPolicy policy, String path) {
        List<Group> groups = new ArrayList<>();
        groups.add(new Group("Chung", List.of(new Item("Trang chủ", "/home", path.equals("/home") || path.equals("/"), false))));

        List<Item> business = new ArrayList<>();
        for (Module m : Module.values()) {
            if (m == Module.USER_AUDIT || !policy.canRead(user.getRoles(), m)) {
                continue;
            }
            business.add(new Item(m.getLabel(), m.getUrl(), m.getUrl() != null && path.startsWith(m.getUrl()), m.getUrl() == null));
        }
        if (!business.isEmpty()) {
            groups.add(new Group("Nghiệp vụ", business));
        }

        List<Item> admin = new ArrayList<>();
        if (policy.canRead(user.getRoles(), Module.USER_AUDIT)) {
            admin.add(new Item("Người dùng", "/admin/users", path.startsWith("/admin/users"), false));
            admin.add(new Item("Nhật ký thao tác", "/admin/audit-logs", path.equals("/admin/audit-logs"), false));
        }
        if (PermissionPolicy.canManagePermissions(user.getRoles())) {
            admin.add(new Item("Vai trò & quyền", "/admin/roles", path.equals("/admin/roles"), false));
        }
        if (!admin.isEmpty()) {
            groups.add(new Group("Quản trị", admin));
        }
        return groups;
    }
}
