package com.oms.web.servlet;

import com.oms.model.DeliveryPoint;
import com.oms.service.CustomerService;
import com.oms.service.DeliveryPointService;
import com.oms.service.ForbiddenException;
import com.oms.service.ServiceException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/** Thêm / sửa / đặt mặc định / xoá điểm giao hàng ngay trên trang chi tiết đại lý (S3-04). */
@WebServlet({"/customers/delivery-points/save", "/customers/delivery-points/default", "/customers/delivery-points/delete"})
public class DeliveryPointServlet extends BaseServlet {

    private final DeliveryPointService pointService = new DeliveryPointService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        redirect(req, resp, "/customers");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        long customerId = longParam(req, "customerId");
        long pointId = longParam(req, "pointId");
        try {
            switch (req.getServletPath()) {
                case "/customers/delivery-points/save" -> {
                    pointService.save(customerId, pointId, readForm(req), currentUser(req));
                    flash(req, pointId == 0 ? "Đã thêm điểm giao hàng." : "Đã cập nhật điểm giao hàng.");
                }
                case "/customers/delivery-points/default" -> {
                    pointService.makeDefault(customerId, pointId, currentUser(req));
                    flash(req, "Đã đặt làm điểm giao mặc định.");
                }
                default -> {
                    pointService.delete(customerId, pointId, currentUser(req));
                    flash(req, "Đã xoá điểm giao hàng.");
                }
            }
        } catch (CustomerService.NotFoundException e) {
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
        redirect(req, resp, "/customers/view?id=" + customerId + "#diem-giao-hang");
    }

    private static DeliveryPoint readForm(HttpServletRequest req) {
        DeliveryPoint p = new DeliveryPoint();
        p.setAddress(param(req, "address"));
        p.setReceiverName(param(req, "receiverName"));
        p.setReceiverPhone(param(req, "receiverPhone"));
        p.setRouteNote(param(req, "routeNote"));
        p.setDefaultPoint(req.getParameter("makeDefault") != null);
        return p;
    }
}
