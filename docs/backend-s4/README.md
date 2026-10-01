# Backend Sprint 4 — S4-02 đến S4-10

Phạm vi: backend Java Servlet/JDBC, migration, endpoint JSON/PDF và kiểm thử. Không thay đổi JSP/CSS/JavaScript hay lịch Sprint Jira. Nhánh: `GHTK-40-48-backend-s4`, tích hợp bằng PR vào `develop`.

## Cài đặt và dữ liệu cần có

- Máy đã có S1–S3: áp migration `016` → `019` theo thứ tự. Migration idempotent, không xoá dữ liệu. Không chạy `schema.sql` lên DB đang dùng vì script cài mới xoá database.
- Cài mới: dùng `schema.sql` và `seed.sql` theo hướng dẫn dự án.
- Quản trị phải gắn `customers.serving_warehouse_id` với kho phục vụ; module tồn kho phải cấp tồn thực tế trong `warehouse_inventory` theo đơn vị cơ sở. Không tự suy ra tồn từ danh mục sản phẩm.
- `customer_receivables` là đầu vào công nợ: `issued_on`, `outstanding` của từng chứng từ. Khoản chưa thanh toán có tuổi lớn hơn `customers.credit_days` chặn tạo/chốt đơn. Module kế toán phải cập nhật số dư trong transaction có khoá đại lý.
- `order_credit_reservations` giữ phần hạn mức đã cam kết cho các đơn được duyệt. Giữ đến khi module hoá đơn chuyển nguyên tử sang khoản phải thu; không được tính cả hai lần hoặc giải phóng trước khi ghi khoản phải thu. S4 chưa thực hiện phát hành hoá đơn/thu tiền của S7.
- Tài khoản đại lý dùng đăng nhập hiện có, vai trò `CUSTOMER`; quản trị cấp mapping `customer_accounts(customer_id,user_id)` trước. API không nhận đại lý tuỳ ý để tạo giỏ hoặc cho khách tự sửa mapping.

## Giao thức chung

Base URL: `/oms`. Mọi endpoint cần session đã đăng nhập. POST dùng `application/x-www-form-urlencoded` và tham số `_csrf` của session (có thể lấy header `X-CSRF-Token` từ `/session/ping`). GET không ghi dữ liệu. Không dùng GET để chốt/duyệt/huỷ. Trả lỗi JSON với 400 (đầu vào/nghiệp vụ), 403 (thiếu quyền), 404 (đơn ngoài phạm vi hoặc không tồn tại), 500 (lỗi hạ tầng, không lộ SQL/stack trace). PDF lỗi trả HTTP phù hợp.

