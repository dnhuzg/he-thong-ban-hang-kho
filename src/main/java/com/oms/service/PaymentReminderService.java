package com.oms.service;

import com.oms.config.Db;
import com.oms.dao.NotificationDao;
import com.oms.dao.PaymentReminderDao;
import com.oms.model.InvoiceReminderCandidate;
import com.oms.model.Notification;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Gửi nhắc hạn thanh toán mỗi ngày: email và thông báo trong ứng dụng. */
public class PaymentReminderService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PaymentReminderDao reminderDao;
    private final NotificationDao notificationDao;
    private final MailService mailService;

    public PaymentReminderService() {
        this(new PaymentReminderDao(), new NotificationDao(), new MailService());
    }

    public PaymentReminderService(PaymentReminderDao reminderDao, NotificationDao notificationDao, MailService mailService) {
        this.reminderDao = reminderDao;
        this.notificationDao = notificationDao;
        this.mailService = mailService;
    }

    /** Chạy một lần cho ngày chỉ định; trả về số hoá đơn đã tạo ít nhất một kênh nhắc thành công. */
    public int sendDailyReminders(LocalDate today) throws SQLException {
        if (today == null) {
            throw new IllegalArgumentException("Ngày chạy nhắc hạn không được để trống.");
        }
        final List<InvoiceReminderCandidate> candidates;
        try (Connection conn = Db.getConnection()) {
            candidates = reminderDao.findCandidates(conn, today);
        }

        int handled = 0;
        for (InvoiceReminderCandidate candidate : candidates) {
            PaymentReminderDao.ReminderLog log = ensureLog(candidate, today);
            boolean emailSent = log.emailSent();
            boolean inAppSent = log.inAppSent();

            if (!emailSent && candidate.recipientEmail() != null && !candidate.recipientEmail().isBlank()) {
                String subject = subject(candidate);
                String body = body(candidate);
                if (mailService.send(candidate.recipientEmail().trim(), subject, body)) {
                    markEmailSent(log.id());
                    emailSent = true;
                }
            }

            if (!inAppSent) {
                inAppSent = createInAppNotification(candidate, today, log.id());
            }

            if (emailSent || inAppSent) {
                handled++;
            }
        }
        return handled;
    }

    private PaymentReminderDao.ReminderLog ensureLog(InvoiceReminderCandidate candidate, LocalDate today) throws SQLException {
        return Db.inTransaction(conn -> {
            PaymentReminderDao.ReminderLog existing = reminderDao.findLog(conn, candidate.invoiceId(), today);
            if (existing != null) {
                return existing;
            }
            long id = reminderDao.insertLog(conn, candidate.invoiceId(), today, candidate.reminderType());
            return new PaymentReminderDao.ReminderLog(id, false, false);
        });
    }

    private void markEmailSent(long logId) throws SQLException {
        Db.inTransaction(conn -> {
            reminderDao.markEmailSent(conn, logId);
            return null;
        });
    }

    private boolean createInAppNotification(InvoiceReminderCandidate candidate, LocalDate today, long logId) throws SQLException {
        return Db.inTransaction(conn -> {
            if (!notificationDao.exists(conn, candidate.recipientUserId(), candidate.invoiceId(), today)) {
                Notification notification = new Notification();
                notification.setRecipientUserId(candidate.recipientUserId());
                notification.setInvoiceId(candidate.invoiceId());
                notification.setReminderDate(today);
                notification.setType(candidate.reminderType());
                notification.setTitle(subject(candidate));
                notification.setMessage(body(candidate));
                notificationDao.insert(conn, notification);
            }
            reminderDao.markInAppSent(conn, logId);
            return true;
        });
    }

    private String subject(InvoiceReminderCandidate candidate) {
        return candidate.reminderType() == InvoiceReminderCandidate.ReminderType.DUE_SOON
                ? "Nhắc hạn thanh toán hóa đơn " + candidate.invoiceCode()
                : "Nhắc quá hạn thanh toán hóa đơn " + candidate.invoiceCode();
    }

    private String body(InvoiceReminderCandidate candidate) {
        String greeting = candidate.recipientName() == null || candidate.recipientName().isBlank()
                ? "Chào bạn"
                : "Chào " + candidate.recipientName();
        String amount = Money.text(candidate.totalAmount());
        if (candidate.reminderType() == InvoiceReminderCandidate.ReminderType.DUE_SOON) {
            return greeting + ",\nHóa đơn " + candidate.invoiceCode() + " sẽ đến hạn thanh toán vào "
                    + DATE.format(candidate.dueDate()) + " (còn 3 ngày).\nSố tiền: " + amount;
        }
        return greeting + ",\nHóa đơn " + candidate.invoiceCode() + " đã quá hạn thanh toán từ "
                + DATE.format(candidate.dueDate()) + ".\nSố tiền: " + amount;
    }
}