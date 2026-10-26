package com.isp.dao;

import com.isp.model.Plan;

import java.util.List;
import java.util.Optional;

/**
 * DAO interface for {@link Plan}.
 */
public interface PlanDAO {

    List<Plan> findAll();

    List<Plan> findActive();

    Optional<Plan> findById(int planId);

    int insert(Plan plan);

    void update(Plan plan);

    void setActive(int planId, boolean active);
}
