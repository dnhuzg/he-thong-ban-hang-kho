package com.oms.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Thông báo trong ứng dụng cho một tài khoản nhận nhắc hạn. */
public class Notification {

    private long id;
    private long recipientUserId;
    private long invoiceId;
    private LocalDate reminderDate;
    private InvoiceReminderCandidate.ReminderType type;
    private String title;
    private String message;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getRecipientUserId() { return recipientUserId; }
    public void setRecipientUserId(long recipientUserId) { this.recipientUserId = recipientUserId; }
    public long getInvoiceId() { return invoiceId; }
    public void setInvoiceId(long invoiceId) { this.invoiceId = invoiceId; }
    public LocalDate getReminderDate() { return reminderDate; }
    public void setReminderDate(LocalDate reminderDate) { this.reminderDate = reminderDate; }
    public InvoiceReminderCandidate.ReminderType getType() { return type; }
    public void setType(InvoiceReminderCandidate.ReminderType type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}