# GHTK-84 — Xem báo cáo công nợ quá hạn theo tuổi nợ

## Phạm vi

GHTK-84 được tích hợp vào trang sổ công nợ hiện có tại `/finance/debt-aging` của GHTK-69. Không thay trang bằng dữ liệu mẫu và không tạo bộ dữ liệu hardcode ở trình duyệt.

- Giữ nguyên sổ công nợ, bộ lọc ngày đối chiếu, từ khoá, nhóm tuổi và chỉ xem quá hạn.
- Tổng hợp số dư quá hạn theo nhóm tuổi của hoá đơn (0–30, 31–60, 61–90, trên 90 ngày tính từ ngày phát hành) từ dữ liệu SQL.
- Tổng hợp số dư quá hạn và số đại lý quá hạn theo nhân viên đang được gán trong `customers.sales_rep_id`.
- Xác định đại lý rủi ro cao nhất bằng SQL: số dư quá hạn lớn nhất; khi bằng nhau, ưu tiên khoản nợ thuộc nhóm tuổi cũ hơn, sau đó số ngày quá hạn dài hơn.
- Mở rộng tệp Excel hiện có thành các sheet: `Tuổi nợ`, `Tổng hợp quá hạn`, `Theo nhân viên`, `Rủi ro cao nhất`.
- Các phần tổng hợp và xếp hạng dùng cùng tập hoá đơn sau khi áp dụng bộ lọc hiện có; nếu đại lý chưa gán nhân viên, báo cáo hiển thị `Chưa phân công`.

## Thành phần sửa

- `DebtAgingDao`: truy vấn nhân viên phụ trách, tổng hợp quá hạn theo tuổi hoá đơn, tổng hợp theo nhân viên và xếp hạng đại lý rủi ro nhất bằng SQL.
- `DebtAgingService`: giữ nguyên nghiệp vụ sổ công nợ GHTK-69, kết hợp số liệu bổ sung vào report và xuất Excel nhiều sheet.
- `debt-aging.jsp` / `debt-aging.css`: bổ sung các khối tổng hợp vào chính trang báo cáo hiện tại; không dùng JavaScript để tạo dữ liệu báo cáo.
- `DebtAgingServiceTest`: kiểm tra nhóm tuổi nợ quá hạn, tổng hợp theo người phụ trách, đại lý rủi ro cao nhất, bộ lọc hoá đơn và các sheet Excel.

## Quy ước nghiệp vụ

- Số dư chưa thanh toán của hoá đơn là `total_amount - paid_amount`.
- Một hoá đơn là quá hạn tại ngày đối chiếu khi `due_date < asOf`, còn số dư chưa thanh toán dương và trạng thái không phải `CANCELLED`.
- Tuổi nhóm được tính theo số ngày từ `issued_at` đến ngày đối chiếu, giữ tương thích với sổ GHTK-69 hiện có.
- Báo cáo chỉ đọc; quyền xem vẫn được kiểm tra bằng `DebtAgingService` hiện có.

## Kiểm thử

Chạy `mvn clean package`. Cần kiểm thử thêm trên MySQL local/DB dùng chung để xác nhận kết quả thực tế và quyền truy cập trước khi merge PR.
