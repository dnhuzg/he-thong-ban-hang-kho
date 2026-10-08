package com.oms.model;

import java.math.BigDecimal;

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

    private Integer deliveredQuantity;

    private BigDecimal factor;
    private BigDecimal baseQuantity;

    private BigDecimal unitPrice;
    private BigDecimal grossAmount;
    private BigDecimal discountAmount;
    private String discountNote;

    public boolean isPriced() {
        return unitPrice != null;
    }

    public BigDecimal getNetAmount() {
        return grossAmount == null
                ? null
                : grossAmount.subtract(discountAmount);
    }

    public boolean isDiscounted() {
        return discountAmount != null
                && discountAmount.signum() > 0;
    }

    public boolean isConverted() {
        return factor != null
                && factor.compareTo(BigDecimal.ONE) != 0;
    }

    public boolean hasDeliveredQuantity() {
        return deliveredQuantity != null;
    }

    public boolean isShortDelivered() {
        return deliveredQuantity != null
                && deliveredQuantity < quantity;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getOrderId() {
        return orderId;
    }

    public void setOrderId(long orderId) {
        this.orderId = orderId;
    }

    public long getProductId() {
        return productId;
    }

    public void setProductId(long productId) {
        this.productId = productId;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public long getUnitId() {
        return unitId;
    }

    public void setUnitId(long unitId) {
        this.unitId = unitId;
    }

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    public String getBaseUnitName() {
        return baseUnitName;
    }

    public void setBaseUnitName(String baseUnitName) {
        this.baseUnitName = baseUnitName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public Integer getDeliveredQuantity() {
        return deliveredQuantity;
    }

    public void setDeliveredQuantity(Integer deliveredQuantity) {
        this.deliveredQuantity = deliveredQuantity;
    }

    public BigDecimal getFactor() {
        return factor;
    }

    public void setFactor(BigDecimal factor) {
        this.factor = factor;
    }

    public BigDecimal getBaseQuantity() {
        return baseQuantity;
    }

    public void setBaseQuantity(BigDecimal baseQuantity) {
        this.baseQuantity = baseQuantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getGrossAmount() {
        return grossAmount;
    }

    public void setGrossAmount(BigDecimal grossAmount) {
        this.grossAmount = grossAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public String getDiscountNote() {
        return discountNote;
    }

    public void setDiscountNote(String discountNote) {
        this.discountNote = discountNote;
    }
}