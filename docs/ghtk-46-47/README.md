# S4-08 / GHTK-46 và S4-09 / GHTK-47

Hai chức năng đã có backend; đợt hoàn thiện bổ sung thao tác từ trang chi tiết đơn hàng, dùng layout và dialog chung của team.

## In hoặc xuất PDF

Ở `/orders/edit?id=...`, chọn **In / xem PDF** để mở PDF trên tab mới rồi in bằng trình xem PDF của trình duyệt, hoặc **Tải PDF** để tải tệp.

- Route mới `/orders/pdf?id=...` và `download=true` gọi cùng OrderPdfService với `/api/orders/pdf` hiện có.
- PDF tiếng Việt có mã đơn, đại lý/điểm giao/ngày giao/người lập/trạng thái, ghi chú, mã vạch Code 128, chi tiết số lượng/giá/chiết khấu/thành tiền và tổng. Đơn nháp có nhãn giá hiện hành; đơn chốt dùng giá snapshot.
- Bảng dài phân trang và lặp tiêu đề; có số trang. Tài khoản không được xem đơn nhận 404, không trả nội dung PDF; URL yêu cầu quyền đọc ORDER. Phản hồi no-store.
- Dùng OpenPDF/font đã có trong develop, không thêm dependency. Thông tin giấy phép trong docs/backend-s4/README.md giữ nguyên.

## Sao chép đơn

Chọn **Sao chép đơn**, đọc xác nhận rồi **Tạo đơn nháp mới**. POST `/orders/copy` có CSRF, gọi cùng OrderCopyService với API `/api/orders/copy`; thành công chuyển tới đơn mới.

- Cùng đại lý, giữ toàn bộ mặt hàng/đơn vị/số lượng, ghi chú và điểm giao. Ngày giao đã qua được bỏ trống để nhập lại.
- Trạng thái mới luôn DRAFT; không kế thừa trạng thái, giữ chỗ, giá chốt hoặc giá sửa tay. Giá và chiết khấu tính lại từ chính sách hiện hành.
- Thiếu giá, SKU/đơn vị không còn hợp lệ hoặc sai phạm vi thì không tạo bản sao dở dang. Đơn nguồn không thay đổi.
- Nút chỉ hiện khi có quyền ghi ORDER; server vẫn kiểm quyền, phạm vi đại lý và CSRF. API JSON giữ nguyên cho frontend khác.

## Kiểm chứng

Test giá mới/giá snapshot, nhiều dòng, reset giá sửa tay và ngày quá khứ, rollback bản sao, phạm vi đại lý, nội dung PDF tiếng Việt và nhiều trang. Kiểm tra thêm Servlet preview/download, redirect của POST giao diện, CSRF và ma trận quyền.

Đã chạy hai chức năng từ JSP thực trên Tomcat 10.1/MySQL 8.4 thử riêng. Không cần migration cho hai story này; migration 029 trong cùng PR chỉ thuộc S6-01.
