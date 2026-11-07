-- Every status change of an order: the timeline of the detail page. One row per change, never updated or deleted.
-- created_at and created_by say when and who ("system" for a change made by a Kafka event).
CREATE TABLE order_status_history (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    sales_order_id BIGINT       NOT NULL,
    -- NULL on the first row: the order had no status before it was created
    from_status    VARCHAR(20)  NULL,
    to_status      VARCHAR(20)  NOT NULL,
    -- Why, when there is more to say: a rejected order names the product that was short
    note           VARCHAR(255) NULL,
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    created_by     VARCHAR(100) NOT NULL,
    updated_by     VARCHAR(100) NOT NULL,
    CONSTRAINT pk_order_status_history PRIMARY KEY (id),
    -- The timeline of one order, oldest first
    INDEX idx_order_status_history_order_created (sales_order_id, created_at),
    CONSTRAINT fk_order_status_history_sales_orders FOREIGN KEY (sales_order_id) REFERENCES sales_orders (id)
);
