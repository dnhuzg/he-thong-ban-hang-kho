package com.oms.model;

/** Số liệu tổng quan ở đầu trang Đơn hàng, tính trong phạm vi người xem được thấy. */
public class OrderStats {

    private long total;
    private long thisMonth;
    private long customers;
    private long dueSoon;
    private long withoutLines;

    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }

    public long getThisMonth() { return thisMonth; }
    public void setThisMonth(long thisMonth) { this.thisMonth = thisMonth; }

    /** Số đại lý khác nhau đang có đơn. */
    public long getCustomers() { return customers; }
    public void setCustomers(long customers) { this.customers = customers; }

    /** Đơn có ngày giao từ hôm nay tới 7 ngày tới. */
    public long getDueSoon() { return dueSoon; }
    public void setDueSoon(long dueSoon) { this.dueSoon = dueSoon; }

    /** Đơn chưa có dòng hàng nào (tạo ra rồi bỏ dở). */
    public long getWithoutLines() { return withoutLines; }
    public void setWithoutLines(long withoutLines) { this.withoutLines = withoutLines; }
}
