package com.oms.web.servlet;

import com.oms.model.User;
import com.oms.service.AuthService;
import com.oms.service.ServiceException;
import com.oms.web.SessionKeys;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/account/change-password")
public class ChangePasswordServlet extends BaseServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        render(req, resp, "account/change-password");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = currentUser(req);
        try {
            int newVersion = authService.changePassword(user.getId(), req.getParameter("currentPassword"),
                    req.getParameter("newPassword"), req.getParameter("confirmPassword"));
            // Giữ phiên hiện tại; các phiên khác mang version cũ sẽ bị thu hồi ở AuthFilter (S1-04)
            req.getSession().setAttribute(SessionKeys.SESSION_VERSION, newVersion);
            flash(req, "Đã đổi mật khẩu. Các phiên đăng nhập trên thiết bị khác đã bị thu hồi.");
            redirect(req, resp, "/home");
        } catch (ServiceException e) {
            req.setAttribute("error", e.getMessage());
            render(req, resp, "account/change-password");
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }
}
