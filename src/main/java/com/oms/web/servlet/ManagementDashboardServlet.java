package com.oms.web.servlet;

import com.oms.model.Role;
import com.oms.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/reports/management-dashboard")
public class ManagementDashboardServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        User user = currentUser(req);

        if (user == null
                || (!user.getRoles().contains(Role.SALES_MANAGER)
                && !user.getRoles().contains(Role.ADMIN))) {

            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        req.setAttribute("dashboardDemo", true);
        render(req, resp, "reports/management-dashboard");
    }
}