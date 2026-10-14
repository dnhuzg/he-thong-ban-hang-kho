<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="o" value="${detail.order}"/>
<c:set var="pageTitle" value="Đơn ${o.code}"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

        <div class="page-header">
          <div>
            <p class="breadcrumb"><a href="${ctx}/portal/orders">Đơn hàng của tôi</a></p>
            <h1>Đơn <c:out value="${o.code}"/></h1>
            <p class="page-header__desc">Đặt <c:out value="${o.createdAtText}"/> · giao dự kiến <c:out value="${empty o.deliveryDateText ? '—' : o.deliveryDateText}"/> · tổng <fmt:formatNumber value="${o.payable}" maxFractionDigits="0"/> ₫</p>
          </div>
          <div class="page-header__actions">
            <span class="badge badge--${o.status.tone}"><c:out value="${o.status.label}"/></span>
            <c:if test="${detail.shortShipped}"><span class="badge badge--warning">Giao thiếu</span></c:if>
            <a class="btn btn--secondary" href="${ctx}/api/orders/pdf?id=${o.id}" target="_blank" rel="noopener">In / PDF</a>
          </div>
        </div>

        <section class="card list-card">
          <div class="list-summary">
            <span class="list-summary__title">Dòng hàng</span>
            <c:if test="${not detail.exported}"><span class="text-muted">Đơn chưa xuất kho; số lượng thực giao sẽ hiện khi kho xuất hàng.</span></c:if>
          </div>
          <div class="table-wrap">
            <table class="table">
              <thead>
                <tr>
                  <th scope="col" class="col-sku">SKU</th>
                  <th scope="col">Sản phẩm</th>
                  <th scope="col" class="num">Số đặt</th>
                  <th scope="col" class="num">Đã xuất</th>
                  <th scope="col" class="num">Đã giao</th>
                  <th scope="col" class="num">Thành tiền</th>
                </tr>
              </thead>
              <tbody>
                <c:forEach var="s" items="${detail.lines}">
                  <tr>
                    <td class="nowrap table__cell-code"><c:out value="${s.line.sku}"/></td>
                    <td>
                      <span class="table__cell-main"><c:out value="${s.line.productName}"/></span>
                      <c:if test="${s.shortShipped}"><span class="badge badge--warning">Thiếu <fmt:formatNumber value="${s.missing}" maxFractionDigits="4"/> <c:out value="${s.line.baseUnitName}"/></span></c:if>
                    </td>
                    <td class="num nowrap"><c:out value="${s.line.quantity}"/> <c:out value="${s.line.unitName}"/> <span class="table__cell-sub">(<fmt:formatNumber value="${s.line.baseQuantity}" maxFractionDigits="4"/> <c:out value="${s.line.baseUnitName}"/>)</span></td>
                    <td class="num nowrap"><c:out value="${detail.exported ? '' : '—'}"/><c:if test="${detail.exported}"><fmt:formatNumber value="${s.shipped}" maxFractionDigits="4"/></c:if></td>
                    <td class="num nowrap"><c:out value="${detail.exported ? '' : '—'}"/><c:if test="${detail.exported}"><fmt:formatNumber value="${s.delivered}" maxFractionDigits="4"/></c:if></td>
                    <td class="num nowrap"><fmt:formatNumber value="${s.line.netAmount}" maxFractionDigits="0"/> ₫</td>
                  </tr>
                </c:forEach>
              </tbody>
            </table>
          </div>
        </section>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
