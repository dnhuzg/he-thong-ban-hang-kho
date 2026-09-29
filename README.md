# he-thong-ban-hang-kho

Hệ thống bán hàng và quản lý kho — ứng dụng web gồm backend Node.js/Express (MySQL) và frontend React/Vite.

**Trạng thái hiện tại:** đã có khung dự án, kiểm tra kết nối (health check) và chức năng đăng nhập (Sprint 1).

## Công nghệ

| Phần | Công nghệ |
| --- | --- |
| Backend | Node.js, Express 5, mysql2, bcryptjs, jsonwebtoken |
| Frontend | React 19, Vite 8, oxlint |
| Cơ sở dữ liệu | MySQL 8.0+ |

## Cấu trúc thư mục

```
backend/
  server.js               # Điểm khởi động, đọc .env
  sql/                    # Script tạo schema (chạy theo thứ tự số)
  src/
    app.js                # Cấu hình Express, đăng ký route
    config/database.js    # Pool kết nối MySQL
    controllers/          # Xử lý request, kiểm tra dữ liệu đầu vào
    services/             # Nghiệp vụ (vd. auth.service.js)
    routes/               # Định nghĩa route
    middleware/           # Xử lý 404 và lỗi chung
    errors/app-error.js   # Lớp lỗi có status + code
  test/                   # Test dùng node:test
frontend/
  src/
    pages/                # HomePage, LoginPage
    layouts/ components/
    services/api.js       # Gọi API backend
docs/
```

## Yêu cầu

- Node.js 20.19+ (hoặc 22.12+) — theo yêu cầu của Vite 8
- MySQL 8.0+

## Cài đặt và chạy

### 1. Cơ sở dữ liệu

Tạo database rồi chạy các script trong `backend/sql/` theo thứ tự:

```bash
mysql -u root -p -e "CREATE DATABASE ban_hang_kho CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
mysql -u root -p ban_hang_kho < backend/sql/001_sprint_s1_auth_schema.sql
```

Schema gồm các bảng: `users`, `roles`, `permissions`, `user_roles`, `role_permissions`, `user_scopes`.

### 2. Backend

```bash
cd backend
npm install
cp .env.example .env    # rồi điền giá trị
npm run dev             # chạy với nodemon; dùng `npm start` cho production
```

Biến môi trường (`backend/.env`):

| Biến | Mô tả | Mặc định |
| --- | --- | --- |
| `PORT` | Cổng backend | `5000` |
| `DB_HOST`, `DB_PORT` | Địa chỉ MySQL | —, `3306` |
| `DB_USER`, `DB_PASSWORD`, `DB_NAME` | Thông tin đăng nhập MySQL | — |
| `JWT_SECRET` | Khoá ký JWT, **bắt buộc tối thiểu 32 ký tự** (nếu thiếu, API đăng nhập trả `503 AUTH_NOT_CONFIGURED`) | — |
| `JWT_EXPIRES_IN` | Thời hạn token | `15m` |

Tạo một chuỗi bí mật ngẫu nhiên:

```bash
node -e "console.log(require('crypto').randomBytes(48).toString('hex'))"
```

Kiểm tra: mở http://localhost:5000/api/health.

### 3. Frontend

```bash
cd frontend
npm install
npm run dev     # mặc định http://localhost:5173
```

Frontend gọi API tại `http://localhost:5000/api`. Đặt `VITE_API_URL` trong `frontend/.env` nếu backend chạy ở địa chỉ khác.

### 4. Tạo tài khoản đăng nhập thử

Chưa có chức năng đăng ký hay dữ liệu mẫu, nên cần thêm người dùng trực tiếp vào database. Tạo mật khẩu đã băm (chạy trong thư mục `backend`):

```bash
node -e "console.log(require('bcryptjs').hashSync('MatKhau@123', 10))"
```

Rồi chèn vào bảng `users`:

```sql
INSERT INTO users (username, email, password_hash)
VALUES ('admin', 'admin@example.com', '<chuỗi băm ở trên>');
```

## API

| Phương thức | Đường dẫn | Mô tả |
| --- | --- | --- |
| `GET` | `/api/health` | Kiểm tra backend đang chạy |
| `POST` | `/api/auth/login` | Đăng nhập |

### `POST /api/auth/login`

Request:

```json
{ "identifier": "admin", "password": "MatKhau@123" }
```

`identifier` có thể là username hoặc email.

Response `200`:

```json
{
  "token": "<JWT>",
  "user": {
    "id": "1",
    "username": "admin",
    "email": "admin@example.com",
    "roles": [{ "code": "...", "name": "..." }],
    "permissions": ["..."]
  }
}
```

Lỗi trả về dạng `{ "message": "...", "code": "..." }`:

| Status | Code | Khi nào |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | Thiếu hoặc sai định dạng tài khoản/mật khẩu |
| 400 | `INVALID_JSON` | Body không phải JSON hợp lệ |
| 401 | `INVALID_CREDENTIALS` | Sai thông tin, tài khoản bị khoá hoặc ngừng hoạt động |
| 413 | `PAYLOAD_TOO_LARGE` | Body lớn hơn 10 KB |
| 503 | `AUTH_NOT_CONFIGURED` | `JWT_SECRET` chưa đặt hoặc quá ngắn |

Sau **5 lần** đăng nhập sai liên tiếp, tài khoản bị khoá **15 phút**. Để tránh lộ thông tin, mọi trường hợp thất bại đều trả cùng một thông báo 401.

## Kiểm thử và lint

```bash
cd backend && npm test      # node --test, không cần MySQL (database được mock)
cd frontend && npm run lint
cd frontend && npm run build
```
