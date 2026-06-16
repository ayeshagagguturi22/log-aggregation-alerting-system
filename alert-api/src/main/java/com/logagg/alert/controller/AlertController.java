package com.logagg.alert.controller;

import com.logagg.alert.model.Alert;
import com.logagg.alert.model.LogEvent;
import com.logagg.alert.repository.AlertRepository;
import com.logagg.alert.repository.LogEventRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API to query alerts and logs stored in PostgreSQL.
 *
 * Endpoints:
 *   GET /api/alerts                        → all alerts
 *   GET /api/alerts/{service}              → alerts for a specific service
 *   GET /api/logs                          → all log events
 *   GET /api/logs/service/{service}        → logs for a specific service
 *   GET /api/logs/level/{level}            → logs filtered by level (ERROR, WARN, INFO)
 */
@RestController
@RequestMapping("/api")
public class AlertController {

    private final AlertRepository alertRepository;
    private final LogEventRepository logEventRepository;

    public AlertController(AlertRepository alertRepository,
                           LogEventRepository logEventRepository) {
        this.alertRepository = alertRepository;
        this.logEventRepository = logEventRepository;
    }

    // ── Alert endpoints ──────────────────────────────────────────────────────

    @GetMapping("/alerts")
    public List<Alert> getAllAlerts() {
        return alertRepository.findAll();
    }

    @GetMapping("/alerts/{service}")
    public List<Alert> getAlertsByService(@PathVariable String service) {
        return alertRepository.findByServiceOrderByCreatedAtDesc(service);
    }

    // ── Log endpoints ─────────────────────────────────────────────────────────

    @GetMapping("/logs")
    public List<LogEvent> getAllLogs() {
        return logEventRepository.findAll();
    }

    @GetMapping("/logs/service/{service}")
    public List<LogEvent> getLogsByService(@PathVariable String service) {
        return logEventRepository.findByServiceOrderByCreatedAtDesc(service);
    }

    @GetMapping("/logs/level/{level}")
    public List<LogEvent> getLogsByLevel(@PathVariable String level) {
        return logEventRepository.findByLevelOrderByCreatedAtDesc(level.toUpperCase());
    }
}
