package com.oms.model;

import java.util.LinkedHashSet;
import java.util.Set;

/** Dòng tệp nhập người dùng (S2-01): giữ chữ gốc để xem trước, kèm vai trò / kho / địa bàn đã đối chiếu. */
public class UserImportRow extends ImportRow {

    private String fullName = "";
    private String username = "";
    private String email = "";
    private String phone = "";
    private String roleText = "";
    private String warehouseText = "";
    private String areaText = "";
    private final Set<Role> roles = new LinkedHashSet<>();
    private final Set<Long> warehouseIds = new LinkedHashSet<>();
    private final Set<Long> areaIds = new LinkedHashSet<>();
    private boolean emailSent;

    public String getRoleLabels() {
        return roles.stream().map(Role::getLabel).reduce((a, b) -> a + ", " + b).orElse("");
    }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getRoleText() { return roleText; }
    public void setRoleText(String roleText) { this.roleText = roleText; }
    public String getWarehouseText() { return warehouseText; }
    public void setWarehouseText(String warehouseText) { this.warehouseText = warehouseText; }
    public String getAreaText() { return areaText; }
    public void setAreaText(String areaText) { this.areaText = areaText; }
    public Set<Role> getRoles() { return roles; }
    public Set<Long> getWarehouseIds() { return warehouseIds; }
    public Set<Long> getAreaIds() { return areaIds; }
    public boolean isEmailSent() { return emailSent; }
    public void setEmailSent(boolean emailSent) { this.emailSent = emailSent; }
}
