<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Chi tiết chuyến giao hàng"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

<div class="page-header">
  <div>
    <p class="breadcrumb">Xuất kho &amp; giao hàng</p>
    <h1>Chuyến giao <c:out value="${detail.trip.code}"/></h1>
    <p class="page-header__desc">
      Xem tuyến giao, gán tài xế và in danh sách điểm giao.
    </p>
  </div>
  <div>
    <a class="btn btn--sm" href="${ctx}/shipments/trips">Quay lại</a>
    <a class="btn btn--primary btn--sm"
       href="${ctx}/shipments/trips/print?id=${detail.trip.id}"
       target="_blank" rel="noopener">In danh sách điểm giao (PDF)</a>
  </div>
</div>

<section class="card list-card">
  <div class="list-summary">
    <span class="list-summary__title">Thông tin chuyến giao</span>
  </div>

  <div class="table-wrap">
    <table class="table">
      <tbody>
        <tr>
          <th scope="row">Mã chuyến</th>
          <td><c:out value="${detail.trip.code}"/></td>
          <th scope="row">Trạng thái</th>
          <td><c:out value="${detail.trip.status}"/></td>
        </tr>
        <tr>
          <th scope="row">Kho</th>
          <td><c:out value="${detail.trip.warehouseName}"/></td>
          <th scope="row">Khu vực</th>
          <td><c:out value="${detail.trip.areaName}"/></td>
        </tr>
        <tr>
          <th scope="row">Ngày giao</th>
          <td><c:out value="${detail.trip.deliveryDate}"/></td>
          <th scope="row">Số điểm giao</th>
          <td><c:out value="${detail.stops.size()}"/></td>
        </tr>
        <tr>
          <th scope="row">Tài xế</th>
          <td><c:out value="${empty detail.trip.driverName ? 'Chưa gán' : detail.trip.driverName}"/></td>
          <th scope="row">Phương tiện</th>
          <td><c:out value="${empty detail.trip.licensePlate ? 'Chưa gán' : detail.trip.licensePlate}"/></td>
        </tr>
      </tbody>
    </table>
  </div>
</section>

<section class="card list-card">
  <div class="list-summary">
    <span class="list-summary__title">Gán tài xế và phương tiện</span>
  </div>

  <c:choose>
    <c:when test="${detail.trip.status eq 'PLANNED'}">
      <form method="post" action="${ctx}/shipments/trips/assign">
        <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
        <input type="hidden" name="id" value="${detail.trip.id}">

        <div class="form-grid">
          <div class="form-group">
            <label for="driverId">Tài xế *</label>
            <select class="form-control" id="driverId" name="driverId" required>
              <option value="">Chọn tài xế</option>
              <c:forEach var="driver" items="${drivers}">
                <option value="${driver.id}"
                  ${detail.trip.driverId eq driver.id ? 'selected="selected"' : ''}>
                  <c:out value="${driver.fullName}"/> -
                  <c:out value="${driver.phone}"/>
                </option>
              </c:forEach>
            </select>
          </div>

          <div class="form-group">
            <label for="vehicleId">Phương tiện *</label>
            <select class="form-control" id="vehicleId" name="vehicleId" required>
              <option value="">Chọn phương tiện</option>
              <c:forEach var="vehicle" items="${vehicles}">
                <option value="${vehicle.id}"
                  ${detail.trip.vehicleId eq vehicle.id ? 'selected="selected"' : ''}>
                  <c:out value="${vehicle.licensePlate}"/> -
                  <c:out value="${vehicle.vehicleType}"/>
                </option>
              </c:forEach>
            </select>
          </div>
        </div>

        <button class="btn btn--primary" type="submit">
          Lưu phân công
        </button>
      </form>
    </c:when>
    <c:otherwise>
      <p class="text-muted">
        Chỉ chuyến đang lập kế hoạch mới được thay đổi tài xế và phương tiện.
      </p>
    </c:otherwise>
  </c:choose>
</section>

<section class="card list-card">
  <div class="list-summary">
    <span class="list-summary__title">Danh sách điểm giao theo thứ tự tuyến</span>
    <span class="badge badge--primary">
      <c:out value="${detail.stops.size()}"/>
    </span>
  </div>

  <div class="table-wrap list-table">
    <table class="table">
      <thead>
        <tr>
          <th>Thứ tự</th>
          <th>Phiếu xuất</th>
          <th>Khách hàng</th>
          <th>Địa chỉ giao</th>
          <th>Người nhận</th>
          <th>Số điện thoại</th>
          <th>Ghi chú tuyến</th>
        </tr>
      </thead>
      <tbody>
        <c:if test="${empty detail.stops}">
          <tr>
            <td colspan="7" class="text-muted">
              Chuyến giao chưa có điểm giao.
            </td>
          </tr>
        </c:if>

        <c:forEach var="s" items="${detail.stops}">
          <tr>
            <td><c:out value="${s.stopOrder}"/></td>
            <td><c:out value="${s.shipmentCode}"/></td>
            <td><c:out value="${s.customerName}"/></td>
            <td><c:out value="${s.address}"/></td>
            <td><c:out value="${s.receiverName}"/></td>
            <td><c:out value="${s.receiverPhone}"/></td>
            <td><c:out value="${s.routeNote}"/></td>
          </tr>
        </c:forEach>
      </tbody>
    </table>
  </div>
</section>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>