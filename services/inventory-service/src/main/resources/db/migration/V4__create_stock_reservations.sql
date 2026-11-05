-- Stock held for an order line in the saga (Phase 6). Reserving raises stock_levels.reserved; shipping confirms the
-- reservation and lowers on_hand; cancelling releases it. Knowing what was reserved for which order makes that
-- possible.
CREATE TABLE stock_reservations (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    -- Lives in order-service's schema: a plain column, no foreign key across schemas
    order_id     BIGINT       NOT NULL,
    warehouse_id BIGINT       NOT NULL,
    product_id   BIGINT       NOT NULL,
    quantity     INT          NOT NULL,
    -- RESERVED, CONFIRMED or RELEASED
    status       VARCHAR(20)  NOT NULL,
    created_at   DATETIME(6)  NOT NULL,
    updated_at   DATETIME(6)  NOT NULL,
    created_by   VARCHAR(100) NOT NULL,
    updated_by   VARCHAR(100) NOT NULL,
    CONSTRAINT pk_stock_reservations PRIMARY KEY (id),
    -- One reservation per order line
    CONSTRAINT uk_stock_reservations_order_warehouse_product UNIQUE (order_id, warehouse_id, product_id),
    INDEX idx_stock_reservations_warehouse_product (warehouse_id, product_id),
    CONSTRAINT fk_stock_reservations_warehouses FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT ck_stock_reservations_quantity CHECK (quantity > 0)
);
