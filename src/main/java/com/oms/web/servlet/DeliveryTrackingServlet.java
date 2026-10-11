package com.oms.web.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * GHTK-64: giao diện theo dõi các chuyến đã xuất kho nhưng chưa giao xong.
 * Dữ liệu hiện do frontend minh họa, chờ backend cung cấp dữ liệu chuyến giao.
 */
@WebServlet("/deliveries/tracking")
public class DeliveryTrackingServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        render(req, resp, "deliveries/tracking");
    }
}