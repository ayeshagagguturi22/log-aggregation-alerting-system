-- Table to store every log event received
CREATE TABLE IF NOT EXISTS log_events (
    id          BIGSERIAL PRIMARY KEY,
    service     VARCHAR(100) NOT NULL,
    level       VARCHAR(20)  NOT NULL,   -- INFO, WARN, ERROR
    message     TEXT         NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Table to store alerts when error rate crosses threshold
CREATE TABLE IF NOT EXISTS alerts (
    id           BIGSERIAL PRIMARY KEY,
    service      VARCHAR(100) NOT NULL,
    error_count  INT          NOT NULL,
    window_start TIMESTAMP    NOT NULL,
    window_end   TIMESTAMP    NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);
