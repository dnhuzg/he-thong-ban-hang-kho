# Quy trình đóng góp

Áp dụng cho cả nhóm. Quy tắc chung về code, Git và Jira nằm trong [CLAUDE.md](CLAUDE.md); file này tập trung vào việc **tránh xung đột và không làm hỏng code của nhau**.

## Quy trình một story

1. Cập nhật `develop`, rồi tạo nhánh mới từ đó, đặt tên theo mã Jira:
   ```
   git checkout develop
   git pull origin develop
   git checkout -b GHTK-30-ten-story
   ```
2. Làm và commit nhỏ, mỗi commit tự build được. Message tiếng Việt, bắt đầu bằng mã Jira, ví dụ `GHTK-30: Thêm chính sách chiết khấu`.
3. Trước khi mở PR, kéo `develop` mới nhất vào nhánh của mình và xử lý xung đột **trên máy mình**:
   ```
   git fetch origin
   git merge origin/develop
   mvn clean package
   ```
4. Push nhánh và mở Pull Request vào `develop` (không vào `main`). Điền checklist trong mẫu PR.
5. Chờ mục **CI** trên PR báo xanh rồi mới merge. Nếu đỏ, xem log, sửa và push lại.
6. Story nhỏ, PR nhỏ, merge trong ngày. Nhánh sống càng lâu càng dễ lệch.

Không push thẳng lên `main` hoặc `develop` (hook `.githooks/pre-push` chặn; bật bằng `git config core.hooksPath .githooks`). `main` chỉ nhận PR từ `develop` khi kết thúc sprint.

## Những chỗ dễ xung đột

Các file dưới đây cả nhóm đều hay thêm dòng vào. Hãy báo nhau trước khi sửa, và chỉ thêm phần của mình, không sắp xếp lại phần cũ.

| File | Bạn thêm gì |
|---|---|
| `web/AccessRules.java` | Một dòng cho mỗi URL của servlet mới |
| `model/AuditEntry.java` | Nhãn thao tác và loại đối tượng mới |
| `assets/css/components.css` | CSS mới, thêm ở cuối file trong khối có chú thích riêng |
| `database/schema.sql`, `database/seed.sql` | Bảng và dữ liệu mẫu mới |
| `src/test/.../support/DatabaseTest.java` | Bảng dùng cho test |

## Số migration

Số `NNN` trong `database/migrations/NNN_*.sql` phải duy nhất. Trước khi đặt số, xem thư mục trên `develop` đã có đến số nào và nhắn nhóm "mình lấy số 008". Migration phải chạy lại được nhiều lần mà không lỗi.

## Không nên làm

- Không format lại cả file, không đổi tên hàng loạt: mọi dòng thành "đã sửa" và gây xung đột không cần thiết.
- Không sửa phần không liên quan tới story đang làm.
- Không dùng `--force`, `--no-verify` hay `reset --hard` khi chưa hỏi cả nhóm.
- Không commit `app.properties`, mật khẩu hay khoá bí mật.

## Khi bị xung đột

Chạy `git merge origin/develop` trên nhánh của mình, mở các file có dấu `<<<<<<<`, `=======`, `>>>>>>>`, giữ phần cần của **cả hai bên**, xoá các dòng đánh dấu, chạy `mvn clean package`, rồi commit và push. Chưa rõ giữ phần nào thì hỏi người đã viết dòng còn lại.
