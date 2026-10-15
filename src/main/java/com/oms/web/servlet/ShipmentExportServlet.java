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
            long shipmentId = exportService.export(longParam(req, "orderId"), currentUser(req));
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
}
