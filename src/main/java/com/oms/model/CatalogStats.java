package com.oms.model;

/** Số liệu tổng quan ở đầu trang Sản phẩm. */
public class CatalogStats {

    private long totalProducts;
    private long activeProducts;
    private long newThisMonth;
    private long effectivePriceLists;
    private long activeWithoutPrice;

    public long getTotalProducts() { return totalProducts; }
    public void setTotalProducts(long totalProducts) { this.totalProducts = totalProducts; }

    public long getActiveProducts() { return activeProducts; }
    public void setActiveProducts(long activeProducts) { this.activeProducts = activeProducts; }

    public long getDiscontinuedProducts() { return totalProducts - activeProducts; }

    public long getNewThisMonth() { return newThisMonth; }
    public void setNewThisMonth(long newThisMonth) { this.newThisMonth = newThisMonth; }

    public long getEffectivePriceLists() { return effectivePriceLists; }
    public void setEffectivePriceLists(long effectivePriceLists) { this.effectivePriceLists = effectivePriceLists; }

    /** Sản phẩm đang kinh doanh nhưng chưa có trong bảng giá nào đang hiệu lực, nên chưa thêm được vào đơn hàng. */
    public long getActiveWithoutPrice() { return activeWithoutPrice; }
    public void setActiveWithoutPrice(long activeWithoutPrice) { this.activeWithoutPrice = activeWithoutPrice; }
}
