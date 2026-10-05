# GHTK-70 · S7-04 · Cảnh báo và khoá bán theo công nợ (phần FE)

## Có trong bản FE

- Màn `/finance/credit-alerts` chỉ dành cho người có quyền đọc module `INVOICE` và vai trò Kế toán, Quản lý kinh doanh hoặc Admin. Phân quyền được kiểm ở server trước khi render.
- Bảng theo đại lý: dư nợ, hạn mức, tuổi khoản nợ mở lâu nhất, số ngày cho phép, lý do gắn cờ và cách xử lý đơn mới theo lựa chọn mẫu.
- Bộ lọc đại lý có cờ, tìm mã/tên; tổng số đại lý có cờ, vượt hạn mức, quá ngày nợ và bình thường.
- Nút đánh giá lại và form mô phỏng thu tiền: trừ khoản nợ cũ trước, tính lại cờ ngay trên trang, chỉ tồn tại trong phiên mở trang hiện tại.

## Quy tắc mẫu

Gắn cờ nếu `dư nợ > hạn mức` hoặc `tuổi khoản nợ mở lâu nhất > số ngày nợ cho phép`. Một đại lý có thể có cả hai lý do. Người xem chọn một trong hai cách hiển thị cho đơn mới: `Chặn` hoặc `Cần duyệt`. Đây là trình diễn UI; không thay đổi cấu hình hệ thống.

## Phần BE chưa có

Các con số đang là dữ liệu mẫu trong JavaScript. Chưa có tác vụ tự chạy mỗi ngày, hook đánh giá lại sau khi ghi nhận thu tiền, cờ trong cơ sở dữ liệu, hay kiểm tra chặn/chuyển duyệt ở bước tạo/chốt đơn. Các điều kiện này phải được BE xử lý trong transaction và ở tầng server; FE không thể thực thi tiêu chí chặn bán thật. Khi BE trả dữ liệu công nợ và trạng thái đã tính, thay dữ liệu mẫu bằng dữ liệu có kiểm quyền và phạm vi người dùng.
