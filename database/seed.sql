-- Dữ liệu danh mục cho Sprint 1. Tài khoản admin KHÔNG tạo ở đây mà do ứng dụng tạo lần đầu
-- (mật khẩu tạm lấy từ app.properties, không để trong file commit).
USE oms;

INSERT INTO roles (code, name, sort_order) VALUES
  ('CUSTOMER',      'Đại lý',               1),
  ('SALES_REP',     'Nhân viên kinh doanh', 2),
  ('SALES_MANAGER', 'Quản lý kinh doanh',   3),
  ('WAREHOUSE',     'Nhân viên kho',        4),
  ('WH_MANAGER',    'Quản lý kho',          5),
  ('ACCOUNTANT',    'Kế toán công nợ',      6),
  ('ADMIN',         'Quản trị hệ thống',    7);

INSERT INTO modules (code, name, sort_order) VALUES
  ('PRODUCT',         'Sản phẩm & bảng giá',      1),
  ('CUSTOMER_CREDIT', 'Đại lý & hạn mức công nợ', 2),
  ('ORDER',           'Đơn hàng',                 3),
  ('ORDER_APPROVAL',  'Duyệt đơn & giá đặc biệt', 4),
  ('INVENTORY',       'Tồn kho & nhập kho',       5),
  ('SHIPMENT',        'Phiếu xuất & giao hàng',   6),
  ('INVOICE',         'Hoá đơn & công nợ',        7),
  ('RETURN',          'Trả hàng & điều chỉnh',    8),
  ('REPORT',          'Báo cáo & dashboard',      9),
  ('USER_AUDIT',      'Người dùng & nhật ký',     10);

-- Ma trận phân quyền ban đầu, lấy từ sheet "2. User Roles"
INSERT INTO role_permissions (role_code, module_code, level) VALUES
  ('CUSTOMER', 'PRODUCT', 'R_OWN'), ('CUSTOMER', 'CUSTOMER_CREDIT', 'R_OWN'), ('CUSTOMER', 'ORDER', 'W_OWN'),
  ('CUSTOMER', 'SHIPMENT', 'R_OWN'), ('CUSTOMER', 'INVOICE', 'R_OWN'), ('CUSTOMER', 'RETURN', 'W_OWN'),

  ('SALES_REP', 'PRODUCT', 'R'), ('SALES_REP', 'CUSTOMER_CREDIT', 'W_OWN'), ('SALES_REP', 'ORDER', 'W_OWN'),
  ('SALES_REP', 'INVENTORY', 'R'), ('SALES_REP', 'SHIPMENT', 'R_OWN'), ('SALES_REP', 'INVOICE', 'R_OWN'),
  ('SALES_REP', 'RETURN', 'W_OWN'), ('SALES_REP', 'REPORT', 'R_OWN'),

  ('WAREHOUSE', 'PRODUCT', 'R'), ('WAREHOUSE', 'ORDER', 'R'), ('WAREHOUSE', 'INVENTORY', 'W'),
  ('WAREHOUSE', 'SHIPMENT', 'W'), ('WAREHOUSE', 'RETURN', 'W'),

  ('WH_MANAGER', 'PRODUCT', 'R'), ('WH_MANAGER', 'ORDER', 'R'), ('WH_MANAGER', 'INVENTORY', 'F'),
  ('WH_MANAGER', 'SHIPMENT', 'F'), ('WH_MANAGER', 'RETURN', 'F'), ('WH_MANAGER', 'REPORT', 'R'),

  ('ACCOUNTANT', 'PRODUCT', 'R'), ('ACCOUNTANT', 'CUSTOMER_CREDIT', 'W'), ('ACCOUNTANT', 'ORDER', 'R'),
  ('ACCOUNTANT', 'ORDER_APPROVAL', 'R'), ('ACCOUNTANT', 'INVENTORY', 'R'), ('ACCOUNTANT', 'SHIPMENT', 'R'),
  ('ACCOUNTANT', 'INVOICE', 'F'), ('ACCOUNTANT', 'RETURN', 'W'), ('ACCOUNTANT', 'REPORT', 'R'),

  ('SALES_MANAGER', 'PRODUCT', 'F'), ('SALES_MANAGER', 'CUSTOMER_CREDIT', 'F'), ('SALES_MANAGER', 'ORDER', 'F'),
  ('SALES_MANAGER', 'ORDER_APPROVAL', 'F'), ('SALES_MANAGER', 'INVENTORY', 'R'), ('SALES_MANAGER', 'SHIPMENT', 'R'),
  ('SALES_MANAGER', 'INVOICE', 'R'), ('SALES_MANAGER', 'RETURN', 'F'), ('SALES_MANAGER', 'REPORT', 'F'),
  ('SALES_MANAGER', 'USER_AUDIT', 'R');

INSERT INTO warehouses (code, name) VALUES
  ('KHO-LB', 'Kho Long Biên'),
  ('KHO-HD', 'Kho Hà Đông'),
  ('KHO-BN', 'Kho Bắc Ninh');

INSERT INTO areas (code, name) VALUES
  ('HN-1', 'Hà Nội – Khu vực 1'),
  ('HN-2', 'Hà Nội – Khu vực 2'),
  ('BN',   'Bắc Ninh');

