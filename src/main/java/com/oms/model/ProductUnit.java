package com.oms.model;

import java.math.BigDecimal;

/** Một đơn vị quy đổi của sản phẩm: 1 unitName = factor × đơn vị cơ sở (S2-07). */
public class ProductUnit {

    private long productId;
    private long unitId;
    private String unitName;
    private BigDecimal factor;

    /** Hệ số dạng gọn để hiển thị: 24 thay vì 24.0000, 1.5 thay vì 1.5000. */
    public String getFactorText() {
        return factor == null ? "" : factor.stripTrailingZeros().toPlainString();
    }

    public long getProductId() { return productId; }
    public void setProductId(long productId) { this.productId = productId; }
    public long getUnitId() { return unitId; }
    public void setUnitId(long unitId) { this.unitId = unitId; }
    public String getUnitName() { return unitName; }
    public void setUnitName(String unitName) { this.unitName = unitName; }
    public BigDecimal getFactor() { return factor; }
    public void setFactor(BigDecimal factor) { this.factor = factor; }
}
