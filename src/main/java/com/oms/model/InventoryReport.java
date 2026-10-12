package com.oms.model;

import java.util.List;

public record InventoryReport(
        List<InventoryValueRow> valueRows,
        List<InventorySkuAgeRow> skuAgeRows,
        int slowMovingThresholdDays) {
}