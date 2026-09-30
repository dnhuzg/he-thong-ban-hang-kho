package com.oms.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Một dòng giá: giá bán và giá sàn theo đơn vị cơ sở (S2-10). costPrice chỉ nạp cho người được xem giá vốn. */
public class PriceListItem {

    private long priceListId;
    private long productId;
    private String sku;
    private String productName;
    private String baseUnitName;
    private BigDecimal salePrice;
    private BigDecimal floorPrice;
    private BigDecimal costPrice;

    /** Biên lợi nhuận trên giá bán, % một chữ số thập phân; null khi không có giá vốn. */
    public BigDecimal getMarginPercent() {
        if (costPrice == null || salePrice == null || salePrice.signum() == 0) {
            return null;
        }
        return salePrice.subtract(costPrice).multiply(BigDecimal.valueOf(100))
                .divide(salePrice, 1, RoundingMode.HALF_UP);
    }

    public long getPriceListId() { return priceListId; }
    public void setPriceListId(long priceListId) { this.priceListId = priceListId; }
    public long getProductId() { return productId; }
    public void setProductId(long productId) { this.productId = productId; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getBaseUnitName() { return baseUnitName; }
    public void setBaseUnitName(String baseUnitName) { this.baseUnitName = baseUnitName; }
    public BigDecimal getSalePrice() { return salePrice; }
    public void setSalePrice(BigDecimal salePrice) { this.salePrice = salePrice; }
    public BigDecimal getFloorPrice() { return floorPrice; }
    public void setFloorPrice(BigDecimal floorPrice) { this.floorPrice = floorPrice; }
    public BigDecimal getCostPrice() { return costPrice; }
    public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }
}
