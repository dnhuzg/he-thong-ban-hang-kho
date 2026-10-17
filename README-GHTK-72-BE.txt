GHTK-72 / S7-06 - BE ONLY PATCH

Patch này dành cho repository mới nhất trên nhánh develop, sau các migration đến 036.
Không áp dụng bằng cách giải nén đè nguyên bộ lên ZIP snapshot cũ ngày 03/10.
Không có file JSP/CSS/JS và không thêm Servlet trùng; FE hiện có được giữ nguyên.

Các thay đổi:
- Snapshot dòng giao dịch của từng biên bản để dữ liệu chi tiết không đổi sau khi chốt.
- Kiểm tra role ở Service cho trang kế toán/cổng đại lý và giới hạn phản hồi vào biên bản thuộc đúng đại lý, chỉ một lần.
- Bổ sung migration 037 và test dịch vụ.
- FE tiếp tục dùng window.print() hiện có để Save as PDF.

Áp dụng ở thư mục gốc repo:
1. Tạo nhánh GHTK-72 từ develop mới nhất.
2. Copy các file và thư mục src/, database/migrations/, docs/ từ patch vào gốc repo.
3. Tại gốc repo chạy: git apply .\schema-sql-update.patch
4. Chạy: mvn "-Dtest=ReconciliationServiceTest" test
5. Chạy: mvn clean package
6. Kiểm tra git diff --check / git status --short trước khi commit.

Database đã có dữ liệu: áp database/migrations/037_chi_tiet_bien_ban_doi_chieu.sql sau migration 036.
Cài mới: áp schema-sql-update.patch để schema.sql cũng tạo các trường/bảng snapshot.
Không chạy schema.sql lên database đang dùng vì schema.sql drop và tạo lại database.
