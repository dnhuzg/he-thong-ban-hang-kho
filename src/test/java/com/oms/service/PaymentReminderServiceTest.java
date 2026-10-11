package com.oms.service;

import com.oms.config.Db;
import com.oms.dao.NotificationDao;
import com.oms.dao.PaymentReminderDao;
import com.oms.model.Role;
import com.oms.support.DatabaseTest;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentReminderServiceTest extends DatabaseTest {

    @Test
    void sendsDueSoonEmailAndInAppNotificationOncePerDay() throws Exception {
        var agent = user("agent-reminder", Role.CUSTOMER);
        addCustomer();
        LocalDate today = LocalDate.of(2026, 10, 4);
        insertInvoice(1001, "HD-1001", agent.getId(), today.plusDays(3), "ISSUED");

        CountingMailService mail = new CountingMailService(true);
        PaymentReminderService service = new PaymentReminderService(new PaymentReminderDao(), new NotificationDao(), mail);

        assertEquals(1, service.sendDailyReminders(today));
        assertEquals(1, service.sendDailyReminders(today));
        assertEquals(1, mail.sent.get());
        assertEquals(1, count("invoice_reminder_logs", "invoice_id = 1001"));
        assertEquals(1, count("notifications", "invoice_id = 1001"));
    }

    @Test
    void sendsOverdueEmailAndInAppNotification() throws Exception {
        var agent = user("agent-overdue", Role.CUSTOMER);
        addCustomer();
        LocalDate today = LocalDate.of(2026, 10, 4);
        insertInvoice(1002, "HD-1002", agent.getId(), today.minusDays(1), "ISSUED");

        CountingMailService mail = new CountingMailService(true);
        PaymentReminderService service = new PaymentReminderService(new PaymentReminderDao(), new NotificationDao(), mail);

        assertEquals(1, service.sendDailyReminders(today));
        assertEquals(1, mail.sent.get());
        assertEquals(1, count("notifications", "invoice_id = 1002"));
    }

    @Test
    void ignoresPaidInvoice() throws Exception {
        var agent = user("agent-paid", Role.CUSTOMER);
        addCustomer();
        LocalDate today = LocalDate.of(2026, 10, 4);
        insertInvoice(1003, "HD-1003", agent.getId(), today.minusDays(1), "PAID");

        CountingMailService mail = new CountingMailService(true);
        PaymentReminderService service = new PaymentReminderService(new PaymentReminderDao(), new NotificationDao(), mail);

        assertEquals(0, service.sendDailyReminders(today));
        assertEquals(0, mail.sent.get());
        assertEquals(0, count("notifications", "invoice_id = 1003"));
    }

    private void addCustomer() throws SQLException {
        try (var conn = Db.getConnection();
             var ps = conn.prepareStatement("INSERT INTO customers (id, code, name, group_id) VALUES (?, ?, ?, ?)")) {
            ps.setLong(1, 1001);
            ps.setString(2, "DL-1001");
            ps.setString(3, "Đại lý thử");
            ps.setLong(4, 1);
            ps.executeUpdate();
        }
    }

    private void insertInvoice(long id, String code, long userId, LocalDate dueDate, String status) throws SQLException {
        try (var conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO invoices (id, code, customer_id, recipient_user_id, total_amount, issued_at, due_date, status) "
                             + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            ps.setLong(1, id);
            ps.setString(2, code);
            ps.setLong(3, 1001);
            ps.setLong(4, userId);
            ps.setBigDecimal(5, new java.math.BigDecimal("100000.00"));
            ps.setTimestamp(6, Timestamp.valueOf("2026-10-01 08:00:00"));
            ps.setDate(7, java.sql.Date.valueOf(dueDate));
            ps.setString(8, status);
            ps.executeUpdate();
        }
    }

    private static class CountingMailService extends MailService {
        private final boolean result;
        private final AtomicInteger sent = new AtomicInteger();

        private CountingMailService(boolean result) {
            this.result = result;
        }

        @Override
        public boolean send(String to, String subject, String body) {
            sent.incrementAndGet();
            return result;
        }
    }
}