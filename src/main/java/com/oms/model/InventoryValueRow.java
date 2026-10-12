package com.oms.model;

import java.math.BigDecimal;

public record InventoryValueRow(
        long warehouseId,
        String warehouseName,
        long categoryId,
        String categoryName,
        BigDecimal quantity,
        BigDecimal stockValue) {
}