-- Đại lý mẫu, chưa gán nhân viên phụ trách (gán khi làm S3-06)
INSERT INTO customers (code, name, phone, area_id) VALUES
  ('DL001', 'Tạp hoá Minh Anh',        '0911000001', 1),
  ('DL002', 'Cửa hàng Hoàng Long',     '0911000002', 1),
  ('DL003', 'Đại lý Phúc Thịnh',       '0911000003', 2),
  ('DL004', 'Siêu thị mini Bình An',   '0911000004', 2),
  ('DL005', 'Nhà phân phối Kinh Bắc',  '0911000005', 3);

-- Đơn vị tính và nhóm hàng của kho sữa (S2-05); sản phẩm, quy đổi và nhà cung cấp nhập qua giao diện
INSERT IGNORE INTO units (code, name) VALUES
  ('HOP', 'Hộp'), ('LOC', 'Lốc'), ('THUNG', 'Thùng'), ('LON', 'Lon'),
  ('CHAI', 'Chai'), ('BICH', 'Bịch'), ('HU', 'Hũ'), ('GOI', 'Gói');

-- Chỉ thêm khi bảng còn trống để chạy lại không nhân đôi
INSERT INTO product_categories (id, parent_id, name)
SELECT * FROM (SELECT 1 AS id, NULL AS parent_id, 'Sữa tươi' AS name UNION ALL
               SELECT 2, 1, 'Sữa tươi tiệt trùng' UNION ALL
               SELECT 3, 1, 'Sữa tươi thanh trùng' UNION ALL
               SELECT 4, NULL, 'Sữa chua' UNION ALL
               SELECT 5, 4, 'Sữa chua ăn' UNION ALL
               SELECT 6, 4, 'Sữa chua uống' UNION ALL
               SELECT 7, NULL, 'Sữa bột' UNION ALL
               SELECT 8, 7, 'Sữa bột trẻ em' UNION ALL
               SELECT 9, 7, 'Sữa bột người lớn' UNION ALL
               SELECT 10, NULL, 'Sữa đặc' UNION ALL
               SELECT 11, NULL, 'Sữa hạt' UNION ALL
               SELECT 12, NULL, 'Phô mai, bơ, kem') seed
WHERE NOT EXISTS (SELECT 1 FROM product_categories);

-- Nhóm khách hàng và bảng giá mẫu (S2-10)
INSERT IGNORE INTO customer_groups (code, name) VALUES
  ('DL1', 'Đại lý cấp 1'), ('DL2', 'Đại lý cấp 2'), ('LE', 'Khách lẻ');

-- Gán nhóm cho đại lý mẫu và 3 bảng giá mẫu đang áp dụng từ 01/09/2026
UPDATE customers SET group_id = (SELECT id FROM customer_groups WHERE code = 'DL1') WHERE code IN ('DL001', 'DL003', 'DL005') AND group_id IS NULL;
UPDATE customers SET group_id = (SELECT id FROM customer_groups WHERE code = 'DL2') WHERE code IN ('DL002', 'DL004') AND group_id IS NULL;

INSERT IGNORE INTO price_lists (code, version, name, customer_group_id, valid_from)
SELECT 'BG-DL1', 1, 'Giá đại lý cấp 1', id, '2026-09-01' FROM customer_groups WHERE code = 'DL1' UNION ALL
SELECT 'BG-DL2', 1, 'Giá đại lý cấp 2', id, '2026-09-01' FROM customer_groups WHERE code = 'DL2' UNION ALL
SELECT 'BG-LE', 1, 'Giá khách lẻ', id, '2026-09-01' FROM customer_groups WHERE code = 'LE';

-- Giá bán = giá vốn × hệ số theo nhóm, giá sàn thấp hơn giá bán một chút (làm tròn trăm đồng)
INSERT IGNORE INTO price_list_items (price_list_id, product_id, sale_price, floor_price)
SELECT pl.id, p.id,
       ROUND(p.cost_price * CASE pl.code WHEN 'BG-DL1' THEN 1.10 WHEN 'BG-DL2' THEN 1.15 ELSE 1.25 END, -2),
       ROUND(p.cost_price * CASE pl.code WHEN 'BG-DL1' THEN 1.05 WHEN 'BG-DL2' THEN 1.08 ELSE 1.15 END, -2)
FROM price_lists pl CROSS JOIN products p
WHERE pl.code IN ('BG-DL1', 'BG-DL2', 'BG-LE') AND pl.version = 1 AND p.cost_price IS NOT NULL;

-- Điểm giao hàng mẫu (S3-04): trụ sở làm điểm mặc định của mỗi đại lý
INSERT INTO customer_delivery_points (customer_id, address, receiver_name, receiver_phone, is_default)
SELECT c.id, CONCAT('Trụ sở ', c.name), c.name, COALESCE(c.phone, '0900000000'), TRUE FROM customers c;

-- Hạn mức công nợ mẫu (S3-05): đại lý cấp 1 nợ tối đa 500 triệu / 30 ngày, cấp 2 là 200 triệu / 15 ngày
UPDATE customers SET credit_limit = 500000000, credit_days = 30 WHERE group_id = (SELECT id FROM customer_groups WHERE code = 'DL1');
UPDATE customers SET credit_limit = 200000000, credit_days = 15 WHERE group_id = (SELECT id FROM customer_groups WHERE code = 'DL2');
