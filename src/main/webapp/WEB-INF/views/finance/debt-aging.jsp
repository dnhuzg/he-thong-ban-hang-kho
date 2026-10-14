<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Báo cáo công nợ theo tuổi nợ"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>
<link rel="stylesheet" href="${ctx}/assets/css/debt-aging.css?v=${assetVersion}">

<section class="aging-screen" id="aging-screen">
  <header class="page-header aging-header">
    <div>
      <p class="breadcrumb">Hoá đơn &amp; công nợ / Báo cáo tuổi nợ</p>
      <h1>Báo cáo công nợ quá hạn theo tuổi nợ</h1>
      <p class="page-header__desc">Tổng hợp theo nhóm tuổi nợ và nhân viên phụ trách, ưu tiên đại lý có rủi ro cao · S8-09.</p>
    </div>
    <button class="btn btn--primary" id="aging-export" type="button">Xuất Excel</button>
  </header>

  <div class="alert alert--info aging-demo" role="note"><strong>Giao diện FE mẫu.</strong> Số liệu bên dưới chỉ để minh hoạ và chưa đồng bộ với hoá đơn, thanh toán hoặc công nợ trong MySQL. Tệp xuất dùng dữ liệu đang hiển thị.</div>

  <div class="aging-summary" aria-label="Tổng hợp công nợ">
    <div class="card aging-stat aging-stat--total"><span>Tổng còn nợ</span><strong id="aging-total">0 ₫</strong><small>Cộng bốn nhóm tuổi nợ</small></div>
    <div class="card aging-stat"><span>0–30 ngày</span><strong id="aging-bucket-0">0 ₫</strong><small>Tính từ ngày phát sinh</small></div>
    <div class="card aging-stat"><span>31–60 ngày</span><strong id="aging-bucket-1">0 ₫</strong><small>Tính từ ngày phát sinh</small></div>
    <div class="card aging-stat"><span>61–90 ngày</span><strong id="aging-bucket-2">0 ₫</strong><small>Tính từ ngày phát sinh</small></div>
    <div class="card aging-stat aging-stat--late"><span>Trên 90 ngày</span><strong id="aging-bucket-3">0 ₫</strong><small>Tính từ ngày phát sinh</small></div>
  </div>

  <section class="card aging-risk-card" aria-labelledby="aging-risk-title" aria-live="polite">
    <div class="aging-risk-card__icon" aria-hidden="true">!</div>
    <div class="aging-risk-card__content">
      <p class="aging-risk-card__eyebrow">Ưu tiên xử lý</p>
      <h2 id="aging-risk-title">Đại lý có rủi ro công nợ cao nhất</h2>
      <strong class="aging-risk-card__name" id="aging-risk-name">Đang tổng hợp…</strong>
      <p class="aging-risk-card__meta" id="aging-risk-meta"></p>
      <p class="aging-risk-card__rule">Xếp hạng theo dư nợ quá hạn giảm dần; nếu bằng nhau, ưu tiên dư nợ trên 90 ngày và số ngày quá hạn dài hơn.</p>
    </div>
    <div class="aging-risk-card__amount">
      <span>Dư nợ quá hạn</span>
      <strong id="aging-risk-balance">—</strong>
      <small id="aging-risk-days"></small>
    </div>
  </section>

  <section class="card aging-panel aging-staff-panel" aria-labelledby="aging-staff-title">
    <div class="aging-panel__heading">
      <div><h2 id="aging-staff-title">Tổng hợp theo nhân viên phụ trách</h2><p>Số liệu được tính từ cùng tập khoản nợ đang áp dụng bộ lọc phía dưới.</p></div>
      <span class="badge" id="aging-staff-count">0 nhân viên</span>
    </div>
    <div class="table-wrap">
      <table class="table aging-staff-table">
        <caption class="visually-hidden">Tổng hợp công nợ và các nhóm tuổi nợ theo nhân viên phụ trách</caption>
        <thead><tr><th scope="col">Mã NV</th><th scope="col">Nhân viên phụ trách</th><th scope="col" class="aging-number">Số đại lý</th><th scope="col" class="aging-number">Nợ quá hạn</th><th scope="col" class="aging-number">0–30</th><th scope="col" class="aging-number">31–60</th><th scope="col" class="aging-number">61–90</th><th scope="col" class="aging-number">&gt;90 ngày</th><th scope="col" class="aging-number">Tổng còn nợ</th></tr></thead>
        <tbody id="aging-staff-rows"></tbody>
        <tfoot><tr><th scope="row" colspan="3">Tổng theo nhân viên</th><td class="aging-number" id="aging-staff-overdue-total">0 ₫</td><td class="aging-number" id="aging-staff-0">0 ₫</td><td class="aging-number" id="aging-staff-1">0 ₫</td><td class="aging-number" id="aging-staff-2">0 ₫</td><td class="aging-number" id="aging-staff-3">0 ₫</td><td class="aging-number" id="aging-staff-balance-total">0 ₫</td></tr></tfoot>
      </table>
    </div>
    <div class="aging-empty" id="aging-staff-empty" hidden><p>Không có dữ liệu nhân viên theo bộ lọc hiện tại.</p></div>
  </section>

  <section class="card aging-panel" aria-labelledby="aging-list-title">
    <div class="aging-panel__heading"><div><h2 id="aging-list-title">Chi tiết công nợ phải thu</h2><p>Tuổi nợ tính từ ngày phát sinh; quá hạn tính từ ngày đến hạn.</p></div><span class="badge" id="aging-count">0 đại lý</span></div>
    <form class="aging-filters" id="aging-filters" role="search">
      <div class="form-field aging-filters__search"><label class="form-label" for="aging-search">Tìm đại lý, hoá đơn hoặc nhân viên</label><input class="input" id="aging-search" type="search" placeholder="Tên/mã đại lý, hoá đơn, nhân viên" autocomplete="off"></div>
      <div class="form-field"><label class="form-label" for="aging-bucket">Nhóm tuổi nợ</label><select class="select" id="aging-bucket"><option value="">Tất cả nhóm</option><option value="0">0–30 ngày</option><option value="1">31–60 ngày</option><option value="2">61–90 ngày</option><option value="3">Trên 90 ngày</option></select></div>
      <div class="form-field"><label class="form-label" for="aging-as-of">Ngày đối chiếu</label><input class="input" id="aging-as-of" type="date"></div>
      <label class="aging-overdue"><input id="aging-overdue" type="checkbox"> Chỉ xem quá hạn</label>
      <button class="btn btn--ghost aging-reset" type="reset">Xoá bộ lọc</button>
    </form>
    <div class="table-wrap aging-table-wrap"><table class="table aging-table"><caption class="visually-hidden">Công nợ phải thu, nhân viên phụ trách và bốn nhóm tuổi nợ theo đại lý</caption><thead><tr><th scope="col">Đại lý</th><th scope="col">Nhân viên phụ trách</th><th scope="col" class="aging-number">Phải thu</th><th scope="col" class="aging-number">Đã thu</th><th scope="col" class="aging-number">Còn nợ</th><th scope="col" class="aging-number">0–30</th><th scope="col" class="aging-number">31–60</th><th scope="col" class="aging-number">61–90</th><th scope="col" class="aging-number">&gt;90 ngày</th><th scope="col">Trạng thái</th></tr></thead><tbody id="aging-rows"></tbody><tfoot><tr><th scope="row" colspan="4">Tổng còn nợ theo bộ lọc</th><td class="aging-number" id="aging-table-total">0 ₫</td><td class="aging-number" id="aging-table-0">0 ₫</td><td class="aging-number" id="aging-table-1">0 ₫</td><td class="aging-number" id="aging-table-2">0 ₫</td><td class="aging-number" id="aging-table-3">0 ₫</td><td></td></tr></tfoot></table></div>
    <div id="aging-mobile" class="aging-mobile"></div>
    <div class="aging-empty" id="aging-empty" hidden><h3>Không có khoản nợ phù hợp</h3><p>Thử thay đổi bộ lọc hoặc ngày đối chiếu.</p></div>
    <p class="aging-export-feedback" id="aging-export-feedback" role="status" aria-live="polite"></p>
    <footer class="aging-footer"><span id="aging-footer-count">Hiển thị 0 đại lý</span><span id="aging-balance">Đối chiếu: 0 ₫ = 0 ₫</span></footer>
  </section>
  <noscript><div class="alert alert--danger">Bật JavaScript để xem dữ liệu công nợ mẫu.</div></noscript>
</section>
<script src="${ctx}/assets/js/customer-ledger-xlsx.js?v=${assetVersion}" defer></script>
<script src="${ctx}/assets/js/debt-aging.js?v=${assetVersion}" defer></script>
<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
