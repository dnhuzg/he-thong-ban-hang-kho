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
            Map.entry("/logout", new Rule(Kind.AUTHENTICATED, null)),
            Map.entry("/session/ping", new Rule(Kind.AUTHENTICATED, null)),
            Map.entry("/account/change-password", new Rule(Kind.AUTHENTICATED, null)),
            Map.entry("/account/profile", new Rule(Kind.AUTHENTICATED, null)),
            Map.entry("/account/avatar", new Rule(Kind.AUTHENTICATED, null)),
            Map.entry("/admin/users", new Rule(Kind.READ, Module.USER_AUDIT)),
            Map.entry("/admin/audit-logs", new Rule(Kind.READ, Module.USER_AUDIT)),
            Map.entry("/admin/users/new", new Rule(Kind.ADMIN_ONLY, null)),
            Map.entry("/admin/users/edit", new Rule(Kind.ADMIN_ONLY, null)),
            Map.entry("/admin/users/lock", new Rule(Kind.ADMIN_ONLY, null)),
            Map.entry("/admin/users/unlock", new Rule(Kind.ADMIN_ONLY, null)),
            Map.entry("/admin/roles", new Rule(Kind.ADMIN_ONLY, null)));

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
