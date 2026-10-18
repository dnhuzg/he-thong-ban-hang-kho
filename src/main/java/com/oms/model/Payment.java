package com.oms.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Phiếu thu tiền của đại lý (S7-02); đã ghi nhận thì không xoá, chỉ lập bút toán điều chỉnh. */
public class Payment {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public enum Method {
        CASH("Tiền mặt"),
        BANK_TRANSFER("Chuyển khoản"),
        OTHER("Khác");

        private final String label;

        Method(String label) {
            this.label = label;
        }

        public String getLabel() { return label; }
        public String getCode() { return name(); }
    }

    private long id;
    private String code;
    private String customerName;
    private String customerCode;
    private BigDecimal amount;
    private Method method;
    private LocalDate paidOn;
    private String collectedByName;
    private String note;
    private String invoiceCodes;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerCode() { return customerCode; }
    public void setCustomerCode(String customerCode) { this.customerCode = customerCode; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Method getMethod() { return method; }
    public void setMethod(Method method) { this.method = method; }
    public void setPaidOn(LocalDate paidOn) { this.paidOn = paidOn; }
    public String getPaidOnText() { return paidOn == null ? "" : paidOn.format(DATE); }
    public String getCollectedByName() { return collectedByName; }
    public void setCollectedByName(String collectedByName) { this.collectedByName = collectedByName; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    /** Các hoá đơn được đối trừ, dạng "HD000001, HD000002". */
    public String getInvoiceCodes() { return invoiceCodes; }
    public void setInvoiceCodes(String invoiceCodes) { this.invoiceCodes = invoiceCodes; }

    /** Một khoản của phiếu thu đối trừ vào một hoá đơn. */
    public static class Allocation {
        private String paymentCode;
        private LocalDate paidOn;
        private Method method;
        private BigDecimal amount;

        public String getPaymentCode() { return paymentCode; }
        public void setPaymentCode(String paymentCode) { this.paymentCode = paymentCode; }
        public void setPaidOn(LocalDate paidOn) { this.paidOn = paidOn; }
        public String getPaidOnText() { return paidOn == null ? "" : paidOn.format(DATE); }
        public Method getMethod() { return method; }
        public void setMethod(Method method) { this.method = method; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
    }

    /** Hoá đơn còn nợ để chọn đối trừ. */
    public static class OpenInvoice {
        private long id;
        private String code;
        private long customerId;
        private String customerName;
        private BigDecimal outstanding;
        private LocalDate dueDate;

        public long getId() { return id; }
        public void setId(long id) { this.id = id; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public long getCustomerId() { return customerId; }
        public void setCustomerId(long customerId) { this.customerId = customerId; }
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        public BigDecimal getOutstanding() { return outstanding; }
        public void setOutstanding(BigDecimal outstanding) { this.outstanding = outstanding; }
        public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
        public String getDueDateText() { return dueDate == null ? "" : dueDate.format(DATE); }
    }
}
