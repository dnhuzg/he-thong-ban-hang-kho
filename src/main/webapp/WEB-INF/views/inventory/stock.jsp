<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Cảnh báo tồn kho"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>
<link rel="stylesheet" href="${ctx}/assets/css/stock.css?v=${assetVersion}">

<section class="stock-screen" id="stock-screen">
  <div class="page-header stock-header">
    <div>
      <p class="breadcrumb">Tồn kho &amp; nhập kho / Cảnh báo tồn kho</p>
      <h1>Tồn kho theo SKU và kho</h1>
      <p class="page-header__desc">
        Theo dõi tồn khả dụng và cảnh báo khi hàng xuống dưới mức tồn tối thiểu.
      </p>
    </div>
    <span class="stock-readonly">
      <span class="icon icon--sm icon--info" aria-hidden="true"></span>
      GHTK-57
    </span>
  </div>

  <div class="alert alert--info stock-demo" role="note">
    <strong>Bản FE GHTK-57.</strong>
    Dữ liệu tồn và mức tối thiểu hiện là dữ liệu minh họa, chờ backend cung cấp dữ liệu theo SKU và kho.
    Tồn khả dụng = tồn thực tế − tồn đang giữ chỗ.
  </div>

  <section class="stock-dashboard" aria-labelledby="stock-dashboard-title">
    <div class="stock-dashboard__heading">
      <div>
        <p class="breadcrumb">Dashboard kho</p>
        <h2 id="stock-dashboard-title">Cảnh báo tồn kho</h2>
      </div>
      <span class="badge badge--warning" id="stock-alert-count">0 cảnh báo</span>
    </div>

    <div class="stock-overview" aria-label="Tổng quan cảnh báo tồn kho">
      <div class="card stock-stat">
        <span class="stock-stat__icon">
          <span class="icon icon--package" aria-hidden="true"></span>
        </span>
        <div>
          <p>Dòng SKU · kho</p>
          <strong id="stat-lines">0</strong>
          <small>Trong phạm vi đang lọc</small>
        </div>
      </div>

      <div class="card stock-stat stock-stat--warning">
        <span class="stock-stat__icon stock-stat__icon--warning">
          <span class="icon icon--info" aria-hidden="true"></span>
        </span>
        <div>
          <p>Dưới mức tối thiểu</p>
          <strong id="stat-low">0</strong>
          <small>Cần chủ động bổ sung hàng</small>
        </div>
      </div>

      <div class="card stock-stat stock-stat--danger">
        <span class="stock-stat__icon stock-stat__icon--danger">
          <span class="icon icon--boxes" aria-hidden="true"></span>
        </span>
        <div>
          <p>Hết hàng khả dụng</p>
          <strong id="stat-out">0</strong>
          <small>Tồn khả dụng bằng 0</small>
        </div>
      </div>
    </div>

    <div class="stock-alert-summary" id="stock-alert-summary" role="status" aria-live="polite"></div>
  </section>

  <section class="card stock-panel" aria-labelledby="stock-table-title">
    <div class="stock-panel__heading">
      <div>
        <h2 id="stock-table-title">Sổ tồn theo SKU và kho</h2>
        <p>Khai báo mức tồn tối thiểu và theo dõi các mặt hàng cần bổ sung.</p>
      </div>
      <div class="stock-panel__actions">
        <button class="btn btn--primary" type="button" id="stock-export" disabled>Xuất Excel</button>
        <span class="badge" id="stock-count">0 kết quả</span>
      </div>
    </div>
    <p class="stock-export-feedback" id="stock-export-feedback" role="status" aria-live="polite" hidden></p>

    <form class="stock-filters" id="stock-filters" role="search">
      <div class="form-field stock-filters__search">
        <label class="form-label" for="stock-search">Tìm sản phẩm</label>
        <div class="stock-search-wrap">
          <span class="icon icon--search" aria-hidden="true"></span>
          <input
            class="input"
            type="search"
            id="stock-search"
            placeholder="Nhập mã SKU hoặc tên sản phẩm"
            autocomplete="off">
        </div>
      </div>

      <div class="form-field">
        <label class="form-label" for="stock-warehouse">Kho</label>
        <select class="select" id="stock-warehouse">
          <option value="">Tất cả kho</option>
          <option value="KHO-HN">Kho Hà Nội</option>
          <option value="KHO-TN">Kho Thái Nguyên</option>
          <option value="KHO-BN">Kho Bắc Ninh</option>
        </select>
      </div>

      <div class="form-field">
        <label class="form-label" for="stock-category">Nhóm hàng</label>
        <select class="select" id="stock-category">
          <option value="">Tất cả nhóm</option>
          <option value="Đồ uống">Đồ uống</option>
          <option value="Thực phẩm">Thực phẩm</option>
          <option value="Gia dụng">Gia dụng</option>
        </select>
      </div>

      <div class="form-field">
        <label class="form-label" for="stock-status">Trạng thái tồn</label>
        <select class="select" id="stock-status">
          <option value="">Tất cả trạng thái</option>
          <option value="available">Đủ tồn</option>
          <option value="low">Dưới mức tối thiểu</option>
          <option value="out">Hết hàng</option>
        </select>
      </div>

      <button class="btn btn--ghost stock-clear" type="reset">Xóa bộ lọc</button>
    </form>

    <div class="stock-table-wrap table-wrap">
      <table class="table stock-table">
        <caption class="visually-hidden">
          Sổ tồn và mức tồn tối thiểu theo từng SKU và từng kho
        </caption>
        <thead>
          <tr>
            <th scope="col">Sản phẩm / SKU</th>
            <th scope="col">Kho</th>
            <th scope="col">Nhóm hàng</th>
            <th scope="col" class="stock-number">Tồn thực tế</th>
            <th scope="col" class="stock-number">Đang giữ chỗ</th>
            <th scope="col" class="stock-number">Khả dụng</th>
            <th scope="col" class="stock-number">Tồn tối thiểu</th>
            <th scope="col">Trạng thái</th>
            <th scope="col">Thao tác</th>
          </tr>
        </thead>
        <tbody id="stock-rows"></tbody>
      </table>
    </div>

    <div class="stock-mobile" id="stock-mobile"></div>

    <div class="stock-empty" id="stock-empty" hidden>
      <span class="icon icon--boxes" aria-hidden="true"></span>
      <h3>Không tìm thấy tồn kho</h3>
      <p>Thử từ khóa khác hoặc xóa bộ lọc.</p>
      <button class="btn btn--secondary" type="button" id="stock-empty-reset">Xóa bộ lọc</button>
    </div>

    <footer class="stock-footer">
      <span id="stock-footer-count">Hiển thị 0 dòng</span>
      <span>Dòng dưới mức tối thiểu sẽ được đánh dấu để Quản lý kho xử lý sớm.</span>
    </footer>
  </section>

  <div class="stock-modal" id="minimum-modal" hidden>
    <div class="stock-modal__backdrop" data-close-minimum></div>

    <section
      class="card stock-modal__dialog"
      role="dialog"
      aria-modal="true"
      aria-labelledby="minimum-modal-title">

      <div class="stock-modal__header">
        <div>
          <p class="breadcrumb">Thiết lập cảnh báo</p>
          <h2 id="minimum-modal-title">Khai báo tồn tối thiểu</h2>
        </div>
        <button
          class="btn btn--ghost stock-modal__close"
          type="button"
          data-close-minimum
          aria-label="Đóng">
          ×
        </button>
      </div>

      <form id="minimum-form">
        <div class="stock-modal__body">
          <div class="stock-minimum-context">
            <strong id="minimum-product"></strong>
            <span id="minimum-warehouse"></span>
          </div>

          <div class="form-field">
            <label class="form-label" for="minimum-value">Mức tồn tối thiểu</label>
            <input
              class="input"
              id="minimum-value"
              name="minimum"
              type="number"
              min="0"
              step="1"
              required>
            <p class="stock-field-help">
              Ngưỡng được khai báo riêng cho từng SKU tại từng kho.
            </p>
          </div>

          <div class="alert alert--info">
            Bản frontend hiện chỉ cập nhật dữ liệu minh họa trong phiên làm việc.
            Chưa lưu xuống MySQL cho đến khi backend GHTK-57 được cung cấp.
          </div>
        </div>

        <div class="stock-modal__actions">
          <button class="btn btn--ghost" type="button" data-close-minimum>Hủy</button>
          <button class="btn btn--primary" type="submit">Lưu mức tối thiểu</button>
        </div>
      </form>
    </section>
  </div>

  <p class="stock-footnote">
    GHTK-57 · S5-09 · Cảnh báo khi tồn xuống dưới mức tối thiểu
  </p>

  <noscript>
    <div class="alert alert--danger">
      Bật JavaScript để xem và cấu hình cảnh báo tồn kho mẫu.
    </div>
  </noscript>
</section>

<script src="${ctx}/assets/js/stock-xlsx.js?v=${assetVersion}" defer></script>
<script src="${ctx}/assets/js/stock.js?v=${assetVersion}" defer></script>
<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>