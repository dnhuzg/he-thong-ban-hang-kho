<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Theo dõi giao hàng"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>
<link rel="stylesheet" href="${ctx}/assets/css/order-delivery-status.css?v=${assetVersion}">

<section class="order-track" id="order-track">
  <div class="page-header order-track__header">
    <div>
      <p class="breadcrumb">Đơn hàng / Theo dõi giao hàng</p>
      <h1>Theo dõi trạng thái giao hàng</h1>
      <p class="page-header__desc">Theo dõi tiến độ đơn để chủ động bố trí người nhận hàng.</p>
    </div>
    <span class="track-live">● Đang cập nhật</span>
  </div>

  <div class="alert alert--info" role="note">
    <strong>Bản FE GHTK-66.</strong>
    Trạng thái, thời gian và số lượng thực giao bên dưới là dữ liệu minh họa,
    chờ backend giao hàng cung cấp dữ liệu thật.
  </div>

  <section class="card order-picker">
    <div>
      <label class="form-label" for="order-select">Đơn hàng</label>
      <select class="select" id="order-select"></select>
    </div>
    <div class="order-picker__summary">
      <span>Đại lý</span>
      <strong id="customer-name">—</strong>
    </div>
    <div class="order-picker__summary">
      <span>Ngày giao dự kiến</span>
      <strong id="delivery-date">—</strong>
    </div>
    <div class="order-picker__summary">
      <span>Trạng thái hiện tại</span>
      <strong id="current-status">—</strong>
    </div>
  </section>

  <section class="card tracking-panel">
    <div class="tracking-panel__heading">
      <div>
        <p class="breadcrumb">Tiến độ đơn hàng</p>
        <h2 id="tracking-code">—</h2>
      </div>
      <span class="status-pill" id="tracking-status">—</span>
    </div>

    <div class="timeline" id="delivery-timeline" aria-label="Các mốc trạng thái giao hàng"></div>

    <div class="arrival-note" id="arrival-note" role="status" aria-live="polite"></div>
  </section>

  <section class="card delivered-panel">
    <div class="delivered-panel__heading">
      <div>
        <h2>Số lượng thực giao</h2>
        <p>Đối chiếu số lượng đặt và số lượng đã giao của từng dòng hàng.</p>
      </div>
      <span class="badge" id="line-count">0 dòng</span>
    </div>

    <div class="table-wrap delivered-table-wrap">
      <table class="table delivered-table">
        <caption class="visually-hidden">Số lượng đặt và thực giao từng dòng hàng</caption>
        <thead>
          <tr>
            <th scope="col">Sản phẩm / SKU</th>
            <th scope="col" class="num">SL đặt</th>
            <th scope="col" class="num">SL thực giao</th>
            <th scope="col">Kết quả</th>
          </tr>
        </thead>
        <tbody id="delivery-lines"></tbody>
      </table>
    </div>

    <div class="delivered-mobile" id="delivery-lines-mobile"></div>

    <footer class="delivered-summary">
      <span id="delivery-summary">—</span>
    </footer>
  </section>

  <p class="order-track__footnote">GHTK-66 · S6-09 · Theo dõi trạng thái giao hàng của đơn</p>
  <noscript><div class="alert alert--danger">Bật JavaScript để xem dữ liệu theo dõi minh họa.</div></noscript>
</section>

<script src="${ctx}/assets/js/order-delivery-status.js?v=${assetVersion}" defer></script>
<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>