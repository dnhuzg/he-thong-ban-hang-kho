package com.oms.web.servlet;

import com.oms.model.Module;
import com.oms.model.User;
import com.oms.security.PermissionPolicy;
import com.oms.service.AuditService;
import com.oms.service.ForbiddenException;
import com.oms.service.HomeService;
import com.oms.service.MailService;
import com.oms.service.PermissionService;
import com.oms.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@WebServlet({"", "/home"})
public class HomeServlet extends BaseServlet {

    private final UserService userService = new UserService();
    private final PermissionService permissionService = new PermissionService();
    private final AuditService auditService = new AuditService();
    private final HomeService homeService = new HomeService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = currentUser(req);
        if (!req.getServletPath().equals("/home")) {
            // Địa chỉ gốc là trang chờ cho khách; đã đăng nhập thì vào thẳng trang chủ
            if (user == null) {
                render(req, resp, "landing");
            } else {
                redirect(req, resp, "/home");
            }
            return;
        }
        try {
            if (user.isAdmin()) {
                // Key dạng chuỗi để JSP tra được bằng userStats['ACTIVE']
                Map<String, Long> stats = new HashMap<>();
                userService.countByStatus().forEach((status, count) -> stats.put(status.name(), count));
                req.setAttribute("userStats", stats);
                req.setAttribute("handover", userService.customersNeedingHandover());
                req.setAttribute("canAssign", PermissionPolicy.canAssignCustomers(user.getRoles()));
                req.setAttribute("canManageUsers", permissionService.policy().canWrite(user.getRoles(), Module.USER_AUDIT));
                req.setAttribute("recentActivity", auditService.recent(5));
                req.setAttribute("mailConfigured", MailService.isConfigured());
            } else {
                req.setAttribute("canViewAuditLog", permissionService.policy().canRead(user.getRoles(), Module.USER_AUDIT));
                try {
                    req.setAttribute("dash", homeService.build(user, LocalDate.now()));
                } catch (ForbiddenException e) {
                    // Ma trận quyền đổi lệch với ràng buộc cố định thì chỉ còn lưới lối tắt, không để trang chủ lỗi 500
                    log("Trang chủ thiếu số liệu cho " + user.getUsername() + ": " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        render(req, resp, "home");
    }
}
