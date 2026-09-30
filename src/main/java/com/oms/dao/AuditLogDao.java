package com.oms.dao;

import com.oms.model.AuditEntry;
import com.oms.model.PageResult;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AuditLogDao {

    /** Điều kiện lọc nhật ký (S2-04); trường null nghĩa là không lọc theo trường đó. */
    public record Filter(Long actorId, String targetType, LocalDate from, LocalDate to) {
    }

    public void log(Connection conn, Long actorId, String action, String targetType, Long targetId, String detail)
            throws SQLException {
        log(conn, actorId, action, targetType, targetId, detail, null, null);
    }

    public void log(Connection conn, Long actorId, String action, String targetType, Long targetId, String detail,
                    String oldValue, String newValue) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO audit_logs (actor_id, action, target_type, target_id, detail, old_value, new_value) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
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
            ps.setString(6, oldValue);
            ps.setString(7, newValue);
            ps.executeUpdate();
        }
    }

    public PageResult<AuditEntry> search(Connection conn, Filter filter, int page, int pageSize) throws SQLException {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (filter.actorId() != null) {
            where.append(" AND a.actor_id = ?");
            params.add(filter.actorId());
        }
        if (filter.targetType() != null) {
            where.append(" AND a.target_type = ?");
            params.add(filter.targetType());
        }
        if (filter.from() != null) {
            where.append(" AND a.created_at >= ?");
            params.add(Timestamp.valueOf(filter.from().atStartOfDay()));
        }
        if (filter.to() != null) {
            // Tính trọn ngày "đến": nhỏ hơn 0 giờ của ngày hôm sau
            where.append(" AND a.created_at < ?");
            params.add(Timestamp.valueOf(filter.to().plusDays(1).atStartOfDay()));
        }

        long total;
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM audit_logs a" + where)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                total = rs.getLong(1);
            }
        }

        String sql = "SELECT a.id, a.created_at, a.action, a.target_type, a.target_id, a.detail, a.old_value, a.new_value, "
                + "u.full_name, u.username, u.avatar_path, t.username AS target_name "
                + "FROM audit_logs a LEFT JOIN users u ON u.id = a.actor_id "
                + "LEFT JOIN users t ON a.target_type = 'USER' AND t.id = a.target_id"
                + where + " ORDER BY a.created_at DESC, a.id DESC LIMIT ? OFFSET ?";
        List<AuditEntry> items = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, params);
            ps.setInt(params.size() + 1, pageSize);
            ps.setInt(params.size() + 2, (page - 1) * pageSize);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(map(rs));
                }
            }
        }
        return new PageResult<>(items, page, pageSize, total);
    }

    private static AuditEntry map(ResultSet rs) throws SQLException {
        AuditEntry e = new AuditEntry();
        e.setId(rs.getLong("id"));
        Timestamp created = rs.getTimestamp("created_at");
        e.setCreatedAt(created == null ? null : created.toLocalDateTime());
        e.setAction(rs.getString("action"));
        e.setTargetType(rs.getString("target_type"));
        long targetId = rs.getLong("target_id");
        e.setTargetId(rs.wasNull() ? null : targetId);
        e.setTargetName(rs.getString("target_name"));
        e.setDetail(rs.getString("detail"));
        e.setOldValue(rs.getString("old_value"));
        e.setNewValue(rs.getString("new_value"));
        e.setActorName(rs.getString("full_name"));
        e.setActorUsername(rs.getString("username"));
        e.setActorAvatarPath(rs.getString("avatar_path"));
        return e;
    }

    private static void bind(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }
}
