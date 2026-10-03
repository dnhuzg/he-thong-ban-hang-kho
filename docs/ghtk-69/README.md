# GHTK-69 · S7-03 · Xem số công nợ theo đại lý với phân tích tuổi nợ (FE)

## Phạm vi

- Trang `/finance/debt-aging` dùng layout JSP chung, quyền đọc module `INVOICE` và chỉ mở cho Kế toán, Quản lý kinh doanh, Admin ở server.
- Bảng theo đại lý có phải thu, đã thu, còn nợ và bốn nhóm tuổi nợ: 0–30, 31–60, 61–90, trên 90 ngày.
- Lọc theo tên/mã đại lý hoặc mã hoá đơn, nhóm tuổi nợ, ngày đối chiếu và riêng khoản quá hạn. Tổng cuối bảng và các thẻ tổng hợp lấy cùng tập dữ liệu sau lọc.
- Nút Xuất Excel tải CSV UTF-8 có BOM để mở trực tiếp trong Excel; nội dung là kết quả lọc hiện tại. Đây chưa phải tệp `.xlsx`.

## Cách tính trong bản mẫu

- Tuổi nợ = số ngày từ **ngày phát sinh hoá đơn** tới ngày đối chiếu (ngày phát sinh tính 0); các khoản chưa đến hạn vẫn thuộc nhóm tuổi theo ngày phát sinh.
- Quá hạn = đã qua **ngày đến hạn** và còn nợ. Chưa đến hạn không xuất hiện khi bật bộ lọc quá hạn.
- Còn nợ của hoá đơn = phải thu − đã thu. Hoá đơn đã thu hết không nằm trong bảng công nợ đang mở. Mỗi khoản còn nợ chỉ thuộc một nhóm tuổi; tổng bốn nhóm luôn bằng tổng còn nợ.
- Các hoá đơn trong một đại lý được cộng lại; nếu lọc nhóm tuổi hoặc quá hạn, phải thu và đã thu của đại lý chỉ tính các hoá đơn phù hợp.

## Giới hạn để nhóm BE tiếp nối

Dữ liệu hoá đơn/đại lý đang là **mẫu trong JavaScript**, không đọc MySQL, không xác định phạm vi đại lý từ người đăng nhập và không phản ánh thanh toán thật. BE cần trả danh sách hoá đơn, các khoản đã thu và thời điểm đối chiếu, kiểm quyền và phạm vi dữ liệu phía server, rồi thay nguồn dữ liệu mẫu. Bản FE không ghi hay điều chỉnh công nợ thật. Giữ API xuất chính thức phía server khi dữ liệu lớn hoặc cần tệp `.xlsx`.
