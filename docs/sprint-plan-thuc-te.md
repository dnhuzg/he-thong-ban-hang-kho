# Sprint Plan theo thời gian làm thực tế

Viết lại sheet "5. Sprint Plan" dựa trên lịch sử commit (30/09 → 19/10/2026) và trạng thái issue trên Jira (đọc ngày 25/10/2026).

## Tóm tắt

- Kế hoạch ban đầu: 8 sprint × 1 tuần, bắt đầu 28/09/2026, mỗi sprint một nhóm story S1-xx … S8-xx.
- Thực tế: **76/76 story Done trên Jira, tổng 350 story point**, làm trong **3 tuần** (30/09 → 19/10). Lần chốt code cuối là 18/10, merge `develop` → `main` sáng 19/10 (PR #125).
- Nhóm không làm tuần tự theo sprint: tuần 1 dựng gần hết giao diện và nghiệp vụ S1–S4 cùng khung giao diện kho, công nợ; tuần 3 nối các màn hình đó với cơ sở dữ liệu thật và làm nốt EP-06, EP-08, EP-09.
- Ngày trong bảng là ngày commit đầu và commit cuối có mã Jira của story; story được xếp vào tuần có commit cuối.

| Tuần thực tế | Thời gian | Story xong | Story point | Nội dung chính |
|---|---|---:|---:|---|
| Tuần 1 | 28/09 – 04/10 | 37 | 171 | Tài khoản, phân quyền, sản phẩm, bảng giá, chiết khấu, đại lý, gõ đơn, kiểm hạn mức / tồn / giá sàn, duyệt đơn; khung giao diện kho và công nợ |
| Tuần 2 | 05/10 – 11/10 | 2 | 8 | Cổng đặt hàng cho đại lý, xuất Excel tồn kho; bắt đầu lô hạn dùng, thẻ kho, theo dõi giao hàng, công nợ của đại lý, phiếu thu, nhắc hạn thanh toán (hoàn tất ở tuần 3) |
| Tuần 3 | 12/10 – 19/10 | 37 | 171 | Nối dữ liệu thật cho kho, xuất kho, hoá đơn, thanh toán, công nợ; trả hàng, điều chỉnh tồn, phân tuyến giao hàng, chỉ tiêu, dashboard, báo cáo |
| Tuần 4 – 8 | 19/10 – 22/11 | — | — | Chưa có commit tính năng mới |

### Theo epic

| Epic | Story | Story point | Thời gian thực tế |
|---|---:|---:|---|
| EP-01 – Tài khoản, Phân quyền & Hồ sơ | 14 | 55 | 30/09 – 01/10 (chỉnh menu đến 15/10) |
| EP-02 – Danh mục Sản phẩm & Bảng giá | 7 | 34 | 30/09 – 01/10 |
| EP-03 – Đại lý & Hạn mức công nợ | 6 | 30 | 01/10 |
| EP-04 – Đặt hàng & Duyệt đơn | 13 | 61 | 01/10 – 15/10 |
| EP-05 – Kho & Tồn kho | 11 | 51 | 30/09 – 18/10 |
| EP-06 – Xuất kho & Giao hàng | 7 | 33 | 06/10 – 18/10 |
| EP-07 – Hoá đơn, Công nợ & Thanh toán | 10 | 47 | 03/10 – 18/10 |
| EP-08 – Trả hàng & Điều chỉnh | 3 | 15 | 12/10 – 18/10 |
| EP-09 – Chỉ tiêu, Dashboard & Báo cáo | 5 | 24 | 12/10 – 19/10 |

### Theo người phụ trách (theo Jira)

| Thành viên | Story | Story point |
|---|---:|---:|
| Dương Ngô Hùng | 22 | 106 |
| NGUYEN DANH HUNG | 16 | 81 |
| TRAN DUY KHANH | 9 | 48 |
| NGUYEN KIM THANH LONG | 12 | 44 |
| VU DUY LINH | 10 | 38 |
| NGUYEN THI LAM | 7 | 33 |

Người phụ trách lấy từ Jira; nhiều story được người khác gộp nhánh, sửa xung đột hoặc nối backend ở tuần 3 nên người commit có thể khác người phụ trách.

## Chi tiết từng tuần

### Tuần 1 (28/09 – 04/10)

Mục tiêu đạt được: Admin tạo tài khoản và phân quyền; hệ thống có danh mục sản phẩm, bảng giá, chiết khấu, hồ sơ đại lý với hạn mức; Sales Rep gõ được đơn và bị chặn ngay khi vượt tồn, vượt hạn mức hoặc dưới giá sàn.

| Jira | Story | Epic | SP | Phụ trách | Commit đầu | Commit cuối | Sprint trên Jira |
|---|---|---|---:|---|---|---|---|
| GHTK-2 | S1-01 – Đăng nhập hệ thống | EP-01 | 5 | Dương Ngô Hùng | 30/09 | 01/10 | S1 |
| GHTK-3 | S1-02 – Duy trì phiên đăng nhập và đăng xuất an toàn | EP-01 | 3 | NGUYEN KIM THANH LONG | 30/09 | 30/09 | S1 |
| GHTK-12 | S1-03 – Đặt lại mật khẩu khi quên thông qua email | EP-01 | 5 | VU DUY LINH | 30/09 | 30/09 | S1 |
| GHTK-13 | S1-04 – Đổi mật khẩu khi đang đăng nhập | EP-01 | 2 | NGUYEN KIM THANH LONG | 30/09 | 30/09 | S1 |
| GHTK-14 | S1-05 – Phân quyền theo vai trò cho toàn hệ thống | EP-01 | 8 | Dương Ngô Hùng | 30/09 | 30/09 | S1 |
| GHTK-16 | S1-07 – Nhận thông báo rõ ràng khi truy cập nhầm chỗ hoặc không đủ quyền | EP-01 | 1 | NGUYEN KIM THANH LONG | 30/09 | 30/09 | S1 |
| GHTK-17 | S1-08 – Tạo, sửa và tìm kiếm tài khoản người dùng | EP-01 | 8 | NGUYEN DANH HUNG | 30/09 | 01/10 | S1 |
| GHTK-18 | S1-09 – Gán vai trò và gắn người dùng với kho hoặc địa bàn | EP-01 | 3 | Dương Ngô Hùng | 30/09 | 30/09 | S1 |
| GHTK-19 | S1-10 – Khoá và mở khoá tài khoản | EP-01 | 2 | Dương Ngô Hùng | 30/09 | 30/09 | S1 |
| GHTK-20 | S2-01 – Nhập danh sách người dùng hàng loạt từ tệp Excel | EP-01 | 5 | Dương Ngô Hùng | 30/09 | 30/09 | S2 |
| GHTK-21 | S2-02 – Xem và cập nhật hồ sơ cá nhân | EP-01 | 3 | Dương Ngô Hùng | 30/09 | 30/09 | S2 |
| GHTK-22 | S2-03 – Tải lên ảnh đại diện | EP-01 | 2 | NGUYEN DANH HUNG | 30/09 | 30/09 | S2 |
| GHTK-23 | S2-04 – Xem nhật ký thao tác trên tồn kho và công nợ | EP-01 | 3 | NGUYEN DANH HUNG | 30/09 | 30/09 | S2 |
| GHTK-24 | S2-05 – Quản lý danh mục sản phẩm | EP-02 | 8 | Dương Ngô Hùng | 30/09 | 01/10 | S2 |
| GHTK-25 | S2-06 – Quản lý nhóm hàng nhiều cấp | EP-02 | 5 | Dương Ngô Hùng | 30/09 | 01/10 | S2 |
| GHTK-26 | S2-07 – Có đơn vị tính quy đổi đúng theo cách kho đang gọi hàng | EP-02 | 5 | Dương Ngô Hùng | 30/09 | 30/09 | S3 |
| GHTK-27 | S2-08 – Nhập danh mục sản phẩm hàng loạt từ Excel | EP-02 | 2 | Dương Ngô Hùng | 30/09 | 30/09 | S3 |
| GHTK-28 | S2-09 – Quản lý danh mục nhà cung cấp | EP-05 | 3 | Dương Ngô Hùng | 30/09 | 30/09 | S3 |
| GHTK-29 | S2-10 – Khai báo bảng giá theo nhóm khách hàng và thời gian hiệu lực | EP-02 | 8 | Dương Ngô Hùng | 30/09 | 01/10 | S3 |
| GHTK-30 | S3-01 – Khai báo chính sách chiết khấu theo sản lượng | EP-02 | 3 | Dương Ngô Hùng | 01/10 | 01/10 | S3 |
| GHTK-31 | S3-02 – Xem lịch sử thay đổi giá của một sản phẩm | EP-02 | 3 | Dương Ngô Hùng | 01/10 | 01/10 | S3 |
| GHTK-32 | S3-03 – Quản lý hồ sơ đại lý | EP-03 | 8 | Dương Ngô Hùng | 01/10 | 01/10 | S3 |
| GHTK-33 | S3-04 – Khai báo nhiều điểm giao hàng cho một đại lý | EP-03 | 5 | Dương Ngô Hùng | 01/10 | 01/10 | S3 |
| GHTK-34 | S3-05 – Khai báo hạn mức công nợ và số ngày nợ cho phép | EP-03 | 5 | Dương Ngô Hùng | 01/10 | 01/10 | S3 |
| GHTK-35 | S3-06 – Phân công nhân viên kinh doanh theo đại lý và khu vực | EP-03 | 5 | Dương Ngô Hùng | 01/10 | 01/10 | S3 |
| GHTK-36 | S3-07 – Khoá hoặc mở giao dịch với một đại lý | EP-03 | 5 | Dương Ngô Hùng | 01/10 | 01/10 | S3 |
| GHTK-37 | S3-08 – Tìm kiếm và lọc danh sách đại lý | EP-03 | 2 | Dương Ngô Hùng | 01/10 | 01/10 | S3 |
| GHTK-38 | S3-09 – Tạo đơn hàng cho một đại lý | EP-04 | 8 | Dương Ngô Hùng | 01/10 | 01/10 | S3 |
| GHTK-39 | S4-01 – Có giá và chiết khấu áp tự động khi thêm dòng hàng | EP-04 | 8 | NGUYEN KIM THANH LONG | 03/10 | 03/10 | S7 |
| GHTK-40 | S4-02 – Được chặn ngay khi đơn vượt hạn mức công nợ của đại lý | EP-04 | 5 | NGUYEN DANH HUNG | 01/10 | 01/10 | S7 |
| GHTK-41 | S4-03 – Nhìn thấy tồn khả dụng và bị chặn khi đặt vượt tồn | EP-04 | 5 | NGUYEN DANH HUNG | 01/10 | 01/10 | S7 |
| GHTK-42 | S4-04 – Xem lịch sử mua hàng của đại lý ngay khi gõ đơn | EP-04 | 5 | NGUYEN DANH HUNG | 01/10 | 01/10 | S7 |
| GHTK-43 | S4-05 – Duyệt đơn vượt hạn mức hoặc bán dưới giá sàn | EP-04 | 5 | NGUYEN DANH HUNG | 01/10 | 01/10 | S7 |
| GHTK-45 | S4-07 – Xem danh sách đơn hàng kèm bộ lọc | EP-04 | 3 | VU DUY LINH | 01/10 | 01/10 | S5 |
| GHTK-51 | S5-03 – Khai báo nhiều kho và vị trí lưu trong kho | EP-05 | 5 | TRAN DUY KHANH | 01/10 | 01/10 | — |
| GHTK-55 | S5-07 – Lập phiếu chuyển kho nội bộ | EP-05 | 5 | TRAN DUY KHANH | 03/10 | 03/10 | — |
| GHTK-70 | S7-04 – Có hệ thống tự cảnh báo và khoá bán khi đại lý vượt hạn mức hoặc quá hạn | EP-07 | 5 | TRAN DUY KHANH | 03/10 | 03/10 | S7 |

### Tuần 2 (05/10 – 11/10)

Tuần ít commit. Các story bắt đầu trong tuần này (lô hạn dùng, thẻ kho, theo dõi giao hàng, công nợ của đại lý, phiếu thu, nhắc hạn thanh toán) phần lớn được hoàn tất ở tuần 3.

| Jira | Story | Epic | SP | Phụ trách | Commit đầu | Commit cuối | Sprint trên Jira |
|---|---|---|---:|---|---|---|---|
| GHTK-48 | S4-10 – Tự đặt hàng trên cổng dành cho đại lý | EP-04 | 5 | NGUYEN KIM THANH LONG | 01/10 | 09/10 | S7 |
| GHTK-83 | S8-08 – Xuất báo cáo tồn kho ra tệp Excel | EP-05 | 3 | VU DUY LINH | 10/10 | 10/10 | S8 |

### Tuần 3 (12/10 – 19/10)

Mục tiêu đạt được: các màn hình kho, công nợ, trả hàng chuyển từ dữ liệu demo trên trình duyệt sang `Servlet → Service → DAO` với MySQL (xem [BACKEND_FIX_SUMMARY.md](../BACKEND_FIX_SUMMARY.md)); chu trình đơn → xuất kho → giao hàng → hoá đơn → thu tiền → công nợ → trả hàng chạy khép kín; có dashboard và báo cáo từ dữ liệu thật. Chốt code 18/10.

| Jira | Story | Epic | SP | Phụ trách | Commit đầu | Commit cuối | Sprint trên Jira |
|---|---|---|---:|---|---|---|---|
| GHTK-15 | S1-06 – Thấy menu điều hướng đúng theo quyền của mình | EP-01 | 5 | NGUYEN DANH HUNG | 30/09 | 15/10 | S1 |
| GHTK-44 | S4-06 – Theo dõi vòng đời và huỷ đơn hàng | EP-04 | 3 | NGUYEN DANH HUNG | 01/10 | 14/10 | S7 |
| GHTK-46 | S4-08 – In hoặc xuất PDF đơn hàng | EP-04 | 3 | NGUYEN DANH HUNG | 01/10 | 15/10 | S5 |
| GHTK-47 | S4-09 – Sao chép một đơn cũ thành đơn mới | EP-04 | 3 | NGUYEN DANH HUNG | 01/10 | 15/10 | S5 |
| GHTK-49 | S5-01 – Theo dõi trạng thái các đơn hàng của mình | EP-04 | 5 | NGUYEN THI LAM | 14/10 | 14/10 | S5 |
| GHTK-50 | S5-02 – Đặt lại một đơn đã mua trước đây | EP-04 | 3 | NGUYEN THI LAM | 15/10 | 15/10 | — |
| GHTK-52 | S5-04 – Lập phiếu nhập kho từ nhà cung cấp | EP-05 | 8 | TRAN DUY KHANH | 01/10 | 14/10 | S5 |
| GHTK-53 | S5-05 – Xem sổ tồn kho theo từng SKU và từng kho | EP-05 | 5 | TRAN DUY KHANH | 01/10 | 14/10 | S5 |
| GHTK-54 | S5-06 – Có tồn được giữ chỗ ngay khi đơn được duyệt | EP-05 | 5 | Dương Ngô Hùng | 18/10 | 18/10 | S5 |
| GHTK-56 | S5-08 – Thực hiện kiểm kê kho và ghi nhận chênh lệch | EP-05 | 5 | TRAN DUY KHANH | 15/10 | 15/10 | — |
| GHTK-57 | S5-09 – Được cảnh báo khi tồn xuống dưới mức tối thiểu | EP-05 | 2 | NGUYEN KIM THANH LONG | 04/10 | 18/10 | — |
| GHTK-58 | S6-01 – Theo dõi lô và hạn sử dụng của hàng hoá | EP-05 | 5 | NGUYEN DANH HUNG | 05/10 | 15/10 | — |
| GHTK-59 | S6-02 – Xem thẻ kho của một sản phẩm | EP-05 | 5 | NGUYEN DANH HUNG | 05/10 | 15/10 | S5 |
| GHTK-60 | S6-03 – Tạo phiếu soạn hàng từ đơn đã duyệt | EP-06 | 8 | NGUYEN DANH HUNG | 14/10 | 14/10 | S5 |
| GHTK-61 | S6-04 – Có hệ thống gợi ý lô xuất theo nguyên tắc hạn dùng gần nhất trước | EP-06 | 5 | NGUYEN THI LAM | 15/10 | 15/10 | — |
| GHTK-62 | S6-05 – Xác nhận xuất kho và xử lý giao thiếu | EP-06 | 5 | NGUYEN THI LAM | 14/10 | 14/10 | S5 |
| GHTK-63 | S6-06 – Phân tuyến giao hàng và gán tài xế | EP-06 | 5 | NGUYEN THI LAM | 16/10 | 18/10 | — |
| GHTK-64 | S6-07 – Theo dõi hàng đã xuất nhưng chưa giao xong | EP-06 | 3 | NGUYEN KIM THANH LONG | 06/10 | 18/10 | — |
| GHTK-65 | S6-08 – Xác nhận giao hàng thành công kèm ảnh chứng từ | EP-06 | 5 | VU DUY LINH | 11/10 | 16/10 | S7 |
| GHTK-66 | S6-09 – Theo dõi trạng thái giao hàng của đơn | EP-06 | 2 | NGUYEN KIM THANH LONG | 06/10 | 14/10 | S5 |
| GHTK-67 | S7-01 – Phát hành hoá đơn bán hàng từ phiếu đã giao | EP-07 | 8 | NGUYEN DANH HUNG | 14/10 | 14/10 | S5 |
| GHTK-68 | S7-02 – Ghi nhận thanh toán và đối trừ vào công nợ | EP-07 | 8 | NGUYEN DANH HUNG | 14/10 | 14/10 | S5 |
| GHTK-69 | S7-03 – Xem sổ công nợ theo đại lý với phân tích tuổi nợ | EP-07 | 5 | TRAN DUY KHANH | 03/10 | 14/10 | S5 |
| GHTK-71 | S7-05 – Thu tiền theo tuyến và nộp về công ty | EP-07 | 5 | VU DUY LINH | 03/10 | 18/10 | S7 |
| GHTK-72 | S7-06 – Lập biên bản đối chiếu công nợ định kỳ với đại lý | EP-07 | 5 | VU DUY LINH | 03/10 | 18/10 | S7 |
| GHTK-73 | S7-07 – Xem công nợ của chính mình | EP-07 | 3 | NGUYEN KIM THANH LONG | 08/10 | 14/10 | S5 |
| GHTK-74 | S7-08 – Nhận nhắc hạn thanh toán tự động | EP-07 | 3 | VU DUY LINH | 11/10 | 12/10 | S7 |
| GHTK-75 | S7-09 – In phiếu thu cho mỗi khoản đã ghi nhận | EP-07 | 2 | NGUYEN KIM THANH LONG | 09/10 | 15/10 | — |
| GHTK-76 | S8-01 – Lập phiếu trả hàng từ đại lý và nhập lại kho | EP-08 | 5 | TRAN DUY KHANH | 12/10 | 15/10 | S8 |
| GHTK-77 | S8-02 – Lập chứng từ điều chỉnh giảm trừ công nợ khi trả hàng | EP-08 | 5 | TRAN DUY KHANH | 12/10 | 12/10 | S8 |
| GHTK-78 | S8-03 – Lập phiếu điều chỉnh tồn kho có phê duyệt | EP-08 | 5 | NGUYEN THI LAM | 16/10 | 18/10 | — |
| GHTK-79 | S8-04 – Đặt chỉ tiêu doanh số cho nhân viên và theo dõi tiến độ | EP-09 | 5 | NGUYEN THI LAM | 18/10 | 19/10 | — |
| GHTK-80 | S8-05 – Xem dashboard điều hành bán hàng và kho | EP-09 | 8 | NGUYEN KIM THANH LONG | 12/10 | 15/10 | S8 |
| GHTK-81 | S8-06 – Xem báo cáo doanh số theo nhiều chiều | EP-09 | 5 | NGUYEN KIM THANH LONG | 14/10 | 15/10 | — |
| GHTK-82 | S8-07 – Xem báo cáo tồn kho và vòng quay hàng | EP-09 | 3 | VU DUY LINH | 12/10 | 14/10 | S8 |
| GHTK-84 | S8-09 – Xem báo cáo công nợ quá hạn theo tuổi nợ | EP-09 | 3 | VU DUY LINH | 14/10 | 16/10 | — |
| GHTK-85 | S8-10 – Xuất sổ chi tiết công nợ của một đại lý | EP-07 | 3 | VU DUY LINH | 03/10 | 12/10 | S8 |

## Chỗ lệch giữa Jira và thực tế

Cần chỉnh trên Jira nếu muốn board khớp với bảng trên:

- Sprint S3 và S5 vẫn ở trạng thái **active**, S7 và S8 vẫn **future** dù toàn bộ story đã Done. Không có sprint S4 và S6.
- 14 story không gắn sprint nào: GHTK-50, 51, 55, 56, 57, 58, 61, 63, 64, 75, 78, 79, 81, 84.
- Nhiều story gắn sprint không đúng tuần làm, ví dụ S4-01 … S4-10 (GHTK-39 → 48) làm xong ở tuần 1 nhưng nằm trong S5 / S7; S6-03, S7-01, S7-02 (GHTK-60, 67, 68) nằm trong S5.
- GHTK-56 (kiểm kê kho) và GHTK-74 (nhắc hạn thanh toán) không có commit mang mã riêng trên nhánh chính; ngày ghi trong bảng lấy theo commit tích hợp backend (15/10) và commit nhánh GHTK-74 (11–12/10).
