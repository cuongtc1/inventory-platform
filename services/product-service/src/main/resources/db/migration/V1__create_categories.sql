-- Product categories. A category can have one parent (e.g. Peripherals -> Electronics); top-level ones have none.
CREATE TABLE categories (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    name       VARCHAR(100) NOT NULL,
    parent_id  BIGINT       NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    CONSTRAINT pk_categories PRIMARY KEY (id),
    CONSTRAINT uk_categories_name UNIQUE (name),
    -- Declared here so the foreign key below uses it (otherwise MySQL adds its own index)
    INDEX idx_categories_parent_id (parent_id),
    -- Same-schema foreign key; RESTRICT (the default) blocks deleting a parent that still has children
    CONSTRAINT fk_categories_categories FOREIGN KEY (parent_id) REFERENCES categories (id)
);
