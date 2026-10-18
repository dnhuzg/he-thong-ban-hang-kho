package com.oms.web.servlet;

import com.oms.service.ForbiddenException;
import com.oms.service.OrderService;
import com.oms.service.ServiceException;
import com.oms.service.ShipmentExportService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/** GHTK-62: xác nhận xuất kho cho đơn đã soạn xong, danh sách đơn còn nợ hàng và phiếu xuất gần đây. */
@WebServlet({"/shipments/export", "/shipments/export/confirm"})
public class ShipmentExportServlet extends BaseServlet {

    private final ShipmentExportService exportService = new ShipmentExportService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!"/shipments/export".equals(req.getServletPath())) {
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        try {
            req.setAttribute("export", exportService.page(currentUser(req)));
            req.setAttribute("canOverrideExpiry", currentUser(req).hasRole(com.oms.model.Role.WH_MANAGER));
            render(req, resp, "shipments/export");
        } catch (ForbiddenException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!"/shipments/export/confirm".equals(req.getServletPath())) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        try {
            long shipmentId = exportService.export(longParam(req, "orderId"), currentUser(req),
                    "true".equals(param(req,"overrideExpiry")),param(req,"overrideReason"),
                    chosenLots(req), param(req, "lotReason"));
            flash(req, String.format("Đã xuất kho, lập phiếu xuất PX%06d; phiếu chờ xác nhận giao hàng.", shipmentId));
        } catch (OrderService.NotFoundException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        } catch (ForbiddenException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        } catch (ServiceException e) {
            flash(req, e.getMessage());
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        redirect(req, resp, "/shipments/export");
    }

    /** Cặp lotProduct[i] → lotId[i] từ hộp thoại chọn lô (GHTK-61). */
    private static java.util.Map<Long, Long> chosenLots(HttpServletRequest req) {
        String[] products = req.getParameterValues("lotProduct");
        String[] lots = req.getParameterValues("lotId");
        java.util.Map<Long, Long> chosen = new java.util.HashMap<>();
        if (products == null || lots == null || products.length != lots.length) {
            return chosen;
        }
        for (int i = 0; i < products.length; i++) {
            try {
                chosen.put(Long.parseLong(products[i]), Long.parseLong(lots[i]));
            } catch (NumberFormatException e) {
                throw new ServiceException("Lô đã chọn không hợp lệ.");
            }
        }
        return chosen;
    }
}
