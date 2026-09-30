package com.oms.web.servlet;

import com.oms.model.Module;
import com.oms.service.CategoryService;
import com.oms.service.PermissionService;
import com.oms.service.ServiceException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/** Nhóm hàng nhiều cấp (S2-06): xem cây, thêm / đổi tên / xoá nhóm, chuyển sản phẩm giữa các nhóm. */
@WebServlet({"/products/categories", "/products/categories/save", "/products/categories/delete",
        "/products/categories/move"})
public class CategoryServlet extends BaseServlet {

    private final CategoryService categoryService = new CategoryService();
    private final PermissionService permissionService = new PermissionService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!req.getServletPath().equals("/products/categories")) {
            redirect(req, resp, "/products/categories");
            return;
        }
        try {
            req.setAttribute("categories", categoryService.tree().ordered());
            req.setAttribute("canWrite", permissionService.policy().canWrite(currentUser(req).getRoles(), Module.PRODUCT));
            req.setAttribute("canViewPriceLists", !permissionService.policy().isOwnScopeOnly(currentUser(req).getRoles(), Module.PRODUCT));
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        render(req, resp, "products/categories");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            switch (req.getServletPath()) {
                case "/products/categories/save" -> {
                    long id = longParam(req, "id");
                    if (id > 0) {
                        categoryService.rename(id, param(req, "name"), currentUser(req));
                        flash(req, "Đã đổi tên nhóm hàng.");
                    } else {
                        long parentId = longParam(req, "parentId");
                        categoryService.create(parentId > 0 ? parentId : null, param(req, "name"), currentUser(req));
                        flash(req, "Đã thêm nhóm hàng \"" + param(req, "name") + "\".");
                    }
                }
                case "/products/categories/delete" -> {
                    categoryService.delete(longParam(req, "id"), currentUser(req));
                    flash(req, "Đã xoá nhóm hàng.");
                }
                case "/products/categories/move" -> {
                    int moved = categoryService.moveProducts(longParam(req, "id"), longParam(req, "targetId"), currentUser(req));
                    flash(req, "Đã chuyển " + moved + " sản phẩm sang nhóm mới.");
                }
                default -> {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
            }
        } catch (ServiceException e) {
            flash(req, e.getMessage());
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        redirect(req, resp, "/products/categories");
    }
}
