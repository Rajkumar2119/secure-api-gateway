-- Normalised catalogue: each product belongs to one category.
CREATE TABLE categories (
    id   BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100) NOT NULL,
    CONSTRAINT uk_categories_name UNIQUE (name),
    CONSTRAINT uk_categories_slug UNIQUE (slug)
) ENGINE = InnoDB;

CREATE TABLE products (
    id             BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    sku            VARCHAR(40)   NOT NULL,
    name           VARCHAR(200)  NOT NULL,
    description    TEXT          NULL,
    price          DECIMAL(12,2) NOT NULL,
    stock_quantity INT           NOT NULL DEFAULT 0,
    category_id    BIGINT        NOT NULL,
    active         BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at     TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    version        BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT uk_products_sku UNIQUE (sku),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT ck_products_price CHECK (price >= 0),
    CONSTRAINT ck_products_stock CHECK (stock_quantity >= 0),
    -- Serves the main listing query: WHERE active = true AND category_id = ? ORDER BY name
    INDEX idx_products_active_category_name (active, category_id, name),
    -- Serves name-prefix search: WHERE active = true AND name LIKE 'abc%'
    INDEX idx_products_active_name (active, name)
) ENGINE = InnoDB;
