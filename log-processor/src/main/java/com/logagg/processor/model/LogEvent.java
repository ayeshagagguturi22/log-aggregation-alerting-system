package com.logagg.processor.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mirrors the LogEvent published by log-producer.
 * Used to deserialize the Kafka message.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LogEvent {
    private String service;
    private String level;
    private String message;
    private long timestamp;
}
