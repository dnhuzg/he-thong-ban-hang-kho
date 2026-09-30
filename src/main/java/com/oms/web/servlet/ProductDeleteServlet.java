package com.oms.web.servlet;

import com.oms.service.ProductService;
import com.oms.service.ServiceException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/** Xoá sản phẩm chưa phát sinh dữ liệu liên quan (S2-05); đã phát sinh thì báo chuyển sang ngừng kinh doanh. */
@WebServlet("/products/delete")
public class ProductDeleteServlet extends BaseServlet {

    private final ProductService productService = new ProductService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            productService.delete(longParam(req, "id"), currentUser(req));
            flash(req, "Đã xoá sản phẩm.");
        } catch (ProductService.NotFoundException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        } catch (ServiceException e) {
            flash(req, e.getMessage());
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        redirect(req, resp, "/products");
    }
}
