package com.oms.model;

import java.math.BigDecimal;

/** Số liệu tổng quan ở đầu trang Đại lý, tính trong phạm vi người xem được thấy. */
public class CustomerStats {

    private long total;
    private long active;
    private long unassigned;
    private long withCredit;
    private BigDecimal totalCreditLimit = BigDecimal.ZERO;

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
}
