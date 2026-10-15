<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Giảm trừ công nợ trả hàng"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>
<link rel="stylesheet" href="${ctx}/assets/css/return-credits.css?v=${assetVersion}">

<section class="credit-screen" id="credit-screen" data-can-create="${canCreateReturnCredit}"
         data-csrf="<c:out value='${csrfToken}'/>" data-create-url="<c:out value='${ctx}/finance/return-credits/create'/>"
         data-page-url="<c:out value='${ctx}/finance/return-credits'/>">
  <div class="page-header credit-header"><div><p class="breadcrumb">Tài chính &amp; công nợ / Trả hàng</p><h1>Chứng từ giảm trừ công nợ</h1><p class="page-header__desc">Sinh chứng từ từ phiếu trả đã nhận và giữ nguyên hóa đơn gốc.</p></div><span class="credit-role">Kế toán công nợ</span></div>

  <div class="credit-stats" aria-label="Tổng quan công nợ"><article class="card credit-stat"><span>Công nợ hiện tại</span><strong id="credit-debt">—</strong><small>Trước chứng từ đang chọn</small></article><article class="card credit-stat"><span>Hạn mức</span><strong id="credit-limit">—</strong><small>Hạn mức của đại lý</small></article><article class="card credit-stat credit-stat--good"><span>Hạn mức khả dụng sau giảm</span><strong id="credit-available">—</strong><small>Cập nhật ngay khi lập chứng từ</small></article></div>

  <div class="credit-grid">
    <section class="card credit-panel"><div class="credit-heading"><div><h2>Phiếu trả đủ điều kiện</h2><p>Chỉ hiển thị phiếu đã được Quản lý kho xác nhận nhận hàng.</p></div><span class="credit-count" id="eligible-count">0 phiếu</span></div><div id="eligible-returns" class="credit-return-list"></div><div class="credit-empty" id="eligible-empty" hidden><strong>Không có phiếu chờ điều chỉnh</strong><p>Hãy xác nhận nhận hàng ở GHTK-76 trước.</p></div></section>

    <section class="card credit-panel credit-detail"><div class="credit-heading"><div><h2>Chi tiết chứng từ</h2><p id="credit-detail-note">Chọn một phiếu trả hàng ở bên trái.</p></div><span class="credit-status" id="credit-status">Chưa chọn</span></div>
      <div id="credit-detail-empty" class="credit-placeholder"><span>₫</span><p>Giá trị giảm trừ được tính theo đơn giá đã bán trên hóa đơn gốc.</p></div>
      <div id="credit-detail-body" hidden><dl class="credit-meta"><div><dt>Phiếu trả</dt><dd id="detail-return-code"></dd></div><div><dt>Hóa đơn gốc</dt><dd id="detail-invoice"></dd></div><div><dt>Đại lý</dt><dd id="detail-customer"></dd></div><div><dt>Ngày kho nhận</dt><dd id="detail-received"></dd></div></dl>
        <div class="credit-table-wrap"><table class="table credit-lines"><thead><tr><th>Sản phẩm</th><th>SL trả</th><th>Giá trên HĐ gốc</th><th>Giảm trừ</th></tr></thead><tbody id="credit-lines"></tbody><tfoot><tr><td colspan="3">Tổng giảm công nợ</td><td id="credit-total"></td></tr></tfoot></table></div>
        <div class="credit-invoice-lock"><span>🔒</span><div><strong>Hóa đơn gốc được giữ nguyên</strong><p>Chứng từ mới chỉ tham chiếu hóa đơn, không sửa đơn giá, số lượng hay tổng tiền trên hóa đơn.</p></div></div>
        <p class="credit-error" id="credit-error" role="alert" hidden></p><div class="credit-actions"><button class="btn btn--primary" type="button" id="credit-create">Lập chứng từ giảm trừ</button></div>
      </div>
    </section>
  </div>

  <section class="card credit-panel credit-history"><div class="credit-heading"><div><h2>Chứng từ đã lập</h2><p>Lịch sử giảm công nợ từ hàng trả.</p></div><span class="credit-count" id="document-count">0 chứng từ</span></div><div class="credit-table-wrap"><table class="table"><thead><tr><th>Mã chứng từ</th><th>Phiếu trả</th><th>Hóa đơn gốc</th><th>Đại lý</th><th>Số tiền giảm</th><th>Trạng thái</th></tr></thead><tbody id="credit-documents"></tbody></table></div><div class="credit-empty" id="document-empty"><strong>Chưa có chứng từ</strong><p>Chứng từ mới sẽ xuất hiện tại đây.</p></div></section>
  <p class="credit-footnote">GHTK-77 · S8-02 · Điều chỉnh giảm trừ công nợ khi trả hàng</p>
</section>
<script type="application/json" id="return-credit-data"><c:out value="${returnCreditData}" escapeXml="false"/></script>
<script src="${ctx}/assets/js/return-credits.js?v=${assetVersion}" defer></script>
<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
