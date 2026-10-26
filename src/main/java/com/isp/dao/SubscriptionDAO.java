package com.isp.dao;

import com.isp.model.Subscription;

import java.util.List;
import java.util.Optional;

/**
 * DAO interface for {@link Subscription}.
 */
public interface SubscriptionDAO {

    List<Subscription> findAll();

    List<Subscription> findByCustomerId(int customerId);

    Optional<Subscription> findById(int subscriptionId);

    int insert(Subscription subscription);

    void update(Subscription subscription);

    void updateStatus(int subscriptionId, String status);

    /** Total active subscriptions (dashboard KPI). */
    int countActive();
}
