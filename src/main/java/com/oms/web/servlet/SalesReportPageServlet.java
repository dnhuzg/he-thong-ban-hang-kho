package com.oms.web.servlet;

import com.oms.model.Role;
import com.oms.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Set;

@WebServlet("/reports/sales")
public class SalesReportPageServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        User user = currentUser(req);

        if (user == null || !canView(user.getRoles())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        req.setAttribute("canViewGrossMargin", true);
        req.setAttribute("salesReportReady", false);
        render(req, resp, "reports/sales-report");
    }

    private static boolean canView(Set<Role> roles) {
        return roles != null
                && (roles.contains(Role.SALES_MANAGER)
                    || roles.contains(Role.ADMIN));
    }
}