<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="pageTitle" value="Phân tích tuổi nợ"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>
<link rel="stylesheet" href="${ctx}/assets/css/debt-aging.css?v=${assetVersion}">

<c:set var="f" value="${aging.filter}"/>
<c:url var="exportUrl" value="/finance/debt-aging/export">
  <c:param name="asOf" value="${f.asOf}"/>
  <c:param name="q" value="${f.keyword}"/>
  <c:param name="bucket" value="${f.bucket}"/>
  <c:if test="${f.overdueOnly}"><c:param name="overdue" value="1"/></c:if>
</c:url>

<section class="aging-screen">
  <header class="page-header aging-header">
    <div>
      <p class="breadcrumb">Hoá đơn &amp; công nợ / Phân tích tuổi nợ</p>
      <h1>Phân tích tuổi nợ</h1>
      <p class="page-header__desc">Sổ công nợ theo đại lý từ hoá đơn đã phát hành và các khoản đã thu. Tuổi nợ tính từ ngày phát hành; quá hạn tính từ hạn thanh toán.</p>
    </div>
    <a class="btn btn--primary" href="${exportUrl}">Xuất Excel</a>
  </header>

  <div class="aging-summary" aria-label="Tổng hợp công nợ">
    <div class="card aging-stat aging-stat--total"><span>Tổng còn nợ</span><strong><fmt:formatNumber value="${aging.total.outstanding}" maxFractionDigits="0"/> ₫</strong><small>Quá hạn: <fmt:formatNumber value="${aging.total.overdue}" maxFractionDigits="0"/> ₫</small></div>
    <c:forEach var="label" items="${aging.bucketLabels}" varStatus="st">
      <div class="card aging-stat${st.index == 3 ? ' aging-stat--late' : ''}"><span><c:out value="${label}"/></span><strong><fmt:formatNumber value="${aging.total.buckets[st.index]}" maxFractionDigits="0"/> ₫</strong><small>Tính từ ngày phát hành</small></div>
    </c:forEach>
  </div>

  <section class="card aging-panel" aria-labelledby="aging-list-title">
    <div class="aging-panel__heading"><div><h2 id="aging-list-title">Sổ công nợ theo đại lý</h2><p>Đối chiếu tới hết ngày <c:out value="${f.asOf}"/>.</p></div><span class="badge"><c:out value="${aging.rows.size()}"/> đại lý</span></div>
    <form class="aging-filters" method="get" action="${ctx}/finance/debt-aging" role="search">
      <div class="form-field aging-filters__search"><label class="form-label" for="aging-search">Tìm đại lý hoặc mã hoá đơn</label><input class="input" id="aging-search" name="q" type="search" value="<c:out value='${f.keyword}'/>" placeholder="Nhập tên, mã đại lý, mã hoá đơn" autocomplete="off"></div>
      <div class="form-field"><label class="form-label" for="aging-bucket">Nhóm tuổi nợ</label>
        <select class="select" id="aging-bucket" name="bucket" data-autosubmit>
          <option value="">Tất cả nhóm</option>
          <c:forEach var="label" items="${aging.bucketLabels}" varStatus="st"><option value="${st.index}" ${f.bucket == st.index ? 'selected' : ''}><c:out value="${label}"/></option></c:forEach>
        </select>
      </div>
      <div class="form-field"><label class="form-label" for="aging-as-of">Ngày đối chiếu</label><input class="input" id="aging-as-of" name="asOf" type="date" max="${today}" value="<c:out value='${f.asOf}'/>"></div>
      <label class="aging-overdue"><input name="overdue" value="1" type="checkbox" ${f.overdueOnly ? 'checked' : ''} data-autosubmit> Chỉ xem quá hạn</label>
      <button class="btn btn--secondary" type="submit">Lọc</button>
    </form>
    <div class="table-wrap aging-table-wrap">
      <table class="table aging-table">
        <caption class="visually-hidden">Công nợ phải thu và bốn nhóm tuổi nợ theo đại lý</caption>
        <thead><tr><th scope="col">Đại lý</th><th scope="col" class="aging-number">Phải thu</th><th scope="col" class="aging-number">Đã thu</th><th scope="col" class="aging-number">Còn nợ</th><th scope="col" class="aging-number">0–30</th><th scope="col" class="aging-number">31–60</th><th scope="col" class="aging-number">61–90</th><th scope="col" class="aging-number">&gt;90 ngày</th><th scope="col">Trạng thái</th></tr></thead>
        <tbody>
          <c:if test="${empty aging.rows}">
            <tr><td colspan="9" class="text-muted">Không có công nợ phù hợp bộ lọc.</td></tr>
          </c:if>
          <c:forEach var="r" items="${aging.rows}">
            <tr>
              <td><span class="aging-table__name"><c:out value="${r.name}"/></span><span class="aging-table__meta"><c:out value="${r.code}"/> · <c:out value="${r.openInvoices}"/> hoá đơn còn nợ</span></td>
              <td class="aging-number"><fmt:formatNumber value="${r.receivable}" maxFractionDigits="0"/></td>
              <td class="aging-number"><fmt:formatNumber value="${r.paid}" maxFractionDigits="0"/></td>
              <td class="aging-number aging-table__balance"><fmt:formatNumber value="${r.outstanding}" maxFractionDigits="0"/></td>
              <c:forEach var="b" items="${r.buckets}" varStatus="st"><td class="aging-number${st.index == 3 and b.signum() > 0 ? ' aging-table__late' : ''}"><fmt:formatNumber value="${b}" maxFractionDigits="0"/></td></c:forEach>
              <td><span class="badge ${r.late ? 'badge--danger' : (r.outstanding.signum() > 0 ? 'badge--warning' : 'badge--success')}"><c:out value="${r.late ? 'Quá hạn' : (r.outstanding.signum() > 0 ? 'Trong hạn' : 'Đã thu đủ')}"/></span></td>
            </tr>
          </c:forEach>
        </tbody>
        <tfoot><tr><th scope="row">Tổng theo bộ lọc</th><td class="aging-number"><fmt:formatNumber value="${aging.total.receivable}" maxFractionDigits="0"/></td><td class="aging-number"><fmt:formatNumber value="${aging.total.paid}" maxFractionDigits="0"/></td><td class="aging-number"><fmt:formatNumber value="${aging.total.outstanding}" maxFractionDigits="0"/></td><c:forEach var="b" items="${aging.total.buckets}"><td class="aging-number"><fmt:formatNumber value="${b}" maxFractionDigits="0"/></td></c:forEach><td></td></tr></tfoot>
      </table>
    </div>
    <div class="aging-mobile">
      <c:forEach var="r" items="${aging.rows}">
        <div class="aging-mobile__card"><strong><c:out value="${r.name}"/></strong><small><c:out value="${r.code}"/></small>
          <div class="aging-mobile__values"><span>Còn nợ <fmt:formatNumber value="${r.outstanding}" maxFractionDigits="0"/> ₫</span><span class="${r.late ? 'aging-table__late' : ''}">Quá hạn <fmt:formatNumber value="${r.overdue}" maxFractionDigits="0"/> ₫</span></div>
        </div>
      </c:forEach>
    </div>
  </section>
</section>
<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
