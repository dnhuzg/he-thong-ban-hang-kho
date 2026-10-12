# CLAUDE.md

File này hướng dẫn Claude Code cách làm việc trong repo này. Hãy đọc kỹ trước khi bắt đầu bất kỳ tác vụ nào.

## Tổng quan dự án

- **Tên dự án:** Hệ thống bán hàng & kho (OMS) — dự án thực tập, Nhóm 3, 8 tuần (8 sprint × 1 tuần), 5 người fullstack.
- **Mục tiêu:** hệ thống quản lý bán hàng và kho trên nền web cho nhà phân phối bán sỉ cho vài trăm đại lý, thay cho Excel, sổ tay và Zalo. Một đơn hàng chỉ nhập **một lần** rồi tự chảy qua tồn kho → xuất hàng → công nợ, không phải gõ lại.
- **Nguyên tắc cốt lõi:** tồn kho, giá bán (giá sàn) và hạn mức công nợ phải được hệ thống **kiểm ngay tại thời điểm chốt đơn**, không phát hiện sau khi hàng đã đi. Đơn vượt hạn mức hoặc dưới giá sàn phải qua luồng duyệt của Quản lý kinh doanh.
- **Trạng thái:** đã có backend + giao diện JSP cho Sprint 1 (EP-01: tài khoản, phân quyền). Repo nhóm: https://github.com/dnhuzg/he-thong-ban-hang-kho (private). Ngày 2026-09-30 nhóm chốt dùng Java Servlet và làm lại lịch sử repo từ đầu (bản Node.js cũ không còn trên GitHub).
- **Tài liệu nghiệp vụ:** [HỆ THỐNG BÁN HÀNG & KHO.xlsx](HỆ%20THỐNG%20BÁN%20HÀNG%20&%20KHO.xlsx) gồm các sheet: Product Overview, User Roles (kèm ma trận phân quyền), Epics (EP-01 → EP-09), Product Backlog (76 user story), Sprint Plan. Đọc file này khi cần hiểu yêu cầu của một story/epic.

### Vai trò và phân quyền

7 vai trò: Customer (đại lý), Sales Rep, Sales Manager, Warehouse, WH Manager, Accountant, Admin. Ma trận quyền theo module nằm ở sheet "2. User Roles".

- Quyền phải kiểm ở **tầng server**, không chỉ ẩn trên giao diện.
- Customer và Sales Rep chỉ thấy dữ liệu của chính mình hoặc nhóm đại lý mình phụ trách.
- **Giá vốn và biên lợi nhuận chỉ Sales Manager (và Admin) được xem** — ràng buộc bắt buộc.

## Công nghệ

- **Backend:** Java Servlet (Jakarta/Java EE), chạy trên servlet container như Tomcat.
- **Frontend:** HTML + CSS (có thể thêm JavaScript thuần khi cần tương tác; không đưa framework vào nếu chưa được yêu cầu).
- **Database:** MySQL 8, truy cập qua JDBC.
- JDK 21 (biên dịch target 17), Tomcat 10.1, Maven 3.9, MySQL 8.

### Hướng dẫn theo tầng

**Backend (Java Servlet)**
- Tách lớp rõ ràng: Servlet (nhận request, trả response) → Service (logic nghiệp vụ) → DAO (truy cập DB) → Model/Entity.
- Servlet không chứa SQL và không chứa logic nghiệp vụ phức tạp.
- Luôn đặt encoding UTF-8 cho request và response để hiển thị đúng tiếng Việt.
- Xử lý lỗi rõ ràng: trả mã HTTP phù hợp, không để lộ stack trace ra người dùng.
- Đóng `Connection`, `PreparedStatement`, `ResultSet` bằng try-with-resources.
- Các thao tác thay đổi tồn kho hoặc công nợ (chốt đơn, xuất kho, thanh toán, trả hàng, điều chỉnh) phải chạy trong transaction và ghi nhật ký thao tác.

**Frontend (HTML + CSS)**
- Dùng HTML ngữ nghĩa (`header`, `nav`, `main`, `section`, `footer`, `form`, `label`...).
- CSS đặt trong file riêng, không viết style inline. Đặt tên class nhất quán (ví dụ BEM hoặc kebab-case).
- Giao diện phải responsive, kiểm tra được trên màn hình nhỏ (Sales Rep gõ đơn tại cửa hàng).
- Khai báo `<meta charset="UTF-8">` và `<meta name="viewport" ...>` ở mọi trang.
- Nếu dùng JSP để render dữ liệu động, không nhúng logic nghiệp vụ hay SQL vào JSP.

