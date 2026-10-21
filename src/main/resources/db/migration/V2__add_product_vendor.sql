ALTER TABLE products
    ADD COLUMN vendor_id BIGINT NULL,
    ADD CONSTRAINT fk_products_vendor
        FOREIGN KEY (vendor_id) REFERENCES users (id)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT,
    ADD INDEX idx_products_vendor_id (vendor_id);
