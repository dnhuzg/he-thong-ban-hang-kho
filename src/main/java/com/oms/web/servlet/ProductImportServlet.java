package com.oms.web.servlet;

import com.oms.model.ImportBatch;
import com.oms.model.ProductImportRow;
import com.oms.service.Excel;
import com.oms.service.ProductImportService;
import com.oms.service.ServiceException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;

/** Nhập sản phẩm hàng loạt từ Excel (S2-08): tải tệp mẫu, xem trước từng dòng, rồi nhập các dòng hợp lệ. */
@WebServlet({"/products/import", "/products/import/apply", "/products/import/template"})
@MultipartConfig(maxFileSize = Excel.MAX_BYTES + 1024, maxRequestSize = Excel.MAX_BYTES + 4096)
public class ProductImportServlet extends BaseServlet {

    private static final String SESSION_KEY = "productImport";
    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ProductImportService importService = new ProductImportService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getServletPath().equals("/products/import/template")) {
            resp.setContentType(XLSX);
            resp.setHeader("Content-Disposition", "attachment; filename=\"mau-nhap-san-pham.xlsx\"");
            resp.getOutputStream().write(importService.template());
            return;
        }
        show(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession();
        try {
            if (req.getServletPath().equals("/products/import/apply")) {
                apply(req, session);
            } else {
                var part = req.getPart("file");
                String name = part == null || part.getSubmittedFileName() == null ? "" : part.getSubmittedFileName();
                session.setAttribute(SESSION_KEY, importService.preview(readUpload(part, Excel.MAX_BYTES), name, currentUser(req)));
            }
        } catch (ServiceException e) {
            flash(req, e.getMessage());
        } catch (IllegalStateException e) {
            flash(req, "Tệp tối đa 5 MB. Hãy chia nhỏ tệp rồi nhập nhiều lần.");
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        redirect(req, resp, "/products/import");
    }

    @SuppressWarnings("unchecked")
    private void apply(HttpServletRequest req, HttpSession session) throws SQLException {
        var batch = (ImportBatch<ProductImportRow>) session.getAttribute(SESSION_KEY);
        if (batch == null || !batch.getToken().equals(param(req, "token"))) {
            throw new ServiceException("Bản xem trước đã cũ hoặc không còn. Vui lòng chọn lại tệp.");
        }
        importService.apply(batch, currentUser(req));
        flash(req, "Đã nhập xong: tạo mới " + batch.getCreatedCount() + ", cập nhật " + batch.getUpdatedCount() + ".");
    }

    private void show(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("batch", req.getSession().getAttribute(SESSION_KEY));
        render(req, resp, "products/import");
    }
}
