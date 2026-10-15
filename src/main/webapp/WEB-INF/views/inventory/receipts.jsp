<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Lập phiếu nhập kho"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>
<link rel="stylesheet" href="${ctx}/assets/css/receipts.css?v=${assetVersion}">

<div class="page-header receipt-header">
  <div>
    <p class="breadcrumb">Tồn kho &amp; nhập kho / Phiếu nhập kho</p>
    <h1>Lập phiếu nhập kho</h1>
    <p class="page-header__desc">Ghi nhận hàng từ nhà cung cấp theo chứng từ, số lượng và số lô.</p>
  </div>
  <a class="btn btn--secondary" href="${ctx}/suppliers">Xem nhà cung cấp</a>
</div>


<div class="receipt-layout" id="receipt-screen" data-can-write="<c:out value='${canWrite}'/>" data-csrf="<c:out value='${csrfToken}'/>" data-save-url="<c:out value='${ctx}/inventory/receipts/save'/>" data-page-url="<c:out value='${ctx}/inventory/receipts'/>">
  <div class="receipt-main">
    <div class="receipt-step"><span class="receipt-step__number">01</span><span>Thông tin phiếu</span><span class="receipt-step__line"></span><span class="receipt-step__number">02</span><span>Hàng nhập</span></div>
    <form id="receipt-form" novalidate>
      <section class="card receipt-card" aria-labelledby="receipt-info-title">
        <div class="receipt-card__heading"><div class="receipt-card__icon"><span class="icon icon--clipboard-list" aria-hidden="true"></span></div><div><h2 id="receipt-info-title">Thông tin chung</h2><p>Chọn nhà cung cấp, chứng từ và kho nhận hàng.</p></div><span class="badge" id="receipt-status">Phiếu mới</span></div>
        <div class="receipt-fields">
          <div class="form-field"><label class="form-label" for="supplier">Nhà cung cấp <span class="required">*</span></label><select class="select" id="supplier" required><option value="">— Chọn nhà cung cấp —</option></select></div>
          <div class="form-field"><label class="form-label" for="document">Số chứng từ <span class="required">*</span></label><input class="input" id="document" type="text" maxlength="50" placeholder="VD: HD-2026-0815" required></div>
          <div class="form-field"><label class="form-label" for="received-date">Ngày nhập <span class="required">*</span></label><input class="input" id="received-date" type="date" required></div>
          <div class="form-field"><label class="form-label" for="warehouse">Kho nhập <span class="required">*</span></label><select class="select" id="warehouse" required><option value="">— Chọn kho nhập —</option></select></div>
          <div class="form-field receipt-fields__full"><label class="form-label" for="note">Ghi chú</label><textarea class="textarea" id="note" maxlength="500" rows="2" placeholder="Ghi chú khi nhận hàng (nếu có)"></textarea></div>
        </div>
      </section>

      <section class="card receipt-card" aria-labelledby="receipt-lines-title">
        <div class="receipt-card__heading"><div class="receipt-card__icon"><span class="icon icon--package" aria-hidden="true"></span></div><div><h2 id="receipt-lines-title">Chi tiết hàng nhập</h2><p>Nhập đúng đơn vị trên chứng từ; hệ thống hiển thị số lượng quy về đơn vị cơ sở.</p></div></div>
        <div class="receipt-lines" id="receipt-lines"></div>
        <button class="btn btn--secondary receipt-add" id="add-line" type="button">+ Thêm mặt hàng</button>
      </section>

      <p class="alert alert--danger receipt-error" id="receipt-error" role="alert" hidden></p>
      <div class="receipt-actions" id="receipt-actions"><button class="btn btn--secondary" type="button" id="reset-form">Làm mới</button><button class="btn btn--secondary" type="button" id="save-draft">Lưu nháp</button><button class="btn btn--primary" type="submit">Xác nhận phiếu</button></div>
    </form>
  </div>

  <aside class="receipt-aside" aria-label="Tổng quan phiếu nhập">
    <section class="card receipt-summary"><h2>Tóm tắt phiếu nhập</h2><dl><div><dt>Trạng thái</dt><dd id="summary-status">Đang lập</dd></div><div><dt>Nhà cung cấp</dt><dd id="summary-supplier">Chưa chọn</dd></div><div><dt>Kho nhận</dt><dd id="summary-warehouse">Chưa chọn</dd></div><div><dt>Số mặt hàng</dt><dd id="summary-items">0</dd></div><div class="receipt-summary__total"><dt>Dòng đã quy đổi</dt><dd id="summary-units">0</dd></div></dl><p>Mỗi sản phẩm quy về đơn vị cơ sở riêng; xem kết quả ở từng dòng.</p></section>
    <section class="card receipt-help"><h2>Quy tắc nhập hàng</h2><ul><li>Ghi số lô và hạn dùng cho sản phẩm quản lý lô; hạn dùng không được trước ngày nhập.</li><li>Quy đổi theo hệ số của đơn vị đã chọn.</li><li>Phiếu đã xác nhận chỉ xem, cần phiếu điều chỉnh để sửa số liệu.</li></ul></section>
    <section class="card receipt-history"><h2>Phiếu nhập gần đây</h2><div id="receipt-history"><p class="text-muted">Chưa có phiếu nào.</p></div></section>
  </aside>
