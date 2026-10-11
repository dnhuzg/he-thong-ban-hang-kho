<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Theo dõi hàng đang giao"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>
<link rel="stylesheet" href="${ctx}/assets/css/delivery-tracking.css?v=${assetVersion}">

<section class="delivery-screen" id="delivery-screen">
  <div class="page-header delivery-header">
    <div>
      <p class="breadcrumb">Xuất kho &amp; Giao hàng / Theo dõi chuyến giao</p>
      <h1>Hàng đã xuất nhưng chưa giao xong</h1>
      <p class="page-header__desc">Theo dõi tiến độ, hàng còn trên đường và các chuyến đã quá thời gian dự kiến.</p>
    </div>
    <span class="delivery-live"><span aria-hidden="true">●</span> Đang theo dõi</span>
  </div>

  <div class="alert alert--info delivery-demo" role="note">
    <strong>Bản FE GHTK-64.</strong>
    Dữ liệu chuyến giao bên dưới là dữ liệu minh họa. Frontend chưa thay đổi tồn kho hoặc trạng thái giao hàng trong MySQL.
  </div>

  <div class="delivery-overview" aria-label="Tổng quan chuyến giao">
    <article class="card delivery-stat">
      <p>Chuyến đang chạy</p>
      <strong id="stat-running">0</strong>
      <small>Chưa hoàn tất toàn bộ điểm giao</small>
    </article>

    <article class="card delivery-stat delivery-stat--warning">
      <p>Chuyến quá ETA</p>
      <strong id="stat-overdue">0</strong>
      <small>Cần ưu tiên kiểm tra</small>
    </article>

    <article class="card delivery-stat">
      <p>Điểm còn phải giao</p>
      <strong id="stat-remaining">0</strong>
      <small>Vẫn thuộc trách nhiệm của chuyến</small>
    </article>

    <article class="card delivery-stat">
      <p>Giá trị hàng trên đường</p>
      <strong id="stat-value">0 ₫</strong>
      <small>Hàng đã xuất nhưng chưa giao xong</small>
    </article>
  </div>

  <section class="card delivery-panel" aria-labelledby="delivery-title">
    <div class="delivery-panel__heading">
      <div>
        <h2 id="delivery-title">Danh sách chuyến giao đang chạy</h2>
        <p>Theo dõi số điểm đã giao, còn lại và trách nhiệm hàng hóa của từng chuyến.</p>
      </div>
      <span class="badge" id="delivery-count">0 chuyến</span>
    </div>

    <form class="delivery-filters" id="delivery-filters" role="search">
      <div class="form-field delivery-filters__search">
        <label class="form-label" for="delivery-search">Tìm chuyến</label>
        <input class="input" id="delivery-search" type="search"
               placeholder="Mã chuyến, tài xế hoặc biển số..." autocomplete="off">
      </div>

      <div class="form-field">
        <label class="form-label" for="delivery-status">Trạng thái</label>
        <select class="select" id="delivery-status">
          <option value="">Tất cả chuyến đang chạy</option>
          <option value="normal">Đúng tiến độ</option>
          <option value="overdue">Quá thời gian dự kiến</option>
        </select>
      </div>

      <button class="btn btn--ghost" type="reset">Xóa bộ lọc</button>
    </form>

    <div class="delivery-alert-summary" id="delivery-alert-summary" role="status" aria-live="polite"></div>

    <div class="table-wrap delivery-table-wrap">
      <table class="table delivery-table">
        <caption class="visually-hidden">Danh sách chuyến giao hàng đang chạy</caption>
        <thead>
          <tr>
            <th scope="col">Chuyến giao</th>
            <th scope="col">Tài xế / xe</th>
            <th scope="col">Tiến độ điểm giao</th>
            <th scope="col">Hàng còn trên chuyến</th>
            <th scope="col">ETA</th>
            <th scope="col">Trạng thái</th>
            <th scope="col">Chi tiết</th>
          </tr>
        </thead>
        <tbody id="delivery-rows"></tbody>
      </table>
    </div>

    <div class="delivery-mobile" id="delivery-mobile"></div>

    <div class="delivery-empty" id="delivery-empty" hidden>
      <h3>Không tìm thấy chuyến giao</h3>
      <p>Thử từ khóa khác hoặc xóa bộ lọc.</p>
    </div>
  </section>

  <div class="delivery-modal" id="delivery-modal" hidden>
    <div class="delivery-modal__backdrop" data-close-detail></div>
    <section class="delivery-modal__dialog" role="dialog" aria-modal="true"
             aria-labelledby="delivery-detail-title">
      <div class="delivery-modal__header">
        <div>
          <p class="breadcrumb">Chi tiết trách nhiệm chuyến</p>
          <h2 id="delivery-detail-title">Chi tiết chuyến</h2>
        </div>
        <button class="delivery-modal__close" type="button" data-close-detail aria-label="Đóng">×</button>
      </div>
      <div id="delivery-detail-content"></div>
    </section>
  </div>

  <p class="delivery-footnote">GHTK-64 · S6-07 · Theo dõi hàng đã xuất nhưng chưa giao xong</p>
  <noscript><div class="alert alert--danger">Bật JavaScript để xem dữ liệu chuyến giao minh họa.</div></noscript>
</section>

<script src="${ctx}/assets/js/delivery-tracking.js?v=${assetVersion}" defer></script>
<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>