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
