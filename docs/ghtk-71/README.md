# GHTK-71 · S7-05 · Thu tiền theo tuyến và nộp về công ty (FE)

Màn `/finance/route-collections` chỉ cho nhân viên kinh doanh, quản lý kinh doanh, kế toán công nợ và admin vào. Nhân viên và quản lý kinh doanh thấy form ghi khoản thu; kế toán và admin thấy nút xác nhận đã nhận tiền. Một tài khoản có nhiều vai trò có thể thấy cả hai. Kiểm tra vai trò nằm trong servlet trước khi render.

- Form thu tiền tại đại lý: ngày, đại lý, số tiền và ảnh biên lai PNG/JPG/WebP tối đa 2 MB. Khoản mới mang trạng thái **Chờ nộp**. Có thể xem ảnh đã chọn.
- Chỉ tài khoản kế toán hoặc admin được bấm **Xác nhận đã nhận** trên bản mẫu; lúc đó trạng thái chuyển thành **Đã nộp** và tổng đối chiếu được tính lại.
- Lọc theo ngày thu, nhân viên, trạng thái; tổng hợp theo từng nhân viên và từng ngày: số khoản, đã thu, đã nộp, còn chờ.
- Dữ liệu khởi tạo là dữ liệu mẫu. Nhân viên thường chỉ thấy các dòng gắn với tên mình trong bản mẫu; người xác nhận thấy thêm dòng của nhân viên tuyến B.

## Ranh giới FE / BE

Dữ liệu mới và ảnh biên lai chỉ nằm trong bộ nhớ trình duyệt cho tới khi tải lại trang. Những dòng mẫu ban đầu không có ảnh. Chưa có upload, ghi cơ sở dữ liệu, chứng từ thật, luồng nộp tiền, quyền dữ liệu theo tuyến thực tế, hay xác nhận kế toán trên server. Backend cần cung cấp API lấy đại lý thuộc tuyến, tạo khoản thu kèm tệp, danh sách theo quyền/phạm vi và API kế toán xác nhận; mọi quyền, kích thước/kiểu ảnh, trạng thái và đối chiếu tiền phải được kiểm tra lại trên server.
