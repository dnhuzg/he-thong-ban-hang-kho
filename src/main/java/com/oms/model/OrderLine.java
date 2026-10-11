package com.oms.model;

import java.math.BigDecimal;

/** Một dòng hàng của đơn: số lượng theo đơn vị đã chọn kèm hệ số quy đổi tại thời điểm ghi; tiền do OrderService tính. */
public class OrderLine {

    private long id;
    private long orderId;
    private long productId;
    private String sku;
    private String productName;
    private long unitId;
    private String unitName;
    private String baseUnitName;
    private int quantity;
    private BigDecimal factor;
    private BigDecimal baseQuantity;

    /*
     * Giá chỉnh thủ công theo đơn vị cơ sở.
     * null nghĩa là dòng hàng dùng giá tự động từ bảng giá.
     */
    private BigDecimal manualBasePrice;

    // Các trường dưới đây do OrderService tính theo bảng giá và chính sách chiết khấu hiện hành
    private BigDecimal unitPrice;
    private BigDecimal grossAmount;
    private BigDecimal discountAmount;
    private String discountNote;

    public boolean isPriced() {
        return unitPrice != null;
    }

    public BigDecimal getNetAmount() {
        return grossAmount == null ? null : grossAmount.subtract(discountAmount);
    }

    public boolean isDiscounted() {
        return discountAmount != null && discountAmount.signum() > 0;
    }

    public boolean isManualPrice() {
        return manualBasePrice != null;
    }

    /** Số lượng quy ra đơn vị cơ sở, chỉ hiện khi dòng đặt theo đơn vị quy đổi (ví dụ "= 24 Lon"). */
    public boolean isConverted() {
        return factor != null && factor.compareTo(BigDecimal.ONE) != 0;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getOrderId() { return orderId; }
    public void setOrderId(long orderId) { this.orderId = orderId; }
    public long getProductId() { return productId; }
    public void setProductId(long productId) { this.productId = productId; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public long getUnitId() { return unitId; }
    public void setUnitId(long unitId) { this.unitId = unitId; }
    public String getUnitName() { return unitName; }
    public void setUnitName(String unitName) { this.unitName = unitName; }
    public String getBaseUnitName() { return baseUnitName; }
    public void setBaseUnitName(String baseUnitName) { this.baseUnitName = baseUnitName; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public BigDecimal getFactor() { return factor; }
    public void setFactor(BigDecimal factor) { this.factor = factor; }
    public BigDecimal getBaseQuantity() { return baseQuantity; }
    public void setBaseQuantity(BigDecimal baseQuantity) { this.baseQuantity = baseQuantity; }
    public BigDecimal getManualBasePrice() { return manualBasePrice; }
    public void setManualBasePrice(BigDecimal manualBasePrice) { this.manualBasePrice = manualBasePrice; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getGrossAmount() { return grossAmount; }
    public void setGrossAmount(BigDecimal grossAmount) { this.grossAmount = grossAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public String getDiscountNote() { return discountNote; }
    public void setDiscountNote(String discountNote) { this.discountNote = discountNote; }
}
