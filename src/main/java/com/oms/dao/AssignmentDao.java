package com.oms.dao;

import com.oms.model.AssignmentChange;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/** Lịch sử phân công người phụ trách đại lý: chỉ có thêm và đọc, không có hàm sửa hay xoá. */
public class AssignmentDao {

    private static final String SELECT = "SELECT a.customer_id, c.code, c.name, f.full_name, t.full_name, a.reason, b.full_name, a.changed_at "
            + "FROM customer_assignments a JOIN customers c ON c.id = a.customer_id "
            + "LEFT JOIN users f ON f.id = a.from_user_id LEFT JOIN users t ON t.id = a.to_user_id "
            + "JOIN users b ON b.id = a.changed_by";

    public void insert(Connection conn, long customerId, Long fromUserId, Long toUserId, String reason, long changedBy) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO customer_assignments "
                + "(customer_id, from_user_id, to_user_id, reason, changed_by) VALUES (?, ?, ?, ?, ?)")) {
            ps.setLong(1, customerId);
            setLong(ps, 2, fromUserId);
            setLong(ps, 3, toUserId);
            ps.setString(4, reason);
            ps.setLong(5, changedBy);
            ps.executeUpdate();
        }
    }

    /** Mới nhất trước. */
    public List<AssignmentChange> listByCustomer(Connection conn, long customerId, int limit) throws SQLException {
        return query(conn, SELECT + " WHERE a.customer_id = ? ORDER BY a.changed_at DESC, a.id DESC LIMIT ?", customerId, limit);
    }

    public List<AssignmentChange> listRecent(Connection conn, int limit) throws SQLException {
        return query(conn, SELECT + " ORDER BY a.changed_at DESC, a.id DESC LIMIT ?", null, limit);
    }

    private List<AssignmentChange> query(Connection conn, String sql, Long customerId, int limit) throws SQLException {
        List<AssignmentChange> changes = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int i = 1;
            if (customerId != null) {
                ps.setLong(i++, customerId);
            }
            ps.setInt(i, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AssignmentChange c = new AssignmentChange();
                    c.setCustomerId(rs.getLong(1));
                    c.setCustomerCode(rs.getString(2));
                    c.setCustomerName(rs.getString(3));
                    c.setFromName(rs.getString(4));
                    c.setToName(rs.getString(5));
                    c.setReason(rs.getString(6));
                    c.setChangedByName(rs.getString(7));
                    Timestamp at = rs.getTimestamp(8);
                    c.setChangedAt(at == null ? null : at.toLocalDateTime());
                    changes.add(c);
                }
            }
        }
        return changes;
    }

    private static void setLong(PreparedStatement ps, int index, Long value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.BIGINT);
        } else {
            ps.setLong(index, value);
        }
    }
}
