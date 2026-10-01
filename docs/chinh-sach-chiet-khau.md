# Chính sách chiết khấu theo sản lượng (S3-01)

Quản lý kinh doanh (và Admin) khai báo tại **Sản phẩm & bảng giá → Chiết khấu**. Các vai trò khác không xem và không sửa được.

## Một chính sách gồm

| Thành phần | Quy định |
|---|---|
| Phạm vi | Một **sản phẩm (SKU)** hoặc một **nhóm hàng**. Chính sách của nhóm hàng áp cho mọi sản phẩm trong nhóm đó và các nhóm con. |
| Các bậc | Từ 1 đến 8 bậc. Mỗi bậc gồm *số lượng tối thiểu* và *mức chiết khấu*. Bậc cao hơn phải giảm nhiều hơn bậc thấp hơn. |
| Cách tính | Một trong hai: **phần trăm** của đơn giá (0 < % ≤ 100), hoặc **số tiền trên mỗi đơn vị** (> 0). Cả chính sách dùng chung một cách tính. |
| Hiệu lực | Ngày bắt đầu và ngày kết thúc, đều tuỳ chọn (cả hai đều tính là còn hiệu lực). Để trống là không thời hạn. |
| Trạng thái | *Đang bật* hoặc *Tạm dừng*. Chính sách tạm dừng không được áp dụng. |

## Quy tắc tính

1. **Số lượng tính bằng đơn vị cơ sở** của sản phẩm. Khi bán theo đơn vị quy đổi (thùng, lốc...), số lượng được đổi ra đơn vị cơ sở trước khi so với bậc.
2. Các dòng **cùng một sản phẩm** trong **một đơn hàng** được cộng lại trước khi so với bậc.
3. Mỗi chính sách lấy **bậc cao nhất mà số lượng đạt tới**. Chưa đạt bậc nào thì chính sách đó không giảm gì.
4. Mức giảm trên mỗi đơn vị cơ sở: theo phần trăm là `đơn giá × % ÷ 100` (làm tròn đến 2 chữ số thập phân); theo số tiền là đúng số tiền của bậc. Mức giảm **không vượt quá đơn giá**.
5. **Nhiều chính sách cùng áp dụng** cho một sản phẩm (chính sách của chính SKU đó, của nhóm hàng chứa nó, của nhóm cha...) thì hệ thống chọn chính sách **cho mức giảm trên mỗi đơn vị cao nhất, tức là có lợi nhất cho khách**. Các chính sách **không cộng dồn**. Hai chính sách giảm bằng nhau thì giữ chính sách tạo trước.
6. Chiết khấu tính trên **đơn giá lấy từ bảng giá** của nhóm khách hàng tại ngày đặt đơn.

## Ví dụ

Chính sách A (SKU `COCA-330`, %): từ 100 giảm 3%, từ 500 giảm 5%.
Chính sách B (nhóm *Đồ uống*, số tiền): từ 100 giảm 450 ₫/đơn vị.

Khách mua 120 lon Coca, đơn giá 10.000 ₫: A giảm 300 ₫, B giảm 450 ₫ → áp dụng **B** (450 ₫/lon), không cộng với A.
Cùng khách mua 500 lon: A giảm 500 ₫, B giảm 450 ₫ → áp dụng **A**.

## Cho đơn hàng (Sprint 4)

`DiscountService.best(conn, productId, qtyBase, unitPrice, date)` trả chính sách có lợi nhất cùng mức giảm trên mỗi đơn vị, hoặc `null` nếu không có chính sách nào áp dụng. Đơn hàng nên lưu lại tên chính sách và số tiền giảm tại thời điểm chốt đơn để sửa hay xoá chính sách sau này không làm đổi đơn cũ.
