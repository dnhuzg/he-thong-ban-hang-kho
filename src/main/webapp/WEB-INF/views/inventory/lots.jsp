<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Lô & hạn sử dụng"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>
<link rel="stylesheet" href="${ctx}/assets/css/inventory-trace.css?v=${assetVersion}">

<section class="trace-screen" id="lots-screen" data-can-write="<c:out value='${canWrite}'/>" data-can-override="<c:out value='${canOverrideExpiry}'/>" data-user-id="<c:out value='${currentUser.id}'/>">
  <div class="page-header trace-header"><div><p class="breadcrumb">Tồn kho &amp; nhập kho / Quản lý lô</p><h1>Lô &amp; hạn sử dụng</h1><p class="page-header__desc">Theo dõi hàng còn lại theo lô và kiểm tra hạn dùng trước khi xuất.</p></div><a class="btn btn--secondary" href="${ctx}/inventory/stock-card">Xem thẻ kho</a></div>
  <div class="alert alert--info trace-note" role="note"><strong>Dữ liệu minh họa.</strong> Cấu hình chỉ lưu trên trình duyệt theo tài khoản. Kiểm tra xuất là mô phỏng, chưa ghi phiếu hoặc thay đổi tồn MySQL.</div>
  <div class="trace-stats" aria-label="Tổng quan lô đang lọc">
    <div class="card trace-stat"><span>Lô còn hàng</span><strong id="lots-total">0</strong><small>Trong phạm vi bộ lọc</small></div>
    <div class="card trace-stat trace-stat--warning"><span>Sắp hết hạn</span><strong id="lots-soon">0</strong><small>Theo ngưỡng của từng SKU</small></div>
    <div class="card trace-stat trace-stat--danger"><span>Đã quá hạn</span><strong id="lots-expired">0</strong><small>Cần quản lý kho ghi đè khi xuất</small></div>
  </div>
  <section class="card trace-panel" aria-labelledby="lot-settings-title">
    <header class="trace-panel__heading"><div><h2 id="lot-settings-title">Cấu hình theo SKU</h2><p>Bật quản lý lô và đặt số ngày cảnh báo sắp hết hạn.</p></div><span class="badge" id="lots-mode"></span></header>
    <form class="trace-filters" id="lot-settings">
      <div class="form-field"><label class="form-label" for="settings-sku">Sản phẩm</label><select class="select" id="settings-sku"></select></div>
      <div class="form-field"><label class="form-label" for="settings-days">Cảnh báo trước (ngày)</label><input class="input" id="settings-days" type="number" min="0" max="365" step="1" required></div>
      <label class="trace-check"><input type="checkbox" id="settings-enabled"> Quản lý theo lô</label>
      <button class="btn btn--primary" type="submit" id="settings-save" hidden>Lưu cấu hình mẫu</button>
    </form>
    <p class="trace-context" id="settings-context"></p>
  </section>
  <section class="card trace-panel" aria-labelledby="lots-title">
    <header class="trace-panel__heading"><div><h2 id="lots-title">Danh sách lô hàng</h2><p>Hạn dùng trong ngày hôm nay vẫn hợp lệ; lô hết hàng hiển thị riêng.</p></div><span class="badge" id="lots-count"></span></header>
    <form class="trace-filters" id="lots-filters" role="search">
      <div class="form-field"><label class="form-label" for="lots-search">Tìm SKU, tên hàng, số lô</label><input class="input" id="lots-search" type="search" placeholder="VD: SP-001 hoặc NK-01"></div>
      <div class="form-field"><label class="form-label" for="lots-warehouse">Kho</label><select class="select" id="lots-warehouse"></select></div>
      <div class="form-field"><label class="form-label" for="lots-status">Tình trạng</label><select class="select" id="lots-status"><option value="">Tất cả</option><option value="valid">Còn hạn</option><option value="soon">Sắp hết hạn</option><option value="expired">Đã quá hạn</option><option value="empty">Hết hàng</option></select></div>
      <button class="btn btn--secondary" type="reset">Xóa bộ lọc</button>
    </form>
    <div class="table-wrap"><table class="table trace-table"><caption class="visually-hidden">Lô, ngày sản xuất, hạn dùng và số lượng còn lại theo sản phẩm và kho</caption><thead><tr><th scope="col">Sản phẩm / SKU</th><th scope="col">Kho / số lô</th><th scope="col">Ngày sản xuất</th><th scope="col">Hạn sử dụng</th><th scope="col" class="trace-number">Còn lại</th><th scope="col">Tình trạng</th><th scope="col">Thao tác</th></tr></thead><tbody id="lots-rows"></tbody></table></div>
    <div class="trace-empty" id="lots-empty" hidden><h3>Không có lô phù hợp</h3><p>Thử xóa bộ lọc hoặc bật quản lý lô cho SKU cần xem.</p></div>
    <footer class="trace-footer">Số lượng theo đơn vị cơ sở. Không cộng chung các sản phẩm khác đơn vị.</footer>
  </section>
  <p id="lots-notice" class="alert alert--info trace-note" role="status" aria-live="polite" hidden></p>
  <dialog class="dialog trace-dialog" id="lot-export-dialog" aria-labelledby="lot-export-title">
    <form id="lot-export-form"><header class="dialog__header"><h2 id="lot-export-title">Kiểm tra xuất lô (mẫu)</h2></header><div class="dialog__body">
      <p id="lot-export-context" class="trace-note"></p>
      <div class="form-field"><label class="form-label" for="lot-export-quantity">Số lượng xuất thử *</label><input class="input" id="lot-export-quantity" type="number" min="0.001" step="0.001" required></div>
      <div id="lot-override-fields" hidden><p class="alert alert--warning trace-note">Lô đã quá hạn. Chỉ Quản lý kho được xác nhận ghi đè kèm lý do.</p><div class="form-field"><label class="form-label" for="lot-override-reason">Lý do ghi đè *</label><textarea class="textarea" id="lot-override-reason" rows="3" maxlength="500"></textarea></div><label class="trace-check"><input type="checkbox" id="lot-override-confirm"> Tôi xác nhận ghi đè hạn dùng cho lần xuất thử này</label></div>
      <p class="alert alert--danger trace-note" id="lot-export-error" role="alert" hidden></p>
    </div><footer class="dialog__footer"><button class="btn btn--secondary" type="button" id="lot-export-close">Đóng</button><button class="btn btn--primary" type="submit" id="lot-export-submit">Kiểm tra</button></footer></form>
  </dialog>
  <noscript><p class="alert alert--danger">Bật JavaScript để xem dữ liệu lô mẫu.</p></noscript>
</section>
<script src="${ctx}/assets/js/inventory-trace-demo.js?v=${assetVersion}" defer></script>
<script src="${ctx}/assets/js/inventory-lots.js?v=${assetVersion}" defer></script>
<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
