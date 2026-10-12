package com.oms.model;

import java.math.BigDecimal;

public record InventorySkuAgeRow(
        long productId,
        String sku,
        String productName,
        BigDecimal quantity,
        BigDecimal averageAgeDays,
        boolean slowMoving) {
}