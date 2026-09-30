# SCRUM-6 — Backend đăng nhập
Người thực hiện: NGUYEN DANH HUNG.

Kế thừa luồng Servlet → Service → DAO, bcrypt và session của dự án chung.
Bổ sung transaction với khoá dòng tài khoản để nhiều yêu cầu đăng nhập đồng thời
không làm mất bộ đếm sai mật khẩu. Lỗi đăng nhập được trả ra sau commit để bộ đếm
không bị rollback. Đăng nhập thành công và audit được commit cùng nhau.
Tài khoản bị quản trị khoá không được đăng nhập kể cả khi mật khẩu đúng.

Kiểm thử: AuthLoginTest (đăng nhập đúng, sai/không tồn tại, 5 yêu cầu đồng thời, tài khoản khoá).
Database H2 trong bộ nhớ chỉ phục vụ test; production tiếp tục dùng MySQL.
Description SCRUM-6 trên Jira hiện lặp tiêu chí khoá của SCRUM-23; triển khai theo
summary đăng nhập và đồng thời đảm bảo điều kiện khoá tài khoản.
