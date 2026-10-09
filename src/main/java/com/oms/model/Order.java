package com.oms.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Đơn hàng nháp của một đại lý (S3-09). Tổng tiền do OrderService tính lại theo bảng giá và chiết khấu hiện hành. */
public class Order {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private long id;
    private String code;
    private long customerId;
    private String customerCode;
    private String customerName;
    private long customerGroupId;
    private String customerGroupName;
    private boolean customerLocked;
    private String customerLockReason;
    private Long customerSalesRepId;
    private Long deliveryPointId;
    private String deliveryPointText;
    private LocalDate deliveryDate;
    private String note;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private long lineCount;
    private List<OrderLine> lines = new ArrayList<>();
    private BigDecimal grossTotal = BigDecimal.ZERO;
    private BigDecimal discountTotal = BigDecimal.ZERO;
    private int unpricedLines;

    public BigDecimal getPayable() {
        return grossTotal.subtract(discountTotal);
    }

    public String getDeliveryDateText() {
        return deliveryDate == null ? "" : deliveryDate.format(DATE);
    }

    public String getUpdatedAtText() {
        return updatedAt == null ? "" : updatedAt.format(TIME);
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public long getCustomerId() { return customerId; }
    public void setCustomerId(long customerId) { this.customerId = customerId; }
    public String getCustomerCode() { return customerCode; }
    public void setCustomerCode(String customerCode) { this.customerCode = customerCode; }
    public String getCustomerName() { return customerName; }
    public String getCustomerInitials() { return Customer.initialsOf(customerName); }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public long getCustomerGroupId() { return customerGroupId; }
    public void setCustomerGroupId(long customerGroupId) { this.customerGroupId = customerGroupId; }
    public String getCustomerGroupName() { return customerGroupName; }
    public void setCustomerGroupName(String customerGroupName) { this.customerGroupName = customerGroupName; }
    public boolean isCustomerLocked() { return customerLocked; }
    public void setCustomerLocked(boolean customerLocked) { this.customerLocked = customerLocked; }
    public String getCustomerLockReason() { return customerLockReason; }
    public void setCustomerLockReason(String customerLockReason) { this.customerLockReason = customerLockReason; }
    public Long getCustomerSalesRepId() { return customerSalesRepId; }
    public void setCustomerSalesRepId(Long customerSalesRepId) { this.customerSalesRepId = customerSalesRepId; }
    public Long getDeliveryPointId() { return deliveryPointId; }
    public void setDeliveryPointId(Long deliveryPointId) { this.deliveryPointId = deliveryPointId; }
    public String getDeliveryPointText() { return deliveryPointText; }
    public void setDeliveryPointText(String deliveryPointText) { this.deliveryPointText = deliveryPointText; }
    public LocalDate getDeliveryDate() { return deliveryDate; }
    public void setDeliveryDate(LocalDate deliveryDate) { this.deliveryDate = deliveryDate; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public long getLineCount() { return lineCount; }
    public void setLineCount(long lineCount) { this.lineCount = lineCount; }
    public List<OrderLine> getLines() { return lines; }
    public void setLines(List<OrderLine> lines) { this.lines = lines; }
    public BigDecimal getGrossTotal() { return grossTotal; }
    public void setGrossTotal(BigDecimal grossTotal) { this.grossTotal = grossTotal; }
    public BigDecimal getDiscountTotal() { return discountTotal; }
    public void setDiscountTotal(BigDecimal discountTotal) { this.discountTotal = discountTotal; }
    public int getUnpricedLines() { return unpricedLines; }
    public void setUnpricedLines(int unpricedLines) { this.unpricedLines = unpricedLines; }
}
