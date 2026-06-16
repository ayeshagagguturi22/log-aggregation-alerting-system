package com.logagg.alert.repository;

import com.logagg.alert.model.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    // Get all alerts for a specific service
    List<Alert> findByServiceOrderByCreatedAtDesc(String service);
}
