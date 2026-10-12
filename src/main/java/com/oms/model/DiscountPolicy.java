package com.oms.model;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Chính sách chiết khấu theo sản lượng (S3-01): áp cho một sản phẩm hoặc một nhóm hàng, gồm nhiều bậc theo số lượng. */
public class DiscountPolicy {

    public enum Type {
        PERCENT("Phần trăm (%)"), AMOUNT("Số tiền trên mỗi đơn vị");

        private final String label;

        Type(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public enum Status {
        ACTIVE("Đang bật"), INACTIVE("Tạm dừng");

        private final String label;

        Status(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private long id;
    private String name;
    private Long productId;
    private String productLabel;
    private Long categoryId;
    private String categoryPath;
    private Type type = Type.PERCENT;
    private LocalDate validFrom;
    private LocalDate validTo;
    private Status status = Status.ACTIVE;
    private List<DiscountTier> tiers = new ArrayList<>();

    public boolean isProductScope() {
        return productId != null;
    }

    public String getScopeKind() {
        return isProductScope() ? "Sản phẩm" : "Nhóm hàng";
    }

    public String getScopeText() {
        return isProductScope() ? productLabel : categoryPath;
    }

    public boolean isValidOn(LocalDate date) {
        return (validFrom == null || !validFrom.isAfter(date)) && (validTo == null || !validTo.isBefore(date));
    }

    /** Trạng thái hiển thị theo ngày hôm nay: Tạm dừng, Chưa hiệu lực, Đã hết hạn hoặc Đang áp dụng. */
    public String getStateLabel() {
        LocalDate today = LocalDate.now();
        if (status == Status.INACTIVE) {
            return Status.INACTIVE.getLabel();
        }
        if (validFrom != null && validFrom.isAfter(today)) {
            return "Chưa hiệu lực";
        }
        if (validTo != null && validTo.isBefore(today)) {
            return "Đã hết hạn";
        }
        return "Đang áp dụng";
    }

    public String getValidityText() {
        if (validFrom == null && validTo == null) {
            return "Không thời hạn";
        }
        if (validTo == null) {
            return "Từ " + validFrom.format(DATE);
        }
        if (validFrom == null) {
            return "Đến " + validTo.format(DATE);
        }
        return validFrom.format(DATE) + " → " + validTo.format(DATE);
    }

    /** Ví dụ: "từ 100: 3% · từ 500: 5%" hoặc "từ 100: 500 ₫/đơn vị". */
    public String getTiersText() {
        StringBuilder sb = new StringBuilder();
        for (DiscountTier t : tiers) {
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append("từ ").append(t.getMinQty()).append(": ").append(valueText(t.getValue()));
        }
        return sb.toString();
    }

    public String valueText(BigDecimal value) {
        return valueText(type, value);
    }

    public static String valueText(Type type, BigDecimal value) {
        NumberFormat nf = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));
        nf.setMaximumFractionDigits(2);
        return type == Type.PERCENT ? nf.format(value) + "%" : nf.format(value) + " ₫/đơn vị";
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductLabel() { return productLabel; }
    public void setProductLabel(String productLabel) { this.productLabel = productLabel; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getCategoryPath() { return categoryPath; }
    public void setCategoryPath(String categoryPath) { this.categoryPath = categoryPath; }
    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
    public LocalDate getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDate validFrom) { this.validFrom = validFrom; }
    public LocalDate getValidTo() { return validTo; }
    public void setValidTo(LocalDate validTo) { this.validTo = validTo; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public List<DiscountTier> getTiers() { return tiers; }
    public void setTiers(List<DiscountTier> tiers) { this.tiers = tiers; }
}
