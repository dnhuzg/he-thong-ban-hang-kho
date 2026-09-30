package com.oms.model;

public enum Role {
    CUSTOMER("Đại lý"),
    SALES_REP("Nhân viên kinh doanh"),
    SALES_MANAGER("Quản lý kinh doanh"),
    WAREHOUSE("Nhân viên kho"),
    WH_MANAGER("Quản lý kho"),
    ACCOUNTANT("Kế toán công nợ"),
    ADMIN("Quản trị hệ thống");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public String getCode() {
        return name();
    }

    /** Vai trò kho bắt buộc gắn với ít nhất một kho (S1-09). */
    public boolean requiresWarehouse() {
        return this == WAREHOUSE || this == WH_MANAGER;
    }

    /** Các vai trò có dòng trong ma trận phân quyền; ADMIN luôn toàn quyền nên không cấu hình. */
    public static Role[] configurable() {
        return new Role[] {CUSTOMER, SALES_REP, WAREHOUSE, WH_MANAGER, ACCOUNTANT, SALES_MANAGER};
    }
}
