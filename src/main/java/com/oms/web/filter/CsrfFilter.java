package com.oms.web.filter;

import com.oms.security.Tokens;
import com.oms.web.SessionKeys;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/** Mọi form POST phải gửi kèm _csrf khớp với token trong session. */
public class CsrfFilter implements Filter {

    public static final String PARAM = "_csrf";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String path = req.getRequestURI().substring(req.getContextPath().length());
        if (path.startsWith("/assets/")) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = req.getSession(true);
        String token = (String) session.getAttribute(SessionKeys.CSRF_TOKEN);

        if ("POST".equalsIgnoreCase(req.getMethod())) {
            if (token == null || !Tokens.constantTimeEquals(token, req.getParameter(PARAM))) {
                // Thường do phiên đã hết hạn khi form còn mở: quay về đăng nhập thay vì báo lỗi khó hiểu
                if (session.getAttribute(SessionKeys.USER_ID) == null) {
                    resp.sendRedirect(req.getContextPath() + "/login?expired=1");
                } else {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                }
                return;
            }
        }

        if (token == null) {
            token = Tokens.random();
            session.setAttribute(SessionKeys.CSRF_TOKEN, token);
        }
        req.setAttribute(SessionKeys.CSRF_TOKEN, token);
        chain.doFilter(request, response);
    }
}
