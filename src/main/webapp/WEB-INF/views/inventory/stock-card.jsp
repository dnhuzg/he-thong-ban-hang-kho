<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="pageTitle" value="Thẻ kho sản phẩm"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>
<link rel="stylesheet" href="${ctx}/assets/css/inventory-trace.css?v=${assetVersion}">

<c:set var="f" value="${card.filter}"/>
<section class="trace-screen">
  <div class="page-header trace-header">
    <div>
      <p class="breadcrumb">Tồn kho &amp; nhập kho / Thẻ kho sản phẩm</p>
      <h1>Thẻ kho sản phẩm</h1>
      <p class="page-header__desc">Mọi phát sinh nhập, xuất, trả về kho của một sản phẩm theo thời gian, kèm tồn sau phát sinh và chứng từ gốc.</p>
    </div>
  </div>

  <section class="card trace-panel" aria-labelledby="card-filter-title">
    <header class="trace-panel__heading"><div><h2 id="card-filter-title">Tra cứu thẻ kho</h2><p>Chọn sản phẩm, kho và khoảng thời gian cần đối chiếu.</p></div></header>
    <form class="trace-filters trace-filters--card" method="get" action="${ctx}/inventory/stock-card" role="search">
      <div class="form-field"><label class="form-label" for="card-sku">Sản phẩm</label>
        <select class="select" id="card-sku" name="product" required>
          <option value="">— Chọn sản phẩm —</option>
          <c:forEach var="p" items="${card.products}"><option value="${p.id}" ${p.id == f.productId ? 'selected' : ''}><c:out value="${p.name}"/></option></c:forEach>
        </select>
      </div>
      <div class="form-field"><label class="form-label" for="card-warehouse">Kho</label>
        <select class="select" id="card-warehouse" name="warehouse">
          <option value="">Tất cả kho</option>
          <c:forEach var="w" items="${card.warehouses}"><option value="${w.id}" ${w.id == f.warehouseId ? 'selected' : ''}><c:out value="${w.name}"/></option></c:forEach>
        </select>
      </div>
      <div class="form-field"><label class="form-label" for="card-from">Từ ngày</label><input class="input" id="card-from" name="from" type="date" value="<c:out value='${f.from}'/>"></div>
      <div class="form-field"><label class="form-label" for="card-to">Đến ngày</label><input class="input" id="card-to" name="to" type="date" value="<c:out value='${f.to}'/>"></div>
      <button class="btn btn--primary" type="submit">Xem thẻ kho</button>
    </form>
  </section>

  <c:if test="${card.selected}">
    <div class="trace-stats trace-stats--four" aria-label="Tổng hợp thẻ kho">
      <div class="card trace-stat"><span>Tồn đầu kỳ</span><strong><fmt:formatNumber value="${card.opening}" maxFractionDigits="4"/></strong><small>Gồm phát sinh trước ngày bắt đầu</small></div>
      <div class="card trace-stat"><span>Tăng trong kỳ</span><strong><fmt:formatNumber value="${card.increase}" maxFractionDigits="4"/></strong><small>Nhập kho, hàng trả về</small></div>
      <div class="card trace-stat"><span>Giảm trong kỳ</span><strong><fmt:formatNumber value="${card.decrease}" maxFractionDigits="4"/></strong><small>Xuất kho</small></div>
      <div class="card trace-stat"><span>Tồn cuối kỳ</span><strong><fmt:formatNumber value="${card.closing}" maxFractionDigits="4"/></strong><small>Đầu kỳ + tăng − giảm</small></div>
    </div>

    <section class="card trace-panel" aria-labelledby="card-title">
      <header class="trace-panel__heading"><div><h2 id="card-title">Lịch sử phát sinh</h2><p><c:out value="${f.from}"/> → <c:out value="${f.to}"/></p></div><span class="badge"><c:out value="${card.lines.size()}"/> phát sinh</span></header>
      <div class="table-wrap">
        <table class="table trace-table">
          <caption class="visually-hidden">Phát sinh nhập xuất, chứng từ gốc và tồn sau phát sinh</caption>
          <thead><tr><th scope="col">Thời điểm</th><th scope="col">Chứng từ gốc</th><th scope="col">Loại phát sinh</th><th scope="col">Kho</th><th scope="col" class="trace-number">Tăng</th><th scope="col" class="trace-number">Giảm</th><th scope="col" class="trace-number">Tồn sau</th></tr></thead>
          <tbody>
            <c:if test="${empty card.lines}">
              <tr><td colspan="7" class="text-muted">Không có phát sinh trong kỳ. Tồn đầu kỳ vẫn tính từ lịch sử trước kỳ.</td></tr>
            </c:if>
            <c:forEach var="l" items="${card.lines}">
              <tr>
                <td class="nowrap"><c:out value="${l.time}"/></td>
                <td class="nowrap table__cell-code"><c:choose><c:when test="${not empty l.documentUrl}"><a href="${ctx}${l.documentUrl}"><c:out value="${l.documentCode}"/></a></c:when><c:otherwise><c:out value="${l.documentCode}"/></c:otherwise></c:choose></td>
                <td><c:out value="${l.type}"/></td>
                <td><c:out value="${l.warehouseName}"/></td>
                <td class="trace-number"><c:if test="${l.increase.signum() > 0}"><fmt:formatNumber value="${l.increase}" maxFractionDigits="4"/></c:if></td>
                <td class="trace-number"><c:if test="${l.decrease.signum() > 0}"><fmt:formatNumber value="${l.decrease}" maxFractionDigits="4"/></c:if></td>
                <td class="trace-number trace-table__balance"><fmt:formatNumber value="${l.balance}" maxFractionDigits="4"/></td>
              </tr>
            </c:forEach>
          </tbody>
        </table>
      </div>
      <footer class="trace-footer">Thứ tự thời gian từ cũ đến mới; số lượng theo đơn vị cơ sở.</footer>
    </section>
  </c:if>
</section>
<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
