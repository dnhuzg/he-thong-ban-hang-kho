<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Phiếu soạn ${list.code}"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>
<link rel="stylesheet" href="${ctx}/assets/css/picking.css?v=${assetVersion}">

        <div class="page-header">
          <div>
            <p class="breadcrumb"><a href="${ctx}/shipments/picking">Phiếu soạn hàng</a></p>
            <h1>Phiếu soạn <c:out value="${list.code}"/></h1>
            <p class="page-header__desc"><c:out value="${list.warehouseName}"/> · <c:out value="${list.orderCount}"/> đơn · lập bởi <c:out value="${list.createdByName}"/> lúc <c:out value="${list.createdAtText}"/>. Hàng xếp theo nhóm hàng rồi mã SKU.</p>
          </div>
          <span class="badge ${list.open ? 'badge--warning' : 'badge--success'}"><c:out value="${list.open ? 'Đang soạn' : 'Đã hoàn tất'}"/></span>
        </div>

        <section class="card list-card">
          <div class="list-summary">
            <span class="list-summary__title">Đã soạn <c:out value="${list.pickedCount}"/>/<c:out value="${list.lineCount}"/> dòng</span>
            <c:if test="${list.open and list.allPicked}">
              <form method="post" action="${ctx}/shipments/picking/complete">
                <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                <input type="hidden" name="id" value="${list.id}">
                <button class="btn btn--primary btn--sm" type="submit">Hoàn tất phiếu soạn</button>
              </form>
            </c:if>
            <c:if test="${not list.open}">
              <a class="btn btn--secondary btn--sm" href="${ctx}/shipments/export">Sang xuất kho</a>
            </c:if>
          </div>

          <div class="pick-list">
            <c:set var="group" value=""/>
            <c:forEach var="l" items="${list.lines}">
              <c:if test="${l.categoryName ne group}">
                <h2 class="pick-list__group"><c:out value="${l.categoryName}"/></h2>
                <c:set var="group" value="${l.categoryName}"/>
              </c:if>
              <article class="pick-line" id="line-${l.id}">
                <div class="pick-line__head">
                  <div class="pick-line__title">
                    <span class="pick-line__name"><c:out value="${l.productName}"/></span>
                    <p class="pick-line__sku"><c:out value="${l.sku}"/> · đơn <c:out value="${l.orderCode}"/> · <c:out value="${l.customerName}"/></p>
                  </div>
                  <c:choose>
                    <c:when test="${l.shortPicked}"><span class="badge badge--danger">Soạn thiếu</span></c:when>
                    <c:when test="${l.done}"><span class="badge badge--success">Đã soạn</span></c:when>
                    <c:otherwise><span class="badge">Chưa soạn</span></c:otherwise>
                  </c:choose>
                </div>
                <div class="pick-line__meta">
                  <p>Cần soạn <strong><c:out value="${l.required}"/> <c:out value="${l.baseUnit}"/></strong>
                    <c:if test="${l.done}"> · thực soạn <strong><c:out value="${l.picked}"/></strong></c:if></p>
                  <c:if test="${l.shortPicked}"><p>Lý do: <c:out value="${l.shortReason}"/></p></c:if>
                </div>
                <c:if test="${list.open}">
                  <form class="pick-line__foot" method="post" action="${ctx}/shipments/picking/pick">
                    <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                    <input type="hidden" name="id" value="${list.id}">
                    <input type="hidden" name="lineId" value="${l.id}">
                    <label class="form-field">
                      <span class="form-label">Thực soạn (<c:out value="${l.baseUnit}"/>)</span>
                      <input class="input" type="number" name="picked" min="0" max="${l.required}" step="any" inputmode="decimal" required
                             value="<c:out value='${l.done ? l.picked : l.required}'/>">
                    </label>
                    <label class="form-field">
                      <span class="form-label">Lý do nếu soạn thiếu</span>
                      <input class="input" type="text" name="shortReason" maxlength="500" value="<c:out value='${l.shortReason}'/>">
                    </label>
                    <button class="btn ${l.done ? 'btn--secondary' : 'btn--primary'} btn--sm" type="submit"><c:out value="${l.done ? 'Cập nhật' : 'Đã soạn'}"/></button>
                  </form>
                </c:if>
              </article>
            </c:forEach>
          </div>
        </section>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
