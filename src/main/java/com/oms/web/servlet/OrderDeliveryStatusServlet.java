package com.oms.web.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * GHTK-66: giao diện theo dõi trạng thái giao hàng của đơn.
 * Dữ liệu hiện do frontend minh họa, chờ backend giao hàng cung cấp dữ liệu thật.
 */
@WebServlet("/orders/delivery-status")
public class OrderDeliveryStatusServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        render(req, resp, "orders/delivery-status");
    }
}