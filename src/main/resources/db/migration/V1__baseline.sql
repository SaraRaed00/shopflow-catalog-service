CREATE TABLE category
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    slug       VARCHAR(100) NOT NULL,
    parent_id  BIGINT,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT unique_category_per_slug UNIQUE (slug),
    CONSTRAINT FK_category_parentID FOREIGN KEY (parent_id) REFERENCES category (id)
);
CREATE TABLE product
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT,
    sku         VARCHAR(100) NOT NULL,
    name           VARCHAR(100) NOT NULL,
    description    TEXT,
    price_amount   DECIMAL(12,3) NOT NULL,
    price_currency CHAR(3) NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    version        BIGINT NOT NULL DEFAULT 0,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT UNIQUE_product_sku UNIQUE (sku),
    CONSTRAINT FK_product_category FOREIGN KEY (category_id) REFERENCES category (id)
);
CREATE TABLE warehouse (
    id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    code    VARCHAR(15) NOT NULL,
    name    VARCHAR(100) NOT NULL,
    country CHAR(2) NOT NULL,
    CONSTRAINT UNIQUE_warehouse_code UNIQUE (code)
);

CREATE TABLE stock_item (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id   BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    quantity     INT NOT NULL DEFAULT 0,
    reserved_qty INT NOT NULL DEFAULT 0,
    version      BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT FK_stock_product FOREIGN KEY (product_id) REFERENCES product (id),
    CONSTRAINT FK_stock_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouse (id),
    CONSTRAINT CHECK_stock_quantity_notneg CHECK (quantity>=0),
    CONSTRAINT CHECK_stock_reserved_notneg CHECK (reserved_qty>=0),
    CONSTRAINT CHECK_stock_reserved_le_qty CHECK (reserved_qty <= quantity),
    CONSTRAINT UNIQUE_product_warehouse_pair UNIQUE (product_id, warehouse_id)
);

CREATE TABLE reservation(
    id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    reference     VARCHAR(100) NOT NULL,
    product_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    quantity INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT UNIQUE_reservation_reference UNIQUE (reference),
    CONSTRAINT FK_reservation_product FOREIGN KEY(product_id) REFERENCES product(id),
    CONSTRAINT FK_reservation_warehouse FOREIGN KEY(warehouse_id) REFERENCES warehouse(id),
    CONSTRAINT CHECK_reservation_quatitity CHECK (quantity>0)
);

CREATE INDEX index_product_category ON product (category_id);
CREATE INDEX index_reservation_product ON reservation (product_id);
