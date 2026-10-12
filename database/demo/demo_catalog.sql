-- Dữ liệu DEMO: danh mục nhóm hàng nhiều cấp và sản phẩm FMCG để thử giao diện, tìm kiếm, lọc, phân trang.
-- Nguồn: file này được sinh từ danh sách có cấu trúc; chạy lại nhiều lần an toàn (chỉ thêm cái chưa có, không đổi gì đã có).
-- Giữ nguyên nhóm hàng và sản phẩm hiện có. Không phải migration: chỉ chạy khi cần dữ liệu thử.
-- Gỡ: database/demo/demo_catalog_remove.sql.
-- Chạy:  Get-Content database\demo\demo_catalog.sql -Raw -Encoding UTF8 | mysql -uoms_app --default-character-set=utf8mb4
USE oms;

-- 1. Đơn vị tính mới (đơn vị cũ giữ nguyên)
INSERT INTO units (code, name) SELECT 'TUYP', 'Tuýp' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM units WHERE code = 'TUYP');
INSERT INTO units (code, name) SELECT 'TUI', 'Túi' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM units WHERE code = 'TUI');
INSERT INTO units (code, name) SELECT 'BICH', 'Bịch' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM units WHERE code = 'BICH');
INSERT INTO units (code, name) SELECT 'CUON', 'Cuộn' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM units WHERE code = 'CUON');
INSERT INTO units (code, name) SELECT 'LO', 'Lọ' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM units WHERE code = 'LO');
INSERT INTO units (code, name) SELECT 'THANH', 'Thanh' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM units WHERE code = 'THANH');
INSERT INTO units (code, name) SELECT 'BAO', 'Bao' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM units WHERE code = 'BAO');
INSERT INTO units (code, name) SELECT 'CAN', 'Can' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM units WHERE code = 'CAN');
INSERT INTO units (code, name) SELECT 'VI', 'Vỉ' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM units WHERE code = 'VI');
INSERT INTO units (code, name) SELECT 'LY', 'Ly' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM units WHERE code = 'LY');

-- 2. Nhóm hàng (cấp 1 → cấp 2 → cấp 3), bỏ qua nhóm đã có cùng tên cùng cha
INSERT INTO product_categories (parent_id, name) SELECT NULL, 'Đồ uống' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM product_categories WHERE parent_id IS NULL AND name = 'Đồ uống');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Nước giải khát' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Đồ uống' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Nước giải khát');
INSERT INTO product_categories (parent_id, name) SELECT p2.id, 'Nước ngọt có ga' FROM product_categories p2 JOIN product_categories p1 ON p1.id = p2.parent_id WHERE p1.parent_id IS NULL AND p1.name = 'Đồ uống' AND p2.name = 'Nước giải khát' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p2.id AND c.name = 'Nước ngọt có ga');
INSERT INTO product_categories (parent_id, name) SELECT p2.id, 'Nước tăng lực' FROM product_categories p2 JOIN product_categories p1 ON p1.id = p2.parent_id WHERE p1.parent_id IS NULL AND p1.name = 'Đồ uống' AND p2.name = 'Nước giải khát' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p2.id AND c.name = 'Nước tăng lực');
INSERT INTO product_categories (parent_id, name) SELECT p2.id, 'Nước trái cây' FROM product_categories p2 JOIN product_categories p1 ON p1.id = p2.parent_id WHERE p1.parent_id IS NULL AND p1.name = 'Đồ uống' AND p2.name = 'Nước giải khát' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p2.id AND c.name = 'Nước trái cây');
INSERT INTO product_categories (parent_id, name) SELECT p2.id, 'Trà đóng chai' FROM product_categories p2 JOIN product_categories p1 ON p1.id = p2.parent_id WHERE p1.parent_id IS NULL AND p1.name = 'Đồ uống' AND p2.name = 'Nước giải khát' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p2.id AND c.name = 'Trà đóng chai');
INSERT INTO product_categories (parent_id, name) SELECT p2.id, 'Cà phê đóng chai' FROM product_categories p2 JOIN product_categories p1 ON p1.id = p2.parent_id WHERE p1.parent_id IS NULL AND p1.name = 'Đồ uống' AND p2.name = 'Nước giải khát' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p2.id AND c.name = 'Cà phê đóng chai');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Bia' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Đồ uống' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Bia');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Nước suối' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Đồ uống' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Nước suối');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Nước uống thể thao' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Đồ uống' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Nước uống thể thao');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Nước dừa và nước yến' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Đồ uống' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Nước dừa và nước yến');
INSERT INTO product_categories (parent_id, name) SELECT NULL, 'Thực phẩm khô' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM product_categories WHERE parent_id IS NULL AND name = 'Thực phẩm khô');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Mì, phở ăn liền' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Thực phẩm khô' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Mì, phở ăn liền');
INSERT INTO product_categories (parent_id, name) SELECT p2.id, 'Mì gói' FROM product_categories p2 JOIN product_categories p1 ON p1.id = p2.parent_id WHERE p1.parent_id IS NULL AND p1.name = 'Thực phẩm khô' AND p2.name = 'Mì, phở ăn liền' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p2.id AND c.name = 'Mì gói');
INSERT INTO product_categories (parent_id, name) SELECT p2.id, 'Mì ly và mì tô' FROM product_categories p2 JOIN product_categories p1 ON p1.id = p2.parent_id WHERE p1.parent_id IS NULL AND p1.name = 'Thực phẩm khô' AND p2.name = 'Mì, phở ăn liền' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p2.id AND c.name = 'Mì ly và mì tô');
INSERT INTO product_categories (parent_id, name) SELECT p2.id, 'Phở và hủ tiếu ăn liền' FROM product_categories p2 JOIN product_categories p1 ON p1.id = p2.parent_id WHERE p1.parent_id IS NULL AND p1.name = 'Thực phẩm khô' AND p2.name = 'Mì, phở ăn liền' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p2.id AND c.name = 'Phở và hủ tiếu ăn liền');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Cháo ăn liền' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Thực phẩm khô' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Cháo ăn liền');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Bún, miến, phở khô' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Thực phẩm khô' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Bún, miến, phở khô');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Đồ hộp' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Thực phẩm khô' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Đồ hộp');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Ngũ cốc và bột' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Thực phẩm khô' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Ngũ cốc và bột');
INSERT INTO product_categories (parent_id, name) SELECT NULL, 'Bánh kẹo' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM product_categories WHERE parent_id IS NULL AND name = 'Bánh kẹo');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Bánh quy' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Bánh kẹo' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Bánh quy');
INSERT INTO product_categories (parent_id, name) SELECT p2.id, 'Bánh quy bơ và sữa' FROM product_categories p2 JOIN product_categories p1 ON p1.id = p2.parent_id WHERE p1.parent_id IS NULL AND p1.name = 'Bánh kẹo' AND p2.name = 'Bánh quy' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p2.id AND c.name = 'Bánh quy bơ và sữa');
INSERT INTO product_categories (parent_id, name) SELECT p2.id, 'Bánh quy kem' FROM product_categories p2 JOIN product_categories p1 ON p1.id = p2.parent_id WHERE p1.parent_id IS NULL AND p1.name = 'Bánh kẹo' AND p2.name = 'Bánh quy' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p2.id AND c.name = 'Bánh quy kem');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Bánh cracker' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Bánh kẹo' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Bánh cracker');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Bánh xốp' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Bánh kẹo' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Bánh xốp');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Kẹo cứng' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Bánh kẹo' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Kẹo cứng');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Kẹo mềm' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Bánh kẹo' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Kẹo mềm');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Chocolate' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Bánh kẹo' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Chocolate');
INSERT INTO product_categories (parent_id, name) SELECT p2.id, 'Chocolate thanh' FROM product_categories p2 JOIN product_categories p1 ON p1.id = p2.parent_id WHERE p1.parent_id IS NULL AND p1.name = 'Bánh kẹo' AND p2.name = 'Chocolate' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p2.id AND c.name = 'Chocolate thanh');
INSERT INTO product_categories (parent_id, name) SELECT p2.id, 'Chocolate viên' FROM product_categories p2 JOIN product_categories p1 ON p1.id = p2.parent_id WHERE p1.parent_id IS NULL AND p1.name = 'Bánh kẹo' AND p2.name = 'Chocolate' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p2.id AND c.name = 'Chocolate viên');
INSERT INTO product_categories (parent_id, name) SELECT NULL, 'Sữa và sản phẩm từ sữa' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM product_categories WHERE parent_id IS NULL AND name = 'Sữa và sản phẩm từ sữa');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Sữa tươi' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Sữa và sản phẩm từ sữa' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Sữa tươi');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Sữa tiệt trùng' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Sữa và sản phẩm từ sữa' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Sữa tiệt trùng');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Sữa chua' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Sữa và sản phẩm từ sữa' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Sữa chua');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Sữa đặc' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Sữa và sản phẩm từ sữa' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Sữa đặc');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Nước uống dinh dưỡng' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Sữa và sản phẩm từ sữa' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Nước uống dinh dưỡng');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Sữa bột' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Sữa và sản phẩm từ sữa' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Sữa bột');
INSERT INTO product_categories (parent_id, name) SELECT NULL, 'Hóa mỹ phẩm' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM product_categories WHERE parent_id IS NULL AND name = 'Hóa mỹ phẩm');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Dầu gội' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Hóa mỹ phẩm' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Dầu gội');
INSERT INTO product_categories (parent_id, name) SELECT p2.id, 'Dầu gội nam' FROM product_categories p2 JOIN product_categories p1 ON p1.id = p2.parent_id WHERE p1.parent_id IS NULL AND p1.name = 'Hóa mỹ phẩm' AND p2.name = 'Dầu gội' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p2.id AND c.name = 'Dầu gội nam');
INSERT INTO product_categories (parent_id, name) SELECT p2.id, 'Dầu gội nữ' FROM product_categories p2 JOIN product_categories p1 ON p1.id = p2.parent_id WHERE p1.parent_id IS NULL AND p1.name = 'Hóa mỹ phẩm' AND p2.name = 'Dầu gội' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p2.id AND c.name = 'Dầu gội nữ');
INSERT INTO product_categories (parent_id, name) SELECT p2.id, 'Dầu gội trị gàu' FROM product_categories p2 JOIN product_categories p1 ON p1.id = p2.parent_id WHERE p1.parent_id IS NULL AND p1.name = 'Hóa mỹ phẩm' AND p2.name = 'Dầu gội' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p2.id AND c.name = 'Dầu gội trị gàu');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Dầu xả' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Hóa mỹ phẩm' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Dầu xả');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Sữa tắm' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Hóa mỹ phẩm' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Sữa tắm');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Xà phòng' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Hóa mỹ phẩm' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Xà phòng');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Kem đánh răng' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Hóa mỹ phẩm' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Kem đánh răng');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Nước súc miệng' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Hóa mỹ phẩm' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Nước súc miệng');
INSERT INTO product_categories (parent_id, name) SELECT NULL, 'Chăm sóc gia đình' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM product_categories WHERE parent_id IS NULL AND name = 'Chăm sóc gia đình');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Nước giặt' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Chăm sóc gia đình' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Nước giặt');
INSERT INTO product_categories (parent_id, name) SELECT p2.id, 'Nước giặt cửa trước' FROM product_categories p2 JOIN product_categories p1 ON p1.id = p2.parent_id WHERE p1.parent_id IS NULL AND p1.name = 'Chăm sóc gia đình' AND p2.name = 'Nước giặt' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p2.id AND c.name = 'Nước giặt cửa trước');
INSERT INTO product_categories (parent_id, name) SELECT p2.id, 'Nước giặt cửa trên' FROM product_categories p2 JOIN product_categories p1 ON p1.id = p2.parent_id WHERE p1.parent_id IS NULL AND p1.name = 'Chăm sóc gia đình' AND p2.name = 'Nước giặt' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p2.id AND c.name = 'Nước giặt cửa trên');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Nước xả vải' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Chăm sóc gia đình' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Nước xả vải');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Nước rửa chén' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Chăm sóc gia đình' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Nước rửa chén');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Nước lau sàn' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Chăm sóc gia đình' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Nước lau sàn');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Chất tẩy rửa' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Chăm sóc gia đình' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Chất tẩy rửa');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Túi rác' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Chăm sóc gia đình' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Túi rác');
INSERT INTO product_categories (parent_id, name) SELECT NULL, 'Giấy và sản phẩm vệ sinh' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM product_categories WHERE parent_id IS NULL AND name = 'Giấy và sản phẩm vệ sinh');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Giấy vệ sinh' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Giấy và sản phẩm vệ sinh' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Giấy vệ sinh');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Khăn giấy' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Giấy và sản phẩm vệ sinh' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Khăn giấy');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Khăn ướt' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Giấy và sản phẩm vệ sinh' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Khăn ướt');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Giấy ăn' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Giấy và sản phẩm vệ sinh' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Giấy ăn');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Băng vệ sinh' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Giấy và sản phẩm vệ sinh' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Băng vệ sinh');
INSERT INTO product_categories (parent_id, name) SELECT NULL, 'Đồ dùng cá nhân' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM product_categories WHERE parent_id IS NULL AND name = 'Đồ dùng cá nhân');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Bàn chải đánh răng' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Đồ dùng cá nhân' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Bàn chải đánh răng');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Dao cạo' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Đồ dùng cá nhân' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Dao cạo');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Khăn tắm và khăn mặt' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Đồ dùng cá nhân' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Khăn tắm và khăn mặt');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Vệ sinh cá nhân' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Đồ dùng cá nhân' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Vệ sinh cá nhân');
INSERT INTO product_categories (parent_id, name) SELECT NULL, 'Gia vị và nguyên liệu nấu ăn' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM product_categories WHERE parent_id IS NULL AND name = 'Gia vị và nguyên liệu nấu ăn');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Nước mắm' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Gia vị và nguyên liệu nấu ăn' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Nước mắm');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Nước tương' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Gia vị và nguyên liệu nấu ăn' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Nước tương');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Dầu ăn' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Gia vị và nguyên liệu nấu ăn' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Dầu ăn');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Muối' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Gia vị và nguyên liệu nấu ăn' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Muối');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Đường' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Gia vị và nguyên liệu nấu ăn' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Đường');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Bột ngọt' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Gia vị và nguyên liệu nấu ăn' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Bột ngọt');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Hạt nêm' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Gia vị và nguyên liệu nấu ăn' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Hạt nêm');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Tương ớt' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Gia vị và nguyên liệu nấu ăn' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Tương ớt');
INSERT INTO product_categories (parent_id, name) SELECT NULL, 'Đồ ăn nhẹ' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM product_categories WHERE parent_id IS NULL AND name = 'Đồ ăn nhẹ');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Snack khoai và bắp' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Đồ ăn nhẹ' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Snack khoai và bắp');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Rong biển' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Đồ ăn nhẹ' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Rong biển');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Hạt các loại' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Đồ ăn nhẹ' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Hạt các loại');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Khô chế biến' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Đồ ăn nhẹ' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Khô chế biến');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Đậu phộng' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Đồ ăn nhẹ' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Đậu phộng');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Bắp rang' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Đồ ăn nhẹ' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Bắp rang');
INSERT INTO product_categories (parent_id, name) SELECT NULL, 'Cà phê và trà' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM product_categories WHERE parent_id IS NULL AND name = 'Cà phê và trà');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Cà phê hòa tan' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Cà phê và trà' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Cà phê hòa tan');
INSERT INTO product_categories (parent_id, name) SELECT p2.id, 'Cà phê 3 trong 1' FROM product_categories p2 JOIN product_categories p1 ON p1.id = p2.parent_id WHERE p1.parent_id IS NULL AND p1.name = 'Cà phê và trà' AND p2.name = 'Cà phê hòa tan' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p2.id AND c.name = 'Cà phê 3 trong 1');
INSERT INTO product_categories (parent_id, name) SELECT p2.id, 'Cà phê đen và cappuccino' FROM product_categories p2 JOIN product_categories p1 ON p1.id = p2.parent_id WHERE p1.parent_id IS NULL AND p1.name = 'Cà phê và trà' AND p2.name = 'Cà phê hòa tan' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p2.id AND c.name = 'Cà phê đen và cappuccino');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Cà phê rang xay' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Cà phê và trà' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Cà phê rang xay');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Trà túi lọc' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Cà phê và trà' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Trà túi lọc');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Trà khô' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Cà phê và trà' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Trà khô');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Trà hòa tan' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Cà phê và trà' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Trà hòa tan');
INSERT INTO product_categories (parent_id, name) SELECT NULL, 'Đồ dùng nhà bếp' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM product_categories WHERE parent_id IS NULL AND name = 'Đồ dùng nhà bếp');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Màng bọc thực phẩm' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Đồ dùng nhà bếp' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Màng bọc thực phẩm');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Giấy bạc' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Đồ dùng nhà bếp' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Giấy bạc');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Túi zip' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Đồ dùng nhà bếp' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Túi zip');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Miếng rửa chén' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Đồ dùng nhà bếp' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Miếng rửa chén');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Găng tay' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Đồ dùng nhà bếp' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Găng tay');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Dụng cụ vệ sinh' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Đồ dùng nhà bếp' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Dụng cụ vệ sinh');
INSERT INTO product_categories (parent_id, name) SELECT NULL, 'Hàng tiêu dùng khác' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM product_categories WHERE parent_id IS NULL AND name = 'Hàng tiêu dùng khác');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Pin' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Hàng tiêu dùng khác' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Pin');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Bật lửa' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Hàng tiêu dùng khác' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Bật lửa');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Khẩu trang' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Hàng tiêu dùng khác' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Khẩu trang');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Đồ dùng tiện ích' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Hàng tiêu dùng khác' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Đồ dùng tiện ích');
INSERT INTO product_categories (parent_id, name) SELECT NULL, 'Thực phẩm đông lạnh' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM product_categories WHERE parent_id IS NULL AND name = 'Thực phẩm đông lạnh');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Xúc xích' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Thực phẩm đông lạnh' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Xúc xích');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Chả cá' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Thực phẩm đông lạnh' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Chả cá');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Viên thả lẩu' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Thực phẩm đông lạnh' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Viên thả lẩu');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Thực phẩm chế biến sẵn' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Thực phẩm đông lạnh' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Thực phẩm chế biến sẵn');
INSERT INTO product_categories (parent_id, name) SELECT NULL, 'Đồ dùng cho trẻ em' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM product_categories WHERE parent_id IS NULL AND name = 'Đồ dùng cho trẻ em');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Tã, bỉm' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Đồ dùng cho trẻ em' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Tã, bỉm');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Khăn ướt trẻ em' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Đồ dùng cho trẻ em' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Khăn ướt trẻ em');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Sữa trẻ em' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Đồ dùng cho trẻ em' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Sữa trẻ em');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Đồ ăn dặm' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Đồ dùng cho trẻ em' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Đồ ăn dặm');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Chăm sóc trẻ em' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Đồ dùng cho trẻ em' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Chăm sóc trẻ em');
INSERT INTO product_categories (parent_id, name) SELECT NULL, 'Thức ăn thú cưng' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM product_categories WHERE parent_id IS NULL AND name = 'Thức ăn thú cưng');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Thức ăn cho chó' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Thức ăn thú cưng' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Thức ăn cho chó');
INSERT INTO product_categories (parent_id, name) SELECT p.id, 'Thức ăn cho mèo' FROM product_categories p WHERE p.parent_id IS NULL AND p.name = 'Thức ăn thú cưng' AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = 'Thức ăn cho mèo');

