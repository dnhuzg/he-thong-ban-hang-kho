<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="${customer.name}"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

        <div class="page-header">
          <div>
            <p class="breadcrumb"><a href="${ctx}/customers">Đại lý &amp; hạn mức công nợ</a> / Đại lý</p>
            <h1><c:out value="${customer.name}"/></h1>
            <p class="page-header__desc"><c:out value="${customer.code}"/></p>
          </div>
          <div class="page-header__actions">
            <span class="badge ${customer.locked ? 'badge--danger' : 'badge--success'}"><c:out value="${customer.status.label}"/></span>
            <c:if test="${canManage}">
              <a class="btn btn--secondary" href="${ctx}/customers/edit?id=${customer.id}">Sửa hồ sơ</a>
            </c:if>
            <a class="btn btn--secondary" href="${ctx}/customers"><span class="icon icon--sm icon--arrow-left" aria-hidden="true"></span>Quay lại danh sách</a>
          </div>
        </div>

        <section class="card">
          <div class="card__header"><h2>Thông tin đại lý</h2></div>
          <dl class="card__body info-list">
            <div><dt>Mã đại lý</dt><dd><c:out value="${customer.code}"/></dd></div>
            <div><dt>Tên đại lý</dt><dd><c:out value="${customer.name}"/></dd></div>
            <div><dt>Mã số thuế</dt><dd><c:out value="${empty customer.taxCode ? '—' : customer.taxCode}"/></dd></div>
            <div><dt>Số điện thoại</dt><dd><c:out value="${empty customer.phone ? '—' : customer.phone}"/></dd></div>
            <div><dt>Nhóm khách hàng</dt><dd><c:out value="${empty customer.groupName ? '—' : customer.groupName}"/></dd></div>
            <div><dt>Khu vực</dt><dd><c:out value="${empty customer.areaName ? '—' : customer.areaName}"/></dd></div>
            <div><dt>Người phụ trách</dt><dd><c:out value="${empty customer.salesRepName ? 'Chưa phân công' : customer.salesRepName}"/></dd></div>
          </dl>
        </section>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
