package com.oms.web.servlet;

import com.oms.service.ServiceException;
import com.oms.service.StockCardService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** GHTK-59: thẻ kho của một sản phẩm từ các bút toán nhập / xuất thật. */
@WebServlet("/inventory/stock-card")
public class StockCardPageServlet extends BaseServlet {

    private final StockCardService cardService = new StockCardService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        long product = longParam(req, "product");
        long warehouse = longParam(req, "warehouse");
        try {
            StockCardService.Filter filter = new StockCardService.Filter(product > 0 ? product : null,
                    warehouse > 0 ? warehouse : null, date(param(req, "from")), date(param(req, "to")));
            req.setAttribute("card", cardService.card(filter));
        } catch (ServiceException e) {
            flash(req, e.getMessage());
            redirect(req, resp, "/inventory/stock-card");
            return;
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        render(req, resp, "inventory/stock-card");
    }

    private static LocalDate date(String value) {
        if (value.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new ServiceException("Ngày không hợp lệ.");
        }
    }
}
