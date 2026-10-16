package com.oms.web.servlet;

import com.oms.model.Role;
import com.oms.model.User;
import com.oms.service.ManagementDashboardService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;

/** GHTK-80: dashboard điều hành bán hàng và kho từ dữ liệu thật. */
@WebServlet("/reports/management-dashboard")
public class ManagementDashboardServlet extends BaseServlet {

    private final ManagementDashboardService dashboardService = new ManagementDashboardService();

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

        try {
            req.setAttribute("dashboard", dashboardService.load(LocalDate.now()));
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        render(req, resp, "reports/management-dashboard");
    }
}
