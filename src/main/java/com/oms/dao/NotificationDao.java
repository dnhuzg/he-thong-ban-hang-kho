package com.oms.dao;

import com.oms.model.Notification;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Lưu và đọc thông báo trong ứng dụng. */
public class NotificationDao {

    public long insert(Connection conn, Notification notification) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO notifications (recipient_user_id, invoice_id, reminder_date, notification_type, title, message) "
                        + "VALUES (?, ?, ?, ?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, notification.getRecipientUserId());
            ps.setLong(2, notification.getInvoiceId());
            ps.setDate(3, Date.valueOf(notification.getReminderDate()));
            ps.setString(4, notification.getType().name());
            ps.setString(5, notification.getTitle());
            ps.setString(6, notification.getMessage());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    public boolean exists(Connection conn, long userId, long invoiceId, java.time.LocalDate reminderDate) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM notifications WHERE recipient_user_id = ? AND invoice_id = ? AND reminder_date = ? LIMIT 1")) {
            ps.setLong(1, userId);
            ps.setLong(2, invoiceId);
            ps.setDate(3, Date.valueOf(reminderDate));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public List<Notification> findUnreadByUser(Connection conn, long userId) throws SQLException {
        String sql = "SELECT id, recipient_user_id, invoice_id, reminder_date, notification_type, title, message, read_at, created_at "
                + "FROM notifications WHERE recipient_user_id = ? AND read_at IS NULL ORDER BY created_at DESC, id DESC";
        List<Notification> items = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Notification n = new Notification();
                    n.setId(rs.getLong("id"));
                    n.setRecipientUserId(rs.getLong("recipient_user_id"));
                    n.setInvoiceId(rs.getLong("invoice_id"));
                    n.setReminderDate(rs.getDate("reminder_date").toLocalDate());
                    n.setType(com.oms.model.InvoiceReminderCandidate.ReminderType.valueOf(rs.getString("notification_type")));
                    n.setTitle(rs.getString("title"));
                    n.setMessage(rs.getString("message"));
                    var readAt = rs.getTimestamp("read_at");
                    n.setReadAt(readAt == null ? null : readAt.toLocalDateTime());
                    n.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    items.add(n);
                }
            }
        }
        return items;
    }
}