package com.isp.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Mirrors the `payment` table.
 */
public class Payment {

    private int        paymentId;
    private int        invoiceId;
    private String     customerName;   // joined
    private BigDecimal amount;
    private String     paymentMethod;  // CASH|CARD|UPI|BANK_TRANSFER|CHEQUE
    private String     transactionRef;
    private LocalDate  paymentDate;
    private Integer    recordedBy;     // user_id
    private String     notes;
    private LocalDateTime createdAt;

    public Payment() {}

    // --- Getters & Setters ---

    public int        getPaymentId()               { return paymentId; }
    public void       setPaymentId(int v)          { paymentId = v; }

    public int        getInvoiceId()               { return invoiceId; }
    public void       setInvoiceId(int v)          { invoiceId = v; }

    public String     getCustomerName()            { return customerName; }
    public void       setCustomerName(String v)    { customerName = v; }

    public BigDecimal getAmount()                  { return amount; }
    public void       setAmount(BigDecimal v)      { amount = v; }

    public String     getPaymentMethod()           { return paymentMethod; }
    public void       setPaymentMethod(String v)   { paymentMethod = v; }

    public String     getTransactionRef()          { return transactionRef; }
    public void       setTransactionRef(String v)  { transactionRef = v; }

    public LocalDate  getPaymentDate()             { return paymentDate; }
    public void       setPaymentDate(LocalDate v)  { paymentDate = v; }

    public Integer    getRecordedBy()              { return recordedBy; }
    public void       setRecordedBy(Integer v)     { recordedBy = v; }

    public String     getNotes()                   { return notes; }
    public void       setNotes(String v)           { notes = v; }

    public LocalDateTime getCreatedAt()            { return createdAt; }
    public void       setCreatedAt(LocalDateTime v){ createdAt = v; }
}
