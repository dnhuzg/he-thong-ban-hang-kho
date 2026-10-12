package com.oms.web.servlet;

import com.oms.security.PermissionPolicy;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Giao diện phân tích tuổi nợ mẫu, chờ backend cung cấp dữ liệu công nợ thật. */
@WebServlet("/finance/debt-aging")
public class DebtAgingPageServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!PermissionPolicy.canManageCredit(currentUser(req).getRoles())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        render(req, resp, "finance/debt-aging");
    }
}
