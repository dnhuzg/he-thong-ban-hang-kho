<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="pageTitle" value="Hoá đơn"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

        <div class="page-header">
          <div>
            <p class="breadcrumb">Hoá đơn &amp; công nợ</p>
            <h1>Hoá đơn</h1>
            <p class="page-header__desc">Lập hoá đơn từ phiếu xuất đã giao thành công, theo số lượng thực giao và giá đã chốt trên đơn. Chọn nhiều phiếu của cùng một đại lý để gộp thành một hoá đơn. Hoá đơn đã phát hành không sửa, không xoá.</p>
          </div>
        </div>

        <c:set var="sum" value="${invoices.summary}"/>
        <section class="card-grid mb-4" aria-label="Tổng quan hoá đơn">
          <div class="card card__body home-stat">
            <div class="home-stat__head">
              <p class="stat__label">Tổng số hoá đơn</p>
              <span class="home-stat__icon home-stat__icon--primary"><span class="icon icon--sm icon--clipboard-list" aria-hidden="true"></span></span>
            </div>
            <p class="home-stat__value"><span class="stat__value"><fmt:formatNumber value="${sum.count}"/></span> hoá đơn</p>
            <p class="home-stat__note">Không tính hoá đơn đã huỷ</p>
          </div>
          <div class="card card__body home-stat">
            <div class="home-stat__head">
              <p class="stat__label">Tổng giá trị hoá đơn</p>
              <span class="home-stat__icon home-stat__icon--primary"><span class="icon icon--sm icon--wallet" aria-hidden="true"></span></span>
            </div>
            <p class="home-stat__value"><span class="stat__value stat__value--money"><fmt:formatNumber value="${sum.total}" maxFractionDigits="0"/></span> ₫</p>
            <p class="home-stat__note">Theo số lượng thực giao</p>
          </div>
          <div class="card card__body home-stat">
            <div class="home-stat__head">
              <p class="stat__label">Đã thanh toán</p>
              <span class="home-stat__icon home-stat__icon--primary"><span class="icon icon--sm icon--circle-check" aria-hidden="true"></span></span>
            </div>
            <p class="home-stat__value"><span class="stat__value stat__value--money"><fmt:formatNumber value="${sum.paid}" maxFractionDigits="0"/></span> ₫</p>
            <p class="home-stat__note"><a href="${ctx}/finance/payments">Ghi nhận thanh toán</a></p>
          </div>
          <div class="card card__body home-stat${sum.overdue > 0 ? ' home-stat--warning' : ''}">
            <div class="home-stat__head">
              <p class="stat__label">Còn phải thu</p>
              <span class="home-stat__icon home-stat__icon--${sum.overdue > 0 ? 'danger' : 'warning'}"><span class="icon icon--sm icon--history" aria-hidden="true"></span></span>
            </div>
            <p class="home-stat__value"><span class="stat__value stat__value--money"><fmt:formatNumber value="${sum.outstanding}" maxFractionDigits="0"/></span> ₫</p>
            <p class="home-stat__note"><c:choose><c:when test="${sum.overdue > 0}"><span class="badge badge--danger"><fmt:formatNumber value="${sum.overdue}"/> hoá đơn quá hạn</span></c:when><c:otherwise>Không có hoá đơn quá hạn</c:otherwise></c:choose></p>
          </div>
        </section>

        <section class="card list-card">
          <form method="post" action="${ctx}/finance/invoices/issue">
            <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
            <div class="list-summary">
              <span class="list-summary__title">Phiếu xuất đã giao, chưa lập hoá đơn</span>
              <span class="badge badge--primary"><c:out value="${invoices.eligible.size()}"/></span>
              <c:if test="${not empty invoices.eligible}">
                <button class="btn btn--primary btn--sm" type="submit">Phát hành hoá đơn từ phiếu đã chọn</button>
              </c:if>
            </div>
            <div class="table-wrap">
              <table class="table">
                <thead>
                  <tr>
                    <th scope="col"><span class="visually-hidden">Chọn</span></th>
                    <th scope="col" class="col-sku">Phiếu xuất</th>
                    <th scope="col">Đại lý</th>
                    <th scope="col">Đơn</th>
                    <th scope="col" class="num">Số mặt hàng</th>
                    <th scope="col">Giao lúc</th>
                  </tr>
                </thead>
                <tbody>
                  <c:if test="${empty invoices.eligible}">
                    <tr><td colspan="6" class="text-muted">Không có phiếu xuất nào đã giao mà chưa lập hoá đơn.</td></tr>
                  </c:if>
                  <c:forEach var="e" items="${invoices.eligible}">
                    <tr>
                      <td><input type="checkbox" name="shipmentId" value="${e.shipmentId}" aria-label="Chọn phiếu <c:out value='${e.shipmentCode}'/>"></td>
                      <td class="nowrap table__cell-code"><c:out value="${e.shipmentCode}"/></td>
                      <td class="table__cell-main"><c:out value="${e.customerName}"/></td>
                      <td class="nowrap"><c:out value="${e.orderCode}"/></td>
                      <td class="num"><c:out value="${e.itemCount}"/></td>
                      <td class="nowrap"><c:out value="${e.deliveredAtText}"/></td>
                    </tr>
                  </c:forEach>
                </tbody>
              </table>
            </div>
          </form>
        </section>

        <section class="card list-card">
          <div class="list-summary">
            <span class="list-summary__title">Hoá đơn gần đây</span>
          </div>
          <div class="table-wrap">
            <table class="table">
              <thead>
                <tr>
                  <th scope="col" class="col-sku">Số hoá đơn</th>
                  <th scope="col">Đại lý</th>
                  <th scope="col" class="num">Tổng tiền</th>
                  <th scope="col" class="num">Đã thanh toán</th>
                  <th scope="col" class="num">Còn phải thu</th>
                  <th scope="col">Phát hành</th>
                  <th scope="col">Hạn thanh toán</th>
                  <th scope="col">Trạng thái</th>
                </tr>
              </thead>
              <tbody>
                <c:if test="${empty invoices.recent}">
                  <tr><td colspan="8" class="text-muted">Chưa có hoá đơn nào.</td></tr>
                </c:if>
                <c:forEach var="i" items="${invoices.recent}">
                  <tr>
                    <td class="nowrap table__cell-code"><a href="${ctx}/finance/invoices/view?id=${i.id}"><c:out value="${i.code}"/></a></td>
                    <td><c:out value="${i.customerName}"/></td>
                    <td class="num nowrap table__cell-main"><fmt:formatNumber value="${i.totalAmount}" maxFractionDigits="0"/> ₫</td>
                    <td class="num nowrap text-success"><fmt:formatNumber value="${i.paidAmount}" maxFractionDigits="0"/> ₫</td>
                    <td class="num nowrap ${i.outstanding.signum() > 0 ? 'text-danger' : 'text-muted'}"><fmt:formatNumber value="${i.outstanding}" maxFractionDigits="0"/> ₫</td>
                    <td class="nowrap"><c:out value="${i.issuedAtText}"/></td>
                    <td class="nowrap"><c:out value="${i.dueDateText}"/></td>
                    <td><span class="badge badge--${i.status.tone}"><c:out value="${i.status.label}"/></span></td>
                  </tr>
                </c:forEach>
              </tbody>
            </table>
          </div>
        </section>

        <section class="card list-card">
          <div class="list-summary">
            <span class="list-summary__title">Công nợ theo đại lý</span>
            <span class="badge badge--warning"><c:out value="${invoices.debtors.size()}"/></span>
          </div>
          <div class="table-wrap">
            <table class="table">
              <thead>
                <tr>
                  <th scope="col">Đại lý</th>
                  <th scope="col" class="num">Tổng nợ</th>
                  <th scope="col" class="num">Trong hạn</th>
                  <th scope="col" class="num">Quá hạn</th>
                  <th scope="col">Hạn sớm nhất đã qua</th>
                  <th scope="col">Trạng thái</th>
                </tr>
              </thead>
              <tbody>
                <c:if test="${empty invoices.debtors}">
                  <tr><td colspan="6" class="text-muted">Không có đại lý nào còn nợ.</td></tr>
                </c:if>
                <c:forEach var="d" items="${invoices.debtors}">
                  <tr>
                    <td class="table__cell-main"><c:out value="${d.name}"/> <span class="table__cell-sub"><c:out value="${d.code}"/></span></td>
                    <td class="num nowrap table__cell-main"><fmt:formatNumber value="${d.outstanding}" maxFractionDigits="0"/> ₫</td>
                    <td class="num nowrap"><fmt:formatNumber value="${d.current}" maxFractionDigits="0"/> ₫</td>
                    <td class="num nowrap ${d.late ? 'text-danger' : 'text-muted'}"><fmt:formatNumber value="${d.overdue}" maxFractionDigits="0"/> ₫</td>
                    <td class="nowrap"><c:out value="${empty d.oldestDueText ? '—' : d.oldestDueText}"/></td>
                    <td><span class="badge ${d.late ? 'badge--danger' : 'badge--success'}"><c:out value="${d.late ? 'Quá hạn' : 'Trong hạn'}"/></span></td>
                  </tr>
                </c:forEach>
              </tbody>
            </table>
          </div>
        </section>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
