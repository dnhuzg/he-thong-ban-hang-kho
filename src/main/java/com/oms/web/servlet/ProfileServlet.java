package com.oms.web.servlet;

import com.oms.service.PhoneNumbers;
import com.oms.service.ServiceException;
import com.oms.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/** Hồ sơ cá nhân (S2-02): xem thông tin, tự sửa họ tên và số điện thoại. */
@WebServlet("/account/profile")
public class ProfileServlet extends BaseServlet {

    private final UserService userService = new UserService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("fullName", currentUser(req).getFullName());
        req.setAttribute("phone", currentUser(req).getPhone());
        show(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Chỉ đọc hai trường được phép sửa; tham số khác (vai trò, kho...) gửi kèm đều bị bỏ qua
        String fullName = param(req, "fullName");
        String phone = param(req, "phone");
        try {
            userService.updateOwnProfile(currentUser(req), fullName, phone);
            flash(req, "Đã cập nhật hồ sơ cá nhân.");
            redirect(req, resp, "/account/profile");
        } catch (ServiceException e) {
            req.setAttribute("error", e.getMessage());
            req.setAttribute("fullName", fullName);
            req.setAttribute("phone", phone);
            show(req, resp);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void show(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("phoneHint", PhoneNumbers.FORMAT_HINT);
        render(req, resp, "account/profile");
    }
}
