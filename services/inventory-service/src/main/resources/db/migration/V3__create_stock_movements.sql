-- The history of stock: one row per change, never updated or deleted. created_at and created_by say when and who.
CREATE TABLE stock_movements (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    warehouse_id  BIGINT       NOT NULL,
    product_id    BIGINT       NOT NULL,
    -- Copies, so history stays readable after the product is renamed or deleted
    product_sku   VARCHAR(50)  NOT NULL,
    product_name  VARCHAR(200) NOT NULL,
    -- IN, OUT, ADJUST, RESERVE or RELEASE
    movement_type VARCHAR(20)  NOT NULL,
    -- Signed: a correction can be negative (ADJUST -2)
    quantity      INT          NOT NULL,
    -- What caused it, e.g. SO-1042, PO-310 or "Count"
    reference     VARCHAR(100) NULL,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    created_by    VARCHAR(100) NOT NULL,
    updated_by    VARCHAR(100) NOT NULL,
    CONSTRAINT pk_stock_movements PRIMARY KEY (id),
    -- Lists show the newest first and filter by product, warehouse and date
    INDEX idx_stock_movements_product_created (product_id, created_at),
    INDEX idx_stock_movements_warehouse_created (warehouse_id, created_at),
    INDEX idx_stock_movements_created_at (created_at),
    CONSTRAINT fk_stock_movements_warehouses FOREIGN KEY (warehouse_id) REFERENCES warehouses (id)
);
