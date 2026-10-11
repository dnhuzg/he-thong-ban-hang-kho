# GHTK-58 / GHTK-59 — Lô, hạn dùng và thẻ kho

Phạm vi thống nhất: **giao diện JSP và dữ liệu mẫu**, cùng cách tích hợp các màn GHTK-51/52/53/55 trên `develop`. Không có migration, không sửa schema, không ghi tồn kho thật. Hai story vẫn cần backend trước khi đáp ứng AC trên dữ liệu nghiệp vụ thật.

## Mở màn hình

- `/inventory/lots`: cấu hình bật/tắt quản lý lô theo SKU, ngưỡng cảnh báo 0–365 ngày, tìm theo SKU/tên/số lô, lọc kho và tình trạng. Xem số lô, ngày sản xuất, hạn dùng, lượng còn lại. Cấu hình lưu riêng theo context ứng dụng và tài khoản trong localStorage; khi lưu trữ bị chặn thì chỉ giữ trong phiên trang.
- `/inventory/stock-card`: chọn SKU, lọc kho và ngày; phát sinh nhập, xuất, chuyển, điều chỉnh theo thời gian tăng dần. Liên kết chứng từ mở dialog mẫu có số chứng từ, kho đối ứng, số lượng và diễn giải. Có thể mở với `?sku=SP-002`.
- Menu nằm trong **Tồn kho & nhập kho**, dùng layout, design token và theme chung. Cả hai URL yêu cầu quyền đọc `INVENTORY` tại server qua `AuthFilter`/`AccessRules`.

## Quy tắc mẫu

- Lô hết hạn khi ngày hạn dùng **trước** ngày hôm nay; hết hạn trong hôm nay vẫn hợp lệ. Sắp hết hạn khi còn 0 đến ngưỡng ngày, tính cả hai đầu. Lô hết hàng không cộng vào số cảnh báo.
- Tắt quản lý lô chỉ ẩn các lô mẫu; bật lại không mất dữ liệu. SKU không quản lý lô (SP-003) chưa có lô; bật lên hiện trạng thái rỗng.
- Kiểm tra xuất chỉ kiểm lượng còn lại và hạn dùng trong bản mẫu. Quyền ghi `INVENTORY` mới có nút kiểm tra; ghi đè quá hạn chỉ hiển thị khi có vai trò `WH_MANAGER` và quyền ghi. Cần lý do không trống (≤500 ký tự) và đánh dấu xác nhận. Admin không tự được ghi đè nếu thiếu vai trò `WH_MANAGER`.
- Cờ quyền từ server chỉ điều khiển thao tác mẫu. **Không dùng JavaScript/localStorage làm kiểm soát bảo mật cho xuất kho thật.** Không có POST hay endpoint thay đổi tồn; không ghi nhật ký nghiệp vụ hoặc quyết định xuất thật.
- Mỗi sản phẩm giữ đơn vị cơ sở riêng. Số tồn cuối của ba SKU mẫu khớp các dòng tương ứng trong GHTK-53; tổng lượng lô của SKU quản lý lô khớp số tồn tại mỗi kho.
- Tồn đầu kỳ = tổng phát sinh trước ngày bắt đầu. Tồn sau = tồn đầu kỳ + phát sinh trong kỳ tới dòng hiện tại. Lọc ngày kết thúc bao gồm toàn bộ ngày đó. Kỳ không có dòng vẫn có tồn đầu/cuối.
- Chọn một kho: tồn sau tại kho đó. Chọn tất cả kho: tồn cộng gộp SKU; hai dòng chuyển đi/chuyển đến cùng chứng từ có tổng biến động bằng 0. Cùng thời điểm sắp theo ID phát sinh để kết quả ổn định.
- Ngày mẫu tương đối với hôm nay để luôn có lô còn hạn/sắp hết/quá hạn. Không liên thông localStorage với phiếu nhập/chuyển kho mẫu của các story khác.

## Điểm nối backend cho team

Giữ URL màn và phân quyền hiện có. Thay `InventoryTraceDemo` bằng dữ liệu từ Servlet → Service → DAO khi team đã chốt schema kho:

| Dữ liệu | Trường tối thiểu |
|---|---|
| Cấu hình SKU | productId, sku, trackLots, warningDays, baseUnit |
| Lô tại kho | lotId, productId, warehouseId, lotNumber, manufacturedOn, expiresOn, remainingBaseQuantity |
| Phát sinh | movementId, productId, warehouseId, occurredAt, type, signedBaseQuantity, documentType, documentId, documentCode |
| Thẻ kho | openingBalance, increase, decrease, closingBalance, rows[].balanceAfter |

Team backend cần kiểm quyền đọc và phạm vi kho được phân công tại server; lưu cấu hình bằng POST có CSRF và kiểm quyền ghi. Lấy số tồn/lượng lô từ một sổ tồn thống nhất với GHTK-52/53/55, xử lý phân trang nhưng tính tồn từ toàn bộ lịch sử trước trang/kỳ. Dùng DECIMAL cho lượng quy đổi.

Khi nối luồng xuất GHTK-60/61/62, kiểm hạn dùng lại tại thời điểm xác nhận bằng ngày nghiệp vụ phía server. Ghi đè phải kiểm vai trò `WH_MANAGER`, quyền ghi, lý do và xác nhận tại server; khóa bản ghi lô, kiểm lượng khả dụng, cập nhật tồn và ghi audit trong **cùng transaction**. Liên kết chứng từ dùng loại/ID chứng từ do server cung cấp và route có quyền phù hợp, thay dialog mẫu. Thống nhất timezone nghiệp vụ và điều kiện ngừng quản lý lô khi còn tồn trước khi viết migration.

## Kiểm tra

1. `mvn clean package`: toàn bộ test Java và đóng gói WAR.
2. `node --test src/test/js/inventory-trace.test.cjs`: tồn đầu/cuối, lọc ngày, cặp chuyển kho, hạn dùng ở biên và điều kiện ghi đè mẫu.
3. Đăng nhập tài khoản quyền đọc kho: xem hai màn, không có lưu/kiểm tra xuất. Quyền ghi kho: lưu ngưỡng, bật/tắt SKU; lô quá hạn bị chặn. Quản lý kho: cần lý do và xác nhận để hoàn tất kiểm tra ghi đè mẫu.
4. Thẻ kho: SP-001/kho Hà Nội cuối kỳ 480 chai; SP-002/kho Hà Nội 160 gói; ngày ngược báo lỗi và xóa kết quả cũ. Chọn một ngày không có phát sinh vẫn giữ tồn đầu/cuối. Chứng từ mở được bằng chuột và bàn phím.
5. Kiểm tra màn 390px, desktop và theme tối. Bảng dài cuộn ngang trong khung; hộp thoại không vượt chiều cao màn hình.

## Ghép vào team

Nhánh bắt đầu từ `develop`; PR vào `develop`, không merge tự động. File dùng chung chỉ thêm hai dòng `AccessRules` và hai mục `Menu`, phần còn lại tách riêng. Trước khi merge cập nhật `origin/develop`, chạy CI và lấy ít nhất một reviewer. Sau merge không có migration hoặc cấu hình mới cần áp dụng.
