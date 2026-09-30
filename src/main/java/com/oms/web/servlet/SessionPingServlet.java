package com.oms.web.servlet;

import com.oms.web.SessionKeys;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * app.js gọi định kỳ khi người dùng đang gõ / thao tác trên trang mà chưa gửi request nào,
 * để phiên không hết hạn giữa chừng (S1-02). Đi qua AuthFilter nên phiên được gia hạn.
 */
@WebServlet("/session/ping")
public class SessionPingServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        resp.setHeader("Cache-Control", "no-store");
        // Sau khi đăng nhập lại ở tab khác, form ở tab cũ cần token CSRF của phiên mới mới lưu được
        resp.setHeader("X-CSRF-Token", (String) req.getAttribute(SessionKeys.CSRF_TOKEN));
        resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }
}
