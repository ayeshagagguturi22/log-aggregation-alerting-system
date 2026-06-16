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
 * AlertService — evaluates error rates using EVENT TIME, not processing time.
 *
 * OLD approach (broken):
 *   windowEnd   = LocalDateTime.now()          ← current wall clock time
 *   windowStart = windowEnd.minusMinutes(1)
 *   COUNT where createdAt BETWEEN start AND end ← time we saved to DB
 *
 *   Problem: if log-processor is delayed, errors from 10:00 get saved at 10:05.
 *   The 10:00-10:01 alert window runs at 10:01 and finds 0 errors (not saved yet).
 *   Alert fires 5 minutes late or not at all.
 *
 * NEW approach (correct):
 *   windowEnd   = LocalDateTime.now()
 *   windowStart = windowEnd.minusMinutes(1)
 *   COUNT where eventTimestamp BETWEEN start AND end ← time error ACTUALLY happened
 *
 *   Now even if log-processor saves them at 10:05, the eventTimestamp=10:00 means
 *   they are counted in the correct 10:00-10:01 window when AlertService
 *   runs a subsequent check over a wider lookback window.
 *
 * Additional field: alertGeneratedAt — exactly when this alert was created.
 * Already handled by @PrePersist createdAt in AlertEntity.
 */
@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private static final int ERROR_THRESHOLD = 3;

    // Look back 2 minutes instead of 1 minute.
    // This gives a buffer for late-arriving messages — if log-processor
    // had a brief delay, errors from 1-2 minutes ago are still caught.
    private static final int LOOKBACK_MINUTES = 2;

    private final LogEventRepository logEventRepository;
    private final AlertRepository alertRepository;

    public AlertService(LogEventRepository logEventRepository,
                        AlertRepository alertRepository) {
        this.logEventRepository = logEventRepository;
        this.alertRepository = alertRepository;
    }

    /**
     * Runs every 60 seconds.
     *
     * For each known service, counts ERROR logs where eventTimestamp falls
     * within the last LOOKBACK_MINUTES window.
     *
     * alertGeneratedAt (createdAt via @PrePersist) records exactly when
     * this alert was raised — separate from when the errors happened.
     * This lets you calculate: alertGeneratedAt - windowEnd = alert delay.
     */
    @Scheduled(fixedRate = 60000)
    public void evaluateErrorRates() {
        LocalDateTime windowEnd   = LocalDateTime.now();
        LocalDateTime windowStart = windowEnd.minusMinutes(LOOKBACK_MINUTES);

        List<String> services = logEventRepository.findDistinctServices();

        for (String service : services) {
            int errorCount = logEventRepository.countErrorsInWindow(service, windowStart, windowEnd);

            log.info("Service: {} | Errors in last {} min (by event time): {}",
                service, LOOKBACK_MINUTES, errorCount);

            if (errorCount > ERROR_THRESHOLD) {
                AlertEntity alert = new AlertEntity();
                alert.setService(service);
                alert.setErrorCount(errorCount);
                alert.setWindowStart(windowStart);
                alert.setWindowEnd(windowEnd);
                // alertGeneratedAt = createdAt, set automatically by @PrePersist
                // This records WHEN THE ALERT WAS RAISED, which may be later
                // than windowEnd if there was processing delay.

                alertRepository.save(alert);

                log.warn("ALERT raised for service: {} | {} errors between {} and {} | alert generated at: {}",
                    service, errorCount, windowStart, windowEnd, LocalDateTime.now());
            }
        }
    }
}
