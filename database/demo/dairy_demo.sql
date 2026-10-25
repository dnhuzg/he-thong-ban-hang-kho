-- Dữ liệu DEMO kho sữa: nhà cung cấp, 35 sản phẩm sữa kèm quy đổi, giá trong 3 bảng giá và tồn kho đầu kỳ
-- (phiếu nhập đã xác nhận, có lô và hạn dùng) ở mọi kho đang hoạt động.
-- Chạy sau schema.sql + seed.sql và sau khi khởi động web lần đầu (cần tài khoản admin để đứng tên phiếu nhập).
-- Chạy lại nhiều lần an toàn: chỉ thêm cái chưa có. Sinh từ database/demo/source/dairy_gen.js: sửa danh sách ở đó rồi sinh lại, không sửa tay file này.
-- Chạy:  Get-Content database\demo\dairy_demo.sql -Raw -Encoding UTF8 | mysql -uoms_app --default-character-set=utf8mb4
USE oms;

INSERT IGNORE INTO units (code, name) VALUES
  ('HOP', 'Hộp'), ('LOC', 'Lốc'), ('THUNG', 'Thùng'), ('LON', 'Lon'), ('CHAI', 'Chai'), ('HU', 'Hũ');

-- Nhà cung cấp
INSERT IGNORE INTO suppliers (code, name, contact_name, payment_terms) VALUES
  ('VINAMILK', 'Công ty CP Sữa Việt Nam (Vinamilk)', 'Phòng kinh doanh', 'Công nợ 30 ngày'),
  ('NCC-TH', 'Công ty CP Sữa TH', 'Phòng kinh doanh', 'Công nợ 30 ngày'),
  ('NCC-FCV', 'Công ty TNHH FrieslandCampina Việt Nam', 'Phòng kinh doanh', 'Công nợ 15 ngày'),
  ('NCC-NUTI', 'Công ty CP Thực phẩm Dinh dưỡng Nutifood', 'Phòng kinh doanh', 'Thanh toán khi nhận hàng');

-- Sản phẩm: theo dõi lô và hạn dùng, cảnh báo sắp hết hạn theo loại sữa
INSERT IGNORE INTO products (sku, name, category_id, base_unit_id, packing_spec, cost_price, track_lots, expiry_warning_days)
SELECT 'VNM-STT-180-CD', 'Sữa tươi tiệt trùng Vinamilk 100% có đường 180ml', (SELECT id FROM product_categories WHERE name = 'Sữa tươi tiệt trùng' LIMIT 1), (SELECT id FROM units WHERE code = 'HOP'), '48 hộp/thùng', 6500.00, TRUE, 30 UNION ALL
SELECT 'VNM-STT-180-KD', 'Sữa tươi tiệt trùng Vinamilk 100% không đường 180ml', (SELECT id FROM product_categories WHERE name = 'Sữa tươi tiệt trùng' LIMIT 1), (SELECT id FROM units WHERE code = 'HOP'), '48 hộp/thùng', 6500.00, TRUE, 30 UNION ALL
SELECT 'VNM-STT-110-CD', 'Sữa tươi tiệt trùng Vinamilk 100% có đường 110ml', (SELECT id FROM product_categories WHERE name = 'Sữa tươi tiệt trùng' LIMIT 1), (SELECT id FROM units WHERE code = 'HOP'), '48 hộp/thùng', 4600.00, TRUE, 30 UNION ALL
SELECT 'TH-STT-180-CD', 'Sữa tươi tiệt trùng TH true MILK có đường 180ml', (SELECT id FROM product_categories WHERE name = 'Sữa tươi tiệt trùng' LIMIT 1), (SELECT id FROM units WHERE code = 'HOP'), '48 hộp/thùng', 7400.00, TRUE, 30 UNION ALL
SELECT 'TH-STT-180-IT', 'Sữa tươi tiệt trùng TH true MILK ít đường 180ml', (SELECT id FROM product_categories WHERE name = 'Sữa tươi tiệt trùng' LIMIT 1), (SELECT id FROM units WHERE code = 'HOP'), '48 hộp/thùng', 7400.00, TRUE, 30 UNION ALL
SELECT 'TH-STT-110-CD', 'Sữa tươi tiệt trùng TH true MILK có đường 110ml', (SELECT id FROM product_categories WHERE name = 'Sữa tươi tiệt trùng' LIMIT 1), (SELECT id FROM units WHERE code = 'HOP'), '48 hộp/thùng', 5000.00, TRUE, 30 UNION ALL
SELECT 'DL-STT-180-CD', 'Sữa tiệt trùng Dutch Lady có đường 180ml', (SELECT id FROM product_categories WHERE name = 'Sữa tươi tiệt trùng' LIMIT 1), (SELECT id FROM units WHERE code = 'HOP'), '48 hộp/thùng', 6300.00, TRUE, 30 UNION ALL
SELECT 'DL-STT-110-CD', 'Sữa tiệt trùng Dutch Lady có đường 110ml', (SELECT id FROM product_categories WHERE name = 'Sữa tươi tiệt trùng' LIMIT 1), (SELECT id FROM units WHERE code = 'HOP'), '48 hộp/thùng', 4300.00, TRUE, 30 UNION ALL
SELECT 'NTF-STT-180-CD', 'Sữa tiệt trùng Nutimilk có đường 180ml', (SELECT id FROM product_categories WHERE name = 'Sữa tươi tiệt trùng' LIMIT 1), (SELECT id FROM units WHERE code = 'HOP'), '48 hộp/thùng', 5800.00, TRUE, 30 UNION ALL
SELECT 'VNM-STT-1L-CD', 'Sữa tươi tiệt trùng Vinamilk 100% có đường hộp 1 lít', (SELECT id FROM product_categories WHERE name = 'Sữa tươi tiệt trùng' LIMIT 1), (SELECT id FROM units WHERE code = 'HOP'), '12 hộp/thùng', 32000.00, TRUE, 30 UNION ALL
SELECT 'TH-STT-1L-CD', 'Sữa tươi tiệt trùng TH true MILK có đường hộp 1 lít', (SELECT id FROM product_categories WHERE name = 'Sữa tươi tiệt trùng' LIMIT 1), (SELECT id FROM units WHERE code = 'HOP'), '12 hộp/thùng', 36000.00, TRUE, 30 UNION ALL
SELECT 'TH-STTR-450', 'Sữa tươi thanh trùng TH true MILK nguyên chất chai 450ml', (SELECT id FROM product_categories WHERE name = 'Sữa tươi thanh trùng' LIMIT 1), (SELECT id FROM units WHERE code = 'CHAI'), '20 chai/thùng', 17000.00, TRUE, 3 UNION ALL
SELECT 'VNM-STTR-450', 'Sữa tươi thanh trùng Vinamilk 100% chai 450ml', (SELECT id FROM product_categories WHERE name = 'Sữa tươi thanh trùng' LIMIT 1), (SELECT id FROM units WHERE code = 'CHAI'), '20 chai/thùng', 16000.00, TRUE, 3 UNION ALL
SELECT 'VNM-SCA-100-CD', 'Sữa chua ăn Vinamilk có đường hũ 100g', (SELECT id FROM product_categories WHERE name = 'Sữa chua ăn' LIMIT 1), (SELECT id FROM units WHERE code = 'HU'), '48 hũ/thùng', 5300.00, TRUE, 10 UNION ALL
SELECT 'VNM-SCA-100-KD', 'Sữa chua ăn Vinamilk không đường hũ 100g', (SELECT id FROM product_categories WHERE name = 'Sữa chua ăn' LIMIT 1), (SELECT id FROM units WHERE code = 'HU'), '48 hũ/thùng', 5300.00, TRUE, 10 UNION ALL
SELECT 'VNM-SCA-100-ND', 'Sữa chua ăn Vinamilk nha đam hũ 100g', (SELECT id FROM product_categories WHERE name = 'Sữa chua ăn' LIMIT 1), (SELECT id FROM units WHERE code = 'HU'), '48 hũ/thùng', 5800.00, TRUE, 10 UNION ALL
SELECT 'TH-SCA-100-CD', 'Sữa chua ăn TH true YOGURT có đường hũ 100g', (SELECT id FROM product_categories WHERE name = 'Sữa chua ăn' LIMIT 1), (SELECT id FROM units WHERE code = 'HU'), '48 hũ/thùng', 6500.00, TRUE, 10 UNION ALL
SELECT 'DL-YOMOST-170', 'Sữa chua uống Yomost hương dâu 170ml', (SELECT id FROM product_categories WHERE name = 'Sữa chua uống' LIMIT 1), (SELECT id FROM units WHERE code = 'HOP'), '48 hộp/thùng', 6500.00, TRUE, 10 UNION ALL
SELECT 'TH-SCU-180', 'Sữa chua uống TH true YOGURT men sống 180ml', (SELECT id FROM product_categories WHERE name = 'Sữa chua uống' LIMIT 1), (SELECT id FROM units WHERE code = 'HOP'), '48 hộp/thùng', 8000.00, TRUE, 10 UNION ALL
SELECT 'VNM-PROBI-65', 'Sữa chua uống Probi Vinamilk có đường chai 65ml', (SELECT id FROM product_categories WHERE name = 'Sữa chua uống' LIMIT 1), (SELECT id FROM units WHERE code = 'CHAI'), '50 chai/thùng', 4000.00, TRUE, 10 UNION ALL
SELECT 'VNM-DIELAC-900', 'Sữa bột Dielac Grow Plus 2+ lon 900g', (SELECT id FROM product_categories WHERE name = 'Sữa bột trẻ em' LIMIT 1), (SELECT id FROM units WHERE code = 'LON'), '12 lon/thùng', 245000.00, TRUE, 60 UNION ALL
SELECT 'VNM-OPTIMUM-850', 'Sữa bột Optimum Gold 4 lon 850g', (SELECT id FROM product_categories WHERE name = 'Sữa bột trẻ em' LIMIT 1), (SELECT id FROM units WHERE code = 'LON'), '12 lon/thùng', 320000.00, TRUE, 60 UNION ALL
SELECT 'DL-FRISO-850', 'Sữa bột Friso Gold 4 lon 850g', (SELECT id FROM product_categories WHERE name = 'Sữa bột trẻ em' LIMIT 1), (SELECT id FROM units WHERE code = 'LON'), '12 lon/thùng', 470000.00, TRUE, 60 UNION ALL
SELECT 'NTF-NUTIIQ-900', 'Sữa bột Nuti IQ Gold 4 lon 900g', (SELECT id FROM product_categories WHERE name = 'Sữa bột trẻ em' LIMIT 1), (SELECT id FROM units WHERE code = 'LON'), '12 lon/thùng', 230000.00, TRUE, 60 UNION ALL
SELECT 'NTF-GROWPLUS-1.5', 'Sữa bột GrowPLUS+ đỏ lon 1,5kg', (SELECT id FROM product_categories WHERE name = 'Sữa bột trẻ em' LIMIT 1), (SELECT id FROM units WHERE code = 'LON'), '6 lon/thùng', 420000.00, TRUE, 60 UNION ALL
SELECT 'VNM-SURE-900', 'Sữa bột Sure Prevent Gold lon 900g', (SELECT id FROM product_categories WHERE name = 'Sữa bột người lớn' LIMIT 1), (SELECT id FROM units WHERE code = 'LON'), '12 lon/thùng', 380000.00, TRUE, 60 UNION ALL
SELECT 'VNM-CANXI-900', 'Sữa bột Canxi Pro lon 900g', (SELECT id FROM product_categories WHERE name = 'Sữa bột người lớn' LIMIT 1), (SELECT id FROM units WHERE code = 'LON'), '12 lon/thùng', 245000.00, TRUE, 60 UNION ALL
SELECT 'NTF-VARNA-850', 'Sữa bột Varna Complete lon 850g', (SELECT id FROM product_categories WHERE name = 'Sữa bột người lớn' LIMIT 1), (SELECT id FROM units WHERE code = 'LON'), '12 lon/thùng', 495000.00, TRUE, 60 UNION ALL
SELECT 'VNM-ONGTHO-380', 'Sữa đặc có đường Ông Thọ trắng lon 380g', (SELECT id FROM product_categories WHERE name = 'Sữa đặc' LIMIT 1), (SELECT id FROM units WHERE code = 'LON'), '48 lon/thùng', 22500.00, TRUE, 30 UNION ALL
SELECT 'VNM-ONGTHO-DO-380', 'Sữa đặc có đường Ông Thọ đỏ lon 380g', (SELECT id FROM product_categories WHERE name = 'Sữa đặc' LIMIT 1), (SELECT id FROM units WHERE code = 'LON'), '48 lon/thùng', 21000.00, TRUE, 30 UNION ALL
SELECT 'VNM-NGOISAO-380', 'Sữa đặc có đường Ngôi Sao Phương Nam xanh lá lon 380g', (SELECT id FROM product_categories WHERE name = 'Sữa đặc' LIMIT 1), (SELECT id FROM units WHERE code = 'LON'), '48 lon/thùng', 19000.00, TRUE, 30 UNION ALL
SELECT 'VNM-HAT-OCCHO-180', 'Sữa hạt óc chó Vinamilk 180ml', (SELECT id FROM product_categories WHERE name = 'Sữa hạt' LIMIT 1), (SELECT id FROM units WHERE code = 'HOP'), '48 hộp/thùng', 8500.00, TRUE, 30 UNION ALL
SELECT 'TH-HAT-MACCA-180', 'Sữa hạt macca TH true NUT 180ml', (SELECT id FROM product_categories WHERE name = 'Sữa hạt' LIMIT 1), (SELECT id FROM units WHERE code = 'HOP'), '48 hộp/thùng', 9000.00, TRUE, 30 UNION ALL
SELECT 'VNM-KEM-VANI-450', 'Kem Vinamilk vani hộp 450ml', (SELECT id FROM product_categories WHERE name = 'Phô mai, bơ, kem' LIMIT 1), (SELECT id FROM units WHERE code = 'HOP'), '12 hộp/thùng', 45000.00, TRUE, 30 UNION ALL
SELECT 'TH-BO-200', 'Bơ lạt TH true BUTTER 200g', (SELECT id FROM product_categories WHERE name = 'Phô mai, bơ, kem' LIMIT 1), (SELECT id FROM units WHERE code = 'HOP'), '12 hộp/thùng', 55000.00, TRUE, 30;

