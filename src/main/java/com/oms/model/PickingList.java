package com.oms.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Phiếu soạn hàng (S6-03): gộp các dòng của một hoặc nhiều đơn đã duyệt trong cùng một kho. */
public class PickingList {

    public static final String OPEN = "OPEN";
    public static final String DONE = "DONE";

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private long id;
    private String code;
    private long warehouseId;
    private String warehouseName;
    private String status;
    private String createdByName;
    private LocalDateTime createdAt;
    private int orderCount;
    private int lineCount;
    private int pickedCount;
    private final List<Line> lines = new ArrayList<>();

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(long warehouseId) { this.warehouseId = warehouseId; }
    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String warehouseName) { this.warehouseName = warehouseName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isOpen() { return OPEN.equals(status); }
    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getCreatedAtText() { return createdAt == null ? "" : createdAt.format(DATE_TIME); }
    public int getOrderCount() { return orderCount; }
    public void setOrderCount(int orderCount) { this.orderCount = orderCount; }
    public int getLineCount() { return lineCount; }
    public void setLineCount(int lineCount) { this.lineCount = lineCount; }
    public int getPickedCount() { return pickedCount; }
    public void setPickedCount(int pickedCount) { this.pickedCount = pickedCount; }
    public boolean isAllPicked() { return lineCount > 0 && pickedCount == lineCount; }
    public List<Line> getLines() { return lines; }

    /** Một dòng cần soạn, số lượng theo đơn vị cơ sở của sản phẩm. */
    public static class Line {
        private long id;
        private long orderId;
        private String orderCode;
        private String customerName;
        private String sku;
        private String productName;
        private String categoryName;
        private String baseUnit;
        private BigDecimal required;
        private BigDecimal picked;
        private String shortReason;

        public long getId() { return id; }
        public void setId(long id) { this.id = id; }
        public long getOrderId() { return orderId; }
        public void setOrderId(long orderId) { this.orderId = orderId; }
        public String getOrderCode() { return orderCode; }
        public void setOrderCode(String orderCode) { this.orderCode = orderCode; }
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        public String getSku() { return sku; }
        public void setSku(String sku) { this.sku = sku; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
        public String getBaseUnit() { return baseUnit; }
        public void setBaseUnit(String baseUnit) { this.baseUnit = baseUnit; }
        public BigDecimal getRequired() { return required; }
        public void setRequired(BigDecimal required) { this.required = required; }
        public BigDecimal getPicked() { return picked; }
        public void setPicked(BigDecimal picked) { this.picked = picked; }
        public String getShortReason() { return shortReason; }
        public void setShortReason(String shortReason) { this.shortReason = shortReason; }
        public boolean isDone() { return picked != null; }
        public boolean isShortPicked() { return picked != null && picked.compareTo(required) < 0; }
    }

    /** Đơn đã duyệt đang chờ đưa vào phiếu soạn. */
    public static class QueueItem {
        private long orderId;
        private String orderCode;
        private String customerName;
        private long warehouseId;
        private String warehouseName;
        private LocalDate deliveryDate;
        private int lineCount;

        public long getOrderId() { return orderId; }
        public void setOrderId(long orderId) { this.orderId = orderId; }
        public String getOrderCode() { return orderCode; }
        public void setOrderCode(String orderCode) { this.orderCode = orderCode; }
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        public long getWarehouseId() { return warehouseId; }
        public void setWarehouseId(long warehouseId) { this.warehouseId = warehouseId; }
        public String getWarehouseName() { return warehouseName; }
        public void setWarehouseName(String warehouseName) { this.warehouseName = warehouseName; }
        public LocalDate getDeliveryDate() { return deliveryDate; }
        public void setDeliveryDate(LocalDate deliveryDate) { this.deliveryDate = deliveryDate; }
        public String getDeliveryDateText() { return deliveryDate == null ? "" : deliveryDate.format(DATE); }
        public int getLineCount() { return lineCount; }
        public void setLineCount(int lineCount) { this.lineCount = lineCount; }
    }
}
