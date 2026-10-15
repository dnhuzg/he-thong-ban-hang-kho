<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Cảnh báo hạn mức công nợ"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>
<link rel="stylesheet" href="${ctx}/assets/css/credit-alerts.css?v=${assetVersion}">

<section class="credit-screen" id="credit-screen">
  <header class="page-header credit-header">
    <div><p class="breadcrumb">Hoá đơn &amp; công nợ / Cảnh báo hạn mức</p><h1>Cảnh báo và khoá bán theo công nợ</h1><p class="page-header__desc">GHTK-70 · S7-04 · Theo dõi đại lý vượt hạn mức hoặc quá số ngày nợ cho phép.</p></div>
    <button class="btn btn--secondary" type="button" id="credit-recheck">Đánh giá lại</button>
  </header>


  <div class="credit-summary" aria-label="Tổng hợp cảnh báo">
    <div class="card credit-stat"><span>Đại lý có cờ</span><strong id="credit-flagged">0</strong><small>Vượt hạn mức hoặc quá ngày nợ</small></div>
    <div class="card credit-stat"><span>Vượt hạn mức tiền</span><strong id="credit-over-limit">0</strong><small>Còn nợ lớn hơn hạn mức</small></div>
    <div class="card credit-stat"><span>Quá số ngày nợ</span><strong id="credit-over-days">0</strong><small>Khoản nợ mở lâu hơn quy định</small></div>
    <div class="card credit-stat"><span>Đại lý bình thường</span><strong id="credit-clear">0</strong><small>Không có điều kiện gắn cờ</small></div>
  </div>

  <section class="card credit-panel" aria-labelledby="credit-list-title">
    <div class="credit-panel__heading"><div><h2 id="credit-list-title">Danh sách đại lý</h2><p id="credit-checked-at">Chưa đánh giá</p></div><span class="badge" id="credit-count">0 đại lý</span></div>
    <form class="credit-filters" id="credit-filters" role="search">
      <div class="form-field credit-filters__search"><label class="form-label" for="credit-search">Tìm đại lý</label><input class="input" id="credit-search" type="search" placeholder="Tên hoặc mã đại lý"></div>
      <div class="form-field"><label class="form-label" for="credit-status">Cờ cảnh báo</label><select class="select" id="credit-status"><option value="">Tất cả</option><option value="flagged">Chỉ đại lý có cờ</option><option value="clear">Chỉ đại lý bình thường</option></select></div>
      <div class="form-field"><label class="form-label" for="credit-policy">Cách hiển thị cảnh báo</label><select class="select" id="credit-policy"><option value="block">Cần chặn đơn mới</option><option value="review">Cần quản lý duyệt</option></select></div>
      <button class="btn btn--ghost" type="reset">Xoá bộ lọc</button>
    </form>
    <div class="table-wrap credit-table-wrap"><table class="table credit-table"><caption class="visually-hidden">Hạn mức, dư nợ, số ngày nợ và trạng thái đơn theo đại lý</caption><thead><tr><th scope="col">Đại lý</th><th scope="col" class="credit-number">Còn nợ</th><th scope="col" class="credit-number">Hạn mức</th><th scope="col">Nợ lâu nhất / tối đa</th><th scope="col">Lý do gắn cờ</th><th scope="col">Đơn mới</th></tr></thead><tbody id="credit-rows"></tbody></table></div>
    <div class="credit-mobile" id="credit-mobile"></div>
    <div class="credit-empty" id="credit-empty" hidden><h3>Không có đại lý phù hợp</h3><p>Thử thay đổi từ khoá hoặc bộ lọc.</p></div>
    <footer class="credit-footer"><span id="credit-visible">Hiển thị 0 đại lý</span><span>Đại lý có cả hai lý do chỉ được đếm một lần trong tổng có cờ.</span></footer>
  </section>

  <noscript><div class="alert alert--danger">Bật JavaScript để xem dữ liệu cảnh báo.</div></noscript>
</section>
<script type="application/json" id="credit-alert-data"><c:out value="${creditAlertData}" escapeXml="false"/></script>
<script src="${ctx}/assets/js/credit-alerts.js?v=${assetVersion}" defer></script>
<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
