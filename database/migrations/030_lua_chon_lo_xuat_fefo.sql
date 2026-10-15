USE oms;

CREATE TABLE picking_line_lots (
    id BIGINT NOT NULL AUTO_INCREMENT,
    picking_line_id BIGINT NOT NULL,
    inventory_lot_id BIGINT NOT NULL,
    quantity DECIMAL(14,4) NOT NULL,
    override_reason VARCHAR(500) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_picking_line_lot (picking_line_id, inventory_lot_id),
    CONSTRAINT fk_pll_line FOREIGN KEY (picking_line_id) REFERENCES picking_list_lines(id) ON DELETE CASCADE,
    CONSTRAINT fk_pll_lot FOREIGN KEY (inventory_lot_id) REFERENCES inventory_lots(id),
    CONSTRAINT chk_pll_quantity CHECK (quantity > 0)
);
