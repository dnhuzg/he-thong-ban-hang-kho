<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Phân tuyến giao hàng"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

<div class="page-header">
  <div>
    <p class="breadcrumb">Xuất kho &amp; giao hàng</p>
    <h1>Phân tuyến giao hàng và gán tài xế</h1>
    <p class="page-header__desc">
      Gom nhiều phiếu xuất cùng kho, cùng khu vực vào một chuyến giao.
    </p>
  </div>
</div>

<section class="card list-card">
  <div class="list-summary">
    <span class="list-summary__title">Lập chuyến giao hàng</span>
  </div>

  <form method="post" action="${ctx}/shipments/trips/create">
    <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">

    <div class="form-grid">
      <div class="form-group">
        <label for="deliveryDate">Ngày giao hàng *</label>
        <input class="form-control" type="date"
               id="deliveryDate" name="deliveryDate" required>
      </div>

      <div class="form-group">
        <label for="driverId">Tài xế</label>
        <select class="form-control" id="driverId" name="driverId">
          <option value="">Chọn sau</option>
          <c:forEach var="driver" items="${page.drivers}">
            <option value="${driver.id}">
              <c:out value="${driver.fullName}"/>
              - <c:out value="${driver.phone}"/>
            </option>
          </c:forEach>
        </select>
      </div>

      <div class="form-group">
        <label for="vehicleId">Phương tiện</label>
        <select class="form-control" id="vehicleId" name="vehicleId">
          <option value="">Chọn sau</option>
          <c:forEach var="vehicle" items="${page.vehicles}">
            <option value="${vehicle.id}">
              <c:out value="${vehicle.licensePlate}"/>
              - <c:out value="${vehicle.vehicleType}"/>
            </option>
          </c:forEach>
        </select>
      </div>
    </div>

    <div class="list-summary">
      <span class="list-summary__title">Phiếu xuất chờ phân tuyến</span>
      <span class="badge badge--primary">
        <c:out value="${page.availableShipments.size()}"/>
      </span>
    </div>

    <p class="text-muted">
      Chọn các phiếu có cùng kho và cùng khu vực để lập một chuyến.
    </p>

    <div class="table-wrap list-table">
      <table class="table">
        <thead>
          <tr>
            <th>Chọn</th>
            <th>Mã phiếu</th>
            <th>Kho</th>
            <th>Khu vực</th>
            <th>Khách hàng</th>
            <th>Địa chỉ giao</th>
            <th>Người nhận</th>
            <th>Số điện thoại</th>
          </tr>
        </thead>
        <tbody>
          <c:if test="${empty page.availableShipments}">
            <tr>
              <td colspan="8" class="text-muted">
                Không có phiếu xuất sẵn sàng để phân tuyến.
              </td>
            </tr>
          </c:if>

          <c:forEach var="s" items="${page.availableShipments}">
            <tr>
              <td>
                <input type="checkbox" name="shipmentId"
                       value="${s.shipmentId}"
                       aria-label="Chọn phiếu ${s.shipmentCode}">
              </td>
              <td><c:out value="${s.shipmentCode}"/></td>
              <td><c:out value="${s.warehouseName}"/></td>
              <td><c:out value="${s.areaName}"/></td>
              <td><c:out value="${s.customerName}"/></td>
              <td><c:out value="${s.address}"/></td>
              <td><c:out value="${s.receiverName}"/></td>
              <td><c:out value="${s.receiverPhone}"/></td>
            </tr>
          </c:forEach>
        </tbody>
      </table>
    </div>

    <c:if test="${not empty page.availableShipments}">
      <button class="btn btn--primary" type="submit">
        Lập chuyến từ các phiếu đã chọn
      </button>
    </c:if>
  </form>
</section>

<section class="card list-card">
  <div class="list-summary">
    <span class="list-summary__title">Danh sách chuyến giao hàng</span>
  </div>

  <div class="table-wrap list-table">
    <table class="table">
      <thead>
        <tr>
          <th>Mã chuyến</th>
          <th>Kho</th>
          <th>Khu vực</th>
          <th>Ngày giao</th>
          <th>Tài xế</th>
          <th>Phương tiện</th>
          <th>Trạng thái</th>
          <th>Thao tác</th>
        </tr>
      </thead>
      <tbody>
        <c:if test="${empty page.trips}">
          <tr>
            <td colspan="8" class="text-muted">
              Chưa có chuyến giao hàng nào.
            </td>
          </tr>
        </c:if>

        <c:forEach var="t" items="${page.trips}">
          <tr>
            <td><c:out value="${t.code}"/></td>
            <td><c:out value="${t.warehouseName}"/></td>
            <td><c:out value="${t.areaName}"/></td>
            <td><c:out value="${t.deliveryDate}"/></td>
            <td><c:out value="${empty t.driverName ? 'Chưa gán' : t.driverName}"/></td>
            <td><c:out value="${empty t.licensePlate ? 'Chưa gán' : t.licensePlate}"/></td>
            <td><c:out value="${t.status}"/></td>
            <td>
              <a class="btn btn--sm"
                 href="${ctx}/shipments/trips/view?id=${t.id}">
                Chi tiết
              </a>
              <a class="btn btn--sm"
                 href="${ctx}/shipments/trips/print?id=${t.id}"
                 target="_blank" rel="noopener">
                In PDF
              </a>
            </td>
          </tr>
        </c:forEach>
      </tbody>
    </table>
  </div>
</section>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>