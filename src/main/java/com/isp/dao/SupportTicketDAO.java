package com.isp.dao;

import com.isp.model.SupportTicket;

import java.util.List;
import java.util.Optional;

/**
 * DAO interface for {@link SupportTicket}.
 */
public interface SupportTicketDAO {

    List<SupportTicket> findAll();

    List<SupportTicket> findByCustomerId(int customerId);

    List<SupportTicket> findOpen();

    Optional<SupportTicket> findById(int ticketId);

    int insert(SupportTicket ticket);

    void update(SupportTicket ticket);

    void updateStatus(int ticketId, String status);

    int countOpen();
}
