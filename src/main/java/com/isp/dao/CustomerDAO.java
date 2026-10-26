package com.isp.dao;

import com.isp.model.Customer;

import java.util.List;
import java.util.Optional;

/**
 * DAO interface for {@link Customer}.
 * Implementations use PreparedStatement / JDBC.
 */
public interface CustomerDAO {

    /** Returns all customers, optionally filtered by status. */
    List<Customer> findAll();

    /** Returns customers whose name or email contains the query string. */
    List<Customer> search(String query);

    Optional<Customer> findById(int customerId);

    /** Inserts a new customer and returns the generated customerId. */
    int insert(Customer customer);

    void update(Customer customer);

    void deleteById(int customerId);

    /** Total number of active customers (for dashboard KPIs). */
    int countActive();
}
