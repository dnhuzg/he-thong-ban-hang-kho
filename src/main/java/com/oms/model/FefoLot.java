
package com.oms.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FefoLot(
        long id,
        long warehouseId,
        long productId,
        String lotNo,
        LocalDate expiryDate,
        BigDecimal remainingQuantity,
        boolean recommended,
        BigDecimal suggestedQuantity
) {
    /** Giữ tương thích với các chỗ đang tạo FefoLot bằng 7 tham số. */
    public FefoLot(
            long id,
            long warehouseId,
            long productId,
            String lotNo,
            LocalDate expiryDate,
            BigDecimal remainingQuantity,
            boolean recommended
    ) {
        this(id, warehouseId, productId, lotNo, expiryDate,
                remainingQuantity, recommended, BigDecimal.ZERO);
    }
}
