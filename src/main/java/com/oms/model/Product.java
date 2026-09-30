package com.oms.model;

import java.math.BigDecimal;

/** Sản phẩm (S2-05). costPrice chỉ được nạp / hiển thị cho người có quyền xem giá vốn. */
public class Product {

    public enum Status {
        ACTIVE("Đang kinh doanh"),
        DISCONTINUED("Ngừng kinh doanh");

        private final String label;

        Status(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public String getCode() {
            return name();
        }
    }

    private long id;
    private String sku;
    private String name;
    private long categoryId;
    private String categoryPath;
    private long baseUnitId;
    private String baseUnitName;
    private String packingSpec;
    private BigDecimal costPrice;
    private String imagePath;
    private Status status;

    public String getImageThumbPath() {
        return imagePath == null ? null : imagePath.replaceFirst("\\.png$", "-64.png");
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public long getCategoryId() { return categoryId; }
    public void setCategoryId(long categoryId) { this.categoryId = categoryId; }
    public String getCategoryPath() { return categoryPath; }
    public void setCategoryPath(String categoryPath) { this.categoryPath = categoryPath; }
    public long getBaseUnitId() { return baseUnitId; }
    public void setBaseUnitId(long baseUnitId) { this.baseUnitId = baseUnitId; }
    public String getBaseUnitName() { return baseUnitName; }
    public void setBaseUnitName(String baseUnitName) { this.baseUnitName = baseUnitName; }
    public String getPackingSpec() { return packingSpec; }
    public void setPackingSpec(String packingSpec) { this.packingSpec = packingSpec; }
    public BigDecimal getCostPrice() { return costPrice; }
    public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
}
