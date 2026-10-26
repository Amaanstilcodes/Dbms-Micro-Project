package com.isp.dao;

import com.isp.model.Invoice;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * DAO interface for {@link Invoice}.
 */
public interface InvoiceDAO {

    List<Invoice> findAll();

    List<Invoice> findBySubscriptionId(int subscriptionId);

    List<Invoice> findOverdue();

    Optional<Invoice> findById(int invoiceId);

    int insert(Invoice invoice);

    void update(Invoice invoice);

    void updateStatus(int invoiceId, String status);

    /** Add to amount_paid and recalculate status. */
    void recordPayment(int invoiceId, BigDecimal amount);

    /** Monthly revenue total (dashboard KPI). */
    BigDecimal getMonthlyRevenue();

    int countOverdue();
}
