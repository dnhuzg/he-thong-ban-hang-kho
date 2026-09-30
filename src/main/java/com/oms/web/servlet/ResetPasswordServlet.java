package com.oms.web.servlet;

import com.oms.service.PasswordResetService;
import com.oms.service.ServiceException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/reset-password")
public class ResetPasswordServlet extends BaseServlet {

    private final PasswordResetService resetService = new PasswordResetService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String token = param(req, "token");
        try {
            req.setAttribute("tokenValid", resetService.isTokenValid(token));
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        req.setAttribute("token", token);
        render(req, resp, "auth/reset-password");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String token = param(req, "token");
        try {
            resetService.resetPassword(token, req.getParameter("newPassword"), req.getParameter("confirmPassword"));
            redirect(req, resp, "/login?reset=1");
        } catch (PasswordResetService.InvalidTokenException e) {
            req.setAttribute("tokenValid", false);
            render(req, resp, "auth/reset-password");
        } catch (ServiceException e) {
            req.setAttribute("tokenValid", true);
            req.setAttribute("token", token);
            req.setAttribute("error", e.getMessage());
            render(req, resp, "auth/reset-password");
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }
}
