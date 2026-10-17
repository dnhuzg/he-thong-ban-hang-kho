# GHTK-72 · S7-06 · BE bổ sung

Áp dụng patch này trên nhánh tạo từ `develop` mới nhất (đã có `034_doi_chieu_cong_no.sql`; bản develop ngày 17/10 có các migration đến 036). Không áp dụng nguyên bộ code này bằng cách ghi đè dự án snapshot cũ ngày 03/10.

## Phạm vi

- Dùng lại `ReconciliationDao`, `ReconciliationService`, `ReconciliationPageServlet` và FE hiện hữu; không tạo API hay migration gốc trùng.
- Khi lập biên bản, lưu cả số dư đầu kỳ, phát sinh, số dư cuối kỳ và snapshot bất biến của các dòng giao dịch trong kỳ. Nếu phát sinh giao dịch ghi lùi ngày sau thời điểm chốt, biên bản đã lưu không đổi.
- Cổng đại lý chỉ cho người có role `CUSTOMER` xem/phản hồi, server ánh xạ `user_id` sang đúng `customer_id`; trạng thái chỉ có thể chuyển từ `PENDING` sang `ACCEPTED` hoặc `DISPUTED` một lần.
- Ghi audit khi lập biên bản/phản hồi. FE hiện tại giữ nguyên; nút Xuất PDF vẫn dùng hộp thoại in của trình duyệt để lưu PDF.
- Migration mới là `037_chi_tiet_bien_ban_doi_chieu.sql` vì develop hiện có migration 035 và 036.

## Áp dụng DB

- Database đang sử dụng: chạy `database/migrations/037_chi_tiet_bien_ban_doi_chieu.sql` sau migration 036.
- Cài mới: ngoài migration, áp dụng phần cập nhật `database/schema.sql` trong file `schema-sql-update.patch` (dùng `git apply schema-sql-update.patch` tại thư mục gốc repository).
- Không chạy `database/schema.sql` lên DB đang có dữ liệu vì file đó drop và tạo lại database.

## Kiểm thử

```powershell
mvn "-Dtest=ReconciliationServiceTest" test
mvn clean package
```

Kiểm thử bao phủ công thức số dư, chốt dòng giao dịch, không trùng kỳ, giới hạn dữ liệu trên portal, role phản hồi và audit.
