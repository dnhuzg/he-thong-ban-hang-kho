<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="pageTitle" value="Chỉ tiêu doanh số"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

        <div class="page-header">
          <div>
            <p class="breadcrumb">Điều hành</p>
            <h1>Chỉ tiêu doanh số</h1>
            <p class="page-header__desc">Đặt chỉ tiêu theo tháng cho từng nhân viên kinh doanh và khu vực. Doanh số tính từ hoá đơn đã phát hành (không tính hoá đơn huỷ) của đại lý do nhân viên phụ trách trong khu vực đó. Sửa chỉ tiêu của tháng đã qua phải ghi lý do.</p>
          </div>
        </div>

        <section class="card card__body mb-4" aria-labelledby="target-create-title">
          <h2 id="target-create-title">Đặt chỉ tiêu</h2>
          <form method="post" action="${ctx}/sales/targets/create" class="form-grid">
            <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
            <div class="form-field">
              <label class="form-label" for="target-month">Tháng</label>
              <input class="input" type="month" id="target-month" name="month" value="${board.monthValue}" required>
            </div>
            <div class="form-field">
              <label class="form-label" for="target-employee">Nhân viên kinh doanh</label>
              <select class="select" id="target-employee" name="employeeId" required>
                <option value="">Chọn nhân viên</option>
                <c:forEach var="r" items="${board.salesReps}"><option value="${r.id}"><c:out value="${r.name}"/></option></c:forEach>
              </select>
            </div>
            <div class="form-field">
              <label class="form-label" for="target-area">Khu vực</label>
              <select class="select" id="target-area" name="areaId" required>
                <option value="">Chọn khu vực</option>
                <c:forEach var="a" items="${board.areas}"><option value="${a.id}"><c:out value="${a.name}"/></option></c:forEach>
              </select>
            </div>
            <div class="form-field">
              <label class="form-label" for="target-amount">Chỉ tiêu (₫)</label>
              <input class="input" type="number" id="target-amount" name="amount" min="0" step="1000" required>
            </div>
            <div class="form-actions form-actions--start">
              <button class="btn btn--primary" type="submit">Đặt chỉ tiêu</button>
            </div>
          </form>
        </section>

        <section class="card list-card">
          <div class="list-summary">
            <span class="list-summary__title">Chỉ tiêu tháng <c:out value="${board.monthValue}"/></span>
            <form class="inline-form inline-form--end" method="get" action="${ctx}/sales/targets">
              <label class="visually-hidden" for="filter-month">Xem tháng</label>
              <input class="input input--date" type="month" id="filter-month" name="month" value="${board.monthValue}" data-autosubmit>
              <button class="btn btn--secondary btn--sm" type="submit">Xem</button>
            </form>
          </div>
          <div class="table-wrap list-table">
            <table class="table">
              <thead>
                <tr>
                  <th>Nhân viên</th>
                  <th>Khu vực</th>
                  <th>Chỉ tiêu (₫)</th>
                  <th>Doanh số (₫)</th>
                  <th>Tiến độ</th>
                </tr>
              </thead>
              <tbody>
                <c:if test="${empty board.rows}">
                  <tr><td colspan="5" class="text-muted">Chưa có chỉ tiêu nào trong tháng này.</td></tr>
                </c:if>
                <c:forEach var="r" items="${board.rows}">
                  <tr>
                    <td><c:out value="${r.employeeName}"/></td>
                    <td><c:out value="${r.areaName}"/></td>
                    <td>
                      <form class="inline-form" method="post" action="${ctx}/sales/targets/update">
                        <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                        <input type="hidden" name="id" value="${r.target.id}">
                        <input type="hidden" name="month" value="${board.monthValue}">
                        <label class="visually-hidden" for="amount-${r.target.id}">Chỉ tiêu của <c:out value="${r.employeeName}"/></label>
                        <input class="input" type="number" id="amount-${r.target.id}" name="amount" min="0" step="1000" value="${r.target.targetAmount}" required>
                        <c:if test="${r.locked}">
                          <label class="visually-hidden" for="reason-${r.target.id}">Lý do sửa</label>
                          <input class="input" type="text" id="reason-${r.target.id}" name="reason" maxlength="500" placeholder="Lý do sửa" required>
                        </c:if>
                        <button class="btn btn--secondary btn--sm" type="submit">Lưu</button>
                      </form>
                    </td>
                    <td><fmt:formatNumber value="${r.revenue}" maxFractionDigits="0"/></td>
                    <td>
                      <div class="meter">
                        <div class="meter__head"><span>Hoàn thành</span><span class="meter__value"><fmt:formatNumber value="${r.percentage}" maxFractionDigits="1"/>%</span></div>
                        <div class="meter__track"><span class="meter__fill meter__fill--${r.step}"></span></div>
                      </div>
                    </td>
                  </tr>
                </c:forEach>
              </tbody>
            </table>
          </div>
        </section>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
