package com.oms.service;

import com.oms.config.Db;
import com.oms.dao.AuditLogDao;
import com.oms.dao.PermissionDao;
import com.oms.model.Module;
import com.oms.model.PermissionLevel;
import com.oms.model.Role;
import com.oms.security.PermissionPolicy;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Giữ ma trận phân quyền trong bộ nhớ; nạp lại sau mỗi lần lưu. */
public class PermissionService {

    private static volatile PermissionPolicy cached;

    private final PermissionDao permissionDao = new PermissionDao();
    private final AuditLogDao auditDao = new AuditLogDao();

    public PermissionPolicy policy() throws SQLException {
        PermissionPolicy p = cached;
        if (p == null) {
            p = reload();
        }
        return p;
    }

    public synchronized PermissionPolicy reload() throws SQLException {
        try (Connection conn = Db.getConnection()) {
            cached = new PermissionPolicy(permissionDao.loadMatrix(conn));
            return cached;
        }
    }

    public void save(Map<Role, Map<Module, PermissionLevel>> matrix, long actorId) throws SQLException {
        Db.inTransaction(conn -> {
            PermissionPolicy before = new PermissionPolicy(permissionDao.loadMatrix(conn));
            PermissionPolicy after = new PermissionPolicy(matrix);
            List<String> oldCells = new ArrayList<>();
            List<String> newCells = new ArrayList<>();
            for (Role role : Role.configurable()) {
                for (Module module : Module.values()) {
                    PermissionLevel from = before.levelOf(role, module);
                    PermissionLevel to = after.levelOf(role, module);
                    if (from != to) {
                        String cell = role.getLabel() + " / " + module.getLabel() + ": ";
                        oldCells.add(cell + from.getSymbol());
                        newCells.add(cell + to.getSymbol());
                    }
                }
            }
            permissionDao.replaceMatrix(conn, matrix);
            // Chỉ ghi các ô thay đổi để nhật ký đọc được; không đổi gì thì ghi rõ là không thay đổi
            auditDao.log(conn, actorId, "PERMISSION_UPDATE", "ROLE_PERMISSIONS", null,
                    oldCells.isEmpty() ? "Không có ô nào thay đổi" : oldCells.size() + " ô thay đổi",
                    oldCells.isEmpty() ? null : String.join("\n", oldCells),
                    newCells.isEmpty() ? null : String.join("\n", newCells));
            return null;
        });
        reload();
    }
}
