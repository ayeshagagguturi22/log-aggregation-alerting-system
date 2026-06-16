package com.logagg.processor.consumer;

import com.logagg.processor.config.LogEventRepository;
import com.logagg.processor.model.LogEvent;
import com.logagg.processor.model.LogEventEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer that listens to the "log-events" topic.
 *
 * Flow:
 *   Kafka topic "log-events" → LogConsumer → PostgreSQL (log_events table)
 *
 * Every log event received from Kafka is persisted to the database.
 * The AlertService then queries this table every minute to check error rates.
 */
@Component
public class LogConsumer {

    private static final Logger log = LoggerFactory.getLogger(LogConsumer.class);

    private final LogEventRepository logEventRepository;

    public LogConsumer(LogEventRepository logEventRepository) {
        this.logEventRepository = logEventRepository;
    }

    /**
     * Listens to "log-events" topic.
     * Consumer group "log-processor-group" ensures each message
     * is processed by only one instance of this service.
     */
    @KafkaListener(topics = "log-events", groupId = "log-processor-group")
    public void consume(LogEvent logEvent) {
        log.info("Received log from Kafka → service: {} | level: {} | message: {}",
            logEvent.getService(), logEvent.getLevel(), logEvent.getMessage());

        // Map the Kafka message to a JPA entity and save to PostgreSQL
        LogEventEntity entity = new LogEventEntity();
        entity.setService(logEvent.getService());
        entity.setLevel(logEvent.getLevel());
        entity.setMessage(logEvent.getMessage());

        logEventRepository.save(entity);

        log.info("Saved log to PostgreSQL for service: {}", logEvent.getService());
    }
}
