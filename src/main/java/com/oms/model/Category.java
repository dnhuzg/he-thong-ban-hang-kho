package com.oms.model;

/** Nhóm hàng trong cây nhiều cấp (S2-05, S2-06). level và path do CategoryTree tính khi dựng cây. */
public class Category {

    private long id;
    private Long parentId;
    private String name;
    private int level;
    private String path;
    private long productCount;
    private long totalProductCount;
    private boolean hasChildren;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public long getProductCount() { return productCount; }
    public void setProductCount(long productCount) { this.productCount = productCount; }
    /** Số sản phẩm của nhóm và mọi nhóm con cháu. */
    public long getTotalProductCount() { return totalProductCount; }
    public void setTotalProductCount(long totalProductCount) { this.totalProductCount = totalProductCount; }
    public boolean isHasChildren() { return hasChildren; }
    public void setHasChildren(boolean hasChildren) { this.hasChildren = hasChildren; }
}
