package com.oms.web.servlet;

import com.oms.model.Role;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * GHTK-75: Man hinh FE minh hoa in phieu thu.
 * Khong phat hanh chung tu hay doc giao dich that.
 */
@WebServlet("/finance/payment-receipts")
public class PaymentReceiptPageServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        if (currentUser(req) == null) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        if (!currentUser(req).hasRole(Role.ACCOUNTANT)
                && !currentUser(req).hasRole(Role.ADMIN)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        resp.setHeader("Cache-Control", "no-store");
        render(req, resp, "finance/payment-receipts");
    }
}