-- Quy đổi đơn vị
INSERT IGNORE INTO product_units (product_id, unit_id, factor)
SELECT p.id, u.id, 4 FROM products p JOIN units u ON u.code = 'LOC' WHERE p.sku = 'VNM-STT-180-CD' UNION ALL
SELECT p.id, u.id, 48 FROM products p JOIN units u ON u.code = 'THUNG' WHERE p.sku = 'VNM-STT-180-CD' UNION ALL
SELECT p.id, u.id, 4 FROM products p JOIN units u ON u.code = 'LOC' WHERE p.sku = 'VNM-STT-180-KD' UNION ALL
SELECT p.id, u.id, 48 FROM products p JOIN units u ON u.code = 'THUNG' WHERE p.sku = 'VNM-STT-180-KD' UNION ALL
SELECT p.id, u.id, 4 FROM products p JOIN units u ON u.code = 'LOC' WHERE p.sku = 'VNM-STT-110-CD' UNION ALL
SELECT p.id, u.id, 48 FROM products p JOIN units u ON u.code = 'THUNG' WHERE p.sku = 'VNM-STT-110-CD' UNION ALL
SELECT p.id, u.id, 4 FROM products p JOIN units u ON u.code = 'LOC' WHERE p.sku = 'TH-STT-180-CD' UNION ALL
SELECT p.id, u.id, 48 FROM products p JOIN units u ON u.code = 'THUNG' WHERE p.sku = 'TH-STT-180-CD' UNION ALL
SELECT p.id, u.id, 4 FROM products p JOIN units u ON u.code = 'LOC' WHERE p.sku = 'TH-STT-180-IT' UNION ALL
SELECT p.id, u.id, 48 FROM products p JOIN units u ON u.code = 'THUNG' WHERE p.sku = 'TH-STT-180-IT' UNION ALL
SELECT p.id, u.id, 4 FROM products p JOIN units u ON u.code = 'LOC' WHERE p.sku = 'TH-STT-110-CD' UNION ALL
SELECT p.id, u.id, 48 FROM products p JOIN units u ON u.code = 'THUNG' WHERE p.sku = 'TH-STT-110-CD' UNION ALL
SELECT p.id, u.id, 4 FROM products p JOIN units u ON u.code = 'LOC' WHERE p.sku = 'DL-STT-180-CD' UNION ALL
SELECT p.id, u.id, 48 FROM products p JOIN units u ON u.code = 'THUNG' WHERE p.sku = 'DL-STT-180-CD' UNION ALL
SELECT p.id, u.id, 4 FROM products p JOIN units u ON u.code = 'LOC' WHERE p.sku = 'DL-STT-110-CD' UNION ALL
SELECT p.id, u.id, 48 FROM products p JOIN units u ON u.code = 'THUNG' WHERE p.sku = 'DL-STT-110-CD' UNION ALL
SELECT p.id, u.id, 4 FROM products p JOIN units u ON u.code = 'LOC' WHERE p.sku = 'NTF-STT-180-CD' UNION ALL
SELECT p.id, u.id, 48 FROM products p JOIN units u ON u.code = 'THUNG' WHERE p.sku = 'NTF-STT-180-CD' UNION ALL
SELECT p.id, u.id, 12 FROM products p JOIN units u ON u.code = 'THUNG' WHERE p.sku = 'VNM-STT-1L-CD' UNION ALL
SELECT p.id, u.id, 12 FROM products p JOIN units u ON u.code = 'THUNG' WHERE p.sku = 'TH-STT-1L-CD' UNION ALL
SELECT p.id, u.id, 20 FROM products p JOIN units u ON u.code = 'THUNG' WHERE p.sku = 'TH-STTR-450' UNION ALL
SELECT p.id, u.id, 20 FROM products p JOIN units u ON u.code = 'THUNG' WHERE p.sku = 'VNM-STTR-450' UNION ALL
SELECT p.id, u.id, 4 FROM products p JOIN units u ON u.code = 'LOC' WHERE p.sku = 'VNM-SCA-100-CD' UNION ALL
SELECT p.id, u.id, 48 FROM products p JOIN units u ON u.code = 'THUNG' WHERE p.sku = 'VNM-SCA-100-CD' UNION ALL
SELECT p.id, u.id, 4 FROM products p JOIN units u ON u.code = 'LOC' WHERE p.sku = 'VNM-SCA-100-KD' UNION ALL
SELECT p.id, u.id, 48 FROM products p JOIN units u ON u.code = 'THUNG' WHERE p.sku = 'VNM-SCA-100-KD' UNION ALL
SELECT p.id, u.id, 4 FROM products p JOIN units u ON u.code = 'LOC' WHERE p.sku = 'VNM-SCA-100-ND' UNION ALL
SELECT p.id, u.id, 48 FROM products p JOIN units u ON u.code = 'THUNG' WHERE p.sku = 'VNM-SCA-100-ND' UNION ALL
SELECT p.id, u.id, 4 FROM products p JOIN units u ON u.code = 'LOC' WHERE p.sku = 'TH-SCA-100-CD' UNION ALL
SELECT p.id, u.id, 48 FROM products p JOIN units u ON u.code = 'THUNG' WHERE p.sku = 'TH-SCA-100-CD' UNION ALL
SELECT p.id, u.id, 4 FROM products p JOIN units u ON u.code = 'LOC' WHERE p.sku = 'DL-YOMOST-170' UNION ALL
SELECT p.id, u.id, 48 FROM products p JOIN units u ON u.code = 'THUNG' WHERE p.sku = 'DL-YOMOST-170' UNION ALL
SELECT p.id, u.id, 4 FROM products p JOIN units u ON u.code = 'LOC' WHERE p.sku = 'TH-SCU-180' UNION ALL
SELECT p.id, u.id, 48 FROM products p JOIN units u ON u.code = 'THUNG' WHERE p.sku = 'TH-SCU-180' UNION ALL
SELECT p.id, u.id, 5 FROM products p JOIN units u ON u.code = 'LOC' WHERE p.sku = 'VNM-PROBI-65' UNION ALL
SELECT p.id, u.id, 50 FROM products p JOIN units u ON u.code = 'THUNG' WHERE p.sku = 'VNM-PROBI-65' UNION ALL
SELECT p.id, u.id, 12 FROM products p JOIN units u ON u.code = 'THUNG' WHERE p.sku = 'VNM-DIELAC-900' UNION ALL
SELECT p.id, u.id, 12 FROM products p JOIN units u ON u.code = 'THUNG' WHERE p.sku = 'VNM-OPTIMUM-850' UNION ALL
SELECT p.id, u.id, 12 FROM products p JOIN units u ON u.code = 'THUNG' WHERE p.sku = 'DL-FRISO-850' UNION ALL
SELECT p.id, u.id, 12 FROM products p JOIN units u ON u.code = 'THUNG' WHERE p.sku = 'NTF-NUTIIQ-900' UNION ALL
SELECT p.id, u.id, 6 FROM products p JOIN units u ON u.code = 'THUNG' WHERE p.sku = 'NTF-GROWPLUS-1.5' UNION ALL
SELECT p.id, u.id, 12 FROM products p JOIN units u ON u.code = 'THUNG' WHERE p.sku = 'VNM-SURE-900' UNION ALL
SELECT p.id, u.id, 12 FROM products p JOIN units u ON u.code = 'THUNG' WHERE p.sku = 'VNM-CANXI-900' UNION ALL
SELECT p.id, u.id, 12 FROM products p JOIN units u ON u.code = 'THUNG' WHERE p.sku = 'NTF-VARNA-850' UNION ALL
SELECT p.id, u.id, 48 FROM products p JOIN units u ON u.code = 'THUNG' WHERE p.sku = 'VNM-ONGTHO-380' UNION ALL
SELECT p.id, u.id, 48 FROM products p JOIN units u ON u.code = 'THUNG' WHERE p.sku = 'VNM-ONGTHO-DO-380' UNION ALL
SELECT p.id, u.id, 48 FROM products p JOIN units u ON u.code = 'THUNG' WHERE p.sku = 'VNM-NGOISAO-380' UNION ALL
SELECT p.id, u.id, 4 FROM products p JOIN units u ON u.code = 'LOC' WHERE p.sku = 'VNM-HAT-OCCHO-180' UNION ALL
SELECT p.id, u.id, 48 FROM products p JOIN units u ON u.code = 'THUNG' WHERE p.sku = 'VNM-HAT-OCCHO-180' UNION ALL
SELECT p.id, u.id, 4 FROM products p JOIN units u ON u.code = 'LOC' WHERE p.sku = 'TH-HAT-MACCA-180' UNION ALL
SELECT p.id, u.id, 48 FROM products p JOIN units u ON u.code = 'THUNG' WHERE p.sku = 'TH-HAT-MACCA-180' UNION ALL
SELECT p.id, u.id, 12 FROM products p JOIN units u ON u.code = 'THUNG' WHERE p.sku = 'VNM-KEM-VANI-450' UNION ALL
SELECT p.id, u.id, 12 FROM products p JOIN units u ON u.code = 'THUNG' WHERE p.sku = 'TH-BO-200';

