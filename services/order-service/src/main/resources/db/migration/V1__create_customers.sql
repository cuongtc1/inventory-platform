-- Companies we sell to. Contact details are optional, like a supplier's. The name is unique because people choose a
-- customer by name on the order form.
CREATE TABLE customers (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    name       VARCHAR(150) NOT NULL,
    email      VARCHAR(150) NULL,
    phone      VARCHAR(30)  NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    CONSTRAINT pk_customers PRIMARY KEY (id),
    CONSTRAINT uk_customers_name UNIQUE (name)
);
