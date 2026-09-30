package com.oms.web.servlet;

import com.oms.model.Role;
import com.oms.model.User;
import com.oms.service.ServiceException;
import com.oms.service.UserForm;
import com.oms.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;

/** Tạo và sửa tài khoản, gán vai trò và kho / địa bàn (S1-08, S1-09). */
@WebServlet({"/admin/users/new", "/admin/users/edit"})
public class UserFormServlet extends BaseServlet {

    private final UserService userService = new UserService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        UserForm form = new UserForm();
        try {
            if (isEdit(req)) {
                User user = userService.get(longParam(req, "id"));
                form.setId(user.getId());
                form.setFullName(user.getFullName());
                form.setUsername(user.getUsername());
                form.setEmail(user.getEmail());
                form.setPhone(user.getPhone() == null ? "" : user.getPhone());
                form.getRoles().addAll(user.getRoles());
                user.getWarehouses().forEach(w -> form.getWarehouseIds().add(w.getId()));
                user.getAreas().forEach(a -> form.getAreaIds().add(a.getId()));
            }
            show(req, resp, form);
        } catch (UserService.NotFoundException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        UserForm form = readForm(req);
        try {
            if (isEdit(req)) {
                form.setId(longParam(req, "id"));
                form.setUsername(userService.get(form.getId()).getUsername());
                userService.update(form, currentUser(req));
                flash(req, "Đã cập nhật tài khoản " + form.getUsername() + ".");
            } else {
                boolean mailSent = userService.create(form, currentUser(req));
                flash(req, mailSent
                        ? "Đã tạo tài khoản " + form.getUsername() + " và gửi email kích hoạt kèm mật khẩu tạm."
                        : "Đã tạo tài khoản " + form.getUsername() + " nhưng CHƯA gửi được email kích hoạt (xem log máy chủ). "
                                + "Người dùng có thể dùng \"Quên mật khẩu\" để tự đặt mật khẩu.");
            }
            redirect(req, resp, "/admin/users");
        } catch (UserService.NotFoundException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (ServiceException e) {
            req.setAttribute("error", e.getMessage());
            try {
                show(req, resp, form);
            } catch (SQLException ex) {
                throw new ServletException(ex);
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void show(HttpServletRequest req, HttpServletResponse resp, UserForm form)
            throws SQLException, ServletException, IOException {
        req.setAttribute("form", form);
        req.setAttribute("roles", Role.values());
        req.setAttribute("warehouses", userService.warehouses());
        req.setAttribute("areas", userService.areas());
        render(req, resp, "admin/user-form");
    }

    private static boolean isEdit(HttpServletRequest req) {
        return req.getServletPath().equals("/admin/users/edit");
    }

    private static UserForm readForm(HttpServletRequest req) {
        UserForm form = new UserForm();
        form.setFullName(param(req, "fullName"));
        form.setUsername(param(req, "username"));
        form.setEmail(param(req, "email"));
        form.setPhone(param(req, "phone"));
        Set<Role> roles = EnumSet.noneOf(Role.class);
        String[] roleValues = req.getParameterValues("roles");
        if (roleValues != null) {
            for (String r : roleValues) {
                try {
                    roles.add(Role.valueOf(r));
                } catch (IllegalArgumentException ignored) {
                    // giá trị lạ từ client: bỏ qua
                }
            }
        }
        form.setRoles(roles);
        form.setWarehouseIds(readIds(req, "warehouses"));
        form.setAreaIds(readIds(req, "areas"));
        return form;
    }

    private static Set<Long> readIds(HttpServletRequest req, String name) {
        Set<Long> ids = new LinkedHashSet<>();
        String[] values = req.getParameterValues(name);
        if (values != null) {
            for (String v : values) {
                try {
                    ids.add(Long.parseLong(v));
                } catch (NumberFormatException ignored) {
                    // giá trị lạ từ client: bỏ qua
                }
            }
        }
        return ids;
    }
}
