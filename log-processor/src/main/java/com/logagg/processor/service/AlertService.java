package com.logagg.processor.service;

import com.logagg.processor.config.AlertRepository;
import com.logagg.processor.config.LogEventRepository;
import com.logagg.processor.model.AlertEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AlertService runs every 60 seconds.
 *
 * For each service that has produced logs, it checks:
 *   "How many ERROR logs did this service produce in the last 1 minute?"
 *
 * If the count exceeds ERROR_THRESHOLD (default: 3), an alert is saved
 * to the alerts table in PostgreSQL.
 *
 * This simulates a simple tumbling window alerting mechanism.
 */
@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    // If a service produces more than this many errors in 1 minute, raise an alert
    private static final int ERROR_THRESHOLD = 3;

    private final LogEventRepository logEventRepository;
    private final AlertRepository alertRepository;

    public AlertService(LogEventRepository logEventRepository,
                        AlertRepository alertRepository) {
        this.logEventRepository = logEventRepository;
        this.alertRepository = alertRepository;
    }

    /**
     * Runs every 60 seconds.
     * Checks each known service for error rate in the past 1-minute window.
     */
    @Scheduled(fixedRate = 60000)
    public void evaluateErrorRates() {
        LocalDateTime windowEnd   = LocalDateTime.now();
        LocalDateTime windowStart = windowEnd.minusMinutes(1);

        // Get all services we have seen so far
        List<String> services = logEventRepository.findDistinctServices();

        for (String service : services) {
            int errorCount = logEventRepository.countErrorsInWindow(service, windowStart, windowEnd);

            log.info("Service: {} | Errors in last 1 min: {}", service, errorCount);

            if (errorCount > ERROR_THRESHOLD) {
                // Error rate crossed threshold — create an alert
                AlertEntity alert = new AlertEntity();
                alert.setService(service);
                alert.setErrorCount(errorCount);
                alert.setWindowStart(windowStart);
                alert.setWindowEnd(windowEnd);

                alertRepository.save(alert);

                log.warn("ALERT raised for service: {} | {} errors in last 1 minute", service, errorCount);
            }
        }
    }
}
