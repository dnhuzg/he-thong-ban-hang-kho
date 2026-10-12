package com.oms.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Một lần đổi hạn mức công nợ của đại lý (S3-05): giá trị cũ - mới, lý do, người đổi, thời điểm. */
public class CreditChange {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private long customerId;
    private String customerCode;
    private String customerName;
    private BigDecimal oldLimit;
    private BigDecimal newLimit;
    private int oldDays;
    private int newDays;
    private String reason;
    private String changedByName;
    private LocalDateTime changedAt;

    public String getChangedAtText() {
        return changedAt == null ? "" : changedAt.format(TIME);
    }

    public long getCustomerId() { return customerId; }
    public void setCustomerId(long customerId) { this.customerId = customerId; }
    public String getCustomerCode() { return customerCode; }
    public void setCustomerCode(String customerCode) { this.customerCode = customerCode; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public BigDecimal getOldLimit() { return oldLimit; }
    public void setOldLimit(BigDecimal oldLimit) { this.oldLimit = oldLimit; }
    public BigDecimal getNewLimit() { return newLimit; }
    public void setNewLimit(BigDecimal newLimit) { this.newLimit = newLimit; }
    public int getOldDays() { return oldDays; }
    public void setOldDays(int oldDays) { this.oldDays = oldDays; }
    public int getNewDays() { return newDays; }
    public void setNewDays(int newDays) { this.newDays = newDays; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getChangedByName() { return changedByName; }
    public void setChangedByName(String changedByName) { this.changedByName = changedByName; }
    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }
}
