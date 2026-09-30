package com.oms.service;

/** Lỗi nghiệp vụ có thông báo hiển thị được cho người dùng. */
public class ServiceException extends RuntimeException {

    public ServiceException(String message) {
        super(message);
    }
}
