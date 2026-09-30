package com.oms.web.servlet;

import com.oms.model.Product;
import com.oms.service.ProductForm;
import com.oms.service.ProductService;
import com.oms.service.ServiceException;
import com.oms.service.UnitConversionService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/** Tạo / sửa sản phẩm kèm ảnh (S2-05). Giới hạn multipart rộng hơn 2MB để báo lỗi ảnh quá cỡ dễ hiểu. */
@WebServlet({"/products/new", "/products/edit"})
@MultipartConfig(maxFileSize = 10L * 1024 * 1024, maxRequestSize = 11L * 1024 * 1024)
public class ProductFormServlet extends BaseServlet {

    private final ProductService productService = new ProductService();
    private final UnitConversionService conversionService = new UnitConversionService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ProductForm form = new ProductForm();
        Product product = null;
        try {
            if (isEdit(req)) {
                product = productService.get(longParam(req, "id"), currentUser(req));
                form.setId(product.getId());
                form.setSku(product.getSku());
                form.setName(product.getName());
                form.setCategoryId(product.getCategoryId());
                form.setBaseUnitId(product.getBaseUnitId());
                form.setPackingSpec(product.getPackingSpec() == null ? "" : product.getPackingSpec());
                form.setCostPrice(product.getCostPrice() == null ? "" : product.getCostPrice().toPlainString());
                form.setStatus(product.getStatus().name());
            }
            show(req, resp, form, product);
        } catch (ProductService.NotFoundException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ProductForm form = new ProductForm();
        form.setSku(param(req, "sku"));
        form.setName(param(req, "name"));
        form.setCategoryId(longParam(req, "categoryId"));
        form.setBaseUnitId(longParam(req, "baseUnitId"));
        form.setPackingSpec(param(req, "packingSpec"));
        form.setCostPrice(param(req, "costPrice"));
        form.setStatus(param(req, "status"));
        try {
            byte[] image = readUpload(req.getPart("image"));
            if (isEdit(req)) {
                form.setId(longParam(req, "id"));
                productService.update(form, image, "on".equals(param(req, "removeImage")), currentUser(req));
                flash(req, "Đã cập nhật sản phẩm " + productService.get(form.getId(), currentUser(req)).getSku() + ".");
            } else {
                productService.create(form, image, currentUser(req));
                flash(req, "Đã thêm sản phẩm " + form.getSku().trim().toUpperCase() + ".");
            }
            redirect(req, resp, "/products");
        } catch (ProductService.NotFoundException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (ServiceException e) {
            req.setAttribute("error", e.getMessage());
            try {
                show(req, resp, form, isEdit(req) ? productService.get(form.getId(), currentUser(req)) : null);
            } catch (SQLException ex) {
                throw new ServletException(ex);
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void show(HttpServletRequest req, HttpServletResponse resp, ProductForm form, Product product)
            throws SQLException, ServletException, IOException {
        req.setAttribute("form", form);
        req.setAttribute("product", product);
        req.setAttribute("categories", productService.categories().ordered());
        req.setAttribute("units", productService.units());
        req.setAttribute("statuses", Product.Status.values());
        if (product != null) {
            req.setAttribute("conversions", conversionService.list(product.getId()));
        }
        render(req, resp, "products/form");
    }

    private static boolean isEdit(HttpServletRequest req) {
        return req.getServletPath().equals("/products/edit");
    }

}
