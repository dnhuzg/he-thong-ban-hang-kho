<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="pageTitle" value="Hoá đơn ${invoice.code}"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

        <div class="page-header">
          <div>
            <p class="breadcrumb"><a href="${ctx}/finance/invoices">Hoá đơn</a></p>
            <h1>Hoá đơn <c:out value="${invoice.code}"/></h1>
            <p class="page-header__desc"><c:out value="${invoice.customerCode}"/> – <c:out value="${invoice.customerName}"/> · phát hành <c:out value="${invoice.issuedAtText}"/> bởi <c:out value="${invoice.issuedByName}"/> · hạn thanh toán <c:out value="${invoice.dueDateText}"/></p>
          </div>
          <div class="page-header__actions">
            <span class="badge badge--${invoice.status.tone}"><c:out value="${invoice.status.label}"/></span>
            <a class="btn btn--secondary" href="${ctx}/finance/invoices/pdf?id=${invoice.id}" target="_blank" rel="noopener">In / PDF</a>
          </div>
        </div>

        <section class="card list-card">
          <div class="table-wrap">
            <table class="table">
              <thead>
                <tr>
                  <th scope="col">Phiếu xuất</th>
                  <th scope="col" class="col-sku">SKU</th>
                  <th scope="col">Sản phẩm</th>
                  <th scope="col" class="num">SL thực giao</th>
                  <th scope="col" class="num">Đơn giá</th>
                  <th scope="col" class="num">Thành tiền</th>
                </tr>
              </thead>
              <tbody>
                <c:forEach var="l" items="${invoice.lines}">
                  <tr>
                    <td class="nowrap"><c:out value="${l.shipmentCode}"/></td>
                    <td class="nowrap table__cell-code"><c:out value="${l.sku}"/></td>
                    <td><c:out value="${l.productName}"/></td>
                    <td class="num nowrap"><c:out value="${l.quantity}"/> <c:out value="${l.baseUnit}"/></td>
                    <td class="num nowrap"><fmt:formatNumber value="${l.unitPrice}" maxFractionDigits="2"/> ₫</td>
                    <td class="num nowrap"><fmt:formatNumber value="${l.amount}" maxFractionDigits="0"/> ₫</td>
                  </tr>
                </c:forEach>
              </tbody>
              <tfoot>
                <tr>
                  <th scope="row" colspan="5" class="num">Tổng tiền</th>
                  <td class="num nowrap"><strong><fmt:formatNumber value="${invoice.totalAmount}" maxFractionDigits="0"/> ₫</strong></td>
                </tr>
              </tfoot>
            </table>
          </div>
        </section>

        <section class="card list-card">
          <div class="list-summary">
            <span class="list-summary__title">Thanh toán</span>
            <span>Đã thu <strong><fmt:formatNumber value="${invoice.paidAmount}" maxFractionDigits="0"/> ₫</strong> · còn phải thu <strong><fmt:formatNumber value="${invoice.outstanding}" maxFractionDigits="0"/> ₫</strong></span>
          </div>
          <div class="table-wrap">
            <table class="table">
              <thead>
                <tr>
                  <th scope="col" class="col-sku">Phiếu thu</th>
                  <th scope="col">Ngày thu</th>
                  <th scope="col">Hình thức</th>
                  <th scope="col" class="num">Đối trừ</th>
                </tr>
              </thead>
              <tbody>
                <c:if test="${empty invoice.payments}">
                  <tr><td colspan="4" class="text-muted">Chưa có khoản thu nào cho hoá đơn này.</td></tr>
                </c:if>
                <c:forEach var="p" items="${invoice.payments}">
                  <tr>
                    <td class="nowrap table__cell-code"><c:out value="${p.paymentCode}"/></td>
                    <td class="nowrap"><c:out value="${p.paidOnText}"/></td>
                    <td><c:out value="${p.method.label}"/></td>
                    <td class="num nowrap"><fmt:formatNumber value="${p.amount}" maxFractionDigits="0"/> ₫</td>
                  </tr>
                </c:forEach>
              </tbody>
            </table>
          </div>
        </section>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
