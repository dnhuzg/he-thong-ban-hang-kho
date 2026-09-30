package com.oms.model;

/** Danh mục đơn giản có id + tên (kho, địa bàn). */
public class NamedItem {

    private final long id;
    private final String name;

    public NamedItem(long id, String name) {
        this.id = id;
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
