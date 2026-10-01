package com.oms.web.filter;

import com.oms.model.User;
import com.oms.security.PermissionPolicy;
import com.oms.service.AuthService;
import com.oms.service.PermissionService;
import com.oms.web.AccessRules;
import com.oms.web.Menu;
import com.oms.web.SessionKeys;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Kiểm tra đăng nhập và quyền ở server cho mọi request.
 * Nạp lại User từ DB mỗi request để việc khoá tài khoản / đổi mật khẩu có hiệu lực ngay với phiên đang mở.
 */
public class AuthFilter implements Filter {

    private final AuthService authService;
    private final PermissionService permissionService;

    public AuthFilter() {
        this(new AuthService(), new PermissionService());
    }

    AuthFilter(AuthService authService, PermissionService permissionService) {
        this.authService = authService;
        this.permissionService = permissionService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String path = req.getRequestURI().substring(req.getContextPath().length());
        if (path.isEmpty()) {
            path = "/";
        }

        if (!path.startsWith("/assets/")) {
            resp.setHeader("Cache-Control", "no-store");
        }

        HttpSession session = req.getSession(false);
        Long userId = session == null ? null : (Long) session.getAttribute(SessionKeys.USER_ID);

        try {
            User user = userId == null ? null : authService.findActiveSessionUser(userId);
            Integer version = session == null ? null : (Integer) session.getAttribute(SessionKeys.SESSION_VERSION);
            boolean revoked = userId != null
                    && (user == null || user.getStatus() == User.Status.LOCKED
                    || version == null || version != user.getSessionVersion());
            if (revoked) {
                session.invalidate();
                user = null;
            }

            // Khách chưa đăng nhập không cần nạp ma trận quyền khi mở trang công khai.
            PermissionPolicy policy = user == null ? null : permissionService.policy();
            if (user != null) {
                req.setAttribute(SessionKeys.CURRENT_USER, user);
                req.setAttribute("menu", Menu.build(user, policy, path));
                req.setAttribute("canViewCostPrice", PermissionPolicy.canViewCostPrice(user.getRoles()));
                req.setAttribute("canManageDiscounts", PermissionPolicy.canManageDiscounts(user.getRoles()));
            }

            if (AccessRules.isPublic(path)) {
                chain.doFilter(request, response);
                return;
            }

            if (user == null && path.equals("/session/ping")) {
                // Gọi bằng fetch: trả 401 để app.js báo hết phiên, không chuyển hướng sang trang đăng nhập
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            if (user == null) {
                // Phiên hết hạn hoặc bị thu hồi thì báo rõ ở trang đăng nhập (S1-02)
                boolean expired = revoked || (req.getRequestedSessionId() != null && !req.isRequestedSessionIdValid());
                resp.sendRedirect(req.getContextPath() + "/login" + (expired ? "?expired=1" : ""));
                return;
            }

            if (user.isMustChangePassword() && !path.equals("/account/change-password") && !path.equals("/logout")
                    && !path.equals("/session/ping")) {
                resp.sendRedirect(req.getContextPath() + "/account/change-password");
                return;
            }

            AccessRules.Rule rule = AccessRules.find(path);
            if (rule == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            if (!AccessRules.isAllowed(rule, user, policy)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }

        chain.doFilter(request, response);
    }
}
