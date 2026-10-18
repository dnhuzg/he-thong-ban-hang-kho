package com.oms.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Đại lý (EP-03). Nhóm khách hàng quyết định bảng giá áp dụng; người phụ trách quyết định ai được xem. */
public class Customer {

    public enum Status {
        ACTIVE("Đang giao dịch"),
        LOCKED("Bị khoá giao dịch");

        private final String label;

        Status(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public String getCode() {
            return name();
        }
    }

    private long id;
    private String code;
    private String name;
    private String phone;
    private String taxCode;
    private Long groupId;
    private String groupName;
    private Long areaId;
    private String areaName;
    private Long salesRepId;
    private String salesRepName;
    private Status status = Status.ACTIVE;
    private BigDecimal creditLimit = BigDecimal.ZERO;
    private BigDecimal debt = BigDecimal.ZERO;
    private int creditDays;
    private String lockReason;
    private LocalDateTime lockedAt;
    private String lockedByName;
    private LocalDateTime createdAt;

    public boolean isLocked() {
        return status == Status.LOCKED;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getTaxCode() { return taxCode; }
    public void setTaxCode(String taxCode) { this.taxCode = taxCode; }
    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public Long getAreaId() { return areaId; }
    public void setAreaId(Long areaId) { this.areaId = areaId; }
    public String getAreaName() { return areaName; }
    public void setAreaName(String areaName) { this.areaName = areaName; }
    public Long getSalesRepId() { return salesRepId; }
    public void setSalesRepId(Long salesRepId) { this.salesRepId = salesRepId; }
    public String getSalesRepName() { return salesRepName; }
    public void setSalesRepName(String salesRepName) { this.salesRepName = salesRepName; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    /** Hai chữ cái đầu của hai từ cuối trong tên, dùng làm ô chữ ở danh sách. */
    public String getInitials() {
        return initialsOf(name);
    }

    /** Dùng chung cho mọi nơi hiện ô chữ của đại lý (danh sách đại lý, danh sách đơn). */
    public static String initialsOf(String name) {
        if (name == null || name.isBlank()) {
            return "?";
        }
        String[] parts = name.trim().split("\\s+");
        String last = parts[parts.length - 1].substring(0, 1);
        String before = parts.length > 1 ? parts[parts.length - 2].substring(0, 1) : "";
        return (before + last).toUpperCase();
    }

    /** Hạn mức nợ tối đa (₫); 0 là không cho nợ. */
    public BigDecimal getCreditLimit() { return creditLimit; }
    public void setCreditLimit(BigDecimal creditLimit) { this.creditLimit = creditLimit; }

    /** Dư nợ hiện hành: tổng còn phải thu trên các hoá đơn đã phát hành. */
    public BigDecimal getDebt() { return debt; }
    public void setDebt(BigDecimal debt) { this.debt = debt == null ? BigDecimal.ZERO : debt; }

    /** Phần trăm hạn mức đã dùng (làm tròn); 0 khi không cấp hạn mức. */
    public int getUsagePercent() {
        return creditLimit.signum() > 0
                ? debt.multiply(BigDecimal.valueOf(100)).divide(creditLimit, 0, java.math.RoundingMode.HALF_UP).intValue() : 0;
    }

    /** Bậc 10% (tối đa 100) cho độ rộng thanh tỉ lệ trên giao diện. */
    public int getUsageStep() { return Math.min(100, getUsagePercent() / 10 * 10); }

    /** Sắc thái thanh tỉ lệ: dưới 70% an toàn, 70–90% cảnh báo, trên 90% nguy hiểm. */
    public String getUsageTone() {
        int p = getUsagePercent();
        return p > 90 ? "danger" : p >= 70 ? "warning" : "success";
    }
    /** Số ngày nợ tối đa; 0 là phải thanh toán ngay. */
    public int getCreditDays() { return creditDays; }
    public void setCreditDays(int creditDays) { this.creditDays = creditDays; }
    /** Lý do, thời điểm và người khoá khi đại lý đang bị khoá giao dịch; null khi đang giao dịch bình thường. */
    public String getLockReason() { return lockReason; }
    public void setLockReason(String lockReason) { this.lockReason = lockReason; }
    public LocalDateTime getLockedAt() { return lockedAt; }
    public void setLockedAt(LocalDateTime lockedAt) { this.lockedAt = lockedAt; }
    public String getLockedByName() { return lockedByName; }
    public void setLockedByName(String lockedByName) { this.lockedByName = lockedByName; }
    public String getLockedAtText() { return lockedAt == null ? "" : lockedAt.toString().replace('T', ' ').substring(0, 16); }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
