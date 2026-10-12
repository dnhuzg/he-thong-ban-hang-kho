# GHTK-55 · S5-07 · Lập phiếu chuyển kho nội bộ (phần FE)

Trang `/inventory/transfers` cho tài khoản có quyền xem Tồn kho. Tài khoản có quyền ghi có thể lưu nháp, bắt đầu chuyển và xác nhận số thực nhận trong bản mẫu của trình duyệt. Kho đi và kho đến phải khác nhau; mỗi SKU có một dòng, số lượng chuyển là số nguyên theo đơn vị cơ sở.

Khi bắt đầu chuyển, phiếu chuyển sang **Đang chuyển** và khóa kho, hàng, số lượng chuyển. Màn nhận hàng cho phép ghi số thực nhận từng SKU. Nhận đủ chuyển sang **Đã nhận đủ**; nhận thiếu hoặc thừa bắt buộc nhập lý do và chuyển sang **Có chênh lệch**. Phiếu đã xử lý chỉ xem.

**Giới hạn:** kho, sản phẩm và phiếu đều là dữ liệu mẫu; phiếu lưu bằng `localStorage` riêng cho người dùng trên trình duyệt đó. Không có API, giao dịch trừ kho đi/cộng kho đến, xác nhận của kho nhận thật, hay xử lý chênh lệch trên MySQL. Backend của nhóm cần cung cấp danh mục theo quyền, kiểm tồn kho đi, chống chuyển vượt tồn và chuyển cùng kho, ghi trạng thái hàng trên đường, phân quyền xác nhận tại kho đến, trừ/cộng tồn trong transaction và ghi nhật ký trước khi nghiệm thu nghiệp vụ theo AC.
