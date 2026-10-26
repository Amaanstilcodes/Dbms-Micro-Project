package com.isp.model;

import java.time.LocalDateTime;

/**
 * Mirrors the `customer` table.
 */
public class Customer {

    private int           customerId;
    private String        customerType;   // INDIVIDUAL | BUSINESS
    private String        firstName;
    private String        lastName;
    private String        companyName;
    private String        email;
    private String        phone;
    private String        alternatePhone;
    private int           areaId;
    private String        areaName;       // joined from service_area (read-only)
    private String        status;         // ACTIVE | INACTIVE | BLACKLISTED
    private String        idProofType;
    private String        idProofNumber;
    private String        gstNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // --- Constructors ---

    public Customer() {}

    public Customer(int customerId, String customerType, String firstName, String lastName,
                    String companyName, String email, String phone, String alternatePhone,
                    int areaId, String status, String idProofType, String idProofNumber,
                    String gstNumber, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.customerId    = customerId;
        this.customerType  = customerType;
        this.firstName     = firstName;
        this.lastName      = lastName;
        this.companyName   = companyName;
        this.email         = email;
        this.phone         = phone;
        this.alternatePhone = alternatePhone;
        this.areaId        = areaId;
        this.status        = status;
        this.idProofType   = idProofType;
        this.idProofNumber = idProofNumber;
        this.gstNumber     = gstNumber;
        this.createdAt     = createdAt;
        this.updatedAt     = updatedAt;
    }

    /** Convenience: full name for display. */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    // --- Getters & Setters ---

    public int           getCustomerId()    { return customerId; }
    public void          setCustomerId(int v){ customerId = v; }

    public String        getCustomerType()  { return customerType; }
    public void          setCustomerType(String v){ customerType = v; }

    public String        getFirstName()     { return firstName; }
    public void          setFirstName(String v){ firstName = v; }

    public String        getLastName()      { return lastName; }
    public void          setLastName(String v){ lastName = v; }

    public String        getCompanyName()   { return companyName; }
    public void          setCompanyName(String v){ companyName = v; }

    public String        getEmail()         { return email; }
    public void          setEmail(String v) { email = v; }

    public String        getPhone()         { return phone; }
    public void          setPhone(String v) { phone = v; }

    public String        getAlternatePhone(){ return alternatePhone; }
    public void          setAlternatePhone(String v){ alternatePhone = v; }

    public int           getAreaId()        { return areaId; }
    public void          setAreaId(int v)   { areaId = v; }

    public String        getAreaName()      { return areaName; }
    public void          setAreaName(String v){ areaName = v; }

    public String        getStatus()        { return status; }
    public void          setStatus(String v){ status = v; }

    public String        getIdProofType()   { return idProofType; }
    public void          setIdProofType(String v){ idProofType = v; }

    public String        getIdProofNumber() { return idProofNumber; }
    public void          setIdProofNumber(String v){ idProofNumber = v; }

    public String        getGstNumber()     { return gstNumber; }
    public void          setGstNumber(String v){ gstNumber = v; }

    public LocalDateTime getCreatedAt()     { return createdAt; }
    public void          setCreatedAt(LocalDateTime v){ createdAt = v; }

    public LocalDateTime getUpdatedAt()     { return updatedAt; }
    public void          setUpdatedAt(LocalDateTime v){ updatedAt = v; }

    @Override
    public String toString() {
        return "[" + customerId + "] " + getFullName() + " <" + email + ">";
    }
}
