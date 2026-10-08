USE oms;

ALTER TABLE order_lines
    ADD COLUMN delivered_quantity INT NULL AFTER quantity,
    ADD CONSTRAINT chk_order_lines_delivered_qty
        CHECK (
            delivered_quantity IS NULL
            OR (
                delivered_quantity >= 0
                AND delivered_quantity <= quantity
            )
        );