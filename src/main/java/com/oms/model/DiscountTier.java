package com.oms.model;

import java.math.BigDecimal;

/** Một bậc chiết khấu: mua từ minQty (đơn vị cơ sở) trở lên thì chiết khấu value (% hoặc đồng trên một đơn vị cơ sở). */
public class DiscountTier {

    private final int minQty;
    private final BigDecimal value;

    public DiscountTier(int minQty, BigDecimal value) {
        this.minQty = minQty;
        this.value = value;
    }

    public int getMinQty() { return minQty; }
    public BigDecimal getValue() { return value; }
}
