# GHTK-77 — Điều chỉnh giảm trừ công nợ khi trả hàng

## Phạm vi FE

- Kế toán công nợ và Admin truy cập trang `/finance/return-credits`.
- Chỉ phiếu trả có trạng thái `received` từ luồng GHTK-76 mới đủ điều kiện.
- Chứng từ giữ liên kết tới phiếu trả và hóa đơn gốc.
- Giá trị giảm được tính bằng số lượng trả nhân đơn giá đã bán trên hóa đơn gốc.
- Sau khi lập chứng từ, công nợ giảm và hạn mức khả dụng mẫu tăng ngay.
- Hóa đơn gốc chỉ được tham chiếu và không bị chỉnh sửa.

## Dữ liệu mẫu

Trang đọc các phiếu đã xác nhận trong `localStorage` của GHTK-76. Nếu trình duyệt chưa có phiếu xác nhận, trang hiển thị một phiếu mẫu để kiểm tra giao diện. Chứng từ GHTK-77 được lưu trong `localStorage`; chưa gọi API hoặc cập nhật MySQL.

## Thử nhanh

1. Đăng nhập vai trò Kế toán công nợ hoặc Admin.
2. Mở **Giảm trừ công nợ trả hàng** và chọn một phiếu đủ điều kiện.
3. Đối chiếu hóa đơn gốc, giá bán, tổng giảm, công nợ và hạn mức khả dụng.
4. Bấm **Lập chứng từ giảm trừ**; kiểm tra phiếu biến mất khỏi danh sách chờ và chứng từ xuất hiện trong lịch sử.
