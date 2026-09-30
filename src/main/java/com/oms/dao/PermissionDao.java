package com.oms.dao;

import com.oms.model.Module;
import com.oms.model.PermissionLevel;
import com.oms.model.Role;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.Map;

public class PermissionDao {

    public Map<Role, Map<Module, PermissionLevel>> loadMatrix(Connection conn) throws SQLException {
        Map<Role, Map<Module, PermissionLevel>> matrix = new EnumMap<>(Role.class);
        try (PreparedStatement ps = conn.prepareStatement("SELECT role_code, module_code, level FROM role_permissions");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Role role = Role.valueOf(rs.getString(1));
                Module module = Module.valueOf(rs.getString(2));
                matrix.computeIfAbsent(role, r -> new EnumMap<>(Module.class))
                        .put(module, PermissionLevel.valueOf(rs.getString(3)));
            }
        }
        return matrix;
    }

    /** Ghi đè toàn bộ ma trận của các vai trò cấu hình được. NONE không lưu. */
    public void replaceMatrix(Connection conn, Map<Role, Map<Module, PermissionLevel>> matrix) throws SQLException {
        try (PreparedStatement del = conn.prepareStatement("DELETE FROM role_permissions WHERE role_code <> 'ADMIN'")) {
            del.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO role_permissions (role_code, module_code, level) VALUES (?, ?, ?)")) {
            for (Map.Entry<Role, Map<Module, PermissionLevel>> row : matrix.entrySet()) {
                for (Map.Entry<Module, PermissionLevel> cell : row.getValue().entrySet()) {
                    if (cell.getValue() == PermissionLevel.NONE) {
                        continue;
                    }
                    ps.setString(1, row.getKey().name());
                    ps.setString(2, cell.getKey().name());
                    ps.setString(3, cell.getValue().name());
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }
    }
}
