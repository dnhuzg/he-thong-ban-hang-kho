## Nội dung
<!-- Làm gì, thuộc story nào (ví dụ GHTK-30) -->

## Trước khi mở PR, hãy kiểm tra
- [ ] Đã kéo `develop` mới nhất vào nhánh của mình (`git fetch origin` rồi `git merge origin/develop`) và đã xử lý xung đột nếu có
- [ ] `mvn clean package` chạy qua trên máy (build + test)
- [ ] Servlet mới đã thêm dòng vào `AccessRules`
- [ ] Đổi CSDL thì đã sửa `schema.sql`, `seed.sql` và thêm `database/migrations/NNN_*.sql` với số chưa ai dùng
- [ ] Không commit `app.properties`, mật khẩu hay khoá bí mật
- [ ] Commit bằng tiếng Việt, bắt đầu bằng mã Jira

## Cần làm sau khi merge
<!-- Ví dụ: chạy migration 008, cấu hình mới trong app.properties -->
