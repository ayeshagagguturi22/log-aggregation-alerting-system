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
     * Count ERROR logs for a service within a time window.
     *
     * IMPORTANT: uses eventTimestamp (when the error actually happened)
     * NOT createdAt (when we saved it to DB).
     *
     * Why this matters:
     * Old query used createdAt — if log-processor was delayed by 5 minutes,
     * errors from 10:00 would be saved with createdAt=10:05 and missed by
     * the alert window checking 10:00-10:01.
     *
     * New query uses eventTimestamp — no matter how delayed the processor is,
     * the alert window correctly captures when errors actually occurred.
     */
    @Query("SELECT COUNT(l) FROM LogEventEntity l " +
           "WHERE l.service = :service " +
           "AND l.level = 'ERROR' " +
           "AND l.eventTimestamp BETWEEN :start AND :end")
    int countErrorsInWindow(
        @Param("service") String service,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );

    /**
     * Get all unique service names seen so far.
     */
    @Query("SELECT DISTINCT l.service FROM LogEventEntity l")
    List<String> findDistinctServices();
}
