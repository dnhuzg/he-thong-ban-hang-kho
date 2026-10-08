<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Chuyển kho nội bộ"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>
<link rel="stylesheet" href="${ctx}/assets/css/transfers.css?v=${assetVersion}">

<div class="page-header transfer-header">
  <div>
    <p class="breadcrumb">Tồn kho &amp; nhập kho / Chuyển kho nội bộ</p>
    <h1>Lập phiếu chuyển kho nội bộ</h1>
    <p class="page-header__desc">Theo dõi hàng từ kho đi đến lúc kho nhận xác nhận số lượng.</p>
  </div>
  <a class="btn btn--secondary" href="${ctx}/inventory/stock">Xem tồn kho theo SKU</a>
</div>

<div class="alert alert--info transfer-demo" role="note"><strong>Bản FE GHTK-55.</strong> Kho, hàng hóa và phiếu bên dưới là dữ liệu mẫu trên trình duyệt. Các thao tác không trừ tồn kho đi hoặc cộng tồn kho đến trong hệ thống thật.</div>

<div class="transfer-layout" id="transfer-screen" data-can-write="<c:out value='${canWrite}'/>" data-user-id="<c:out value='${currentUser.id}'/>">
  <div class="transfer-main">
    <section class="card transfer-card" aria-labelledby="transfer-flow-title">
      <h2 id="transfer-flow-title">Tiến trình chuyển kho</h2>
      <ol class="transfer-progress" id="transfer-progress"><li>Đang lập</li><li>Đang chuyển</li><li>Kho đến xác nhận</li></ol>
    </section>

    <form id="transfer-form" novalidate>
      <section class="card transfer-card" aria-labelledby="transfer-info-title">
        <div class="transfer-card__heading"><span class="transfer-card__icon"><span class="icon icon--truck" aria-hidden="true"></span></span><div><h2 id="transfer-info-title">Thông tin điều chuyển</h2><p>Chọn hai kho khác nhau và ngày dự kiến chuyển.</p></div><span class="badge" id="transfer-status">Phiếu mới</span></div>
        <div class="transfer-fields">
          <div class="form-field"><label class="form-label" for="transfer-code">Mã phiếu</label><input class="input" id="transfer-code" type="text" value="Tạo khi lưu" readonly></div>
          <div class="form-field"><label class="form-label" for="transfer-date">Ngày chuyển <span class="required">*</span></label><input class="input" id="transfer-date" type="date" required></div>
          <div class="form-field"><label class="form-label" for="source-warehouse">Kho đi <span class="required">*</span></label><select class="select" id="source-warehouse" required><option value="">— Chọn kho đi —</option><option value="KHO-HN">KHO-HN · Kho Hà Nội</option><option value="KHO-TN">KHO-TN · Kho Thái Nguyên</option><option value="KHO-BN">KHO-BN · Kho Bắc Ninh</option></select></div>
          <div class="form-field"><label class="form-label" for="destination-warehouse">Kho đến <span class="required">*</span></label><select class="select" id="destination-warehouse" required><option value="">— Chọn kho đến —</option><option value="KHO-HN">KHO-HN · Kho Hà Nội</option><option value="KHO-TN">KHO-TN · Kho Thái Nguyên</option><option value="KHO-BN">KHO-BN · Kho Bắc Ninh</option></select></div>
          <div class="form-field transfer-fields__full"><label class="form-label" for="transfer-note">Ghi chú</label><textarea class="textarea" id="transfer-note" maxlength="500" rows="2" placeholder="Thông tin thêm cho kho nhận (nếu có)"></textarea></div>
        </div>
      </section>

      <section class="card transfer-card" aria-labelledby="transfer-lines-title">
        <div class="transfer-card__heading"><span class="transfer-card__icon"><span class="icon icon--package" aria-hidden="true"></span></span><div><h2 id="transfer-lines-title">Danh sách hàng chuyển</h2><p>Số lượng chuyển theo đơn vị cơ sở của từng SKU.</p></div></div>
        <div id="transfer-lines" class="transfer-lines"></div>
        <button class="btn btn--secondary transfer-add" id="transfer-add-line" type="button">+ Thêm mặt hàng</button>
      </section>

      <section class="card transfer-card transfer-receiving" id="transfer-receiving" aria-labelledby="transfer-receiving-title" hidden>
        <div class="transfer-card__heading"><span class="transfer-card__icon"><span class="icon icon--boxes" aria-hidden="true"></span></span><div><h2 id="transfer-receiving-title">Xác nhận hàng tại kho đến</h2><p>Ghi số lượng thực nhận cho từng mặt hàng; nếu lệch phải nêu lý do.</p></div></div>
        <div id="transfer-received-lines" class="transfer-received-lines"></div>
        <div class="form-field transfer-reason"><label class="form-label" for="transfer-reason">Lý do chênh lệch <span class="required" id="transfer-reason-required" hidden>*</span></label><textarea class="textarea" id="transfer-reason" maxlength="500" rows="3" placeholder="VD: Thiếu 2 chai do hư hỏng khi vận chuyển"></textarea><p class="transfer-help">Bắt buộc khi số nhận khác số chuyển.</p></div>
      </section>

      <p class="alert alert--danger transfer-error" id="transfer-error" role="alert" hidden></p>
      <div class="transfer-actions" id="transfer-actions"><button class="btn btn--secondary" id="transfer-new" type="button">Lập phiếu mới</button><button class="btn btn--secondary" id="transfer-save" type="button">Lưu nháp</button><button class="btn btn--primary" id="transfer-send" type="button">Bắt đầu chuyển</button><button class="btn btn--primary" id="transfer-receive" type="button" hidden>Xác nhận đã nhận</button></div>
    </form>
  </div>

  <aside class="transfer-aside" aria-label="Tóm tắt và các phiếu chuyển kho">
    <section class="card transfer-summary"><h2>Tóm tắt phiếu</h2><dl><div><dt>Trạng thái</dt><dd id="transfer-summary-status">Đang lập</dd></div><div><dt>Kho đi</dt><dd id="transfer-summary-source">Chưa chọn</dd></div><div><dt>Kho đến</dt><dd id="transfer-summary-destination">Chưa chọn</dd></div><div><dt>Mặt hàng</dt><dd id="transfer-summary-items">0</dd></div><div><dt>Chênh lệch</dt><dd id="transfer-summary-difference">—</dd></div></dl></section>
    <section class="card transfer-help-card"><h2>Quy tắc chuyển kho</h2><ul><li>Hàng đang chuyển được ghi nhận là đang trên đường trong bản mẫu.</li><li>Chỉ sau khi kho đến xác nhận nhận đủ, trạng thái mới hoàn tất.</li><li>Nếu nhận thiếu hoặc thừa, nhập lý do chênh lệch.</li></ul></section>
    <section class="card transfer-history"><h2>Phiếu trên trình duyệt này</h2><div id="transfer-history"><p class="text-muted">Chưa có phiếu nào.</p></div></section>
  </aside>
