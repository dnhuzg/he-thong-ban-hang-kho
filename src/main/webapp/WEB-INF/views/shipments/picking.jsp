<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Phiếu soạn hàng"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

        <div class="page-header">
          <div>
            <p class="breadcrumb">Xuất kho &amp; giao hàng</p>
            <h1>Phiếu soạn hàng</h1>
            <p class="page-header__desc">Chọn một hoặc nhiều đơn đã duyệt cùng kho để gộp thành một lượt soạn. Đơn được đưa vào phiếu chuyển sang Đang soạn hàng.</p>
          </div>
        </div>

        <section class="card list-card">
          <form method="post" action="${ctx}/shipments/picking/create">
            <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
            <div class="list-summary">
              <span class="list-summary__title">Đơn đã duyệt chờ soạn</span>
              <span class="badge badge--primary"><c:out value="${picking.queue.size()}"/></span>
              <c:if test="${not empty picking.queue}">
                <button class="btn btn--primary btn--sm" type="submit">Lập phiếu soạn từ đơn đã chọn</button>
              </c:if>
            </div>

            <div class="table-wrap list-table">
              <table class="table">
                <thead>
                  <tr>
                    <th scope="col"><span class="visually-hidden">Chọn</span></th>
                    <th scope="col" class="col-sku">Mã đơn</th>
                    <th scope="col">Đại lý</th>
                    <th scope="col">Kho</th>
                    <th scope="col">Ngày giao</th>
                    <th scope="col" class="num">Số dòng</th>
                  </tr>
                </thead>
                <tbody>
                  <c:if test="${empty picking.queue}">
                    <tr><td colspan="6" class="text-muted">Không có đơn đã duyệt nào đang chờ soạn.</td></tr>
                  </c:if>
                  <c:forEach var="o" items="${picking.queue}">
                    <tr>
                      <td><input type="checkbox" name="orderId" value="${o.orderId}" aria-label="Chọn đơn <c:out value='${o.orderCode}'/>"></td>
                      <td class="nowrap table__cell-code"><a href="${ctx}/orders/edit?id=${o.orderId}"><c:out value="${o.orderCode}"/></a></td>
                      <td class="table__cell-main"><c:out value="${o.customerName}"/></td>
                      <td><c:out value="${o.warehouseName}"/></td>
                      <td class="nowrap"><c:out value="${empty o.deliveryDateText ? '—' : o.deliveryDateText}"/></td>
                      <td class="num"><c:out value="${o.lineCount}"/></td>
                    </tr>
                  </c:forEach>
                </tbody>
              </table>
            </div>

            <%-- Điện thoại: mỗi đơn một thẻ --%>
            <div class="product-cards">
              <c:if test="${empty picking.queue}">
                <p class="text-muted">Không có đơn đã duyệt nào đang chờ soạn.</p>
              </c:if>
              <c:forEach var="o" items="${picking.queue}">
                <article class="product-card">
                  <label class="product-card__head">
                    <input type="checkbox" name="orderId" value="${o.orderId}">
                    <span class="product-card__title">
                      <span class="product-card__name"><c:out value="${o.customerName}"/></span>
                      <span class="product-card__sku"><c:out value="${o.orderCode}"/> · <c:out value="${o.warehouseName}"/></span>
                    </span>
                  </label>
                  <div class="product-card__meta">
                    <p>Ngày giao <strong><c:out value="${empty o.deliveryDateText ? '—' : o.deliveryDateText}"/></strong> · <c:out value="${o.lineCount}"/> dòng</p>
                  </div>
                </article>
              </c:forEach>
            </div>
          </form>
        </section>

        <section class="card list-card">
          <div class="list-summary">
            <span class="list-summary__title">Phiếu soạn gần đây</span>
          </div>
          <div class="table-wrap list-table">
            <table class="table">
              <thead>
                <tr>
                  <th scope="col" class="col-sku">Mã phiếu</th>
                  <th scope="col">Kho</th>
                  <th scope="col" class="num">Số đơn</th>
                  <th scope="col" class="num">Đã soạn</th>
                  <th scope="col">Người lập</th>
                  <th scope="col">Trạng thái</th>
                </tr>
              </thead>
              <tbody>
                <c:if test="${empty picking.recent}">
                  <tr><td colspan="6" class="text-muted">Chưa có phiếu soạn nào.</td></tr>
                </c:if>
                <c:forEach var="p" items="${picking.recent}">
                  <tr>
                    <td class="nowrap table__cell-code"><a href="${ctx}/shipments/picking/view?id=${p.id}"><c:out value="${p.code}"/></a></td>
                    <td><c:out value="${p.warehouseName}"/></td>
                    <td class="num"><c:out value="${p.orderCount}"/></td>
                    <td class="num nowrap"><c:out value="${p.pickedCount}"/>/<c:out value="${p.lineCount}"/></td>
                    <td><c:out value="${p.createdByName}"/> · <c:out value="${p.createdAtText}"/></td>
                    <td><span class="badge ${p.open ? 'badge--warning' : 'badge--success'}"><c:out value="${p.open ? 'Đang soạn' : 'Đã hoàn tất'}"/></span></td>
                  </tr>
                </c:forEach>
              </tbody>
            </table>
          </div>
          <div class="product-cards">
            <c:if test="${empty picking.recent}">
              <p class="text-muted">Chưa có phiếu soạn nào.</p>
            </c:if>
            <c:forEach var="p" items="${picking.recent}">
              <article class="product-card">
                <div class="product-card__head">
                  <div class="product-card__title">
                    <a class="product-card__name" href="${ctx}/shipments/picking/view?id=${p.id}"><c:out value="${p.code}"/></a>
                    <p class="product-card__sku"><c:out value="${p.warehouseName}"/> · <c:out value="${p.orderCount}"/> đơn</p>
                  </div>
                  <span class="badge ${p.open ? 'badge--warning' : 'badge--success'}"><c:out value="${p.open ? 'Đang soạn' : 'Đã hoàn tất'}"/></span>
                </div>
                <div class="product-card__meta">
                  <p>Đã soạn <strong><c:out value="${p.pickedCount}"/>/<c:out value="${p.lineCount}"/></strong> dòng</p>
                </div>
              </article>
            </c:forEach>
          </div>
        </section>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
