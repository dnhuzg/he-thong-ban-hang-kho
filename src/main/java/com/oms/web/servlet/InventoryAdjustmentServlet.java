
package com.oms.web.servlet;

import com.oms.config.Db;
import com.oms.dao.InventoryAdjustmentDao;
import com.oms.dao.LookupDao;
import com.oms.model.NamedItem;
import com.oms.model.User;
import com.oms.service.ForbiddenException;
import com.oms.service.InventoryAdjustmentService;
import com.oms.service.ServiceException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@WebServlet({
        "/inventory/adjustments",
        "/inventory/adjustments/detail",
        "/inventory/adjustments/create",
        "/inventory/adjustments/submit",
        "/inventory/adjustments/approve",
        "/inventory/adjustments/reject"
})
public class InventoryAdjustmentServlet extends BaseServlet {

    private final InventoryAdjustmentService service =
            new InventoryAdjustmentService();

    @Override
    protected void doGet(
            HttpServletRequest req,
            HttpServletResponse resp
    ) throws ServletException, IOException {

        try {
            User actor = currentUser(req);

            switch (req.getServletPath()) {
                case "/inventory/adjustments" -> {
                    if ("json".equalsIgnoreCase(
                            req.getParameter("format"))) {

                        List<InventoryAdjustmentDao.Adjustment> adjustments =
                                service.list(actor);

                        StringBuilder json = new StringBuilder("[");

                        for (int i = 0; i < adjustments.size(); i++) {
                            if (i > 0) {
                                json.append(",");
                            }
                            json.append(adjustmentJson(
                                    adjustments.get(i)));
                        }

                        json.append("]");
                        respondJson(resp, 200, json.toString());
                        return;
                    }

                    // Kiểm tra quyền truy cập trước khi render JSP.
                    service.list(actor);

                    List<NamedItem> warehouses;
                    try (Connection conn = Db.getConnection()) {
                        warehouses = new LookupDao().warehouses(conn);
                    }

                    req.setAttribute("adjustmentWarehouses", warehouses);
                    req.setAttribute("adjustmentCurrentUserId", actor.getId());

                    render(req, resp, "inventory/adjustments");
                }

                case "/inventory/adjustments/detail" -> {
                    long id = positiveLong(req, "id");

                    InventoryAdjustmentService.Detail detail =
                            service.get(id, actor);

                    StringBuilder json = new StringBuilder();

                    json.append("{\"adjustment\":")
                            .append(adjustmentJson(detail.adjustment()))
                            .append(",\"lines\":[");

                    for (int i = 0; i < detail.lines().size(); i++) {
                        if (i > 0) {
                            json.append(",");
                        }

                        InventoryAdjustmentDao.AdjustmentLine line =
                                detail.lines().get(i);

                        json.append("{\"id\":")
                                .append(line.id())
                                .append(",\"productId\":")
                                .append(line.productId())
                                .append(",\"reason\":")
                                .append(quote(line.reason()))
                                .append(",\"reasonDetail\":")
                                .append(quote(line.reasonDetail()))
                                .append(",\"quantity\":")
                                .append(line.quantity().toPlainString())
                                .append(",\"lossValue\":")
                                .append(line.lossValue().toPlainString())
                                .append("}");
                    }

                    json.append("]}");
                    respondJson(resp, 200, json.toString());
                }

                default -> resp.sendError(
                        HttpServletResponse.SC_NOT_FOUND);
            }

        } catch (InventoryAdjustmentService.NotFoundException e) {
            respondError(resp, 404, e.getMessage());

        } catch (ForbiddenException e) {
            respondError(resp, 403, e.getMessage());

        } catch (ServiceException | IllegalArgumentException e) {
            respondError(resp, 400, e.getMessage());

        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(
            HttpServletRequest req,
            HttpServletResponse resp
    ) throws ServletException, IOException {

        try {
            User actor = currentUser(req);

            switch (req.getServletPath()) {

                case "/inventory/adjustments/create" -> {
                    long warehouseId =
                            positiveLong(req, "warehouseId");

                    long id = service.create(
                            warehouseId,
                            req.getParameter("note"),
                            parseLines(req),
                            actor
                    );

                    respondJson(
                            resp,
                            201,
                            "{\"id\":" + id + ",\"status\":\"DRAFT\"}"
                    );
                }

                case "/inventory/adjustments/submit" -> {
                    long id = positiveLong(req, "id");

                    service.submit(id, actor);

                    respondJson(
                            resp,
                            200,
                            "{\"id\":" + id + ",\"status\":\"PENDING\"}"
                    );
                }

                case "/inventory/adjustments/approve" -> {
                    long id = positiveLong(req, "id");

                    service.approve(id, actor);

                    respondJson(
                            resp,
                            200,
                            "{\"id\":" + id + ",\"status\":\"APPROVED\"}"
                    );
                }

                case "/inventory/adjustments/reject" -> {
                    long id = positiveLong(req, "id");

                    service.reject(
                            id,
                            req.getParameter("rejectionReason"),
                            actor
                    );

                    respondJson(
                            resp,
                            200,
                            "{\"id\":" + id + ",\"status\":\"REJECTED\"}"
                    );
                }

                default -> resp.sendError(
                        HttpServletResponse.SC_NOT_FOUND);
            }

        } catch (InventoryAdjustmentService.NotFoundException e) {
            respondError(resp, 404, e.getMessage());

        } catch (ForbiddenException e) {
            respondError(resp, 403, e.getMessage());

        } catch (ServiceException | IllegalArgumentException e) {
            respondError(resp, 400, e.getMessage());

        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private static List<InventoryAdjustmentService.LineInput> parseLines(
            HttpServletRequest req
    ) {
        String[] productIds = req.getParameterValues("productId");
        String[] reasons = req.getParameterValues("reason");
        String[] details = req.getParameterValues("reasonDetail");
        String[] quantities = req.getParameterValues("quantity");
        String[] lossValues = req.getParameterValues("lossValue");

        if (productIds == null || productIds.length == 0) {
            throw new ServiceException(
                    "Phiếu phải có ít nhất một dòng hàng.");
        }

        int size = productIds.length;

        if (size > 100
                || reasons == null || reasons.length != size
                || details == null || details.length != size
                || quantities == null || quantities.length != size
                || lossValues == null || lossValues.length != size) {

            throw new ServiceException(
                    "Dữ liệu chi tiết phiếu điều chỉnh không hợp lệ.");
        }

        List<InventoryAdjustmentService.LineInput> lines =
                new ArrayList<>();

        try {
            for (int i = 0; i < size; i++) {
                lines.add(new InventoryAdjustmentService.LineInput(
                        Long.parseLong(productIds[i].trim()),
                        reasons[i],
                        details[i],
                        new BigDecimal(quantities[i].trim()),
                        new BigDecimal(lossValues[i].trim())
                ));
            }
        } catch (NumberFormatException | NullPointerException e) {
            throw new ServiceException(
                    "Mã sản phẩm, số lượng hoặc giá trị tổn thất không hợp lệ.");
        }

        return lines;
    }

    private static long positiveLong(
            HttpServletRequest req,
            String name
    ) {
        String raw = req.getParameter(name);

        try {
            long id = Long.parseLong(raw.trim());

            if (id <= 0) {
                throw new NumberFormatException();
            }

            return id;
        } catch (NumberFormatException | NullPointerException e) {
            throw new ServiceException(
                    "Tham số " + name + " không hợp lệ.");
        }
    }

    private static String adjustmentJson(
            InventoryAdjustmentDao.Adjustment a
    ) {
        return "{\"id\":" + a.id()
                + ",\"code\":" + quote(a.code())
                + ",\"warehouseId\":" + a.warehouseId()
                + ",\"status\":" + quote(a.status())
                + ",\"note\":" + quote(a.note())
                + ",\"totalLossValue\":"
                + a.totalLossValue().toPlainString()
                + ",\"createdBy\":" + a.createdBy()
                + ",\"approvedBy\":"
                + (a.approvedBy() == null
                ? "null" : a.approvedBy())
                + "}";
    }

    private static String quote(String value) {
        if (value == null) {
            return "null";
        }

        StringBuilder result = new StringBuilder("\"");

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            switch (c) {
                case '"' -> result.append("\\\"");
                case '\\' -> result.append("\\\\");
                case '\n' -> result.append("\\n");
                case '\r' -> result.append("\\r");
                case '\t' -> result.append("\\t");

                default -> {
                    if (c < 0x20) {
                        result.append(String.format(
                                "\\u%04x", (int) c));
                    } else {
                        result.append(c);
                    }
                }
            }
        }

        return result.append("\"").toString();
    }

    private static void respondJson(
            HttpServletResponse resp,
            int status,
            String body
    ) throws IOException {

        resp.setStatus(status);
        resp.setContentType("application/json;charset=UTF-8");
        resp.setHeader("Cache-Control", "no-store");

        PrintWriter out = resp.getWriter();
        out.write(body);
    }

    private static void respondError(
            HttpServletResponse resp,
            int status,
            String message
    ) throws IOException {

        respondJson(
                resp,
                status,
                "{\"error\":" + quote(message) + "}"
        );
    }
}
