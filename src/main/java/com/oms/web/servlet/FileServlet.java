package com.oms.web.servlet;

import com.oms.service.FileStorage;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

/** Phục vụ ảnh trong thư mục upload cho người đã đăng nhập (kiểm tra ở AuthFilter qua AccessRules). */
@WebServlet("/files/*")
public class FileServlet extends BaseServlet {

    // Chỉ các thư mục ảnh và tên tệp do hệ thống sinh ra; mọi đường dẫn khác trả 404
    private static final Pattern ALLOWED = Pattern.compile("^(avatars|products|receipts)/[0-9a-f-]{36}(-64)?\\.(png|jpg)$");

    private final FileStorage storage = new FileStorage();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String relative = req.getPathInfo() == null ? "" : req.getPathInfo().substring(1);
        if (!ALLOWED.matcher(relative).matches()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        Path file = storage.resolve(relative);
        if (!Files.isRegularFile(file)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        resp.setContentType(relative.endsWith(".png") ? "image/png" : "image/jpeg");
        resp.setHeader("Cache-Control", "private, max-age=86400");
        resp.setContentLengthLong(Files.size(file));
        Files.copy(file, resp.getOutputStream());
    }
}
