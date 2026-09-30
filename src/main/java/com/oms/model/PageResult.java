package com.oms.model;

import java.util.List;

public class PageResult<T> {

    private final List<T> items;
    private final int page;
    private final int pageSize;
    private final long total;

    public PageResult(List<T> items, int page, int pageSize, long total) {
        this.items = items;
        this.page = page;
        this.pageSize = pageSize;
        this.total = total;
    }

    public List<T> getItems() { return items; }
    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }
    public long getTotal() { return total; }

    public int getTotalPages() {
        return (int) Math.max(1, (total + pageSize - 1) / pageSize);
    }

    public long getFrom() {
        return total == 0 ? 0 : (long) (page - 1) * pageSize + 1;
    }

    public long getTo() {
        return Math.min(total, (long) page * pageSize);
    }
}
