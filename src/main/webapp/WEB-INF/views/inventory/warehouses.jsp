<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Kho & vị trí lưu trữ"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>
<link rel="stylesheet" href="${ctx}/assets/css/warehouses.css?v=${assetVersion}">
<section id="warehouse-screen" data-can-write="<c:out value='${canWrite}'/>" data-user-id="<c:out value='${currentUser.id}'/>">

      <div class="wh-demo"><span><strong>Bản FE GHTK-51.</strong> Kho, vị trí và đại lý bên dưới là dữ liệu mẫu, chưa kết nối MySQL.</span><button type="button" class="wh-link" id="reset" hidden>Khôi phục mẫu</button></div>
      <div class="page-header">
        <div><p class="breadcrumb" id="breadcrumb">Kho & tồn kho / Danh sách kho</p><h1 id="page-title">Danh sách kho</h1><p class="wh-description" id="page-description">Khai báo thông tin và quản lý trạng thái các kho.</p></div>
        <button class="btn btn--primary" type="button" id="add" hidden>+ Thêm kho</button>
      </div>
      <div class="wh-stats" id="stats" aria-label="Tổng quan dữ liệu mẫu"></div>
      <nav class="wh-tabs" aria-label="Các phần của nhiệm vụ">
        <a href="#warehouses" data-page="warehouses">Danh sách kho</a>
        <a href="#locations" data-page="locations">Vị trí lưu trữ</a>
        <a href="#dealers" data-page="dealers">Kho phục vụ đại lý</a>
      </nav>
      <section class="card wh-panel" aria-labelledby="table-title">
        <div class="card__header wh-card-heading"><div><h2 id="table-title">Thông tin kho</h2><p class="wh-description" id="table-description">Mỗi kho có mã định danh riêng.</p></div><span class="badge" id="result-count"></span></div>
        <div class="toolbar wh-toolbar">
          <label class="wh-search"><span class="visually-hidden" id="search-label">Tìm kiếm</span><input class="input" type="search" id="search" aria-labelledby="search-label" placeholder="Tìm mã kho, tên kho hoặc địa chỉ…"></label>
          <label class="wh-filter" id="warehouse-filter-wrap" hidden><span>Kho</span><select class="select" id="warehouse-filter"></select></label>
          <label class="wh-filter"><span id="filter-label">Trạng thái</span><select class="select" id="filter" aria-labelledby="filter-label"></select></label>
        </div>
        <div id="warehouse-summary" class="wh-context" hidden></div>
        <div class="table-wrap"><table class="table wh-table"><caption class="visually-hidden" id="table-caption">Danh sách kho</caption><thead id="thead"></thead><tbody id="tbody"></tbody></table></div>
        <div class="wh-empty" id="empty" hidden><strong>Không tìm thấy kết quả</strong><p>Thử thay đổi từ khóa hoặc bộ lọc.</p><button class="btn btn--secondary" type="button" id="clear-filter">Xóa bộ lọc</button></div>
        <footer class="wh-table-footer"><span id="row-count"></span><span id="table-note">Chọn “Vị trí” để khai báo khu và kệ của kho.</span></footer>
      </section>
      <p class="wh-footnote">GHTK-51 · S5-03 · Khai báo nhiều kho và vị trí lưu trong kho</p>
      <noscript><p class="alert alert--danger">Bật JavaScript để xem và thao tác bản giao diện này.</p></noscript>

<div id="notice" class="wh-toast" role="status" aria-live="polite" hidden></div>
<dialog class="dialog wh-dialog" id="editor" aria-labelledby="editor-title">
  <form id="editor-form">
    <header class="dialog__header wh-dialog-heading"><div><p class="wh-eyebrow" id="editor-eyebrow">THÔNG TIN KHO</p><h2 id="editor-title">Thêm kho</h2></div><button class="wh-close" type="button" data-close aria-label="Đóng hộp thoại">×</button></header>
    <div class="dialog__body"><p class="wh-required">Các trường có dấu * là bắt buộc.</p><div class="wh-form-grid" id="fields"></div><p class="alert alert--danger wh-form-error" id="form-error" role="alert" hidden></p></div>
    <footer class="dialog__footer"><button class="btn btn--secondary" type="button" data-close>Hủy</button><button class="btn btn--primary" type="submit" id="save">Lưu kho</button></footer>
  </form>
</dialog>
<dialog class="dialog" id="reset-dialog" aria-labelledby="reset-title"><form method="dialog"><header class="dialog__header"><h2 id="reset-title">Khôi phục dữ liệu mẫu?</h2></header><div class="dialog__body">Các thay đổi thử nghiệm của nhiệm vụ này trên trình duyệt sẽ được thay bằng dữ liệu ban đầu.</div><footer class="dialog__footer"><button class="btn btn--secondary" value="cancel">Hủy</button><button class="btn btn--primary" value="confirm">Khôi phục</button></footer></form></dialog>

</section>
<script src="${ctx}/assets/js/warehouse-demo.js?v=${assetVersion}" defer></script>
<script src="${ctx}/assets/js/warehouses.js?v=${assetVersion}" defer></script>
<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
