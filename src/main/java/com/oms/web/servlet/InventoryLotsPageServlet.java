package com.oms.web.servlet;

import com.oms.model.Module;
import com.oms.model.Role;
import com.oms.service.PermissionService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/** GHTK-58: giao diện mẫu quản lý lô; không cập nhật cấu hình SKU hoặc xuất kho thật. */
@WebServlet("/inventory/lots")
public class InventoryLotsPageServlet extends BaseServlet {
    private final PermissionService permissionService = new PermissionService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            boolean canWrite = permissionService.policy().canWrite(currentUser(req).getRoles(), Module.INVENTORY);
            req.setAttribute("canWrite", canWrite);
            req.setAttribute("canOverrideExpiry", canWrite && currentUser(req).getRoles().contains(Role.WH_MANAGER));
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        render(req, resp, "inventory/lots");
    }
}
