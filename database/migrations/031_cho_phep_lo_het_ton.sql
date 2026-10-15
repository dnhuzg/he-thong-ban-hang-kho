USE oms;

ALTER TABLE inventory_lots
    DROP CHECK chk_inventory_lots_quantity,
    ADD CONSTRAINT chk_inventory_lots_quantity CHECK (remaining_quantity >= 0);
