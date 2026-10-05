# GHTK-51 — Giao diện khai báo kho

Phạm vi bàn giao: giao diện FE cho kho, vị trí lưu trữ và gán đại lý vào kho.
Dữ liệu mẫu lưu trong localStorage của trình duyệt, tách theo context ứng dụng
và mã người dùng. Chưa kết nối dữ liệu kho/đại lý trong MySQL; chưa hoàn thành
nghiệp vụ backend của story.

## Mở màn hình

Chạy ứng dụng theo README gốc bằng `./run.ps1`, đăng nhập bằng tài khoản có
quyền đọc module INVENTORY, vào **Tồn kho & nhập kho → Kho & vị trí lưu trữ**.
Đường dẫn mặc định: `http://localhost:8080/oms/inventory/warehouses`.
Trang JSP cần chạy qua Tomcat, không mở trực tiếp bằng trình duyệt hoặc Live Server.

## Nội dung

- Danh sách kho: tìm kiếm, lọc trạng thái, phân trang, thêm và sửa dữ liệu mẫu.
- Vị trí lưu trữ: lọc theo kho, thêm/sửa khu vực và kệ, kiểm tra mã trùng.
- Đại lý theo kho: tìm kiếm, lọc kho và đổi kho được gán trong dữ liệu mẫu.
- Responsive, dùng layout, menu, icon và design token có sẵn của dự án.
- Nút đặt lại khôi phục dữ liệu mẫu của người dùng hiện tại trên trình duyệt.

## Tích hợp và phân quyền

`WarehousePageServlet` chỉ render trang GET và lấy quyền từ PermissionService.
AccessRules yêu cầu quyền đọc INVENTORY để truy cập; Menu chỉ hiện mục khi
người dùng có quyền tương ứng. Quyền ghi quyết định hiển thị các nút thao tác
demo trên FE. Không có endpoint lưu/xóa và không ghi dữ liệu nghiệp vụ lên server.

`warehouse-demo.js` cung cấp dữ liệu mẫu; `warehouses.js` xử lý tương tác.
Khi BE tiếp nhận, thay lớp dữ liệu mẫu bằng service/API hoặc dữ liệu render từ
server theo thống nhất của nhóm. BE phải kiểm tra quyền ghi, CSRF và validate
dữ liệu tại server cho từng thao tác; không dùng việc ẩn nút FE làm kiểm soát quyền.
Không thay schema, DAO, service nghiệp vụ hoặc ma trận quyền trong bản bàn giao này.

## Kiểm chứng

- Maven 3.9.9, JDK 17: `mvn package` thành công; 182 test qua, không lỗi/bỏ qua.
- Hai test mới kiểm tra URL/menu theo quyền và URL ghi chưa đăng ký bị từ chối.
- Chromium: kiểm tra bản HTML dựng từ JSP và layout chung ở 1440×1000 và
  390×844; thử thêm kho, vị trí, gán đại lý, lưu sau tải lại, chế độ chỉ xem,
  menu điện thoại và tràn ngang. Không phát hiện lỗi JavaScript trong lần chạy.
- Chưa kiểm tra đăng nhập, biên dịch JSP và luồng đầy đủ trên Tomcat/MySQL.
  Cần chạy `./run.ps1` trong môi trường nhóm và thử màn hình trước khi merge.

## File thay đổi

Sửa `AccessRules.java`, `Menu.java`; thêm `WarehousePageServlet.java`,
`warehouses.jsp`, `warehouses.css`, `warehouses.js`, `warehouse-demo.js`,
`WarehousePageAccessTest.java` và tài liệu này. Không thêm thư viện phụ thuộc.
