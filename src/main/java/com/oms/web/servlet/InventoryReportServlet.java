package com.oms.web.servlet;

import com.oms.model.InventoryReport;
import com.oms.model.InventorySkuAgeRow;
import com.oms.model.InventoryValueRow;
import com.oms.model.Module;
import com.oms.model.Role;
import com.oms.model.User;
import com.oms.security.PermissionPolicy;
import com.oms.service.InventoryReportService;
import com.oms.service.PermissionService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

@WebServlet(urlPatterns = {"/reports/inventory", "/reports/inventory/export"})
public class InventoryReportServlet extends BaseServlet {

    private final InventoryReportService service = new InventoryReportService();
    private final PermissionService permissionService = new PermissionService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = currentUser(req);
        try {
            if (!canView(user)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }

        Long warehouseId = optionalPositiveLong(req.getParameter("warehouseId"));
        Long categoryId = optionalPositiveLong(req.getParameter("categoryId"));
        // So sánh bằng equals: warehouseId / categoryId là Long và bằng null khi không lọc
        if (Long.valueOf(INVALID).equals(warehouseId) || Long.valueOf(INVALID).equals(categoryId)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Mã kho hoặc mã nhóm hàng không hợp lệ.");
            return;
        }

        try {
            if ("/reports/inventory/export".equals(req.getServletPath())) {
                byte[] data = service.exportXlsx(warehouseId, categoryId);
                resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
                resp.setHeader("Content-Disposition",
                        "attachment; filename=\"bao-cao-ton-kho-vong-quay.xlsx\"");
                resp.setContentLengthLong(data.length);
                resp.getOutputStream().write(data);
                return;
            }

            InventoryReport report = service.report(warehouseId, categoryId);
            writeJson(resp, report);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private boolean canView(User user) throws SQLException {
        if (user == null) {
            return false;
        }
        boolean reportPermission = permissionService.policy().canRead(user.getRoles(), Module.REPORT);
        return reportPermission
                && (user.getRoles().contains(Role.WH_MANAGER) || user.getRoles().contains(Role.ADMIN));
    }

    private void writeJson(HttpServletResponse resp, InventoryReport report) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());

        StringBuilder json = new StringBuilder(4096);
        json.append("{\"slowMovingThresholdDays\":")
                .append(report.slowMovingThresholdDays())
                .append(",\"valueRows\":[");
        for (int i = 0; i < report.valueRows().size(); i++) {
            if (i > 0) json.append(',');
            InventoryValueRow row = report.valueRows().get(i);
            json.append("{\"warehouseId\":").append(row.warehouseId())
                    .append(",\"warehouseName\":\"").append(json(row.warehouseName()))
                    .append("\",\"categoryId\":").append(row.categoryId())
                    .append(",\"categoryName\":\"").append(json(row.categoryName()))
                    .append("\",\"quantity\":").append(row.quantity())
                    .append(",\"stockValue\":").append(row.stockValue())
                    .append('}');
        }
        json.append("],\"skuAgeRows\":[");
        for (int i = 0; i < report.skuAgeRows().size(); i++) {
            if (i > 0) json.append(',');
            InventorySkuAgeRow row = report.skuAgeRows().get(i);
            json.append("{\"productId\":").append(row.productId())
                    .append(",\"sku\":\"").append(json(row.sku()))
                    .append("\",\"productName\":\"").append(json(row.productName()))
                    .append("\",\"quantity\":").append(row.quantity())
                    .append(",\"averageAgeDays\":").append(row.averageAgeDays())
                    .append(",\"slowMoving\":").append(row.slowMoving())
                    .append('}');
        }
        json.append("]}");

        resp.getWriter().write(json.toString());
    }

    private static final long INVALID = -1;

    private static Long optionalPositiveLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            long number = Long.parseLong(value);
            return number > 0 ? number : INVALID;
        } catch (NumberFormatException e) {
            return INVALID;
        }
    }

    private static String json(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '\\' -> out.append("\\\\");
                case '"' -> out.append("\\\"");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        out.append(String.format("\\u%04x", (int) ch));
                    } else {
                        out.append(ch);
                    }
                }
            }
        }
        return out.toString();
    }
}