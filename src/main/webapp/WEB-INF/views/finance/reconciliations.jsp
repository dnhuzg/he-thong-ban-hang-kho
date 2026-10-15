<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Biên bản đối chiếu công nợ"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>
<link rel="stylesheet" href="${ctx}/assets/css/reconciliations.css?v=${assetVersion}">
<section class="recon-screen" id="recon-screen" data-portal="${reconciliationPortal}" data-csrf="<c:out value='${csrfToken}'/>" data-create-url="<c:out value='${ctx}/finance/reconciliations/create'/>" data-respond-url="<c:out value='${ctx}/portal/reconciliations/respond'/>" data-page-url="<c:choose><c:when test='${reconciliationPortal}'><c:out value='${ctx}/portal/reconciliations'/></c:when><c:otherwise><c:out value='${ctx}/finance/reconciliations'/></c:otherwise></c:choose>">
  <header class="page-header"><div><p class="breadcrumb"><c:choose><c:when test="${reconciliationPortal}">Cổng đại lý</c:when><c:otherwise>Hoá đơn &amp; công nợ</c:otherwise></c:choose> / Đối chiếu công nợ</p><h1>Biên bản đối chiếu công nợ</h1><p class="page-header__desc">GHTK-72 · S7-06 · Số dư đầu kỳ, phát sinh và số dư cuối kỳ theo đại lý.</p></div></header>
  <section class="card recon-controls" aria-labelledby="recon-controls-title"><h2 id="recon-controls-title"><c:choose><c:when test="${reconciliationPortal}">Biên bản của đại lý</c:when><c:otherwise>Lập biên bản theo kỳ</c:otherwise></c:choose></h2>
    <div class="recon-controls__fields">
      <c:if test="${not reconciliationPortal}"><div class="form-field"><label class="form-label" for="recon-agency">Đại lý</label><select class="select" id="recon-agency"></select></div></c:if>
      <div class="form-field"><label class="form-label" for="recon-period">Kỳ đối chiếu</label><select class="select" id="recon-period"></select></div>
      <button class="btn btn--primary" type="button" id="recon-generate"><c:choose><c:when test="${reconciliationPortal}">Xem biên bản</c:when><c:otherwise>Lập biên bản</c:otherwise></c:choose></button>
    </div>
  </section>

  <section class="card recon-document" id="recon-document" aria-labelledby="recon-document-title">
    <div class="recon-document__head"><div><p class="recon-company">CÔNG TY · BÁN HÀNG &amp; KHO</p><h2 id="recon-document-title">BIÊN BẢN ĐỐI CHIẾU CÔNG NỢ</h2><p id="recon-subtitle"></p></div><button class="btn btn--secondary recon-print" type="button" id="recon-print">Xuất PDF</button></div>
    <div class="recon-meta"><p><strong>Đại lý:</strong> <span id="recon-name"></span></p><p><strong>Kỳ:</strong> <span id="recon-dates"></span></p><p><strong>Mã biên bản:</strong> <span id="recon-code"></span></p></div>
    <div class="recon-summary" aria-label="Tổng hợp số dư"><div><span>Số dư đầu kỳ</span><strong id="recon-opening"></strong></div><div><span>Phát sinh tăng</span><strong id="recon-increase"></strong></div><div><span>Đã thanh toán</span><strong id="recon-payment"></strong></div><div><span>Số dư cuối kỳ</span><strong id="recon-closing"></strong></div></div>
    <div class="table-wrap"><table class="table recon-table"><caption class="visually-hidden">Chi tiết các khoản phát sinh trong kỳ</caption><thead><tr><th scope="col">Ngày</th><th scope="col">Nội dung</th><th scope="col">Phát sinh tăng</th><th scope="col">Thanh toán</th></tr></thead><tbody id="recon-rows"></tbody></table></div>
    <p class="recon-formula">Số dư cuối kỳ = Số dư đầu kỳ + Phát sinh tăng − Đã thanh toán. Số tiền dương là đại lý còn phải thanh toán.</p>
    <div class="recon-signatures"><div><strong>ĐẠI DIỆN CÔNG TY</strong><span>Ký, ghi rõ họ tên</span></div><div><strong>ĐẠI DIỆN ĐẠI LÝ</strong><span>Ký, ghi rõ họ tên</span></div></div>
  </section>

  <section class="card recon-response" aria-labelledby="recon-response-title"><h2 id="recon-response-title">Xác nhận số liệu</h2><p id="recon-status" role="status" aria-live="polite"></p>
    <c:choose><c:when test="${reconciliationPortal}"><div class="recon-response__actions"><button class="btn btn--primary" type="button" id="recon-accept">Xác nhận khớp số liệu</button><button class="btn btn--secondary" type="button" id="recon-dispute-toggle">Phản hồi chênh lệch</button></div><form id="recon-dispute-form" hidden><div class="form-field"><label class="form-label" for="recon-reason">Khoản chênh lệch và lý do</label><textarea class="input recon-textarea" id="recon-reason" minlength="10" maxlength="1000" required placeholder="Nêu số tiền chênh lệch, chứng từ và lý do..."></textarea></div><button class="btn btn--primary" type="submit">Gửi phản hồi</button></form></c:when><c:otherwise><p class="recon-hint">Trạng thái phản hồi của đại lý được cập nhật trực tiếp từ hệ thống.</p></c:otherwise></c:choose>
  </section>
  <noscript><div class="alert alert--danger">Bật JavaScript để xem biên bản.</div></noscript>
</section>
<script type="application/json" id="reconciliation-data"><c:out value="${reconciliationData}" escapeXml="false"/></script>
<script src="${ctx}/assets/js/reconciliations.js?v=${assetVersion}" defer></script>
<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
