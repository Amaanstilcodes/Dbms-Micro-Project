package com.isp.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Mirrors the `plan` table.
 */
public class Plan {

    private int        planId;
    private String     planName;
    private String     planType;       // BROADBAND | FIBER | WIRELESS | LEASED_LINE
    private int        speedMbps;
    private int        uploadMbps;
    private Integer    dataCapGb;      // null = unlimited
    private BigDecimal price;
    private String     billingCycle;   // MONTHLY | QUARTERLY | ANNUALLY
    private boolean    isActive;
    private String     description;
    private LocalDateTime createdAt;

    public Plan() {}

    public Plan(int planId, String planName, String planType,
                int speedMbps, int uploadMbps, Integer dataCapGb,
                BigDecimal price, String billingCycle,
                boolean isActive, String description, LocalDateTime createdAt) {
        this.planId       = planId;
        this.planName     = planName;
        this.planType     = planType;
        this.speedMbps    = speedMbps;
        this.uploadMbps   = uploadMbps;
        this.dataCapGb    = dataCapGb;
        this.price        = price;
        this.billingCycle = billingCycle;
        this.isActive     = isActive;
        this.description  = description;
        this.createdAt    = createdAt;
    }

    /** Display label for combo boxes. */
    public String getDisplayLabel() {
        String cap = (dataCapGb == null) ? "Unlimited" : dataCapGb + " GB";
        return planName + " | " + speedMbps + " Mbps | " + cap + " | ₹" + price;
    }

    // --- Getters & Setters ---

    public int        getPlanId()       { return planId; }
    public void       setPlanId(int v)  { planId = v; }

    public String     getPlanName()     { return planName; }
    public void       setPlanName(String v){ planName = v; }

    public String     getPlanType()     { return planType; }
    public void       setPlanType(String v){ planType = v; }

    public int        getSpeedMbps()    { return speedMbps; }
    public void       setSpeedMbps(int v){ speedMbps = v; }

    public int        getUploadMbps()   { return uploadMbps; }
    public void       setUploadMbps(int v){ uploadMbps = v; }

    public Integer    getDataCapGb()    { return dataCapGb; }
    public void       setDataCapGb(Integer v){ dataCapGb = v; }

    public BigDecimal getPrice()        { return price; }
    public void       setPrice(BigDecimal v){ price = v; }

    public String     getBillingCycle() { return billingCycle; }
    public void       setBillingCycle(String v){ billingCycle = v; }

    public boolean    isActive()        { return isActive; }
    public void       setActive(boolean v){ isActive = v; }

    public String     getDescription()  { return description; }
    public void       setDescription(String v){ description = v; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void       setCreatedAt(LocalDateTime v){ createdAt = v; }

    @Override
    public String toString() { return getDisplayLabel(); }
}
