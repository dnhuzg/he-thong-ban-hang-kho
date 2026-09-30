-- Mẫu tạo user MySQL riêng cho ứng dụng. Chép thành database/local/create-app-user.sql (không commit),
-- thay <MAT_KHAU> bằng mật khẩu của bạn, chạy MỘT LẦN bằng tài khoản root (MySQL Workbench hoặc mysql CLI),
-- rồi điền cùng mật khẩu đó vào db.password trong src/main/resources/app.properties.
CREATE USER IF NOT EXISTS 'oms_app'@'localhost' IDENTIFIED BY '<MAT_KHAU>';
GRANT ALL PRIVILEGES ON oms.* TO 'oms_app'@'localhost';
