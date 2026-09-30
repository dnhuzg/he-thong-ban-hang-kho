package com.oms.web.servlet;

import com.oms.model.User;
import com.oms.service.Images;
import com.oms.web.SessionKeys;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;

abstract class BaseServlet extends HttpServlet {

    protected void render(HttpServletRequest req, HttpServletResponse resp, String view)
            throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/views/" + view + ".jsp").forward(req, resp);
    }

    protected void redirect(HttpServletRequest req, HttpServletResponse resp, String path) throws IOException {
        resp.sendRedirect(req.getContextPath() + path);
    }

    protected User currentUser(HttpServletRequest req) {
        return (User) req.getAttribute(SessionKeys.CURRENT_USER);
    }

    /** Thông báo hiển thị một lần ở trang kế tiếp sau redirect. */
    protected void flash(HttpServletRequest req, String message) {
        req.getSession().setAttribute(SessionKeys.FLASH, message);
    }

    protected static String param(HttpServletRequest req, String name) {
        String value = req.getParameter(name);
        return value == null ? "" : value.trim();
    }

    protected static long longParam(HttpServletRequest req, String name) {
        try {
            return Long.parseLong(param(req, name));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    protected static int intParam(HttpServletRequest req, String name, int defaultValue) {
        try {
            return Integer.parseInt(param(req, name));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /** Đọc tệp tải lên tối đa 2MB + 1 byte: đủ để biết tệp quá cỡ mà không nạp cả tệp lớn vào bộ nhớ. */
    protected static byte[] readUpload(Part part) throws IOException {
        if (part == null || part.getSize() == 0) {
            return new byte[0];
        }
        try (InputStream in = part.getInputStream()) {
            return in.readNBytes((int) Images.MAX_BYTES + 1);
        }
    }
}
