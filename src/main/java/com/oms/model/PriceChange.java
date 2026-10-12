package com.oms.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Một lần đổi giá của sản phẩm trong một bảng giá (S3-02). */
public class PriceChange {

    public enum Type {
        ADD("Thêm vào bảng giá"), UPDATE("Đổi giá"), REMOVE("Bỏ khỏi bảng giá");

        private final String label;

        Type(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Type type;
    private String priceListCode;
    private int priceListVersion;
    private String groupName;
    private BigDecimal oldSalePrice;
    private BigDecimal oldFloorPrice;
    private BigDecimal newSalePrice;
    private BigDecimal newFloorPrice;
    private LocalDate effectiveFrom;
    private String changedByName;
    private LocalDateTime changedAt;

    public String getEffectiveText() {
        return effectiveFrom == null ? "" : effectiveFrom.format(DATE);
    }

    public String getChangedAtText() {
        return changedAt == null ? "" : changedAt.format(TIME);
    }

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
    public String getPriceListCode() { return priceListCode; }
    public void setPriceListCode(String priceListCode) { this.priceListCode = priceListCode; }
    public int getPriceListVersion() { return priceListVersion; }
    public void setPriceListVersion(int priceListVersion) { this.priceListVersion = priceListVersion; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public BigDecimal getOldSalePrice() { return oldSalePrice; }
    public void setOldSalePrice(BigDecimal oldSalePrice) { this.oldSalePrice = oldSalePrice; }
    public BigDecimal getOldFloorPrice() { return oldFloorPrice; }
    public void setOldFloorPrice(BigDecimal oldFloorPrice) { this.oldFloorPrice = oldFloorPrice; }
    public BigDecimal getNewSalePrice() { return newSalePrice; }
    public void setNewSalePrice(BigDecimal newSalePrice) { this.newSalePrice = newSalePrice; }
    public BigDecimal getNewFloorPrice() { return newFloorPrice; }
    public void setNewFloorPrice(BigDecimal newFloorPrice) { this.newFloorPrice = newFloorPrice; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    public String getChangedByName() { return changedByName; }
    public void setChangedByName(String changedByName) { this.changedByName = changedByName; }
    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }
}
