<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="pageTitle" value="Hàng đang giao"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

        <div class="page-header">
          <div>
            <p class="breadcrumb">Xuất kho &amp; giao hàng</p>
            <h1>Hàng đã xuất nhưng chưa giao xong</h1>
            <p class="page-header__desc">Các chuyến còn điểm chờ giao. Hàng chưa giao vẫn thuộc trách nhiệm của chuyến cho đến khi kho xác nhận giao thành công hoặc nhận lại, không tự về kho.</p>
          </div>
        </div>

        <section class="card-grid mb-4" aria-label="Tổng quan hàng đang giao">
          <div class="card card__body home-stat">
            <div class="home-stat__head">
              <p class="stat__label">Chuyến đang chạy</p>
              <span class="home-stat__icon home-stat__icon--primary"><span class="icon icon--sm icon--truck" aria-hidden="true"></span></span>
            </div>
            <p class="home-stat__value"><span class="stat__value"><fmt:formatNumber value="${page.rows.size()}"/></span> chuyến</p>
            <p class="home-stat__note"><fmt:formatNumber value="${page.remainingStops}"/> điểm còn chờ giao</p>
          </div>
          <div class="card card__body home-stat">
            <div class="home-stat__head">
              <p class="stat__label">Tiền hàng trên đường</p>
              <span class="home-stat__icon home-stat__icon--primary"><span class="icon icon--sm icon--wallet" aria-hidden="true"></span></span>
            </div>
            <p class="home-stat__value"><span class="stat__value stat__value--money"><fmt:formatNumber value="${page.totalValue}" maxFractionDigits="0"/></span> ₫</p>
            <p class="home-stat__note">Theo số lượng đã xuất và giá đã chốt trên đơn</p>
          </div>
          <div class="card card__body home-stat${page.overdueTrips > 0 ? ' home-stat--warning' : ''}">
            <div class="home-stat__head">
              <p class="stat__label">Chuyến quá thời gian dự kiến</p>
              <span class="home-stat__icon home-stat__icon--${page.overdueTrips > 0 ? 'danger' : 'primary'}"><span class="icon icon--sm icon--history" aria-hidden="true"></span></span>
            </div>
            <p class="home-stat__value"><span class="stat__value"><fmt:formatNumber value="${page.overdueTrips}"/></span> chuyến</p>
            <p class="home-stat__note">Đã qua ngày giao mà còn điểm chưa giao</p>
          </div>
        </section>

        <section class="card list-card">
          <div class="list-summary">
            <span class="list-summary__title">Chuyến đang giao</span>
          </div>
          <div class="table-wrap list-table">
            <table class="table">
              <thead>
                <tr>
                  <th>Mã chuyến</th>
                  <th>Kho</th>
                  <th>Khu vực</th>
                  <th>Ngày giao</th>
                  <th>Tài xế / xe</th>
                  <th>Đã giao</th>
                  <th>Không thành công</th>
                  <th>Còn lại</th>
                  <th>Tiền hàng chưa giao</th>
                  <th>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                <c:if test="${empty page.rows}">
                  <tr><td colspan="10" class="text-muted">Không có chuyến nào đang giao.</td></tr>
                </c:if>
                <c:forEach var="r" items="${page.rows}">
                  <c:set var="t" value="${r.row.trip}"/>
                  <tr>
                    <td>
                      <c:out value="${t.code}"/>
                      <c:if test="${r.overdue}"><span class="badge badge--danger">Quá hạn</span></c:if>
                    </td>
                    <td><c:out value="${t.warehouseName}"/></td>
                    <td><c:out value="${t.areaName}"/></td>
                    <td><c:out value="${t.deliveryDate}"/></td>
                    <td>
                      <c:out value="${empty t.driverName ? 'Chưa gán' : t.driverName}"/>
                      <c:if test="${not empty t.licensePlate}"> · <c:out value="${t.licensePlate}"/></c:if>
                    </td>
                    <td><c:out value="${r.row.delivered}"/>/<c:out value="${r.row.stops}"/></td>
                    <td><c:out value="${r.row.failed}"/></td>
                    <td><span class="badge badge--warning"><c:out value="${r.row.remaining}"/> điểm</span></td>
                    <td><fmt:formatNumber value="${r.row.remainingValue}" maxFractionDigits="0"/> ₫</td>
                    <td><a href="${ctx}/shipments/trips/view?id=${t.id}">Xem chuyến</a></td>
                  </tr>
                </c:forEach>
              </tbody>
            </table>
          </div>
        </section>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
