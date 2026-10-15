package com.oms.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Phiếu nhập kho từ nhà cung cấp (S5-04). Dạng record để trang phiếu nhập đọc qua JSON. */
public record GoodsReceipt(long id, String code, long supplierId, String supplierName, String documentNo,
                           LocalDate receivedDate, long warehouseId, String warehouseName, String note,
                           String status, List<Line> lines) {

    public static final String DRAFT = "DRAFT";
    public static final String CONFIRMED = "CONFIRMED";

    public boolean confirmed() {
        return CONFIRMED.equals(status);
    }

    /** Một dòng hàng: số lượng theo đơn vị trên chứng từ kèm bản chụp hệ số và số lượng quy đổi. */
    public record Line(long productId, long unitId, BigDecimal quantity, BigDecimal factor,
                       BigDecimal baseQuantity, String lotNo, LocalDate expiryDate, LocalDate manufacturedDate) {
    }

    /** Dòng trong danh sách phiếu gần đây. */
    public record Summary(long id, String code, String documentNo, LocalDate receivedDate,
                          String supplierName, String warehouseName, String status) {
    }
}
