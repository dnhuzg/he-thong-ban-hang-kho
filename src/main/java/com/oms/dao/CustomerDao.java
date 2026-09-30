package com.oms.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Đại lý — hiện chỉ phục vụ cảnh báo bàn giao khi khoá nhân viên (S1-10). EP-03 sẽ mở rộng. */
public class CustomerDao {

    /** Đại lý thuộc một nhân viên đã bị khoá: cần bàn giao cho người khác. */
    public record HandoverItem(String customerCode, String customerName, String repFullName, String repUsername) {
        public String getCustomerCode() { return customerCode; }
        public String getCustomerName() { return customerName; }
        public String getRepFullName() { return repFullName; }
        public String getRepUsername() { return repUsername; }
    }

    /** Số đại lý đang phụ trách theo từng nhân viên (chỉ trả về người có ít nhất một đại lý). */
    public Map<Long, Long> countBySalesReps(Connection conn, Collection<Long> userIds) throws SQLException {
        Map<Long, Long> counts = new HashMap<>();
        if (userIds.isEmpty()) {
            return counts;
        }
        String placeholders = String.join(",", Collections.nCopies(userIds.size(), "?"));
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT sales_rep_id, COUNT(*) FROM customers WHERE sales_rep_id IN (" + placeholders + ") GROUP BY sales_rep_id")) {
            int i = 1;
            for (Long id : userIds) {
                ps.setLong(i++, id);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    counts.put(rs.getLong(1), rs.getLong(2));
                }
            }
        }
        return counts;
    }

    public List<String> namesBySalesRep(Connection conn, long userId) throws SQLException {
        List<String> names = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT name FROM customers WHERE sales_rep_id = ? ORDER BY name")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    names.add(rs.getString(1));
                }
            }
        }
        return names;
    }

    public List<HandoverItem> findNeedingHandover(Connection conn) throws SQLException {
        List<HandoverItem> items = new ArrayList<>();
        String sql = "SELECT c.code, c.name, u.full_name, u.username FROM customers c "
                + "JOIN users u ON u.id = c.sales_rep_id WHERE u.status = 'LOCKED' ORDER BY u.full_name, c.name";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                items.add(new HandoverItem(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)));
            }
        }
        return items;
    }
}
