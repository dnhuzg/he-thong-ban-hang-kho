# GHTK-56 · Kiểm kê kho

Trang `/inventory/counts` tạo ảnh chụp tồn hệ thống, nhận số đếm thực tế và chốt điều chỉnh tồn. Khi chốt, hệ thống khóa tồn, không cho hạ dưới lượng giữ chỗ, ghi movement tăng/giảm và audit log trong cùng transaction. Cần migration `032_kiem_ke_kho.sql`.
