package com.oms.model;

import java.time.LocalDateTime;

/** Một lần khoá hoặc mở giao dịch với đại lý (S3-07). */
public class LockChange {

    private boolean lock;
    private String reason;
    private String changedByName;
    private LocalDateTime changedAt;

    public String getActionLabel() {
        return lock ? "Khoá giao dịch" : "Mở giao dịch";
    }

    public String getTimeText() {
        return changedAt == null ? "" : changedAt.toString().replace('T', ' ').substring(0, 16);
    }

    public boolean isLock() { return lock; }
    public void setLock(boolean lock) { this.lock = lock; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getChangedByName() { return changedByName; }
    public void setChangedByName(String changedByName) { this.changedByName = changedByName; }
    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }
}
