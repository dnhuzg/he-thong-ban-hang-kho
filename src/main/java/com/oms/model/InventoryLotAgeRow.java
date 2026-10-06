package com.oms.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InventoryLotAgeRow(
        long productId,
        String sku,
        String productName,
        BigDecimal quantity,
        LocalDateTime receivedAt) {
}