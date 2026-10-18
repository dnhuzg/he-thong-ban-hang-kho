package com.oms.web.servlet;

import com.oms.security.PermissionPolicy;
import com.oms.service.FinanceOverviewService;
import com.oms.web.Json;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/** GHTK-70: màn hình minh hoạ cảnh báo công nợ; không chặn đơn bán thật. */
@WebServlet("/finance/credit-alerts")
public class CreditAlertsPageServlet extends BaseServlet {
    private final FinanceOverviewService service = new FinanceOverviewService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!PermissionPolicy.canManageCredit(currentUser(req).getRoles())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        try {
            req.setAttribute("creditAlertData", Json.encode(service.creditAlerts()).replace("<", "\\u003c"));
            render(req, resp, "finance/credit-alerts");
        } catch (SQLException e) { throw new ServletException(e); }
    }
}
