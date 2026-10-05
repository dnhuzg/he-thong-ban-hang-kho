# GHTK-72 · S7-06 · Biên bản đối chiếu công nợ định kỳ (phần FE)

- `/finance/reconciliations`: Kế toán công nợ và Admin chọn đại lý, kỳ để xem biên bản mẫu có số dư đầu kỳ, phát sinh tăng, thanh toán, cuối kỳ và bảng chi tiết. Chỉ hai vai trò này vào được trang.
- `/portal/reconciliations`: tài khoản vai trò Đại lý xem biên bản mẫu gắn với tên đăng nhập, xác nhận khớp hoặc gửi lý do chênh lệch. Không có lựa chọn xem đại lý khác. Servlet chặn người không có vai trò phù hợp ở server.
- Nút **Xuất PDF** mở giao diện in của trình duyệt; chọn **Save as PDF** để lưu bản xem trước có dòng đánh dấu chưa có giá trị xác nhận. Chưa sinh hoặc gửi file PDF trên máy chủ.
- Dữ liệu mẫu gồm kỳ tháng hiện tại và tháng trước. Số dư đầu tháng hiện tại lấy từ số dư cuối tháng trước trên cùng bộ mẫu. Phản hồi lưu trong bộ nhớ của trang và mất khi tải lại.

## Cần BE khi tích hợp

Truy vấn số dư, phát sinh và chứng từ theo đại lý/kỳ; liên kết tài khoản đại lý với mã đại lý và chặn dữ liệu ngoài phạm vi ở server; tạo và lưu biên bản có mã bất biến; xuất PDF từ dữ liệu đã chốt; gửi/thông báo đại lý; lưu xác nhận, lý do lệch, thời gian và người thao tác; xử lý thay đổi dữ liệu sau chốt, quy trình ký và đối soát. Bản FE này không thực hiện các nghiệp vụ đó.
