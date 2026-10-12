package com.oms.model;

/** Điểm giao hàng của một đại lý (S3-04). Mỗi đại lý có tối đa một điểm mặc định. */
public class DeliveryPoint {

    private long id;
    private long customerId;
    private String address;
    private String receiverName;
    private String receiverPhone;
    private String routeNote;
    private boolean defaultPoint;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getCustomerId() { return customerId; }
    public void setCustomerId(long customerId) { this.customerId = customerId; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }
    public String getReceiverPhone() { return receiverPhone; }
    public void setReceiverPhone(String receiverPhone) { this.receiverPhone = receiverPhone; }
    public String getRouteNote() { return routeNote; }
    public void setRouteNote(String routeNote) { this.routeNote = routeNote; }
    public boolean isDefaultPoint() { return defaultPoint; }
    public void setDefaultPoint(boolean defaultPoint) { this.defaultPoint = defaultPoint; }
}
