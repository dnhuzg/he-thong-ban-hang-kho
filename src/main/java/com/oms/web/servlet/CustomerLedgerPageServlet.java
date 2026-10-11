package com.oms.web.servlet;

import com.oms.model.Role;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** GHTK-85: sổ chi tiết công nợ theo một đại lý, dữ liệu trình diễn phía FE. */
@WebServlet("/finance/customer-ledger")
public class CustomerLedgerPageServlet extends BaseServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!currentUser(req).hasRole(Role.ACCOUNTANT) && !currentUser(req).hasRole(Role.ADMIN)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        render(req, resp, "finance/customer-ledger");
    }
}
