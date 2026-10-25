-- Chuyển một database OMS đang chạy sang kho sữa, để nhập dữ liệu thật từ đầu.
-- XOÁ toàn bộ hàng hoá và chứng từ (đơn, hoá đơn, thanh toán, tồn, lô, phiếu nhập/xuất, bảng giá chi tiết);
-- GIỮ tài khoản, phân quyền, kho, vị trí kho, khu vực, đại lý, nhóm đại lý, bảng giá (rỗng), chỉ tiêu, nhật ký.
-- Sao lưu trước khi chạy (mysqldump). Chạy lại được nhiều lần.
USE oms;

SET FOREIGN_KEY_CHECKS = 0;

-- Chứng từ bán hàng, giao hàng, công nợ
TRUNCATE TABLE notifications;
TRUNCATE TABLE return_credit_notes;
TRUNCATE TABLE sales_return_lines;
TRUNCATE TABLE sales_returns;
TRUNCATE TABLE route_collections;
TRUNCATE TABLE debt_reconciliation_lines;
TRUNCATE TABLE debt_reconciliations;
TRUNCATE TABLE customer_receivables;
TRUNCATE TABLE payment_allocations;
TRUNCATE TABLE payments;
TRUNCATE TABLE invoice_reminder_logs;
TRUNCATE TABLE invoice_lines;
TRUNCATE TABLE invoices;
TRUNCATE TABLE delivery_trip_shipments;
TRUNCATE TABLE delivery_trips;
TRUNCATE TABLE shipment_lot_allocations;
TRUNCATE TABLE shipment_items;
TRUNCATE TABLE shipments;
TRUNCATE TABLE picking_list_lines;
TRUNCATE TABLE picking_lists;
TRUNCATE TABLE order_inventory_reservations;
TRUNCATE TABLE order_credit_reservations;
TRUNCATE TABLE order_backorders;
TRUNCATE TABLE order_status_history;
TRUNCATE TABLE order_lines;
TRUNCATE TABLE orders;

-- Chứng từ kho
TRUNCATE TABLE goods_receipt_lines;
TRUNCATE TABLE goods_receipts;
TRUNCATE TABLE warehouse_transfer_lines;
TRUNCATE TABLE warehouse_transfers;
TRUNCATE TABLE inventory_count_lines;
TRUNCATE TABLE inventory_counts;
TRUNCATE TABLE inventory_adjustment_lines;
TRUNCATE TABLE inventory_adjustments;
TRUNCATE TABLE stock_adjustments;
TRUNCATE TABLE inventory_movements;
TRUNCATE TABLE inventory_lots;
TRUNCATE TABLE warehouse_damaged_inventory;
TRUNCATE TABLE warehouse_inventory;

-- Hàng hoá và giá
TRUNCATE TABLE discount_tiers;
TRUNCATE TABLE discount_policies;
TRUNCATE TABLE price_history;
TRUNCATE TABLE price_list_items;
TRUNCATE TABLE product_units;
TRUNCATE TABLE products;
TRUNCATE TABLE product_categories;

SET FOREIGN_KEY_CHECKS = 1;

-- Số chứng từ đánh lại từ 1
UPDATE document_sequences SET last_number = 0;

-- Bỏ nhà cung cấp mẫu hàng tiêu dùng cũ, giữ nhà cung cấp đã nhập tay
DELETE FROM suppliers WHERE code IN ('NCC-COCA', 'NCC-SUNTORY', 'NCC-ACECOOK');

-- Đơn vị tính hay dùng cho sữa
INSERT IGNORE INTO units (code, name) VALUES
  ('HOP', 'Hộp'), ('LOC', 'Lốc'), ('THUNG', 'Thùng'), ('LON', 'Lon'),
  ('CHAI', 'Chai'), ('BICH', 'Bịch'), ('HU', 'Hũ'), ('GOI', 'Gói');

-- Nhóm hàng sữa 2 cấp
INSERT INTO product_categories (id, parent_id, name) VALUES
  (1, NULL, 'Sữa tươi'),
  (2, 1, 'Sữa tươi tiệt trùng'),
  (3, 1, 'Sữa tươi thanh trùng'),
  (4, NULL, 'Sữa chua'),
  (5, 4, 'Sữa chua ăn'),
  (6, 4, 'Sữa chua uống'),
  (7, NULL, 'Sữa bột'),
  (8, 7, 'Sữa bột trẻ em'),
  (9, 7, 'Sữa bột người lớn'),
  (10, NULL, 'Sữa đặc'),
  (11, NULL, 'Sữa hạt'),
  (12, NULL, 'Phô mai, bơ, kem');
