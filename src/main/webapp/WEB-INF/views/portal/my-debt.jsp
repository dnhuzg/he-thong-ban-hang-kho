<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Công nợ của tôi"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

<link rel="stylesheet" href="${ctx}/assets/css/my-debt.css?v=${assetVersion}">

<main class="my-debt" id="my-debt">

  <header class="page-header">
    <div>
      <p class="breadcrumb">Cổng đại lý / Tài chính</p>
      <h1>Công nợ của tôi</h1>
      <p class="page-header__desc">
        Theo dõi hóa đơn, công nợ và hạn mức còn lại.
      </p>
    </div>
  </header>

  <div class="alert alert--info" role="note">
    <strong>GHTK-73 · Giao diện minh họa.</strong>
    Tất cả số tiền, hóa đơn và hạn mức bên dưới là dữ liệu mẫu.
    Chưa kết nối công nợ thật hoặc hệ thống phát hành hóa đơn.
  </div>

  <section class="my-debt__overview" aria-label="Tổng quan công nợ">

    <article class="card debt-stat">
      <span class="debt-stat__label">Tổng công nợ còn lại</span>
      <strong id="debt-total">—</strong>
      <small>Số tiền chưa thanh toán</small>
    </article>

    <article class="card debt-stat">
      <span class="debt-stat__label">Hạn mức công nợ</span>
      <strong id="debt-limit">—</strong>
      <small>Hạn mức được cấp</small>
    </article>

    <article class="card debt-stat debt-stat--available">
      <span class="debt-stat__label">Hạn mức còn lại</span>
      <strong id="debt-available">—</strong>
      <small>Khả năng tiếp tục lấy hàng theo hạn mức</small>
    </article>

    <article class="card debt-stat debt-stat--overdue">
      <span class="debt-stat__label">Công nợ quá hạn</span>
      <strong id="debt-overdue">—</strong>
      <small>Cần ưu tiên thanh toán</small>
    </article>

  </section>

  <section class="card debt-limit-panel" aria-label="Mức sử dụng hạn mức">
    <div class="debt-limit-panel__head">
      <div>
        <h2>Tình hình sử dụng hạn mức</h2>
        <p>Tỷ lệ công nợ hiện tại trên hạn mức được cấp.</p>
      </div>
      <strong id="debt-percent">0%</strong>
    </div>

    <div class="debt-progress"
         role="progressbar"
         aria-label="Tỷ lệ sử dụng hạn mức"
         aria-valuemin="0"
         aria-valuemax="100"
         aria-valuenow="0"
         id="debt-progress">
      <div class="debt-progress__fill" id="debt-progress-fill"></div>
    </div>

    <p class="debt-limit-panel__note" id="debt-limit-note"></p>
  </section>

  <section class="card debt-invoices">

    <header class="debt-invoices__header">
      <div>
        <h2>Danh sách hóa đơn</h2>
        <p>Theo dõi số tiền, hạn thanh toán và trạng thái từng hóa đơn.</p>
      </div>
      <span class="badge" id="invoice-count">0 hóa đơn</span>
    </header>

    <div class="debt-toolbar">
      <div class="form-field">
        <label class="form-label" for="invoice-search">Tìm hóa đơn</label>
        <input class="input" id="invoice-search" type="search"
               placeholder="Nhập mã hóa đơn...">
      </div>

      <div class="form-field">
        <label class="form-label" for="invoice-filter">Trạng thái</label>
        <select class="select" id="invoice-filter">
          <option value="all">Tất cả</option>
          <option value="unpaid">Chưa thanh toán</option>
          <option value="partial">Thanh toán một phần</option>
          <option value="overdue">Quá hạn</option>
          <option value="paid">Đã thanh toán</option>
        </select>
      </div>
    </div>

    <div class="table-wrap debt-table-wrap">
      <table class="table debt-table">
        <caption class="visually-hidden">Danh sách hóa đơn của tài khoản minh họa</caption>
        <thead>
          <tr>
            <th scope="col">Mã hóa đơn</th>
            <th scope="col">Ngày phát hành</th>
            <th scope="col">Hạn thanh toán</th>
            <th scope="col" class="num">Số tiền</th>
            <th scope="col" class="num">Còn nợ</th>
            <th scope="col">Trạng thái</th>
            <th scope="col">Thao tác</th>
          </tr>
        </thead>
        <tbody id="invoice-rows"></tbody>
      </table>
    </div>

    <div class="debt-mobile" id="invoice-mobile"></div>

    <p class="debt-empty" id="invoice-empty" hidden>
      Không tìm thấy hóa đơn phù hợp.
    </p>

  </section>

  <dialog class="debt-dialog" id="invoice-dialog"
          aria-labelledby="invoice-dialog-title">
    <div class="debt-dialog__header">
      <div>
        <p class="breadcrumb">Bản xem trước · Dữ liệu mẫu</p>
        <h2 id="invoice-dialog-title">Chi tiết hóa đơn</h2>
      </div>
      <button class="btn btn--secondary" type="button"
              id="invoice-close" aria-label="Đóng">Đóng</button>
    </div>

    <div id="invoice-preview"></div>

    <div class="debt-dialog__footer">
      <button class="btn btn--secondary" type="button"
              id="invoice-download">Tải hóa đơn mẫu (HTML)</button>
      <button class="btn btn--primary" type="button"
              id="invoice-done">Hoàn tất</button>
    </div>
  </dialog>

  <p class="my-debt__footer">
    GHTK-73 · S7-07 · Xem công nợ của chính mình · Bản frontend demo
  </p>

  <noscript>
    <div class="alert alert--danger">
      Cần bật JavaScript để xem dữ liệu công nợ minh họa.
    </div>
  </noscript>

</main>

<script src="${ctx}/assets/js/my-debt.js?v=${assetVersion}" defer></script>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>