package com.oms.web.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** GHTK-59: thẻ kho mẫu, chờ backend cung cấp sổ phát sinh và chứng từ gốc. */
@WebServlet("/inventory/stock-card")
public class StockCardPageServlet extends BaseServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        render(req, resp, "inventory/stock-card");
    }
}
