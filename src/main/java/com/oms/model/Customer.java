package com.oms.model;

import java.time.LocalDateTime;

/** Đại lý (EP-03). Nhóm khách hàng quyết định bảng giá áp dụng; người phụ trách quyết định ai được xem. */
public class Customer {

    public enum Status {
        ACTIVE("Đang giao dịch"),
        LOCKED("Bị khoá giao dịch");

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
    private String code;
    private String name;
    private String phone;
    private String taxCode;
    private Long groupId;
    private String groupName;
    private Long areaId;
    private String areaName;
    private Long salesRepId;
    private String salesRepName;
    private Status status = Status.ACTIVE;
    private LocalDateTime createdAt;

    public boolean isLocked() {
        return status == Status.LOCKED;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getTaxCode() { return taxCode; }
    public void setTaxCode(String taxCode) { this.taxCode = taxCode; }
    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public Long getAreaId() { return areaId; }
    public void setAreaId(Long areaId) { this.areaId = areaId; }
    public String getAreaName() { return areaName; }
    public void setAreaName(String areaName) { this.areaName = areaName; }
    public Long getSalesRepId() { return salesRepId; }
    public void setSalesRepId(Long salesRepId) { this.salesRepId = salesRepId; }
    public String getSalesRepName() { return salesRepName; }
    public void setSalesRepName(String salesRepName) { this.salesRepName = salesRepName; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
