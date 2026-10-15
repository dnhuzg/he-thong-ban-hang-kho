package com.oms.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Dữ liệu hiển thị cho màn hình xuất kho (S6-05). */
public final class ShipmentExport {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private ShipmentExport() {
    }

    private static String text(LocalDateTime time) {
        return time == null ? "" : time.format(DATE_TIME);
    }

    /** Đơn đã soạn xong, chờ xác nhận xuất kho. */
    public static class Ready {
        private long orderId;
        private String orderCode;
        private String customerName;
        private String warehouseName;
        private String pickingCode;
        private int lineCount;
        private int shortCount;

        public long getOrderId() { return orderId; }
        public void setOrderId(long orderId) { this.orderId = orderId; }
        public String getOrderCode() { return orderCode; }
        public void setOrderCode(String orderCode) { this.orderCode = orderCode; }
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        public String getWarehouseName() { return warehouseName; }
        public void setWarehouseName(String warehouseName) { this.warehouseName = warehouseName; }
        public String getPickingCode() { return pickingCode; }
        public void setPickingCode(String pickingCode) { this.pickingCode = pickingCode; }
        public int getLineCount() { return lineCount; }
        public void setLineCount(int lineCount) { this.lineCount = lineCount; }
        public int getShortCount() { return shortCount; }
        public void setShortCount(int shortCount) { this.shortCount = shortCount; }
    }

    /** Phần đơn còn nợ hàng sau khi xuất thiếu. */
    public static class Backorder {
        private long orderId;
        private String orderCode;
        private String customerName;
        private String sku;
        private String productName;
        private String baseUnit;
        private BigDecimal quantity;
        private String reason;
        private LocalDateTime createdAt;

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
        public String getBaseUnit() { return baseUnit; }
        public void setBaseUnit(String baseUnit) { this.baseUnit = baseUnit; }
        public BigDecimal getQuantity() { return quantity; }
        public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public String getCreatedAtText() { return text(createdAt); }
    }

    /** Phiếu xuất gần đây; xác nhận giao ở /shipments/confirm (S6-08). */
    public static class Recent {
        private long id;
        private String code;
        private String orderCode;
        private String customerName;
        private Shipment.Status status;
        private LocalDateTime createdAt;

        public long getId() { return id; }
        public void setId(long id) { this.id = id; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getOrderCode() { return orderCode; }
        public void setOrderCode(String orderCode) { this.orderCode = orderCode; }
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        public Shipment.Status getStatus() { return status; }
        public void setStatus(Shipment.Status status) { this.status = status; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public String getCreatedAtText() { return text(createdAt); }
    }
}
