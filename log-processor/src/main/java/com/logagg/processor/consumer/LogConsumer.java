package com.logagg.processor.consumer;

import com.logagg.processor.config.LogEventRepository;
import com.logagg.processor.model.LogEvent;
import com.logagg.processor.model.LogEventEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Kafka batch consumer.
 *
 * OLD approach (one message at a time):
 *   - 1 Kafka message → 1 DB INSERT → commit offset
 *   - 10,000 logs/sec = 10,000 individual INSERTs hitting PostgreSQL
 *   - Auto-commit: offset committed even if DB save failed → message lost
 *
 * NEW approach (batch):
 *   - Up to 100 Kafka messages → 1 batch INSERT (saveAll) → then commit offset
 *   - Offset committed ONLY after all 100 are saved successfully
 *   - If DB save fails → offset NOT committed → Kafka resends the batch → no data loss
 */
@Component
public class LogConsumer {

    private static final Logger log = LoggerFactory.getLogger(LogConsumer.class);

    private final LogEventRepository logEventRepository;

    public LogConsumer(LogEventRepository logEventRepository) {
        this.logEventRepository = logEventRepository;
    }

    /**
     * Receives a batch of up to 100 log events from Kafka.
     *
     * Steps:
     * 1. Map all LogEvent objects to LogEventEntity objects
     * 2. Save all entities in one batch INSERT (saveAll)
     * 3. Only after successful save — acknowledge to Kafka (commit offset)
     *
     * If step 2 throws an exception:
     * - acknowledgment.acknowledge() is never called
     * - Kafka offset stays at previous position
     * - On restart, Kafka resends the same batch
     * - No messages are lost
     *
     * @param logEvents  list of log events from Kafka (up to MAX_POLL_RECORDS = 100)
     * @param acknowledgment  used to manually tell Kafka "I have processed this batch"
     */
    @KafkaListener(topics = "log-events", groupId = "log-processor-group")
    public void consume(List<LogEvent> logEvents, Acknowledgment acknowledgment) {

        log.info("Received batch of {} log events from Kafka", logEvents.size());

        // Step 1: Map each Kafka LogEvent → LogEventEntity (JPA entity for DB)
        List<LogEventEntity> entities = logEvents.stream()
            .map(this::mapToEntity)
            .collect(Collectors.toList());

        // Step 2: One batch INSERT for the entire list
        // Without Hibernate batch_size config in application.yml, saveAll() would
        // still do individual INSERTs. The batch_size setting makes it truly batched.
        logEventRepository.saveAll(entities);

        // Step 3: Only NOW commit the offset to Kafka
        // This tells Kafka: "I have successfully processed all messages up to this point"
        // If saveAll() above threw an exception, we never reach this line,
        // so the offset stays uncommitted and Kafka will resend on restart.
        acknowledgment.acknowledge();

        log.info("Batch of {} logs saved to PostgreSQL and offset committed", entities.size());
    }

    /**
     * Maps a Kafka LogEvent (plain Java object) to a LogEventEntity (JPA/DB entity).
     *
     * Key point: eventTimestamp is set from logEvent.getTimestamp() — the time
     * the error ACTUALLY happened on the service, not the time we saved it.
     *
     * This is critical for correct alert window calculation.
     * Example: if log-processor was delayed by 5 minutes, the error still happened
     * at the original time — we preserve that here.
     */
    private LogEventEntity mapToEntity(LogEvent logEvent) {
        LogEventEntity entity = new LogEventEntity();
        entity.setService(logEvent.getService());
        entity.setLevel(logEvent.getLevel());
        entity.setMessage(logEvent.getMessage());

        // Convert epoch milliseconds (from LogEvent) to LocalDateTime
        // This is WHEN THE ERROR ACTUALLY HAPPENED — not when we processed it
        LocalDateTime eventTime = Instant.ofEpochMilli(logEvent.getTimestamp())
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime();
        entity.setEventTimestamp(eventTime);

        // createdAt is set automatically by @PrePersist in the entity
        // It records WHEN WE SAVED IT — useful for debugging pipeline delays
        return entity;
    }
}
