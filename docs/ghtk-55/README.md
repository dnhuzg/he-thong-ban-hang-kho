# GHTK-55 · Chuyển kho nội bộ

Trang `/inventory/transfers` dùng dữ liệu thật. Bắt đầu chuyển trừ tồn khả dụng kho đi; xác nhận nhận cộng số thực nhận vào kho đến. Hai bước cập nhật tồn chạy trong transaction, khóa tồn, kiểm phạm vi kho, lưu chênh lệch và audit log. Cần migration `031_chuyen_kho.sql`.
