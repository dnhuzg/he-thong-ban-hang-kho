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

  <section class="aging-insights" aria-label="Phân tích công nợ quá hạn">
    <section class="card aging-panel aging-overdue-panel" aria-labelledby="overdue-aging-title">
      <div class="aging-panel__heading">
        <div>
          <h2 id="overdue-aging-title">Công nợ quá hạn theo tuổi nợ</h2>
          <p>Chỉ tính số dư chưa thanh toán của hoá đơn đã quá hạn; nhóm tuổi tính từ ngày phát hành hoá đơn.</p>
        </div>
        <span class="badge badge--danger">Quá hạn: <fmt:formatNumber value="${aging.total.overdue}" maxFractionDigits="0"/> ₫</span>
      </div>
      <div class="aging-overdue-buckets">
        <c:forEach var="label" items="${aging.bucketLabels}" varStatus="st">
          <div class="aging-overdue-bucket${st.index == 3 ? ' aging-overdue-bucket--critical' : ''}">
            <span><c:out value="${label}"/></span>
            <strong><fmt:formatNumber value="${aging.overdueAgeBuckets[st.index]}" maxFractionDigits="0"/> ₫</strong>
          </div>
        </c:forEach>
      </div>
    </section>

    <section class="card aging-panel aging-risk-panel" aria-labelledby="highest-risk-title">
      <div class="aging-panel__heading">
        <div>
          <h2 id="highest-risk-title">Đại lý có rủi ro cao nhất</h2>
          <p>Xếp hạng theo số dư quá hạn; nếu bằng nhau, ưu tiên nhóm tuổi nợ cũ hơn.</p>
        </div>
        <span class="badge badge--danger">Ưu tiên xử lý</span>
      </div>
      <c:choose>
        <c:when test="${not empty aging.highestRiskCustomer}">
          <div class="aging-risk-detail">
            <div>
              <span class="aging-table__meta"><c:out value="${aging.highestRiskCustomer.code}"/></span>
              <strong class="aging-risk-detail__name"><c:out value="${aging.highestRiskCustomer.name}"/></strong>
              <span class="aging-table__meta">Nhân viên phụ trách: <c:out value="${aging.highestRiskCustomer.salesRepName}"/></span>
            </div>
            <div class="aging-risk-detail__amount">
              <span>Tổng quá hạn</span>
              <strong><fmt:formatNumber value="${aging.highestRiskCustomer.overdueTotal}" maxFractionDigits="0"/> ₫</strong>
              <small><c:out value="${aging.highestRiskCustomer.overdueInvoiceCount}"/> hoá đơn quá hạn · lâu nhất <c:out value="${aging.highestRiskCustomer.oldestOverdueDays}"/> ngày</small>
            </div>
          </div>
          <div class="aging-risk-buckets">
            <c:forEach var="label" items="${aging.bucketLabels}" varStatus="st">
              <div><span><c:out value="${label}"/></span><strong><fmt:formatNumber value="${aging.highestRiskCustomer.ageBuckets[st.index]}" maxFractionDigits="0"/> ₫</strong></div>
            </c:forEach>
          </div>
        </c:when>
        <c:otherwise>
          <p class="aging-empty">Không có đại lý có công nợ quá hạn phù hợp bộ lọc.</p>
        </c:otherwise>
      </c:choose>
    </section>
  </section>

  <section class="card aging-panel aging-owner-panel" aria-labelledby="owner-aging-title">
    <div class="aging-panel__heading">
      <div><h2 id="owner-aging-title">Công nợ quá hạn theo nhân viên phụ trách</h2><p>Số liệu được tổng hợp từ hoá đơn quá hạn và phân công đại lý trên hệ thống.</p></div>
      <span class="badge"><c:out value="${aging.ownerSummaries.size()}"/> nhân viên</span>
    </div>
    <div class="table-wrap">
      <table class="table aging-owner-table">
        <caption class="visually-hidden">Công nợ quá hạn theo nhân viên phụ trách và nhóm tuổi nợ</caption>
        <thead><tr><th scope="col">Nhân viên phụ trách</th><th scope="col" class="aging-number">Đại lý quá hạn</th><th scope="col" class="aging-number">0–30 ngày</th><th scope="col" class="aging-number">31–60 ngày</th><th scope="col" class="aging-number">61–90 ngày</th><th scope="col" class="aging-number">Trên 90 ngày</th><th scope="col" class="aging-number">Tổng quá hạn</th></tr></thead>
        <tbody>
          <c:if test="${empty aging.ownerSummaries}"><tr><td colspan="7" class="text-muted">Không có công nợ quá hạn phù hợp bộ lọc.</td></tr></c:if>
          <c:forEach var="owner" items="${aging.ownerSummaries}">
            <tr>
              <td><strong><c:out value="${owner.salesRepName}"/></strong></td>
              <td class="aging-number"><c:out value="${owner.overdueCustomerCount}"/></td>
              <c:forEach var="amount" items="${owner.ageBuckets}"><td class="aging-number"><fmt:formatNumber value="${amount}" maxFractionDigits="0"/></td></c:forEach>
              <td class="aging-number aging-table__late"><strong><fmt:formatNumber value="${owner.overdueTotal}" maxFractionDigits="0"/></strong></td>
            </tr>
          </c:forEach>
        </tbody>
      </table>
    </div>
  </section>

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
