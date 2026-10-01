package com.oms.service;

/** Người dùng đăng nhập hợp lệ nhưng không đủ quyền cho thao tác này; servlet trả mã 403. */
public class ForbiddenException extends ServiceException {

    public ForbiddenException(String message) {
        super(message);
    }
}
