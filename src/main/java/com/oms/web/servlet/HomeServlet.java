package com.oms.web.servlet;

import com.oms.model.Module;
import com.oms.model.User;
import com.oms.service.PermissionService;
import com.oms.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@WebServlet({"", "/home"})
public class HomeServlet extends BaseServlet {

    private final UserService userService = new UserService();
    private final PermissionService permissionService = new PermissionService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!req.getServletPath().equals("/home")) {
            redirect(req, resp, "/home");
            return;
        }
        User user = currentUser(req);
        try {
            if (permissionService.policy().canRead(user.getRoles(), Module.USER_AUDIT)) {
                // Key dạng chuỗi để JSP tra được bằng userStats['ACTIVE']
                Map<String, Long> stats = new HashMap<>();
                userService.countByStatus().forEach((status, count) -> stats.put(status.name(), count));
                req.setAttribute("userStats", stats);
                req.setAttribute("handover", userService.customersNeedingHandover());
                req.setAttribute("canManageUsers", permissionService.policy().canWrite(user.getRoles(), Module.USER_AUDIT));
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        render(req, resp, "home");
    }
}
