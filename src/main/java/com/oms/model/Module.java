package com.oms.model;

/** Module nghiệp vụ trong ma trận phân quyền. url = null nghĩa là chưa làm (hiện "Sắp có" trên menu). */
public enum Module {
    PRODUCT("Sản phẩm & bảng giá", null),
    CUSTOMER_CREDIT("Đại lý & hạn mức công nợ", null),
    ORDER("Đơn hàng", null),
    ORDER_APPROVAL("Duyệt đơn & giá đặc biệt", null),
    INVENTORY("Tồn kho & nhập kho", null),
    SHIPMENT("Phiếu xuất & giao hàng", null),
    INVOICE("Hoá đơn & công nợ", null),
    RETURN("Trả hàng & điều chỉnh", null),
    REPORT("Báo cáo & dashboard", null),
    USER_AUDIT("Người dùng & nhật ký", "/admin/users");

    private final String label;
    private final String url;

    Module(String label, String url) {
        this.label = label;
        this.url = url;
    }

    public String getLabel() {
        return label;
    }

    public String getUrl() {
        return url;
    }

    public String getCode() {
        return name();
    }
}
