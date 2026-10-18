package com.oms.dao;

import com.oms.model.SalesTarget;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SalesTargetDao {

    public long create(Connection conn, SalesTarget target)
            throws SQLException {
        String sql = "INSERT INTO sales_targets "
                + "(employee_id, area_id, target_month, "
                + "target_amount, created_by) "
                + "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, target.getEmployeeId());
            ps.setLong(2, target.getAreaId());
            ps.setDate(3, Date.valueOf(target.getTargetMonth()));
            ps.setBigDecimal(4, target.getTargetAmount());
            ps.setLong(5, target.getCreatedBy());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        throw new SQLException("Cannot create sales target");
    }

    public SalesTarget findById(Connection conn, long id)
            throws SQLException {
        String sql = "SELECT * FROM sales_targets WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public SalesTarget findByIdForUpdate(
            Connection conn, long id) throws SQLException {
        String sql = "SELECT * FROM sales_targets "
                + "WHERE id = ? FOR UPDATE";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }
    public List<SalesTarget> findByMonth(
            Connection conn, LocalDate month) throws SQLException {
        List<SalesTarget> result = new ArrayList<>();
        String sql = "SELECT * FROM sales_targets "
                + "WHERE target_month = ? "
                + "ORDER BY employee_id, area_id";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(month.withDayOfMonth(1)));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(map(rs));
            }
        }
        return result;
    }

    public List<SalesTarget> findByEmployee(
            Connection conn, long employeeId) throws SQLException {
        List<SalesTarget> result = new ArrayList<>();
        String sql = "SELECT * FROM sales_targets "
                + "WHERE employee_id = ? "
                + "ORDER BY target_month DESC, area_id";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(map(rs));
            }
        }
        return result;
    }

    public boolean updateAmount(Connection conn,
            long id, java.math.BigDecimal amount) throws SQLException {
        String sql = "UPDATE sales_targets "
                + "SET target_amount = ? WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, amount);
            ps.setLong(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    public java.math.BigDecimal revenueByEmployeeArea(
            Connection conn, long employeeId, long areaId,
            LocalDate from, LocalDate to) throws SQLException {

        String sql = "SELECT COALESCE(SUM(il.amount), 0) "
                + "FROM invoice_lines il "
                + "JOIN invoices i ON i.id = il.invoice_id "
                + "JOIN customers c ON c.id = i.customer_id "
                + "WHERE c.sales_rep_id = ? "
                + "AND c.area_id = ? "
                + "AND i.status <> 'CANCELLED' "
                + "AND i.issued_at >= ? "
                + "AND i.issued_at < ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, employeeId);
            ps.setLong(2, areaId);
            ps.setTimestamp(3,
                    java.sql.Timestamp.valueOf(from.atStartOfDay()));
            ps.setTimestamp(4,
                    java.sql.Timestamp.valueOf(
                            to.plusDays(1).atStartOfDay()));

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBigDecimal(1);
            }
        }
    }
    private SalesTarget map(ResultSet rs) throws SQLException {
        SalesTarget t = new SalesTarget();
        t.setId(rs.getLong("id"));
        t.setEmployeeId(rs.getLong("employee_id"));
        t.setAreaId(rs.getLong("area_id"));
        t.setTargetMonth(
                rs.getDate("target_month").toLocalDate());
        t.setTargetAmount(rs.getBigDecimal("target_amount"));
        t.setCreatedBy(rs.getLong("created_by"));
        t.setCreatedAt(
                rs.getTimestamp("created_at").toLocalDateTime());
        t.setUpdatedAt(
                rs.getTimestamp("updated_at").toLocalDateTime());
        return t;
    }
}
