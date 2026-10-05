# GHTK-52 · S5-04 · Lập phiếu nhập kho từ nhà cung cấp (phần FE)

Truy cập `/inventory/receipts` bằng tài khoản có quyền xem Tồn kho. Tài khoản có quyền ghi có thể lập phiếu mẫu, lưu nháp và xác nhận trong trình duyệt. Vai trò chỉ xem không có nút ghi.

Giao diện có nhà cung cấp, số chứng từ, ngày nhập, kho, các dòng sản phẩm và số lượng theo đơn vị nhập; hệ số được hiển thị cùng số lượng quy đổi về đơn vị cơ sở. Với sản phẩm mẫu quản lý lô, số lô và hạn dùng bắt buộc. Phiếu mẫu sau xác nhận chỉ xem và không thể sửa.

**Giới hạn:** đây chỉ là FE. Nhà cung cấp, kho, sản phẩm và hệ số đều là dữ liệu mẫu; phiếu được lưu bằng `localStorage` riêng cho người dùng trên trình duyệt đó. Nút xác nhận không gọi API, không ghi MySQL, không cộng tồn hay tạo phiếu điều chỉnh thật. Backend của nhóm cần cung cấp danh mục thực, endpoint ghi nháp/xác nhận, giao dịch cộng tồn và cơ chế phiếu điều chỉnh trước khi nghiệm thu chức năng nghiệp vụ theo AC.
