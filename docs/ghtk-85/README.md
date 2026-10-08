# GHTK-85 · S8-10 · Sổ chi tiết công nợ của một đại lý (FE)

Màn `/finance/customer-ledger` dành cho Kế toán công nợ hoặc Admin, có kiểm tra vai trò ở servlet. Người xem chọn một đại lý, lọc khoảng từ ngày/đến ngày; bảng hiển thị ngày, chứng từ, diễn giải, phát sinh tăng/giảm và dư lũy kế. Dư đầu khoảng cộng toàn bộ biến động mẫu trước ngày bắt đầu; dư cuối bằng dư đầu cộng tăng trừ giảm trong khoảng, kể cả khi khoảng không có dòng phát sinh.

- **Xuất PDF** gọi hộp thoại in của trình duyệt để chọn Save as PDF; bản in ghi rõ dữ liệu mẫu.
- **Xuất Excel** tạo tệp `.xlsx` OOXML trong trình duyệt, không dùng CDN hay thư viện ngoài. Nội dung theo đúng đại lý và khoảng ngày đang xem, số tiền là ô số để tính tiếp trong Excel.

Dữ liệu công nợ/chứng từ hiện là mẫu trong JavaScript. Cần BE cung cấp sổ từ dữ liệu kế toán thật, phân quyền/phạm vi đại lý trên server, số dư tại đầu kỳ, chứng từ, thuật toán đối soát và xuất file từ dữ liệu được chốt. Các file mẫu không có giá trị xác nhận công nợ.
