-- The lines of an order. Name and price are copied at the moment of the order, so renaming a product or changing its
-- price never rewrites an order already placed. The line total is quantity * unit_price: calculated, never stored.
CREATE TABLE sales_order_items (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    sales_order_id BIGINT        NOT NULL,
    -- Lives in product-service's schema: a plain column, no foreign key across schemas
    product_id     BIGINT        NOT NULL,
    product_sku    VARCHAR(50)   NOT NULL,
    product_name   VARCHAR(200)  NOT NULL,
    quantity       INT           NOT NULL,
    -- USD, the product's sale price at that moment
    unit_price     DECIMAL(12,2) NOT NULL,
    created_at     DATETIME(6)   NOT NULL,
    updated_at     DATETIME(6)   NOT NULL,
    created_by     VARCHAR(100)  NOT NULL,
    updated_by     VARCHAR(100)  NOT NULL,
    CONSTRAINT pk_sales_order_items PRIMARY KEY (id),
    -- One line per product, which is also what a stock reservation expects
    CONSTRAINT uk_sales_order_items_order_product UNIQUE (sales_order_id, product_id),
    INDEX idx_sales_order_items_product_id (product_id),
    CONSTRAINT fk_sales_order_items_sales_orders FOREIGN KEY (sales_order_id) REFERENCES sales_orders (id),
    CONSTRAINT ck_sales_order_items_quantity CHECK (quantity > 0),
    CONSTRAINT ck_sales_order_items_unit_price CHECK (unit_price >= 0)
);