-- 3. Sản phẩm: nạp vào bảng tạm rồi gắn nhóm hàng và đơn vị theo tên
DROP TEMPORARY TABLE IF EXISTS tmp_demo_products;
CREATE TEMPORARY TABLE tmp_demo_products (
  sku VARCHAR(50) NOT NULL, name VARCHAR(255) NOT NULL,
  cat1 VARCHAR(150) NOT NULL, cat2 VARCHAR(150) NULL, cat3 VARCHAR(150) NULL,
  base_unit VARCHAR(20) NOT NULL, packing_spec VARCHAR(255) NULL, cost_price DECIMAL(15,2) NOT NULL,
  status VARCHAR(20) NOT NULL,
  conv1_unit VARCHAR(20) NULL, conv1_factor DECIMAL(12,4) NULL, conv2_unit VARCHAR(20) NULL, conv2_factor DECIMAL(12,4) NULL
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO tmp_demo_products (sku, name, cat1, cat2, cat3, base_unit, packing_spec, cost_price, status, conv1_unit, conv1_factor, conv2_unit, conv2_factor) VALUES
-- Đồ uống/Nước giải khát/Nước ngọt có ga
('7UP-330', '7UP lon 330ml', 'Đồ uống', 'Nước giải khát', 'Nước ngọt có ga', 'LON', '24 lon/thùng', 6900, 'ACTIVE', 'LOC', 6, 'THUNG', 24),
('SPRITE-330', 'Sprite lon 330ml', 'Đồ uống', 'Nước giải khát', 'Nước ngọt có ga', 'LON', '24 lon/thùng', 7000, 'ACTIVE', 'LOC', 6, 'THUNG', 24),
('FANTA-CAM-330', 'Fanta cam lon 330ml', 'Đồ uống', 'Nước giải khát', 'Nước ngọt có ga', 'LON', '24 lon/thùng', 6900, 'ACTIVE', 'LOC', 6, 'THUNG', 24),
('MIRINDA-CAM-330', 'Mirinda cam lon 330ml', 'Đồ uống', 'Nước giải khát', 'Nước ngọt có ga', 'LON', '24 lon/thùng', 6800, 'DISCONTINUED', 'LOC', 6, 'THUNG', 24),
('MTNDEW-330', 'Mountain Dew lon 330ml', 'Đồ uống', 'Nước giải khát', 'Nước ngọt có ga', 'LON', '24 lon/thùng', 7000, 'ACTIVE', 'LOC', 6, 'THUNG', 24),
('SCHWEPPES-330', 'Schweppes soda lon 330ml', 'Đồ uống', 'Nước giải khát', 'Nước ngọt có ga', 'LON', '24 lon/thùng', 7600, 'ACTIVE', 'LOC', 6, 'THUNG', 24),
('COCA-ZERO-330', 'Coca-Cola Zero lon 330ml', 'Đồ uống', 'Nước giải khát', 'Nước ngọt có ga', 'LON', '24 lon/thùng', 7400, 'ACTIVE', 'LOC', 6, 'THUNG', 24),
('PEPSI-ZERO-330', 'Pepsi không calo lon 330ml', 'Đồ uống', 'Nước giải khát', 'Nước ngọt có ga', 'LON', '24 lon/thùng', 7200, 'ACTIVE', 'LOC', 6, 'THUNG', 24),
('COCA-390', 'Coca-Cola chai 390ml', 'Đồ uống', 'Nước giải khát', 'Nước ngọt có ga', 'CHAI', '24 chai/thùng', 5800, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('COCA-1.5L', 'Coca-Cola chai 1,5 lít', 'Đồ uống', 'Nước giải khát', 'Nước ngọt có ga', 'CHAI', '12 chai/thùng', 17800, 'ACTIVE', 'THUNG', 12, NULL, NULL),
('PEPSI-1.5L', 'Pepsi chai 1,5 lít', 'Đồ uống', 'Nước giải khát', 'Nước ngọt có ga', 'CHAI', '12 chai/thùng', 17500, 'ACTIVE', 'THUNG', 12, NULL, NULL),
-- Đồ uống/Nước giải khát/Nước tăng lực
('STING-DAU-330', 'Sting dâu chai 330ml', 'Đồ uống', 'Nước giải khát', 'Nước tăng lực', 'CHAI', '24 chai/thùng', 6200, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('STING-VANG-330', 'Sting vàng chai 330ml', 'Đồ uống', 'Nước giải khát', 'Nước tăng lực', 'CHAI', '24 chai/thùng', 6200, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('NUMBER1-330', 'Number 1 chai 330ml', 'Đồ uống', 'Nước giải khát', 'Nước tăng lực', 'CHAI', '24 chai/thùng', 6500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('WARRIOR-330', 'Warrior dâu chai 330ml', 'Đồ uống', 'Nước giải khát', 'Nước tăng lực', 'CHAI', '24 chai/thùng', 6300, 'DISCONTINUED', 'THUNG', 24, NULL, NULL),
('MONSTER-355', 'Monster Energy lon 355ml', 'Đồ uống', 'Nước giải khát', 'Nước tăng lực', 'LON', '24 lon/thùng', 19500, 'ACTIVE', 'LOC', 6, 'THUNG', 24),
('BOHUC-250', 'Bò húc Thái Lan lon 250ml', 'Đồ uống', 'Nước giải khát', 'Nước tăng lực', 'LON', '24 lon/thùng', 12800, 'ACTIVE', 'LOC', 6, 'THUNG', 24),
-- Đồ uống/Nước giải khát/Nước trái cây
('TWISTER-CAM-455', 'Tropicana Twister cam chai 455ml', 'Đồ uống', 'Nước giải khát', 'Nước trái cây', 'CHAI', '24 chai/thùng', 9800, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('NUTRI-CAM-297', 'Nutriboost cam chai 297ml', 'Đồ uống', 'Nước giải khát', 'Nước trái cây', 'CHAI', '24 chai/thùng', 8800, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('VFRESH-CAM-1L', 'Nước ép Vfresh cam hộp 1 lít', 'Đồ uống', 'Nước giải khát', 'Nước trái cây', 'HOP', '12 hộp/thùng', 24500, 'ACTIVE', 'THUNG', 12, NULL, NULL),
('DELMONTE-TAO-1L', 'Nước ép Del Monte táo hộp 1 lít', 'Đồ uống', 'Nước giải khát', 'Nước trái cây', 'HOP', '12 hộp/thùng', 26500, 'ACTIVE', 'THUNG', 12, NULL, NULL),
('ZICO-DUA-330', 'Nước dừa Zico hộp 330ml', 'Đồ uống', 'Nước giải khát', 'Nước trái cây', 'HOP', '24 hộp/thùng', 12500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('TEPPY-CAM-327', 'Nước cam ép Teppy chai 327ml', 'Đồ uống', 'Nước giải khát', 'Nước trái cây', 'CHAI', '24 chai/thùng', 6900, 'ACTIVE', 'THUNG', 24, NULL, NULL),
-- Đồ uống/Nước giải khát/Trà đóng chai
('C2-XANH-455', 'C2 trà xanh chai 455ml', 'Đồ uống', 'Nước giải khát', 'Trà đóng chai', 'CHAI', '24 chai/thùng', 5400, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('TEAPLUS-OLONG-455', 'Trà Ô Long Tea+ chai 455ml', 'Đồ uống', 'Nước giải khát', 'Trà đóng chai', 'CHAI', '24 chai/thùng', 6900, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('O-DO-XANH-455', 'Trà xanh Không Độ chai 455ml', 'Đồ uống', 'Nước giải khát', 'Trà đóng chai', 'CHAI', '24 chai/thùng', 5200, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('LIPTON-CHANH-455', 'Trà Lipton chanh chai 455ml', 'Đồ uống', 'Nước giải khát', 'Trà đóng chai', 'CHAI', '24 chai/thùng', 5500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('DRTHANH-500', 'Trà thanh nhiệt Dr Thanh chai 500ml', 'Đồ uống', 'Nước giải khát', 'Trà đóng chai', 'CHAI', '24 chai/thùng', 6000, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('WONDER-BIDAO-310', 'Trà bí đao Wonderfarm hộp 310ml', 'Đồ uống', 'Nước giải khát', 'Trà đóng chai', 'HOP', '24 hộp/thùng', 6400, 'ACTIVE', 'THUNG', 24, NULL, NULL),
-- Đồ uống/Nước giải khát/Cà phê đóng chai
('HIGHLANDS-235', 'Cà phê Highlands chai 235ml', 'Đồ uống', 'Nước giải khát', 'Cà phê đóng chai', 'CHAI', '24 chai/thùng', 14500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('NESCAFE-LON-170', 'Cà phê sữa Nescafé lon 170ml', 'Đồ uống', 'Nước giải khát', 'Cà phê đóng chai', 'LON', '24 lon/thùng', 9800, 'ACTIVE', 'LOC', 6, 'THUNG', 24),
('TRUECOFFEE-225', 'Cà phê TH True Coffee chai 225ml', 'Đồ uống', 'Nước giải khát', 'Cà phê đóng chai', 'CHAI', '24 chai/thùng', 11800, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('BIRDY-170', 'Cà phê Birdy lon 170ml', 'Đồ uống', 'Nước giải khát', 'Cà phê đóng chai', 'LON', '24 lon/thùng', 9500, 'ACTIVE', 'LOC', 6, 'THUNG', 24),
-- Đồ uống/Bia
('HEINEKEN-330', 'Heineken lon 330ml', 'Đồ uống', 'Bia', NULL, 'LON', '24 lon/thùng', 18500, 'ACTIVE', 'LOC', 6, 'THUNG', 24),
('HEINEKEN-SILVER-330', 'Heineken Silver lon 330ml', 'Đồ uống', 'Bia', NULL, 'LON', '24 lon/thùng', 18800, 'ACTIVE', 'LOC', 6, 'THUNG', 24),
('333-330', 'Bia 333 lon 330ml', 'Đồ uống', 'Bia', NULL, 'LON', '24 lon/thùng', 9200, 'ACTIVE', 'LOC', 6, 'THUNG', 24),
('HANOI-330', 'Bia Hà Nội lon 330ml', 'Đồ uống', 'Bia', NULL, 'LON', '24 lon/thùng', 9800, 'ACTIVE', 'LOC', 6, 'THUNG', 24),
('SAIGON-LAGER-330', 'Bia Sài Gòn Lager lon 330ml', 'Đồ uống', 'Bia', NULL, 'LON', '24 lon/thùng', 10400, 'ACTIVE', 'LOC', 6, 'THUNG', 24),
('SAIGON-SPECIAL-330', 'Bia Sài Gòn Special lon 330ml', 'Đồ uống', 'Bia', NULL, 'LON', '24 lon/thùng', 12800, 'ACTIVE', 'LOC', 6, 'THUNG', 24),
('SAPPORO-330', 'Bia Sapporo Premium lon 330ml', 'Đồ uống', 'Bia', NULL, 'LON', '24 lon/thùng', 14500, 'ACTIVE', 'LOC', 6, 'THUNG', 24),
('HUDA-330', 'Bia Huda lon 330ml', 'Đồ uống', 'Bia', NULL, 'LON', '24 lon/thùng', 9000, 'ACTIVE', 'LOC', 6, 'THUNG', 24),
('LARUE-330', 'Bia Larue lon 330ml', 'Đồ uống', 'Bia', NULL, 'LON', '24 lon/thùng', 9300, 'DISCONTINUED', 'LOC', 6, 'THUNG', 24),
('BIVINA-330', 'Bia Bivina lon 330ml', 'Đồ uống', 'Bia', NULL, 'LON', '24 lon/thùng', 9400, 'ACTIVE', 'LOC', 6, 'THUNG', 24),
('TIGER-CRYSTAL-330', 'Tiger Crystal lon 330ml', 'Đồ uống', 'Bia', NULL, 'LON', '24 lon/thùng', 15200, 'ACTIVE', 'LOC', 6, 'THUNG', 24),
('TIGER-CHAI-330', 'Bia Tiger chai 330ml', 'Đồ uống', 'Bia', NULL, 'CHAI', '24 chai/thùng', 14000, 'ACTIVE', 'THUNG', 24, NULL, NULL),
-- Đồ uống/Nước suối
('AQUAFINA-500', 'Aquafina chai 500ml', 'Đồ uống', 'Nước suối', NULL, 'CHAI', '24 chai/thùng', 3200, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('AQUAFINA-1.5L', 'Aquafina chai 1,5 lít', 'Đồ uống', 'Nước suối', NULL, 'CHAI', '12 chai/thùng', 5200, 'ACTIVE', 'THUNG', 12, NULL, NULL),
('LAVIE-500', 'Lavie chai 500ml', 'Đồ uống', 'Nước suối', NULL, 'CHAI', '24 chai/thùng', 3000, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('LAVIE-1.5L', 'Lavie chai 1,5 lít', 'Đồ uống', 'Nước suối', NULL, 'CHAI', '12 chai/thùng', 4800, 'ACTIVE', 'THUNG', 12, NULL, NULL),
('DASANI-500', 'Dasani chai 500ml', 'Đồ uống', 'Nước suối', NULL, 'CHAI', '24 chai/thùng', 3100, 'DISCONTINUED', 'THUNG', 24, NULL, NULL),
('VINHHAO-500', 'Nước khoáng Vĩnh Hảo chai 500ml', 'Đồ uống', 'Nước suối', NULL, 'CHAI', '24 chai/thùng', 3400, 'ACTIVE', 'THUNG', 24, NULL, NULL),
-- Đồ uống/Nước uống thể thao
('REVIVE-CHANHMUOI-500', 'Revive chanh muối chai 500ml', 'Đồ uống', 'Nước uống thể thao', NULL, 'CHAI', '24 chai/thùng', 8800, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('REVIVE-500', 'Revive vị tự nhiên chai 500ml', 'Đồ uống', 'Nước uống thể thao', NULL, 'CHAI', '24 chai/thùng', 8800, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('POCARI-350', 'Pocari Sweat chai 350ml', 'Đồ uống', 'Nước uống thể thao', NULL, 'CHAI', '24 chai/thùng', 14500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('AQUARIUS-390', 'Aquarius chai 390ml', 'Đồ uống', 'Nước uống thể thao', NULL, 'CHAI', '24 chai/thùng', 8200, 'ACTIVE', 'THUNG', 24, NULL, NULL),
-- Đồ uống/Nước dừa và nước yến
('COCOXIM-330', 'Nước dừa Cocoxim hộp 330ml', 'Đồ uống', 'Nước dừa và nước yến', NULL, 'HOP', '24 hộp/thùng', 10500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('COCOLAND-330', 'Nước dừa Cocoland hộp 330ml', 'Đồ uống', 'Nước dừa và nước yến', NULL, 'HOP', '24 hộp/thùng', 11500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('SANEST-190', 'Nước yến Sanest lon 190ml', 'Đồ uống', 'Nước dừa và nước yến', NULL, 'LON', '30 lon/thùng', 15500, 'ACTIVE', 'LOC', 6, 'THUNG', 30),
-- Thực phẩm khô/Mì, phở ăn liền/Mì gói
('KOKOMI-TOM', 'Mì Kokomi tôm chua cay 65g', 'Thực phẩm khô', 'Mì, phở ăn liền', 'Mì gói', 'GOI', '30 gói/thùng', 3500, 'ACTIVE', 'THUNG', 30, NULL, NULL),
('OMACHI-SUON', 'Mì Omachi sườn hầm ngũ quả', 'Thực phẩm khô', 'Mì, phở ăn liền', 'Mì gói', 'GOI', '30 gói/thùng', 5600, 'ACTIVE', 'THUNG', 30, NULL, NULL),
('OMACHI-XOTBO', 'Mì Omachi xốt bò hầm', 'Thực phẩm khô', 'Mì, phở ăn liền', 'Mì gói', 'GOI', '30 gói/thùng', 5600, 'ACTIVE', 'THUNG', 30, NULL, NULL),
('GAUDO-GA', 'Mì Gấu Đỏ gà', 'Thực phẩm khô', 'Mì, phở ăn liền', 'Mì gói', 'GOI', '30 gói/thùng', 2700, 'ACTIVE', 'THUNG', 30, NULL, NULL),
('GAUDO-TOM', 'Mì Gấu Đỏ tôm', 'Thực phẩm khô', 'Mì, phở ăn liền', 'Mì gói', 'GOI', '30 gói/thùng', 2700, 'ACTIVE', 'THUNG', 30, NULL, NULL),
('3MIEN-GA', 'Mì 3 Miền gà', 'Thực phẩm khô', 'Mì, phở ăn liền', 'Mì gói', 'GOI', '30 gói/thùng', 2500, 'ACTIVE', 'THUNG', 30, NULL, NULL),
('HAOHAO-SATE', 'Mì Hảo Hảo sa tế hành tím', 'Thực phẩm khô', 'Mì, phở ăn liền', 'Mì gói', 'GOI', '30 gói/thùng', 3300, 'ACTIVE', 'THUNG', 30, NULL, NULL),
('HAOHAO-GA', 'Mì Hảo Hảo gà', 'Thực phẩm khô', 'Mì, phở ăn liền', 'Mì gói', 'GOI', '30 gói/thùng', 3300, 'ACTIVE', 'THUNG', 30, NULL, NULL),
('MODERN-LAUTHAI', 'Mì Modern lẩu Thái', 'Thực phẩm khô', 'Mì, phở ăn liền', 'Mì gói', 'GOI', '30 gói/thùng', 3900, 'ACTIVE', 'THUNG', 30, NULL, NULL),
('SIUKAY-SUON', 'Mì Siukay sườn heo', 'Thực phẩm khô', 'Mì, phở ăn liền', 'Mì gói', 'GOI', '30 gói/thùng', 4000, 'ACTIVE', 'THUNG', 30, NULL, NULL),
('CUNGDINH-HAI', 'Mì Cung Đình hải sản', 'Thực phẩm khô', 'Mì, phở ăn liền', 'Mì gói', 'GOI', '30 gói/thùng', 5500, 'ACTIVE', 'THUNG', 30, NULL, NULL),
('MAMA-TOMYUM', 'Mì Mama Tomyum', 'Thực phẩm khô', 'Mì, phở ăn liền', 'Mì gói', 'GOI', '30 gói/thùng', 5200, 'DISCONTINUED', 'THUNG', 30, NULL, NULL),
('VIFON-BO', 'Mì Vifon bò hầm', 'Thực phẩm khô', 'Mì, phở ăn liền', 'Mì gói', 'GOI', '30 gói/thùng', 3600, 'ACTIVE', 'THUNG', 30, NULL, NULL),
-- Thực phẩm khô/Mì, phở ăn liền/Mì ly và mì tô
('HAOHAO-LY-TOM', 'Mì ly Hảo Hảo tôm chua cay 67g', 'Thực phẩm khô', 'Mì, phở ăn liền', 'Mì ly và mì tô', 'LY', '24 ly/thùng', 6200, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('OMACHI-LY-SUON', 'Mì ly Omachi sườn hầm', 'Thực phẩm khô', 'Mì, phở ăn liền', 'Mì ly và mì tô', 'LY', '24 ly/thùng', 8800, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('KOKOMI-LY-TOM', 'Mì ly Kokomi tôm chua cay', 'Thực phẩm khô', 'Mì, phở ăn liền', 'Mì ly và mì tô', 'LY', '24 ly/thùng', 5800, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('CUNGDINH-LY', 'Mì ly Cung Đình hải sản', 'Thực phẩm khô', 'Mì, phở ăn liền', 'Mì ly và mì tô', 'LY', '24 ly/thùng', 7200, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('HAOHAO-TO-TOM', 'Mì tô Hảo Hảo tôm chua cay', 'Thực phẩm khô', 'Mì, phở ăn liền', 'Mì ly và mì tô', 'CAI', '24 cái/thùng', 8500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
-- Thực phẩm khô/Mì, phở ăn liền/Phở và hủ tiếu ăn liền
('DENHAT-BOHAM', 'Phở Đệ Nhất bò hầm', 'Thực phẩm khô', 'Mì, phở ăn liền', 'Phở và hủ tiếu ăn liền', 'GOI', '30 gói/thùng', 6200, 'ACTIVE', 'THUNG', 30, NULL, NULL),
('VIFON-PHOBO', 'Phở Vifon bò tái', 'Thực phẩm khô', 'Mì, phở ăn liền', 'Phở và hủ tiếu ăn liền', 'GOI', '30 gói/thùng', 6000, 'ACTIVE', 'THUNG', 30, NULL, NULL),
('VIFON-HUTIEU', 'Hủ tiếu Nam Vang Vifon', 'Thực phẩm khô', 'Mì, phở ăn liền', 'Phở và hủ tiếu ăn liền', 'GOI', '30 gói/thùng', 6500, 'ACTIVE', 'THUNG', 30, NULL, NULL),
('GAUDO-PHOGA', 'Phở Gấu Đỏ gà', 'Thực phẩm khô', 'Mì, phở ăn liền', 'Phở và hủ tiếu ăn liền', 'GOI', '30 gói/thùng', 5200, 'ACTIVE', 'THUNG', 30, NULL, NULL),
-- Thực phẩm khô/Cháo ăn liền
('GAUDO-CHAO-THIT', 'Cháo Gấu Đỏ thịt bằm', 'Thực phẩm khô', 'Cháo ăn liền', NULL, 'GOI', '30 gói/thùng', 4800, 'ACTIVE', 'THUNG', 30, NULL, NULL),
('GAUDO-CHAO-GA', 'Cháo Gấu Đỏ gà', 'Thực phẩm khô', 'Cháo ăn liền', NULL, 'GOI', '30 gói/thùng', 4800, 'ACTIVE', 'THUNG', 30, NULL, NULL),
('CAYTHI-CHAO-GA', 'Cháo Cây Thị gà', 'Thực phẩm khô', 'Cháo ăn liền', NULL, 'GOI', '30 gói/thùng', 5500, 'ACTIVE', 'THUNG', 30, NULL, NULL),
('CAYTHI-CHAO-SUON', 'Cháo Cây Thị sườn', 'Thực phẩm khô', 'Cháo ăn liền', NULL, 'GOI', '30 gói/thùng', 5500, 'ACTIVE', 'THUNG', 30, NULL, NULL),
('VIFON-CHAO-THIT', 'Cháo Vifon thịt bằm', 'Thực phẩm khô', 'Cháo ăn liền', NULL, 'GOI', '30 gói/thùng', 4600, 'DISCONTINUED', 'THUNG', 30, NULL, NULL),
-- Thực phẩm khô/Bún, miến, phở khô
('SAFOCO-BUNGAO-300', 'Bún gạo khô Safoco 300g', 'Thực phẩm khô', 'Bún, miến, phở khô', NULL, 'GOI', '20 gói/thùng', 9500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
('PHUHUONG-MIENDONG-200', 'Miến dong Phú Hương 200g', 'Thực phẩm khô', 'Bún, miến, phở khô', NULL, 'GOI', '20 gói/thùng', 21000, 'ACTIVE', 'THUNG', 20, NULL, NULL),
('BICHCHI-PHOKHO-500', 'Phở khô Bích Chi 500g', 'Thực phẩm khô', 'Bún, miến, phở khô', NULL, 'GOI', '20 gói/thùng', 18500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
('BICHCHI-HUTIEU-500', 'Hủ tiếu khô Bích Chi 500g', 'Thực phẩm khô', 'Bún, miến, phở khô', NULL, 'GOI', '20 gói/thùng', 19000, 'ACTIVE', 'THUNG', 20, NULL, NULL),
('SAFOCO-NUI-400', 'Nui ống Safoco 400g', 'Thực phẩm khô', 'Bún, miến, phở khô', NULL, 'GOI', '20 gói/thùng', 12500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
-- Thực phẩm khô/Đồ hộp
('HALONG-CAMOI-155', 'Cá mòi sốt cà Hạ Long hộp 155g', 'Thực phẩm khô', 'Đồ hộp', NULL, 'HOP', '24 hộp/thùng', 14500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('HALONG-CANGU-140', 'Cá ngừ ngâm dầu Hạ Long hộp 140g', 'Thực phẩm khô', 'Đồ hộp', NULL, 'HOP', '24 hộp/thùng', 22000, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('COTDEN-PATE-170', 'Pate Cột Đèn Hải Phòng hộp 170g', 'Thực phẩm khô', 'Đồ hộp', NULL, 'HOP', '24 hộp/thùng', 19500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('VISSAN-THITHAM-150', 'Thịt heo hầm Vissan hộp 150g', 'Thực phẩm khô', 'Đồ hộp', NULL, 'HOP', '24 hộp/thùng', 21500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('SPAM-340', 'Thịt hộp Spam 340g', 'Thực phẩm khô', 'Đồ hộp', NULL, 'HOP', '24 hộp/thùng', 62000, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('DOHAN-DAUHALAN-400', 'Đậu Hà Lan Dohan hộp 400g', 'Thực phẩm khô', 'Đồ hộp', NULL, 'HOP', '24 hộp/thùng', 18000, 'DISCONTINUED', 'THUNG', 24, NULL, NULL),
-- Thực phẩm khô/Ngũ cốc và bột
('MEIZAN-BOTMI-1KG', 'Bột mì đa dụng Meizan 1kg', 'Thực phẩm khô', 'Ngũ cốc và bột', NULL, 'BICH', '20 bịch/thùng', 21500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
('TAIKY-BOTCHIEN-150', 'Bột chiên giòn Tài Ký 150g', 'Thực phẩm khô', 'Ngũ cốc và bột', NULL, 'GOI', '30 gói/thùng', 9500, 'ACTIVE', 'THUNG', 30, NULL, NULL),
('TAIKY-BOTNANG-400', 'Bột năng Tài Ký 400g', 'Thực phẩm khô', 'Ngũ cốc và bột', NULL, 'BICH', '20 bịch/thùng', 12500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
('QUAKER-YENMACH-400', 'Yến mạch Quaker hộp 400g', 'Thực phẩm khô', 'Ngũ cốc và bột', NULL, 'HOP', '12 hộp/thùng', 48000, 'ACTIVE', 'THUNG', 12, NULL, NULL),
('NESVITA-NGUCOC-400', 'Ngũ cốc Nesvita bịch 400g', 'Thực phẩm khô', 'Ngũ cốc và bột', NULL, 'BICH', '12 bịch/thùng', 58000, 'ACTIVE', 'THUNG', 12, NULL, NULL),
-- Bánh kẹo/Bánh quy/Bánh quy bơ và sữa
('COSY-MARIE-144', 'Bánh quy Cosy Marie 144g', 'Bánh kẹo', 'Bánh quy', 'Bánh quy bơ và sữa', 'GOI', '24 gói/thùng', 12500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('COSY-BO-300', 'Bánh quy bơ Cosy 300g', 'Bánh kẹo', 'Bánh quy', 'Bánh quy bơ và sữa', 'GOI', '24 gói/thùng', 24500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('DANISA-BO-454', 'Bánh quy bơ Danisa hộp thiếc 454g', 'Bánh kẹo', 'Bánh quy', 'Bánh quy bơ và sữa', 'HOP', '12 hộp/thùng', 98000, 'ACTIVE', 'THUNG', 12, NULL, NULL),
('AFC-VANG-100', 'Bánh AFC vị lúa mì 100g', 'Bánh kẹo', 'Bánh quy', 'Bánh quy bơ và sữa', 'GOI', '24 gói/thùng', 9500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('KINHDO-BOSUA-138', 'Bánh quy Kinh Đô bơ sữa 138g', 'Bánh kẹo', 'Bánh quy', 'Bánh quy bơ và sữa', 'GOI', '24 gói/thùng', 13500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
-- Bánh kẹo/Bánh quy/Bánh quy kem
('OREO-137', 'Bánh Oreo kem vani 137g', 'Bánh kẹo', 'Bánh quy', 'Bánh quy kem', 'GOI', '24 gói/thùng', 15500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('OREO-DAUTAY-137', 'Bánh Oreo kem dâu 137g', 'Bánh kẹo', 'Bánh quy', 'Bánh quy kem', 'GOI', '24 gói/thùng', 15500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('CHOCOPIE-12', 'Bánh Choco Pie hộp 12 cái', 'Bánh kẹo', 'Bánh quy', 'Bánh quy kem', 'HOP', '12 hộp/thùng', 52000, 'ACTIVE', 'THUNG', 12, NULL, NULL),
('COSY-KEM-300', 'Bánh quy kem Cosy 300g', 'Bánh kẹo', 'Bánh quy', 'Bánh quy kem', 'GOI', '24 gói/thùng', 26500, 'DISCONTINUED', 'THUNG', 24, NULL, NULL),
-- Bánh kẹo/Bánh cracker
('AFC-CRACKER-200', 'Bánh cracker AFC 200g', 'Bánh kẹo', 'Bánh cracker', NULL, 'GOI', '24 gói/thùng', 17500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('RITZ-CRACKER-100', 'Bánh cracker Ritz 100g', 'Bánh kẹo', 'Bánh cracker', NULL, 'GOI', '24 gói/thùng', 18500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('COSY-CRACKER-300', 'Bánh cracker Cosy 300g', 'Bánh kẹo', 'Bánh cracker', NULL, 'GOI', '24 gói/thùng', 24000, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('HAIHA-CRACKER-130', 'Bánh cracker Hải Hà 130g', 'Bánh kẹo', 'Bánh cracker', NULL, 'GOI', '24 gói/thùng', 12500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
-- Bánh kẹo/Bánh xốp
('WAFER-HANHNHAN-150', 'Bánh xốp Hanh Nhân phủ socola 150g', 'Bánh kẹo', 'Bánh xốp', NULL, 'GOI', '30 gói/thùng', 19000, 'ACTIVE', 'THUNG', 30, NULL, NULL),
('JOJO-XOP-100', 'Bánh xốp Jojo kem sữa 100g', 'Bánh kẹo', 'Bánh xốp', NULL, 'GOI', '30 gói/thùng', 11500, 'ACTIVE', 'THUNG', 30, NULL, NULL),
('TIPO-XOP-100', 'Bánh xốp Tipo phô mai 100g', 'Bánh kẹo', 'Bánh xốp', NULL, 'GOI', '30 gói/thùng', 12500, 'ACTIVE', 'THUNG', 30, NULL, NULL),
('GOLDENWAFER-135', 'Bánh xốp Golden Wafer cuộn 135g', 'Bánh kẹo', 'Bánh xốp', NULL, 'GOI', '30 gói/thùng', 14500, 'ACTIVE', 'THUNG', 30, NULL, NULL),
-- Bánh kẹo/Kẹo cứng
('ALPENLIEBE-CARAMEN-120', 'Kẹo Alpenliebe caramen 120g', 'Bánh kẹo', 'Kẹo cứng', NULL, 'BICH', '20 bịch/thùng', 14000, 'ACTIVE', 'THUNG', 20, NULL, NULL),
('ALPENLIEBE-CAM-120', 'Kẹo Alpenliebe cam 120g', 'Bánh kẹo', 'Kẹo cứng', NULL, 'BICH', '20 bịch/thùng', 14000, 'ACTIVE', 'THUNG', 20, NULL, NULL),
('HAIHA-KEODUA-200', 'Kẹo dừa Hải Hà 200g', 'Bánh kẹo', 'Kẹo cứng', NULL, 'BICH', '20 bịch/thùng', 15500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
('CHUPA-CHUPS-12', 'Kẹo mút Chupa Chups túi 12 cây', 'Bánh kẹo', 'Kẹo cứng', NULL, 'TUI', '20 túi/thùng', 22000, 'ACTIVE', 'THUNG', 20, NULL, NULL),
('SWING-BAHAHA-120', 'Kẹo bạc hà Doublemint 100g', 'Bánh kẹo', 'Kẹo cứng', NULL, 'BICH', '20 bịch/thùng', 13500, 'DISCONTINUED', 'THUNG', 20, NULL, NULL),
-- Bánh kẹo/Kẹo mềm
('MENTOS-BACHA-37', 'Kẹo Mentos bạc hà cuộn 37g', 'Bánh kẹo', 'Kẹo mềm', NULL, 'CUON', '60 cuộn/thùng', 6500, 'ACTIVE', 'THUNG', 60, NULL, NULL),
('MENTOS-TRAICAY-37', 'Kẹo Mentos trái cây cuộn 37g', 'Bánh kẹo', 'Kẹo mềm', NULL, 'CUON', '60 cuộn/thùng', 6500, 'ACTIVE', 'THUNG', 60, NULL, NULL),
('HARIBO-GAUVANG-80', 'Kẹo dẻo Haribo gấu vàng 80g', 'Bánh kẹo', 'Kẹo mềm', NULL, 'TUI', '24 túi/thùng', 24500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('CHEWY-SUA-100', 'Kẹo sữa dẻo Chewy 100g', 'Bánh kẹo', 'Kẹo mềm', NULL, 'BICH', '20 bịch/thùng', 12500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
('MAMEE-KEODEO-90', 'Kẹo dẻo trái cây Mamee 90g', 'Bánh kẹo', 'Kẹo mềm', NULL, 'BICH', '20 bịch/thùng', 11000, 'ACTIVE', 'THUNG', 20, NULL, NULL),
-- Bánh kẹo/Chocolate/Chocolate thanh
('KITKAT-4F-35', 'Chocolate KitKat 4 thanh 35g', 'Bánh kẹo', 'Chocolate', 'Chocolate thanh', 'THANH', '48 thanh/thùng', 9500, 'ACTIVE', 'THUNG', 48, NULL, NULL),
('SNICKERS-51', 'Chocolate Snickers thanh 51g', 'Bánh kẹo', 'Chocolate', 'Chocolate thanh', 'THANH', '48 thanh/thùng', 14500, 'ACTIVE', 'THUNG', 48, NULL, NULL),
('MARS-51', 'Chocolate Mars thanh 51g', 'Bánh kẹo', 'Chocolate', 'Chocolate thanh', 'THANH', '48 thanh/thùng', 14500, 'ACTIVE', 'THUNG', 48, NULL, NULL),
('TWIX-50', 'Chocolate Twix thanh 50g', 'Bánh kẹo', 'Chocolate', 'Chocolate thanh', 'THANH', '48 thanh/thùng', 14500, 'ACTIVE', 'THUNG', 48, NULL, NULL),
('LOTTE-XYLI-40', 'Chocolate Lotte Ghana 40g', 'Bánh kẹo', 'Chocolate', 'Chocolate thanh', 'THANH', '48 thanh/thùng', 12500, 'ACTIVE', 'THUNG', 48, NULL, NULL),
-- Bánh kẹo/Chocolate/Chocolate viên
('MM-DAUPHONG-40', 'Chocolate M&M''s đậu phộng 40g', 'Bánh kẹo', 'Chocolate', 'Chocolate viên', 'GOI', '24 gói/thùng', 15500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('MM-SOCOLA-40', 'Chocolate M&M''s sữa 40g', 'Bánh kẹo', 'Chocolate', 'Chocolate viên', 'GOI', '24 gói/thùng', 15500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('MALTESERS-37', 'Chocolate Maltesers 37g', 'Bánh kẹo', 'Chocolate', 'Chocolate viên', 'GOI', '24 gói/thùng', 18500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('FERRERO-T16', 'Chocolate Ferrero Rocher hộp 16 viên', 'Bánh kẹo', 'Chocolate', 'Chocolate viên', 'HOP', '12 hộp/thùng', 168000, 'ACTIVE', 'THUNG', 12, NULL, NULL),
-- Sữa và sản phẩm từ sữa/Sữa tươi
('VINAMILK-TUOI-ITDUONG-180', 'Sữa tươi Vinamilk 100% ít đường 180ml', 'Sữa và sản phẩm từ sữa', 'Sữa tươi', NULL, 'HOP', '48 hộp/thùng', 6200, 'ACTIVE', 'LOC', 4, 'THUNG', 48),
('VINAMILK-TUOI-KDUONG-180', 'Sữa tươi Vinamilk 100% không đường 180ml', 'Sữa và sản phẩm từ sữa', 'Sữa tươi', NULL, 'HOP', '48 hộp/thùng', 6200, 'ACTIVE', 'LOC', 4, 'THUNG', 48),
('TH-TRUEMILK-180', 'Sữa tươi TH True Milk 180ml', 'Sữa và sản phẩm từ sữa', 'Sữa tươi', NULL, 'HOP', '48 hộp/thùng', 7200, 'ACTIVE', 'LOC', 4, 'THUNG', 48),
('TH-TRUEMILK-ITDUONG-180', 'Sữa tươi TH True Milk ít đường 180ml', 'Sữa và sản phẩm từ sữa', 'Sữa tươi', NULL, 'HOP', '48 hộp/thùng', 7200, 'ACTIVE', 'LOC', 4, 'THUNG', 48),
('DALAT-TUOI-180', 'Sữa tươi Dalat Milk 180ml', 'Sữa và sản phẩm từ sữa', 'Sữa tươi', NULL, 'HOP', '48 hộp/thùng', 6800, 'ACTIVE', 'LOC', 4, 'THUNG', 48),
('VINAMILK-TUOI-1L', 'Sữa tươi Vinamilk 100% hộp 1 lít', 'Sữa và sản phẩm từ sữa', 'Sữa tươi', NULL, 'HOP', '12 hộp/thùng', 31500, 'ACTIVE', 'THUNG', 12, NULL, NULL),
-- Sữa và sản phẩm từ sữa/Sữa tiệt trùng
('DUTCHLADY-DAU-180', 'Sữa tiệt trùng Dutch Lady dâu 180ml', 'Sữa và sản phẩm từ sữa', 'Sữa tiệt trùng', NULL, 'HOP', '48 hộp/thùng', 5600, 'ACTIVE', 'LOC', 4, 'THUNG', 48),
('DUTCHLADY-SOCOLA-180', 'Sữa tiệt trùng Dutch Lady socola 180ml', 'Sữa và sản phẩm từ sữa', 'Sữa tiệt trùng', NULL, 'HOP', '48 hộp/thùng', 5600, 'ACTIVE', 'LOC', 4, 'THUNG', 48),
('VINAMILK-FLEX-180', 'Sữa tiệt trùng Vinamilk Flex 180ml', 'Sữa và sản phẩm từ sữa', 'Sữa tiệt trùng', NULL, 'HOP', '48 hộp/thùng', 5400, 'ACTIVE', 'LOC', 4, 'THUNG', 48),
('DELIMIL-DAU-180', 'Sữa tiệt trùng Fami dâu 180ml', 'Sữa và sản phẩm từ sữa', 'Sữa tiệt trùng', NULL, 'HOP', '48 hộp/thùng', 5200, 'ACTIVE', 'LOC', 4, 'THUNG', 48),
('MILO-HOP-180', 'Sữa Milo hộp 180ml', 'Sữa và sản phẩm từ sữa', 'Sữa tiệt trùng', NULL, 'HOP', '48 hộp/thùng', 6900, 'DISCONTINUED', 'LOC', 4, 'THUNG', 48),
-- Sữa và sản phẩm từ sữa/Sữa chua
('VINAMILK-SC-CODUONG-100', 'Sữa chua ăn Vinamilk có đường hộp 100g', 'Sữa và sản phẩm từ sữa', 'Sữa chua', NULL, 'HOP', '48 hộp/thùng', 5400, 'ACTIVE', 'LOC', 4, 'THUNG', 48),
('VINAMILK-SC-TRAICAY-100', 'Sữa chua trái cây Vinamilk hộp 100g', 'Sữa và sản phẩm từ sữa', 'Sữa chua', NULL, 'HOP', '48 hộp/thùng', 6200, 'ACTIVE', 'LOC', 4, 'THUNG', 48),
('TH-SC-100', 'Sữa chua TH True Yogurt hộp 100g', 'Sữa và sản phẩm từ sữa', 'Sữa chua', NULL, 'HOP', '48 hộp/thùng', 7200, 'ACTIVE', 'LOC', 4, 'THUNG', 48),
('PROBI-65', 'Sữa chua uống Probi chai 65ml', 'Sữa và sản phẩm từ sữa', 'Sữa chua', NULL, 'CHAI', '60 chai/thùng', 3800, 'ACTIVE', 'LOC', 5, 'THUNG', 60),
('YAKULT-65', 'Sữa chua uống Yakult chai 65ml', 'Sữa và sản phẩm từ sữa', 'Sữa chua', NULL, 'CHAI', '50 chai/thùng', 3500, 'ACTIVE', 'LOC', 5, 'THUNG', 50),
-- Sữa và sản phẩm từ sữa/Sữa đặc
('ONGTHO-380', 'Sữa đặc Ông Thọ hộp 380g', 'Sữa và sản phẩm từ sữa', 'Sữa đặc', NULL, 'HOP', '48 hộp/thùng', 24500, 'ACTIVE', 'THUNG', 48, NULL, NULL),
('NGOISAO-380', 'Sữa đặc Ngôi Sao Phương Nam hộp 380g', 'Sữa và sản phẩm từ sữa', 'Sữa đặc', NULL, 'HOP', '48 hộp/thùng', 23500, 'ACTIVE', 'THUNG', 48, NULL, NULL),
('VINAMILK-DACTUOI-380', 'Sữa đặc Vinamilk hộp 380g', 'Sữa và sản phẩm từ sữa', 'Sữa đặc', NULL, 'HOP', '48 hộp/thùng', 25500, 'ACTIVE', 'THUNG', 48, NULL, NULL),
('TUI-ONGTHO-545', 'Sữa đặc Ông Thọ túi 545g', 'Sữa và sản phẩm từ sữa', 'Sữa đặc', NULL, 'TUI', '24 túi/thùng', 32500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
-- Sữa và sản phẩm từ sữa/Nước uống dinh dưỡng
('ENSURE-LON-237', 'Sữa Ensure Gold lon 237ml', 'Sữa và sản phẩm từ sữa', 'Nước uống dinh dưỡng', NULL, 'LON', '24 lon/thùng', 38500, 'ACTIVE', 'LOC', 6, 'THUNG', 24),
('VINAMILK-SUSU-180', 'Sữa Susu Vinamilk 180ml', 'Sữa và sản phẩm từ sữa', 'Nước uống dinh dưỡng', NULL, 'HOP', '48 hộp/thùng', 5200, 'ACTIVE', 'LOC', 4, 'THUNG', 48),
('SUSU-DAU-110', 'Sữa Susu dâu 110ml', 'Sữa và sản phẩm từ sữa', 'Nước uống dinh dưỡng', NULL, 'HOP', '48 hộp/thùng', 4300, 'ACTIVE', 'LOC', 4, 'THUNG', 48),
('SUPERNUT-180', 'Sữa hạt Vinamilk Super Nut 180ml', 'Sữa và sản phẩm từ sữa', 'Nước uống dinh dưỡng', NULL, 'HOP', '48 hộp/thùng', 8500, 'ACTIVE', 'LOC', 4, 'THUNG', 48),
('OVALTINE-180', 'Thức uống lúa mạch Ovaltine 180ml', 'Sữa và sản phẩm từ sữa', 'Nước uống dinh dưỡng', NULL, 'HOP', '48 hộp/thùng', 6400, 'ACTIVE', 'LOC', 4, 'THUNG', 48),
-- Sữa và sản phẩm từ sữa/Sữa bột
('ENSURE-GOLD-850', 'Sữa bột Ensure Gold 850g', 'Sữa và sản phẩm từ sữa', 'Sữa bột', NULL, 'HOP', '6 hộp/thùng', 790000, 'ACTIVE', 'THUNG', 6, NULL, NULL),
('SURE-PREVENT-900', 'Sữa bột Sure Prevent 900g', 'Sữa và sản phẩm từ sữa', 'Sữa bột', NULL, 'HOP', '6 hộp/thùng', 560000, 'ACTIVE', 'THUNG', 6, NULL, NULL),
('VINAMILK-CANXI-400', 'Sữa bột Vinamilk Canxi 400g', 'Sữa và sản phẩm từ sữa', 'Sữa bột', NULL, 'HOP', '12 hộp/thùng', 125000, 'ACTIVE', 'THUNG', 12, NULL, NULL),
('DIELAC-ALPHA-900', 'Sữa bột Dielac Alpha Gold 900g', 'Sữa và sản phẩm từ sữa', 'Sữa bột', NULL, 'HOP', '6 hộp/thùng', 245000, 'ACTIVE', 'THUNG', 6, NULL, NULL),
-- Hóa mỹ phẩm/Dầu gội/Dầu gội nam
('CLEARMEN-650', 'Dầu gội Clear Men sạch gàu 650g', 'Hóa mỹ phẩm', 'Dầu gội', 'Dầu gội nam', 'CHAI', '12 chai/thùng', 135000, 'ACTIVE', 'THUNG', 12, NULL, NULL),
('ROMANO-CLASSIC-650', 'Dầu gội Romano Classic 650g', 'Hóa mỹ phẩm', 'Dầu gội', 'Dầu gội nam', 'CHAI', '12 chai/thùng', 98000, 'ACTIVE', 'THUNG', 12, NULL, NULL),
('DOVEMEN-CARE-640', 'Dầu gội Dove Men+Care 640g', 'Hóa mỹ phẩm', 'Dầu gội', 'Dầu gội nam', 'CHAI', '12 chai/thùng', 142000, 'ACTIVE', 'THUNG', 12, NULL, NULL),
-- Hóa mỹ phẩm/Dầu gội/Dầu gội nữ
('SUNSILK-MEMUOT-650', 'Dầu gội Sunsilk mềm mượt diệu kỳ 650g', 'Hóa mỹ phẩm', 'Dầu gội', 'Dầu gội nữ', 'CHAI', '12 chai/thùng', 118000, 'ACTIVE', 'THUNG', 12, NULL, NULL),
('DOVE-PHUCHOI-640', 'Dầu gội Dove phục hồi hư tổn 640g', 'Hóa mỹ phẩm', 'Dầu gội', 'Dầu gội nữ', 'CHAI', '12 chai/thùng', 138000, 'ACTIVE', 'THUNG', 12, NULL, NULL),
('TRESEMME-VAINEP-640', 'Dầu gội TRESemmé vào nếp 640g', 'Hóa mỹ phẩm', 'Dầu gội', 'Dầu gội nữ', 'CHAI', '12 chai/thùng', 132000, 'ACTIVE', 'THUNG', 12, NULL, NULL),
('PANTENE-TOCDEP-650', 'Dầu gội Pantene tóc dài óng ả 650g', 'Hóa mỹ phẩm', 'Dầu gội', 'Dầu gội nữ', 'CHAI', '12 chai/thùng', 126000, 'DISCONTINUED', 'THUNG', 12, NULL, NULL),
-- Hóa mỹ phẩm/Dầu gội/Dầu gội trị gàu
('CLEAR-BACHA-630', 'Dầu gội Clear bạc hà mát lạnh 630g', 'Hóa mỹ phẩm', 'Dầu gội', 'Dầu gội trị gàu', 'CHAI', '12 chai/thùng', 128000, 'ACTIVE', 'THUNG', 12, NULL, NULL),
('HEADSHOULDERS-TUOIMAT-625', 'Dầu gội Head & Shoulders tươi mát 625ml', 'Hóa mỹ phẩm', 'Dầu gội', 'Dầu gội trị gàu', 'CHAI', '12 chai/thùng', 134000, 'ACTIVE', 'THUNG', 12, NULL, NULL),
('NIZORAL-100', 'Dầu gội Nizoral trị gàu 100ml', 'Hóa mỹ phẩm', 'Dầu gội', 'Dầu gội trị gàu', 'CHAI', '24 chai/thùng', 165000, 'ACTIVE', 'THUNG', 24, NULL, NULL),
-- Hóa mỹ phẩm/Dầu xả
('SUNSILK-DAUXA-320', 'Dầu xả Sunsilk mềm mượt 320g', 'Hóa mỹ phẩm', 'Dầu xả', NULL, 'CHAI', '12 chai/thùng', 62000, 'ACTIVE', 'THUNG', 12, NULL, NULL),
('DOVE-DAUXA-335', 'Dầu xả Dove dưỡng chất 335g', 'Hóa mỹ phẩm', 'Dầu xả', NULL, 'CHAI', '12 chai/thùng', 72000, 'ACTIVE', 'THUNG', 12, NULL, NULL),
('TRESEMME-DAUXA-340', 'Dầu xả TRESemmé vào nếp 340g', 'Hóa mỹ phẩm', 'Dầu xả', NULL, 'CHAI', '12 chai/thùng', 78000, 'ACTIVE', 'THUNG', 12, NULL, NULL),
('CLEAR-DAUXA-300', 'Dầu xả Clear sạch gàu 300g', 'Hóa mỹ phẩm', 'Dầu xả', NULL, 'CHAI', '12 chai/thùng', 65000, 'ACTIVE', 'THUNG', 12, NULL, NULL),
-- Hóa mỹ phẩm/Sữa tắm
('DOVE-SUATAM-530', 'Sữa tắm Dove dưỡng ẩm 530g', 'Hóa mỹ phẩm', 'Sữa tắm', NULL, 'CHAI', '12 chai/thùng', 142000, 'ACTIVE', 'THUNG', 12, NULL, NULL),
('LIFEBUOY-SUATAM-850', 'Sữa tắm Lifebuoy bảo vệ vượt trội 850g', 'Hóa mỹ phẩm', 'Sữa tắm', NULL, 'CHAI', '12 chai/thùng', 118000, 'ACTIVE', 'THUNG', 12, NULL, NULL),
('LUX-SUATAM-800', 'Sữa tắm Lux hoa hồng 800g', 'Hóa mỹ phẩm', 'Sữa tắm', NULL, 'CHAI', '12 chai/thùng', 112000, 'ACTIVE', 'THUNG', 12, NULL, NULL),
('PALMOLIVE-SUATAM-750', 'Sữa tắm Palmolive thiên nhiên 750ml', 'Hóa mỹ phẩm', 'Sữa tắm', NULL, 'CHAI', '12 chai/thùng', 98000, 'ACTIVE', 'THUNG', 12, NULL, NULL),
('ENCHANTEUR-SUATAM-650', 'Sữa tắm Enchanteur hương nước hoa 650g', 'Hóa mỹ phẩm', 'Sữa tắm', NULL, 'CHAI', '12 chai/thùng', 125000, 'DISCONTINUED', 'THUNG', 12, NULL, NULL),
-- Hóa mỹ phẩm/Xà phòng
('LIFEBUOY-XP-90', 'Xà phòng Lifebuoy bánh 90g', 'Hóa mỹ phẩm', 'Xà phòng', NULL, 'CAI', '72 cái/thùng', 8500, 'ACTIVE', 'THUNG', 72, NULL, NULL),
('LUX-XP-90', 'Xà phòng Lux bánh 90g', 'Hóa mỹ phẩm', 'Xà phòng', NULL, 'CAI', '72 cái/thùng', 8800, 'ACTIVE', 'THUNG', 72, NULL, NULL),
('DOVE-XP-90', 'Xà phòng Dove bánh 90g', 'Hóa mỹ phẩm', 'Xà phòng', NULL, 'CAI', '72 cái/thùng', 14500, 'ACTIVE', 'THUNG', 72, NULL, NULL),
('CAMAY-XP-90', 'Xà phòng Camay bánh 90g', 'Hóa mỹ phẩm', 'Xà phòng', NULL, 'CAI', '72 cái/thùng', 9500, 'ACTIVE', 'THUNG', 72, NULL, NULL),
-- Hóa mỹ phẩm/Kem đánh răng
('PS-180', 'Kem đánh răng P/S bảo vệ 123 tuýp 180g', 'Hóa mỹ phẩm', 'Kem đánh răng', NULL, 'TUYP', '48 tuýp/thùng', 28500, 'ACTIVE', 'THUNG', 48, NULL, NULL),
('PS-MUOI-180', 'Kem đánh răng P/S muối tuýp 180g', 'Hóa mỹ phẩm', 'Kem đánh răng', NULL, 'TUYP', '48 tuýp/thùng', 29500, 'ACTIVE', 'THUNG', 48, NULL, NULL),
('COLGATE-MAXFRESH-200', 'Kem đánh răng Colgate Maxfresh tuýp 200g', 'Hóa mỹ phẩm', 'Kem đánh răng', NULL, 'TUYP', '48 tuýp/thùng', 38500, 'ACTIVE', 'THUNG', 48, NULL, NULL),
('CLOSEUP-HOA-160', 'Kem đánh răng Closeup nụ hoa tuýp 160g', 'Hóa mỹ phẩm', 'Kem đánh răng', NULL, 'TUYP', '48 tuýp/thùng', 33500, 'ACTIVE', 'THUNG', 48, NULL, NULL),
('SENSODYNE-100', 'Kem đánh răng Sensodyne tuýp 100g', 'Hóa mỹ phẩm', 'Kem đánh răng', NULL, 'TUYP', '24 tuýp/thùng', 68000, 'ACTIVE', 'THUNG', 24, NULL, NULL),
-- Hóa mỹ phẩm/Nước súc miệng
('LISTERINE-COOLMINT-750', 'Nước súc miệng Listerine Coolmint 750ml', 'Hóa mỹ phẩm', 'Nước súc miệng', NULL, 'CHAI', '12 chai/thùng', 112000, 'ACTIVE', 'THUNG', 12, NULL, NULL),
('COLGATE-PLAX-500', 'Nước súc miệng Colgate Plax 500ml', 'Hóa mỹ phẩm', 'Nước súc miệng', NULL, 'CHAI', '12 chai/thùng', 68000, 'ACTIVE', 'THUNG', 12, NULL, NULL),
('PS-SUCMIENG-250', 'Nước súc miệng P/S 250ml', 'Hóa mỹ phẩm', 'Nước súc miệng', NULL, 'CHAI', '24 chai/thùng', 38500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
-- Chăm sóc gia đình/Nước giặt/Nước giặt cửa trước
('OMO-MATIC-CT-2.7', 'Nước giặt OMO Matic cửa trước 2,7kg', 'Chăm sóc gia đình', 'Nước giặt', 'Nước giặt cửa trước', 'CHAI', '4 chai/thùng', 168000, 'ACTIVE', 'THUNG', 4, NULL, NULL),
('COMFORT-GIAT-CT-2.6', 'Nước giặt Comfort cửa trước 2,6kg', 'Chăm sóc gia đình', 'Nước giặt', 'Nước giặt cửa trước', 'CHAI', '4 chai/thùng', 158000, 'ACTIVE', 'THUNG', 4, NULL, NULL),
('SURF-CT-2.4', 'Nước giặt Surf cửa trước 2,4kg', 'Chăm sóc gia đình', 'Nước giặt', 'Nước giặt cửa trước', 'CHAI', '4 chai/thùng', 118000, 'ACTIVE', 'THUNG', 4, NULL, NULL),
('ARIEL-CT-2.4', 'Nước giặt Ariel cửa trước 2,4kg', 'Chăm sóc gia đình', 'Nước giặt', 'Nước giặt cửa trước', 'CHAI', '4 chai/thùng', 192000, 'DISCONTINUED', 'THUNG', 4, NULL, NULL),
-- Chăm sóc gia đình/Nước giặt/Nước giặt cửa trên
('OMO-MATIC-CTR-2.7', 'Nước giặt OMO Matic cửa trên 2,7kg', 'Chăm sóc gia đình', 'Nước giặt', 'Nước giặt cửa trên', 'CHAI', '4 chai/thùng', 162000, 'ACTIVE', 'THUNG', 4, NULL, NULL),
('SUNLIGHT-GIAT-3.6', 'Nước giặt Sunlight cửa trên 3,6kg', 'Chăm sóc gia đình', 'Nước giặt', 'Nước giặt cửa trên', 'CHAI', '4 chai/thùng', 135000, 'ACTIVE', 'THUNG', 4, NULL, NULL),
('ATTACK-3.6', 'Nước giặt Attack 3,6kg', 'Chăm sóc gia đình', 'Nước giặt', 'Nước giặt cửa trên', 'CHAI', '4 chai/thùng', 148000, 'ACTIVE', 'THUNG', 4, NULL, NULL),
('ABA-3.2', 'Nước giặt Aba 3,2kg', 'Chăm sóc gia đình', 'Nước giặt', 'Nước giặt cửa trên', 'CHAI', '4 chai/thùng', 128000, 'ACTIVE', 'THUNG', 4, NULL, NULL),
-- Chăm sóc gia đình/Nước xả vải
('COMFORT-DIUDANG-3.2', 'Nước xả vải Comfort đậm đặc 3,2 lít', 'Chăm sóc gia đình', 'Nước xả vải', NULL, 'CHAI', '6 chai/thùng', 155000, 'ACTIVE', 'THUNG', 6, NULL, NULL),
('COMFORT-TUI-1.6', 'Nước xả vải Comfort túi 1,6 lít', 'Chăm sóc gia đình', 'Nước xả vải', NULL, 'TUI', '8 túi/thùng', 78000, 'ACTIVE', 'THUNG', 8, NULL, NULL),
('DOWNY-NANGMAI-3', 'Nước xả vải Downy nắng mai 3 lít', 'Chăm sóc gia đình', 'Nước xả vải', NULL, 'CHAI', '6 chai/thùng', 148000, 'ACTIVE', 'THUNG', 6, NULL, NULL),
('LENOR-2.5', 'Nước xả vải Lenor 2,5 lít', 'Chăm sóc gia đình', 'Nước xả vải', NULL, 'CHAI', '6 chai/thùng', 128000, 'ACTIVE', 'THUNG', 6, NULL, NULL),
-- Chăm sóc gia đình/Nước rửa chén
('SUNLIGHT-CHEN-750', 'Nước rửa chén Sunlight chanh 750ml', 'Chăm sóc gia đình', 'Nước rửa chén', NULL, 'CHAI', '12 chai/thùng', 38500, 'ACTIVE', 'THUNG', 12, NULL, NULL),
('SUNLIGHT-CHEN-3.6', 'Nước rửa chén Sunlight chanh 3,6 kg', 'Chăm sóc gia đình', 'Nước rửa chén', NULL, 'CAN', '4 can/thùng', 128000, 'ACTIVE', 'THUNG', 4, NULL, NULL),
('MYKINH-CHEN-750', 'Nước rửa chén Mỹ Hảo 750ml', 'Chăm sóc gia đình', 'Nước rửa chén', NULL, 'CHAI', '12 chai/thùng', 32500, 'ACTIVE', 'THUNG', 12, NULL, NULL),
('LIX-CHEN-750', 'Nước rửa chén Lix trà xanh 750ml', 'Chăm sóc gia đình', 'Nước rửa chén', NULL, 'CHAI', '12 chai/thùng', 34500, 'DISCONTINUED', 'THUNG', 12, NULL, NULL),
('SUNLIGHT-TUI-600', 'Nước rửa chén Sunlight túi 600ml', 'Chăm sóc gia đình', 'Nước rửa chén', NULL, 'TUI', '20 túi/thùng', 24500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
-- Chăm sóc gia đình/Nước lau sàn
('SUNLIGHT-LAUSAN-1KG', 'Nước lau sàn Sunlight 1kg', 'Chăm sóc gia đình', 'Nước lau sàn', NULL, 'CHAI', '12 chai/thùng', 42500, 'ACTIVE', 'THUNG', 12, NULL, NULL),
('GIFT-LAUSAN-1KG', 'Nước lau sàn Gift 1kg', 'Chăm sóc gia đình', 'Nước lau sàn', NULL, 'CHAI', '12 chai/thùng', 38500, 'ACTIVE', 'THUNG', 12, NULL, NULL),
('LIX-LAUSAN-1KG', 'Nước lau sàn Lix hương hoa 1kg', 'Chăm sóc gia đình', 'Nước lau sàn', NULL, 'CHAI', '12 chai/thùng', 36500, 'ACTIVE', 'THUNG', 12, NULL, NULL),
('VIM-LAUSAN-1KG', 'Nước lau sàn Vim 1kg', 'Chăm sóc gia đình', 'Nước lau sàn', NULL, 'CHAI', '12 chai/thùng', 44500, 'ACTIVE', 'THUNG', 12, NULL, NULL),
-- Chăm sóc gia đình/Chất tẩy rửa
('VIM-TAYBON-880', 'Nước tẩy bồn cầu Vim 880ml', 'Chăm sóc gia đình', 'Chất tẩy rửa', NULL, 'CHAI', '12 chai/thùng', 38500, 'ACTIVE', 'THUNG', 12, NULL, NULL),
('HARPIC-CAU-750', 'Nước tẩy bồn cầu Harpic 750ml', 'Chăm sóc gia đình', 'Chất tẩy rửa', NULL, 'CHAI', '12 chai/thùng', 52500, 'ACTIVE', 'THUNG', 12, NULL, NULL),
('JAVEL-MR-1L', 'Nước tẩy Javel Mr Muscle 1 lít', 'Chăm sóc gia đình', 'Chất tẩy rửa', NULL, 'CHAI', '12 chai/thùng', 28500, 'ACTIVE', 'THUNG', 12, NULL, NULL),
('CIF-KEM-690', 'Kem tẩy đa năng Cif 690g', 'Chăm sóc gia đình', 'Chất tẩy rửa', NULL, 'CHAI', '12 chai/thùng', 48500, 'ACTIVE', 'THUNG', 12, NULL, NULL),
('LIX-TAYNHA-900', 'Nước tẩy rửa nhà tắm Lix 900ml', 'Chăm sóc gia đình', 'Chất tẩy rửa', NULL, 'CHAI', '12 chai/thùng', 32500, 'ACTIVE', 'THUNG', 12, NULL, NULL),
-- Chăm sóc gia đình/Túi rác
('TUIRAC-NHO-50', 'Túi rác đen cuộn nhỏ 50 cái', 'Chăm sóc gia đình', 'Túi rác', NULL, 'CUON', '20 cuộn/thùng', 15500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
('TUIRAC-TRUNG-30', 'Túi rác đen cuộn trung 30 cái', 'Chăm sóc gia đình', 'Túi rác', NULL, 'CUON', '20 cuộn/thùng', 19500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
('TUIRAC-LON-20', 'Túi rác đen cuộn lớn 20 cái', 'Chăm sóc gia đình', 'Túi rác', NULL, 'CUON', '20 cuộn/thùng', 24500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
('TUIRAC-SINHHOC-20', 'Túi rác tự phân hủy sinh học cuộn 20 cái', 'Chăm sóc gia đình', 'Túi rác', NULL, 'CUON', '20 cuộn/thùng', 32500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
-- Giấy và sản phẩm vệ sinh/Giấy vệ sinh
('BLESSYOU-10CUON', 'Giấy vệ sinh Bless You 10 cuộn', 'Giấy và sản phẩm vệ sinh', 'Giấy vệ sinh', NULL, 'LOC', '12 lốc/thùng', 52000, 'ACTIVE', 'THUNG', 12, NULL, NULL),
('BLESSYOU-4CUON', 'Giấy vệ sinh Bless You 4 cuộn', 'Giấy và sản phẩm vệ sinh', 'Giấy vệ sinh', NULL, 'LOC', '12 lốc/thùng', 22000, 'ACTIVE', 'THUNG', 12, NULL, NULL),
('PULPPY-10CUON', 'Giấy vệ sinh Pulppy 10 cuộn', 'Giấy và sản phẩm vệ sinh', 'Giấy vệ sinh', NULL, 'LOC', '12 lốc/thùng', 48000, 'ACTIVE', 'THUNG', 12, NULL, NULL),
('SAIGON-GVS-10', 'Giấy vệ sinh Sài Gòn 10 cuộn', 'Giấy và sản phẩm vệ sinh', 'Giấy vệ sinh', NULL, 'LOC', '12 lốc/thùng', 38000, 'ACTIVE', 'THUNG', 12, NULL, NULL),
('ELLEMME-GVS-10', 'Giấy vệ sinh Elleme 10 cuộn', 'Giấy và sản phẩm vệ sinh', 'Giấy vệ sinh', NULL, 'LOC', '12 lốc/thùng', 58000, 'DISCONTINUED', 'THUNG', 12, NULL, NULL),
('WATERSILK-GVS-12', 'Giấy vệ sinh Watersilk 12 cuộn', 'Giấy và sản phẩm vệ sinh', 'Giấy vệ sinh', NULL, 'LOC', '12 lốc/thùng', 68000, 'ACTIVE', 'THUNG', 12, NULL, NULL),
-- Giấy và sản phẩm vệ sinh/Khăn giấy
('PULPPY-KG-RUT', 'Khăn giấy rút Pulppy 180 tờ', 'Giấy và sản phẩm vệ sinh', 'Khăn giấy', NULL, 'GOI', '30 gói/thùng', 9500, 'ACTIVE', 'THUNG', 30, NULL, NULL),
('SAIGON-KG-RUT', 'Khăn giấy rút Sài Gòn 200 tờ', 'Giấy và sản phẩm vệ sinh', 'Khăn giấy', NULL, 'GOI', '30 gói/thùng', 8500, 'ACTIVE', 'THUNG', 30, NULL, NULL),
('BLESSYOU-KG-HOP', 'Khăn giấy hộp Bless You 180 tờ', 'Giấy và sản phẩm vệ sinh', 'Khăn giấy', NULL, 'HOP', '24 hộp/thùng', 22500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('WATERSILK-KG-RUT', 'Khăn giấy rút Watersilk 3 lớp', 'Giấy và sản phẩm vệ sinh', 'Khăn giấy', NULL, 'GOI', '30 gói/thùng', 14500, 'ACTIVE', 'THUNG', 30, NULL, NULL),
-- Giấy và sản phẩm vệ sinh/Khăn ướt
('BOBBY-KU-80', 'Khăn ướt Bobby 80 tờ', 'Giấy và sản phẩm vệ sinh', 'Khăn ướt', NULL, 'GOI', '48 gói/thùng', 28500, 'ACTIVE', 'THUNG', 48, NULL, NULL),
('MAMAMY-KU-100', 'Khăn ướt Mamamy 100 tờ', 'Giấy và sản phẩm vệ sinh', 'Khăn ướt', NULL, 'GOI', '48 gói/thùng', 24500, 'ACTIVE', 'THUNG', 48, NULL, NULL),
('BIORE-KU-20', 'Khăn ướt Bioré 20 tờ', 'Giấy và sản phẩm vệ sinh', 'Khăn ướt', NULL, 'GOI', '48 gói/thùng', 12500, 'ACTIVE', 'THUNG', 48, NULL, NULL),
('WATERWIPES-KU-60', 'Khăn ướt WaterWipes 60 tờ', 'Giấy và sản phẩm vệ sinh', 'Khăn ướt', NULL, 'GOI', '24 gói/thùng', 98000, 'ACTIVE', 'THUNG', 24, NULL, NULL),
-- Giấy và sản phẩm vệ sinh/Giấy ăn
('PULPPY-GA-NAPKIN', 'Giấy ăn vuông Pulppy 100 tờ', 'Giấy và sản phẩm vệ sinh', 'Giấy ăn', NULL, 'GOI', '48 gói/thùng', 7500, 'ACTIVE', 'THUNG', 48, NULL, NULL),
('SAIGON-GA-NAPKIN', 'Giấy ăn vuông Sài Gòn 100 tờ', 'Giấy và sản phẩm vệ sinh', 'Giấy ăn', NULL, 'GOI', '48 gói/thùng', 6800, 'ACTIVE', 'THUNG', 48, NULL, NULL),
('NAPKIN-NHA-HANG', 'Giấy ăn nhà hàng Hoa Mai 500 tờ', 'Giấy và sản phẩm vệ sinh', 'Giấy ăn', NULL, 'GOI', '20 gói/thùng', 16500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
-- Giấy và sản phẩm vệ sinh/Băng vệ sinh
('DIANA-SENSI-8', 'Băng vệ sinh Diana Sensi cool fresh 8 miếng', 'Giấy và sản phẩm vệ sinh', 'Băng vệ sinh', NULL, 'GOI', '48 gói/thùng', 14500, 'ACTIVE', 'THUNG', 48, NULL, NULL),
('LAURIER-8', 'Băng vệ sinh Laurier siêu mỏng cánh 8 miếng', 'Giấy và sản phẩm vệ sinh', 'Băng vệ sinh', NULL, 'GOI', '48 gói/thùng', 16500, 'ACTIVE', 'THUNG', 48, NULL, NULL),
('KOTEX-STYLE-8', 'Băng vệ sinh Kotex Style 8 miếng', 'Giấy và sản phẩm vệ sinh', 'Băng vệ sinh', NULL, 'GOI', '48 gói/thùng', 15500, 'ACTIVE', 'THUNG', 48, NULL, NULL),
('CAREFREE-BVS-20', 'Băng hằng ngày Carefree 20 miếng', 'Giấy và sản phẩm vệ sinh', 'Băng vệ sinh', NULL, 'GOI', '48 gói/thùng', 18500, 'ACTIVE', 'THUNG', 48, NULL, NULL),
-- Đồ dùng cá nhân/Bàn chải đánh răng
('PS-BANCHAI-MEM', 'Bàn chải P/S lông mềm', 'Đồ dùng cá nhân', 'Bàn chải đánh răng', NULL, 'CAI', '72 cái/thùng', 12500, 'ACTIVE', 'THUNG', 72, NULL, NULL),
('COLGATE-SLIM-SOFT', 'Bàn chải Colgate Slim Soft', 'Đồ dùng cá nhân', 'Bàn chải đánh răng', NULL, 'CAI', '72 cái/thùng', 28500, 'ACTIVE', 'THUNG', 72, NULL, NULL),
('ORALB-CLASSIC', 'Bàn chải Oral-B Classic', 'Đồ dùng cá nhân', 'Bàn chải đánh răng', NULL, 'CAI', '72 cái/thùng', 24500, 'ACTIVE', 'THUNG', 72, NULL, NULL),
('SENSODYNE-BANCHAI', 'Bàn chải Sensodyne mềm', 'Đồ dùng cá nhân', 'Bàn chải đánh răng', NULL, 'CAI', '72 cái/thùng', 42500, 'DISCONTINUED', 'THUNG', 72, NULL, NULL),
-- Đồ dùng cá nhân/Dao cạo
('GILLETTE-BLUE2', 'Dao cạo râu Gillette Blue II', 'Đồ dùng cá nhân', 'Dao cạo', NULL, 'CAI', '48 cái/thùng', 18500, 'ACTIVE', 'THUNG', 48, NULL, NULL),
('GILLETTE-VECTOR3', 'Dao cạo râu Gillette Vector 3', 'Đồ dùng cá nhân', 'Dao cạo', NULL, 'CAI', '48 cái/thùng', 38500, 'ACTIVE', 'THUNG', 48, NULL, NULL),
('SCHICK-XTREME3', 'Dao cạo râu Schick Xtreme 3', 'Đồ dùng cá nhân', 'Dao cạo', NULL, 'CAI', '48 cái/thùng', 36500, 'ACTIVE', 'THUNG', 48, NULL, NULL),
-- Đồ dùng cá nhân/Khăn tắm và khăn mặt
('KHANMAT-COTTON-30X50', 'Khăn mặt cotton 30x50cm', 'Đồ dùng cá nhân', 'Khăn tắm và khăn mặt', NULL, 'CAI', '20 cái/thùng', 18500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
('KHANTAM-COTTON-60X120', 'Khăn tắm cotton 60x120cm', 'Đồ dùng cá nhân', 'Khăn tắm và khăn mặt', NULL, 'CAI', '20 cái/thùng', 68500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
('KHANMAT-TRE-30X30', 'Khăn mặt sợi tre 30x30cm', 'Đồ dùng cá nhân', 'Khăn tắm và khăn mặt', NULL, 'CAI', '20 cái/thùng', 14500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
-- Đồ dùng cá nhân/Vệ sinh cá nhân
('TAMBONG-GOI-100', 'Tăm bông ngoáy tai hộp 100 que', 'Đồ dùng cá nhân', 'Vệ sinh cá nhân', NULL, 'HOP', '48 hộp/thùng', 8500, 'ACTIVE', 'THUNG', 48, NULL, NULL),
('LUOC-NHUA', 'Lược nhựa chải tóc', 'Đồ dùng cá nhân', 'Vệ sinh cá nhân', NULL, 'CAI', '48 cái/thùng', 7500, 'ACTIVE', 'THUNG', 48, NULL, NULL),
('BAMMONG-INOX', 'Bấm móng tay inox', 'Đồ dùng cá nhân', 'Vệ sinh cá nhân', NULL, 'CAI', '48 cái/thùng', 14500, 'ACTIVE', 'THUNG', 48, NULL, NULL),
('GIAYTHAM-DAU-50', 'Giấy thấm dầu hộp 50 tờ', 'Đồ dùng cá nhân', 'Vệ sinh cá nhân', NULL, 'HOP', '48 hộp/thùng', 18500, 'ACTIVE', 'THUNG', 48, NULL, NULL),
-- Gia vị và nguyên liệu nấu ăn/Nước mắm
('NAMNGU-500', 'Nước mắm Nam Ngư chai 500ml', 'Gia vị và nguyên liệu nấu ăn', 'Nước mắm', NULL, 'CHAI', '12 chai/thùng', 36500, 'ACTIVE', 'THUNG', 12, NULL, NULL),
('NAMNGU-750', 'Nước mắm Nam Ngư chai 750ml', 'Gia vị và nguyên liệu nấu ăn', 'Nước mắm', NULL, 'CHAI', '12 chai/thùng', 52500, 'ACTIVE', 'THUNG', 12, NULL, NULL),
('CHINSU-500', 'Nước mắm Chinsu chai 500ml', 'Gia vị và nguyên liệu nấu ăn', 'Nước mắm', NULL, 'CHAI', '12 chai/thùng', 34500, 'ACTIVE', 'THUNG', 12, NULL, NULL),
('PHUQUOC-ONG-500', 'Nước mắm Phú Quốc Khải Hoàn 500ml', 'Gia vị và nguyên liệu nấu ăn', 'Nước mắm', NULL, 'CHAI', '12 chai/thùng', 68500, 'ACTIVE', 'THUNG', 12, NULL, NULL),
('HUNGTHINH-500', 'Nước mắm cá cơm Hưng Thịnh 500ml', 'Gia vị và nguyên liệu nấu ăn', 'Nước mắm', NULL, 'CHAI', '12 chai/thùng', 42500, 'DISCONTINUED', 'THUNG', 12, NULL, NULL),
-- Gia vị và nguyên liệu nấu ăn/Nước tương
('MAGGI-TUONG-300', 'Nước tương Maggi đậu nành 300ml', 'Gia vị và nguyên liệu nấu ăn', 'Nước tương', NULL, 'CHAI', '12 chai/thùng', 21500, 'ACTIVE', 'THUNG', 12, NULL, NULL),
('CHINSU-TUONG-500', 'Nước tương Chinsu 500ml', 'Gia vị và nguyên liệu nấu ăn', 'Nước tương', NULL, 'CHAI', '12 chai/thùng', 22500, 'ACTIVE', 'THUNG', 12, NULL, NULL),
('TAMTHAI-TUONG-500', 'Nước tương Tam Thái Tử 500ml', 'Gia vị và nguyên liệu nấu ăn', 'Nước tương', NULL, 'CHAI', '12 chai/thùng', 18500, 'ACTIVE', 'THUNG', 12, NULL, NULL),
('KIKKOMAN-TUONG-250', 'Nước tương Kikkoman 250ml', 'Gia vị và nguyên liệu nấu ăn', 'Nước tương', NULL, 'CHAI', '24 chai/thùng', 38500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
-- Gia vị và nguyên liệu nấu ăn/Dầu ăn
('NEPTUNE-1L', 'Dầu ăn Neptune 1 lít', 'Gia vị và nguyên liệu nấu ăn', 'Dầu ăn', NULL, 'CHAI', '12 chai/thùng', 58500, 'ACTIVE', 'THUNG', 12, NULL, NULL),
('SIMPLY-1L', 'Dầu ăn Simply đậu nành 1 lít', 'Gia vị và nguyên liệu nấu ăn', 'Dầu ăn', NULL, 'CHAI', '12 chai/thùng', 52500, 'ACTIVE', 'THUNG', 12, NULL, NULL),
('TUONGAN-1L', 'Dầu ăn Tường An 1 lít', 'Gia vị và nguyên liệu nấu ăn', 'Dầu ăn', NULL, 'CHAI', '12 chai/thùng', 49500, 'ACTIVE', 'THUNG', 12, NULL, NULL),
('MARVELA-1L', 'Dầu ăn Marvela 1 lít', 'Gia vị và nguyên liệu nấu ăn', 'Dầu ăn', NULL, 'CHAI', '12 chai/thùng', 46500, 'ACTIVE', 'THUNG', 12, NULL, NULL),
('NEPTUNE-5L', 'Dầu ăn Neptune can 5 lít', 'Gia vị và nguyên liệu nấu ăn', 'Dầu ăn', NULL, 'CAN', '4 can/thùng', 275000, 'ACTIVE', 'THUNG', 4, NULL, NULL),
('SIMPLY-2L', 'Dầu ăn Simply chai 2 lít', 'Gia vị và nguyên liệu nấu ăn', 'Dầu ăn', NULL, 'CHAI', '8 chai/thùng', 102000, 'ACTIVE', 'THUNG', 8, NULL, NULL),
-- Gia vị và nguyên liệu nấu ăn/Muối
('MUOI-IOT-500', 'Muối i-ốt Bạc Liêu 500g', 'Gia vị và nguyên liệu nấu ăn', 'Muối', NULL, 'GOI', '40 gói/thùng', 4500, 'ACTIVE', 'THUNG', 40, NULL, NULL),
('MUOI-TINH-1KG', 'Muối tinh Việt Nam 1kg', 'Gia vị và nguyên liệu nấu ăn', 'Muối', NULL, 'GOI', '40 gói/thùng', 7500, 'ACTIVE', 'THUNG', 40, NULL, NULL),
('MUOI-TOM-250', 'Muối tôm Tây Ninh 250g', 'Gia vị và nguyên liệu nấu ăn', 'Muối', NULL, 'GOI', '40 gói/thùng', 12500, 'DISCONTINUED', 'THUNG', 40, NULL, NULL),
('MUOI-HONG-HIMALAYA-500', 'Muối hồng Himalaya 500g', 'Gia vị và nguyên liệu nấu ăn', 'Muối', NULL, 'GOI', '40 gói/thùng', 38500, 'ACTIVE', 'THUNG', 40, NULL, NULL),
-- Gia vị và nguyên liệu nấu ăn/Đường
('DUONG-BIENHOA-1KG', 'Đường tinh luyện Biên Hòa 1kg', 'Gia vị và nguyên liệu nấu ăn', 'Đường', NULL, 'BICH', '20 bịch/thùng', 24500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
('DUONG-LAGI-1KG', 'Đường trắng Lam Sơn 1kg', 'Gia vị và nguyên liệu nấu ăn', 'Đường', NULL, 'BICH', '20 bịch/thùng', 21500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
('DUONG-PHEN-500', 'Đường phèn Biên Hòa 500g', 'Gia vị và nguyên liệu nấu ăn', 'Đường', NULL, 'BICH', '20 bịch/thùng', 22500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
('DUONG-CATTAY-1KG', 'Đường cát vàng 1kg', 'Gia vị và nguyên liệu nấu ăn', 'Đường', NULL, 'BICH', '20 bịch/thùng', 23500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
-- Gia vị và nguyên liệu nấu ăn/Bột ngọt
('AJINOMOTO-400', 'Bột ngọt Ajinomoto gói 400g', 'Gia vị và nguyên liệu nấu ăn', 'Bột ngọt', NULL, 'GOI', '20 gói/thùng', 38500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
('AJINOMOTO-1KG', 'Bột ngọt Ajinomoto gói 1kg', 'Gia vị và nguyên liệu nấu ăn', 'Bột ngọt', NULL, 'GOI', '10 gói/thùng', 88500, 'ACTIVE', 'THUNG', 10, NULL, NULL),
('VEDAN-400', 'Bột ngọt Vedan gói 400g', 'Gia vị và nguyên liệu nấu ăn', 'Bột ngọt', NULL, 'GOI', '20 gói/thùng', 33500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
('MIWON-400', 'Bột ngọt Miwon gói 400g', 'Gia vị và nguyên liệu nấu ăn', 'Bột ngọt', NULL, 'GOI', '20 gói/thùng', 34500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
-- Gia vị và nguyên liệu nấu ăn/Hạt nêm
('KNORR-HATNEM-900', 'Hạt nêm Knorr thịt thăn xương ống 900g', 'Gia vị và nguyên liệu nấu ăn', 'Hạt nêm', NULL, 'GOI', '20 gói/thùng', 62500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
('KNORR-HATNEM-400', 'Hạt nêm Knorr 400g', 'Gia vị và nguyên liệu nấu ăn', 'Hạt nêm', NULL, 'GOI', '20 gói/thùng', 31500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
('MAGGI-HATNEM-900', 'Hạt nêm Maggi 900g', 'Gia vị và nguyên liệu nấu ăn', 'Hạt nêm', NULL, 'GOI', '20 gói/thùng', 58500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
('CHINSU-HATNEM-400', 'Hạt nêm Chinsu 400g', 'Gia vị và nguyên liệu nấu ăn', 'Hạt nêm', NULL, 'GOI', '20 gói/thùng', 29500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
('AJI-NGON-HATNEM-1KG', 'Hạt nêm Aji-ngon 1kg', 'Gia vị và nguyên liệu nấu ăn', 'Hạt nêm', NULL, 'GOI', '20 gói/thùng', 72500, 'DISCONTINUED', 'THUNG', 20, NULL, NULL),
-- Gia vị và nguyên liệu nấu ăn/Tương ớt
('CHINSU-TUONGOT-250', 'Tương ớt Chinsu chai 250g', 'Gia vị và nguyên liệu nấu ăn', 'Tương ớt', NULL, 'CHAI', '24 chai/thùng', 12500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('CHINSU-TUONGOT-500', 'Tương ớt Chinsu chai 500g', 'Gia vị và nguyên liệu nấu ăn', 'Tương ớt', NULL, 'CHAI', '12 chai/thùng', 21500, 'ACTIVE', 'THUNG', 12, NULL, NULL),
('CHOLIMEX-TUONGOT-270', 'Tương ớt Cholimex 270g', 'Gia vị và nguyên liệu nấu ăn', 'Tương ớt', NULL, 'CHAI', '24 chai/thùng', 13500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('HEINZ-CA-300', 'Tương cà Heinz chai 300g', 'Gia vị và nguyên liệu nấu ăn', 'Tương ớt', NULL, 'CHAI', '24 chai/thùng', 28500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('MAGGI-TUONGCA-250', 'Tương cà Maggi chai 250g', 'Gia vị và nguyên liệu nấu ăn', 'Tương ớt', NULL, 'CHAI', '24 chai/thùng', 15500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
-- Đồ ăn nhẹ/Snack khoai và bắp
('OISHI-BAPNGOT-42', 'Snack Oishi bắp ngọt 42g', 'Đồ ăn nhẹ', 'Snack khoai và bắp', NULL, 'GOI', '24 gói/thùng', 4800, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('LAYS-TUMUC-52', 'Snack Lay''s khoai tây vị tự nhiên 52g', 'Đồ ăn nhẹ', 'Snack khoai và bắp', NULL, 'GOI', '24 gói/thùng', 11500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('POCA-KHOAI-54', 'Snack Poca khoai tây vị phô mai 54g', 'Đồ ăn nhẹ', 'Snack khoai và bắp', NULL, 'GOI', '24 gói/thùng', 8500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('OSTAR-KHOAIT-42', 'Snack O''Star khoai tây 42g', 'Đồ ăn nhẹ', 'Snack khoai và bắp', NULL, 'GOI', '24 gói/thùng', 7800, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('PRINGLES-107', 'Snack Pringles hộp 107g', 'Đồ ăn nhẹ', 'Snack khoai và bắp', NULL, 'HOP', '12 hộp/thùng', 36500, 'ACTIVE', 'THUNG', 12, NULL, NULL),
-- Đồ ăn nhẹ/Rong biển
('ONLY-RONGBIEN-5', 'Rong biển ăn liền Only Seaweed 5g', 'Đồ ăn nhẹ', 'Rong biển', NULL, 'GOI', '48 gói/thùng', 5500, 'ACTIVE', 'THUNG', 48, NULL, NULL),
('TAOKAENOI-RONGBIEN-32', 'Rong biển Tao Kae Noi 32g', 'Đồ ăn nhẹ', 'Rong biển', NULL, 'GOI', '48 gói/thùng', 17500, 'ACTIVE', 'THUNG', 48, NULL, NULL),
('HANSOL-RONGBIEN-10', 'Rong biển sấy Hansol 10g', 'Đồ ăn nhẹ', 'Rong biển', NULL, 'GOI', '48 gói/thùng', 8500, 'DISCONTINUED', 'THUNG', 48, NULL, NULL),
-- Đồ ăn nhẹ/Hạt các loại
('HATDIEU-RANGMUOI-500', 'Hạt điều rang muối Bình Phước 500g', 'Đồ ăn nhẹ', 'Hạt các loại', NULL, 'GOI', '20 gói/thùng', 128000, 'ACTIVE', 'THUNG', 20, NULL, NULL),
('HATHUONGDUONG-200', 'Hạt hướng dương Chacheer 130g', 'Đồ ăn nhẹ', 'Hạt các loại', NULL, 'GOI', '20 gói/thùng', 18500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
('HATDE-SAY-200', 'Hạt dẻ cười Mỹ 200g', 'Đồ ăn nhẹ', 'Hạt các loại', NULL, 'GOI', '20 gói/thùng', 88500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
('HATBI-300', 'Hạt bí rang 300g', 'Đồ ăn nhẹ', 'Hạt các loại', NULL, 'GOI', '20 gói/thùng', 48500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
-- Đồ ăn nhẹ/Khô chế biến
('KHOBO-MIENG-100', 'Khô bò miếng Vissan 100g', 'Đồ ăn nhẹ', 'Khô chế biến', NULL, 'GOI', '20 gói/thùng', 78500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
('KHAGA-LALIEU-100', 'Khô gà lá chanh 100g', 'Đồ ăn nhẹ', 'Khô chế biến', NULL, 'GOI', '20 gói/thùng', 52500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
('KHOMUC-NUONG-100', 'Khô mực nướng xé sợi 100g', 'Đồ ăn nhẹ', 'Khô chế biến', NULL, 'GOI', '20 gói/thùng', 98500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
('THITHEO-SAY-100', 'Thịt heo sấy 100g', 'Đồ ăn nhẹ', 'Khô chế biến', NULL, 'GOI', '20 gói/thùng', 62500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
-- Đồ ăn nhẹ/Đậu phộng
('OHAYO-DAUPHONG-100', 'Đậu phộng rang Ohayo 100g', 'Đồ ăn nhẹ', 'Đậu phộng', NULL, 'GOI', '30 gói/thùng', 12500, 'ACTIVE', 'THUNG', 30, NULL, NULL),
('TANTAN-DAUPHONG-80', 'Đậu phộng da cá Tân Tân 80g', 'Đồ ăn nhẹ', 'Đậu phộng', NULL, 'GOI', '30 gói/thùng', 9500, 'ACTIVE', 'THUNG', 30, NULL, NULL),
('DAUPHONG-TOI-100', 'Đậu phộng rang tỏi ớt 100g', 'Đồ ăn nhẹ', 'Đậu phộng', NULL, 'GOI', '30 gói/thùng', 11500, 'ACTIVE', 'THUNG', 30, NULL, NULL),
-- Đồ ăn nhẹ/Bắp rang
('POPCORN-BOT-90', 'Bắp rang bơ Hàn Quốc 90g', 'Đồ ăn nhẹ', 'Bắp rang', NULL, 'GOI', '30 gói/thùng', 14500, 'ACTIVE', 'THUNG', 30, NULL, NULL),
('POPCORN-CARAMEN-90', 'Bắp rang caramel 90g', 'Đồ ăn nhẹ', 'Bắp rang', NULL, 'GOI', '30 gói/thùng', 15500, 'ACTIVE', 'THUNG', 30, NULL, NULL),
('POPCORN-LAMNONG-100', 'Bắp rang vị phô mai 100g', 'Đồ ăn nhẹ', 'Bắp rang', NULL, 'GOI', '30 gói/thùng', 14500, 'ACTIVE', 'THUNG', 30, NULL, NULL),
-- Cà phê và trà/Cà phê hòa tan/Cà phê 3 trong 1
('NESCAFE-3IN1-20', 'Cà phê Nescafé 3 trong 1 hộp 20 gói', 'Cà phê và trà', 'Cà phê hòa tan', 'Cà phê 3 trong 1', 'HOP', '24 hộp/thùng', 52500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('G7-3IN1-21', 'Cà phê G7 3 trong 1 hộp 21 gói', 'Cà phê và trà', 'Cà phê hòa tan', 'Cà phê 3 trong 1', 'HOP', '24 hộp/thùng', 54500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('VINACAFE-3IN1-24', 'Cà phê Vinacafé gold 3 trong 1 hộp 24 gói', 'Cà phê và trà', 'Cà phê hòa tan', 'Cà phê 3 trong 1', 'HOP', '24 hộp/thùng', 49500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('TRUNGNGUYEN-3IN1-18', 'Cà phê Trung Nguyên Legend 3 trong 1 hộp 18 gói', 'Cà phê và trà', 'Cà phê hòa tan', 'Cà phê 3 trong 1', 'HOP', '24 hộp/thùng', 58500, 'DISCONTINUED', 'THUNG', 24, NULL, NULL),
-- Cà phê và trà/Cà phê hòa tan/Cà phê đen và cappuccino
('NESCAFE-DEN-17', 'Cà phê đen Nescafé hộp 17 gói', 'Cà phê và trà', 'Cà phê hòa tan', 'Cà phê đen và cappuccino', 'HOP', '24 hộp/thùng', 48500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('G7-DEN-15', 'Cà phê đen G7 hộp 15 gói', 'Cà phê và trà', 'Cà phê hòa tan', 'Cà phê đen và cappuccino', 'HOP', '24 hộp/thùng', 42500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('CAPPUCCINO-NESCAFE-12', 'Cà phê cappuccino Nescafé hộp 12 gói', 'Cà phê và trà', 'Cà phê hòa tan', 'Cà phê đen và cappuccino', 'HOP', '24 hộp/thùng', 56500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
-- Cà phê và trà/Cà phê rang xay
('TRUNGNGUYEN-SANGTAO-500', 'Cà phê rang xay Trung Nguyên Sáng tạo 5 gói 500g', 'Cà phê và trà', 'Cà phê rang xay', NULL, 'GOI', '20 gói/thùng', 128000, 'ACTIVE', 'THUNG', 20, NULL, NULL),
('HIGHLANDS-RANGXAY-200', 'Cà phê rang xay Highlands 200g', 'Cà phê và trà', 'Cà phê rang xay', NULL, 'GOI', '20 gói/thùng', 62500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
('CHON-RANGXAY-500', 'Cà phê rang xay Chồn nguyên chất 500g', 'Cà phê và trà', 'Cà phê rang xay', NULL, 'GOI', '20 gói/thùng', 98500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
-- Cà phê và trà/Trà túi lọc
('LIPTON-NHANVANG-25', 'Trà Lipton nhãn vàng hộp 25 túi', 'Cà phê và trà', 'Trà túi lọc', NULL, 'HOP', '48 hộp/thùng', 32500, 'ACTIVE', 'THUNG', 48, NULL, NULL),
('DILMAH-VANG-25', 'Trà Dilmah hộp 25 túi', 'Cà phê và trà', 'Trà túi lọc', NULL, 'HOP', '48 hộp/thùng', 42500, 'ACTIVE', 'THUNG', 48, NULL, NULL),
('COZY-DAO-25', 'Trà túi lọc Cozy hương đào hộp 25 túi', 'Cà phê và trà', 'Trà túi lọc', NULL, 'HOP', '48 hộp/thùng', 28500, 'DISCONTINUED', 'THUNG', 48, NULL, NULL),
('GUNG-TUILOC-20', 'Trà gừng túi lọc hộp 20 túi', 'Cà phê và trà', 'Trà túi lọc', NULL, 'HOP', '48 hộp/thùng', 24500, 'ACTIVE', 'THUNG', 48, NULL, NULL),
-- Cà phê và trà/Trà khô
('TRATHAINGUYEN-100', 'Chè Thái Nguyên khô gói 100g', 'Cà phê và trà', 'Trà khô', NULL, 'GOI', '20 gói/thùng', 38500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
('TRASEN-HANOI-100', 'Trà sen Hà Nội 100g', 'Cà phê và trà', 'Trà khô', NULL, 'GOI', '20 gói/thùng', 78500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
('TRAOLONG-200', 'Trà ô long khô 200g', 'Cà phê và trà', 'Trà khô', NULL, 'GOI', '20 gói/thùng', 88500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
-- Cà phê và trà/Trà hòa tan
('TRASUA-XANH-10', 'Trà sữa hòa tan Maccaffe hộp 10 gói', 'Cà phê và trà', 'Trà hòa tan', NULL, 'HOP', '24 hộp/thùng', 42500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('TRADAO-HOATAN-10', 'Trà đào hòa tan hộp 10 gói', 'Cà phê và trà', 'Trà hòa tan', NULL, 'HOP', '24 hộp/thùng', 38500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('TRAGUNG-HOATAN-10', 'Trà gừng hòa tan hộp 10 gói', 'Cà phê và trà', 'Trà hòa tan', NULL, 'HOP', '24 hộp/thùng', 34500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
-- Đồ dùng nhà bếp/Màng bọc thực phẩm
('MANGBOC-30X100', 'Màng bọc thực phẩm PE 30cm x 100m', 'Đồ dùng nhà bếp', 'Màng bọc thực phẩm', NULL, 'CUON', '20 cuộn/thùng', 48500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
('MANGBOC-30X30', 'Màng bọc thực phẩm PE 30cm x 30m', 'Đồ dùng nhà bếp', 'Màng bọc thực phẩm', NULL, 'CUON', '20 cuộn/thùng', 22500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
('MANGBOC-HOP-LON', 'Màng bọc thực phẩm cuộn lớn 45cm x 100m', 'Đồ dùng nhà bếp', 'Màng bọc thực phẩm', NULL, 'CUON', '20 cuộn/thùng', 72500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
-- Đồ dùng nhà bếp/Giấy bạc
('GIAYBAC-30X5', 'Giấy bạc nướng 30cm x 5m', 'Đồ dùng nhà bếp', 'Giấy bạc', NULL, 'CUON', '20 cuộn/thùng', 22500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
('GIAYBAC-30X10', 'Giấy bạc nướng 30cm x 10m', 'Đồ dùng nhà bếp', 'Giấy bạc', NULL, 'CUON', '20 cuộn/thùng', 38500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
('GIAYNUONG-30X10', 'Giấy nến nướng 30cm x 10m', 'Đồ dùng nhà bếp', 'Giấy bạc', NULL, 'CUON', '20 cuộn/thùng', 28500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
-- Đồ dùng nhà bếp/Túi zip
('TUIZIP-S-20', 'Túi zip nhỏ gói 20 cái', 'Đồ dùng nhà bếp', 'Túi zip', NULL, 'GOI', '48 gói/thùng', 12500, 'ACTIVE', 'THUNG', 48, NULL, NULL),
('TUIZIP-M-20', 'Túi zip trung gói 20 cái', 'Đồ dùng nhà bếp', 'Túi zip', NULL, 'GOI', '48 gói/thùng', 18500, 'ACTIVE', 'THUNG', 48, NULL, NULL),
('TUIZIP-L-15', 'Túi zip lớn gói 15 cái', 'Đồ dùng nhà bếp', 'Túi zip', NULL, 'GOI', '48 gói/thùng', 22500, 'ACTIVE', 'THUNG', 48, NULL, NULL),
-- Đồ dùng nhà bếp/Miếng rửa chén
('SCOTCH-XANH-3', 'Miếng rửa chén Scotch-Brite xanh', 'Đồ dùng nhà bếp', 'Miếng rửa chén', NULL, 'CAI', '120 cái/thùng', 6500, 'ACTIVE', 'THUNG', 120, NULL, NULL),
('COSMOS-BOT-BIEN', 'Miếng rửa chén bọt biển Cosmos', 'Đồ dùng nhà bếp', 'Miếng rửa chén', NULL, 'CAI', '120 cái/thùng', 4500, 'ACTIVE', 'THUNG', 120, NULL, NULL),
('COSMOS-INOX-3', 'Cọ rửa chén sợi inox bộ 3', 'Đồ dùng nhà bếp', 'Miếng rửa chén', NULL, 'CAI', '120 cái/thùng', 12500, 'ACTIVE', 'THUNG', 120, NULL, NULL),
-- Đồ dùng nhà bếp/Găng tay
('GANGTAY-CAOSU-M', 'Găng tay cao su rửa chén size M', 'Đồ dùng nhà bếp', 'Găng tay', NULL, 'CAI', '24 cái/thùng', 18500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('GANGTAY-NYLON-100', 'Găng tay nilon dùng một lần 100 cái', 'Đồ dùng nhà bếp', 'Găng tay', NULL, 'HOP', '24 hộp/thùng', 22500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('GANGTAY-Y-TE-100', 'Găng tay y tế latex hộp 100 cái', 'Đồ dùng nhà bếp', 'Găng tay', NULL, 'HOP', '24 hộp/thùng', 78500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
-- Đồ dùng nhà bếp/Dụng cụ vệ sinh
('CHOIQUET-NHA', 'Chổi quét nhà cán nhôm', 'Đồ dùng nhà bếp', 'Dụng cụ vệ sinh', NULL, 'CAI', '24 cái/thùng', 38500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('CAY-LAU-NHA-360', 'Cây lau nhà xoay 360 độ', 'Đồ dùng nhà bếp', 'Dụng cụ vệ sinh', NULL, 'CAI', '24 cái/thùng', 168000, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('XONHUA-12L', 'Xô nhựa 12 lít có quai', 'Đồ dùng nhà bếp', 'Dụng cụ vệ sinh', NULL, 'CAI', '24 cái/thùng', 32500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('HOT-RAC-NHUA', 'Hót rác nhựa có cán', 'Đồ dùng nhà bếp', 'Dụng cụ vệ sinh', NULL, 'CAI', '24 cái/thùng', 18500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
-- Hàng tiêu dùng khác/Pin
('PANASONIC-AA-4', 'Pin Panasonic AA vỉ 4 viên', 'Hàng tiêu dùng khác', 'Pin', NULL, 'VI', '20 vỉ/thùng', 32500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
('PANASONIC-AAA-4', 'Pin Panasonic AAA vỉ 4 viên', 'Hàng tiêu dùng khác', 'Pin', NULL, 'VI', '20 vỉ/thùng', 32500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
('ENERGIZER-AA-4', 'Pin Energizer AA vỉ 4 viên', 'Hàng tiêu dùng khác', 'Pin', NULL, 'VI', '20 vỉ/thùng', 58500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
('MAXELL-CR2032', 'Pin cúc áo Maxell CR2032', 'Hàng tiêu dùng khác', 'Pin', NULL, 'CAI', '20 cái/thùng', 22500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
-- Hàng tiêu dùng khác/Bật lửa
('BATLUA-FLAMINGO', 'Bật lửa Flamingo', 'Hàng tiêu dùng khác', 'Bật lửa', NULL, 'CAI', '50 cái/thùng', 4500, 'ACTIVE', 'THUNG', 50, NULL, NULL),
('BATLUA-ZIPPO-STD', 'Bật lửa gas dùng nhiều lần', 'Hàng tiêu dùng khác', 'Bật lửa', NULL, 'CAI', '50 cái/thùng', 12500, 'ACTIVE', 'THUNG', 50, NULL, NULL),
('BATLUA-BEP', 'Bật lửa bếp gas cán dài', 'Hàng tiêu dùng khác', 'Bật lửa', NULL, 'CAI', '50 cái/thùng', 18500, 'ACTIVE', 'THUNG', 50, NULL, NULL),
-- Hàng tiêu dùng khác/Khẩu trang
('KHAUTRANG-YTE-50', 'Khẩu trang y tế 4 lớp hộp 50 cái', 'Hàng tiêu dùng khác', 'Khẩu trang', NULL, 'HOP', '40 hộp/thùng', 28500, 'ACTIVE', 'THUNG', 40, NULL, NULL),
('KHAUTRANG-N95-10', 'Khẩu trang N95 hộp 10 cái', 'Hàng tiêu dùng khác', 'Khẩu trang', NULL, 'HOP', '40 hộp/thùng', 85000, 'ACTIVE', 'THUNG', 40, NULL, NULL),
('KHAUTRANG-VAI-3', 'Khẩu trang vải kháng khuẩn gói 3 cái', 'Hàng tiêu dùng khác', 'Khẩu trang', NULL, 'GOI', '40 gói/thùng', 22500, 'ACTIVE', 'THUNG', 40, NULL, NULL),
-- Hàng tiêu dùng khác/Đồ dùng tiện ích
('DEN-PIN-LED', 'Đèn pin LED cầm tay', 'Hàng tiêu dùng khác', 'Đồ dùng tiện ích', NULL, 'CAI', '24 cái/thùng', 38500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('O-CAM-DIEN-3L', 'Ổ cắm điện 3 lỗ dây 3m', 'Hàng tiêu dùng khác', 'Đồ dùng tiện ích', NULL, 'CAI', '24 cái/thùng', 68500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('BONG-LED-9W', 'Bóng đèn LED 9W', 'Hàng tiêu dùng khác', 'Đồ dùng tiện ích', NULL, 'CAI', '50 cái/thùng', 22500, 'ACTIVE', 'THUNG', 50, NULL, NULL),
('BANG-KEO-TRONG', 'Băng keo trong 5cm x 100 yard', 'Hàng tiêu dùng khác', 'Đồ dùng tiện ích', NULL, 'CUON', '72 cuộn/thùng', 14500, 'ACTIVE', 'THUNG', 72, NULL, NULL),
-- Thực phẩm đông lạnh/Xúc xích
('VISSAN-XX-HEO-500', 'Xúc xích heo Vissan gói 500g', 'Thực phẩm đông lạnh', 'Xúc xích', NULL, 'GOI', '20 gói/thùng', 78500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
('CPFOODS-XX-GA-450', 'Xúc xích gà CP gói 450g', 'Thực phẩm đông lạnh', 'Xúc xích', NULL, 'GOI', '20 gói/thùng', 68500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
('DUCVIET-XX-TIETTRUNG-200', 'Xúc xích Đức Việt tiệt trùng 200g', 'Thực phẩm đông lạnh', 'Xúc xích', NULL, 'GOI', '20 gói/thùng', 38500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
('LEGEND-XX-XONGKHOI-175', 'Xúc xích xông khói Legend gói 175g', 'Thực phẩm đông lạnh', 'Xúc xích', NULL, 'GOI', '20 gói/thùng', 52500, 'DISCONTINUED', 'THUNG', 20, NULL, NULL),
-- Thực phẩm đông lạnh/Chả cá
('CHACA-THATLAT-500', 'Chả cá thát lát Vĩnh Hoàn 500g', 'Thực phẩm đông lạnh', 'Chả cá', NULL, 'GOI', '20 gói/thùng', 98500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
('CHACA-VIEN-500', 'Cá viên chiên CP gói 500g', 'Thực phẩm đông lạnh', 'Chả cá', NULL, 'GOI', '20 gói/thùng', 62500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
('CHACA-HAINAM-300', 'Chả cá Hải Nam gói 300g', 'Thực phẩm đông lạnh', 'Chả cá', NULL, 'GOI', '20 gói/thùng', 52500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
-- Thực phẩm đông lạnh/Viên thả lẩu
('TOMVIEN-CP-500', 'Tôm viên CP gói 500g', 'Thực phẩm đông lạnh', 'Viên thả lẩu', NULL, 'GOI', '20 gói/thùng', 88500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
('BOVIEN-VISSAN-500', 'Bò viên Vissan gói 500g', 'Thực phẩm đông lạnh', 'Viên thả lẩu', NULL, 'GOI', '20 gói/thùng', 82500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
('CAVIEN-TUOI-500', 'Cá viên thả lẩu gói 500g', 'Thực phẩm đông lạnh', 'Viên thả lẩu', NULL, 'GOI', '20 gói/thùng', 58500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
('TAUHU-CA-300', 'Tàu hũ cá thả lẩu gói 300g', 'Thực phẩm đông lạnh', 'Viên thả lẩu', NULL, 'GOI', '20 gói/thùng', 42500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
-- Thực phẩm đông lạnh/Thực phẩm chế biến sẵn
('NUGGET-GA-CP-400', 'Gà viên chiên Nugget CP gói 400g', 'Thực phẩm đông lạnh', 'Thực phẩm chế biến sẵn', NULL, 'GOI', '20 gói/thùng', 72500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
('GIOCHA-DONGLANH-500', 'Giò chả đông lạnh 500g', 'Thực phẩm đông lạnh', 'Thực phẩm chế biến sẵn', NULL, 'GOI', '20 gói/thùng', 68500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
('BANHBAO-NHANTHIT-300', 'Bánh bao nhân thịt gói 300g', 'Thực phẩm đông lạnh', 'Thực phẩm chế biến sẵn', NULL, 'GOI', '20 gói/thùng', 38500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
('HAKAO-TOM-300', 'Há cảo tôm gói 300g', 'Thực phẩm đông lạnh', 'Thực phẩm chế biến sẵn', NULL, 'GOI', '20 gói/thùng', 58500, 'ACTIVE', 'THUNG', 20, NULL, NULL),
-- Đồ dùng cho trẻ em/Tã, bỉm
('BOBBY-M-60', 'Tã dán Bobby size M 60 miếng', 'Đồ dùng cho trẻ em', 'Tã, bỉm', NULL, 'GOI', '4 gói/thùng', 165000, 'ACTIVE', 'THUNG', 4, NULL, NULL),
('BOBBY-L-54', 'Tã dán Bobby size L 54 miếng', 'Đồ dùng cho trẻ em', 'Tã, bỉm', NULL, 'GOI', '4 gói/thùng', 165000, 'ACTIVE', 'THUNG', 4, NULL, NULL),
('PAMPERS-M-52', 'Tã dán Pampers size M 52 miếng', 'Đồ dùng cho trẻ em', 'Tã, bỉm', NULL, 'GOI', '4 gói/thùng', 248000, 'ACTIVE', 'THUNG', 4, NULL, NULL),
('HUGGIES-L-50', 'Tã quần Huggies size L 50 miếng', 'Đồ dùng cho trẻ em', 'Tã, bỉm', NULL, 'GOI', '4 gói/thùng', 235000, 'ACTIVE', 'THUNG', 4, NULL, NULL),
('MOONY-NB-60', 'Tã dán Moony size NB 60 miếng', 'Đồ dùng cho trẻ em', 'Tã, bỉm', NULL, 'GOI', '4 gói/thùng', 275000, 'DISCONTINUED', 'THUNG', 4, NULL, NULL),
-- Đồ dùng cho trẻ em/Khăn ướt trẻ em
('BOBBY-KUTE-100', 'Khăn ướt trẻ em Bobby 100 tờ', 'Đồ dùng cho trẻ em', 'Khăn ướt trẻ em', NULL, 'GOI', '24 gói/thùng', 28500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('PAMPERS-KUTE-80', 'Khăn ướt trẻ em Pampers 80 tờ', 'Đồ dùng cho trẻ em', 'Khăn ướt trẻ em', NULL, 'GOI', '24 gói/thùng', 42500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('ANTOAN-KUTE-50', 'Khăn ướt trẻ em An Toàn 50 tờ', 'Đồ dùng cho trẻ em', 'Khăn ướt trẻ em', NULL, 'GOI', '24 gói/thùng', 18500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
-- Đồ dùng cho trẻ em/Sữa trẻ em
('DIELAC-ALPHA-380', 'Sữa bột Dielac Alpha Gold 2 hộp 380g', 'Đồ dùng cho trẻ em', 'Sữa trẻ em', NULL, 'HOP', '6 hộp/thùng', 118000, 'ACTIVE', 'THUNG', 6, NULL, NULL),
('ENFAGROW-3-870', 'Sữa bột Enfagrow A+ số 3 hộp 870g', 'Đồ dùng cho trẻ em', 'Sữa trẻ em', NULL, 'HOP', '6 hộp/thùng', 565000, 'ACTIVE', 'THUNG', 6, NULL, NULL),
('NAN-OPTIPRO-4-800', 'Sữa bột Nan Optipro số 4 hộp 800g', 'Đồ dùng cho trẻ em', 'Sữa trẻ em', NULL, 'HOP', '6 hộp/thùng', 465000, 'ACTIVE', 'THUNG', 6, NULL, NULL),
('FRISO-GOLD-3-900', 'Sữa bột Friso Gold số 3 hộp 900g', 'Đồ dùng cho trẻ em', 'Sữa trẻ em', NULL, 'HOP', '6 hộp/thùng', 535000, 'ACTIVE', 'THUNG', 6, NULL, NULL),
('ABBOTT-GROW-900', 'Sữa bột Similac Gold số 4 hộp 900g', 'Đồ dùng cho trẻ em', 'Sữa trẻ em', NULL, 'HOP', '6 hộp/thùng', 545000, 'DISCONTINUED', 'THUNG', 6, NULL, NULL),
-- Đồ dùng cho trẻ em/Đồ ăn dặm
('BOTAN-DAM-GAO-200', 'Bột ăn dặm Nestlé Cerelac gạo sữa 200g', 'Đồ dùng cho trẻ em', 'Đồ ăn dặm', NULL, 'GOI', '24 gói/thùng', 62500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('NGUCOC-ANDAM-MIX-250', 'Ngũ cốc ăn dặm Gerber 250g', 'Đồ dùng cho trẻ em', 'Đồ ăn dặm', NULL, 'GOI', '24 gói/thùng', 98500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('CHAO-TUOI-SAMYANG', 'Cháo tươi ăn dặm Cây Thị gói 240g', 'Đồ dùng cho trẻ em', 'Đồ ăn dặm', NULL, 'GOI', '24 gói/thùng', 21500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
('BANHAN-DAM-GERBER', 'Bánh ăn dặm Gerber 35g', 'Đồ dùng cho trẻ em', 'Đồ ăn dặm', NULL, 'GOI', '24 gói/thùng', 52500, 'ACTIVE', 'THUNG', 24, NULL, NULL),
-- Đồ dùng cho trẻ em/Chăm sóc trẻ em
('JOHNSON-TAMGOI-500', 'Sữa tắm gội Johnson''s Baby 500ml', 'Đồ dùng cho trẻ em', 'Chăm sóc trẻ em', NULL, 'CHAI', '12 chai/thùng', 98500, 'ACTIVE', 'THUNG', 12, NULL, NULL),
('LACTACYD-BABY-250', 'Sữa tắm Lactacyd Baby 250ml', 'Đồ dùng cho trẻ em', 'Chăm sóc trẻ em', NULL, 'CHAI', '12 chai/thùng', 88500, 'ACTIVE', 'THUNG', 12, NULL, NULL),
('JOHNSON-PHAN-200', 'Phấn rôm Johnson''s Baby 200g', 'Đồ dùng cho trẻ em', 'Chăm sóc trẻ em', NULL, 'HOP', '12 hộp/thùng', 38500, 'ACTIVE', 'THUNG', 12, NULL, NULL),
('BEPANTHEN-30', 'Kem chống hăm Bepanthen tuýp 30g', 'Đồ dùng cho trẻ em', 'Chăm sóc trẻ em', NULL, 'TUYP', '24 tuýp/thùng', 128000, 'ACTIVE', 'THUNG', 24, NULL, NULL),
-- Thức ăn thú cưng/Thức ăn cho chó
('SMARTHEART-BO-1.5', 'Thức ăn chó SmartHeart vị bò 1,5kg', 'Thức ăn thú cưng', 'Thức ăn cho chó', NULL, 'GOI', '12 gói/thùng', 118000, 'ACTIVE', 'THUNG', 12, NULL, NULL),
('PEDIGREE-BO-400', 'Thức ăn chó Pedigree vị bò 400g', 'Thức ăn thú cưng', 'Thức ăn cho chó', NULL, 'GOI', '12 gói/thùng', 52500, 'ACTIVE', 'THUNG', 12, NULL, NULL),
('ANF-CHO-3KG', 'Thức ăn hạt cho chó Ganador 3kg', 'Thức ăn thú cưng', 'Thức ăn cho chó', NULL, 'BAO', '4 bao/thùng', 268000, 'ACTIVE', 'THUNG', 4, NULL, NULL),
-- Thức ăn thú cưng/Thức ăn cho mèo
('WHISKAS-CA-400', 'Thức ăn mèo Whiskas vị cá thu 400g', 'Thức ăn thú cưng', 'Thức ăn cho mèo', NULL, 'GOI', '12 gói/thùng', 58500, 'ACTIVE', 'THUNG', 12, NULL, NULL),
('ME-O-GOLD-1.2', 'Thức ăn mèo Me-O Gold 1,2kg', 'Thức ăn thú cưng', 'Thức ăn cho mèo', NULL, 'GOI', '12 gói/thùng', 138000, 'ACTIVE', 'THUNG', 12, NULL, NULL),
('CATSRANG-3KG', 'Thức ăn hạt mèo Cat''s Eye 3kg', 'Thức ăn thú cưng', 'Thức ăn cho mèo', NULL, 'BAO', '4 bao/thùng', 215000, 'ACTIVE', 'THUNG', 4, NULL, NULL);

INSERT INTO products (sku, name, category_id, base_unit_id, packing_spec, cost_price, status)
SELECT t.sku, t.name, COALESCE(c3.id, c2.id, c1.id), bu.id, t.packing_spec, t.cost_price, t.status
FROM tmp_demo_products t
JOIN units bu ON bu.code = t.base_unit
JOIN product_categories c1 ON c1.parent_id IS NULL AND c1.name = t.cat1
LEFT JOIN product_categories c2 ON c2.parent_id = c1.id AND c2.name = t.cat2
LEFT JOIN product_categories c3 ON c3.parent_id = c2.id AND c3.name = t.cat3
WHERE NOT EXISTS (SELECT 1 FROM products p WHERE p.sku = t.sku);

-- 4. Quy đổi đơn vị (lốc, thùng...) cho các sản phẩm demo
INSERT IGNORE INTO product_units (product_id, unit_id, factor)
SELECT p.id, u.id, t.conv1_factor FROM tmp_demo_products t
JOIN products p ON p.sku = t.sku JOIN units u ON u.code = t.conv1_unit WHERE t.conv1_unit IS NOT NULL;
INSERT IGNORE INTO product_units (product_id, unit_id, factor)
SELECT p.id, u.id, t.conv2_factor FROM tmp_demo_products t
JOIN products p ON p.sku = t.sku JOIN units u ON u.code = t.conv2_unit WHERE t.conv2_unit IS NOT NULL;

DROP TEMPORARY TABLE tmp_demo_products;

