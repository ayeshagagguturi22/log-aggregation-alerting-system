package com.logagg.alert.repository;

import com.logagg.alert.model.LogEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LogEventRepository extends JpaRepository<LogEvent, Long> {

    // Get recent logs for a specific service
    List<LogEvent> findByServiceOrderByCreatedAtDesc(String service);

    // Get all logs of a specific level (e.g. ERROR)
    List<LogEvent> findByLevelOrderByCreatedAtDesc(String level);
}
