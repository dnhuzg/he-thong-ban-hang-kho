# S7-08 – Nhận nhắc hạn thanh toán tự động

## Phạm vi backend

Backend tự động tìm các hoá đơn còn đúng 3 ngày đến hạn hoặc đã quá hạn, sau đó gửi email và tạo thông báo trong ứng dụng cho tài khoản của đại lý.

Tác vụ chạy một lần mỗi ngày lúc 00:05 theo múi giờ của máy chủ. `PaymentReminderStartupListener` quản lý vòng đời scheduler để khi Tomcat dừng hoặc reload ứng dụng thì thread nền được dừng theo.

## Chống gửi trùng

`invoice_reminder_logs` có unique key `(invoice_id, reminder_date)`. Một hoá đơn chỉ có một nhật ký nhắc trong cùng một ngày. Bảng `notifications` cũng có unique key `(recipient_user_id, invoice_id, reminder_date)` để bảo vệ thêm cho kênh thông báo trong ứng dụng.

Email chỉ được đánh dấu `email_sent = 1` sau khi `MailService.send(...)` trả về thành công. Thông báo trong ứng dụng được tạo trong transaction và nhật ký được đánh dấu `in_app_sent = 1` cùng transaction.

## Thành phần

- `PaymentReminderDao`: tìm hoá đơn đến hạn và quản lý nhật ký chống trùng.
- `NotificationDao`: tạo và đọc thông báo chưa đọc.
- `PaymentReminderService`: xử lý nghiệp vụ và gọi `MailService` sẵn có của project.
- `PaymentReminderScheduler`: lịch chạy hằng ngày.
- `PaymentReminderStartupListener`: khởi động/dừng scheduler theo Tomcat.

## Migration

`database/migrations/022_nhac_han_thanh_toan.sql` tạo `invoices`, `invoice_reminder_logs` và `notifications`.

`database/schema.sql` cũng được cập nhật để cài mới có đủ ba bảng.

## Kiểm thử

`PaymentReminderServiceTest` kiểm tra:

1. Nhắc trước hạn 3 ngày và không gửi trùng khi chạy lại trong cùng ngày.
2. Nhắc khi quá hạn.
3. Không nhắc hoá đơn đã thanh toán.