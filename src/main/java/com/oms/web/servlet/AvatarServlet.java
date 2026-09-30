package com.oms.web.servlet;

import com.oms.service.AvatarService;
import com.oms.service.Images;
import com.oms.service.ServiceException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

/**
 * Tải lên / xoá ảnh đại diện (S2-03). Giới hạn multipart để rộng hơn 2MB: tệp quá cỡ vẫn được nhận
 * để báo lỗi dễ hiểu, thay vì Tomcat từ chối cả request (mất luôn token CSRF).
 */
@WebServlet("/account/avatar")
@MultipartConfig(maxFileSize = 10L * 1024 * 1024, maxRequestSize = 11L * 1024 * 1024)
public class AvatarServlet extends BaseServlet {

    private final AvatarService avatarService = new AvatarService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            if ("remove".equals(param(req, "action"))) {
                avatarService.remove(currentUser(req));
                flash(req, "Đã xoá ảnh đại diện.");
            } else {
                avatarService.upload(currentUser(req), readLimited(req.getPart("avatar")));
                flash(req, "Đã cập nhật ảnh đại diện.");
            }
        } catch (ServiceException e) {
            flash(req, e.getMessage());
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        redirect(req, resp, "/account/profile");
    }

    /** Đọc tối đa 2MB + 1 byte: đủ để biết tệp quá cỡ mà không nạp cả tệp lớn vào bộ nhớ. */
    private static byte[] readLimited(Part part) throws IOException {
        if (part == null || part.getSize() == 0) {
            return new byte[0];
        }
        try (InputStream in = part.getInputStream()) {
            return in.readNBytes((int) Images.MAX_BYTES + 1);
        }
    }
}
