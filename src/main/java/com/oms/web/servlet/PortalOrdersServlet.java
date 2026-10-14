package com.oms.web.servlet;

import com.oms.service.ForbiddenException;
import com.oms.service.OrderService;
import com.oms.service.OrderTrackingService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/** GHTK-49: đại lý xem danh sách và chi tiết đơn của chính mình trên cổng. */
@WebServlet({"/portal/orders", "/portal/orders/view"})
public class PortalOrdersServlet extends BaseServlet {

    private final OrderTrackingService trackingService = new OrderTrackingService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            if ("/portal/orders/view".equals(req.getServletPath())) {
                req.setAttribute("detail", trackingService.detail(longParam(req, "id"), currentUser(req)));
                render(req, resp, "portal/order-view");
            } else {
                req.setAttribute("orders", trackingService.list(currentUser(req)));
                render(req, resp, "portal/orders");
            }
        } catch (OrderService.NotFoundException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (ForbiddenException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }
}
