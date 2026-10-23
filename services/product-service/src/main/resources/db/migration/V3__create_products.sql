-- Products in the catalog. Stock quantities live in inventory-service, not here.
CREATE TABLE products (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    sku           VARCHAR(50)   NOT NULL,
    name          VARCHAR(200)  NOT NULL,
    category_id   BIGINT        NOT NULL,
    supplier_id   BIGINT        NULL,
    cost_price    DECIMAL(12,2) NOT NULL,
    sale_price    DECIMAL(12,2) NOT NULL,
    -- inventory-service raises a low-stock alert when available stock drops below this
    reorder_level INT           NOT NULL DEFAULT 0,
    status        VARCHAR(20)   NOT NULL,
    created_at    DATETIME(6)   NOT NULL,
    updated_at    DATETIME(6)   NOT NULL,
    CONSTRAINT pk_products PRIMARY KEY (id),
    CONSTRAINT uk_products_sku UNIQUE (sku),
    INDEX idx_products_category_id (category_id),
    INDEX idx_products_supplier_id (supplier_id),
    INDEX idx_products_status (status),
    CONSTRAINT fk_products_categories FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT fk_products_suppliers FOREIGN KEY (supplier_id) REFERENCES suppliers (id)
);
