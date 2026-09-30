package com.oms.web;

public final class SessionKeys {

    public static final String USER_ID = "userId";
    public static final String SESSION_VERSION = "sessionVersion";
    public static final String CSRF_TOKEN = "csrfToken";
    public static final String FLASH = "flash";

    /** Request attribute chứa User đã nạp mới từ DB ở mỗi request. */
    public static final String CURRENT_USER = "currentUser";

    public static final int SESSION_TIMEOUT_SECONDS = 30 * 60;

    private SessionKeys() {
    }
}
