package com.logagg.producer.controller;

import com.logagg.producer.model.LogEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller to accept log events and publish them to Kafka.
 *
 * Flow:
 *   Client → POST /api/logs → Kafka topic "log-events"
 *
 * The service name is used as the Kafka message key so that all logs
 * from the same service land on the same partition (ordered processing).
 */
@RestController
@RequestMapping("/api/logs")
public class LogProducerController {

    private static final String TOPIC = "log-events";

    private final KafkaTemplate<String, LogEvent> kafkaTemplate;

    public LogProducerController(KafkaTemplate<String, LogEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Accepts a log event and publishes it to Kafka.
     *
     * Example request body:
     * {
     *   "service": "payment-service",
     *   "level": "ERROR",
     *   "message": "Payment gateway timeout",
     *   "timestamp": 1718000000000
     * }
     */
    @PostMapping
    public String publishLog(@RequestBody LogEvent logEvent) {
        // Use service name as key — ensures ordered delivery per service
        kafkaTemplate.send(TOPIC, logEvent.getService(), logEvent);
        return "Log published to Kafka: [" + logEvent.getLevel() + "] " + logEvent.getService();
    }

    /**
     * Convenience endpoint to simulate bulk logs for testing.
     * Sends 5 ERROR logs for the given service.
     */
    @PostMapping("/simulate/{service}")
    public String simulateErrors(@PathVariable String service) {
        for (int i = 1; i <= 5; i++) {
            LogEvent event = new LogEvent(
                service,
                "ERROR",
                "Simulated error #" + i + " from " + service,
                System.currentTimeMillis()
            );
            kafkaTemplate.send(TOPIC, service, event);
        }
        return "Simulated 5 ERROR logs for service: " + service;
    }
}
