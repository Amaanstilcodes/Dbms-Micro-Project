package com.isp.model;

import java.time.LocalDateTime;

/**
 * Mirrors the `support_ticket` table.
 */
public class SupportTicket {

    private int           ticketId;
    private int           customerId;
    private String        customerName;    // joined
    private Integer       subscriptionId;
    private String        category;        // CONNECTIVITY|BILLING|EQUIPMENT|SPEED|OTHER
    private String        priority;        // LOW|MEDIUM|HIGH|CRITICAL
    private String        subject;
    private String        description;
    private String        status;          // OPEN|IN_PROGRESS|PENDING_CUSTOMER|RESOLVED|CLOSED
    private Integer       assignedTo;      // technician_id
    private String        technicianName;  // joined
    private String        resolution;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;

    public SupportTicket() {}

    // --- Getters & Setters ---

    public int           getTicketId()              { return ticketId; }
    public void          setTicketId(int v)         { ticketId = v; }

    public int           getCustomerId()            { return customerId; }
    public void          setCustomerId(int v)       { customerId = v; }

    public String        getCustomerName()          { return customerName; }
    public void          setCustomerName(String v)  { customerName = v; }

    public Integer       getSubscriptionId()        { return subscriptionId; }
    public void          setSubscriptionId(Integer v){ subscriptionId = v; }

    public String        getCategory()              { return category; }
    public void          setCategory(String v)      { category = v; }

    public String        getPriority()              { return priority; }
    public void          setPriority(String v)      { priority = v; }

    public String        getSubject()               { return subject; }
    public void          setSubject(String v)       { subject = v; }

    public String        getDescription()           { return description; }
    public void          setDescription(String v)   { description = v; }

    public String        getStatus()                { return status; }
    public void          setStatus(String v)        { status = v; }

    public Integer       getAssignedTo()            { return assignedTo; }
    public void          setAssignedTo(Integer v)   { assignedTo = v; }

    public String        getTechnicianName()        { return technicianName; }
    public void          setTechnicianName(String v){ technicianName = v; }

    public String        getResolution()            { return resolution; }
    public void          setResolution(String v)    { resolution = v; }

    public LocalDateTime getCreatedAt()             { return createdAt; }
    public void          setCreatedAt(LocalDateTime v){ createdAt = v; }

    public LocalDateTime getUpdatedAt()             { return updatedAt; }
    public void          setUpdatedAt(LocalDateTime v){ updatedAt = v; }

    public LocalDateTime getResolvedAt()            { return resolvedAt; }
    public void          setResolvedAt(LocalDateTime v){ resolvedAt = v; }
}
