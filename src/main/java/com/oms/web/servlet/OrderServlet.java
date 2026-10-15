
package com.oms.web.servlet;

import com.oms.model.Order;
import com.oms.model.Role;
import com.oms.service.CustomerService;
import com.oms.service.ForbiddenException;
import com.oms.service.OrderLifecycleService;
import com.oms.service.OrderService;
import com.oms.service.OrderWorkflowService;
import com.oms.service.ServiceException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Quản lý đơn hàng và chức năng S5-02: đặt lại đơn đã mua.
 */
@WebServlet({
        "/orders",
        "/orders/new",
        "/orders/create",
        "/orders/edit",
        "/orders/save",
        "/orders/delete",
        "/orders/lines/add",
        "/orders/lines/save",
        "/orders/lines/delete",
        "/orders/products",
        "/orders/submit",
        "/orders/cancel",
        "/orders/decide",
        "/orders/approvals",
        "/orders/reorder"
})
public class OrderServlet extends BaseServlet {

    private final OrderService orderService = new OrderService();
    private final CustomerService customerService = new CustomerService();
    private final OrderWorkflowService workflowService =
            new OrderWorkflowService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        try {
            switch (req.getServletPath()) {

                case "/orders" -> {
                    String q = param(req, "q");

                    req.setAttribute(
                            "result",
                            orderService.search(
                                    q,
                                    intParam(req, "page", 1),
                                    currentUser(req)
                            )
                    );

                    req.setAttribute(
                            "stats",
                            orderService.stats(
                                    currentUser(req),
                                    LocalDate.now()
                            )
                    );

                    req.setAttribute("q", q);
                    render(req, resp, "orders/list");
                }

                case "/orders/new" -> {
                    String q = param(req, "q");

                    orderService.search(null, 1, currentUser(req));
                    req.setAttribute("q", q);

                    req.setAttribute(
                            "customers",
                            customerService.search(
                                    q, null, null, null, null,
                                    1, currentUser(req)
                            )
                    );

                    render(req, resp, "orders/new");
                }

                case "/orders/edit" -> showEdit(req, resp);

                // S5-02: Trang chọn dòng hàng để đặt lại.
                case "/orders/reorder" -> showReorder(req, resp);

                case "/orders/approvals" -> {
                    req.setAttribute(
                            "pending",
                            workflowService.pending(currentUser(req))
                    );
                    render(req, resp, "orders/approvals");
                }

                case "/orders/products" -> {
                    Order order = orderService.get(
                            longParam(req, "id"),
                            currentUser(req)
                    );

                    String q = param(req, "q");

                    req.setAttribute("order", order);
                    req.setAttribute("q", q);
                    req.setAttribute(
                            "choices",
                            orderService.searchProducts(order, q)
                    );

                    resp.setHeader("Cache-Control", "no-store");
                    render(req, resp, "orders/product-results");
                }

                default -> redirect(req, resp, "/orders");
            }

        } catch (ForbiddenException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);

        } catch (OrderService.NotFoundException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);

        } catch (ServiceException e) {
            flash(req, e.getMessage());
            redirect(req, resp, "/portal/orders");

        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String path = req.getServletPath();
        long id = longParam(req, "id");
        String back = "/orders/edit?id=" + id;

        try {
            switch (path) {

                case "/orders/create" -> {
                    long newId = orderService.create(
                            longParam(req, "customerId"),
                            currentUser(req)
                    );

                    flash(
                            req,
                            "Đã tạo đơn nháp. Chọn điểm giao và thêm hàng bên dưới."
                    );

                    redirect(req, resp, "/orders/edit?id=" + newId);
                    return;
                }

                // S5-02: Tạo đơn mới từ dòng hàng được chọn.
                case "/orders/reorder" -> {
                    if (!currentUser(req).hasRole(Role.CUSTOMER)) {
                        throw new ForbiddenException(
                                "Chỉ Đại lý được phép đặt lại đơn hàng."
                        );
                    }

                    String[] selected =
                            req.getParameterValues("lineIds");

                    if (selected == null || selected.length == 0) {
                        throw new ServiceException(
                                "Vui lòng chọn ít nhất một mặt hàng để đặt lại."
                        );
                    }

                    long[] lineIds = new long[selected.length];

                    try {
                        for (int i = 0; i < selected.length; i++) {
                            lineIds[i] = Long.parseLong(selected[i]);
                        }
                    } catch (NumberFormatException e) {
                        throw new ServiceException(
                                "Danh sách dòng hàng không hợp lệ."
                        );
                    }

                    OrderService.ReorderResult result =
                            orderService.reorder(
                                    id,
                                    lineIds,
                                    currentUser(req)
                            );

                    flash(req, result.message());

                    // S5-02: Mở đúng đơn nháp vừa được tạo lại.
                    redirect(
                            req,
                            resp,
                            "/portal?id=" + result.newOrderId()
                    );
                    return;
                }

                case "/orders/save" -> {
                    long point = longParam(req, "deliveryPointId");

                    orderService.saveHeader(
                            id,
                            point > 0 ? point : null,
                            date(param(req, "deliveryDate")),
                            param(req, "note"),
                            currentUser(req)
                    );

                    flash(req, "Đã lưu nháp.");
                }

                case "/orders/lines/add" -> {
                    orderService.addLine(
                            id,
                            longParam(req, "productId"),
                            longParam(req, "unitId"),
                            param(req, "quantity"),
                            currentUser(req)
                    );

                    flash(req, "Đã thêm dòng hàng.");
                    back += "#dong-hang";
                }

                case "/orders/lines/save" -> {
                    orderService.updateLine(
                            id,
                            longParam(req, "lineId"),
                            longParam(req, "unitId"),
                            param(req, "quantity"),
                            param(req, "manualPrice"),
                            currentUser(req)
                    );

                    flash(req, "Đã cập nhật dòng hàng.");
                    back += "#dong-hang";
                }

                case "/orders/lines/delete" -> {
                    orderService.deleteLine(
                            id,
                            longParam(req, "lineId"),
                            currentUser(req)
                    );

                    flash(req, "Đã xoá dòng hàng.");
                    back += "#dong-hang";
                }

                case "/orders/submit" -> {
                    Order.Status next = workflowService.submit(
                            id,
                            currentUser(req)
                    );

                    flash(
                            req,
                            next == Order.Status.APPROVED
                                    ? "Đã chốt đơn. Đơn được duyệt ngay và đã giữ hàng trong kho."
                                    : "Đã chốt đơn. Đơn đang chờ duyệt, lý do ghi ở đầu trang."
                    );
                }

                case "/orders/decide" -> {
                    String action = param(req, "action");

                    workflowService.decide(
                            id,
                            action,
                            param(req, "opinion"),
                            currentUser(req)
                    );

                    flash(
                            req,
                            switch (action) {
                                case "APPROVE" ->
                                        "Đã duyệt đơn. Hàng đã được giữ chỗ trong kho.";
                                case "REJECT" ->
                                        "Đã từ chối đơn.";
                                default ->
                                        "Đã trả đơn về Nháp để sửa.";
                            }
                    );
                }

                case "/orders/cancel" -> {
                    new OrderLifecycleService().cancel(
                            id,
                            param(req, "reason"),
                            currentUser(req)
                    );

                    flash(
                            req,
                            "Đã huỷ đơn. Hàng và hạn mức đang giữ cho đơn đã được nhả."
                    );
                }

                case "/orders/delete" -> {
                    orderService.delete(
                            id,
                            currentUser(req)
                    );

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

            String errorBack = switch (path) {
                case "/orders/create" -> "/orders/new";
                case "/orders/reorder" -> "/orders/reorder?id=" + id;
                default -> back;
            };

            redirect(req, resp, errorBack);

        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    /**
     * S5-02: Hiển thị các dòng của đơn cũ để chọn đặt lại.
     */
    private void showReorder(
            HttpServletRequest req,
            HttpServletResponse resp
    ) throws SQLException, ServletException, IOException {

        if (!currentUser(req).hasRole(Role.CUSTOMER)) {
            throw new ForbiddenException(
                    "Chỉ Đại lý được phép đặt lại đơn hàng."
            );
        }

        Order order = orderService.get(
                longParam(req, "id"),
                currentUser(req)
        );

        if (order.getStatus() == Order.Status.DRAFT) {
            throw new ServiceException(
                    "Không thể đặt lại từ đơn hàng đang ở trạng thái Nháp."
            );
        }

        req.setAttribute("order", order);
        render(req, resp, "orders/reorder");
    }

    private void showEdit(
            HttpServletRequest req,
            HttpServletResponse resp
    ) throws SQLException, ServletException, IOException {

        Order order = orderService.get(
                longParam(req, "id"),
                currentUser(req)
        );

        String q = param(req, "q");

        req.setAttribute("order", order);
        req.setAttribute("canCopyOrder", new com.oms.service.PermissionService().policy()
                .canWrite(currentUser(req).getRoles(), com.oms.model.Module.ORDER));
        req.setAttribute(
                "points",
                orderService.deliveryPoints(order.getCustomerId())
        );
        req.setAttribute(
                "unitOptions",
                orderService.unitOptions(order)
        );
        req.setAttribute("q", q);
        req.setAttribute(
                "choices",
                orderService.searchProducts(order, q)
        );
        req.setAttribute("today", LocalDate.now());
        req.setAttribute(
                "history",
                workflowService.history(
                        order.getId(),
                        currentUser(req)
                )
        );
        req.setAttribute("statuses", Order.Status.values());
        req.setAttribute(
                "canDecide",
                order.getStatus() == Order.Status.PENDING_APPROVAL
                        && OrderWorkflowService.canDecide(
                                order,
                                currentUser(req)
                        )
        );

        render(req, resp, "orders/edit");
    }

    private static LocalDate date(String text) {
        if (text.isEmpty()) {
            return null;
        }

        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new ServiceException(
                    "Ngày giao không hợp lệ: " + text
            );
        }
    }
}
