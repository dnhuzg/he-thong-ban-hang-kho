# GHTK-76 · Trả hàng từ đại lý

Phiếu trả tham chiếu hóa đơn và đơn giá gốc. Service khóa hóa đơn/dòng hàng để chặn trả vượt số đã giao. Chỉ khi Quản lý kho xác nhận, hàng tốt mới cộng tồn bán, hàng hỏng vào tồn lỗi; movement và audit nằm trong transaction. Cần migration `035_tra_hang_giam_cong_no.sql`.
