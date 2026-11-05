-- How much of one product one warehouse holds: at most one row per product and warehouse.
-- available = on_hand - reserved is calculated, never stored, so the two can't disagree.
CREATE TABLE stock_levels (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    warehouse_id  BIGINT       NOT NULL,
    -- Lives in product-service's schema: a plain column, no foreign key across schemas
    product_id    BIGINT       NOT NULL,
    -- Copies of what lists show and filter on. A query here can't join product_db, and the low-stock filter
    -- compares with reorder_level in SQL
    product_sku   VARCHAR(50)  NOT NULL,
    product_name  VARCHAR(200) NOT NULL,
    reorder_level INT          NOT NULL DEFAULT 0,
    -- Physically in the warehouse
    on_hand       INT          NOT NULL DEFAULT 0,
    -- Promised to orders that haven't shipped yet
    reserved      INT          NOT NULL DEFAULT 0,
    -- Optimistic locking (@Version): an UPDATE only succeeds if nobody changed the row since it was read, so two
    -- orders competing for the last items can't both win (T6.3)
    version       BIGINT       NOT NULL DEFAULT 0,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    created_by    VARCHAR(100) NOT NULL,
    updated_by    VARCHAR(100) NOT NULL,
    CONSTRAINT pk_stock_levels PRIMARY KEY (id),
    CONSTRAINT uk_stock_levels_warehouse_product UNIQUE (warehouse_id, product_id),
    INDEX idx_stock_levels_product_id (product_id),
    CONSTRAINT fk_stock_levels_warehouses FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    -- The last line of defence against overselling, whatever the code does
    CONSTRAINT ck_stock_levels_on_hand CHECK (on_hand >= 0),
    CONSTRAINT ck_stock_levels_reserved CHECK (reserved >= 0 AND reserved <= on_hand)
);
