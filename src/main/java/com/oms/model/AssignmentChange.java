package com.oms.model;

import java.time.LocalDateTime;

/** Một lần đổi người phụ trách của đại lý (S3-06). fromName / toName null nghĩa là chưa có người phụ trách. */
public class AssignmentChange {

    private long customerId;
    private String customerCode;
    private String customerName;
    private String fromName;
    private String toName;
    private String reason;
    private String changedByName;
    private LocalDateTime changedAt;

    public String getFromLabel() {
        return fromName == null ? "Chưa phân công" : fromName;
    }

    public String getToLabel() {
        return toName == null ? "Bỏ phân công" : toName;
    }

    public String getTimeText() {
        return changedAt == null ? "" : changedAt.toString().replace('T', ' ').substring(0, 16);
    }

    public long getCustomerId() { return customerId; }
    public void setCustomerId(long customerId) { this.customerId = customerId; }
    public String getCustomerCode() { return customerCode; }
    public void setCustomerCode(String customerCode) { this.customerCode = customerCode; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getFromName() { return fromName; }
    public void setFromName(String fromName) { this.fromName = fromName; }
    public String getToName() { return toName; }
    public void setToName(String toName) { this.toName = toName; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getChangedByName() { return changedByName; }
    public void setChangedByName(String changedByName) { this.changedByName = changedByName; }
    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }
}
