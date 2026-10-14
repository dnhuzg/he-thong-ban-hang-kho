package com.oms.model;

import java.math.BigDecimal;

/** Số liệu tổng quan ở đầu trang Đại lý, tính trong phạm vi người xem được thấy. */
public class CustomerStats {

    private long total;
    private long active;
    private long unassigned;
    private long withCredit;
    private BigDecimal totalCreditLimit = BigDecimal.ZERO;
    private BigDecimal totalDebt = BigDecimal.ZERO;
    private long nearLimit;

    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }

    public long getActive() { return active; }
    public void setActive(long active) { this.active = active; }

    public long getLocked() { return total - active; }

    public long getUnassigned() { return unassigned; }
    public void setUnassigned(long unassigned) { this.unassigned = unassigned; }

    /** Đại lý được cho nợ (hạn mức lớn hơn 0); còn lại phải thanh toán ngay. */
    public long getWithCredit() { return withCredit; }
    public void setWithCredit(long withCredit) { this.withCredit = withCredit; }

    public long getPayNow() { return total - withCredit; }

    public BigDecimal getTotalCreditLimit() { return totalCreditLimit; }
    public void setTotalCreditLimit(BigDecimal totalCreditLimit) { this.totalCreditLimit = totalCreditLimit; }

    /** Tổng số còn phải thu trên các hoá đơn đã phát hành. */
    public BigDecimal getTotalDebt() { return totalDebt; }
    public void setTotalDebt(BigDecimal totalDebt) { this.totalDebt = totalDebt; }

    /** Đại lý có dư nợ từ 90% hạn mức trở lên. */
    public long getNearLimit() { return nearLimit; }
    public void setNearLimit(long nearLimit) { this.nearLimit = nearLimit; }

    /** Tỉ lệ dư nợ trên tổng hạn mức, phần trăm làm tròn. */
    public long getUsagePercent() {
        return totalCreditLimit.signum() > 0 ? totalDebt.multiply(BigDecimal.valueOf(100)).divide(totalCreditLimit, 0, java.math.RoundingMode.HALF_UP).longValue() : 0;
    }
}
