package com.oms.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

public class AuditLogDao {

    public void log(Connection conn, Long actorId, String action, String targetType, Long targetId, String detail)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO audit_logs (actor_id, action, target_type, target_id, detail) VALUES (?, ?, ?, ?, ?)")) {
            if (actorId == null) {
                ps.setNull(1, Types.BIGINT);
            } else {
                ps.setLong(1, actorId);
            }
            ps.setString(2, action);
            ps.setString(3, targetType);
            if (targetId == null) {
                ps.setNull(4, Types.BIGINT);
            } else {
                ps.setLong(4, targetId);
            }
            ps.setString(5, detail);
            ps.executeUpdate();
        }
    }
}
