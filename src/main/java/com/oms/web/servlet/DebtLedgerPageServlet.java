package com.oms.web.servlet;

import com.oms.model.Role;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Set;

/** GHTK-85: màn FE sổ chi tiết công nợ theo đại lý. */
@WebServlet("/finance/debt-ledger")
public class DebtLedgerPageServlet extends BaseServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Set<Role> roles = currentUser(req).getRoles();
        if (!roles.contains(Role.ACCOUNTANT) && !roles.contains(Role.ADMIN)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        render(req, resp, "finance/debt-ledger");
    }
}
