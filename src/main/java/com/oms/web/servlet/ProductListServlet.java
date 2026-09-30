package com.oms.web.servlet;

import com.oms.model.Module;
import com.oms.model.Product;
import com.oms.service.PermissionService;
import com.oms.service.ProductService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/** Danh sách sản phẩm (S2-05): tìm theo SKU / tên, lọc nhóm hàng (gồm nhóm con) và trạng thái. */
@WebServlet("/products")
public class ProductListServlet extends BaseServlet {

    private final ProductService productService = new ProductService();
    private final PermissionService permissionService = new PermissionService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String keyword = param(req, "q");
        long categoryId = longParam(req, "category");
        Product.Status status = null;
        try {
            status = param(req, "status").isEmpty() ? null : Product.Status.valueOf(param(req, "status"));
        } catch (IllegalArgumentException ignored) {
            // giá trị lạ từ URL: coi như không lọc
        }
        try {
            req.setAttribute("result", productService.search(keyword, categoryId, status, intParam(req, "page", 1), currentUser(req)));
            req.setAttribute("categories", productService.categories().ordered());
            req.setAttribute("canWrite", permissionService.policy().canWrite(currentUser(req).getRoles(), Module.PRODUCT));
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        req.setAttribute("q", keyword);
        req.setAttribute("selectedCategory", categoryId);
        req.setAttribute("selectedStatus", status == null ? "" : status.name());
        req.setAttribute("statuses", Product.Status.values());
        render(req, resp, "products/list");
    }
}
