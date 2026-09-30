package com.oms.web.servlet;

import com.oms.model.Module;
import com.oms.model.PermissionLevel;
import com.oms.model.Role;
import com.oms.service.PermissionService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.Map;

/** Cấu hình ma trận phân quyền (S1-05). Chỉ Admin — kiểm ở AuthFilter qua AccessRules. */
@WebServlet("/admin/roles")
public class RolesServlet extends BaseServlet {

    private final PermissionService permissionService = new PermissionService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            req.setAttribute("matrix", permissionService.policy().asMatrix());
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        req.setAttribute("roles", Role.configurable());
        req.setAttribute("modules", Module.values());
        req.setAttribute("levels", PermissionLevel.values());
        render(req, resp, "admin/roles");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Map<Role, Map<Module, PermissionLevel>> matrix = new EnumMap<>(Role.class);
        for (Role role : Role.configurable()) {
            Map<Module, PermissionLevel> row = new EnumMap<>(Module.class);
            for (Module module : Module.values()) {
                PermissionLevel level = parseLevel(req.getParameter("perm." + module.name() + "." + role.name()));
                if (level == null) {
                    // Form bị cắt / thiếu ô mà coi là "không có quyền" thì sẽ xoá nhầm cả ma trận
                    flash(req, "Dữ liệu phân quyền gửi lên không đầy đủ nên chưa lưu. Vui lòng tải lại trang và thử lại.");
                    redirect(req, resp, "/admin/roles");
                    return;
                }
                row.put(module, level);
            }
            matrix.put(role, row);
        }
        try {
            permissionService.save(matrix, currentUser(req).getId());
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        flash(req, "Đã lưu phân quyền. Thay đổi có hiệu lực ngay với mọi người dùng.");
        redirect(req, resp, "/admin/roles");
    }

    /** Trả về null khi thiếu ô hoặc giá trị lạ, để từ chối cả lần lưu thay vì đoán. */
    private static PermissionLevel parseLevel(String value) {
        if (value == null) {
            return null;
        }
        try {
            return PermissionLevel.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
