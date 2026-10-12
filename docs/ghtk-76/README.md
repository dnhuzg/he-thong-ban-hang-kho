# GHTK-76 — Lập phiếu trả hàng từ đại lý

## Phạm vi

- Nhân viên kinh doanh chọn hóa đơn, dòng hàng, số lượng, lý do và tình trạng hàng trả.
- Không cho trả vượt số lượng đã giao trừ số đã trả trước đó và các phiếu đang chờ/đã nhận.
- Sản phẩm có quản lý lô phải chọn đúng lô đã xuất. Hàng tốt được cộng vào tồn tốt; hàng hỏng được ghi vào tồn hàng lỗi.
- Chỉ Quản lý kho hoặc Admin mới có nút xác nhận nhận hàng. Tồn mẫu chỉ tăng sau bước này.
- Có mục menu riêng và giới hạn truy cập theo vai trò.

## Dữ liệu mẫu và giới hạn

Trang sử dụng hóa đơn, sản phẩm và số tồn mẫu ở `assets/js/returns.js`. Phiếu và số lượng mẫu được lưu bằng `localStorage` trên trình duyệt hiện tại để có thể chuyển vai trò thử quy trình trên cùng trình duyệt. Đây là bản FE; chưa gọi API và chưa ghi MySQL, nên dữ liệu mẫu không thay thế nghiệp vụ thật.

## Thử nhanh

1. Đăng nhập tài khoản Nhân viên kinh doanh, mở **Trả hàng từ đại lý**, chọn hóa đơn và dòng hàng.
2. Thử nhập số lượng lớn hơn số lượng còn được trả hoặc bỏ trống lý do để kiểm tra chặn.
3. Thêm dòng, lập phiếu. Trạng thái hiển thị **Chờ kho nhận**, tồn mẫu chưa tăng.
4. Đăng nhập vai trò Quản lý kho hoặc Admin trên cùng trình duyệt, xác nhận phiếu và kiểm tra tồn tốt / tồn lỗi.

## Tệp

- `SalesReturnPageServlet.java`: kiểm tra vai trò và mở trang.
- `returns.jsp`, `returns.css`, `returns.js`: giao diện và tương tác mẫu.
- `AccessRules.java`, `Menu.java`: khai báo URL và mục menu.
