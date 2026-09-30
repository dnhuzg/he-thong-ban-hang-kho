# SCRUM-23 — Backend khoá/mở khoá tài khoản
Người thực hiện: NGUYEN DANH HUNG.

Kế thừa lý do khoá, tăng session_version, audit và danh sách đại lý cần bàn giao.
Bổ sung kiểm tra lại quyền Admin và phiên trong transaction; đường dẫn khoá/mở khoá
chỉ dành cho Admin. Kiểm tra trạng thái dưới khoá dòng để chặn lặp thao tác và audit.
Mở khoá tiếp tục tăng version: các phiên cũ không sống lại, phải đăng nhập mới.
Tài khoản dùng mật khẩu tạm trở về PENDING và vẫn buộc đổi mật khẩu.

Kiểm thử: UserLockTest — lý do bắt buộc/giới hạn, tự khoá, tài khoản mất quyền,
thu hồi phiên, không khôi phục phiên cũ, trạng thái PENDING, đại lý cần bàn giao,
không tồn tại và thao tác lặp. AccessRulesTest kiểm tra URL kể cả khi ma trận bị
cấu hình cấp toàn quyền USER_AUDIT cho vai trò không phải Admin.
