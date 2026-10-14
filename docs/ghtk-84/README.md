# GHTK-84 · S8-09 · Báo cáo công nợ quá hạn theo tuổi nợ (FE)

## Phạm vi

- Nâng cấp `/finance/debt-aging` để thể hiện đại lý có rủi ro công nợ cao nhất trong tập dữ liệu lọc hiện tại.
- Tổng hợp dư nợ theo nhân viên phụ trách, đại lý và bốn nhóm tuổi nợ: 0–30, 31–60, 61–90, trên 90 ngày.
- Bổ sung nhân viên phụ trách vào bảng chi tiết; phần tìm kiếm có thể tìm theo mã/tên nhân viên.
- Xuất tệp Excel `.xlsx` theo bộ lọc hiện tại, gồm tổng hợp tuổi nợ, đại lý rủi ro cao nhất, tổng hợp theo nhân viên và chi tiết theo đại lý.

## Quy tắc trong bản mẫu

- Tuổi nợ = số ngày từ ngày phát sinh hoá đơn tới ngày đối chiếu (ngày phát sinh tính 0), giữ nguyên quy tắc của màn phân tích tuổi nợ hiện tại.
- Quá hạn = ngày đến hạn nhỏ hơn ngày đối chiếu và còn dư nợ.
- Xếp hạng đại lý rủi ro cao nhất: dư nợ quá hạn cao nhất; nếu bằng nhau thì xét dư nợ nhóm trên 90 ngày, số ngày quá hạn lâu nhất, rồi tổng còn nợ.
- Các bộ tổng hợp dùng cùng các dòng hoá đơn sau bộ lọc từ khoá, nhóm tuổi nợ, ngày đối chiếu và chỉ xem quá hạn.

## Giới hạn cần lưu ý

Đây là phần giao diện FE mẫu, dữ liệu hoá đơn, khoản thu và phân công nhân viên đang hardcode trong JavaScript, chưa truy vấn MySQL và không đại diện công nợ thực. Bản này chỉ xuất đúng dữ liệu mẫu đang lọc. Backend cần thay nguồn dữ liệu bằng truy vấn công nợ thật, xác minh phân công đại lý theo dữ liệu hệ thống và thực hiện kiểm quyền/phạm vi ở phía server.
