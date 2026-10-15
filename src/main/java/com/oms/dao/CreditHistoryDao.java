package com.oms.dao;

import com.oms.model.CreditChange;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/** Lịch sử đổi hạn mức công nợ: chỉ có thêm và đọc, không có hàm sửa hay xoá. */
public class CreditHistoryDao {

    private static final String SELECT = "SELECT h.old_limit, h.new_limit, h.old_days, h.new_days, h.reason, u.full_name, "
            + "h.changed_at, h.customer_id, c.code, c.name FROM customer_credit_history h JOIN users u ON u.id = h.changed_by "
            + "JOIN customers c ON c.id = h.customer_id";

    public void insert(Connection conn, long customerId, BigDecimal oldLimit, BigDecimal newLimit, int oldDays, int newDays,
                       String reason, long changedBy) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO customer_credit_history "
                + "(customer_id, old_limit, new_limit, old_days, new_days, reason, changed_by) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            ps.setLong(1, customerId);
            ps.setBigDecimal(2, oldLimit);
            ps.setBigDecimal(3, newLimit);
            ps.setInt(4, oldDays);
            ps.setInt(5, newDays);
            ps.setString(6, reason);
            ps.setLong(7, changedBy);
            ps.executeUpdate();
        }
    }

    /** Mới nhất trước. */
    public List<CreditChange> list(Connection conn, long customerId, int limit) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT + " WHERE h.customer_id = ? "
                + "ORDER BY h.changed_at DESC, h.id DESC LIMIT ?")) {
            ps.setLong(1, customerId);
            ps.setInt(2, limit);
            return query(ps);
        }
    }

    /** Các lần đổi mới nhất của mọi đại lý, cho trang chủ của người quản lý hạn mức. */
    public List<CreditChange> recent(Connection conn, int limit) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT + " ORDER BY h.changed_at DESC, h.id DESC LIMIT ?")) {
            ps.setInt(1, limit);
            return query(ps);
        }
    }

    private static List<CreditChange> query(PreparedStatement ps) throws SQLException {
        List<CreditChange> changes = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                CreditChange c = new CreditChange();
                c.setOldLimit(rs.getBigDecimal(1));
                c.setNewLimit(rs.getBigDecimal(2));
                c.setOldDays(rs.getInt(3));
                c.setNewDays(rs.getInt(4));
                c.setReason(rs.getString(5));
                c.setChangedByName(rs.getString(6));
                Timestamp at = rs.getTimestamp(7);
                c.setChangedAt(at == null ? null : at.toLocalDateTime());
                c.setCustomerId(rs.getLong(8));
                c.setCustomerCode(rs.getString(9));
                c.setCustomerName(rs.getString(10));
                changes.add(c);
            }
        }
        return changes;
    }
}
