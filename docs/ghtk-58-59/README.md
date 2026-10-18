# GHTK-58 / S6-01 — Lô và hạn sử dụng

S6-01 đã chuyển từ giao diện mẫu sang Servlet → Service → DAO với MySQL. Dùng lại `inventory_lots`, `warehouse_inventory`, phiếu nhập GHTK-52 và xác nhận xuất GHTK-62 của team; không tạo sổ tồn thứ hai. Thẻ kho GHTK-59 của team giữ nguyên.

## Sử dụng

- Mở `/inventory/lots`, lọc theo SKU, kho được phân công, tình trạng hoặc số lô/tên hàng. Chọn SKU rồi bấm Tra cứu để xem cấu hình. Hiển thị tối đa 200 lô; thu hẹp bộ lọc để tra cứu thêm.
- Quản lý kho/Admin có quyền ghi INVENTORY được khai báo ngưỡng cảnh báo 0–365 ngày, bật/tắt quản lý lô. Nhân viên kho có quyền đọc chỉ xem.
- Trước khi bật SKU đã có hàng, bổ sung số lô/ngày sản xuất/hạn dùng của lô cũ qua nút Bổ sung thông tin. Tổng lượng lô ở từng kho phải khớp sổ tồn. Không suy đoán hạn dùng hoặc tự sửa lượng tồn trong migration.
- Chỉ tắt quản lý lô khi SKU không còn tồn/giữ chỗ tại tất cả kho. Không sửa ngày hạn của lô đã đủ thông tin trong lúc đang quản lý lô để né chặn quá hạn.
- Xác nhận phiếu nhập: SKU quản lý lô bắt buộc đủ ba trường. Ngày sản xuất không sau ngày nhập, hạn dùng không trước ngày nhập. Phiếu nhập tạo lô kèm thông tin này trong cùng transaction với cộng tồn và bút toán.
- Lô sắp hết hạn có ngày còn lại từ 0 đến ngưỡng, tính cả hai đầu; hạn dùng hôm nay vẫn hợp lệ. Lô lượng 0 hiển thị Hết hàng và được giữ để truy nguyên.
- Tại màn Xuất kho, hệ thống ưu tiên lô còn hạn theo hạn dùng rồi ID. Không đủ lượng lô hợp lệ thì rollback cả phiếu, giữ chỗ, lượng lô và chuyển trạng thái.
- Ghi đè quá hạn: chỉ vai trò WH_MANAGER, có quyền ghi INVENTORY và đúng kho, phải đánh dấu xác nhận và nhập lý do không trống ≤500 ký tự. Admin đơn thuần không được ghi đè nếu chưa có vai trò WH_MANAGER. Lưu người xác nhận, lý do, lượng và lô thực xuất; ghi audit cho mỗi lô quá hạn.
- Giao hàng thất bại: hoàn lượng đã phân bổ về đúng lô trong transaction trả kho hiện có; xác nhận lặp không hoàn lần hai.
- Endpoint chuyển trạng thái cũ không được đưa SKU quản lý lô thẳng sang SHIPPED; phải đi qua màn Xuất kho để kiểm lô.

## Triển khai

1. Merge PR vào `develop` sau review và CI xanh.
2. Database hiện có: áp `database/migrations/029_lo_han_su_dung.sql` sau 028 trước khi deploy WAR. Script chạy lại an toàn; thêm cột vào bảng hiện có, đổi điều kiện lượng lô thành ≥0, tạo `shipment_lot_allocations`.
3. Cài mới: `schema.sql` đã chứa đầy đủ thay đổi. Không chạy schema cài mới lên database đang sử dụng vì script đó xóa database.
4. SKU mặc định chưa bật quản lý lô. Lô cũ giữ nguyên; quản lý bổ sung thông tin thật và đối chiếu lượng với sổ tồn trước khi bật. Nếu số dư lô cũ bị lệch do các bản xuất kho trước đây chưa trừ lô, cần kiểm kê/đối chiếu dữ liệu trước; script không tự điều chỉnh số dư.
5. Không cần dependency hoặc cấu hình ứng dụng mới. Cấu hình mẫu localStorage không được nhập vào DB. JS mẫu và workflow kiểm thử dữ liệu giả cũ đã được gỡ khỏi nhánh sửa.

## Kiểm chứng

- Maven clean package: toàn bộ test Java, bao gồm cấu hình, phạm vi kho, thu hồi quyền, metadata, rollback xuất hết hạn, ghi đè, hoàn đúng lô và xuất đồng thời.
- Kiểm thử MySQL 8.4 riêng: áp schema develop, seed và migration 029 hai lần, kiểm tra danh mục được giữ nguyên; schema cài mới cũng chạy thành công.
- Kiểm tra giao diện thực trên Tomcat 10.1/MySQL riêng: cấu hình lưu qua POST/CSRF, lọc lô, quyền đọc/ghi, phạm vi kho, chặn hết hạn và xác nhận ghi đè; desktop/mobile. Toàn bộ dùng dữ liệu giả, không truy cập DB team.

## Quan hệ với GHTK-59

Trang `/inventory/stock-card` đang dùng StockCardService/StockCardDao đọc bút toán thật của team. Đợt này không thay cách tính thẻ kho. Luồng xuất và trả thất bại vẫn dùng bút toán cùng transaction hiện có; phần bổ sung chỉ cập nhật phân bổ lô đi kèm.
