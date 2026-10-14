package com.oms.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Hoá đơn bán hàng (S7-01) lập từ phiếu xuất đã giao; đã phát hành thì không sửa, không xoá. */
public class Invoice {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public enum Status {
        ISSUED("Chưa thanh toán", "warning"),
        PARTIAL("Thanh toán một phần", "primary"),
        PAID("Đã thanh toán", "success"),
        CANCELLED("Đã huỷ", "danger");

        private final String label;
        private final String tone;

        Status(String label, String tone) {
            this.label = label;
            this.tone = tone;
        }

        public String getLabel() { return label; }
        public String getTone() { return tone; }
    }

    private long id;
    private String code;
    private long customerId;
    private String customerCode;
    private String customerName;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount = BigDecimal.ZERO;
    private LocalDateTime issuedAt;
    private LocalDate dueDate;
    private Status status;
    private String issuedByName;
    private final List<Line> lines = new ArrayList<>();
    private final List<Payment.Allocation> payments = new ArrayList<>();

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public long getCustomerId() { return customerId; }
    public void setCustomerId(long customerId) { this.customerId = customerId; }
    public String getCustomerCode() { return customerCode; }
    public void setCustomerCode(String customerCode) { this.customerCode = customerCode; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
    public BigDecimal getOutstanding() { return totalAmount == null ? BigDecimal.ZERO : totalAmount.subtract(paidAmount); }
    public LocalDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }
    public String getIssuedAtText() { return issuedAt == null ? "" : issuedAt.format(DATE_TIME); }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public String getDueDateText() { return dueDate == null ? "" : dueDate.format(DATE); }
    /** Còn nợ và đã qua hạn thanh toán tính tới hôm nay. */
    public boolean isOverdue() { return getOutstanding().signum() > 0 && dueDate != null && dueDate.isBefore(LocalDate.now()); }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getIssuedByName() { return issuedByName; }
    public void setIssuedByName(String issuedByName) { this.issuedByName = issuedByName; }
    public List<Line> getLines() { return lines; }
    /** Các lần thu đã đối trừ vào hoá đơn này. */
    public List<Payment.Allocation> getPayments() { return payments; }

    /** Một dòng hàng thực giao; số lượng và đơn giá theo đơn vị cơ sở. */
    public static class Line {
        private String shipmentCode;
        private String sku;
        private String productName;
        private String baseUnit;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal amount;

        public String getShipmentCode() { return shipmentCode; }
        public void setShipmentCode(String shipmentCode) { this.shipmentCode = shipmentCode; }
        public String getSku() { return sku; }
        public void setSku(String sku) { this.sku = sku; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public String getBaseUnit() { return baseUnit; }
        public void setBaseUnit(String baseUnit) { this.baseUnit = baseUnit; }
        public BigDecimal getQuantity() { return quantity; }
        public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
    }

    /** Phiếu xuất đã giao thành công, chưa lập hoá đơn. */
    public static class Eligible {
        private long shipmentId;
        private String shipmentCode;
        private String orderCode;
        private long customerId;
        private String customerName;
        private LocalDateTime deliveredAt;
        private int itemCount;

        public long getShipmentId() { return shipmentId; }
        public void setShipmentId(long shipmentId) { this.shipmentId = shipmentId; }
        public String getShipmentCode() { return shipmentCode; }
        public void setShipmentCode(String shipmentCode) { this.shipmentCode = shipmentCode; }
        public String getOrderCode() { return orderCode; }
        public void setOrderCode(String orderCode) { this.orderCode = orderCode; }
        public long getCustomerId() { return customerId; }
        public void setCustomerId(long customerId) { this.customerId = customerId; }
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }
        public String getDeliveredAtText() { return deliveredAt == null ? "" : deliveredAt.format(DATE_TIME); }
        public int getItemCount() { return itemCount; }
        public void setItemCount(int itemCount) { this.itemCount = itemCount; }
    }
}
