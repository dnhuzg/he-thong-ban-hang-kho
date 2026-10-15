package com.oms.web.servlet;

import com.oms.service.ForbiddenException;
import com.oms.service.ServiceException;
import com.oms.service.StockAdjustmentService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;

/** GHTK-78: lập và duyệt phiếu điều chỉnh giảm tồn (hàng hỏng, mất, hết hạn phải huỷ). */
@WebServlet({"/inventory/adjustments", "/inventory/adjustments/create", "/inventory/adjustments/approve",
        "/inventory/adjustments/reject"})
public class StockAdjustmentPageServlet extends BaseServlet {

    private final StockAdjustmentService adjustmentService = new StockAdjustmentService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            req.setAttribute("adjust", adjustmentService.page(currentUser(req)));
            render(req, resp, "inventory/adjustments");
        } catch (ForbiddenException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            switch (req.getServletPath()) {
                case "/inventory/adjustments/create" -> {
                    // stock = "khoId:sanPhamId" từ ô chọn SKU theo kho
                    String[] stock = param(req, "stock").split(":");
                    if (stock.length != 2) {
                        throw new ServiceException("Chọn SKU và kho cần điều chỉnh.");
                    }
                    long lot = longParam(req, "lotId");
                    adjustmentService.create(new StockAdjustmentService.Form(Long.parseLong(stock[0]),
                            Long.parseLong(stock[1]), lot > 0 ? lot : null, quantity(param(req, "quantity")),
                            param(req, "reason"), param(req, "note")), currentUser(req));
                    flash(req, "Đã lập phiếu điều chỉnh, chờ Quản lý kho duyệt. Tồn chưa bị trừ.");
                }
                case "/inventory/adjustments/approve" -> {
                    adjustmentService.approve(longParam(req, "id"), currentUser(req));
                    flash(req, "Đã duyệt phiếu điều chỉnh; tồn kho đã được trừ và ghi lên thẻ kho.");
                }
                case "/inventory/adjustments/reject" -> {
                    adjustmentService.reject(longParam(req, "id"), param(req, "rejectReason"), currentUser(req));
                    flash(req, "Đã từ chối phiếu điều chỉnh.");
                }
                default -> {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
            }
        } catch (ForbiddenException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        } catch (StockAdjustmentService.NotFoundException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        } catch (ServiceException e) {
            flash(req, e.getMessage());
        } catch (NumberFormatException e) {
            flash(req, "Dữ liệu phiếu điều chỉnh không hợp lệ.");
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        redirect(req, resp, "/inventory/adjustments");
    }

    private static BigDecimal quantity(String value) {
        try {
            return new BigDecimal(value.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new ServiceException("Số lượng không hợp lệ.");
        }
    }
}
