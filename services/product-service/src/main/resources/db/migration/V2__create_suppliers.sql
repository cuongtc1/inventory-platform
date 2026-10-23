-- Companies we buy products from. Contact details are optional.
CREATE TABLE suppliers (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    name         VARCHAR(150) NOT NULL,
    contact_name VARCHAR(100) NULL,
    email        VARCHAR(150) NULL,
    phone        VARCHAR(30)  NULL,
    created_at   DATETIME(6)  NOT NULL,
    updated_at   DATETIME(6)  NOT NULL,
    CONSTRAINT pk_suppliers PRIMARY KEY (id),
    CONSTRAINT uk_suppliers_name UNIQUE (name)
);
