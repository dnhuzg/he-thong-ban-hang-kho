# SCRUM-22 — Backend gán vai trò, kho và địa bàn
Người thực hiện: NGUYEN DANH HUNG.

Kế thừa lưu nhiều vai trò/kho/địa bàn, bắt buộc có kho với vai trò kho và chặn
tự thu hồi vai trò Admin. Bổ sung kiểm tra ID kho/địa bàn trước khi ghi quan hệ;
ID không tồn tại trả lỗi nghiệp vụ và rollback toàn bộ cập nhật.
Đường dẫn tạo/sửa tài khoản chỉ cho Admin. Service nạp lại Admin từ database trong
transaction để quyền hoặc session đã bị thu hồi không thể dùng snapshot cũ để gán quyền.

Kiểm thử: UserAssignmentTest và AccessRulesTest — đa vai trò/đa kho, kho bắt buộc,
tự gỡ Admin, ID không hợp lệ, rollback và chống nâng quyền qua lời gọi Service.
Danh sách kho/địa bàn được nạp vào User mỗi request. Các API tồn kho/xuất kho chưa
có trong repository; khi nhóm triển khai chúng phải kiểm tra ID kho yêu cầu với
User.getWarehouses() cùng ma trận quyền. Phần này không tự tạo module kho ngoài ticket.
