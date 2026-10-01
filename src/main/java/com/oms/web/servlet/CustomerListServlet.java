package com.oms.web.servlet;

import com.oms.security.PermissionPolicy;
import com.oms.service.CustomerService;
import com.oms.service.ForbiddenException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/** Danh sách đại lý (S3-03); Nhân viên kinh doanh chỉ thấy đại lý mình phụ trách. */
@WebServlet("/customers")
public class CustomerListServlet extends BaseServlet {

    private final CustomerService customerService = new CustomerService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            req.setAttribute("result", customerService.search(null, null, null, null, null,
                    intParam(req, "page", 1), currentUser(req)));
            req.setAttribute("canManage", PermissionPolicy.canManageCustomers(currentUser(req).getRoles()));
            render(req, resp, "customers/list");
        } catch (ForbiddenException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }
}