-- Giá bán = giá vốn × hệ số theo nhóm đại lý, giá sàn thấp hơn một chút (làm tròn trăm đồng)
INSERT IGNORE INTO price_list_items (price_list_id, product_id, sale_price, floor_price)
SELECT pl.id, p.id,
       ROUND(p.cost_price * CASE pl.code WHEN 'BG-DL1' THEN 1.10 WHEN 'BG-DL2' THEN 1.15 ELSE 1.25 END, -2),
       ROUND(p.cost_price * CASE pl.code WHEN 'BG-DL1' THEN 1.05 WHEN 'BG-DL2' THEN 1.08 ELSE 1.15 END, -2)
FROM price_lists pl JOIN products p ON p.sku IN ('VNM-STT-180-CD', 'VNM-STT-180-KD', 'VNM-STT-110-CD', 'TH-STT-180-CD', 'TH-STT-180-IT', 'TH-STT-110-CD', 'DL-STT-180-CD', 'DL-STT-110-CD', 'NTF-STT-180-CD', 'VNM-STT-1L-CD', 'TH-STT-1L-CD', 'TH-STTR-450', 'VNM-STTR-450', 'VNM-SCA-100-CD', 'VNM-SCA-100-KD', 'VNM-SCA-100-ND', 'TH-SCA-100-CD', 'DL-YOMOST-170', 'TH-SCU-180', 'VNM-PROBI-65', 'VNM-DIELAC-900', 'VNM-OPTIMUM-850', 'DL-FRISO-850', 'NTF-NUTIIQ-900', 'NTF-GROWPLUS-1.5', 'VNM-SURE-900', 'VNM-CANXI-900', 'NTF-VARNA-850', 'VNM-ONGTHO-380', 'VNM-ONGTHO-DO-380', 'VNM-NGOISAO-380', 'VNM-HAT-OCCHO-180', 'TH-HAT-MACCA-180', 'VNM-KEM-VANI-450', 'TH-BO-200')
WHERE pl.code IN ('BG-DL1', 'BG-DL2', 'BG-LE') AND (pl.valid_to IS NULL OR pl.valid_to >= CURDATE());

-- Tồn đầu kỳ: phiếu nhập đã xác nhận, ghi đúng như GoodsReceiptService (phiếu, dòng, lô, sổ biến động, tồn)
SET @actor = (SELECT MIN(u.id) FROM users u JOIN user_roles r ON r.user_id = u.id AND r.role_code = 'ADMIN');

-- Kho thứ 1 · VINAMILK
SET @wh = (SELECT id FROM warehouses WHERE status = 'ACTIVE' ORDER BY id LIMIT 0, 1);
SET @sup = (SELECT id FROM suppliers WHERE code = 'VINAMILK');
INSERT INTO goods_receipts (supplier_id, document_no, received_date, warehouse_id, note, status, created_by, confirmed_by, confirmed_at)
SELECT @sup, 'DEMO-K1-VINAMILK', '2026-10-20', @wh, 'Tồn đầu kỳ (dữ liệu demo)', 'CONFIRMED', @actor, @actor, '2026-10-20 08:00:00'
WHERE @wh IS NOT NULL AND @actor IS NOT NULL AND NOT EXISTS (SELECT 1 FROM goods_receipts WHERE supplier_id = @sup AND document_no = 'DEMO-K1-VINAMILK');
SET @r = (SELECT id FROM goods_receipts WHERE supplier_id = @sup AND document_no = 'DEMO-K1-VINAMILK');
UPDATE goods_receipts SET code = CONCAT('PN', LPAD(id, 6, '0')) WHERE id = @r AND code IS NULL;
SET @new = @r IS NOT NULL AND NOT EXISTS (SELECT 1 FROM goods_receipt_lines WHERE receipt_id = @r);
CREATE TEMPORARY TABLE IF NOT EXISTS demo_lines (line_no INT, sku VARCHAR(50), qty INT, lot_no VARCHAR(50), expiry DATE);
TRUNCATE TABLE demo_lines;
INSERT INTO demo_lines VALUES
  (1, 'VNM-STT-180-CD', 20, 'L261015-VNM-STT-180-CD', '2027-04-13'),
  (2, 'VNM-STT-180-KD', 20, 'L261015-VNM-STT-180-KD', '2027-04-13'),
  (3, 'VNM-STT-110-CD', 20, 'L261015-VNM-STT-110-CD', '2027-04-13'),
  (4, 'VNM-STT-1L-CD', 20, 'L261015-VNM-STT-1L-CD', '2027-04-13'),
  (5, 'VNM-STTR-450', 20, 'L261015-VNM-STTR-450', '2026-10-25'),
  (6, 'VNM-SCA-100-CD', 20, 'L261015-VNM-SCA-100-CD', '2026-11-29'),
  (7, 'VNM-SCA-100-KD', 20, 'L261015-VNM-SCA-100-KD', '2026-11-29'),
  (8, 'VNM-SCA-100-ND', 20, 'L261015-VNM-SCA-100-ND', '2026-11-29'),
  (9, 'VNM-PROBI-65', 20, 'L261015-VNM-PROBI-65', '2026-11-29'),
  (10, 'VNM-DIELAC-900', 20, 'L261015-VNM-DIELAC-900', '2028-10-14'),
  (11, 'VNM-OPTIMUM-850', 20, 'L261015-VNM-OPTIMUM-850', '2028-10-14'),
  (12, 'VNM-SURE-900', 20, 'L261015-VNM-SURE-900', '2028-10-14'),
  (13, 'VNM-CANXI-900', 20, 'L261015-VNM-CANXI-900', '2028-10-14'),
  (14, 'VNM-ONGTHO-380', 20, 'L261015-VNM-ONGTHO-380', '2027-10-15'),
  (15, 'VNM-ONGTHO-DO-380', 20, 'L261015-VNM-ONGTHO-DO-380', '2027-10-15'),
  (16, 'VNM-NGOISAO-380', 20, 'L261015-VNM-NGOISAO-380', '2027-10-15'),
  (17, 'VNM-HAT-OCCHO-180', 20, 'L261015-VNM-HAT-OCCHO-180', '2027-07-12'),
  (18, 'VNM-KEM-VANI-450', 20, 'L261015-VNM-KEM-VANI-450', '2027-10-15');
INSERT INTO goods_receipt_lines (receipt_id, line_no, product_id, unit_id, quantity, factor, base_quantity, manufactured_date, lot_no, expiry_date)
SELECT @r, d.line_no, p.id, u.id, d.qty, pu.factor, d.qty * pu.factor, '2026-10-15', d.lot_no, d.expiry
FROM demo_lines d JOIN products p ON p.sku = d.sku JOIN units u ON u.code = 'THUNG' JOIN product_units pu ON pu.product_id = p.id AND pu.unit_id = u.id
WHERE @new;
INSERT INTO inventory_lots (warehouse_id, product_id, remaining_quantity, unit_cost, received_at, source_type, source_id, lot_no, manufactured_date, expiry_date)
SELECT @wh, l.product_id, l.base_quantity, COALESCE(p.cost_price, 0), '2026-10-20 00:00:00', 'GOODS_RECEIPT', @r, l.lot_no, l.manufactured_date, l.expiry_date
FROM goods_receipt_lines l JOIN products p ON p.id = l.product_id WHERE @new AND l.receipt_id = @r;
INSERT INTO inventory_movements (warehouse_id, product_id, unit_id, quantity, movement_type, reference_type, reference_id, created_by, created_at)
SELECT @wh, l.product_id, p.base_unit_id, l.base_quantity, 'GOODS_RECEIPT', 'GOODS_RECEIPT', @r, @actor, '2026-10-20 08:00:00'
FROM goods_receipt_lines l JOIN products p ON p.id = l.product_id WHERE @new AND l.receipt_id = @r;
INSERT INTO warehouse_inventory (warehouse_id, product_id, on_hand, reserved)
SELECT @wh, l.product_id, l.base_quantity, 0 FROM goods_receipt_lines l WHERE @new AND l.receipt_id = @r
ON DUPLICATE KEY UPDATE on_hand = on_hand + VALUES(on_hand);

