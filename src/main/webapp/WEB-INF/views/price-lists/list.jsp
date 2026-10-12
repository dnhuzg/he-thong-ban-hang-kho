<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Bảng giá"/>
<c:set var="catalogTab" value="prices"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

        <div class="page-header">
          <div>
            <p class="breadcrumb">Sản phẩm &amp; bảng giá</p>
            <h1>Bảng giá</h1>
            <p class="page-header__desc">Bảng giá theo nhóm khách hàng và thời gian hiệu lực.</p>
          </div>
          <c:if test="${canWrite}">
            <a class="btn btn--primary" href="${ctx}/products/price-lists/new"><span class="icon icon--sm icon--plus" aria-hidden="true"></span>Tạo bảng giá</a>
          </c:if>
        </div>

<%@ include file="/WEB-INF/views/products/nav.jspf" %>

        <section class="card list-card">
          <form class="toolbar" action="${ctx}/products/price-lists" method="get">
            <label class="visually-hidden" for="group">Nhóm khách hàng</label>
            <select class="select toolbar__wide" id="group" name="group" data-autosubmit>
              <option value="">Tất cả nhóm khách</option>
              <c:forEach var="g" items="${groups}">
                <option value="${g.id}" ${g.id == selectedGroup ? 'selected' : ''}><c:out value="${g.name}"/></option>
              </c:forEach>
            </select>
            <button class="btn btn--secondary" type="submit"><span class="icon icon--sm icon--list-filter" aria-hidden="true"></span>Lọc</button>
          </form>

          <div class="pl-list">
            <div class="pl-row pl-row--head" aria-hidden="true">
              <span>Bảng giá</span>
              <span>Nhóm khách hàng</span>
              <span>Hiệu lực</span>
              <span>Số dòng giá</span>
              <span></span>
            </div>
            <c:if test="${empty lists}">
              <p class="pl-empty text-muted">Chưa có bảng giá nào.</p>
            </c:if>
            <c:forEach var="pl" items="${lists}">
              <a class="pl-row" href="${ctx}/products/price-lists/view?id=${pl.id}">
                <span class="pl-row__name">
                  <span class="pl-row__title"><c:out value="${pl.name}"/></span>
                  <span class="pl-row__sub"><c:out value="${pl.code} · phiên bản ${pl.version}"/></span>
                  <span class="badge-list">
                    <span class="badge ${pl.validityLabel == 'Đang áp dụng' ? 'badge--success' : ''}"><c:out value="${pl.validityLabel}"/></span>
                    <c:if test="${pl.locked}"><span class="badge badge--warning">Đã phát sinh đơn</span></c:if>
                    <c:if test="${pl.hasNewerVersion}"><span class="badge">Đã có bản mới</span></c:if>
                  </span>
                </span>
                <span class="pl-row__group"><c:out value="${pl.customerGroupName}"/></span>
                <span class="pl-row__valid"><c:out value="${pl.validFromText} – ${pl.validToText}"/></span>
                <span class="pl-row__count"><strong>${pl.itemCount}</strong><span class="pl-row__count-suffix"> dòng giá</span></span>
                <span class="icon icon--sm icon--chevron-right pl-row__chevron" aria-hidden="true"></span>
              </a>
            </c:forEach>
          </div>
          <p class="pl-foot text-muted">Hiển thị 1–${lists.size()} trên ${lists.size()} bảng giá</p>
        </section>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