**Database (MySQL)**
- Dùng `utf8mb4` cho database và bảng để hỗ trợ tiếng Việt và emoji.
- Đặt tên bảng và cột theo `snake_case`; mỗi bảng có khóa chính rõ ràng.
- Khai báo khóa ngoại và index cho các cột hay tra cứu.
- Lưu script tạo schema và dữ liệu mẫu trong thư mục riêng (ví dụ `database/`), không sửa DB thủ công mà không ghi lại.
- **Bắt buộc dùng `PreparedStatement`** với tham số `?`, không nối chuỗi SQL để tránh SQL injection.
- Không hardcode thông tin kết nối DB trong code; đọc từ file cấu hình không commit hoặc biến môi trường.
- Tiền tệ dùng `DECIMAL`, không dùng `FLOAT`/`DOUBLE`.

## Cơ sở dữ liệu

Dữ liệu demo lớn (118 nhóm hàng nhiều cấp, 416 sản phẩm FMCG kèm quy đổi đơn vị) nằm ở `database/demo/demo_catalog.sql` (chạy lại an toàn, không đổi dữ liệu có sẵn); giá bán demo cho các sản phẩm đó ở `demo_prices.sql` (chạy sau `demo_catalog.sql`, để thử tạo đơn); gỡ cả hai bằng `demo_catalog_remove.sql`; nguồn có cấu trúc ở `database/demo/source/` (sửa `data.js` rồi chạy `gen.js`). Không phải migration, chỉ chạy khi cần dữ liệu thử.

Hiện dùng schema tự thiết kế cho Sprint 1 trong `database/schema.sql` + `seed.sql`. Thiết kế đầy đủ của nhóm là `OMS_schema_mysql.sql` (69 bảng) nhưng file này chưa có trong repo; khi có thì phải đối chiếu và chuyển sang.

## Môi trường

- Hệ điều hành: Windows 11. Shell chính là PowerShell; Bash (Git Bash) cũng dùng được.
- Dùng cú pháp đúng với shell đang chạy. PowerShell không có `&&`, `head`, `tail`, `rm -rf`...
- Đường dẫn dùng dấu `\` trong PowerShell, `/` trong Bash.
- Thư mục làm việc: `D:\He_thong_ban_hang_kho`.
- Máy chưa cài Python; đừng giả định có sẵn.

## Lệnh thường dùng

Công cụ cài dạng portable (không nằm trong PATH): JDK 21 `C:\Program Files\Java\jdk-21.0.12`, Maven `%USERPROFILE%\tools\apache-maven-3.9.16`, Tomcat 10.1 `%USERPROFILE%\tools\apache-tomcat-10.1.60`. MySQL 8 chạy dạng service `MySQL80`.

```powershell
# Build + test + deploy + chạy Tomcat (Ctrl+C để dừng) → http://localhost:8080/oms
.\run.ps1
.\run.ps1 -SkipTests

# Chỉ build / test
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.12"
& "$env:USERPROFILE\tools\apache-maven-3.9.16\bin\mvn.cmd" -q clean package
& "$env:USERPROFILE\tools\apache-maven-3.9.16\bin\mvn.cmd" test -Dtest=PermissionPolicyTest

