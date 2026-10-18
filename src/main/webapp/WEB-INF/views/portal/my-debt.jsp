<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="pageTitle" value="Công nợ của tôi"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

<link rel="stylesheet" href="${ctx}/assets/css/my-debt.css?v=${assetVersion}">

<section class="my-debt">

  <header class="page-header">
    <div>
      <p class="breadcrumb">Cổng đại lý / Tài chính</p>
      <h1>Công nợ của tôi</h1>
      <p class="page-header__desc"><c:out value="${debt.customer.code}"/> – <c:out value="${debt.customer.name}"/> · theo dõi hoá đơn, công nợ và hạn mức còn lại.</p>
    </div>
  </header>

  <section class="my-debt__overview" aria-label="Tổng quan công nợ">
    <article class="card debt-stat">
      <span class="debt-stat__label">Tổng công nợ còn lại</span>
      <strong><fmt:formatNumber value="${debt.debt}" maxFractionDigits="0"/> ₫</strong>
    </article>
    <article class="card debt-stat">
      <span class="debt-stat__label">Hạn mức công nợ</span>
      <strong><c:choose><c:when test="${debt.limit.signum() > 0}"><fmt:formatNumber value="${debt.limit}" maxFractionDigits="0"/> ₫</c:when><c:otherwise>Không cho nợ</c:otherwise></c:choose></strong>
    </article>
    <article class="card debt-stat debt-stat--available">
      <span class="debt-stat__label">Hạn mức còn lại</span>
      <strong><fmt:formatNumber value="${debt.remaining}" maxFractionDigits="0"/> ₫</strong>
    </article>
    <article class="card debt-stat debt-stat--overdue">
      <span class="debt-stat__label">Công nợ quá hạn</span>
      <strong><fmt:formatNumber value="${debt.overdue}" maxFractionDigits="0"/> ₫</strong>
    </article>
  </section>

  <c:if test="${debt.limit.signum() > 0}">
    <section class="card debt-limit-panel" aria-label="Mức sử dụng hạn mức">
      <div class="meter meter--${debt.usageTone}">
        <div class="meter__head"><span>Đã dùng hạn mức (gồm <fmt:formatNumber value="${debt.committed}" maxFractionDigits="0"/> ₫ đang giữ cho đơn chưa lập hoá đơn)</span><span class="meter__value">${debt.usagePercent}%</span></div>
        <div class="meter__track"><span class="meter__fill meter__fill--${debt.usageStep}"></span></div>
      </div>
    </section>
  </c:if>

  <section class="card debt-invoices">
    <header class="debt-invoices__header">
      <div>
        <h2>Hoá đơn</h2>
      </div>
      <span class="badge"><c:out value="${debt.invoices.size()}"/> hoá đơn</span>
    </header>

    <div class="table-wrap debt-table-wrap">
      <table class="table debt-table">
        <caption class="visually-hidden">Danh sách hoá đơn của đại lý</caption>
        <thead>
          <tr>
            <th scope="col">Mã hoá đơn</th>
            <th scope="col">Ngày phát hành</th>
            <th scope="col">Hạn thanh toán</th>
            <th scope="col" class="num">Số tiền</th>
            <th scope="col" class="num">Còn nợ</th>
            <th scope="col">Trạng thái</th>
            <th scope="col">Thao tác</th>
          </tr>
        </thead>
        <tbody>
          <c:if test="${empty debt.invoices}">
            <tr><td colspan="7" class="text-muted">Chưa có hoá đơn nào.</td></tr>
          </c:if>
          <c:forEach var="i" items="${debt.invoices}">
            <tr>
              <td class="nowrap table__cell-code"><c:out value="${i.code}"/></td>
              <td class="nowrap"><c:out value="${i.issuedAtText}"/></td>
              <td class="nowrap"><c:out value="${i.dueDateText}"/></td>
              <td class="num nowrap"><fmt:formatNumber value="${i.totalAmount}" maxFractionDigits="0"/> ₫</td>
              <td class="num nowrap ${i.outstanding.signum() > 0 ? 'table__cell-main' : 'text-muted'}"><fmt:formatNumber value="${i.outstanding}" maxFractionDigits="0"/> ₫</td>
              <td><c:choose><c:when test="${i.overdue}"><span class="badge badge--danger">Quá hạn</span></c:when><c:otherwise><span class="badge badge--${i.status.tone}"><c:out value="${i.status.label}"/></span></c:otherwise></c:choose></td>
              <td><a class="btn btn--secondary btn--sm" href="${ctx}/portal/my-debt/invoice?id=${i.id}" target="_blank" rel="noopener">Xem / tải PDF</a></td>
            </tr>
          </c:forEach>
        </tbody>
      </table>
    </div>

    <div class="debt-mobile">
      <c:forEach var="i" items="${debt.invoices}">
        <article class="debt-card">
          <div class="debt-card__head"><strong><c:out value="${i.code}"/></strong><c:choose><c:when test="${i.overdue}"><span class="badge badge--danger">Quá hạn</span></c:when><c:otherwise><span class="badge badge--${i.status.tone}"><c:out value="${i.status.label}"/></span></c:otherwise></c:choose></div>
          <p class="debt-card__details">Hạn <c:out value="${i.dueDateText}"/> · còn nợ <fmt:formatNumber value="${i.outstanding}" maxFractionDigits="0"/> ₫</p>
          <a class="btn btn--secondary btn--sm" href="${ctx}/portal/my-debt/invoice?id=${i.id}" target="_blank" rel="noopener">Xem / tải PDF</a>
        </article>
      </c:forEach>
    </div>
  </section>
</section>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
