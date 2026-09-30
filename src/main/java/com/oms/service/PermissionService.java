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
            permissionDao.replaceMatrix(conn, matrix);
            auditDao.log(conn, actorId, "PERMISSION_UPDATE", "ROLE_PERMISSIONS", null, null);
            return null;
        });
        reload();
    }
}
