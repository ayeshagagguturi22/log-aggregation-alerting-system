package com.logagg.producer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single log event emitted by any service.
 * This is the message that gets published to Kafka.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LogEvent {

    // Name of the service that generated this log (e.g. "payment-service")
    private String service;

    // Log level: INFO, WARN, ERROR
    private String level;

    // The actual log message
    private String message;

    // When the log was generated (epoch milliseconds)
    private long timestamp;
}
