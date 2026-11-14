-- The transactional outbox (T6.5): events this service publishes, stored in the same transaction as the change they
-- announce, then sent to Kafka by stockline-common's OutboxPublisher. A row is sent while published_at is empty.
CREATE TABLE outbox_events (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    -- The envelope's eventId: the same on every resend, so consumers can skip a copy they already applied
    event_id     VARCHAR(36)  NOT NULL,
    topic        VARCHAR(100) NOT NULL,
    -- The Kafka key: the id of the main entity (for order events, the order id)
    message_key  VARCHAR(100) NOT NULL,
    -- The whole envelope as JSON text, sent exactly as stored
    message      MEDIUMTEXT   NOT NULL,
    -- When Kafka acknowledged it; empty until then
    published_at DATETIME(6)  NULL,
    created_at   DATETIME(6)  NOT NULL,
    updated_at   DATETIME(6)  NOT NULL,
    created_by   VARCHAR(100) NOT NULL,
    updated_by   VARCHAR(100) NOT NULL,
    CONSTRAINT pk_outbox_events PRIMARY KEY (id),
    CONSTRAINT uk_outbox_events_event_id UNIQUE (event_id),
    -- The publisher's query: unsent rows, oldest first
    INDEX idx_outbox_events_published_at (published_at, id)
);
