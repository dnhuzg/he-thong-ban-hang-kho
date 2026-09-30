package com.oms.service;

import java.math.BigDecimal;

/** Đọc số tiền người dùng nhập (giá vốn, giá bán, giá sàn); lưu DECIMAL 2 chữ số thập phân. */
public final class Money {

    private static final BigDecimal MAX = new BigDecimal("9999999999999.99");

    private Money() {
    }

    /**
     * Nhận "7200", "7200.5", "7.200", "1.250.000" hoặc "7,200.50"; bỏ trống trả về null.
     * label dùng trong thông báo lỗi, ví dụ "Giá vốn".
     */
    public static BigDecimal parse(String raw, String label) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String cleaned = raw.trim().replace(" ", "");
        // Dấu chấm phân tách hàng nghìn kiểu Việt Nam: "7.200" hoặc "1.250.000"
        if (cleaned.matches("\\d{1,3}(\\.\\d{3})+")) {
            cleaned = cleaned.replace(".", "");
        }
        cleaned = cleaned.replace(",", "");
        try {
            BigDecimal value = new BigDecimal(cleaned);
            if (value.signum() < 0 || value.compareTo(MAX) > 0 || value.scale() > 2) {
                throw new NumberFormatException();
            }
            return value.setScale(2);
        } catch (NumberFormatException e) {
            throw new ServiceException(label + " phải là số không âm, tối đa 2 chữ số thập phân.");
        }
    }

    public static String text(BigDecimal value) {
        return value == null ? "—" : value.stripTrailingZeros().toPlainString();
    }
}
