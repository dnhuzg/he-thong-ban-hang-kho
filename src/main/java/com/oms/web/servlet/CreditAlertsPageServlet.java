package com.oms.web.servlet;

import com.oms.security.PermissionPolicy;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** GHTK-70: màn hình minh hoạ cảnh báo công nợ; không chặn đơn bán thật. */
@WebServlet("/finance/credit-alerts")
public class CreditAlertsPageServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!PermissionPolicy.canManageCredit(currentUser(req).getRoles())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        render(req, resp, "finance/credit-alerts");
    }
}