-- Kho thứ 1 · NCC-TH
SET @wh = (SELECT id FROM warehouses WHERE status = 'ACTIVE' ORDER BY id LIMIT 0, 1);
SET @sup = (SELECT id FROM suppliers WHERE code = 'NCC-TH');
INSERT INTO goods_receipts (supplier_id, document_no, received_date, warehouse_id, note, status, created_by, confirmed_by, confirmed_at)
SELECT @sup, 'DEMO-K1-NCC-TH', '2026-10-20', @wh, 'Tồn đầu kỳ (dữ liệu demo)', 'CONFIRMED', @actor, @actor, '2026-10-20 08:00:00'
WHERE @wh IS NOT NULL AND @actor IS NOT NULL AND NOT EXISTS (SELECT 1 FROM goods_receipts WHERE supplier_id = @sup AND document_no = 'DEMO-K1-NCC-TH');
SET @r = (SELECT id FROM goods_receipts WHERE supplier_id = @sup AND document_no = 'DEMO-K1-NCC-TH');
UPDATE goods_receipts SET code = CONCAT('PN', LPAD(id, 6, '0')) WHERE id = @r AND code IS NULL;
SET @new = @r IS NOT NULL AND NOT EXISTS (SELECT 1 FROM goods_receipt_lines WHERE receipt_id = @r);
CREATE TEMPORARY TABLE IF NOT EXISTS demo_lines (line_no INT, sku VARCHAR(50), qty INT, lot_no VARCHAR(50), expiry DATE);
TRUNCATE TABLE demo_lines;
INSERT INTO demo_lines VALUES
  (1, 'TH-STT-180-CD', 20, 'L261015-TH-STT-180-CD', '2027-04-13'),
  (2, 'TH-STT-180-IT', 20, 'L261015-TH-STT-180-IT', '2027-04-13'),
  (3, 'TH-STT-110-CD', 20, 'L261015-TH-STT-110-CD', '2027-04-13'),
  (4, 'TH-STT-1L-CD', 20, 'L261015-TH-STT-1L-CD', '2027-04-13'),
  (5, 'TH-STTR-450', 20, 'L261015-TH-STTR-450', '2026-10-25'),
  (6, 'TH-SCA-100-CD', 20, 'L261015-TH-SCA-100-CD', '2026-11-29'),
  (7, 'TH-SCU-180', 20, 'L261015-TH-SCU-180', '2026-12-14'),
  (8, 'TH-HAT-MACCA-180', 20, 'L261015-TH-HAT-MACCA-180', '2027-07-12'),
  (9, 'TH-BO-200', 20, 'L261015-TH-BO-200', '2027-10-15');
INSERT INTO goods_receipt_lines (receipt_id, line_no, product_id, unit_id, quantity, factor, base_quantity, manufactured_date, lot_no, expiry_date)
SELECT @r, d.line_no, p.id, u.id, d.qty, pu.factor, d.qty * pu.factor, '2026-10-15', d.lot_no, d.expiry
FROM demo_lines d JOIN products p ON p.sku = d.sku JOIN units u ON u.code = 'THUNG' JOIN product_units pu ON pu.product_id = p.id AND pu.unit_id = u.id
WHERE @new;
INSERT INTO inventory_lots (warehouse_id, product_id, remaining_quantity, unit_cost, received_at, source_type, source_id, lot_no, manufactured_date, expiry_date)
SELECT @wh, l.product_id, l.base_quantity, COALESCE(p.cost_price, 0), '2026-10-20 00:00:00', 'GOODS_RECEIPT', @r, l.lot_no, l.manufactured_date, l.expiry_date
FROM goods_receipt_lines l JOIN products p ON p.id = l.product_id WHERE @new AND l.receipt_id = @r;
INSERT INTO inventory_movements (warehouse_id, product_id, unit_id, quantity, movement_type, reference_type, reference_id, created_by, created_at)
SELECT @wh, l.product_id, p.base_unit_id, l.base_quantity, 'GOODS_RECEIPT', 'GOODS_RECEIPT', @r, @actor, '2026-10-20 08:00:00'
FROM goods_receipt_lines l JOIN products p ON p.id = l.product_id WHERE @new AND l.receipt_id = @r;
INSERT INTO warehouse_inventory (warehouse_id, product_id, on_hand, reserved)
SELECT @wh, l.product_id, l.base_quantity, 0 FROM goods_receipt_lines l WHERE @new AND l.receipt_id = @r
ON DUPLICATE KEY UPDATE on_hand = on_hand + VALUES(on_hand);

-- Kho thứ 1 · NCC-FCV
SET @wh = (SELECT id FROM warehouses WHERE status = 'ACTIVE' ORDER BY id LIMIT 0, 1);
SET @sup = (SELECT id FROM suppliers WHERE code = 'NCC-FCV');
INSERT INTO goods_receipts (supplier_id, document_no, received_date, warehouse_id, note, status, created_by, confirmed_by, confirmed_at)
SELECT @sup, 'DEMO-K1-NCC-FCV', '2026-10-20', @wh, 'Tồn đầu kỳ (dữ liệu demo)', 'CONFIRMED', @actor, @actor, '2026-10-20 08:00:00'
WHERE @wh IS NOT NULL AND @actor IS NOT NULL AND NOT EXISTS (SELECT 1 FROM goods_receipts WHERE supplier_id = @sup AND document_no = 'DEMO-K1-NCC-FCV');
SET @r = (SELECT id FROM goods_receipts WHERE supplier_id = @sup AND document_no = 'DEMO-K1-NCC-FCV');
UPDATE goods_receipts SET code = CONCAT('PN', LPAD(id, 6, '0')) WHERE id = @r AND code IS NULL;
SET @new = @r IS NOT NULL AND NOT EXISTS (SELECT 1 FROM goods_receipt_lines WHERE receipt_id = @r);
CREATE TEMPORARY TABLE IF NOT EXISTS demo_lines (line_no INT, sku VARCHAR(50), qty INT, lot_no VARCHAR(50), expiry DATE);
TRUNCATE TABLE demo_lines;
INSERT INTO demo_lines VALUES
  (1, 'DL-STT-180-CD', 20, 'L261015-DL-STT-180-CD', '2027-04-13'),
  (2, 'DL-STT-110-CD', 20, 'L261015-DL-STT-110-CD', '2027-04-13'),
  (3, 'DL-YOMOST-170', 20, 'L261015-DL-YOMOST-170', '2026-12-14'),
  (4, 'DL-FRISO-850', 20, 'L261015-DL-FRISO-850', '2028-10-14');
INSERT INTO goods_receipt_lines (receipt_id, line_no, product_id, unit_id, quantity, factor, base_quantity, manufactured_date, lot_no, expiry_date)
SELECT @r, d.line_no, p.id, u.id, d.qty, pu.factor, d.qty * pu.factor, '2026-10-15', d.lot_no, d.expiry
FROM demo_lines d JOIN products p ON p.sku = d.sku JOIN units u ON u.code = 'THUNG' JOIN product_units pu ON pu.product_id = p.id AND pu.unit_id = u.id
WHERE @new;
INSERT INTO inventory_lots (warehouse_id, product_id, remaining_quantity, unit_cost, received_at, source_type, source_id, lot_no, manufactured_date, expiry_date)
SELECT @wh, l.product_id, l.base_quantity, COALESCE(p.cost_price, 0), '2026-10-20 00:00:00', 'GOODS_RECEIPT', @r, l.lot_no, l.manufactured_date, l.expiry_date
FROM goods_receipt_lines l JOIN products p ON p.id = l.product_id WHERE @new AND l.receipt_id = @r;
INSERT INTO inventory_movements (warehouse_id, product_id, unit_id, quantity, movement_type, reference_type, reference_id, created_by, created_at)
SELECT @wh, l.product_id, p.base_unit_id, l.base_quantity, 'GOODS_RECEIPT', 'GOODS_RECEIPT', @r, @actor, '2026-10-20 08:00:00'
FROM goods_receipt_lines l JOIN products p ON p.id = l.product_id WHERE @new AND l.receipt_id = @r;
INSERT INTO warehouse_inventory (warehouse_id, product_id, on_hand, reserved)
SELECT @wh, l.product_id, l.base_quantity, 0 FROM goods_receipt_lines l WHERE @new AND l.receipt_id = @r
ON DUPLICATE KEY UPDATE on_hand = on_hand + VALUES(on_hand);

-- Kho thứ 1 · NCC-NUTI
SET @wh = (SELECT id FROM warehouses WHERE status = 'ACTIVE' ORDER BY id LIMIT 0, 1);
SET @sup = (SELECT id FROM suppliers WHERE code = 'NCC-NUTI');
INSERT INTO goods_receipts (supplier_id, document_no, received_date, warehouse_id, note, status, created_by, confirmed_by, confirmed_at)
SELECT @sup, 'DEMO-K1-NCC-NUTI', '2026-10-20', @wh, 'Tồn đầu kỳ (dữ liệu demo)', 'CONFIRMED', @actor, @actor, '2026-10-20 08:00:00'
WHERE @wh IS NOT NULL AND @actor IS NOT NULL AND NOT EXISTS (SELECT 1 FROM goods_receipts WHERE supplier_id = @sup AND document_no = 'DEMO-K1-NCC-NUTI');
SET @r = (SELECT id FROM goods_receipts WHERE supplier_id = @sup AND document_no = 'DEMO-K1-NCC-NUTI');
UPDATE goods_receipts SET code = CONCAT('PN', LPAD(id, 6, '0')) WHERE id = @r AND code IS NULL;
SET @new = @r IS NOT NULL AND NOT EXISTS (SELECT 1 FROM goods_receipt_lines WHERE receipt_id = @r);
CREATE TEMPORARY TABLE IF NOT EXISTS demo_lines (line_no INT, sku VARCHAR(50), qty INT, lot_no VARCHAR(50), expiry DATE);
TRUNCATE TABLE demo_lines;
INSERT INTO demo_lines VALUES
  (1, 'NTF-STT-180-CD', 20, 'L261015-NTF-STT-180-CD', '2027-04-13'),
  (2, 'NTF-NUTIIQ-900', 20, 'L261015-NTF-NUTIIQ-900', '2028-10-14'),
  (3, 'NTF-GROWPLUS-1.5', 20, 'L261015-NTF-GROWPLUS-1.5', '2028-10-14'),
  (4, 'NTF-VARNA-850', 20, 'L261015-NTF-VARNA-850', '2028-10-14');
