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

    /** tone là hậu tố class badge (badge--tone) để màu trạng thái thống nhất ở mọi màn hình. */
    public enum Status {
        DRAFT("Nháp", ""),
        PENDING_APPROVAL("Chờ duyệt", "warning"),
        APPROVED("Đã duyệt", "primary"),
        PICKING("Đang soạn hàng", "primary"),
        SHIPPED("Đã xuất kho", "primary"),
        DELIVERED("Đã giao", "success"),
        CLOSED("Đã đóng", "success"),
        CANCELLED("Đã huỷ", "danger"),
        REJECTED("Bị từ chối", "danger");

        private final String label;
        private final String tone;

        Status(String label, String tone) {
            this.label = label;
            this.tone = tone;
        }

        public String getLabel() { return label; }
        public String getTone() { return tone; }
        public String getCode() { return name(); }
    }
    private Status status = Status.DRAFT;
    private Long warehouseId;
    private String approvalReason;
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public boolean isDraft() { return status == Status.DRAFT; }
    /** Hàng đã rời kho (Đã xuất trở đi) hoặc đơn đã kết thúc thì không huỷ được, phải đi đường trả hàng. */
    public boolean isCancellable() {
        return status == Status.DRAFT || status == Status.PENDING_APPROVAL || status == Status.APPROVED
                || status == Status.PICKING || status == Status.REJECTED;
    }
    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
    public String getApprovalReason() { return approvalReason; }
    public void setApprovalReason(String approvalReason) { this.approvalReason = approvalReason; }
    private Long customerAccountUserId;
    private boolean customerSubmitted;
    public Long getCustomerAccountUserId() { return customerAccountUserId; }
    public void setCustomerAccountUserId(Long id) { customerAccountUserId = id; }
    public boolean isCustomerSubmitted() { return customerSubmitted; }
    public void setCustomerSubmitted(boolean value) { customerSubmitted = value; }
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
    private boolean requiresApproval;

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
    public String getCreatedAtText() { return createdAt == null ? "" : createdAt.format(TIME); }
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
    public boolean isRequiresApproval() { return requiresApproval; }
    public void setRequiresApproval(boolean requiresApproval) { this.requiresApproval = requiresApproval; }
}
