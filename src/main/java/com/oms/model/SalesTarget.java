package com.oms.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class SalesTarget {

    private long id;
    private long employeeId;
    private long areaId;
    private LocalDate targetMonth;
    private BigDecimal targetAmount;
    private long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getEmployeeId() { return employeeId; }
    public void setEmployeeId(long employeeId) {
        this.employeeId = employeeId;
    }

    public long getAreaId() { return areaId; }
    public void setAreaId(long areaId) {
        this.areaId = areaId;
    }

    public LocalDate getTargetMonth() { return targetMonth; }
    public void setTargetMonth(LocalDate targetMonth) {
        this.targetMonth = targetMonth;
    }

    public BigDecimal getTargetAmount() { return targetAmount; }
    public void setTargetAmount(BigDecimal targetAmount) {
        this.targetAmount = targetAmount;
    }

    public long getCreatedBy() { return createdBy; }
    public void setCreatedBy(long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
