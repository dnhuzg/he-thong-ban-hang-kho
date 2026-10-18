package com.oms.web.servlet;

import com.oms.service.DeliveryTripService;
import com.oms.service.ForbiddenException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;

/** GHTK-64: chuyến giao đang chạy, số điểm đã giao / còn lại và tiền hàng còn trên đường. */
@WebServlet("/shipments/in-transit")
public class InTransitPageServlet extends BaseServlet {

    private final DeliveryTripService service = new DeliveryTripService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            req.setAttribute("page", service.inTransit(currentUser(req), LocalDate.now()));
            render(req, resp, "shipments/in-transit");
        } catch (ForbiddenException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }
}
