package com.oms.web.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** UTF-8 cho mọi request/response để hiển thị đúng tiếng Việt, kèm vài header bảo mật cơ bản. */
public class EncodingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        HttpServletResponse resp = (HttpServletResponse) response;
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("Referrer-Policy", "same-origin");
        // Bắt trình duyệt hỏi lại máy chủ (ETag) trước khi dùng CSS/JS đã lưu, để sửa giao diện có hiệu lực ngay
        HttpServletRequest req = (HttpServletRequest) request;
        if (req.getRequestURI().startsWith(req.getContextPath() + "/assets/")) {
            resp.setHeader("Cache-Control", "no-cache");
        }
        chain.doFilter(request, response);
    }
}
