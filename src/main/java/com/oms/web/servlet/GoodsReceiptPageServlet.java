package com.oms.web.servlet;

import com.oms.model.Module;
import com.oms.service.ForbiddenException;
import com.oms.service.GoodsReceiptService;
import com.oms.service.PermissionService;
import com.oms.service.ServiceException;
import com.oms.web.Json;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** GHTK-52: lập phiếu nhập kho; trang tải danh mục từ cơ sở dữ liệu, lưu / xác nhận phiếu qua JSON. */
@WebServlet({"/inventory/receipts", "/inventory/receipts/save"})
public class GoodsReceiptPageServlet extends BaseServlet {

    private final PermissionService permissionService = new PermissionService();
    private final GoodsReceiptService receiptService = new GoodsReceiptService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!"/inventory/receipts".equals(req.getServletPath())) {
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        long id = longParam(req, "id");
        try {
            req.setAttribute("canWrite", permissionService.policy()
                    .canWrite(currentUser(req).getRoles(), Module.INVENTORY));
            // Chặn "</script>" trong tên sản phẩm / nhà cung cấp phá khối JSON nhúng trong trang
            req.setAttribute("receiptData", Json.encode(receiptService.page(id > 0 ? id : null, currentUser(req)))
                    .replace("<", "\\u003c"));
        } catch (GoodsReceiptService.NotFoundException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        render(req, resp, "inventory/receipts");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (!"/inventory/receipts/save".equals(req.getServletPath())) {
            resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            resp.getWriter().write(Json.encode(Map.of("error", "Thao tác không hợp lệ.")));
            return;
        }
        try {
            GoodsReceiptService.Saved saved = receiptService.save(form(req), currentUser(req));
            if ("CONFIRMED".equals(saved.status())) {
                flash(req, "Đã xác nhận phiếu " + saved.code() + "; tồn kho đã được cộng.");
            } else {
                flash(req, "Đã lưu nháp phiếu " + saved.code() + ".");
            }
            resp.getWriter().write(Json.encode(saved));
        } catch (GoodsReceiptService.NotFoundException e) {
            error(resp, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        } catch (ForbiddenException e) {
            error(resp, HttpServletResponse.SC_FORBIDDEN, e.getMessage());
        } catch (ServiceException e) {
            error(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (SQLException e) {
            log("Không lưu được phiếu nhập kho", e);
            error(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Không thể lưu phiếu nhập. Vui lòng thử lại.");
        }
    }

    private static GoodsReceiptService.Form form(HttpServletRequest req) {
        String[] products = values(req, "productId");
        String[] units = values(req, "unitId");
        String[] quantities = values(req, "quantity");
        String[] lots = values(req, "lotNo");
        String[] expiries = values(req, "expiryDate");
        String[] manufactured = values(req, "manufacturedDate");
        // Client cũ chưa có ô ngày sản xuất vẫn gửi được SKU không quản lý lô.
        if (manufactured.length==0) manufactured=new String[products.length];
        if (units.length != products.length || quantities.length != products.length
                || lots.length != products.length || expiries.length != products.length || manufactured.length != products.length) {
            throw new ServiceException("Dữ liệu dòng hàng không hợp lệ.");
        }
        List<GoodsReceiptService.LineInput> lines = new ArrayList<>();
        for (int i = 0; i < products.length; i++) {
            String at = "Dòng " + (i + 1) + ": ";
            lines.add(new GoodsReceiptService.LineInput(id(products[i], at + "sản phẩm không hợp lệ."),
                    id(units[i], at + "đơn vị không hợp lệ."), quantity(quantities[i], at),
                    lots[i], date(expiries[i], at + "hạn dùng không hợp lệ."),date(manufactured[i],at+"ngày sản xuất không hợp lệ.")));
        }
        long id = longParam(req, "id");
        return new GoodsReceiptService.Form(id > 0 ? id : null,
                id(param(req, "supplierId"), "Nhà cung cấp không hợp lệ."), param(req, "documentNo"),
                date(param(req, "receivedDate"), "Ngày nhập không hợp lệ."),
                id(param(req, "warehouseId"), "Kho nhập không hợp lệ."), param(req, "note"), lines,
                "true".equals(param(req, "confirm")));
    }

    private static String[] values(HttpServletRequest req, String name) {
        String[] values = req.getParameterValues(name);
        return values == null ? new String[0] : values;
    }

    private static Long id(String value, String message) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            long id = Long.parseLong(value.trim());
            if (id <= 0) {
                throw new ServiceException(message);
            }
            return id;
        } catch (NumberFormatException e) {
            throw new ServiceException(message);
        }
    }

    private static BigDecimal quantity(String value, String at) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            throw new ServiceException(at + "số lượng không hợp lệ.");
        }
    }

    private static LocalDate date(String value, String message) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new ServiceException(message);
        }
    }

    private static void error(HttpServletResponse resp, int status, String message) throws IOException {
        resp.setStatus(status);
        resp.getWriter().write(Json.encode(Map.of("error", message)));
    }
}