INSERT INTO goods_receipt_lines (receipt_id, line_no, product_id, unit_id, quantity, factor, base_quantity, manufactured_date, lot_no, expiry_date)
SELECT @r, d.line_no, p.id, u.id, d.qty, pu.factor, d.qty * pu.factor, '2026-10-15', d.lot_no, d.expiry
FROM demo_lines d JOIN products p ON p.sku = d.sku JOIN units u ON u.code = 'THUNG' JOIN product_units pu ON pu.product_id = p.id AND pu.unit_id = u.id
WHERE @new;
INSERT INTO inventory_lots (warehouse_id, product_id, remaining_quantity, unit_cost, received_at, source_type, source_id, lot_no, manufactured_date, expiry_date)
SELECT @wh, l.product_id, l.base_quantity, COALESCE(p.cost_price, 0), '2026-10-20 00:00:00', 'GOODS_RECEIPT', @r, l.lot_no, l.manufactured_date, l.expiry_date
FROM goods_receipt_lines l JOIN products p ON p.id = l.product_id WHERE @new AND l.receipt_id = @r;
INSERT INTO inventory_movements (warehouse_id, product_id, unit_id, quantity, movement_type, reference_type, reference_id, created_by, created_at)
SELECT @wh, l.product_id, p.base_unit_id, l.base_quantity, 'GOODS_RECEIPT', 'GOODS_RECEIPT', @r, @actor, '2026-10-20 08:00:00'
FROM goods_receipt_lines l JOIN products p ON p.id = l.product_id WHERE @new AND l.receipt_id = @r;
INSERT INTO warehouse_inventory (warehouse_id, product_id, on_hand, reserved)
SELECT @wh, l.product_id, l.base_quantity, 0 FROM goods_receipt_lines l WHERE @new AND l.receipt_id = @r
ON DUPLICATE KEY UPDATE on_hand = on_hand + VALUES(on_hand);

-- Kho thứ 2 · VINAMILK
SET @wh = (SELECT id FROM warehouses WHERE status = 'ACTIVE' ORDER BY id LIMIT 1, 1);
SET @sup = (SELECT id FROM suppliers WHERE code = 'VINAMILK');
INSERT INTO goods_receipts (supplier_id, document_no, received_date, warehouse_id, note, status, created_by, confirmed_by, confirmed_at)
SELECT @sup, 'DEMO-K2-VINAMILK', '2026-10-20', @wh, 'Tồn đầu kỳ (dữ liệu demo)', 'CONFIRMED', @actor, @actor, '2026-10-20 08:00:00'
WHERE @wh IS NOT NULL AND @actor IS NOT NULL AND NOT EXISTS (SELECT 1 FROM goods_receipts WHERE supplier_id = @sup AND document_no = 'DEMO-K2-VINAMILK');
SET @r = (SELECT id FROM goods_receipts WHERE supplier_id = @sup AND document_no = 'DEMO-K2-VINAMILK');
UPDATE goods_receipts SET code = CONCAT('PN', LPAD(id, 6, '0')) WHERE id = @r AND code IS NULL;
SET @new = @r IS NOT NULL AND NOT EXISTS (SELECT 1 FROM goods_receipt_lines WHERE receipt_id = @r);
CREATE TEMPORARY TABLE IF NOT EXISTS demo_lines (line_no INT, sku VARCHAR(50), qty INT, lot_no VARCHAR(50), expiry DATE);
TRUNCATE TABLE demo_lines;
INSERT INTO demo_lines VALUES
  (1, 'VNM-STT-180-CD', 10, 'L261015-VNM-STT-180-CD', '2027-04-13'),
  (2, 'VNM-STT-180-KD', 10, 'L261015-VNM-STT-180-KD', '2027-04-13'),
  (3, 'VNM-STT-110-CD', 10, 'L261015-VNM-STT-110-CD', '2027-04-13'),
  (4, 'VNM-STT-1L-CD', 10, 'L261015-VNM-STT-1L-CD', '2027-04-13'),
  (5, 'VNM-STTR-450', 10, 'L261015-VNM-STTR-450', '2026-10-25'),
  (6, 'VNM-SCA-100-CD', 10, 'L261015-VNM-SCA-100-CD', '2026-11-29'),
  (7, 'VNM-SCA-100-KD', 10, 'L261015-VNM-SCA-100-KD', '2026-11-29'),
  (8, 'VNM-SCA-100-ND', 10, 'L261015-VNM-SCA-100-ND', '2026-11-29'),
  (9, 'VNM-PROBI-65', 10, 'L261015-VNM-PROBI-65', '2026-11-29'),
  (10, 'VNM-DIELAC-900', 10, 'L261015-VNM-DIELAC-900', '2028-10-14'),
  (11, 'VNM-OPTIMUM-850', 10, 'L261015-VNM-OPTIMUM-850', '2028-10-14'),
  (12, 'VNM-SURE-900', 10, 'L261015-VNM-SURE-900', '2028-10-14'),
  (13, 'VNM-CANXI-900', 10, 'L261015-VNM-CANXI-900', '2028-10-14'),
  (14, 'VNM-ONGTHO-380', 10, 'L261015-VNM-ONGTHO-380', '2027-10-15'),
  (15, 'VNM-ONGTHO-DO-380', 10, 'L261015-VNM-ONGTHO-DO-380', '2027-10-15'),
  (16, 'VNM-NGOISAO-380', 10, 'L261015-VNM-NGOISAO-380', '2027-10-15'),
  (17, 'VNM-HAT-OCCHO-180', 10, 'L261015-VNM-HAT-OCCHO-180', '2027-07-12'),
  (18, 'VNM-KEM-VANI-450', 10, 'L261015-VNM-KEM-VANI-450', '2027-10-15');
INSERT INTO goods_receipt_lines (receipt_id, line_no, product_id, unit_id, quantity, factor, base_quantity, manufactured_date, lot_no, expiry_date)
SELECT @r, d.line_no, p.id, u.id, d.qty, pu.factor, d.qty * pu.factor, '2026-10-15', d.lot_no, d.expiry
FROM demo_lines d JOIN products p ON p.sku = d.sku JOIN units u ON u.code = 'THUNG' JOIN product_units pu ON pu.product_id = p.id AND pu.unit_id = u.id
WHERE @new;
INSERT INTO inventory_lots (warehouse_id, product_id, remaining_quantity, unit_cost, received_at, source_type, source_id, lot_no, manufactured_date, expiry_date)
SELECT @wh, l.product_id, l.base_quantity, COALESCE(p.cost_price, 0), '2026-10-20 00:00:00', 'GOODS_RECEIPT', @r, l.lot_no, l.manufactured_date, l.expiry_date
FROM goods_receipt_lines l JOIN products p ON p.id = l.product_id WHERE @new AND l.receipt_id = @r;
INSERT INTO inventory_movements (warehouse_id, product_id, unit_id, quantity, movement_type, reference_type, reference_id, created_by, created_at)
SELECT @wh, l.product_id, p.base_unit_id, l.base_quantity, 'GOODS_RECEIPT', 'GOODS_RECEIPT', @r, @actor, '2026-10-20 08:00:00'
FROM goods_receipt_lines l JOIN products p ON p.id = l.product_id WHERE @new AND l.receipt_id = @r;
INSERT INTO warehouse_inventory (warehouse_id, product_id, on_hand, reserved)
SELECT @wh, l.product_id, l.base_quantity, 0 FROM goods_receipt_lines l WHERE @new AND l.receipt_id = @r
ON DUPLICATE KEY UPDATE on_hand = on_hand + VALUES(on_hand);

-- Kho thứ 2 · NCC-TH
SET @wh = (SELECT id FROM warehouses WHERE status = 'ACTIVE' ORDER BY id LIMIT 1, 1);
SET @sup = (SELECT id FROM suppliers WHERE code = 'NCC-TH');
INSERT INTO goods_receipts (supplier_id, document_no, received_date, warehouse_id, note, status, created_by, confirmed_by, confirmed_at)
SELECT @sup, 'DEMO-K2-NCC-TH', '2026-10-20', @wh, 'Tồn đầu kỳ (dữ liệu demo)', 'CONFIRMED', @actor, @actor, '2026-10-20 08:00:00'
WHERE @wh IS NOT NULL AND @actor IS NOT NULL AND NOT EXISTS (SELECT 1 FROM goods_receipts WHERE supplier_id = @sup AND document_no = 'DEMO-K2-NCC-TH');
SET @r = (SELECT id FROM goods_receipts WHERE supplier_id = @sup AND document_no = 'DEMO-K2-NCC-TH');
UPDATE goods_receipts SET code = CONCAT('PN', LPAD(id, 6, '0')) WHERE id = @r AND code IS NULL;
SET @new = @r IS NOT NULL AND NOT EXISTS (SELECT 1 FROM goods_receipt_lines WHERE receipt_id = @r);
CREATE TEMPORARY TABLE IF NOT EXISTS demo_lines (line_no INT, sku VARCHAR(50), qty INT, lot_no VARCHAR(50), expiry DATE);
TRUNCATE TABLE demo_lines;
INSERT INTO demo_lines VALUES
  (1, 'TH-STT-180-CD', 10, 'L261015-TH-STT-180-CD', '2027-04-13'),
  (2, 'TH-STT-180-IT', 10, 'L261015-TH-STT-180-IT', '2027-04-13'),
  (3, 'TH-STT-110-CD', 10, 'L261015-TH-STT-110-CD', '2027-04-13'),
  (4, 'TH-STT-1L-CD', 10, 'L261015-TH-STT-1L-CD', '2027-04-13'),
  (5, 'TH-STTR-450', 10, 'L261015-TH-STTR-450', '2026-10-25'),
  (6, 'TH-SCA-100-CD', 10, 'L261015-TH-SCA-100-CD', '2026-11-29'),
  (7, 'TH-SCU-180', 10, 'L261015-TH-SCU-180', '2026-12-14'),
  (8, 'TH-HAT-MACCA-180', 10, 'L261015-TH-HAT-MACCA-180', '2027-07-12'),
  (9, 'TH-BO-200', 10, 'L261015-TH-BO-200', '2027-10-15');