| Jira | Endpoint | Tham số / kết quả |
|---|---|---|
| GHTK-40 | GET `/api/orders/credit` | `id`; nợ hiện tại, cam kết đơn khác, hạn mức, còn lại, tổng đơn, phần vượt, quá hạn, cần duyệt |
| GHTK-41 | GET `/api/orders/stock` | `id`; số lượng yêu cầu/tồn khả dụng theo SKU và kho; khả dụng cho đơn đã giữ chỗ cộng lại phần giữ của chính đơn |
| GHTK-42 | GET `/api/orders/purchase-history` | `customerId`; SKU đã mua 3 tháng, lượng cơ sở bình quân trên mỗi đơn đã xuất/giao/đóng |
| GHTK-42 | POST `/api/orders/add-last-purchase` | `id` đơn Nháp; thêm cả nhóm dòng của lần mua gần nhất, áp giá hiện hành |
| GHTK-43 | POST `/api/orders/submit` | `id`; chốt giá và chiết khấu; hợp lệ → `APPROVED`, vi phạm → `PENDING_APPROVAL` |
| GHTK-43 | GET `/api/orders/pending` | Hàng đợi tối đa 200 đơn, lý do và mức vi phạm; QLKD xem tất cả, NVKD xem đơn cổng đại lý phụ trách |
| GHTK-43 | POST `/api/orders/decide` | `id`, `action=APPROVE/REJECT/RETURN`, `opinion`; REJECT/RETURN bắt buộc ý kiến |
| GHTK-44 | GET `/api/orders/history` | `id`; lịch sử thêm mới, không có endpoint sửa/xoá |
| GHTK-44 | POST `/api/orders/cancel` | `id`, `reason`; bắt buộc lý do, nhả tồn và cam kết hạn mức |
| GHTK-44 | POST `/api/orders/advance` | `id`, `status=PICKING/SHIPPED/DELIVERED/CLOSED`; không bỏ bước/đi ngược |
| GHTK-45 | GET `/api/orders/search` | `status`, `customerId`, `creatorId` (nhân viên lập đơn), `areaId`, `from/to=yyyy-MM-dd`, `page`, `pageSize` (1–100); tổng tiền cả tập lọc |
| GHTK-46 | GET `/api/orders/pdf` | `id`; PDF tiếng Việt có Code 128, lặp tiêu đề bảng và phân trang |
| GHTK-47 | POST `/api/orders/copy` | `id` đơn nguồn; 201, ID đơn Nháp mới; không kế thừa giá chốt |
| GHTK-48 | GET `/api/portal/catalog` | `q`, `page`; hàng đang kinh doanh có giá trong nhóm của tài khoản đại lý |
| GHTK-48 | POST `/api/portal/create` | 201, ID giỏ Nháp; đại lý xác định từ session, bỏ qua `customerId` phía client |
| GHTK-48 | GET `/api/portal/cart` | `id`; dòng hàng, tổng, chiết khấu, công nợ còn lại, điểm giao |
| GHTK-48 | POST `/api/portal/add` | `id`, `productId`, `unitId`, `quantity` |
| GHTK-48 | POST `/api/portal/update` | `id`, `lineId`, `unitId`, `quantity` |
| GHTK-48 | POST `/api/portal/remove` | `id`, `lineId` |
| GHTK-48 | POST `/api/portal/header` | `id`, `deliveryPointId`, `deliveryDate`, `note` |
| GHTK-48 | POST `/api/portal/submit` | `id`; luôn `PENDING_APPROVAL`, chưa giữ chỗ |

NVKD chỉ xử lý đại lý đang được phân công. Đại lý chỉ xử lý giỏ/đơn thuộc mapping của mình. Nhân viên phụ trách xác nhận đơn cổng không vi phạm; mọi vi phạm hạn mức/giá sàn phải QLKD/Admin duyệt. Kiểm lại công nợ và giá sàn khi nhân viên xác nhận, không cho lách bằng endpoint chốt chung. Đơn chờ duyệt chưa giữ tồn; lúc duyệt kiểm tồn lại và giữ chỗ trong cùng transaction. SKU khoá theo thứ tự để tránh âm tồn khi có người khác chốt cùng lúc.

Vòng đời: DRAFT → PENDING_APPROVAL/APPROVED → PICKING → SHIPPED → DELIVERED → CLOSED. RETURN đưa về Nháp để sửa, REJECT kết thúc với trạng thái REJECTED. Huỷ chỉ trước SHIPPED; hàng đã xuất phải qua module trả hàng. Warehouse/WH Manager chỉ soạn/xuất kho được gắn, NVKD phụ trách có thể xác nhận giao; đóng đơn dành cho Kế toán/Admin. Giá đã chốt không tự đổi theo bảng giá mới; đơn Nháp và bản sao dùng giá hiện hành.

## Kiểm chứng

`mvn -B clean package` với JDK 21, target Java 17. Test H2 chế độ MySQL kiểm công nợ quá hạn/hạn mức, giữ tồn đồng thời, submit đồng thời, rollback, quyền đại lý/nhân viên/kho, luồng duyệt/huỷ, bộ lọc/tổng, PDF Unicode nhiều trang, sao chép giá mới, giỏ đại lý và CSRF/HTTP. Không tự áp migration vào database đang chạy. Mẫu PDF kiểm thử tại `target/s4-order-example.pdf` (không commit).

PDF dùng OpenPDF 2.0.5 (nhánh hỗ trợ Java 17) và font Liberation đi kèm `openpdf-fonts-extra`, giấy A4, mã vạch Code 128. Thư viện và font giữ giấy phép từ dependency gốc: https://github.com/LibrePDF/OpenPDF.
