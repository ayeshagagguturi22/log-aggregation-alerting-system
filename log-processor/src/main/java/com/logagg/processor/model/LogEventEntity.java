package com.logagg.processor.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "log_events")
@AllArgsConstructor
@NoArgsConstructor
public class LogEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String service;
    private String level;

    @Column(columnDefinition = "TEXT")
    private String message;

    /**
     * WHEN THE ERROR ACTUALLY HAPPENED on the source service.
     * Comes from logEvent.getTimestamp() — the timestamp the service set
     * when it generated the log.
     *
     * Used for alert window calculation — so alerts are based on
     * real error time, not processing time.
     *
     * Example: if payment-service had errors at 10:00:00 but log-processor
     * was delayed and saved them at 10:05:00, this field still shows 10:00:00.
     */
    private LocalDateTime eventTimestamp;

    /**
     * WHEN WE SAVED THIS ROW TO THE DATABASE.
     * Set automatically by @PrePersist.
     *
     * Useful for monitoring pipeline delay:
     *   delay = createdAt - eventTimestamp
     * If this gap is large, log-processor is falling behind.
     */
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
