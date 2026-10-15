<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Thu tiền theo tuyến"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>
<link rel="stylesheet" href="${ctx}/assets/css/route-collections.css?v=${assetVersion}">

<section class="route-screen" id="route-screen" data-can-confirm="${canConfirmRouteCollection}" data-can-record="${canRecordRouteCollection}" data-employee="<c:out value='${currentUser.fullName}'/>" data-csrf="<c:out value='${csrfToken}'/>" data-record-url="<c:out value='${ctx}/finance/route-collections/record'/>" data-confirm-url="<c:out value='${ctx}/finance/route-collections/confirm'/>" data-page-url="<c:out value='${ctx}/finance/route-collections'/>" data-file-prefix="<c:out value='${ctx}/files/'/>">
  <header class="page-header route-header"><div><p class="breadcrumb">Hoá đơn &amp; công nợ / Thu tiền theo tuyến</p><h1>Thu tiền và nộp về công ty</h1><p class="page-header__desc">GHTK-71 · S7-05 · Ghi khoản thu, theo dõi chờ nộp và đối chiếu theo ngày.</p></div></header>

  <div class="route-stats" aria-label="Tổng hợp khoản thu">
    <div class="card route-stat"><span>Đã thu</span><strong id="route-total">0 ₫</strong><small>Tất cả khoản thu theo tuyến</small></div>
    <div class="card route-stat"><span>Chờ nộp</span><strong id="route-pending">0 ₫</strong><small>Chưa được kế toán xác nhận</small></div>
    <div class="card route-stat"><span>Đã nộp</span><strong id="route-confirmed">0 ₫</strong><small>Đã xác nhận nhận tiền</small></div>
  </div>

  <c:if test="${canRecordRouteCollection}">
  <section class="card route-card" aria-labelledby="route-form-title">
    <div class="route-card__head"><div><h2 id="route-form-title">Ghi nhận khoản thu tại cửa hàng</h2><p>Nhập thông tin và chọn ảnh biên lai. Sau khi ghi, khoản thu ở trạng thái Chờ nộp.</p></div></div>
    <form id="route-form" class="route-form">
      <div class="form-field"><label class="form-label" for="route-agency">Đại lý / cửa hàng</label><select class="select" id="route-agency" required><option value="">Chọn đại lý còn công nợ</option></select></div>
      <div class="form-field"><label class="form-label" for="route-date">Ngày thu</label><input class="input" type="date" id="route-date" required></div>
      <div class="form-field"><label class="form-label" for="route-amount">Số tiền thu (VND)</label><input class="input" type="number" id="route-amount" min="1" step="1" required placeholder="Ví dụ: 2500000"></div>
      <div class="form-field"><label class="form-label" for="route-receipt">Ảnh biên lai</label><input class="input" type="file" id="route-receipt" accept="image/png,image/jpeg,image/webp" required><small>PNG, JPG hoặc WebP · tối đa 2 MB</small></div>
      <div class="route-form__action"><button class="btn btn--primary" type="submit">Ghi khoản thu</button></div>
    </form>
    <p class="route-feedback" id="route-feedback" role="status" aria-live="polite"></p>
  </section>
  </c:if>

  <section class="card route-card" aria-labelledby="route-list-title">
    <div class="route-card__head"><div><h2 id="route-list-title">Khoản thu theo tuyến</h2><p>Chờ nộp cho tới khi kế toán xác nhận đã nhận tiền.</p></div><span class="badge" id="route-count">0 khoản</span></div>
    <div class="route-filters"><div class="form-field"><label class="form-label" for="route-filter-day">Ngày thu</label><input class="input" type="date" id="route-filter-day"></div><div class="form-field"><label class="form-label" for="route-filter-rep">Nhân viên</label><select class="select" id="route-filter-rep"><option value="">Tất cả</option></select></div><div class="form-field"><label class="form-label" for="route-filter-status">Trạng thái</label><select class="select" id="route-filter-status"><option value="">Tất cả</option><option value="pending">Chờ nộp</option><option value="confirmed">Đã nộp</option></select></div><button class="btn btn--ghost" type="button" id="route-clear">Xoá lọc</button></div>
    <div class="table-wrap"><table class="table route-table"><caption class="visually-hidden">Khoản thu và biên lai theo tuyến</caption><thead><tr><th scope="col">Ngày / nhân viên</th><th scope="col">Đại lý</th><th scope="col">Số tiền</th><th scope="col">Biên lai</th><th scope="col">Trạng thái</th><th scope="col">Thao tác</th></tr></thead><tbody id="route-rows"></tbody></table></div>
    <p class="route-empty" id="route-empty" hidden>Không có khoản thu phù hợp bộ lọc.</p>
  </section>

  <section class="card route-card" aria-labelledby="route-reconcile-title"><div class="route-card__head"><div><h2 id="route-reconcile-title">Đối chiếu theo nhân viên và ngày</h2><p>So sánh số tiền đã thu, đã nộp và còn chờ theo từng nhân viên mỗi ngày.</p></div></div><div class="table-wrap"><table class="table route-table"><caption class="visually-hidden">Bảng đối chiếu tiền thu và tiền nộp</caption><thead><tr><th scope="col">Ngày thu</th><th scope="col">Nhân viên</th><th scope="col">Số khoản</th><th scope="col">Đã thu</th><th scope="col">Đã nộp</th><th scope="col">Chờ nộp</th></tr></thead><tbody id="route-reconcile"></tbody></table></div><p class="route-empty" id="route-reconcile-empty" hidden>Chưa có khoản thu để đối chiếu.</p></section>
  <noscript><div class="alert alert--danger">Bật JavaScript để xem thu tiền theo tuyến.</div></noscript>
</section>
<script type="application/json" id="route-data"><c:out value="${routeData}" escapeXml="false"/></script>
<script src="${ctx}/assets/js/route-collections.js?v=${assetVersion}" defer></script>
<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
