<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Xuất kho"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

        <div class="page-header">
          <div>
            <p class="breadcrumb">Xuất kho &amp; giao hàng</p>
            <h1>Xuất kho</h1>
            <p class="page-header__desc">Đơn đã soạn xong chờ xác nhận xuất. Xác nhận sẽ trừ tồn đúng số thực soạn, lập phiếu xuất chờ giao; phần soạn thiếu được ghi nợ hàng để giao lượt sau.</p>
          </div>
        </div>

        <section class="card list-card">
          <div class="list-summary">
            <span class="list-summary__title">Đơn chờ xuất kho</span>
            <span class="badge badge--primary"><c:out value="${export.ready.size()}"/></span>
          </div>
          <div class="table-wrap list-table">
            <table class="table">
              <thead>
                <tr>
                  <th scope="col" class="col-sku">Mã đơn</th>
                  <th scope="col">Đại lý</th>
                  <th scope="col">Phiếu soạn</th>
                  <th scope="col" class="num">Số dòng</th>
                  <th scope="col">Soạn thiếu</th>
                  <th scope="col" class="col-menu"><span class="visually-hidden">Thao tác</span></th>
                </tr>
              </thead>
              <tbody>
                <c:if test="${empty export.ready}">
                  <tr><td colspan="6" class="text-muted">Không có đơn nào đã soạn xong đang chờ xuất.</td></tr>
                </c:if>
                <c:forEach var="r" items="${export.ready}">
                  <tr>
                    <td class="nowrap table__cell-code"><a href="${ctx}/orders/edit?id=${r.orderId}"><c:out value="${r.orderCode}"/></a></td>
                    <td class="table__cell-main"><c:out value="${r.customerName}"/></td>
                    <td class="nowrap"><c:out value="${r.pickingCode}"/></td>
                    <td class="num"><c:out value="${r.lineCount}"/></td>
                    <td><c:choose><c:when test="${r.shortCount > 0}"><span class="badge badge--danger"><c:out value="${r.shortCount}"/> dòng</span></c:when><c:otherwise>—</c:otherwise></c:choose></td>
                    <td class="nowrap">
                      <form method="post" action="${ctx}/shipments/export/confirm">
                        <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                        <input type="hidden" name="orderId" value="${r.orderId}">
                        <button class="btn btn--primary btn--sm" type="submit">Xác nhận xuất</button>
                      </form>
                    </td>
                  </tr>
                </c:forEach>
              </tbody>
            </table>
          </div>
          <div class="product-cards">
            <c:if test="${empty export.ready}">
              <p class="text-muted">Không có đơn nào đã soạn xong đang chờ xuất.</p>
            </c:if>
            <c:forEach var="r" items="${export.ready}">
              <article class="product-card">
                <div class="product-card__head">
                  <div class="product-card__title">
                    <span class="product-card__name"><c:out value="${r.customerName}"/></span>
                    <p class="product-card__sku"><c:out value="${r.orderCode}"/> · <c:out value="${r.pickingCode}"/></p>
                  </div>
                  <c:if test="${r.shortCount > 0}"><span class="badge badge--danger">Thiếu <c:out value="${r.shortCount}"/> dòng</span></c:if>
                </div>
                <form class="product-card__foot" method="post" action="${ctx}/shipments/export/confirm">
                  <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                  <input type="hidden" name="orderId" value="${r.orderId}">
                  <button class="btn btn--primary btn--sm" type="submit">Xác nhận xuất</button>
                </form>
              </article>
            </c:forEach>
          </div>
        </section>

        <section class="card list-card">
          <div class="list-summary">
            <span class="list-summary__title">Đơn còn nợ hàng</span>
            <span class="badge badge--warning"><c:out value="${export.backorders.size()}"/></span>
          </div>
          <div class="table-wrap">
            <table class="table">
              <thead>
                <tr>
                  <th scope="col" class="col-sku">Mã đơn</th>
                  <th scope="col">Đại lý</th>
                  <th scope="col">Sản phẩm</th>
                  <th scope="col" class="num">Còn nợ</th>
                  <th scope="col">Lý do</th>
                  <th scope="col">Ghi nhận lúc</th>
                </tr>
              </thead>
              <tbody>
                <c:if test="${empty export.backorders}">
                  <tr><td colspan="6" class="text-muted">Không có đơn nào còn nợ hàng.</td></tr>
                </c:if>
                <c:forEach var="b" items="${export.backorders}">
                  <tr>
                    <td class="nowrap table__cell-code"><a href="${ctx}/orders/edit?id=${b.orderId}"><c:out value="${b.orderCode}"/></a></td>
                    <td><c:out value="${b.customerName}"/></td>
                    <td><c:out value="${b.sku}"/> · <c:out value="${b.productName}"/></td>
                    <td class="num nowrap"><c:out value="${b.quantity}"/> <c:out value="${b.baseUnit}"/></td>
                    <td><c:out value="${b.reason}"/></td>
                    <td class="nowrap"><c:out value="${b.createdAtText}"/></td>
                  </tr>
                </c:forEach>
              </tbody>
            </table>
          </div>
        </section>

        <section class="card list-card">
          <div class="list-summary">
            <span class="list-summary__title">Phiếu xuất gần đây</span>
          </div>
          <div class="table-wrap">
            <table class="table">
              <thead>
                <tr>
                  <th scope="col" class="col-sku">Mã phiếu</th>
                  <th scope="col">Đơn</th>
                  <th scope="col">Đại lý</th>
                  <th scope="col">Lập lúc</th>
                  <th scope="col">Trạng thái</th>
                </tr>
              </thead>
              <tbody>
                <c:if test="${empty export.recent}">
                  <tr><td colspan="5" class="text-muted">Chưa có phiếu xuất nào.</td></tr>
                </c:if>
                <c:forEach var="s" items="${export.recent}">
                  <tr>
                    <td class="nowrap table__cell-code"><a href="${ctx}/shipments/confirm?id=${s.id}"><c:out value="${s.code}"/></a></td>
                    <td class="nowrap"><c:out value="${s.orderCode}"/></td>
                    <td><c:out value="${s.customerName}"/></td>
                    <td class="nowrap"><c:out value="${s.createdAtText}"/></td>
                    <td><c:out value="${s.status.label}"/></td>
                  </tr>
                </c:forEach>
              </tbody>
            </table>
          </div>
        </section>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
