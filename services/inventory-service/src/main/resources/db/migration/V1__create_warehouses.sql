-- Places that hold stock.
CREATE TABLE warehouses (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    -- A code people type and read, e.g. WH-01
    code       VARCHAR(50)  NOT NULL,
    name       VARCHAR(100) NOT NULL,
    address    VARCHAR(255) NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    CONSTRAINT pk_warehouses PRIMARY KEY (id),
    CONSTRAINT uk_warehouses_code UNIQUE (code)
);
