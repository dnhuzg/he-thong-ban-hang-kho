package com.oms.web.servlet;

import com.oms.model.Module;
import com.oms.model.Supplier;
import com.oms.service.PermissionService;
import com.oms.service.SupplierService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/** Danh sách nhà cung cấp (S2-09): tìm theo mã / tên / MST, lọc trạng thái. */
@WebServlet("/suppliers")
public class SupplierListServlet extends BaseServlet {

    private final SupplierService supplierService = new SupplierService();
    private final PermissionService permissionService = new PermissionService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String keyword = param(req, "q");
        Supplier.Status status = null;
        try {
            status = param(req, "status").isEmpty() ? null : Supplier.Status.valueOf(param(req, "status"));
        } catch (IllegalArgumentException ignored) {
            // giá trị lạ từ URL: coi như không lọc
        }
        try {
            req.setAttribute("result", supplierService.search(keyword, status, intParam(req, "page", 1)));
            req.setAttribute("canWrite", permissionService.policy().canWrite(currentUser(req).getRoles(), Module.INVENTORY));
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        req.setAttribute("q", keyword);
        req.setAttribute("selectedStatus", status == null ? "" : status.name());
        req.setAttribute("statuses", Supplier.Status.values());
        render(req, resp, "suppliers/list");
    }
}
