package com.oms.service;

import com.oms.model.Role;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;

/** Dữ liệu người dùng nhập ở form tạo / sửa tài khoản. */
public class UserForm {

    private long id;
    private String fullName = "";
    private String username = "";
    private String email = "";
    private String phone = "";
    private Set<Role> roles = EnumSet.noneOf(Role.class);
    private Set<Long> warehouseIds = new LinkedHashSet<>();
    private Set<Long> areaIds = new LinkedHashSet<>();

    public boolean isNew() {
        return id == 0;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Set<Role> getRoles() { return roles; }
    public void setRoles(Set<Role> roles) { this.roles = roles; }
    public Set<Long> getWarehouseIds() { return warehouseIds; }
    public void setWarehouseIds(Set<Long> warehouseIds) { this.warehouseIds = warehouseIds; }
    public Set<Long> getAreaIds() { return areaIds; }
    public void setAreaIds(Set<Long> areaIds) { this.areaIds = areaIds; }

    // Dùng trong JSP để đánh dấu checkbox đã chọn
    public boolean hasRole(String code) {
        return roles.stream().anyMatch(r -> r.name().equals(code));
    }
}
