<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="pageTitle" value="Tồn kho theo SKU"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>
<link rel="stylesheet" href="${ctx}/assets/css/stock.css?v=${assetVersion}">

<c:set var="f" value="${stock.filter}"/>
<c:url var="exportUrl" value="/inventory/stock/export">
  <c:param name="q" value="${f.keyword}"/>
  <c:param name="warehouse" value="${f.warehouseId}"/>
  <c:param name="category" value="${f.categoryId}"/>
  <c:param name="status" value="${f.status}"/>
</c:url>

<section class="stock-screen">
  <div class="page-header stock-header">
    <div>
      <p class="breadcrumb">Tồn kho &amp; nhập kho / Tồn kho theo SKU</p>
      <h1>Tồn kho theo SKU và kho</h1>
      <p class="page-header__desc">Tồn thực tế cộng từ phiếu nhập và trừ khi xuất kho; đang giữ chỗ là phần của đơn đã duyệt chưa xuất. Khả dụng = thực tế − giữ chỗ.</p>
    </div>
  </div>

  <div class="stock-overview" aria-label="Tổng quan tồn kho">
    <div class="card stock-stat">
      <span class="stock-stat__icon"><span class="icon icon--package" aria-hidden="true"></span></span>
      <div><p>Còn khả dụng</p><strong><fmt:formatNumber value="${stock.availableCount}"/></strong><small>Dòng SKU · kho đặt hàng được</small></div>
    </div>
    <div class="card stock-stat stock-stat--warning">
      <span class="stock-stat__icon stock-stat__icon--warning"><span class="icon icon--info" aria-hidden="true"></span></span>
      <div><p>Đã giữ chỗ hết</p><strong><fmt:formatNumber value="${stock.reservedOutCount}"/></strong><small>Còn hàng nhưng đã giữ cho đơn</small></div>
    </div>
    <div class="card stock-stat stock-stat--danger">
      <span class="stock-stat__icon stock-stat__icon--danger"><span class="icon icon--boxes" aria-hidden="true"></span></span>
      <div><p>Hết hàng</p><strong><fmt:formatNumber value="${stock.emptyCount}"/></strong><small>Tồn thực tế bằng 0</small></div>
    </div>
  </div>

  <section class="card stock-panel" aria-labelledby="stock-table-title">
    <div class="stock-panel__heading">
      <div>
        <h2 id="stock-table-title">Sổ tồn theo SKU và kho</h2>
        <p>Chỉ hiện SKU đã từng có tồn ở kho.</p>
      </div>
      <div class="stock-panel__actions">
        <a class="btn btn--primary" href="${exportUrl}">Xuất Excel</a>
        <span class="badge"><c:out value="${stock.lines.size()}"/> kết quả</span>
      </div>
    </div>

    <form class="stock-filters" method="get" action="${ctx}/inventory/stock" role="search">
      <div class="form-field stock-filters__search">
        <label class="form-label" for="stock-search">Tìm sản phẩm</label>
        <div class="stock-search-wrap">
          <span class="icon icon--search" aria-hidden="true"></span>
          <input class="input" type="search" id="stock-search" name="q" value="<c:out value='${f.keyword}'/>" placeholder="Nhập mã SKU hoặc tên sản phẩm" autocomplete="off">
        </div>
      </div>
      <div class="form-field">
        <label class="form-label" for="stock-warehouse">Kho</label>
        <select class="select" id="stock-warehouse" name="warehouse" data-autosubmit>
          <option value="">Tất cả kho</option>
          <c:forEach var="w" items="${stock.warehouses}"><option value="${w.id}" ${w.id == f.warehouseId ? 'selected' : ''}><c:out value="${w.name}"/></option></c:forEach>
        </select>
      </div>
      <div class="form-field">
        <label class="form-label" for="stock-category">Nhóm hàng</label>
        <select class="select" id="stock-category" name="category" data-autosubmit>
          <option value="">Tất cả nhóm</option>
          <c:forEach var="cat" items="${stock.categories}"><option value="${cat.id}" ${cat.id == f.categoryId ? 'selected' : ''}><c:forEach begin="2" end="${cat.level}">— </c:forEach><c:out value="${cat.name}"/></option></c:forEach>
        </select>
      </div>
      <div class="form-field">
        <label class="form-label" for="stock-status">Trạng thái tồn</label>
        <select class="select" id="stock-status" name="status" data-autosubmit>
          <option value="">Tất cả trạng thái</option>
          <option value="AVAILABLE" ${f.status == 'AVAILABLE' ? 'selected' : ''}>Còn khả dụng</option>
          <option value="RESERVED_OUT" ${f.status == 'RESERVED_OUT' ? 'selected' : ''}>Đã giữ chỗ hết</option>
          <option value="EMPTY" ${f.status == 'EMPTY' ? 'selected' : ''}>Hết hàng</option>
        </select>
      </div>
      <button class="btn btn--secondary" type="submit">Lọc</button>
    </form>

    <div class="stock-table-wrap table-wrap">
      <table class="table stock-table">
        <caption class="visually-hidden">Tồn thực tế, đang giữ chỗ và khả dụng theo SKU và kho</caption>
        <thead>
          <tr>
            <th scope="col">Sản phẩm / SKU</th>
            <th scope="col">Kho</th>
            <th scope="col">Nhóm hàng</th>
            <th scope="col" class="stock-number">Tồn thực tế</th>
            <th scope="col" class="stock-number">Đang giữ chỗ</th>
            <th scope="col" class="stock-number">Khả dụng</th>
            <th scope="col">Trạng thái</th>
          </tr>
        </thead>
        <tbody>
          <c:if test="${empty stock.lines}">
            <tr><td colspan="7" class="text-muted">Không có dòng tồn nào khớp bộ lọc.</td></tr>
          </c:if>
          <c:forEach var="l" items="${stock.lines}">
            <tr>
              <td><span class="table__cell-main"><c:out value="${l.productName}"/></span><span class="table__cell-sub"><c:out value="${l.sku}"/> · <c:out value="${l.baseUnit}"/></span></td>
              <td><c:out value="${l.warehouseName}"/></td>
              <td class="text-muted"><c:out value="${l.categoryName}"/></td>
              <td class="stock-number"><fmt:formatNumber value="${l.onHand}" maxFractionDigits="4"/></td>
              <td class="stock-number"><fmt:formatNumber value="${l.reserved}" maxFractionDigits="4"/></td>
              <td class="stock-number table__cell-main"><fmt:formatNumber value="${l.available}" maxFractionDigits="4"/></td>
              <td>
                <c:choose>
                  <c:when test="${l.status == 'AVAILABLE'}"><span class="badge badge--success">Còn khả dụng</span></c:when>
                  <c:when test="${l.status == 'RESERVED_OUT'}"><span class="badge badge--warning">Đã giữ chỗ hết</span></c:when>
                  <c:otherwise><span class="badge badge--danger">Hết hàng</span></c:otherwise>
                </c:choose>
              </td>
            </tr>
          </c:forEach>
        </tbody>
      </table>
    </div>

    <div class="stock-mobile">
      <c:forEach var="l" items="${stock.lines}">
        <div class="stock-mobile__card${l.status == 'EMPTY' ? ' stock-mobile__card--out' : (l.status == 'RESERVED_OUT' ? ' stock-mobile__card--low' : '')}">
          <div class="stock-mobile__header"><strong><c:out value="${l.productName}"/></strong></div>
          <p class="stock-mobile__subtitle"><c:out value="${l.sku}"/> · <c:out value="${l.warehouseName}"/></p>
          <div class="stock-mobile__values">
            <div><span>Thực tế</span><strong><fmt:formatNumber value="${l.onHand}" maxFractionDigits="4"/></strong></div>
            <div><span>Giữ chỗ</span><strong><fmt:formatNumber value="${l.reserved}" maxFractionDigits="4"/></strong></div>
            <div><span>Khả dụng</span><strong><fmt:formatNumber value="${l.available}" maxFractionDigits="4"/></strong></div>
          </div>
        </div>
      </c:forEach>
    </div>
  </section>
</section>
<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
