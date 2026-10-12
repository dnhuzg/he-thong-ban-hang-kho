package com.oms.web.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * GHTK-73: Giao dien FE xem cong no cua dai ly.
 * Chi hien thi du lieu mau, khong truy van cong no thuc.
 */
@WebServlet("/portal/my-debt")
public class MyDebtPageServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (currentUser(req) == null) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        resp.setHeader("Cache-Control", "no-store");
        render(req, resp, "portal/my-debt");
    }
}