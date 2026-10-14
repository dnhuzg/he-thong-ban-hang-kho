package com.oms.dao;

import com.oms.model.InvoiceReminderCandidate;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Truy vấn hoá đơn cần nhắc và nhật ký chống gửi trùng. */
public class PaymentReminderDao {

    public record ReminderLog(long id, boolean emailSent, boolean inAppSent) {
    }

    public List<InvoiceReminderCandidate> findCandidates(Connection conn, LocalDate today) throws SQLException {
        LocalDate dueSoonDate = today.plusDays(3);
        String sql = "SELECT i.id, i.code, i.recipient_user_id, u.email, u.full_name, i.total_amount, i.due_date, "
                + "CASE WHEN i.due_date = ? THEN 'DUE_SOON' ELSE 'OVERDUE' END AS reminder_type "
                + "FROM invoices i JOIN users u ON u.id = i.recipient_user_id "
                + "WHERE i.status IN ('ISSUED', 'PARTIAL') AND u.status = 'ACTIVE' "
                + "AND (i.due_date = ? OR i.due_date < ?) ORDER BY i.due_date, i.id";
        List<InvoiceReminderCandidate> candidates = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(dueSoonDate));
            ps.setDate(2, Date.valueOf(dueSoonDate));
            ps.setDate(3, Date.valueOf(today));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    candidates.add(new InvoiceReminderCandidate(
                            rs.getLong("id"),
                            rs.getString("code"),
                            rs.getLong("recipient_user_id"),
                            rs.getString("email"),
                            rs.getString("full_name"),
                            rs.getBigDecimal("total_amount"),
                            rs.getDate("due_date").toLocalDate(),
                            InvoiceReminderCandidate.ReminderType.valueOf(rs.getString("reminder_type"))));
                }
            }
        }
        return candidates;
    }

    public ReminderLog findLog(Connection conn, long invoiceId, LocalDate reminderDate) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, email_sent, in_app_sent FROM invoice_reminder_logs WHERE invoice_id = ? AND reminder_date = ?")) {
            ps.setLong(1, invoiceId);
            ps.setDate(2, Date.valueOf(reminderDate));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new ReminderLog(rs.getLong(1), rs.getBoolean(2), rs.getBoolean(3));
            }
        }
    }

    public long insertLog(Connection conn, long invoiceId, LocalDate reminderDate,
                           InvoiceReminderCandidate.ReminderType type) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO invoice_reminder_logs (invoice_id, reminder_date, reminder_type) VALUES (?, ?, ?)",
                PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, invoiceId);
            ps.setDate(2, Date.valueOf(reminderDate));
            ps.setString(3, type.name());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    public void markEmailSent(Connection conn, long logId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE invoice_reminder_logs SET email_sent = 1, email_sent_at = CURRENT_TIMESTAMP WHERE id = ? AND email_sent = 0")) {
            ps.setLong(1, logId);
            ps.executeUpdate();
        }
    }

    public void markInAppSent(Connection conn, long logId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE invoice_reminder_logs SET in_app_sent = 1, in_app_sent_at = CURRENT_TIMESTAMP WHERE id = ? AND in_app_sent = 0")) {
            ps.setLong(1, logId);
            ps.executeUpdate();
        }
    }
}