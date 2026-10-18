<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="pageTitle" value="Lô & hạn sử dụng"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>
<link rel="stylesheet" href="${ctx}/assets/css/inventory-trace.css?v=${assetVersion}">
<section class="trace-screen">
  <div class="page-header trace-header"><div><p class="breadcrumb">Tồn kho &amp; nhập kho / Quản lý lô</p><h1>Lô &amp; hạn sử dụng</h1><p class="page-header__desc">Tra cứu lô từ phiếu nhập và lượng còn lại sau xuất kho.</p></div><a class="btn btn--secondary" href="${ctx}/inventory/stock-card">Xem thẻ kho</a></div>
  <div class="trace-stats" aria-label="Tổng quan các lô đang hiển thị">
    <div class="card trace-stat"><span>Lô hiển thị</span><strong><c:out value="${lotPage.lots.size()}"/></strong><small>Trong phạm vi kho và bộ lọc</small></div>
    <div class="card trace-stat trace-stat--warning"><span>Sắp hết hạn</span><strong><c:out value="${lotPage.soon}"/></strong><small>Theo ngưỡng của SKU</small></div>
    <div class="card trace-stat trace-stat--danger"><span>Đã quá hạn</span><strong><c:out value="${lotPage.expired}"/></strong><small>Bị chặn khi xuất nếu không có ghi đè hợp lệ</small></div>
  </div>
  <section class="card trace-panel" aria-labelledby="lot-filter-title">
    <header class="trace-panel__heading"><div><h2 id="lot-filter-title">Tra cứu lô hàng</h2><p>Chọn SKU để xem cấu hình. Ngày hết hạn hôm nay vẫn còn hiệu lực.</p></div></header>
    <form class="trace-filters" method="get" action="${ctx}/inventory/lots" role="search">
      <div class="form-field"><label class="form-label" for="lot-product">Sản phẩm</label><select class="select" id="lot-product" name="product"><option value="">Tất cả SKU</option><c:forEach var="p" items="${lotPage.products}"><option value="${p.id}" ${param.product == p.id ? 'selected' : ''}><c:out value="${p.sku} · ${p.name}"/></option></c:forEach></select></div>
      <div class="form-field"><label class="form-label" for="lot-warehouse">Kho</label><select class="select" id="lot-warehouse" name="warehouse"><option value="">Tất cả kho được xem</option><c:forEach var="w" items="${lotPage.warehouses}"><option value="${w.id}" ${param.warehouse == w.id ? 'selected' : ''}><c:out value="${w.name}"/></option></c:forEach></select></div>
      <div class="form-field"><label class="form-label" for="lot-status">Tình trạng</label><select class="select" id="lot-status" name="status"><option value="">Tất cả</option><option value="valid" ${param.status == 'valid' ? 'selected' : ''}>Còn hạn</option><option value="soon" ${param.status == 'soon' ? 'selected' : ''}>Sắp hết hạn</option><option value="expired" ${param.status == 'expired' ? 'selected' : ''}>Đã quá hạn</option><option value="incomplete" ${param.status == 'incomplete' ? 'selected' : ''}>Thiếu thông tin lô</option><option value="empty" ${param.status == 'empty' ? 'selected' : ''}>Hết hàng</option></select></div>
      <button class="btn btn--primary" type="submit">Tra cứu</button>
      <div class="form-field"><label class="form-label" for="lot-search">Tìm số lô, SKU hoặc tên hàng</label><input class="input" type="search" id="lot-search" name="q" maxlength="100" value="<c:out value='${param.q}'/>"></div><a class="btn btn--secondary" href="${ctx}/inventory/lots">Xóa bộ lọc</a>
    </form>
  </section>
  <c:if test="${not empty lotPage.selected}">
    <section class="card trace-panel"><header class="trace-panel__heading"><div><h2>Cấu hình <c:out value="${lotPage.selected.sku}"/></h2><p>Áp dụng cho SKU tại mọi kho. Quản lý kho hoặc Admin có quyền ghi mới được lưu.</p></div><span class="badge"><c:out value="${lotPage.selected.tracked ? 'Đang quản lý lô' : 'Chưa quản lý lô'}"/></span></header>
      <form class="trace-filters" method="post" action="${ctx}/inventory/lots/configure">
        <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>"><input type="hidden" name="product" value="${lotPage.selected.id}">
        <div class="form-field"><label class="form-label" for="lot-warning">Cảnh báo trước (ngày)</label><input class="input" id="lot-warning" name="warningDays" type="number" min="0" max="365" step="1" required value="${lotPage.selected.warningDays}" ${lotPage.canManage ? '' : 'disabled'}></div>
        <label class="trace-check"><input type="checkbox" name="tracked" value="true" ${lotPage.selected.tracked ? 'checked' : ''} ${lotPage.canManage ? '' : 'disabled'}> Quản lý theo lô</label>
        <c:if test="${lotPage.canManage}"><button class="btn btn--primary" type="submit">Lưu cấu hình</button></c:if>
      </form><p class="trace-context">Trước khi bật, các lô cũ cần đủ thông tin và khớp sổ tồn. Chỉ tắt khi SKU đã hết tồn và giữ chỗ tại mọi kho.</p>
    </section>
  </c:if>
  <section class="card trace-panel" aria-labelledby="lots-title"><header class="trace-panel__heading"><div><h2 id="lots-title">Danh sách lô</h2><p>Số lượng theo đơn vị cơ sở; lô đã hết vẫn được giữ để truy nguyên.</p></div></header>
    <c:if test="${lotPage.truncated}"><p class="alert alert--warning trace-context">Hiển thị tối đa 200 lô. Thu hẹp theo SKU, kho hoặc số lô để tra cứu thêm.</p></c:if>
    <div class="table-wrap"><table class="table trace-table"><caption class="visually-hidden">Lô thực tế, ngày sản xuất, hạn dùng và lượng còn lại</caption><thead><tr><th scope="col">Sản phẩm / SKU</th><th scope="col">Kho / số lô</th><th scope="col">Ngày sản xuất</th><th scope="col">Hạn sử dụng</th><th scope="col" class="trace-number">Còn lại</th><th scope="col">Tình trạng</th><th scope="col">Thao tác</th></tr></thead><tbody>
      <c:forEach var="l" items="${lotPage.lots}"><tr>
        <td><strong class="trace-table__name"><c:out value="${l.productName}"/></strong><span class="trace-table__meta"><c:out value="${l.sku}"/> · <c:out value="${l.tracked ? 'Quản lý lô' : 'Chưa bật quản lý lô'}"/></span></td>
        <td><c:out value="${l.warehouseName}"/><span class="trace-table__meta"><c:out value="${empty l.number ? 'Chưa có số lô' : l.number}"/></span></td>
        <td><c:out value="${l.manufacturedText}"/></td><td><c:out value="${l.expiryText}"/></td>
        <td class="trace-number"><fmt:formatNumber value="${l.remaining}" maxFractionDigits="4"/> <c:out value="${l.unit}"/></td><td><span class="badge badge--${l.tone}"><c:out value="${l.statusLabel}"/></span></td>
        <td><c:if test="${lotPage.canManage and (not l.tracked or empty l.number or empty l.manufactured or empty l.expiry)}"><button class="btn btn--secondary btn--sm" type="button" data-dialog-open="lot-${l.id}">Bổ sung thông tin</button></c:if></td>
      </tr></c:forEach>
      <c:if test="${empty lotPage.lots}"><tr><td colspan="7" class="text-muted">Không tìm thấy lô phù hợp. Lô mới được tạo khi xác nhận phiếu nhập kho.</td></tr></c:if>
    </tbody></table></div>
  </section>
  <c:if test="${lotPage.canManage}"><c:forEach var="l" items="${lotPage.lots}"><c:if test="${not l.tracked or empty l.number or empty l.manufactured or empty l.expiry}">
    <dialog class="dialog trace-dialog" id="lot-${l.id}" aria-labelledby="lot-title-${l.id}"><form method="post" action="${ctx}/inventory/lots/metadata">
      <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>"><input type="hidden" name="id" value="${l.id}"><input type="hidden" name="product" value="${l.productId}">
      <header class="dialog__header"><h2 id="lot-title-${l.id}">Thông tin lô · <c:out value="${l.sku}"/></h2></header><div class="dialog__body"><p>Kho: <c:out value="${l.warehouseName}"/>. Không sửa số lượng tồn.</p>
        <div class="form-field"><label class="form-label" for="lot-no-${l.id}">Số lô *</label><input class="input" id="lot-no-${l.id}" name="lotNo" maxlength="50" required value="<c:out value='${l.number}'/>"></div>
        <div class="form-field"><label class="form-label" for="lot-made-${l.id}">Ngày sản xuất *</label><input class="input" type="date" id="lot-made-${l.id}" name="manufacturedDate" required value="${l.manufactured}"></div>
        <div class="form-field"><label class="form-label" for="lot-expiry-${l.id}">Hạn dùng *</label><input class="input" type="date" id="lot-expiry-${l.id}" name="expiryDate" required value="${l.expiry}"></div>
      </div><footer class="dialog__footer"><button class="btn btn--secondary" type="button" data-dialog-close>Hủy</button><button class="btn btn--primary" type="submit">Lưu thông tin lô</button></footer>
    </form></dialog>
  </c:if></c:forEach></c:if>
</section>
<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
