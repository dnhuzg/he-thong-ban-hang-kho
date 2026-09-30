package com.oms.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class User {

    public enum Status {
        PENDING("Chờ kích hoạt"),
        ACTIVE("Đang hoạt động"),
        LOCKED("Đã khoá");

        private final String label;

        Status(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public String getCode() {
            return name();
        }
    }

    private long id;
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private String passwordHash;
    private Status status;
    private boolean mustChangePassword;
    private int failedLoginCount;
    private LocalDateTime loginLockedUntil;
    private int sessionVersion;
    private String lockReason;
    private Set<Role> roles = EnumSet.noneOf(Role.class);
    private List<NamedItem> warehouses = new ArrayList<>();
    private List<NamedItem> areas = new ArrayList<>();

    public boolean hasRole(Role role) {
        return roles.contains(role);
    }

    public boolean isAdmin() {
        return roles.contains(Role.ADMIN);
    }

    public String getInitials() {
        if (fullName == null || fullName.isBlank()) {
            return "?";
        }
        String[] parts = fullName.trim().split("\\s+");
        String first = parts[0].substring(0, 1);
        String last = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (first + last).toUpperCase();
    }

    /** Chuỗi hiển thị vai trò trên topbar, ví dụ "Quản lý kho, Nhân viên kho". */
    public String getRoleLabels() {
        List<String> labels = new ArrayList<>();
        for (Role r : roles) {
            labels.add(r.getLabel());
        }
        return String.join(", ", labels);
    }

    /** Kho hoặc địa bàn đang làm việc (S1-06). */
    public String getScopeLabel() {
        List<String> names = new ArrayList<>();
        for (NamedItem w : warehouses) {
            names.add(w.getName());
        }
        for (NamedItem a : areas) {
            names.add(a.getName());
        }
        if (!names.isEmpty()) {
            return String.join(", ", names);
        }
        return isAdmin() ? "Toàn hệ thống" : "Chưa được gắn kho / địa bàn";
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public boolean isMustChangePassword() { return mustChangePassword; }
    public void setMustChangePassword(boolean mustChangePassword) { this.mustChangePassword = mustChangePassword; }
    public int getFailedLoginCount() { return failedLoginCount; }
    public void setFailedLoginCount(int failedLoginCount) { this.failedLoginCount = failedLoginCount; }
    public LocalDateTime getLoginLockedUntil() { return loginLockedUntil; }
    public void setLoginLockedUntil(LocalDateTime loginLockedUntil) { this.loginLockedUntil = loginLockedUntil; }
    public int getSessionVersion() { return sessionVersion; }
    public void setSessionVersion(int sessionVersion) { this.sessionVersion = sessionVersion; }
    public String getLockReason() { return lockReason; }
    public void setLockReason(String lockReason) { this.lockReason = lockReason; }
    public Set<Role> getRoles() { return roles; }
    public void setRoles(Set<Role> roles) { this.roles = roles; }
    public List<NamedItem> getWarehouses() { return warehouses; }
    public void setWarehouses(List<NamedItem> warehouses) { this.warehouses = warehouses; }
    public List<NamedItem> getAreas() { return areas; }
    public void setAreas(List<NamedItem> areas) { this.areas = areas; }
}
