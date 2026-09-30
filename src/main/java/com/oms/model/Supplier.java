package com.oms.model;

/** Nhà cung cấp (S2-09). */
public class Supplier {

    public enum Status {
        ACTIVE("Đang giao dịch"),
        INACTIVE("Ngừng giao dịch");

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
    private String taxCode;
    private String contactName;
    private String contactPhone;
    private String email;
    private String address;
    private String paymentTerms;
    private Status status = Status.ACTIVE;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTaxCode() { return taxCode; }
    public void setTaxCode(String taxCode) { this.taxCode = taxCode; }
    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPaymentTerms() { return paymentTerms; }
    public void setPaymentTerms(String paymentTerms) { this.paymentTerms = paymentTerms; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
}
