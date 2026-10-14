<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="pageTitle" value="Ghi nhận thanh toán"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

        <div class="page-header">
          <div>
            <p class="breadcrumb">Hoá đơn &amp; công nợ</p>
            <h1>Ghi nhận thanh toán</h1>
            <p class="page-header__desc">Ghi khoản đại lý đã trả và đối trừ vào hoá đơn: chọn một hoá đơn cụ thể hoặc để hệ thống tự trừ từ hoá đơn cũ nhất. Được thu một phần, không thu vượt công nợ còn lại. Phiếu thu đã ghi không xoá được.</p>
          </div>
        </div>

        <section class="card">
          <div class="card__body">
            <c:choose>
              <c:when test="${empty payments.debtors}">
                <p class="text-muted">Không có đại lý nào đang còn nợ hoá đơn.</p>
              </c:when>
              <c:otherwise>
                <form class="form-grid" method="post" action="${ctx}/finance/payments/record">
                  <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                  <div class="form-field">
                    <label class="form-label" for="customerId">Đại lý <span class="required">*</span></label>
                    <select class="select" id="customerId" name="customerId" required>
                      <option value="">— Chọn đại lý —</option>
                      <c:forEach var="d" items="${payments.debtors}">
                        <option value="${d.id}"><c:out value="${d.name}"/> · còn nợ <fmt:formatNumber value="${d.outstanding}" maxFractionDigits="0"/> ₫</option>
                      </c:forEach>
                    </select>
                  </div>
                  <div class="form-field">
                    <label class="form-label" for="invoiceId">Đối trừ vào</label>
                    <select class="select" id="invoiceId" name="invoiceId">
                      <option value="">Tự động: hoá đơn cũ nhất trước</option>
                      <c:forEach var="i" items="${payments.openInvoices}">
                        <option value="${i.id}"><c:out value="${i.code}"/> · <c:out value="${i.customerName}"/> · còn <fmt:formatNumber value="${i.outstanding}" maxFractionDigits="0"/> ₫</option>
                      </c:forEach>
                    </select>
                  </div>
                  <div class="form-field">
                    <label class="form-label" for="amount">Số tiền (₫) <span class="required">*</span></label>
                    <input class="input" id="amount" name="amount" type="number" min="1" step="1" inputmode="numeric" required>
                  </div>
                  <div class="form-field">
                    <label class="form-label" for="method">Hình thức thu <span class="required">*</span></label>
                    <select class="select" id="method" name="method" required>
                      <c:forEach var="m" items="${payments.methods}">
                        <option value="${m.code}"><c:out value="${m.label}"/></option>
                      </c:forEach>
                    </select>
                  </div>
                  <div class="form-field">
                    <label class="form-label" for="paidOn">Ngày thu <span class="required">*</span></label>
                    <input class="input" id="paidOn" name="paidOn" type="date" max="${today}" value="${today}" required>
                  </div>
                  <div class="form-field">
                    <label class="form-label" for="collectedBy">Người thu <span class="required">*</span></label>
                    <select class="select" id="collectedBy" name="collectedBy" required>
                      <c:forEach var="u" items="${payments.collectors}">
                        <option value="${u.id}" ${u.id == currentUser.id ? 'selected' : ''}><c:out value="${u.name}"/></option>
                      </c:forEach>
                    </select>
                  </div>
                  <div class="form-field">
                    <label class="form-label" for="note">Ghi chú</label>
                    <input class="input" id="note" name="note" type="text" maxlength="500">
                  </div>
                  <div class="form-actions">
                    <button class="btn btn--primary" type="submit">Ghi nhận thanh toán</button>
                  </div>
                </form>
              </c:otherwise>
            </c:choose>
          </div>
        </section>

        <section class="card list-card">
          <div class="list-summary">
            <span class="list-summary__title">Hoá đơn còn nợ</span>
            <span class="badge badge--warning"><c:out value="${payments.openInvoices.size()}"/></span>
          </div>
          <div class="table-wrap">
            <table class="table">
              <thead>
                <tr>
                  <th scope="col" class="col-sku">Số hoá đơn</th>
                  <th scope="col">Đại lý</th>
                  <th scope="col" class="num">Còn phải thu</th>
                  <th scope="col">Hạn thanh toán</th>
                </tr>
              </thead>
              <tbody>
                <c:if test="${empty payments.openInvoices}">
                  <tr><td colspan="4" class="text-muted">Không có hoá đơn nào còn nợ.</td></tr>
                </c:if>
                <c:forEach var="i" items="${payments.openInvoices}">
                  <tr>
                    <td class="nowrap table__cell-code"><a href="${ctx}/finance/invoices/view?id=${i.id}"><c:out value="${i.code}"/></a></td>
                    <td><c:out value="${i.customerName}"/></td>
                    <td class="num nowrap"><fmt:formatNumber value="${i.outstanding}" maxFractionDigits="0"/> ₫</td>
                    <td class="nowrap"><c:out value="${i.dueDateText}"/></td>
                  </tr>
                </c:forEach>
              </tbody>
            </table>
          </div>
        </section>

        <section class="card list-card">
          <div class="list-summary">
            <span class="list-summary__title">Phiếu thu gần đây</span>
          </div>
          <div class="table-wrap">
            <table class="table">
              <thead>
                <tr>
                  <th scope="col" class="col-sku">Phiếu thu</th>
                  <th scope="col">Đại lý</th>
                  <th scope="col" class="num">Số tiền</th>
                  <th scope="col">Hình thức</th>
                  <th scope="col">Ngày thu</th>
                  <th scope="col">Người thu</th>
                  <th scope="col">Đối trừ vào</th>
                </tr>
              </thead>
              <tbody>
                <c:if test="${empty payments.recent}">
                  <tr><td colspan="7" class="text-muted">Chưa có phiếu thu nào.</td></tr>
                </c:if>
                <c:forEach var="p" items="${payments.recent}">
                  <tr>
                    <td class="nowrap table__cell-code"><c:out value="${p.code}"/></td>
                    <td><c:out value="${p.customerName}"/></td>
                    <td class="num nowrap"><fmt:formatNumber value="${p.amount}" maxFractionDigits="0"/> ₫</td>
                    <td><c:out value="${p.method.label}"/></td>
                    <td class="nowrap"><c:out value="${p.paidOnText}"/></td>
                    <td><c:out value="${p.collectedByName}"/></td>
                    <td><c:out value="${p.invoiceCodes}"/></td>
                  </tr>
                </c:forEach>
              </tbody>
            </table>
          </div>
        </section>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
