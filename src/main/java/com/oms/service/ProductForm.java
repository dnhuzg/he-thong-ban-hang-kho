package com.oms.service;

/** Dữ liệu người dùng nhập ở form sản phẩm (S2-05); giá vốn giữ dạng chữ để báo lỗi định dạng rõ ràng. */
public class ProductForm {

    private long id;
    private String sku = "";
    private String name = "";
    private long categoryId;
    private long baseUnitId;
    private String packingSpec = "";
    private String costPrice = "";
    private String status = "ACTIVE";

    public boolean isNew() {
        return id == 0;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public long getCategoryId() { return categoryId; }
    public void setCategoryId(long categoryId) { this.categoryId = categoryId; }
    public long getBaseUnitId() { return baseUnitId; }
    public void setBaseUnitId(long baseUnitId) { this.baseUnitId = baseUnitId; }
    public String getPackingSpec() { return packingSpec; }
    public void setPackingSpec(String packingSpec) { this.packingSpec = packingSpec; }
    public String getCostPrice() { return costPrice; }
    public void setCostPrice(String costPrice) { this.costPrice = costPrice; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
