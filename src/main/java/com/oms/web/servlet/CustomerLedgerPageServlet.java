package com.oms.web.servlet;

import com.oms.model.Role;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** URL cũ GHTK-85 được hợp nhất về một màn sổ công nợ duy nhất. */
@WebServlet("/finance/customer-ledger")
public class CustomerLedgerPageServlet extends BaseServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!currentUser(req).hasRole(Role.ACCOUNTANT) && !currentUser(req).hasRole(Role.ADMIN)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        String customer = param(req, "customer");
        redirect(req, resp, "/finance/debt-ledger" + (customer.isBlank() ? "" : "?customer=" + java.net.URLEncoder.encode(customer, java.nio.charset.StandardCharsets.UTF_8)));
    }
}
