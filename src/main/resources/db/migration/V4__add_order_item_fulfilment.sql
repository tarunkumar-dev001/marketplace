ALTER TABLE order_items
    ADD COLUMN fulfillment_status VARCHAR(20) NOT NULL DEFAULT 'PLACED';