INSERT INTO goods_receipt_lines (receipt_id, line_no, product_id, unit_id, quantity, factor, base_quantity, manufactured_date, lot_no, expiry_date)
SELECT @r, d.line_no, p.id, u.id, d.qty, pu.factor, d.qty * pu.factor, '2026-10-15', d.lot_no, d.expiry
FROM demo_lines d JOIN products p ON p.sku = d.sku JOIN units u ON u.code = 'THUNG' JOIN product_units pu ON pu.product_id = p.id AND pu.unit_id = u.id
WHERE @new;
INSERT INTO inventory_lots (warehouse_id, product_id, remaining_quantity, unit_cost, received_at, source_type, source_id, lot_no, manufactured_date, expiry_date)
SELECT @wh, l.product_id, l.base_quantity, COALESCE(p.cost_price, 0), '2026-10-20 00:00:00', 'GOODS_RECEIPT', @r, l.lot_no, l.manufactured_date, l.expiry_date
FROM goods_receipt_lines l JOIN products p ON p.id = l.product_id WHERE @new AND l.receipt_id = @r;
INSERT INTO inventory_movements (warehouse_id, product_id, unit_id, quantity, movement_type, reference_type, reference_id, created_by, created_at)
SELECT @wh, l.product_id, p.base_unit_id, l.base_quantity, 'GOODS_RECEIPT', 'GOODS_RECEIPT', @r, @actor, '2026-10-20 08:00:00'
FROM goods_receipt_lines l JOIN products p ON p.id = l.product_id WHERE @new AND l.receipt_id = @r;
INSERT INTO warehouse_inventory (warehouse_id, product_id, on_hand, reserved)
SELECT @wh, l.product_id, l.base_quantity, 0 FROM goods_receipt_lines l WHERE @new AND l.receipt_id = @r
ON DUPLICATE KEY UPDATE on_hand = on_hand + VALUES(on_hand);

-- Kho thứ 2 · NCC-FCV
SET @wh = (SELECT id FROM warehouses WHERE status = 'ACTIVE' ORDER BY id LIMIT 1, 1);
SET @sup = (SELECT id FROM suppliers WHERE code = 'NCC-FCV');
INSERT INTO goods_receipts (supplier_id, document_no, received_date, warehouse_id, note, status, created_by, confirmed_by, confirmed_at)
SELECT @sup, 'DEMO-K2-NCC-FCV', '2026-10-20', @wh, 'Tồn đầu kỳ (dữ liệu demo)', 'CONFIRMED', @actor, @actor, '2026-10-20 08:00:00'
WHERE @wh IS NOT NULL AND @actor IS NOT NULL AND NOT EXISTS (SELECT 1 FROM goods_receipts WHERE supplier_id = @sup AND document_no = 'DEMO-K2-NCC-FCV');
SET @r = (SELECT id FROM goods_receipts WHERE supplier_id = @sup AND document_no = 'DEMO-K2-NCC-FCV');
UPDATE goods_receipts SET code = CONCAT('PN', LPAD(id, 6, '0')) WHERE id = @r AND code IS NULL;
SET @new = @r IS NOT NULL AND NOT EXISTS (SELECT 1 FROM goods_receipt_lines WHERE receipt_id = @r);
CREATE TEMPORARY TABLE IF NOT EXISTS demo_lines (line_no INT, sku VARCHAR(50), qty INT, lot_no VARCHAR(50), expiry DATE);
TRUNCATE TABLE demo_lines;
INSERT INTO demo_lines VALUES
  (1, 'DL-STT-180-CD', 10, 'L261015-DL-STT-180-CD', '2027-04-13'),
  (2, 'DL-STT-110-CD', 10, 'L261015-DL-STT-110-CD', '2027-04-13'),
  (3, 'DL-YOMOST-170', 10, 'L261015-DL-YOMOST-170', '2026-12-14'),
  (4, 'DL-FRISO-850', 10, 'L261015-DL-FRISO-850', '2028-10-14');
INSERT INTO goods_receipt_lines (receipt_id, line_no, product_id, unit_id, quantity, factor, base_quantity, manufactured_date, lot_no, expiry_date)
SELECT @r, d.line_no, p.id, u.id, d.qty, pu.factor, d.qty * pu.factor, '2026-10-15', d.lot_no, d.expiry
FROM demo_lines d JOIN products p ON p.sku = d.sku JOIN units u ON u.code = 'THUNG' JOIN product_units pu ON pu.product_id = p.id AND pu.unit_id = u.id
WHERE @new;
INSERT INTO inventory_lots (warehouse_id, product_id, remaining_quantity, unit_cost, received_at, source_type, source_id, lot_no, manufactured_date, expiry_date)
SELECT @wh, l.product_id, l.base_quantity, COALESCE(p.cost_price, 0), '2026-10-20 00:00:00', 'GOODS_RECEIPT', @r, l.lot_no, l.manufactured_date, l.expiry_date
FROM goods_receipt_lines l JOIN products p ON p.id = l.product_id WHERE @new AND l.receipt_id = @r;
INSERT INTO inventory_movements (warehouse_id, product_id, unit_id, quantity, movement_type, reference_type, reference_id, created_by, created_at)
SELECT @wh, l.product_id, p.base_unit_id, l.base_quantity, 'GOODS_RECEIPT', 'GOODS_RECEIPT', @r, @actor, '2026-10-20 08:00:00'
FROM goods_receipt_lines l JOIN products p ON p.id = l.product_id WHERE @new AND l.receipt_id = @r;
INSERT INTO warehouse_inventory (warehouse_id, product_id, on_hand, reserved)
SELECT @wh, l.product_id, l.base_quantity, 0 FROM goods_receipt_lines l WHERE @new AND l.receipt_id = @r
ON DUPLICATE KEY UPDATE on_hand = on_hand + VALUES(on_hand);

-- Kho thứ 2 · NCC-NUTI
SET @wh = (SELECT id FROM warehouses WHERE status = 'ACTIVE' ORDER BY id LIMIT 1, 1);
SET @sup = (SELECT id FROM suppliers WHERE code = 'NCC-NUTI');
INSERT INTO goods_receipts (supplier_id, document_no, received_date, warehouse_id, note, status, created_by, confirmed_by, confirmed_at)
SELECT @sup, 'DEMO-K2-NCC-NUTI', '2026-10-20', @wh, 'Tồn đầu kỳ (dữ liệu demo)', 'CONFIRMED', @actor, @actor, '2026-10-20 08:00:00'
WHERE @wh IS NOT NULL AND @actor IS NOT NULL AND NOT EXISTS (SELECT 1 FROM goods_receipts WHERE supplier_id = @sup AND document_no = 'DEMO-K2-NCC-NUTI');
SET @r = (SELECT id FROM goods_receipts WHERE supplier_id = @sup AND document_no = 'DEMO-K2-NCC-NUTI');
UPDATE goods_receipts SET code = CONCAT('PN', LPAD(id, 6, '0')) WHERE id = @r AND code IS NULL;
SET @new = @r IS NOT NULL AND NOT EXISTS (SELECT 1 FROM goods_receipt_lines WHERE receipt_id = @r);
CREATE TEMPORARY TABLE IF NOT EXISTS demo_lines (line_no INT, sku VARCHAR(50), qty INT, lot_no VARCHAR(50), expiry DATE);
TRUNCATE TABLE demo_lines;
INSERT INTO demo_lines VALUES
  (1, 'NTF-STT-180-CD', 10, 'L261015-NTF-STT-180-CD', '2027-04-13'),
  (2, 'NTF-NUTIIQ-900', 10, 'L261015-NTF-NUTIIQ-900', '2028-10-14'),
  (3, 'NTF-GROWPLUS-1.5', 10, 'L261015-NTF-GROWPLUS-1.5', '2028-10-14'),
  (4, 'NTF-VARNA-850', 10, 'L261015-NTF-VARNA-850', '2028-10-14');
INSERT INTO goods_receipt_lines (receipt_id, line_no, product_id, unit_id, quantity, factor, base_quantity, manufactured_date, lot_no, expiry_date)
SELECT @r, d.line_no, p.id, u.id, d.qty, pu.factor, d.qty * pu.factor, '2026-10-15', d.lot_no, d.expiry
FROM demo_lines d JOIN products p ON p.sku = d.sku JOIN units u ON u.code = 'THUNG' JOIN product_units pu ON pu.product_id = p.id AND pu.unit_id = u.id
WHERE @new;
INSERT INTO inventory_lots (warehouse_id, product_id, remaining_quantity, unit_cost, received_at, source_type, source_id, lot_no, manufactured_date, expiry_date)
SELECT @wh, l.product_id, l.base_quantity, COALESCE(p.cost_price, 0), '2026-10-20 00:00:00', 'GOODS_RECEIPT', @r, l.lot_no, l.manufactured_date, l.expiry_date
FROM goods_receipt_lines l JOIN products p ON p.id = l.product_id WHERE @new AND l.receipt_id = @r;
INSERT INTO inventory_movements (warehouse_id, product_id, unit_id, quantity, movement_type, reference_type, reference_id, created_by, created_at)
SELECT @wh, l.product_id, p.base_unit_id, l.base_quantity, 'GOODS_RECEIPT', 'GOODS_RECEIPT', @r, @actor, '2026-10-20 08:00:00'
FROM goods_receipt_lines l JOIN products p ON p.id = l.product_id WHERE @new AND l.receipt_id = @r;
INSERT INTO warehouse_inventory (warehouse_id, product_id, on_hand, reserved)
SELECT @wh, l.product_id, l.base_quantity, 0 FROM goods_receipt_lines l WHERE @new AND l.receipt_id = @r
ON DUPLICATE KEY UPDATE on_hand = on_hand + VALUES(on_hand);

