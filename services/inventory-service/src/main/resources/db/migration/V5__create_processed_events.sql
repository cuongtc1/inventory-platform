-- Kafka events already handled. Kafka can deliver a message twice; a consumer stores eventId here in the same
-- transaction as its work and skips any event it finds, which makes it idempotent (T6.4). The unique key also stops
-- two consumers handling the same event at the same moment.
CREATE TABLE processed_events (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    -- The envelope's eventId, a UUID (docs/conventions.md section 6)
    event_id   VARCHAR(36)  NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    CONSTRAINT pk_processed_events PRIMARY KEY (id),
    CONSTRAINT uk_processed_events_event_id UNIQUE (event_id)
);
