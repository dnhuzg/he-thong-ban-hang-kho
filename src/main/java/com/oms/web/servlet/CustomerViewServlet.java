package com.oms.web.servlet;

import com.oms.model.Customer;
import com.oms.security.PermissionPolicy;
import com.oms.service.CustomerService;
import com.oms.service.DeliveryPointService;
import com.oms.service.ForbiddenException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/** Trang chi tiết một đại lý (S3-03) kèm điểm giao hàng (S3-04); các story sau gắn thêm hạn mức, lịch sử phân công. */
@WebServlet("/customers/view")
public class CustomerViewServlet extends BaseServlet {

    private final CustomerService customerService = new CustomerService();
    private final DeliveryPointService pointService = new DeliveryPointService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            Customer customer = customerService.get(longParam(req, "id"), currentUser(req));
            req.setAttribute("customer", customer);
            req.setAttribute("canManage", PermissionPolicy.canManageCustomers(currentUser(req).getRoles()));
            req.setAttribute("points", pointService.list(customer.getId(), currentUser(req)));
            req.setAttribute("canEditPoints", DeliveryPointService.canEdit(currentUser(req), customer));
            render(req, resp, "customers/view");
        } catch (CustomerService.NotFoundException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (ForbiddenException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }
}
