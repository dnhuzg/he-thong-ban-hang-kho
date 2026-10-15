# GHTK-70 · Cảnh báo công nợ

Trang `/finance/credit-alerts` tổng hợp trực tiếp từ `customers` và `customer_receivables`. Quy tắc chặn nợ quá hạn ở server tiếp tục do `OrderCreditService` thực thi khi tạo/chốt đơn; giao diện không tự sửa dữ liệu.
