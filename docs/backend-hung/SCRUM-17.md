# SCRUM-17 — Backend đổi mật khẩu
Người thực hiện: NGUYEN DANH HUNG.

Giữ quy tắc tối thiểu 8 ký tự, có chữ và số, nhập mật khẩu hiện tại và xác nhận.
Bổ sung khoá dòng trong transaction để hai yêu cầu đồng thời không cùng sử dụng
mật khẩu cũ. Kiểm tra lại trạng thái khoá trong transaction. Giới hạn 72 byte UTF-8
của bcrypt được kiểm tra rõ ràng thay vì để phần cuối mật khẩu bị cắt.
Đổi mật khẩu tăng session_version, ghi audit và kích hoạt tài khoản dùng mật khẩu
tạm; Servlet giữ phiên hiện tại bằng version mới, phiên khác bị AuthFilter từ chối.

Kiểm thử: ChangePasswordTest và PasswordsTest — mật khẩu hiện tại sai, mật khẩu
mới yếu/trùng/không khớp/quá giới hạn, phiên version, kích hoạt và thay đổi đồng thời.
