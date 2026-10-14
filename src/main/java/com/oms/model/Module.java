package com.oms.model;

/** Module nghiệp vụ trong ma trận phân quyền. */
public enum Module {
    PRODUCT("Sản phẩm & bảng giá"),
    CUSTOMER_CREDIT("Đại lý & hạn mức công nợ"),
    ORDER("Đơn hàng"),
    ORDER_APPROVAL("Duyệt đơn & giá đặc biệt"),
    INVENTORY("Tồn kho & nhập kho"),
    SHIPMENT("Phiếu xuất & giao hàng"),
    INVOICE("Hoá đơn & công nợ"),
    RETURN("Trả hàng & điều chỉnh"),
    REPORT("Báo cáo & dashboard"),
    USER_AUDIT("Người dùng & nhật ký");

    private final String label;

    Module(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public String getCode() {
        return name();
    }
}
