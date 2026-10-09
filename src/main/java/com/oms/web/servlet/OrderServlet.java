package com.oms.web.servlet;

import com.oms.model.Order;
import com.oms.service.CustomerService;
import com.oms.service.ForbiddenException;
import com.oms.service.OrderService;
import com.oms.service.ServiceException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Đơn hàng nháp (S3-09): danh sách, chọn đại lý, màn hình gõ đơn (giao hàng, thêm / sửa / xoá dòng hàng), xoá nháp. */
@WebServlet({"/orders", "/orders/new", "/orders/create", "/orders/edit", "/orders/save", "/orders/delete",
        "/orders/lines/add", "/orders/lines/save", "/orders/lines/delete", "/orders/products"})
public class OrderServlet extends BaseServlet {

    private final OrderService orderService = new OrderService();
    private final CustomerService customerService = new CustomerService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            switch (req.getServletPath()) {
                case "/orders" -> {
                    String q = param(req, "q");
                    req.setAttribute("result", orderService.search(q, intParam(req, "page", 1), currentUser(req)));
                    req.setAttribute("stats", orderService.stats(currentUser(req), LocalDate.now()));
                    req.setAttribute("q", q);
                    render(req, resp, "orders/list");
                }
                case "/orders/new" -> {
                    String q = param(req, "q");
                    // chặn người không được lập đơn trước khi tra danh sách đại lý
                    orderService.search(null, 1, currentUser(req));
                    req.setAttribute("q", q);
                    req.setAttribute("customers", customerService.search(q, null, null, null, null, 1, currentUser(req)));
                    render(req, resp, "orders/new");
                }
                case "/orders/edit" -> showEdit(req, resp);
                case "/orders/products" -> {
                    // gợi ý khi gõ: chỉ trả phần danh sách sản phẩm, không kèm khung trang
                    Order order = orderService.get(longParam(req, "id"), currentUser(req));
                    String q = param(req, "q");
                    req.setAttribute("order", order);
                    req.setAttribute("q", q);
                    req.setAttribute("choices", orderService.searchProducts(order, q));
                    resp.setHeader("Cache-Control", "no-store");
                    render(req, resp, "orders/product-results");
                }
                default -> redirect(req, resp, "/orders");
            }
        } catch (ForbiddenException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
        } catch (OrderService.NotFoundException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();
        long id = longParam(req, "id");
        String back = "/orders/edit?id=" + id;
        try {
            switch (path) {
                case "/orders/create" -> {
                    long newId = orderService.create(longParam(req, "customerId"), currentUser(req));
                    flash(req, "Đã tạo đơn nháp. Chọn điểm giao và thêm hàng bên dưới.");
                    redirect(req, resp, "/orders/edit?id=" + newId);
                    return;
                }
                case "/orders/save" -> {
                    long point = longParam(req, "deliveryPointId");
                    orderService.saveHeader(id, point > 0 ? point : null, date(param(req, "deliveryDate")), param(req, "note"), currentUser(req));
                    flash(req, "Đã lưu nháp.");
                }
                case "/orders/lines/add" -> {
                    orderService.addLine(id, longParam(req, "productId"), longParam(req, "unitId"), param(req, "quantity"), currentUser(req));
                    flash(req, "Đã thêm dòng hàng.");
                    back += "#dong-hang";
                }
                case "/orders/lines/save" -> {
                    orderService.updateLine(id, longParam(req, "lineId"), longParam(req, "unitId"), param(req, "quantity"), currentUser(req));
                    flash(req, "Đã cập nhật dòng hàng.");
                    back += "#dong-hang";
                }
                case "/orders/lines/delete" -> {
                    orderService.deleteLine(id, longParam(req, "lineId"), currentUser(req));
                    flash(req, "Đã xoá dòng hàng.");
                    back += "#dong-hang";
                }
                case "/orders/delete" -> {
                    orderService.delete(id, currentUser(req));
                    flash(req, "Đã xoá đơn nháp.");
                    redirect(req, resp, "/orders");
                    return;
                }
                default -> {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
            }
            redirect(req, resp, back);
        } catch (ForbiddenException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
        } catch (OrderService.NotFoundException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (ServiceException e) {
            flash(req, e.getMessage());
            redirect(req, resp, path.equals("/orders/create") ? "/orders/new" : back);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void showEdit(HttpServletRequest req, HttpServletResponse resp) throws SQLException, ServletException, IOException {
        Order order = orderService.get(longParam(req, "id"), currentUser(req));
        String q = param(req, "q");
        req.setAttribute("order", order);
        req.setAttribute("points", orderService.deliveryPoints(order.getCustomerId()));
        req.setAttribute("unitOptions", orderService.unitOptions(order));
        req.setAttribute("q", q);
        req.setAttribute("choices", orderService.searchProducts(order, q));
        req.setAttribute("today", LocalDate.now());
        render(req, resp, "orders/edit");
    }

    private static LocalDate date(String text) {
        if (text.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new ServiceException("Ngày giao không hợp lệ: " + text);
        }
    }
}
