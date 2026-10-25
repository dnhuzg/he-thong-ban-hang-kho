# Hệ thống bán hàng & kho (OMS)

Ứng dụng web quản lý bán hàng và kho cho nhà phân phối bán sỉ — dự án thực tập Nhóm 3.
Backlog, sprint và người phụ trách: Jira project **GHTK** (https://dnhunq.atlassian.net).

**Công nghệ:** Java 17 (chạy trên JDK 21), Jakarta Servlet 6 + JSP/JSTL, Tomcat 10.1, MySQL 8 (JDBC), Maven 3.9.

## Cài đặt lần đầu

Ngay sau khi clone, bật hook chặn push thẳng lên `main` / `develop` (chạy một lần trong thư mục repo):
`git config core.hooksPath .githooks`

1. Cài **JDK 21**, **MySQL 8**, tải **Maven 3.9** và **Tomcat 10.1** (bản zip, giải nén là dùng được).
   Nếu không cài vào vị trí mặc định trong `run.ps1`, đặt biến môi trường `JAVA_HOME`, `MAVEN_HOME`, `CATALINA_HOME`.
2. Tạo user MySQL cho ứng dụng: chép `database/create-app-user.example.sql` thành `database/local/create-app-user.sql`,
   thay mật khẩu, chạy bằng tài khoản root (MySQL Workbench).
3. Tạo database: chạy lần lượt `database/schema.sql` rồi `database/seed.sql` bằng user `oms_app`
   (MySQL Workbench: File → Open SQL Script → Execute). **Lưu ý: `schema.sql` xoá và tạo lại database `oms`.**
4. Chép `src/main/resources/app.properties.example` thành `app.properties` và điền:
   `db.password` (mật khẩu bước 2), `admin.initialPassword` (mật khẩu tạm cho tài khoản `admin`, ≥ 8 ký tự, có chữ và số).
   Muốn gửi email thật thì điền thêm `mail.username` / `mail.password` (Gmail App Password); để trống thì email chỉ ghi ra log.
   **Không commit `app.properties`** (đã có trong `.gitignore`).

## Chạy

```powershell
.\run.ps1              # build + test + chạy Tomcat, mở http://localhost:8080/oms
.\run.ps1 -SkipTests   # bỏ qua test cho nhanh
```

Lần chạy đầu, ứng dụng tự tạo tài khoản `admin` với `admin.initialPassword`; đăng nhập xong sẽ bị yêu cầu đổi mật khẩu.

Database đã tạo từ trước thì không chạy lại `schema.sql`; áp các file mới trong `database/migrations/` theo thứ tự số.

## Dữ liệu mẫu để test

Sau khi web đã chạy lần đầu (đã có tài khoản `admin`), chạy `database/demo/dairy_demo.sql` bằng user `oms_app`:

```powershell
Get-Content database\demo\dairy_demo.sql -Raw -Encoding UTF8 | & "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -uoms_app -p --default-character-set=utf8mb4
```

Script nạp dữ liệu một kho sữa: 4 nhà cung cấp, 35 sản phẩm kèm quy đổi hộp/lốc/thùng, giá ở 3 bảng giá,
tồn đầu kỳ ở cả 3 kho (12 phiếu nhập đã xác nhận, có lô và hạn dùng) và tài khoản demo cho từng vai trò.
Chạy lại nhiều lần không bị nhân đôi.

| Tài khoản | Vai trò | Phạm vi |
|---|---|---|
| `demo_quanly` | Quản lý kinh doanh | toàn hệ thống, duyệt đơn vượt hạn mức / dưới giá sàn |
| `demo_nvkd` | Nhân viên kinh doanh | đại lý DL001, DL002 |
| `demo_ketoan` | Kế toán công nợ | hoá đơn, thanh toán, công nợ |
| `demo_kho` | Thủ kho | kho đầu tiên |
| `demo_qlkho` | Quản lý kho | kho đầu tiên |
| `demo_daily` | Đại lý | cổng đặt hàng của DL001 |

Mật khẩu chung **`Demo@2026`** — chỉ dùng trên máy thử, không dùng cho hệ thống thật.
Gợi ý thử nhanh: đăng nhập `demo_nvkd` tạo đơn cho DL001 → `demo_quanly` duyệt (nếu cần) → `demo_kho` soạn và xuất kho
→ `demo_ketoan` lập hoá đơn, ghi nhận thanh toán.

Muốn đổi danh sách sản phẩm hay giá: sửa `database/demo/source/dairy_gen.js` rồi chạy
`node database/demo/source/dairy_gen.js database/demo/dairy_demo.sql`.

## Quy trình làm việc nhóm

1. Nhận việc trên Jira, tạo nhánh từ `develop` đặt tên theo mã issue, ví dụ `GHTK-24-quan-ly-san-pham`.
2. Commit message viết tiếng Việt, bắt đầu bằng mã Jira, ví dụ `GHTK-24: Thêm màn hình danh sách sản phẩm`.
3. Chạy `.\run.ps1` (có test) trước khi mở Pull Request vào `develop`; cần ít nhất một người review mới merge.
4. Không commit thẳng lên `main` / `develop`. Hook `.githooks/pre-push` chặn việc này trên máy;
   đừng sửa file trực tiếp trên trang web GitHub vì hook không chặn được thao tác trên web.

Quy ước code chi tiết (tầng Servlet → Service → DAO, bảo mật, giao diện): xem [CLAUDE.md](CLAUDE.md).
