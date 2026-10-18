package com.oms.support;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/** Request/response/session trong bộ nhớ để kiểm thử hành vi Servlet mà không chạy Tomcat. */
public class HttpFixture {
    public final Map<String,String> parameters = new HashMap<>();
    public final java.io.StringWriter body = new java.io.StringWriter();
    public final java.io.ByteArrayOutputStream binaryBody = new java.io.ByteArrayOutputStream();
    public String contentType;
    public final Map<String, Object> attributes = new HashMap<>();
    public final Map<String, Object> sessionAttributes = new HashMap<>();
    public final Map<String, String> headers = new HashMap<>();
    public boolean sessionPresent = true;
    public boolean invalidated;
    public boolean sessionIdValid = true;
    public String path = "/home";
    public String method = "GET";
    public int status = 200;
    public String redirect;

    public final HttpSession session = proxy(HttpSession.class, (name, args) -> {
        return switch (name) {
            case "getAttribute" -> sessionAttributes.get(args[0]);
            case "setAttribute" -> { sessionAttributes.put((String) args[0], args[1]); yield null; }
            case "invalidate" -> { invalidated = true; sessionPresent = false; sessionAttributes.clear(); yield null; }
            case "getId" -> "test-session";
            default -> null;
        };
    });

    public final HttpServletRequest request = proxy(HttpServletRequest.class, (name, args) -> {
        return switch (name) {
            case "getContextPath" -> "/oms";
            case "getRequestURI" -> "/oms" + path;
            case "getServletPath" -> path;
            case "getMethod" -> method;
            case "getParameter" -> parameters.get(args[0]);
            case "getSession" -> sessionPresent ? session : null;
            case "getAttribute" -> attributes.get(args[0]);
            case "setAttribute" -> { attributes.put((String) args[0], args[1]); yield null; }
            case "getRequestedSessionId" -> "test-session";
            case "isRequestedSessionIdValid" -> sessionIdValid;
            default -> null;
        };
    });

    public final HttpServletResponse response = proxy(HttpServletResponse.class, (name, args) -> {
        return switch (name) {
            case "setHeader" -> { headers.put((String) args[0], (String) args[1]); yield null; }
            case "sendRedirect" -> { redirect = (String) args[0]; status = 302; yield null; }
            case "sendError", "setStatus" -> { status = (Integer) args[0]; yield null; }
            case "getStatus" -> status;
            case "getWriter" -> new java.io.PrintWriter(body);
            case "getOutputStream" -> new jakarta.servlet.ServletOutputStream() {
                public boolean isReady() { return true; }
                public void setWriteListener(jakarta.servlet.WriteListener listener) { }
                public void write(int value) { binaryBody.write(value); }
            };
            case "setContentType" -> { contentType=(String)args[0]; yield null; }
            case "getHeader" -> headers.get(args[0]);
            default -> null;
        };
    });

    private interface Call {
        Object run(String name, Object[] args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Call call) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> call.run(method.getName(), args));
    }
}
