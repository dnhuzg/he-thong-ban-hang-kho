-- S8-07: dá»¯ liá»‡u tá»“n theo lÃ´ dÃ¹ng cho bÃ¡o cÃ¡o giÃ¡ trá»‹ tá»“n vÃ  sá»‘ ngÃ y tá»“n bÃ¬nh quÃ¢n.
-- remaining_quantity lÃ  lÆ°á»£ng cÃ²n láº¡i cá»§a lÃ´; nghiá»‡p vá»¥ nháº­p/xuáº¥t/chuyá»ƒn kho cáº­p nháº­t read model nÃ y.
USE oms;

CREATE TABLE IF NOT EXISTS inventory_lots (
  id                  BIGINT        NOT NULL AUTO_INCREMENT,
  warehouse_id        BIGINT        NOT NULL,
  product_id          BIGINT        NOT NULL,
  remaining_quantity  DECIMAL(14,4) NOT NULL,
  unit_cost           DECIMAL(15,2) NOT NULL,
  received_at         DATETIME      NOT NULL,
  source_type         VARCHAR(30)   NULL,
  source_id           BIGINT        NULL,
  created_at          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_inventory_lots_warehouse_product (warehouse_id, product_id),
  KEY idx_inventory_lots_product_received (product_id, received_at),
  CONSTRAINT fk_inventory_lots_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
  CONSTRAINT fk_inventory_lots_product FOREIGN KEY (product_id) REFERENCES products (id),
  CONSTRAINT chk_inventory_lots_quantity CHECK (remaining_quantity > 0),
  CONSTRAINT chk_inventory_lots_cost CHECK (unit_cost >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;