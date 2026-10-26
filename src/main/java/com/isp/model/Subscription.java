package com.isp.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Mirrors the `subscription` table.
 */
public class Subscription {

    private int        subscriptionId;
    private int        customerId;
    private String     customerName;   // joined
    private int        planId;
    private String     planName;       // joined
    private int        areaId;
    private String     areaName;       // joined
    private LocalDate  startDate;
    private LocalDate  endDate;
    private String     status;         // PENDING | ACTIVE | SUSPENDED | TERMINATED
    private String     notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Subscription() {}

    // --- Getters & Setters ---

    public int        getSubscriptionId()         { return subscriptionId; }
    public void       setSubscriptionId(int v)    { subscriptionId = v; }

    public int        getCustomerId()             { return customerId; }
    public void       setCustomerId(int v)        { customerId = v; }

    public String     getCustomerName()           { return customerName; }
    public void       setCustomerName(String v)   { customerName = v; }

    public int        getPlanId()                 { return planId; }
    public void       setPlanId(int v)            { planId = v; }

    public String     getPlanName()               { return planName; }
    public void       setPlanName(String v)       { planName = v; }

    public int        getAreaId()                 { return areaId; }
    public void       setAreaId(int v)            { areaId = v; }

    public String     getAreaName()               { return areaName; }
    public void       setAreaName(String v)       { areaName = v; }

    public LocalDate  getStartDate()              { return startDate; }
    public void       setStartDate(LocalDate v)   { startDate = v; }

    public LocalDate  getEndDate()                { return endDate; }
    public void       setEndDate(LocalDate v)     { endDate = v; }

    public String     getStatus()                 { return status; }
    public void       setStatus(String v)         { status = v; }

    public String     getNotes()                  { return notes; }
    public void       setNotes(String v)          { notes = v; }

    public LocalDateTime getCreatedAt()           { return createdAt; }
    public void       setCreatedAt(LocalDateTime v){ createdAt = v; }

    public LocalDateTime getUpdatedAt()           { return updatedAt; }
    public void       setUpdatedAt(LocalDateTime v){ updatedAt = v; }
}
