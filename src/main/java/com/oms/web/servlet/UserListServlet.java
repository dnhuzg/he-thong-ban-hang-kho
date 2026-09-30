package com.oms.web.servlet;

import com.oms.model.Module;
import com.oms.model.PageResult;
import com.oms.model.Role;
import com.oms.model.User;
import com.oms.service.PermissionService;
import com.oms.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/admin/users")
public class UserListServlet extends BaseServlet {

    private final UserService userService = new UserService();
    private final PermissionService permissionService = new PermissionService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String keyword = param(req, "q");
        Role role = parseEnum(Role.class, param(req, "role"));
        User.Status status = parseEnum(User.Status.class, param(req, "status"));
        int page = intParam(req, "page", 1);
        try {
            PageResult<User> result = userService.search(keyword, role, status, page);
            req.setAttribute("result", result);
            req.setAttribute("customerCounts", userService.customerCounts(result.getItems()));
            req.setAttribute("canWrite", permissionService.policy().canWrite(currentUser(req).getRoles(), Module.USER_AUDIT));
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        req.setAttribute("q", keyword);
        req.setAttribute("selectedRole", role == null ? "" : role.name());
        req.setAttribute("selectedStatus", status == null ? "" : status.name());
        req.setAttribute("roles", Role.values());
        req.setAttribute("statuses", User.Status.values());
        render(req, resp, "admin/users");
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value) {
        try {
            return value.isEmpty() ? null : Enum.valueOf(type, value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