# Tạo lại database từ đầu (XOÁ toàn bộ dữ liệu — hỏi người dùng trước)
# user oms_app, mật khẩu trong app.properties; truyền qua biến MYSQL_PWD, không đưa lên dòng lệnh
Get-Content database\schema.sql -Raw -Encoding UTF8 | & "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -uoms_app --default-character-set=utf8mb4
Get-Content database\seed.sql   -Raw -Encoding UTF8 | & "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -uoms_app --default-character-set=utf8mb4
```

- Cấu hình máy cá nhân ở `src/main/resources/app.properties` (không commit; mẫu: `app.properties.example`). File SQL tạo user `oms_app` ở `database/local/` (không commit).
- Khi database chưa có người dùng, ứng dụng tự tạo tài khoản `admin` với `admin.initialPassword`; đăng nhập lần đầu bị bắt đổi mật khẩu.
- Email (kích hoạt, đặt lại mật khẩu) gửi qua SMTP theo cấu hình `mail.*` trong `app.properties` (Gmail App Password). Chưa điền đủ host/username/password thì chỉ ghi ra log Tomcat với tiền tố `[EMAIL]`; gửi lỗi cũng ghi log mà không chặn thao tác.

## Cấu trúc thư mục

`README.md` hướng dẫn cài đặt cho thành viên mới. Maven webapp, Java 17 (chạy trên JDK 21), Servlet 6 / JSP 3.1 + JSTL, MySQL qua JDBC thuần, bcrypt.

```
src/main/java/com/oms/
├── config/     AppConfig (đọc app.properties), Db (kết nối + Db.inTransaction)
├── model/      User, Role, Module, PermissionLevel, NamedItem, PageResult
├── security/   PermissionPolicy (logic phân quyền thuần, có test), Passwords (bcrypt, luật mật khẩu), Tokens
├── dao/        mỗi bảng/nhóm bảng một DAO; mọi method nhận Connection để service gom transaction
├── service/    nghiệp vụ; lỗi hiển thị cho người dùng ném ServiceException
└── web/
    ├── filter/   EncodingFilter → AuthFilter → CsrfFilter (thứ tự khai trong web.xml)
    ├── servlet/  mỗi màn hình một servlet, kế thừa BaseServlet
    ├── AccessRules  bảng URL → quyền cần có
    └── Menu         dựng menu theo quyền
