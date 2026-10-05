package com.oms.web.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** GHTK-53: trang xem tồn kho mẫu, chờ backend cung cấp số liệu tồn theo SKU và kho. */
@WebServlet("/inventory/stock")
public class StockPageServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        render(req, resp, "inventory/stock");
    }
}
