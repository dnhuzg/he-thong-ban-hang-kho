# Phần Backend — NGUYEN DANH HUNG

- Email tác giả commit: dtc225201515@ictu.edu.vn
- Jira: https://ictu-k11-n3.atlassian.net/jira/software/projects/SCRUM/boards/1/backlog
- Repository: https://github.com/dnhuzg/he-thong-ban-hang-kho
- Nhánh riêng: SCRUM-6-15-17-22-23-backend-hung
- Nhánh tích hợp của nhóm: develop
- Mã nguồn lưu trên máy: D:\N3_Thực tập\he-thong-ban-hang-kho

## Phạm vi công việc
Năm ticket đang giao cho NGUYEN DANH HUNG trên Jira SCRUM: 6, 15, 17, 22, 23.
Repository nhóm đã có Java Servlet/JDBC/JSP cho Sprint 1. Các commit ở nhánh này
là bổ sung và kiểm thử Backend trên code có sẵn; không nhận các commit gốc của nhóm
là phần tự viết mới. Mỗi ticket có tài liệu chi tiết trong thư mục này.
Các màn hình JSP và module nghiệp vụ khác tiếp tục dùng code của nhóm.
Không thay đổi schema production, không thao tác database của nhóm.

## Các endpoint hiện có phục vụ phần này
| Ticket | Endpoint | Hành vi |
|---|---|---|
| SCRUM-6 | POST /login | Xác minh bcrypt, chống mất bộ đếm sai khi đồng thời |
| SCRUM-15 | GET /session/ping, POST /logout | Gia hạn khi hoạt động, trả 401 khi hết phiên, huỷ phiên |
| SCRUM-17 | POST /account/change-password | Mật khẩu hiện tại, xác nhận, thu hồi phiên khác |
| SCRUM-22 | POST /admin/users/edit | Admin gán nhiều vai trò/kho/địa bàn, validate và transaction |
| SCRUM-23 | POST /admin/users/lock, /admin/users/unlock | Admin, lý do khoá, thu hồi phiên, cảnh báo bàn giao |

POST phải có _csrf của phiên. Các endpoint đang phục vụ giao diện JSP, phản hồi
redirect/HTML hoặc 204/401 cho ping theo thiết kế chung của nhóm.

## Build và kiểm thử tại máy này
Chạy PowerShell:
```powershell
$env:JAVA_HOME = 'D:/N3_Thực tập/tools/jdk-21.0.12.1+1'
& 'D:/N3_Thực tập/tools/apache-maven-3.9.16/bin/mvn.cmd' '-B' '-Dmaven.repo.local=D:/N3_Thực tập/tools/maven-cache' clean package
```
H2 scope test kiểm thử JDBC/transaction trong bộ nhớ; không được đóng gói vào WAR.
Production dùng MySQL như trước. Test không gửi email và không đọc credentials thật.
WAR sau build: target/oms.war. Triển khai thật cần app.properties riêng và Tomcat 10.1
theo README của dự án. Bộ kiểm thử này chưa xác nhận chạy end-to-end trên MySQL/Tomcat.
```
git log origin/develop..HEAD --oneline
git push -u origin SCRUM-6-15-17-22-23-backend-hung
```
Đưa vào develop bằng Pull Request của nhóm; không push trực tiếp vào main/develop.

## Kết quả xác nhận
Ngày 30/09/2026: Maven clean package thành công; 44 test chạy, không lỗi và không bỏ qua.
Kiểm tra WAR không chứa H2, class kiểm thử hoặc app.properties dùng trong test.
Báo cáo và log build nằm tại D:/N3_Thực tập.
