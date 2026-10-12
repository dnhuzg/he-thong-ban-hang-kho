package com.oms.web.servlet;

import com.oms.model.Role;
import com.oms.security.PermissionPolicy;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Set;

/** GHTK-76: màn hình FE minh họa quy trình trả hàng; chưa ghi dữ liệu nghiệp vụ. */
@WebServlet("/sales/returns")
public class SalesReturnPageServlet extends BaseServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Set<Role> roles = currentUser(req).getRoles();
        boolean canCreate = PermissionPolicy.canTakeOrders(roles);
        boolean canConfirm = roles.contains(Role.WH_MANAGER) || roles.contains(Role.ADMIN);
        if (!canCreate && !canConfirm) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        req.setAttribute("canCreateReturn", canCreate);
        req.setAttribute("canConfirmReturn", canConfirm);
        render(req, resp, "sales/returns");
    }
}
