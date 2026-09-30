package com.oms.model;

/** Mức quyền trong ma trận. NONE không lưu trong DB (thiếu dòng = không truy cập). */
public enum PermissionLevel {
    NONE("–", false, false, false),
    R_OWN("R*", true, false, true),
    R("R", true, false, false),
    W_OWN("W*", true, true, true),
    W("W", true, true, false),
    F("F", true, true, false);

    private final String symbol;
    private final boolean canRead;
    private final boolean canWrite;
    private final boolean ownOnly;

    PermissionLevel(String symbol, boolean canRead, boolean canWrite, boolean ownOnly) {
        this.symbol = symbol;
        this.canRead = canRead;
        this.canWrite = canWrite;
        this.ownOnly = ownOnly;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getCode() {
        return name();
    }

    public boolean canRead() {
        return canRead;
    }

    public boolean canWrite() {
        return canWrite;
    }

    public boolean isOwnOnly() {
        return ownOnly;
    }
}
