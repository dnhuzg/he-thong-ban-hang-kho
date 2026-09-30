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

-- Đơn vị tính, nhóm hàng 3 cấp và sản phẩm mẫu (S2-05)
INSERT IGNORE INTO units (code, name) VALUES
  ('LON', 'Lon'), ('LOC', 'Lốc'), ('THUNG', 'Thùng'), ('CHAI', 'Chai'),
  ('GOI', 'Gói'), ('HOP', 'Hộp'), ('KG', 'Kg'), ('CAI', 'Cái');

-- Nhóm hàng mẫu 3 cấp; chỉ thêm khi bảng còn trống để chạy lại không nhân đôi
INSERT INTO product_categories (id, parent_id, name)
SELECT * FROM (SELECT 1 AS id, NULL AS parent_id, 'Đồ uống' AS name UNION ALL
               SELECT 2, 1, 'Nước giải khát' UNION ALL
               SELECT 3, 2, 'Nước ngọt có ga' UNION ALL
               SELECT 4, 2, 'Nước tăng lực' UNION ALL
               SELECT 5, 1, 'Bia' UNION ALL
               SELECT 6, NULL, 'Thực phẩm khô' UNION ALL
               SELECT 7, 6, 'Mì, phở ăn liền') seed
WHERE NOT EXISTS (SELECT 1 FROM product_categories);

INSERT IGNORE INTO products (sku, name, category_id, base_unit_id, packing_spec, cost_price)
SELECT 'COCA-330', 'Coca-Cola lon 330ml', 3, (SELECT id FROM units WHERE code='LON'), '24 lon/thùng', 7200.00 UNION ALL
SELECT 'PEPSI-330', 'Pepsi lon 330ml', 3, (SELECT id FROM units WHERE code='LON'), '24 lon/thùng', 7000.00 UNION ALL
SELECT 'REDBULL-250', 'Red Bull lon 250ml', 4, (SELECT id FROM units WHERE code='LON'), '24 lon/thùng', 9500.00 UNION ALL
SELECT 'TIGER-330', 'Bia Tiger lon 330ml', 5, (SELECT id FROM units WHERE code='LON'), '24 lon/thùng', 14500.00 UNION ALL
SELECT 'HAOHAO-TOM', 'Mì Hảo Hảo tôm chua cay', 7, (SELECT id FROM units WHERE code='GOI'), '30 gói/thùng', 3300.00;

-- Quy đổi mẫu (S2-07)
INSERT IGNORE INTO product_units (product_id, unit_id, factor)
SELECT p.id, u.id, 6 FROM products p JOIN units b ON b.id = p.base_unit_id AND b.code = 'LON' JOIN units u ON u.code = 'LOC'
UNION ALL
SELECT p.id, u.id, 24 FROM products p JOIN units b ON b.id = p.base_unit_id AND b.code = 'LON' JOIN units u ON u.code = 'THUNG'
UNION ALL
SELECT p.id, u.id, 30 FROM products p JOIN units b ON b.id = p.base_unit_id AND b.code = 'GOI' JOIN units u ON u.code = 'THUNG';
