package com.isp.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Mirrors the `invoice` table.
 */
public class Invoice {

    private int        invoiceId;
    private int        subscriptionId;
    private String     customerName;   // joined
    private String     planName;       // joined
    private LocalDate  billingPeriodStart;
    private LocalDate  billingPeriodEnd;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal lateFee;
    private BigDecimal totalAmount;
    private BigDecimal amountPaid;
    private LocalDate  dueDate;
    private String     status; // DRAFT|ISSUED|PARTIALLY_PAID|PAID|OVERDUE|CANCELLED
    private LocalDateTime issuedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Invoice() {}

    /** Outstanding = total - paid. */
    public BigDecimal getOutstanding() {
        return totalAmount.subtract(amountPaid);
    }

    // --- Getters & Setters ---

    public int        getInvoiceId()               { return invoiceId; }
    public void       setInvoiceId(int v)          { invoiceId = v; }

    public int        getSubscriptionId()          { return subscriptionId; }
    public void       setSubscriptionId(int v)     { subscriptionId = v; }

    public String     getCustomerName()            { return customerName; }
    public void       setCustomerName(String v)    { customerName = v; }

    public String     getPlanName()                { return planName; }
    public void       setPlanName(String v)        { planName = v; }

    public LocalDate  getBillingPeriodStart()      { return billingPeriodStart; }
    public void       setBillingPeriodStart(LocalDate v){ billingPeriodStart = v; }

    public LocalDate  getBillingPeriodEnd()        { return billingPeriodEnd; }
    public void       setBillingPeriodEnd(LocalDate v)  { billingPeriodEnd = v; }

    public BigDecimal getSubtotal()                { return subtotal; }
    public void       setSubtotal(BigDecimal v)    { subtotal = v; }

    public BigDecimal getTaxAmount()               { return taxAmount; }
    public void       setTaxAmount(BigDecimal v)   { taxAmount = v; }

    public BigDecimal getDiscountAmount()          { return discountAmount; }
    public void       setDiscountAmount(BigDecimal v){ discountAmount = v; }

    public BigDecimal getLateFee()                 { return lateFee; }
    public void       setLateFee(BigDecimal v)     { lateFee = v; }

    public BigDecimal getTotalAmount()             { return totalAmount; }
    public void       setTotalAmount(BigDecimal v) { totalAmount = v; }

    public BigDecimal getAmountPaid()              { return amountPaid; }
    public void       setAmountPaid(BigDecimal v)  { amountPaid = v; }

    public LocalDate  getDueDate()                 { return dueDate; }
    public void       setDueDate(LocalDate v)      { dueDate = v; }

    public String     getStatus()                  { return status; }
    public void       setStatus(String v)          { status = v; }

    public LocalDateTime getIssuedAt()             { return issuedAt; }
    public void       setIssuedAt(LocalDateTime v) { issuedAt = v; }

    public LocalDateTime getCreatedAt()            { return createdAt; }
    public void       setCreatedAt(LocalDateTime v){ createdAt = v; }

    public LocalDateTime getUpdatedAt()            { return updatedAt; }
    public void       setUpdatedAt(LocalDateTime v){ updatedAt = v; }
}
