package com.oms.web.servlet;

import com.oms.model.Role;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Set;

/** GHTK-77: màn FE lập chứng từ giảm công nợ từ phiếu trả đã nhận. */
@WebServlet("/finance/return-credits")
public class ReturnCreditPageServlet extends BaseServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Set<Role> roles = currentUser(req).getRoles();
        boolean canCreate = roles.contains(Role.ACCOUNTANT) || roles.contains(Role.ADMIN);
        if (!canCreate) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        req.setAttribute("canCreateReturnCredit", true);
        render(req, resp, "finance/return-credits");
    }
}
