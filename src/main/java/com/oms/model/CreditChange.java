package com.oms.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Một lần đổi hạn mức công nợ của đại lý (S3-05): giá trị cũ - mới, lý do, người đổi, thời điểm. */
public class CreditChange {

    private BigDecimal oldLimit;
    private BigDecimal newLimit;
    private int oldDays;
    private int newDays;
    private String reason;
    private String changedByName;
    private LocalDateTime changedAt;

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
