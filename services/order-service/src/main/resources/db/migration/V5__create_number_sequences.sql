-- Counters for the numbers people read on documents: SO-1001, SO-1002, ... (and purchase orders in Phase 7).
-- MySQL has no sequences, and MAX(order_number) + 1 would give two orders the same number when they arrive at once.
-- Taking a number locks its row (SELECT ... FOR UPDATE) until the order is saved, so numbers are unique and, because a
-- rolled-back order also rolls back its number, without gaps.
CREATE TABLE number_sequences (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    -- Which counter: sales_order
    name       VARCHAR(50)  NOT NULL,
    -- The number the next document gets
    next_value BIGINT       NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    CONSTRAINT pk_number_sequences PRIMARY KEY (id),
    CONSTRAINT uk_number_sequences_name UNIQUE (name),
    CONSTRAINT ck_number_sequences_next_value CHECK (next_value > 0)
);

-- Sales orders start at SO-1001, like the mockup's numbers. UTC_TIMESTAMP: audit columns are UTC
INSERT INTO number_sequences (name, next_value, created_at, updated_at, created_by, updated_by)
VALUES ('sales_order', 1001, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 'system', 'system');
