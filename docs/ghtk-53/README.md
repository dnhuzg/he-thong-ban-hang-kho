# GHTK-53 · S5-05 · Xem số tồn theo SKU và từng kho (phần FE)

Truy cập `/inventory/stock` bằng tài khoản có quyền xem Tồn kho. Màn hình chỉ xem có ba cột tách biệt: tồn thực tế, tồn đang giữ chỗ và tồn khả dụng (hiện tính bằng thực tế trừ giữ chỗ). Mỗi dòng là một cặp SKU + kho, hiển thị đơn vị cơ sở riêng của sản phẩm. Có tìm kiếm mã/tên, lọc kho, nhóm hàng, trạng thái tồn; giao diện điện thoại dùng thẻ thay cho bảng rộng.

**Giới hạn:** toàn bộ số lượng, nhóm hàng, kho và ngưỡng sắp hết đều là dữ liệu mẫu khai báo trong `stock.js`. Chưa kết nối DB, phiếu nhập, đơn giữ chỗ hoặc xuất hàng. Backend nhóm cần cấp dữ liệu theo quyền của người dùng và tính số tồn/giữ chỗ nhất quán, sau đó thay mảng mẫu; không dùng các số mẫu để quyết định bán hàng.