-- Kho thứ 3 · VINAMILK
SET @wh = (SELECT id FROM warehouses WHERE status = 'ACTIVE' ORDER BY id LIMIT 2, 1);
SET @sup = (SELECT id FROM suppliers WHERE code = 'VINAMILK');
INSERT INTO goods_receipts (supplier_id, document_no, received_date, warehouse_id, note, status, created_by, confirmed_by, confirmed_at)
SELECT @sup, 'DEMO-K3-VINAMILK', '2026-10-20', @wh, 'Tồn đầu kỳ (dữ liệu demo)', 'CONFIRMED', @actor, @actor, '2026-10-20 08:00:00'
WHERE @wh IS NOT NULL AND @actor IS NOT NULL AND NOT EXISTS (SELECT 1 FROM goods_receipts WHERE supplier_id = @sup AND document_no = 'DEMO-K3-VINAMILK');
SET @r = (SELECT id FROM goods_receipts WHERE supplier_id = @sup AND document_no = 'DEMO-K3-VINAMILK');
UPDATE goods_receipts SET code = CONCAT('PN', LPAD(id, 6, '0')) WHERE id = @r AND code IS NULL;
SET @new = @r IS NOT NULL AND NOT EXISTS (SELECT 1 FROM goods_receipt_lines WHERE receipt_id = @r);
CREATE TEMPORARY TABLE IF NOT EXISTS demo_lines (line_no INT, sku VARCHAR(50), qty INT, lot_no VARCHAR(50), expiry DATE);
TRUNCATE TABLE demo_lines;
INSERT INTO demo_lines VALUES
  (1, 'VNM-STT-180-CD', 5, 'L261015-VNM-STT-180-CD', '2027-04-13'),
  (2, 'VNM-STT-180-KD', 5, 'L261015-VNM-STT-180-KD', '2027-04-13'),
  (3, 'VNM-STT-110-CD', 5, 'L261015-VNM-STT-110-CD', '2027-04-13'),
  (4, 'VNM-STT-1L-CD', 5, 'L261015-VNM-STT-1L-CD', '2027-04-13'),
  (5, 'VNM-STTR-450', 5, 'L261015-VNM-STTR-450', '2026-10-25'),
  (6, 'VNM-SCA-100-CD', 5, 'L261015-VNM-SCA-100-CD', '2026-11-29'),
  (7, 'VNM-SCA-100-KD', 5, 'L261015-VNM-SCA-100-KD', '2026-11-29'),
  (8, 'VNM-SCA-100-ND', 5, 'L261015-VNM-SCA-100-ND', '2026-11-29'),
  (9, 'VNM-PROBI-65', 5, 'L261015-VNM-PROBI-65', '2026-11-29'),
  (10, 'VNM-DIELAC-900', 5, 'L261015-VNM-DIELAC-900', '2028-10-14'),
  (11, 'VNM-OPTIMUM-850', 5, 'L261015-VNM-OPTIMUM-850', '2028-10-14'),
  (12, 'VNM-SURE-900', 5, 'L261015-VNM-SURE-900', '2028-10-14'),
  (13, 'VNM-CANXI-900', 5, 'L261015-VNM-CANXI-900', '2028-10-14'),
  (14, 'VNM-ONGTHO-380', 5, 'L261015-VNM-ONGTHO-380', '2027-10-15'),
  (15, 'VNM-ONGTHO-DO-380', 5, 'L261015-VNM-ONGTHO-DO-380', '2027-10-15'),
  (16, 'VNM-NGOISAO-380', 5, 'L261015-VNM-NGOISAO-380', '2027-10-15'),
  (17, 'VNM-HAT-OCCHO-180', 5, 'L261015-VNM-HAT-OCCHO-180', '2027-07-12'),
  (18, 'VNM-KEM-VANI-450', 5, 'L261015-VNM-KEM-VANI-450', '2027-10-15');
INSERT INTO goods_receipt_lines (receipt_id, line_no, product_id, unit_id, quantity, factor, base_quantity, manufactured_date, lot_no, expiry_date)
SELECT @r, d.line_no, p.id, u.id, d.qty, pu.factor, d.qty * pu.factor, '2026-10-15', d.lot_no, d.expiry
FROM demo_lines d JOIN products p ON p.sku = d.sku JOIN units u ON u.code = 'THUNG' JOIN product_units pu ON pu.product_id = p.id AND pu.unit_id = u.id
WHERE @new;
INSERT INTO inventory_lots (warehouse_id, product_id, remaining_quantity, unit_cost, received_at, source_type, source_id, lot_no, manufactured_date, expiry_date)
SELECT @wh, l.product_id, l.base_quantity, COALESCE(p.cost_price, 0), '2026-10-20 00:00:00', 'GOODS_RECEIPT', @r, l.lot_no, l.manufactured_date, l.expiry_date
FROM goods_receipt_lines l JOIN products p ON p.id = l.product_id WHERE @new AND l.receipt_id = @r;
INSERT INTO inventory_movements (warehouse_id, product_id, unit_id, quantity, movement_type, reference_type, reference_id, created_by, created_at)
SELECT @wh, l.product_id, p.base_unit_id, l.base_quantity, 'GOODS_RECEIPT', 'GOODS_RECEIPT', @r, @actor, '2026-10-20 08:00:00'
FROM goods_receipt_lines l JOIN products p ON p.id = l.product_id WHERE @new AND l.receipt_id = @r;
INSERT INTO warehouse_inventory (warehouse_id, product_id, on_hand, reserved)
SELECT @wh, l.product_id, l.base_quantity, 0 FROM goods_receipt_lines l WHERE @new AND l.receipt_id = @r
ON DUPLICATE KEY UPDATE on_hand = on_hand + VALUES(on_hand);

-- Kho thứ 3 · NCC-TH
SET @wh = (SELECT id FROM warehouses WHERE status = 'ACTIVE' ORDER BY id LIMIT 2, 1);
SET @sup = (SELECT id FROM suppliers WHERE code = 'NCC-TH');
INSERT INTO goods_receipts (supplier_id, document_no, received_date, warehouse_id, note, status, created_by, confirmed_by, confirmed_at)
SELECT @sup, 'DEMO-K3-NCC-TH', '2026-10-20', @wh, 'Tồn đầu kỳ (dữ liệu demo)', 'CONFIRMED', @actor, @actor, '2026-10-20 08:00:00'
WHERE @wh IS NOT NULL AND @actor IS NOT NULL AND NOT EXISTS (SELECT 1 FROM goods_receipts WHERE supplier_id = @sup AND document_no = 'DEMO-K3-NCC-TH');
SET @r = (SELECT id FROM goods_receipts WHERE supplier_id = @sup AND document_no = 'DEMO-K3-NCC-TH');
UPDATE goods_receipts SET code = CONCAT('PN', LPAD(id, 6, '0')) WHERE id = @r AND code IS NULL;
SET @new = @r IS NOT NULL AND NOT EXISTS (SELECT 1 FROM goods_receipt_lines WHERE receipt_id = @r);
CREATE TEMPORARY TABLE IF NOT EXISTS demo_lines (line_no INT, sku VARCHAR(50), qty INT, lot_no VARCHAR(50), expiry DATE);
TRUNCATE TABLE demo_lines;
INSERT INTO demo_lines VALUES
  (1, 'TH-STT-180-CD', 5, 'L261015-TH-STT-180-CD', '2027-04-13'),
  (2, 'TH-STT-180-IT', 5, 'L261015-TH-STT-180-IT', '2027-04-13'),
  (3, 'TH-STT-110-CD', 5, 'L261015-TH-STT-110-CD', '2027-04-13'),
  (4, 'TH-STT-1L-CD', 5, 'L261015-TH-STT-1L-CD', '2027-04-13'),
  (5, 'TH-STTR-450', 5, 'L261015-TH-STTR-450', '2026-10-25'),
  (6, 'TH-SCA-100-CD', 5, 'L261015-TH-SCA-100-CD', '2026-11-29'),
  (7, 'TH-SCU-180', 5, 'L261015-TH-SCU-180', '2026-12-14'),
  (8, 'TH-HAT-MACCA-180', 5, 'L261015-TH-HAT-MACCA-180', '2027-07-12'),
  (9, 'TH-BO-200', 5, 'L261015-TH-BO-200', '2027-10-15');
INSERT INTO goods_receipt_lines (receipt_id, line_no, product_id, unit_id, quantity, factor, base_quantity, manufactured_date, lot_no, expiry_date)
SELECT @r, d.line_no, p.id, u.id, d.qty, pu.factor, d.qty * pu.factor, '2026-10-15', d.lot_no, d.expiry
FROM demo_lines d JOIN products p ON p.sku = d.sku JOIN units u ON u.code = 'THUNG' JOIN product_units pu ON pu.product_id = p.id AND pu.unit_id = u.id
WHERE @new;
INSERT INTO inventory_lots (warehouse_id, product_id, remaining_quantity, unit_cost, received_at, source_type, source_id, lot_no, manufactured_date, expiry_date)
SELECT @wh, l.product_id, l.base_quantity, COALESCE(p.cost_price, 0), '2026-10-20 00:00:00', 'GOODS_RECEIPT', @r, l.lot_no, l.manufactured_date, l.expiry_date
FROM goods_receipt_lines l JOIN products p ON p.id = l.product_id WHERE @new AND l.receipt_id = @r;
INSERT INTO inventory_movements (warehouse_id, product_id, unit_id, quantity, movement_type, reference_type, reference_id, created_by, created_at)
SELECT @wh, l.product_id, p.base_unit_id, l.base_quantity, 'GOODS_RECEIPT', 'GOODS_RECEIPT', @r, @actor, '2026-10-20 08:00:00'
FROM goods_receipt_lines l JOIN products p ON p.id = l.product_id WHERE @new AND l.receipt_id = @r;
INSERT INTO warehouse_inventory (warehouse_id, product_id, on_hand, reserved)
SELECT @wh, l.product_id, l.base_quantity, 0 FROM goods_receipt_lines l WHERE @new AND l.receipt_id = @r
ON DUPLICATE KEY UPDATE on_hand = on_hand + VALUES(on_hand);

-- Kho thứ 3 · NCC-FCV
SET @wh = (SELECT id FROM warehouses WHERE status = 'ACTIVE' ORDER BY id LIMIT 2, 1);
SET @sup = (SELECT id FROM suppliers WHERE code = 'NCC-FCV');
INSERT INTO goods_receipts (supplier_id, document_no, received_date, warehouse_id, note, status, created_by, confirmed_by, confirmed_at)
SELECT @sup, 'DEMO-K3-NCC-FCV', '2026-10-20', @wh, 'Tồn đầu kỳ (dữ liệu demo)', 'CONFIRMED', @actor, @actor, '2026-10-20 08:00:00'
WHERE @wh IS NOT NULL AND @actor IS NOT NULL AND NOT EXISTS (SELECT 1 FROM goods_receipts WHERE supplier_id = @sup AND document_no = 'DEMO-K3-NCC-FCV');
SET @r = (SELECT id FROM goods_receipts WHERE supplier_id = @sup AND document_no = 'DEMO-K3-NCC-FCV');
UPDATE goods_receipts SET code = CONCAT('PN', LPAD(id, 6, '0')) WHERE id = @r AND code IS NULL;
SET @new = @r IS NOT NULL AND NOT EXISTS (SELECT 1 FROM goods_receipt_lines WHERE receipt_id = @r);
CREATE TEMPORARY TABLE IF NOT EXISTS demo_lines (line_no INT, sku VARCHAR(50), qty INT, lot_no VARCHAR(50), expiry DATE);
TRUNCATE TABLE demo_lines;
INSERT INTO demo_lines VALUES
  (1, 'DL-STT-180-CD', 5, 'L261015-DL-STT-180-CD', '2027-04-13'),
  (2, 'DL-STT-110-CD', 5, 'L261015-DL-STT-110-CD', '2027-04-13'),
  (3, 'DL-YOMOST-170', 5, 'L261015-DL-YOMOST-170', '2026-12-14'),
  (4, 'DL-FRISO-850', 5, 'L261015-DL-FRISO-850', '2028-10-14');
