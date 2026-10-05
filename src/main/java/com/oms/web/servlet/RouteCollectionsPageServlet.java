package com.oms.web.servlet;

import com.oms.security.PermissionPolicy;
import com.oms.model.Role;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** GHTK-71: màn hình FE mẫu thu tiền theo tuyến và đối chiếu nộp tiền. */
@WebServlet("/finance/route-collections")
public class RouteCollectionsPageServlet extends BaseServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!PermissionPolicy.canTakeOrders(currentUser(req).getRoles())
                && !PermissionPolicy.canManageCredit(currentUser(req).getRoles())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        req.setAttribute("canConfirmRouteCollection", currentUser(req).getRoles().contains(Role.ACCOUNTANT)
                || currentUser(req).getRoles().contains(Role.ADMIN));
        req.setAttribute("canRecordRouteCollection", PermissionPolicy.canTakeOrders(currentUser(req).getRoles()));
        render(req, resp, "finance/route-collections");
    }
}
