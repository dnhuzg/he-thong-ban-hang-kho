package com.oms.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Dữ liệu tối thiểu để tạo một lần nhắc hạn thanh toán. */
public record InvoiceReminderCandidate(
        long invoiceId,
        String invoiceCode,
        long recipientUserId,
        String recipientEmail,
        String recipientName,
        BigDecimal totalAmount,
        LocalDate dueDate,
        ReminderType reminderType) {

    public enum ReminderType {
        DUE_SOON("Nhắc hạn thanh toán"),
        OVERDUE("Nhắc quá hạn thanh toán");

        private final String label;

        ReminderType(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }
}