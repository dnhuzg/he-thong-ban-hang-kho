package com.oms.web.servlet;

import com.oms.model.Module;
import com.oms.service.PermissionService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/** GHTK-51: ghép bản FE khai báo kho vào layout chung; chưa ghi dữ liệu nghiệp vụ. */
@WebServlet("/inventory/warehouses")
public class WarehousePageServlet extends BaseServlet {

    private final PermissionService permissionService = new PermissionService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            req.setAttribute("canWrite", permissionService.policy()
                    .canWrite(currentUser(req).getRoles(), Module.INVENTORY));
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        render(req, resp, "inventory/warehouses");
    }
}
