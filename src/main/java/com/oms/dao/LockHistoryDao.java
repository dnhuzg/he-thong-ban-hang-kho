package com.oms.dao;

import com.oms.model.LockChange;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/** Lịch sử khoá / mở giao dịch với đại lý: chỉ có thêm và đọc, không có hàm sửa hay xoá. */
public class LockHistoryDao {

    public void insert(Connection conn, long customerId, boolean lock, String reason, long changedBy) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO customer_lock_history (customer_id, action, reason, changed_by) VALUES (?, ?, ?, ?)")) {
            ps.setLong(1, customerId);
            ps.setString(2, lock ? "LOCK" : "UNLOCK");
            ps.setString(3, reason);
            ps.setLong(4, changedBy);
            ps.executeUpdate();
        }
    }

    /** Mới nhất trước. */
    public List<LockChange> list(Connection conn, long customerId, int limit) throws SQLException {
        List<LockChange> changes = new ArrayList<>();
        String sql = "SELECT h.action, h.reason, u.full_name, h.changed_at FROM customer_lock_history h "
                + "JOIN users u ON u.id = h.changed_by WHERE h.customer_id = ? ORDER BY h.changed_at DESC, h.id DESC LIMIT ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, customerId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LockChange c = new LockChange();
                    c.setLock("LOCK".equals(rs.getString(1)));
                    c.setReason(rs.getString(2));
                    c.setChangedByName(rs.getString(3));
                    Timestamp at = rs.getTimestamp(4);
                    c.setChangedAt(at == null ? null : at.toLocalDateTime());
                    changes.add(c);
                }
            }
        }
        return changes;
    }
}
