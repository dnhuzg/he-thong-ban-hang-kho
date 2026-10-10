<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Tồn kho theo SKU"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>
<link rel="stylesheet" href="${ctx}/assets/css/stock.css?v=${assetVersion}">

<section class="stock-screen" id="stock-screen">
  <div class="page-header stock-header">
    <div><p class="breadcrumb">Tồn kho &amp; nhập kho / Tra cứu tồn kho</p><h1>Tồn kho theo SKU và kho</h1><p class="page-header__desc">Tra cứu nhanh lượng hàng có thể bán tại từng kho.</p></div>
    <span class="stock-readonly"><span class="icon icon--sm icon--info" aria-hidden="true"></span>Chế độ xem</span>
  </div>

  <div class="alert alert--info stock-demo" role="note"><strong>Bản FE GHTK-53.</strong> Các con số bên dưới là dữ liệu minh họa, chưa đọc tồn kho thật từ MySQL. Tồn khả dụng = tồn thực tế − tồn đang giữ chỗ.</div>

  <div class="stock-overview" aria-label="Tổng quan kết quả lọc">
    <div class="card stock-stat"><span class="stock-stat__icon"><span class="icon icon--package" aria-hidden="true"></span></span><div><p>Dòng SKU · kho</p><strong id="stat-lines">0</strong><small>Trong phạm vi đang lọc</small></div></div>
    <div class="card stock-stat"><span class="stock-stat__icon stock-stat__icon--warning"><span class="icon icon--info" aria-hidden="true"></span></span><div><p>Sắp hết hàng</p><strong id="stat-low">0</strong><small>Khả dụng từ 1 đến ngưỡng cảnh báo</small></div></div>
    <div class="card stock-stat"><span class="stock-stat__icon stock-stat__icon--danger"><span class="icon icon--boxes" aria-hidden="true"></span></span><div><p>Hết hàng khả dụng</p><strong id="stat-out">0</strong><small>Khả dụng bằng 0</small></div></div>
  </div>

  <section class="card stock-panel" aria-labelledby="stock-table-title">
    <div class="stock-panel__heading"><div><h2 id="stock-table-title">Danh sách tồn kho</h2><p>Đối chiếu số lượng của từng mã hàng ở từng kho.</p></div><div class="stock-panel__actions"><button class="btn btn--primary" type="button" id="stock-export" disabled>Xuất Excel</button><span class="badge" id="stock-count">0 kết quả</span></div></div>
    <p class="stock-export-feedback" id="stock-export-feedback" role="status" aria-live="polite" hidden></p>
    <form class="stock-filters" id="stock-filters" role="search">
      <div class="form-field stock-filters__search"><label class="form-label" for="stock-search">Tìm sản phẩm</label><div class="stock-search-wrap"><span class="icon icon--search" aria-hidden="true"></span><input class="input" type="search" id="stock-search" placeholder="Nhập mã SKU hoặc tên sản phẩm" autocomplete="off"></div></div>
      <div class="form-field"><label class="form-label" for="stock-warehouse">Kho</label><select class="select" id="stock-warehouse"><option value="">Tất cả kho</option><option value="KHO-HN">Kho Hà Nội</option><option value="KHO-TN">Kho Thái Nguyên</option><option value="KHO-BN">Kho Bắc Ninh</option></select></div>
      <div class="form-field"><label class="form-label" for="stock-category">Nhóm hàng</label><select class="select" id="stock-category"><option value="">Tất cả nhóm</option><option value="Đồ uống">Đồ uống</option><option value="Thực phẩm">Thực phẩm</option><option value="Gia dụng">Gia dụng</option></select></div>
      <div class="form-field"><label class="form-label" for="stock-status">Trạng thái tồn</label><select class="select" id="stock-status"><option value="">Tất cả trạng thái</option><option value="available">Còn hàng</option><option value="low">Sắp hết</option><option value="out">Hết hàng</option></select></div>
      <button class="btn btn--ghost stock-clear" type="reset">Xóa bộ lọc</button>
    </form>

    <div class="stock-table-wrap table-wrap"><table class="table stock-table"><caption class="visually-hidden">Số tồn thực tế, đang giữ chỗ và khả dụng theo SKU và kho</caption><thead><tr><th scope="col">Sản phẩm / SKU</th><th scope="col">Kho</th><th scope="col">Nhóm hàng</th><th scope="col" class="stock-number">Tồn thực tế</th><th scope="col" class="stock-number">Đang giữ chỗ</th><th scope="col" class="stock-number">Khả dụng</th><th scope="col">Trạng thái</th></tr></thead><tbody id="stock-rows"></tbody></table></div>
    <div class="stock-mobile" id="stock-mobile"></div>
    <div class="stock-empty" id="stock-empty" hidden><span class="icon icon--boxes" aria-hidden="true"></span><h3>Không tìm thấy tồn kho</h3><p>Thử từ khóa khác hoặc xóa bộ lọc.</p><button class="btn btn--secondary" type="button" id="stock-empty-reset">Xóa bộ lọc</button></div>
    <footer class="stock-footer"><span id="stock-footer-count">Hiển thị 0 dòng</span><span>Ngưỡng sắp hết là dữ liệu mẫu theo từng sản phẩm.</span></footer>
  </section>
  <p class="stock-footnote">GHTK-53 · S5-05 · Xem số tồn kho theo từng SKU và từng kho</p>
  <noscript><div class="alert alert--danger">Bật JavaScript để xem bảng tồn kho mẫu.</div></noscript>
</section>
<script src="${ctx}/assets/js/stock-xlsx.js?v=${assetVersion}" defer></script>
<script src="${ctx}/assets/js/stock.js?v=${assetVersion}" defer></script>
<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
