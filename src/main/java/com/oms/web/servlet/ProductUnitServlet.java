package com.oms.web.servlet;

import com.oms.service.ProductService;
import com.oms.service.ServiceException;
import com.oms.service.UnitConversionService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/** Thêm / sửa hệ số / xoá đơn vị quy đổi của một sản phẩm (S2-07); quay về trang sửa sản phẩm. */
@WebServlet({"/products/units/save", "/products/units/delete"})
public class ProductUnitServlet extends BaseServlet {

    private final UnitConversionService conversionService = new UnitConversionService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        long productId = longParam(req, "productId");
        long unitId = longParam(req, "unitId");
        try {
            if (req.getServletPath().equals("/products/units/save")) {
                conversionService.save(productId, unitId, param(req, "factor"), currentUser(req));
                flash(req, "Đã lưu đơn vị quy đổi.");
            } else {
                conversionService.delete(productId, unitId, currentUser(req));
                flash(req, "Đã xoá đơn vị quy đổi.");
            }
        } catch (ProductService.NotFoundException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        } catch (ServiceException e) {
            flash(req, e.getMessage());
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        redirect(req, resp, "/products/edit?id=" + productId + "#don-vi-quy-doi");
    }
}
