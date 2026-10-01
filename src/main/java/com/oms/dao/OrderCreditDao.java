package com.oms.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;

/** Công nợ đã phát sinh và phần hạn mức đang cam kết cho đơn chưa xuất. */
public class OrderCreditDao {
    public record Balance(BigDecimal debt, BigDecimal committed, boolean overdue) { }

    public Balance balance(Connection conn, long customerId, long excludeOrder, int days, LocalDate today) throws SQLException {
        BigDecimal debt;
        boolean overdue;
        try (var ps = conn.prepareStatement("SELECT COALESCE(SUM(outstanding),0), "
                + "COALESCE(SUM(CASE WHEN outstanding > 0 AND issued_on < ? THEN 1 ELSE 0 END),0) "
                + "FROM customer_receivables WHERE customer_id = ?")) {
            ps.setDate(1, Date.valueOf(today.minusDays(days)));
            ps.setLong(2, customerId);
            try (var rs = ps.executeQuery()) {
                rs.next(); debt = rs.getBigDecimal(1); overdue = rs.getLong(2) > 0;
            }
        }
        try (var ps = conn.prepareStatement("SELECT COALESCE(SUM(amount),0) FROM order_credit_reservations "
                + "WHERE customer_id = ? AND order_id <> ?")) {
            ps.setLong(1, customerId); ps.setLong(2, excludeOrder);
            try (var rs = ps.executeQuery()) {
                rs.next(); return new Balance(debt, rs.getBigDecimal(1), overdue);
            }
        }
    }

    public void reserve(Connection conn, long orderId, long customerId, BigDecimal amount) throws SQLException {
        try (var ps = conn.prepareStatement("INSERT INTO order_credit_reservations (order_id,customer_id,amount) VALUES (?,?,?)")) {
            ps.setLong(1, orderId); ps.setLong(2, customerId); ps.setBigDecimal(3, amount); ps.executeUpdate();
        }
    }
    public void release(Connection conn, long orderId) throws SQLException {
        try (var ps = conn.prepareStatement("DELETE FROM order_credit_reservations WHERE order_id = ?")) {
            ps.setLong(1, orderId); ps.executeUpdate();
        }
    }
}
