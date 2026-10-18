<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Sổ chi tiết công nợ"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>
<link rel="stylesheet" href="${ctx}/assets/css/debt-ledger.css?v=${assetVersion}">

<section class="ledger-screen" id="ledger-screen">
  <div class="page-header ledger-header">
    <div><p class="breadcrumb">Tài chính &amp; công nợ / Đối chiếu</p><h1>Sổ chi tiết công nợ đại lý</h1><p class="page-header__desc">Theo dõi phát sinh tăng, giảm và số dư lũy kế trong kỳ.</p></div>
    <span class="ledger-role">Kế toán công nợ</span>
  </div>

  <section class="card ledger-filter" aria-label="Bộ lọc sổ công nợ">
    <label><span>Đại lý</span><select id="ledger-customer"></select></label>
    <label><span>Từ ngày</span><input type="date" id="ledger-from"></label>
    <label><span>Đến ngày</span><input type="date" id="ledger-to"></label>
    <button class="btn btn--primary" type="button" id="ledger-apply">Xem sổ</button>
    <p class="ledger-error" id="ledger-error" role="alert" hidden></p>
  </section>

  <section class="ledger-summary" aria-label="Tổng hợp công nợ">
    <article class="card ledger-stat"><span>Dư đầu kỳ</span><strong id="sum-opening">—</strong></article>
    <article class="card ledger-stat ledger-stat--up"><span>Phát sinh tăng</span><strong id="sum-increase">—</strong></article>
    <article class="card ledger-stat ledger-stat--down"><span>Phát sinh giảm</span><strong id="sum-decrease">—</strong></article>
    <article class="card ledger-stat ledger-stat--balance"><span>Dư cuối kỳ</span><strong id="sum-closing">—</strong></article>
  </section>

  <section class="card ledger-card">
    <div class="ledger-toolbar">
      <div><h2 id="ledger-title">Sổ chi tiết công nợ</h2><p id="ledger-period"></p></div>
      <div class="ledger-actions"><button class="btn" type="button" id="export-pdf">Xuất PDF</button><button class="btn btn--primary" type="button" id="export-excel">Xuất Excel</button></div>
    </div>
    <div class="ledger-table-wrap">
      <table class="table ledger-table" id="ledger-table"><thead><tr><th>Ngày</th><th>Chứng từ</th><th>Diễn giải</th><th>Phát sinh tăng</th><th>Phát sinh giảm</th><th>Số dư</th></tr></thead><tbody id="ledger-rows"></tbody><tfoot><tr><td colspan="3">Cộng phát sinh trong kỳ</td><td id="foot-increase"></td><td id="foot-decrease"></td><td id="foot-closing"></td></tr></tfoot></table>
    </div>
    <div class="ledger-empty" id="ledger-empty" hidden><strong>Không có phát sinh trong khoảng thời gian này</strong><p>Hãy chọn khoảng thời gian khác để đối chiếu.</p></div>
    <p class="ledger-note">Số dư = Dư đầu kỳ + Phát sinh tăng − Phát sinh giảm.</p>
  </section>
</section>
<script type="application/json" id="ledger-data"><c:out value="${ledgerData}" escapeXml="false"/></script>
<script src="${ctx}/assets/js/debt-ledger.js?v=${assetVersion}" defer></script>
<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
