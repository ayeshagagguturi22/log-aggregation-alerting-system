-- Table to store every log event received
CREATE TABLE IF NOT EXISTS log_events (
    id               BIGSERIAL PRIMARY KEY,
    service          VARCHAR(100) NOT NULL,
    level            VARCHAR(20)  NOT NULL,
    message          TEXT         NOT NULL,

    -- When the error ACTUALLY happened on the source service (from logEvent.timestamp)
    -- Used for alert window calculation — correct even if log-processor is delayed
    event_timestamp  TIMESTAMP    NOT NULL,

    -- When we saved this row to the DB (set by @PrePersist)
    -- Useful for measuring pipeline delay: created_at - event_timestamp = lag
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Table to store alerts
-- created_at = alert_generated_at — when AlertService raised this alert
-- window_start/window_end = the event time window that triggered it
CREATE TABLE IF NOT EXISTS alerts (
    id           BIGSERIAL PRIMARY KEY,
    service      VARCHAR(100) NOT NULL,
    error_count  INT          NOT NULL,
    window_start TIMESTAMP    NOT NULL,
    window_end   TIMESTAMP    NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Index on event_timestamp for fast window queries in AlertService
-- Without this, every COUNT query does a full table scan
CREATE INDEX IF NOT EXISTS idx_log_events_event_timestamp
    ON log_events(service, level, event_timestamp);

-- Index on created_at to monitor pipeline delay
CREATE INDEX IF NOT EXISTS idx_log_events_created_at
    ON log_events(created_at);
