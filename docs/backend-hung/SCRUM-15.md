# SCRUM-15 — Backend phiên đăng nhập
Người thực hiện: NGUYEN DANH HUNG.

Kế thừa timeout 30 phút, ping khi có hoạt động và POST logout có CSRF.
Bổ sung huỷ session khi tài khoản đã bị xoá, cùng với kiểm tra trạng thái khoá
và session_version đang có. Các phản hồi ngoài assets dùng Cache-Control: no-store
để hạn chế hiển thị lại dữ liệu được bảo vệ sau khi đăng xuất.
Ping của phiên hết hạn/bị thu hồi trả HTTP 401 và không tạo session mới.

Kiểm thử: AuthFilterTest, SessionServletTest — session đúng/sai version, tài khoản
xoá/khoá, bắt đổi mật khẩu tạm, ping và huỷ session khi logout.
Gia hạn thời gian không hoạt động do Tomcat quản lý khi request/ping đi qua session;
test Servlet trong bộ nhớ không mô phỏng đồng hồ timeout của Tomcat.
