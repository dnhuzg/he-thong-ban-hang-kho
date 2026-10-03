<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Sổ chi tiết công nợ đại lý"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>
<link rel="stylesheet" href="${ctx}/assets/css/customer-ledger.css?v=${assetVersion}">
<section id="ledger-screen" class="ledger-screen">
  <header class="page-header"><div><p class="breadcrumb">Hoá đơn &amp; công nợ / Sổ chi tiết</p><h1>Sổ chi tiết công nợ đại lý</h1><p class="page-header__desc">GHTK-85 · S8-10 · Theo dõi phát sinh và số dư lũy kế của một đại lý.</p></div></header>
  <div class="alert alert--info ledger-demo" role="note"><strong>Giao diện FE mẫu.</strong> Số liệu dưới đây được tạo để xem bố cục, chưa lấy từ sổ kế toán thật. Xuất PDF mở hộp thoại in; Xuất Excel tải tệp .xlsx của dữ liệu đang hiển thị.</div>
  <form class="card ledger-filters" id="ledger-filters">
    <div class="form-field"><label class="form-label" for="ledger-agency">Đại lý</label><select class="select" id="ledger-agency"><option value="minh-anh">Đại lý Minh Anh</option><option value="thai-nguyen">Đại lý Thái Nguyên</option><option value="huong-giang">Đại lý Hương Giang</option></select></div>
    <div class="form-field"><label class="form-label" for="ledger-from">Từ ngày</label><input class="input" type="date" id="ledger-from" required></div>
    <div class="form-field"><label class="form-label" for="ledger-to">Đến ngày</label><input class="input" type="date" id="ledger-to" required></div>
    <button class="btn btn--primary" type="submit">Lọc sổ</button>
  </form>
  <p class="ledger-feedback" id="ledger-feedback" role="status" aria-live="polite"></p>
  <section class="card ledger-document" id="ledger-document" aria-labelledby="ledger-title">
    <header class="ledger-document__head"><div><p class="ledger-company">CÔNG TY · BÁN HÀNG &amp; KHO</p><h2 id="ledger-title">SỔ CHI TIẾT CÔNG NỢ ĐẠI LÝ</h2><p id="ledger-meta"></p><p class="ledger-preview">BẢN XEM TRƯỚC · DỮ LIỆU MẪU</p></div><div class="ledger-actions"><button class="btn btn--secondary" type="button" id="ledger-pdf">Xuất PDF</button><button class="btn btn--primary" type="button" id="ledger-excel">Xuất Excel</button></div></header>
    <div class="ledger-summary" aria-label="Tổng hợp sổ"><div><span>Dư đầu khoảng</span><strong id="ledger-opening"></strong></div><div><span>Phát sinh tăng</span><strong id="ledger-increase"></strong></div><div><span>Phát sinh giảm</span><strong id="ledger-decrease"></strong></div><div><span>Dư cuối khoảng</span><strong id="ledger-closing"></strong></div></div>
    <div class="table-wrap"><table class="table ledger-table"><caption class="visually-hidden">Phát sinh tăng giảm và dư lũy kế theo thời gian</caption><thead><tr><th scope="col">Ngày</th><th scope="col">Chứng từ</th><th scope="col">Diễn giải</th><th scope="col">Tăng</th><th scope="col">Giảm</th><th scope="col">Dư lũy kế</th></tr></thead><tbody id="ledger-rows"></tbody><tfoot><tr><th colspan="3" scope="row">Cộng trong khoảng</th><td id="ledger-total-increase"></td><td id="ledger-total-decrease"></td><td id="ledger-total-closing"></td></tr></tfoot></table></div>
    <p id="ledger-empty" class="ledger-empty" hidden>Không có phát sinh trong khoảng ngày này. Dư đầu và dư cuối vẫn được tính từ các giao dịch trước đó.</p>
    <p class="ledger-note">Dư lũy kế = dư trước đó + phát sinh tăng − phát sinh giảm. Số dương là đại lý còn phải thanh toán.</p>
  </section>
  <noscript><div class="alert alert--danger">Bật JavaScript để xem sổ công nợ mẫu.</div></noscript>
</section>
<script src="${ctx}/assets/js/customer-ledger-xlsx.js?v=${assetVersion}" defer></script>
<script src="${ctx}/assets/js/customer-ledger.js?v=${assetVersion}" defer></script>
<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
