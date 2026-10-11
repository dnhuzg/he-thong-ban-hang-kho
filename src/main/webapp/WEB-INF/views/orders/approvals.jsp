<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="pageTitle" value="Duyệt đơn"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

        <div class="page-header">
          <div>
            <p class="breadcrumb">Bán hàng</p>
            <h1>Duyệt đơn</h1>
            <p class="page-header__desc">Đơn vượt hạn mức công nợ, bán dưới giá sàn hoặc do đại lý tự gửi qua cổng. Mở đơn để xem chi tiết và Duyệt, Trả lại sửa hoặc Từ chối.</p>
          </div>
        </div>

        <section class="card list-card">
          <div class="list-summary">
            <span class="list-summary__title">Đơn chờ duyệt</span>
            <span class="badge badge--warning"><c:out value="${pending.size()}"/></span>
          </div>

          <div class="table-wrap list-table">
            <table class="table">
              <thead>
                <tr>
                  <th scope="col" class="col-sku">Mã đơn</th>
                  <th scope="col">Đại lý</th>
                  <th scope="col" class="num">Tổng phải thu</th>
                  <th scope="col">Lý do cần duyệt</th>
                  <th scope="col" class="col-menu"><span class="visually-hidden">Thao tác</span></th>
                </tr>
              </thead>
              <tbody>
                <c:if test="${empty pending}">
                  <tr><td colspan="5" class="text-muted">Không có đơn nào đang chờ duyệt.</td></tr>
                </c:if>
                <c:forEach var="p" items="${pending}">
                  <tr>
                    <td class="nowrap table__cell-code"><a href="${ctx}/orders/edit?id=${p.id}#duyet-don"><c:out value="${p.code}"/></a></td>
                    <td class="table__cell-main"><c:out value="${p.customer}"/></td>
                    <td class="num nowrap table__cell-main"><fmt:formatNumber value="${p.total}" maxFractionDigits="2"/> ₫</td>
                    <td><c:out value="${p.reason}"/></td>
                    <td class="nowrap"><a class="btn btn--secondary btn--sm" href="${ctx}/orders/edit?id=${p.id}#duyet-don">Xem &amp; duyệt</a></td>
                  </tr>
                </c:forEach>
              </tbody>
            </table>
          </div>

          <%-- Điện thoại: mỗi đơn một thẻ --%>
          <div class="product-cards">
            <c:if test="${empty pending}">
              <p class="text-muted">Không có đơn nào đang chờ duyệt.</p>
            </c:if>
            <c:forEach var="p" items="${pending}">
              <article class="product-card">
                <div class="product-card__head">
                  <div class="product-card__title">
                    <a class="product-card__name" href="${ctx}/orders/edit?id=${p.id}#duyet-don"><c:out value="${p.customer}"/></a>
                    <p class="product-card__sku"><c:out value="${p.code}"/></p>
                  </div>
                </div>
                <div class="product-card__meta">
                  <p>Phải thu <strong><fmt:formatNumber value="${p.total}" maxFractionDigits="2"/> ₫</strong></p>
                  <p><c:out value="${p.reason}"/></p>
                </div>
                <div class="product-card__foot">
                  <a class="btn btn--secondary btn--sm" href="${ctx}/orders/edit?id=${p.id}#duyet-don">Xem &amp; duyệt</a>
                </div>
              </article>
            </c:forEach>
          </div>
        </section>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