</div>

<template id="receipt-line-template">
  <fieldset class="receipt-line">
    <legend class="receipt-line__title">Mặt hàng <span data-line-number></span></legend>
    <button type="button" class="receipt-line__remove" data-remove aria-label="Xóa mặt hàng">Xóa</button>
    <div class="receipt-line__grid">
      <div class="form-field receipt-line__product"><label class="form-label">Sản phẩm <span class="required">*</span><select class="select" data-field="product" required><option value="">— Chọn sản phẩm —</option></select></label></div>
      <div class="form-field"><label class="form-label">Số lượng <span class="required">*</span><input class="input" data-field="quantity" type="number" min="0.001" step="any" inputmode="decimal" placeholder="0" required></label></div>
      <div class="form-field"><label class="form-label">Đơn vị nhập <span class="required">*</span><select class="select" data-field="unit" required><option value="">Chọn sản phẩm trước</option></select></label></div>
      <div class="form-field"><label class="form-label">Số lô<input class="input" data-field="lot" type="text" maxlength="50" placeholder="VD: LO-1026"></label></div>
      <div class="form-field"><label class="form-label">Ngày sản xuất<input class="input" data-field="manufactured" type="date"></label></div>
      <div class="form-field"><label class="form-label">Hạn sử dụng<input class="input" data-field="expiry" type="date"></label></div>
      <p class="receipt-line__converted" data-converted>Quy đổi: —</p>
    </div>
  </fieldset>
</template>

<dialog class="dialog receipt-dialog" id="confirm-dialog" aria-labelledby="confirm-title"><form method="dialog"><div class="dialog__header"><h2 id="confirm-title">Xác nhận phiếu nhập?</h2></div><div class="dialog__body"><p>Tồn kho sẽ được cộng ngay theo số lượng quy về đơn vị cơ sở. <strong>Phiếu đã xác nhận không sửa được</strong>; muốn sửa số liệu cần lập phiếu điều chỉnh.</p></div><div class="dialog__footer"><button class="btn btn--secondary" value="cancel">Kiểm tra lại</button><button class="btn btn--primary" value="confirm">Xác nhận</button></div></form></dialog>
<div class="receipt-toast" id="receipt-toast" role="status" aria-live="polite" hidden></div>
<noscript><div class="alert alert--danger">Bật JavaScript để lập phiếu nhập kho.</div></noscript>
<script type="application/json" id="receipt-data">${receiptData}</script>
<script src="${ctx}/assets/js/receipts.js?v=${assetVersion}" defer></script>
<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
