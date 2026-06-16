package com.logagg.processor.config;

import com.logagg.processor.model.LogEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LogEventRepository extends JpaRepository<LogEventEntity, Long> {

    /**
     * Count how many ERROR logs a specific service produced in a given time window.
     * This is used by the AlertService to decide whether to raise an alert.
     */
    @Query("SELECT COUNT(l) FROM LogEventEntity l " +
           "WHERE l.service = :service " +
           "AND l.level = 'ERROR' " +
           "AND l.createdAt BETWEEN :start AND :end")
    int countErrorsInWindow(
        @Param("service") String service,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );

    /**
     * Get all unique service names that have logged anything.
     * Used to iterate over services during scheduled window evaluation.
     */
    @Query("SELECT DISTINCT l.service FROM LogEventEntity l")
    List<String> findDistinctServices();
}
