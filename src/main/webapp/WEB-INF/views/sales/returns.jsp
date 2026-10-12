<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Trả hàng từ đại lý"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>
<link rel="stylesheet" href="${ctx}/assets/css/returns.css?v=${assetVersion}">

<section class="returns-screen" id="returns-screen"
         data-can-create="${canCreateReturn}" data-can-confirm="${canConfirmReturn}">
  <div class="page-header returns-header">
    <div><p class="breadcrumb">Bán hàng / Trả hàng từ đại lý</p><h1>Phiếu trả hàng</h1>
      <p class="page-header__desc">Ghi nhận hàng đại lý trả và chờ Quản lý kho xác nhận nhận hàng.</p></div>
    <span class="returns-status"><span class="returns-status__dot"></span>Quy trình 2 bước</span>
  </div>
  <div class="alert alert--info returns-demo" role="note"><strong>Bản FE GHTK-76.</strong> Dữ liệu và tồn kho hiển thị là minh họa, lưu trong trình duyệt hiện tại; chưa ghi vào cơ sở dữ liệu.</div>

  <div class="returns-grid">
    <main class="returns-main">
      <section class="card returns-card" id="return-create-section" <c:if test="${not canCreateReturn}">hidden</c:if>>
        <div class="returns-card__heading"><span class="returns-icon"><span class="icon icon--clipboard-list" aria-hidden="true"></span></span><div><h2>Lập phiếu trả hàng</h2><p>Đối chiếu hóa đơn gốc rồi thêm hàng cần trả.</p></div><span class="returns-step">Bước 1</span></div>
        <div class="returns-form">
          <div class="form-field"><label class="form-label" for="return-invoice">Hóa đơn gốc <span class="required">*</span></label><select class="select" id="return-invoice"><option value="">— Chọn hóa đơn —</option></select></div>
          <div class="form-field"><label class="form-label" for="return-line">Dòng hàng <span class="required">*</span></label><select class="select" id="return-line" disabled><option value="">Chọn hóa đơn trước</option></select></div>
          <div class="form-field"><label class="form-label" for="return-quantity">Số lượng trả <span class="required">*</span></label><input class="input" id="return-quantity" type="number" min="1" step="1" placeholder="0" disabled><small class="returns-help" id="return-remaining">Còn có thể trả: —</small></div>
          <div class="form-field"><label class="form-label" for="return-reason">Lý do trả <span class="required">*</span></label><select class="select" id="return-reason" disabled><option value="">— Chọn lý do —</option><option>Hàng lỗi / hư hỏng</option><option>Giao nhầm sản phẩm</option><option>Giao thừa số lượng</option><option>Đại lý đổi nhu cầu</option><option>Lý do khác</option></select></div>
          <div class="form-field"><label class="form-label" for="return-condition">Tình trạng hàng <span class="required">*</span></label><select class="select" id="return-condition" disabled><option value="good">Còn tốt — nhập lại lô cũ</option><option value="damaged">Hàng hỏng — kho hàng lỗi</option></select></div>
          <div class="form-field returns-lot-field" id="return-lot-wrap"><label class="form-label" for="return-lot">Lô hàng trả về <span class="required">*</span></label><select class="select" id="return-lot" disabled><option value="">Chọn dòng hàng trước</option></select><small class="returns-help">Sản phẩm quản lý lô phải nhập đúng lô xuất.</small></div>
        </div>
        <div class="returns-add-row"><p class="returns-line-info" id="return-line-info">Chọn hóa đơn để xem số lượng đã giao và đã trả.</p><button type="button" class="btn btn--secondary" id="return-add" disabled>+ Thêm dòng trả</button></div>
        <div class="returns-error" id="return-error" role="alert" hidden></div>
        <div class="returns-draft" id="return-draft-wrap" hidden><h3>Các dòng trong phiếu</h3><div id="return-draft-lines"></div><div class="returns-create-action"><button type="button" class="btn btn--primary" id="return-submit">Lập phiếu chờ kho nhận</button></div></div>
      </section>

      <section class="card returns-card" aria-labelledby="return-list-title">
        <div class="returns-card__heading"><span class="returns-icon returns-icon--list"><span class="icon icon--boxes" aria-hidden="true"></span></span><div><h2 id="return-list-title">Phiếu trả hàng</h2><p>Theo dõi phiếu chờ nhận và lịch sử xác nhận.</p></div><span class="returns-count" id="return-count">0 phiếu</span></div>
        <div class="returns-table-wrap"><table class="table returns-table"><thead><tr><th>Mã phiếu</th><th>Hóa đơn · Đại lý</th><th>Hàng trả</th><th>Ngày lập</th><th>Trạng thái</th><th></th></tr></thead><tbody id="return-rows"></tbody></table></div>
        <div class="returns-empty" id="return-empty" hidden><span class="icon icon--clipboard-list" aria-hidden="true"></span><strong>Chưa có phiếu trả hàng</strong><p>Phiếu sẽ xuất hiện ở đây sau khi được lập.</p></div>
      </section>
    </main>

    <aside class="returns-aside">
      <section class="card returns-summary"><div class="returns-card__heading"><span class="returns-icon"><span class="icon icon--info" aria-hidden="true"></span></span><div><h2>Quy trình xử lý</h2><p>Phiếu chỉ cập nhật tồn sau khi kho xác nhận.</p></div></div>
        <ol class="returns-process"><li><span>1</span><div><strong>Kinh doanh lập phiếu</strong><small>Chọn hóa đơn, hàng, số lượng và lý do.</small></div></li><li><span>2</span><div><strong>Quản lý kho nhận hàng</strong><small>Hàng tốt về lô cũ; hàng hỏng vào kho lỗi.</small></div></li><li><span>3</span><div><strong>Cập nhật tồn minh họa</strong><small>Chỉ cộng sau khi xác nhận nhận hàng.</small></div></li></ol>
      </section>
      <section class="card returns-summary returns-inventory"><div class="returns-card__heading"><span class="returns-icon returns-icon--list"><span class="icon icon--boxes" aria-hidden="true"></span></span><div><h2>Tồn kho mẫu</h2><p>Thay đổi khi quản lý xác nhận phiếu.</p></div></div><div id="return-inventory"></div></section>
    </aside>
  </div>
  <p class="returns-footnote">GHTK-76 · S8-01 · Lập phiếu trả hàng từ đại lý và nhập lại kho</p>
</section>
<script src="${ctx}/assets/js/returns.js?v=${assetVersion}" defer></script>
<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
