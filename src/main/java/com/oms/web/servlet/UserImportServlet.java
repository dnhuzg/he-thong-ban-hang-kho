package com.oms.web.servlet;

import com.oms.model.ImportBatch;
import com.oms.model.UserImportRow;
import com.oms.service.Excel;
import com.oms.service.ServiceException;
import com.oms.service.UserImportService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;

/** Nhập người dùng hàng loạt từ Excel (S2-01): tải tệp mẫu, xem trước từng dòng, rồi nhập các dòng hợp lệ. */
@WebServlet({"/admin/users/import", "/admin/users/import/apply", "/admin/users/import/template"})
@MultipartConfig(maxFileSize = Excel.MAX_BYTES + 1024, maxRequestSize = Excel.MAX_BYTES + 4096)
public class UserImportServlet extends BaseServlet {

    private static final String SESSION_KEY = "userImport";
    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final UserImportService importService = new UserImportService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getServletPath().equals("/admin/users/import/template")) {
            resp.setContentType(XLSX);
            resp.setHeader("Content-Disposition", "attachment; filename=\"mau-nhap-nguoi-dung.xlsx\"");
            try {
                resp.getOutputStream().write(importService.template());
            } catch (SQLException e) {
                throw new ServletException(e);
            }
            return;
        }
        show(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession();
        try {
            if (req.getServletPath().equals("/admin/users/import/apply")) {
                apply(req, session);
            } else {
                var part = req.getPart("file");
                String name = part == null || part.getSubmittedFileName() == null ? "" : part.getSubmittedFileName();
                session.setAttribute(SESSION_KEY, importService.preview(readUpload(part, Excel.MAX_BYTES), name));
            }
        } catch (ServiceException e) {
            flash(req, e.getMessage());
        } catch (IllegalStateException e) {
            flash(req, "Tệp tối đa 5 MB. Hãy chia nhỏ tệp rồi nhập nhiều lần.");
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        redirect(req, resp, "/admin/users/import");
    }

    @SuppressWarnings("unchecked")
    private void show(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        var batch = (ImportBatch<UserImportRow>) req.getSession().getAttribute(SESSION_KEY);
        req.setAttribute("batch", batch);
        if (batch != null && batch.isApplied()) {
            req.setAttribute("emailsNotSent", importService.emailsNotSent(batch));
        }
        render(req, resp, "admin/users-import");
    }

    @SuppressWarnings("unchecked")
    private void apply(HttpServletRequest req, HttpSession session) throws SQLException {
        var batch = (ImportBatch<UserImportRow>) session.getAttribute(SESSION_KEY);
        if (batch == null || !batch.getToken().equals(param(req, "token"))) {
            throw new ServiceException("Bản xem trước đã cũ hoặc không còn. Vui lòng chọn lại tệp.");
        }
        importService.apply(batch, currentUser(req));
        flash(req, "Đã nhập xong: tạo mới " + batch.getCreatedCount() + " tài khoản, bỏ qua " + batch.getSkippedCount() + " dòng lỗi.");
    }
}
