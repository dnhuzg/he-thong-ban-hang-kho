package com.oms.web.servlet;

import com.oms.service.ForbiddenException;
import com.oms.service.PickingService;
import com.oms.service.ServiceException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** GHTK-60: phiếu soạn hàng từ đơn đã duyệt; trang danh sách, trang soạn (dùng được trên điện thoại). */
@WebServlet({"/shipments/picking", "/shipments/picking/view", "/shipments/picking/create",
        "/shipments/picking/pick", "/shipments/picking/complete"})
public class PickingServlet extends BaseServlet {

    private final PickingService pickingService = new PickingService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            switch (req.getServletPath()) {
                case "/shipments/picking" -> {
                    req.setAttribute("picking", pickingService.page(currentUser(req)));
                    render(req, resp, "shipments/picking");
                }
                case "/shipments/picking/view" -> {
                    req.setAttribute("list", pickingService.get(longParam(req, "id"), currentUser(req)));
                    render(req, resp, "shipments/picking-view");
                }
                default -> resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            }
        } catch (PickingService.NotFoundException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (ForbiddenException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        long listId = longParam(req, "id");
        String back = "/shipments/picking/view?id=" + listId;
        try {
            switch (req.getServletPath()) {
                case "/shipments/picking/create" -> {
                    back = "/shipments/picking";
                    long id = pickingService.create(orderIds(req), currentUser(req));
                    flash(req, "Đã lập phiếu soạn; các đơn chuyển sang Đang soạn hàng.");
                    back = "/shipments/picking/view?id=" + id;
                }
                case "/shipments/picking/pick" -> {
                    long lineId = longParam(req, "lineId");
                    back += "#line-" + lineId;
                    pickingService.pick(listId, lineId, quantity(param(req, "picked")), param(req, "shortReason"),
                            currentUser(req));
                }
                case "/shipments/picking/complete" -> {
                    pickingService.complete(listId, currentUser(req));
                    flash(req, "Đã hoàn tất phiếu soạn; các đơn sẵn sàng xuất kho.");
                }
                default -> {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
            }
        } catch (PickingService.NotFoundException e) {
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
        redirect(req, resp, back);
    }

    private static List<Long> orderIds(HttpServletRequest req) {
        List<Long> ids = new ArrayList<>();
        String[] values = req.getParameterValues("orderId");
        if (values != null) {
            for (String value : values) {
                try {
                    ids.add(Long.parseLong(value.trim()));
                } catch (NumberFormatException e) {
                    throw new ServiceException("Đơn được chọn không hợp lệ.");
                }
            }
        }
        return ids;
    }

    private static BigDecimal quantity(String value) {
        if (value.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(value.replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new ServiceException("Số lượng thực soạn không hợp lệ.");
        }
    }
}
