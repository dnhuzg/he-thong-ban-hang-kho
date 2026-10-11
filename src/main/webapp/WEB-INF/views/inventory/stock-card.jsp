<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Thẻ kho sản phẩm"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>
<link rel="stylesheet" href="${ctx}/assets/css/inventory-trace.css?v=${assetVersion}">

<section class="trace-screen" id="stock-card-screen">
  <div class="page-header trace-header"><div><p class="breadcrumb">Tồn kho &amp; nhập kho / Thẻ kho</p><h1>Thẻ kho sản phẩm</h1><p class="page-header__desc">Truy nguyên từng phát sinh và đối chiếu số tồn sau chứng từ.</p></div><a class="btn btn--secondary" href="${ctx}/inventory/lots">Lô &amp; hạn sử dụng</a></div>
  <div class="alert alert--info trace-note" role="note"><strong>Dữ liệu minh họa.</strong> Lịch sử và chứng từ dưới đây là mẫu, chưa đọc sổ tồn kho MySQL.</div>
  <section class="card trace-panel" aria-labelledby="card-filter-title">
    <header class="trace-panel__heading"><div><h2 id="card-filter-title">Tra cứu thẻ kho</h2><p>Chọn sản phẩm, kho và khoảng thời gian cần đối chiếu.</p></div></header>
    <form class="trace-filters trace-filters--card" id="card-filters" role="search">
      <div class="form-field"><label class="form-label" for="card-sku">Sản phẩm</label><select class="select" id="card-sku"></select></div>
      <div class="form-field"><label class="form-label" for="card-warehouse">Kho</label><select class="select" id="card-warehouse"></select></div>
      <div class="form-field"><label class="form-label" for="card-from">Từ ngày</label><input class="input" id="card-from" type="date"></div>
      <div class="form-field"><label class="form-label" for="card-to">Đến ngày</label><input class="input" id="card-to" type="date"></div>
      <button class="btn btn--secondary" type="reset">Xóa bộ lọc</button>
    </form>
    <p class="alert alert--danger trace-context" id="card-error" role="alert" hidden></p>
  </section>
  <div class="trace-stats trace-stats--four" aria-label="Tổng hợp theo sản phẩm, kho và khoảng ngày">
    <div class="card trace-stat"><span>Tồn đầu kỳ</span><strong id="card-opening">—</strong><small>Gồm phát sinh trước ngày bắt đầu</small></div>
    <div class="card trace-stat"><span>Tăng trong kỳ</span><strong id="card-in">—</strong><small>Nhập, chuyển đến, điều chỉnh tăng</small></div>
    <div class="card trace-stat"><span>Giảm trong kỳ</span><strong id="card-out">—</strong><small>Xuất, chuyển đi, điều chỉnh giảm</small></div>
    <div class="card trace-stat"><span>Tồn cuối kỳ</span><strong id="card-closing">—</strong><small>Đầu kỳ + tăng − giảm</small></div>
  </div>
  <section class="card trace-panel" aria-labelledby="card-title">
    <header class="trace-panel__heading"><div><h2 id="card-title">Lịch sử phát sinh</h2><p id="card-context"></p></div><span class="badge" id="card-count"></span></header>
    <div class="table-wrap"><table class="table trace-table"><caption class="visually-hidden">Phát sinh nhập xuất chuyển điều chỉnh, chứng từ gốc và tồn sau phát sinh</caption><thead><tr><th scope="col">Thời điểm</th><th scope="col">Chứng từ gốc</th><th scope="col">Loại phát sinh</th><th scope="col">Kho</th><th scope="col" class="trace-number">Tăng</th><th scope="col" class="trace-number">Giảm</th><th scope="col" class="trace-number">Tồn sau</th><th scope="col">Diễn giải</th></tr></thead><tbody id="card-rows"></tbody></table></div>
    <div class="trace-empty" id="card-empty" hidden><h3>Không có phát sinh trong kỳ</h3><p>Tồn đầu kỳ vẫn được tính từ lịch sử trước kỳ. Thử chọn khoảng ngày khác.</p></div>
    <footer class="trace-footer" id="card-footer">Thứ tự thời gian từ cũ đến mới, cùng thời điểm thì theo mã phát sinh.</footer>
  </section>
  <dialog class="dialog trace-dialog" id="card-document" aria-labelledby="card-document-title"><header class="dialog__header"><h2 id="card-document-title">Chứng từ mẫu</h2></header><div class="dialog__body"><p class="alert alert--info trace-note">Bản xem chứng từ minh họa của phát sinh đã chọn.</p><dl class="trace-document" id="card-document-detail"></dl></div><footer class="dialog__footer"><button class="btn btn--secondary" type="button" id="card-document-close">Đóng</button></footer></dialog>
  <noscript><p class="alert alert--danger">Bật JavaScript để xem thẻ kho mẫu.</p></noscript>
</section>
<script src="${ctx}/assets/js/inventory-trace-demo.js?v=${assetVersion}" defer></script>
<script src="${ctx}/assets/js/stock-card.js?v=${assetVersion}" defer></script>
<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
