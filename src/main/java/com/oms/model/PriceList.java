package com.oms.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Bảng giá của một nhóm khách hàng trong một khoảng hiệu lực (S2-10). */
public class PriceList {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private long id;
    private String code;
    private int version;
    private String name;
    private long customerGroupId;
    private String customerGroupName;
    private LocalDate validFrom;
    private LocalDate validTo;
    private Long previousId;
    private LocalDateTime lockedAt;
    private boolean hasNewerVersion;
    private long itemCount;
    private List<PriceListItem> items = List.of();

    public boolean isLocked() {
        return lockedAt != null;
    }

    /** Sửa trực tiếp chỉ khi chưa phát sinh đơn và chưa bị phiên bản mới hơn thay thế. */
    public boolean isEditable() {
        return !isLocked() && !hasNewerVersion;
    }

    /** Trạng thái hiệu lực so với ngày hôm nay. */
    public String getValidityLabel() {
        LocalDate today = LocalDate.now();
        if (validFrom.isAfter(today)) {
            return "Sắp áp dụng";
        }
        if (validTo != null && validTo.isBefore(today)) {
            return "Hết hiệu lực";
        }
        return "Đang áp dụng";
    }

    public String getValidFromText() {
        return validFrom == null ? "" : validFrom.format(DATE);
    }

    public String getValidToText() {
        return validTo == null ? "Không thời hạn" : validTo.format(DATE);
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public long getCustomerGroupId() { return customerGroupId; }
    public void setCustomerGroupId(long customerGroupId) { this.customerGroupId = customerGroupId; }
    public String getCustomerGroupName() { return customerGroupName; }
    public void setCustomerGroupName(String customerGroupName) { this.customerGroupName = customerGroupName; }
    public LocalDate getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDate validFrom) { this.validFrom = validFrom; }
    public LocalDate getValidTo() { return validTo; }
    public void setValidTo(LocalDate validTo) { this.validTo = validTo; }
    public Long getPreviousId() { return previousId; }
    public void setPreviousId(Long previousId) { this.previousId = previousId; }
    public LocalDateTime getLockedAt() { return lockedAt; }
    public void setLockedAt(LocalDateTime lockedAt) { this.lockedAt = lockedAt; }
    public boolean isHasNewerVersion() { return hasNewerVersion; }
    public void setHasNewerVersion(boolean hasNewerVersion) { this.hasNewerVersion = hasNewerVersion; }
    public long getItemCount() { return itemCount; }
    public void setItemCount(long itemCount) { this.itemCount = itemCount; }
    public List<PriceListItem> getItems() { return items; }
    public void setItems(List<PriceListItem> items) { this.items = items; }
}
