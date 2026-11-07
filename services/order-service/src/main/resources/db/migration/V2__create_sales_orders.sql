-- One sales order: who buys, from which warehouse, and where it stands. Created by and when come from the audit
-- columns. An order is never deleted: it ends as COMPLETED, REJECTED or CANCELLED.
CREATE TABLE sales_orders (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    -- What people read and search for: SO-1042
    order_number   VARCHAR(20)   NOT NULL,
    customer_id    BIGINT        NOT NULL,
    -- Lives in inventory-service's schema: a plain column, no foreign key across schemas. The saga reserves stock here
    warehouse_id   BIGINT        NOT NULL,
    -- A copy of what lists show. A query here can't join inventory_db
    warehouse_code VARCHAR(50)   NOT NULL,
    -- PENDING, RESERVED, PAID, SHIPPED, COMPLETED, REJECTED or CANCELLED
    status         VARCHAR(20)   NOT NULL,
    -- Sum of the lines, in USD. Stored, unlike line totals, because the list shows and sorts by it. The lines never
    -- change after the order is created, so the two can't disagree
    total_amount   DECIMAL(12,2) NOT NULL,
    -- Optimistic locking (@Version): a user and a Kafka event changing the status at once can't overwrite each other
    version        BIGINT        NOT NULL DEFAULT 0,
    created_at     DATETIME(6)   NOT NULL,
    updated_at     DATETIME(6)   NOT NULL,
    created_by     VARCHAR(100)  NOT NULL,
    updated_by     VARCHAR(100)  NOT NULL,
    CONSTRAINT pk_sales_orders PRIMARY KEY (id),
    CONSTRAINT uk_sales_orders_order_number UNIQUE (order_number),
    INDEX idx_sales_orders_warehouse_id (warehouse_id),
    INDEX idx_sales_orders_status (status),
    -- The default sort of the list, and the date filters of reports
    INDEX idx_sales_orders_created_at (created_at),
    -- A customer with orders can't be deleted
    CONSTRAINT fk_sales_orders_customers FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT ck_sales_orders_total_amount CHECK (total_amount >= 0)
);
