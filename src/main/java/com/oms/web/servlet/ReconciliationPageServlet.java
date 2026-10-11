package com.oms.web.servlet;

import com.oms.model.Role;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** GHTK-72: kế toán lập mẫu biên bản; đại lý xem và phản hồi mẫu trong cổng đại lý. */
@WebServlet({"/finance/reconciliations", "/portal/reconciliations"})
public class ReconciliationPageServlet extends BaseServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        boolean portal = req.getServletPath().equals("/portal/reconciliations");
        boolean allowed = portal ? currentUser(req).hasRole(Role.CUSTOMER)
                : currentUser(req).hasRole(Role.ACCOUNTANT) || currentUser(req).hasRole(Role.ADMIN);
        if (!allowed) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        req.setAttribute("reconciliationPortal", portal);
        render(req, resp, "finance/reconciliations");
    }
}