INSERT INTO goods_receipt_lines (receipt_id, line_no, product_id, unit_id, quantity, factor, base_quantity, manufactured_date, lot_no, expiry_date)
SELECT @r, d.line_no, p.id, u.id, d.qty, pu.factor, d.qty * pu.factor, '2026-10-15', d.lot_no, d.expiry
FROM demo_lines d JOIN products p ON p.sku = d.sku JOIN units u ON u.code = 'THUNG' JOIN product_units pu ON pu.product_id = p.id AND pu.unit_id = u.id
WHERE @new;
INSERT INTO inventory_lots (warehouse_id, product_id, remaining_quantity, unit_cost, received_at, source_type, source_id, lot_no, manufactured_date, expiry_date)
SELECT @wh, l.product_id, l.base_quantity, COALESCE(p.cost_price, 0), '2026-10-20 00:00:00', 'GOODS_RECEIPT', @r, l.lot_no, l.manufactured_date, l.expiry_date
FROM goods_receipt_lines l JOIN products p ON p.id = l.product_id WHERE @new AND l.receipt_id = @r;
INSERT INTO inventory_movements (warehouse_id, product_id, unit_id, quantity, movement_type, reference_type, reference_id, created_by, created_at)
SELECT @wh, l.product_id, p.base_unit_id, l.base_quantity, 'GOODS_RECEIPT', 'GOODS_RECEIPT', @r, @actor, '2026-10-20 08:00:00'
FROM goods_receipt_lines l JOIN products p ON p.id = l.product_id WHERE @new AND l.receipt_id = @r;
INSERT INTO warehouse_inventory (warehouse_id, product_id, on_hand, reserved)
SELECT @wh, l.product_id, l.base_quantity, 0 FROM goods_receipt_lines l WHERE @new AND l.receipt_id = @r
ON DUPLICATE KEY UPDATE on_hand = on_hand + VALUES(on_hand);

-- Kho thứ 3 · NCC-NUTI
SET @wh = (SELECT id FROM warehouses WHERE status = 'ACTIVE' ORDER BY id LIMIT 2, 1);
SET @sup = (SELECT id FROM suppliers WHERE code = 'NCC-NUTI');
INSERT INTO goods_receipts (supplier_id, document_no, received_date, warehouse_id, note, status, created_by, confirmed_by, confirmed_at)
SELECT @sup, 'DEMO-K3-NCC-NUTI', '2026-10-20', @wh, 'Tồn đầu kỳ (dữ liệu demo)', 'CONFIRMED', @actor, @actor, '2026-10-20 08:00:00'
WHERE @wh IS NOT NULL AND @actor IS NOT NULL AND NOT EXISTS (SELECT 1 FROM goods_receipts WHERE supplier_id = @sup AND document_no = 'DEMO-K3-NCC-NUTI');
SET @r = (SELECT id FROM goods_receipts WHERE supplier_id = @sup AND document_no = 'DEMO-K3-NCC-NUTI');
UPDATE goods_receipts SET code = CONCAT('PN', LPAD(id, 6, '0')) WHERE id = @r AND code IS NULL;
SET @new = @r IS NOT NULL AND NOT EXISTS (SELECT 1 FROM goods_receipt_lines WHERE receipt_id = @r);
CREATE TEMPORARY TABLE IF NOT EXISTS demo_lines (line_no INT, sku VARCHAR(50), qty INT, lot_no VARCHAR(50), expiry DATE);
TRUNCATE TABLE demo_lines;
INSERT INTO demo_lines VALUES
  (1, 'NTF-STT-180-CD', 5, 'L261015-NTF-STT-180-CD', '2027-04-13'),
  (2, 'NTF-NUTIIQ-900', 5, 'L261015-NTF-NUTIIQ-900', '2028-10-14'),
  (3, 'NTF-GROWPLUS-1.5', 5, 'L261015-NTF-GROWPLUS-1.5', '2028-10-14'),
  (4, 'NTF-VARNA-850', 5, 'L261015-NTF-VARNA-850', '2028-10-14');
INSERT INTO goods_receipt_lines (receipt_id, line_no, product_id, unit_id, quantity, factor, base_quantity, manufactured_date, lot_no, expiry_date)
SELECT @r, d.line_no, p.id, u.id, d.qty, pu.factor, d.qty * pu.factor, '2026-10-15', d.lot_no, d.expiry
FROM demo_lines d JOIN products p ON p.sku = d.sku JOIN units u ON u.code = 'THUNG' JOIN product_units pu ON pu.product_id = p.id AND pu.unit_id = u.id
WHERE @new;
INSERT INTO inventory_lots (warehouse_id, product_id, remaining_quantity, unit_cost, received_at, source_type, source_id, lot_no, manufactured_date, expiry_date)
SELECT @wh, l.product_id, l.base_quantity, COALESCE(p.cost_price, 0), '2026-10-20 00:00:00', 'GOODS_RECEIPT', @r, l.lot_no, l.manufactured_date, l.expiry_date
FROM goods_receipt_lines l JOIN products p ON p.id = l.product_id WHERE @new AND l.receipt_id = @r;
INSERT INTO inventory_movements (warehouse_id, product_id, unit_id, quantity, movement_type, reference_type, reference_id, created_by, created_at)
SELECT @wh, l.product_id, p.base_unit_id, l.base_quantity, 'GOODS_RECEIPT', 'GOODS_RECEIPT', @r, @actor, '2026-10-20 08:00:00'
FROM goods_receipt_lines l JOIN products p ON p.id = l.product_id WHERE @new AND l.receipt_id = @r;
INSERT INTO warehouse_inventory (warehouse_id, product_id, on_hand, reserved)
SELECT @wh, l.product_id, l.base_quantity, 0 FROM goods_receipt_lines l WHERE @new AND l.receipt_id = @r
ON DUPLICATE KEY UPDATE on_hand = on_hand + VALUES(on_hand);

DROP TEMPORARY TABLE IF EXISTS demo_lines;

-- Tài khoản demo cho 6 vai trò (mật khẩu Demo@2026, không bắt đổi). Chỉ thêm khi đã có admin: ứng dụng chỉ tự tạo
-- admin lúc bảng users còn trống, thêm demo trước lần khởi động đầu thì sẽ không bao giờ có admin.
INSERT IGNORE INTO users (username, email, full_name, password_hash, status, must_change_password)
SELECT * FROM (
  SELECT 'demo_quanly' AS username, 'demo_quanly@oms.local' AS email, 'Demo Quản lý kinh doanh' AS full_name, '$2a$10$bGBhVjc9OUCnd901MXybbOb/wP0xA8fGIkD0.uRUbmDUVB2eJDE56' AS password_hash, 'ACTIVE' AS status, 0 AS must_change_password UNION ALL
  SELECT 'demo_nvkd', 'demo_nvkd@oms.local', 'Demo Nhân viên kinh doanh', '$2a$10$bGBhVjc9OUCnd901MXybbOb/wP0xA8fGIkD0.uRUbmDUVB2eJDE56', 'ACTIVE', 0 UNION ALL
  SELECT 'demo_ketoan', 'demo_ketoan@oms.local', 'Demo Kế toán công nợ', '$2a$10$bGBhVjc9OUCnd901MXybbOb/wP0xA8fGIkD0.uRUbmDUVB2eJDE56', 'ACTIVE', 0 UNION ALL
  SELECT 'demo_kho', 'demo_kho@oms.local', 'Demo Thủ kho', '$2a$10$bGBhVjc9OUCnd901MXybbOb/wP0xA8fGIkD0.uRUbmDUVB2eJDE56', 'ACTIVE', 0 UNION ALL
  SELECT 'demo_qlkho', 'demo_qlkho@oms.local', 'Demo Quản lý kho', '$2a$10$bGBhVjc9OUCnd901MXybbOb/wP0xA8fGIkD0.uRUbmDUVB2eJDE56', 'ACTIVE', 0 UNION ALL
  SELECT 'demo_daily', 'demo_daily@oms.local', 'Demo Đại lý Minh Anh', '$2a$10$bGBhVjc9OUCnd901MXybbOb/wP0xA8fGIkD0.uRUbmDUVB2eJDE56', 'ACTIVE', 0
) demo WHERE @actor IS NOT NULL;
INSERT IGNORE INTO user_roles (user_id, role_code)
SELECT id, 'SALES_MANAGER' FROM users WHERE username = 'demo_quanly' UNION ALL
SELECT id, 'SALES_REP' FROM users WHERE username = 'demo_nvkd' UNION ALL
SELECT id, 'ACCOUNTANT' FROM users WHERE username = 'demo_ketoan' UNION ALL
SELECT id, 'WAREHOUSE' FROM users WHERE username = 'demo_kho' UNION ALL
SELECT id, 'WH_MANAGER' FROM users WHERE username = 'demo_qlkho' UNION ALL
SELECT id, 'CUSTOMER' FROM users WHERE username = 'demo_daily';
-- Thủ kho và quản lý kho phụ trách kho đầu tiên; nhân viên kinh doanh phụ trách địa bàn và hai đại lý đầu
INSERT IGNORE INTO user_warehouses (user_id, warehouse_id)
SELECT u.id, (SELECT MIN(id) FROM warehouses WHERE status = 'ACTIVE') FROM users u WHERE u.username IN ('demo_kho', 'demo_qlkho');
INSERT IGNORE INTO user_areas (user_id, area_id)
SELECT u.id, c.area_id FROM users u JOIN customers c ON c.code IN ('DL001', 'DL002') WHERE u.username = 'demo_nvkd' AND c.area_id IS NOT NULL;
UPDATE customers SET sales_rep_id = (SELECT id FROM users WHERE username = 'demo_nvkd')
WHERE code IN ('DL001', 'DL002') AND sales_rep_id IS NULL;
-- Đại lý demo đăng nhập cổng đặt hàng thay cho DL001 (mỗi đại lý một tài khoản)
INSERT IGNORE INTO customer_accounts (customer_id, user_id)
SELECT c.id, u.id FROM customers c JOIN users u ON u.username = 'demo_daily' WHERE c.code = 'DL001';