src/main/webapp/
├── assets/css, assets/js   giao diện (xem bên dưới)
└── WEB-INF/
    ├── web.xml
    └── views/   JSP: layout/*.jspf (khung dùng chung), auth/, account/, admin/, error/, home.jsp
database/        schema.sql + seed.sql (cài mới, XOÁ database), migrations/NNN_*.sql (áp lên DB đang có, không mất dữ liệu)
```

Những điểm cần đọc nhiều file mới thấy:
- **Phân quyền mặc định từ chối:** `AuthFilter` tra `AccessRules`. URL không có trong bảng trả 404 kể cả khi servlet tồn tại, nên **mỗi servlet mới phải thêm dòng vào `AccessRules`**. Ma trận vai trò × module lưu trong bảng `role_permissions`, cache trong `PermissionService` và nạp lại khi lưu. ADMIN luôn toàn quyền; quyền xem giá vốn cố định trong `PermissionPolicy.canViewCostPrice`.
- **Thu hồi phiên:** cột `users.session_version` được lưu cùng session lúc đăng nhập. `AuthFilter` nạp lại User từ DB ở mọi request; nếu version lệch hoặc tài khoản bị khoá thì huỷ phiên. Đổi mật khẩu, đặt lại mật khẩu hay khoá tài khoản chỉ cần tăng version.
- **Giữ phiên khi đang nhập liệu:** `app.js` gọi `GET /session/ping` 5 phút một lần nếu người dùng có thao tác; ping trả `X-CSRF-Token` để form ở tab cũ vẫn lưu được sau khi đăng nhập lại ở tab khác. Chưa đăng nhập thì ping trả 401 (không chuyển hướng).
- **Thay đổi schema:** sửa `schema.sql` (cho cài mới) VÀ thêm file `database/migrations/NNN_*.sql` idempotent cho DB đang chạy. Bảng `customers` hiện chỉ tối thiểu cho S1-10 (đếm đại lý cần bàn giao khi khoá nhân viên); EP-03 mở rộng.
- **CSRF:** mọi form POST phải có `<input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">`.
- **JSP:** luôn in dữ liệu bằng `<c:out>` (EL `${}` không tự escape). Trang mới dùng `<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>` … `app-bottom.jspf` và đặt `pageTitle` trước khi include. Flash message lưu trong session key `flash` (`BaseServlet.flash`).

### Giao diện (assets)

- `base.css`: design token (`:root`), reset, tiện ích. Muốn đổi màu hay khoảng cách thì sửa ở đây.
- `layout.css`: sidebar, topbar, thanh điều hướng nhanh trên điện thoại, responsive. `components.css`: nút, form, alert, card, bảng, badge, phân trang, dialog, danh sách / thẻ trên điện thoại, trang lỗi. `auth.css`: các trang trước đăng nhập (đăng nhập hai cột).
- `effects.css` + `js/effects.js` + `WEB-INF/views/effects/*.jspf`: **nơi duy nhất chứa hiệu ứng giao diện** (ô gạch chân nhãn nổi ở đăng nhập, nút sáng / tối và bộ màu tối). Hiệu ứng mới thêm vào cuối hai file này, không viết rải ở file khác. Bộ màu tối là khối `:root[data-theme="dark"]` trong `base.css`.
- `icons.css` + `assets/img/icons/*.svg`: biểu tượng tải từ Figma, dùng `<span class="icon icon--tên"></span>` (mặt nạ CSS, màu theo màu chữ). Thêm biểu tượng: lưu SVG vào `img/icons` và thêm một dòng `.icon--tên` trong `icons.css`.
- `landing.css` + `views/landing.jsp`: trang chờ cho khách chưa đăng nhập ở địa chỉ gốc `/` (đã đăng nhập thì `/` chuyển sang `/home`), theo thiết kế Figma của nhóm; màu riêng là các biến `--landing-*` trong `base.css` (xanh rêu `#005b52`).
- `js/app.js`: chỉ tương tác UI (menu trên màn hình nhỏ, hiện mật khẩu, `<dialog>` qua `data-dialog-open`, ô chọn bộ lọc `data-autosubmit`).
- CSS/JS gắn `?v=${assetVersion}` (đổi mỗi lần khởi động ứng dụng) nên không cần Ctrl+F5 sau khi triển khai.
- Thiết kế giao diện nằm trong file Figma của nhóm (đăng nhập, sản phẩm, form sản phẩm, bảng giá, người dùng; desktop và điện thoại). Làm giao diện chỉ đổi cách hiển thị, không đổi chức năng.
- Giao diện sáng, màu nhấn xanh rêu (`#005b52`, theo thiết kế Figma của nhóm). Chỉ dùng biến trong `base.css`, không hardcode mã màu ở file khác; không viết `style` inline.

## Quy ước code

- Code mới phải giống phong cách của code xung quanh: cách đặt tên, mật độ comment, idiom.
- Ưu tiên giải pháp đơn giản nhất đáp ứng yêu cầu. Không thêm abstraction, tính năng hay cấu hình mà chưa ai yêu cầu.
- Chỉ thêm comment khi giải thích *tại sao*, không giải thích *cái gì*.
- Thống nhất tiếng Việt: comment trong code, commit message, tài liệu và chữ trên giao diện viết tiếng Việt; tên lớp / hàm / biến / bảng / cột giữ tiếng Anh (hoặc snake_case không dấu cho DB), thuật ngữ kỹ thuật (token, session, CSRF...) giữ nguyên.
- Không để lại code chết, `System.out.println`/`console.log` debug hay TODO không rõ chủ.
- Không sửa những phần không liên quan đến tác vụ (không refactor, format lại hàng loạt).

## Quy trình làm việc

1. **Hiểu trước, sửa sau:** đọc file liên quan và tìm code có sẵn để tái sử dụng trước khi viết mới.
2. **Việc mơ hồ hoặc cần quyết định:** dừng lại và hỏi người dùng trước khi làm (xem mục "Hỏi lại người dùng" bên dưới). Việc nhỏ, rõ ràng thì làm luôn.
3. **Kiểm chứng:** sau khi sửa, chạy build/test/lint (khi có) để xác nhận. Nếu không chạy được thì nói rõ, không khẳng định "đã xong" khi chưa kiểm tra.
4. **Báo cáo trung thực:** test fail, bước bị bỏ qua hay điều chưa chắc chắn đều phải nói thẳng.

## Hỏi lại người dùng

**Luôn hỏi trước khi làm** khi gặp một trong các tình huống sau. Không tự đoán rồi làm tiếp.

- **Yêu cầu mơ hồ:** có nhiều cách hiểu hợp lý, thiếu thông tin cần thiết (ví dụ chưa rõ trường dữ liệu, luồng nghiệp vụ, giao diện mong muốn).
- **Cần đưa ra quyết định:** chọn giữa nhiều hướng thiết kế hoặc thư viện, thay đổi cấu trúc DB, thêm phụ thuộc mới, đổi kiến trúc, hay bất kỳ lựa chọn nào ảnh hưởng đến phần còn lại của dự án.
- **Thao tác khó hoàn tác:** xóa file hoặc dữ liệu, sửa/xóa bảng MySQL, ghi đè code đang có.

Cách hỏi:
- Hỏi ngắn gọn, cụ thể. Đưa ra 2–3 phương án kèm ưu nhược điểm ngắn và **nêu phương án bạn khuyên dùng**.
- Gom các câu hỏi liên quan vào một lần hỏi, không hỏi lắt nhắt từng câu.
- Không hỏi những việc có thể tự xác minh bằng cách đọc code hoặc file backlog, hoặc những việc đã có quy ước sẵn trong file này.
- Nhận được câu trả lời thì làm theo, không hỏi lại điều người dùng đã quyết.

## Git

- Nhánh `main` (bản ổn định) và `develop` (tích hợp). Làm việc trên nhánh riêng đặt theo mã Jira, ví dụ `GHTK-24-quan-ly-san-pham`, rồi mở Pull Request vào `develop`. Không commit thẳng lên `main` / `develop`.
- Repo private trên GitHub Free nên KHÔNG bật được bảo vệ nhánh; thay bằng hook `.githooks/pre-push` chặn push lên `main` / `develop` (mỗi máy chạy `git config core.hooksPath .githooks`). Không dùng `--no-verify` để lách hook.
- Commit message viết **tiếng Việt**, bắt đầu bằng mã Jira để Jira tự gắn commit vào issue, ví dụ `GHTK-24: Thêm màn hình danh sách sản phẩm`.
- Chỉ commit hoặc push khi người dùng yêu cầu.
- Không dùng `--no-verify`, `--force`, `reset --hard` hay các thao tác phá hủy khác nếu chưa được đồng ý.
- Commit message ngắn gọn, nêu rõ *tại sao* thay đổi.
- Không commit file bí mật (`.env`, khóa API, token, file cấu hình DB).

## Bảo mật

- Không hardcode secret, mật khẩu, token vào code hoặc file cấu hình được commit.
- Không gửi dữ liệu dự án hoặc thông tin cá nhân tới dịch vụ bên ngoài nếu chưa được yêu cầu.
- Validate dữ liệu đầu vào ở các ranh giới hệ thống (input người dùng, API ngoài).
- Chống SQL injection (dùng `PreparedStatement`) và XSS (escape dữ liệu khi in ra HTML/JSP, ví dụ `<c:out>`).
- Mật khẩu phải băm (bcrypt hoặc tương đương) trước khi lưu, không lưu dạng thô.

## Jira

Backlog được quản lý trên Jira qua connector Atlassian (claude.ai), không cần cấu hình trong repo.

- **Site:** https://dnhunq.atlassian.net — **Project:** `GHTK` (Hệ Thống Bán Hàng & Kho).
- Cấu trúc: 9 Epic (`EP-01`…`EP-09`) → 76 Story → Subtask. Summary của mỗi issue bắt đầu bằng mã trong file Excel, ví dụ `S1-04 – Đổi mật khẩu khi đang đăng nhập` là `GHTK-13`. Tìm issue theo mã đó bằng JQL `project = GHTK AND summary ~ "S1-04"`.
- Mỗi Story có user story và tiêu chí chấp nhận (AC) trong description, story point ở field "Story point estimate", sprint ở field "Sprint", ưu tiên MoSCoW ở label (`moscow-must`...).
- Jira là nguồn chính cho trạng thái, người phụ trách và sprint; file Excel chỉ là bản kế hoạch ban đầu.
- Trước khi code một story, đọc issue trên Jira để lấy AC mới nhất.
- Chỉ tạo, sửa, chuyển trạng thái hay comment trên Jira khi người dùng yêu cầu. Không tạo lại issue đã có.

## Figma (MCP)

Repo có cấu hình MCP Figma (remote server `https://mcp.figma.com/mcp`) trong [.mcp.json](.mcp.json).

- Khi người dùng gửi link `figma.com` hoặc yêu cầu triển khai thiết kế thành code, dùng các tool Figma (`get_design_context`, `get_screenshot`...) thay vì đoán.
- Bám sát design token, khoảng cách, màu và component có sẵn trong thiết kế và design system của dự án.

## Giao tiếp

- Trả lời bằng **tiếng Việt**, trừ khi người dùng viết bằng ngôn ngữ khác.
- Giữ nguyên thuật ngữ kỹ thuật, tên hàm/biến, đường dẫn và câu lệnh bằng tiếng Anh.
- Trả lời ngắn gọn, đi thẳng vào kết quả. Không lặp lại những gì đã nói.
- Khi nhắc tới file hoặc dòng code, dùng link markdown dạng [tên file](đường/dẫn) để bấm được.
