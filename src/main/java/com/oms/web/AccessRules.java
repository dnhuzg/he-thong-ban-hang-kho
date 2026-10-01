package com.oms.web;

import com.oms.model.Module;
import com.oms.model.User;
import com.oms.security.PermissionPolicy;

import java.util.Map;

/**
 * Bảng quyền theo URL. Đường dẫn không có trong bảng bị từ chối (trả 404) nên servlet mới
 * bắt buộc phải khai báo ở đây mới truy cập được — mặc định từ chối (S1-05).
 */
public final class AccessRules {

    public enum Kind { AUTHENTICATED, READ, WRITE, ADMIN_ONLY }

    public record Rule(Kind kind, Module module) {
    }

    private static final Map<String, Rule> RULES = Map.ofEntries(
            Map.entry("/", new Rule(Kind.AUTHENTICATED, null)),
            Map.entry("/home", new Rule(Kind.AUTHENTICATED, null)),
            Map.entry("/api/orders/stock", new Rule(Kind.AUTHENTICATED, null)),

            Map.entry("/api/orders/credit", new Rule(Kind.AUTHENTICATED, null)),

            Map.entry("/logout", new Rule(Kind.AUTHENTICATED, null)),
            Map.entry("/session/ping", new Rule(Kind.AUTHENTICATED, null)),
            Map.entry("/account/change-password", new Rule(Kind.AUTHENTICATED, null)),
            Map.entry("/account/profile", new Rule(Kind.AUTHENTICATED, null)),
            Map.entry("/account/avatar", new Rule(Kind.AUTHENTICATED, null)),
            Map.entry("/admin/users", new Rule(Kind.READ, Module.USER_AUDIT)),
            Map.entry("/admin/audit-logs", new Rule(Kind.READ, Module.USER_AUDIT)),
            Map.entry("/admin/users/new", new Rule(Kind.ADMIN_ONLY, null)),
            Map.entry("/admin/users/import", new Rule(Kind.ADMIN_ONLY, null)),
            Map.entry("/admin/users/import/apply", new Rule(Kind.ADMIN_ONLY, null)),
            Map.entry("/admin/users/import/template", new Rule(Kind.ADMIN_ONLY, null)),
            Map.entry("/admin/users/edit", new Rule(Kind.ADMIN_ONLY, null)),
            Map.entry("/admin/users/lock", new Rule(Kind.ADMIN_ONLY, null)),
            Map.entry("/admin/users/unlock", new Rule(Kind.ADMIN_ONLY, null)),
            Map.entry("/admin/roles", new Rule(Kind.ADMIN_ONLY, null)),
            Map.entry("/products", new Rule(Kind.READ, Module.PRODUCT)),
            Map.entry("/products/new", new Rule(Kind.WRITE, Module.PRODUCT)),
            Map.entry("/products/edit", new Rule(Kind.WRITE, Module.PRODUCT)),
            Map.entry("/products/delete", new Rule(Kind.WRITE, Module.PRODUCT)),
            Map.entry("/products/categories", new Rule(Kind.READ, Module.PRODUCT)),
            Map.entry("/products/categories/save", new Rule(Kind.WRITE, Module.PRODUCT)),
            Map.entry("/products/categories/delete", new Rule(Kind.WRITE, Module.PRODUCT)),
            Map.entry("/products/categories/move", new Rule(Kind.WRITE, Module.PRODUCT)),
            Map.entry("/products/units/save", new Rule(Kind.WRITE, Module.PRODUCT)),
            Map.entry("/products/units/delete", new Rule(Kind.WRITE, Module.PRODUCT)),
            Map.entry("/suppliers", new Rule(Kind.READ, Module.INVENTORY)),
            Map.entry("/suppliers/new", new Rule(Kind.WRITE, Module.INVENTORY)),
            Map.entry("/suppliers/edit", new Rule(Kind.WRITE, Module.INVENTORY)),
            Map.entry("/suppliers/delete", new Rule(Kind.WRITE, Module.INVENTORY)),
            Map.entry("/orders", new Rule(Kind.READ, Module.ORDER)),
            Map.entry("/orders/new", new Rule(Kind.WRITE, Module.ORDER)),
            Map.entry("/orders/create", new Rule(Kind.WRITE, Module.ORDER)),
            Map.entry("/orders/edit", new Rule(Kind.WRITE, Module.ORDER)),
            Map.entry("/orders/save", new Rule(Kind.WRITE, Module.ORDER)),
            Map.entry("/orders/delete", new Rule(Kind.WRITE, Module.ORDER)),
            Map.entry("/orders/lines/add", new Rule(Kind.WRITE, Module.ORDER)),
            Map.entry("/orders/lines/save", new Rule(Kind.WRITE, Module.ORDER)),
            Map.entry("/orders/lines/delete", new Rule(Kind.WRITE, Module.ORDER)),
            Map.entry("/products/discounts", new Rule(Kind.READ, Module.PRODUCT)),
            Map.entry("/products/discounts/new", new Rule(Kind.WRITE, Module.PRODUCT)),
            Map.entry("/products/discounts/edit", new Rule(Kind.WRITE, Module.PRODUCT)),
            Map.entry("/products/discounts/delete", new Rule(Kind.WRITE, Module.PRODUCT)),
            Map.entry("/products/price-history", new Rule(Kind.READ, Module.PRODUCT)),
            Map.entry("/products/price-lists", new Rule(Kind.READ, Module.PRODUCT)),
            Map.entry("/products/price-lists/view", new Rule(Kind.READ, Module.PRODUCT)),
            Map.entry("/products/price-lists/new", new Rule(Kind.WRITE, Module.PRODUCT)),
            Map.entry("/products/price-lists/header", new Rule(Kind.WRITE, Module.PRODUCT)),
            Map.entry("/products/price-lists/items/save", new Rule(Kind.WRITE, Module.PRODUCT)),
            Map.entry("/products/price-lists/items/delete", new Rule(Kind.WRITE, Module.PRODUCT)),
            Map.entry("/products/price-lists/new-version", new Rule(Kind.WRITE, Module.PRODUCT)),
            Map.entry("/products/import", new Rule(Kind.WRITE, Module.PRODUCT)),
            Map.entry("/products/import/apply", new Rule(Kind.WRITE, Module.PRODUCT)),
            Map.entry("/products/import/template", new Rule(Kind.WRITE, Module.PRODUCT)),
            Map.entry("/customers", new Rule(Kind.READ, Module.CUSTOMER_CREDIT)),
            Map.entry("/customers/view", new Rule(Kind.READ, Module.CUSTOMER_CREDIT)),
            Map.entry("/customers/new", new Rule(Kind.WRITE, Module.CUSTOMER_CREDIT)),
            Map.entry("/customers/edit", new Rule(Kind.WRITE, Module.CUSTOMER_CREDIT)),
            Map.entry("/customers/delete", new Rule(Kind.WRITE, Module.CUSTOMER_CREDIT)),
            Map.entry("/customers/delivery-points/save", new Rule(Kind.WRITE, Module.CUSTOMER_CREDIT)),
            Map.entry("/customers/delivery-points/default", new Rule(Kind.WRITE, Module.CUSTOMER_CREDIT)),
            Map.entry("/customers/delivery-points/delete", new Rule(Kind.WRITE, Module.CUSTOMER_CREDIT)),
            Map.entry("/customers/credit/save", new Rule(Kind.WRITE, Module.CUSTOMER_CREDIT)),
            Map.entry("/customers/transfer", new Rule(Kind.WRITE, Module.CUSTOMER_CREDIT)),
            Map.entry("/customers/transfer/apply", new Rule(Kind.WRITE, Module.CUSTOMER_CREDIT)),
            Map.entry("/customers/lock", new Rule(Kind.WRITE, Module.CUSTOMER_CREDIT)),
            Map.entry("/customers/unlock", new Rule(Kind.WRITE, Module.CUSTOMER_CREDIT)));

    // Ảnh tải lên (S2-03): mọi người đã đăng nhập đều xem được; FileServlet tự lọc tên tệp hợp lệ
    private static final Rule FILES = new Rule(Kind.AUTHENTICATED, null);

    private AccessRules() {
    }

    public static Rule find(String path) {
        Rule rule = RULES.get(path);
        if (rule == null && path.startsWith("/files/")) {
            return FILES;
        }
        return rule;
    }

    public static boolean isAllowed(Rule rule, User user, PermissionPolicy policy) {
        return switch (rule.kind()) {
            case AUTHENTICATED -> true;
            case READ -> policy.canRead(user.getRoles(), rule.module());
            case WRITE -> policy.canWrite(user.getRoles(), rule.module());
            case ADMIN_ONLY -> PermissionPolicy.canManagePermissions(user.getRoles());
        };
    }

    /** Các trang không cần đăng nhập. */
    public static boolean isPublic(String path) {
        return path.equals("/login") || path.equals("/forgot-password") || path.equals("/reset-password")
                || path.startsWith("/assets/") || path.equals("/favicon.ico");
    }
}