</div>

<template id="transfer-line-template"><fieldset class="transfer-line"><legend>Mặt hàng <span data-line-number></span></legend><button class="transfer-line__remove" type="button" data-remove aria-label="Xóa mặt hàng">Xóa</button><div class="transfer-line__fields"><div class="form-field"><label class="form-label">SKU / Sản phẩm <span class="required">*</span><select class="select" data-field="product" required><option value="">— Chọn mặt hàng —</option><option value="SP-001">SP-001 · Nước khoáng 500ml (chai)</option><option value="SP-002">SP-002 · Bánh quy bơ 200g (gói)</option><option value="SP-003">SP-003 · Giấy vệ sinh 10 cuộn (cuộn)</option></select></label></div><div class="form-field"><label class="form-label">Số lượng chuyển <span class="required">*</span><input class="input" data-field="quantity" type="number" min="1" max="1000000" step="1" inputmode="numeric" placeholder="0" required></label></div></div></fieldset></template>
<template id="transfer-received-template"><div class="transfer-received-row"><div><strong data-product></strong><span data-sent></span></div><div class="form-field"><label class="form-label">Số thực nhận <span class="required">*</span><input class="input" data-received type="number" min="0" max="1000000" step="1" inputmode="numeric" required></label></div><span data-difference class="transfer-received-row__difference"></span></div></template>

<dialog class="dialog transfer-dialog" id="transfer-dialog" aria-labelledby="transfer-dialog-title"><form method="dialog"><div class="dialog__header"><h2 id="transfer-dialog-title"></h2></div><div class="dialog__body"><p id="transfer-dialog-message"></p></div><div class="dialog__footer"><button class="btn btn--secondary" value="cancel">Quay lại</button><button class="btn btn--primary" value="confirm">Xác nhận</button></div></form></dialog>
<div class="transfer-toast" id="transfer-toast" role="status" aria-live="polite" hidden></div>
<noscript><div class="alert alert--danger">Bật JavaScript để thao tác trên bản giao diện mẫu.</div></noscript>
<script src="${ctx}/assets/js/transfers.js?v=${assetVersion}" defer></script>
<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
