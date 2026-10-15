-- S6-04: FEFO
USE oms;

ALTER TABLE inventory_lots
    ADD COLUMN lot_no VARCHAR(50) NULL AFTER product_id,
    ADD COLUMN expiry_date DATE NULL AFTER lot_no,
    ADD INDEX idx_inventory_lots_fefo (warehouse_id, product_id, expiry_date, received_at);
