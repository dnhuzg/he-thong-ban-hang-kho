package com.oms.model;

import java.math.BigDecimal;

/** Dòng tệp nhập sản phẩm (S2-08): giữ chữ gốc để xem trước, kèm giá trị đã đối chiếu để ghi. */
public class ProductImportRow extends ImportRow {

    private String sku = "";
    private String name = "";
    private String category = "";
    private String unit = "";
    private String packingSpec = "";
    private String costPrice = "";
    private String status = "";

    /** SKU đã có trong hệ thống: nhập sẽ cập nhật sản phẩm đó thay vì tạo mới. */
    private boolean existing;
    private long categoryId;
    private long unitId;
    private BigDecimal costValue;
    private Product.Status statusValue;

    public String getActionLabel() {
        return existing ? "Cập nhật" : "Tạo mới";
    }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getPackingSpec() { return packingSpec; }
    public void setPackingSpec(String packingSpec) { this.packingSpec = packingSpec; }
    public String getCostPrice() { return costPrice; }
    public void setCostPrice(String costPrice) { this.costPrice = costPrice; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isExisting() { return existing; }
    public void setExisting(boolean existing) { this.existing = existing; }
    public long getCategoryId() { return categoryId; }
    public void setCategoryId(long categoryId) { this.categoryId = categoryId; }
    public long getUnitId() { return unitId; }
    public void setUnitId(long unitId) { this.unitId = unitId; }
    public BigDecimal getCostValue() { return costValue; }
    public void setCostValue(BigDecimal costValue) { this.costValue = costValue; }
    public Product.Status getStatusValue() { return statusValue; }
    public void setStatusValue(Product.Status statusValue) { this.statusValue = statusValue; }
}
