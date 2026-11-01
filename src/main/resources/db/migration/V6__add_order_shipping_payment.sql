ALTER TABLE customer_orders
    ADD COLUMN shipping_name VARCHAR(120) NULL,
    ADD COLUMN shipping_phone VARCHAR(20) NULL,
    ADD COLUMN shipping_address VARCHAR(300) NULL,
    ADD COLUMN shipping_city VARCHAR(80) NULL,
    ADD COLUMN shipping_pincode VARCHAR(10) NULL,
    ADD COLUMN payment_method VARCHAR(20) NOT NULL DEFAULT 'COD',
    ADD COLUMN payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING';
