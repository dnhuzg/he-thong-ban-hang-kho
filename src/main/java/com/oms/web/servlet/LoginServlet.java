package com.oms.web.servlet;

import com.oms.model.User;
import com.oms.security.Tokens;
import com.oms.service.AuthService;
import com.oms.service.ServiceException;
import com.oms.web.SessionKeys;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/login")
public class LoginServlet extends BaseServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (currentUser(req) != null) {
            redirect(req, resp, "/home");
            return;
        }
        render(req, resp, "auth/login");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String username = param(req, "username");
        try {
            User user = authService.login(username, req.getParameter("password"));

            // Cấp session mới sau khi đăng nhập để chống session fixation
            HttpSession old = req.getSession(false);
            if (old != null) {
                old.invalidate();
            }
            HttpSession session = req.getSession(true);
            session.setMaxInactiveInterval(SessionKeys.SESSION_TIMEOUT_SECONDS);
            session.setAttribute(SessionKeys.USER_ID, user.getId());
            session.setAttribute(SessionKeys.SESSION_VERSION, user.getSessionVersion());
            session.setAttribute(SessionKeys.CSRF_TOKEN, Tokens.random());

            redirect(req, resp, user.isMustChangePassword() ? "/account/change-password" : "/home");
        } catch (ServiceException e) {
            req.setAttribute("error", e.getMessage());
            req.setAttribute("username", username);
            render(req, resp, "auth/login");
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }
}
