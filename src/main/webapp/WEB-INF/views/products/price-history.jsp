<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="pageTitle" value="Lịch sử giá ${product.sku}"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

        <div class="page-header">
          <div>
            <p class="breadcrumb"><a href="${ctx}/products">Sản phẩm &amp; bảng giá</a> / Lịch sử giá</p>
            <h1>Lịch sử giá</h1>
            <p class="page-header__desc"><c:out value="${product.sku} · ${product.name}"/> — giá tính theo đơn vị cơ sở (<c:out value="${product.baseUnitName}"/>).</p>
          </div>
          <div class="page-header__actions">
            <a class="btn btn--secondary" href="${ctx}/products"><span class="icon icon--sm icon--arrow-left" aria-hidden="true"></span>Quay lại danh sách</a>
          </div>
        </div>

        <section class="card list-card">
          <div class="list-summary">
            <span class="list-summary__title">Các lần đổi giá</span>
            <span class="badge badge--primary"><c:out value="${changes.size()}"/></span>
          </div>
          <p class="text-muted price-history__note"><span class="icon icon--xs icon--lock-keyhole" aria-hidden="true"></span> Lịch sử chỉ được thêm, không sửa hay xoá được.</p>

          <div class="table-wrap list-table">
            <table class="table">
              <thead>
                <tr>
                  <th scope="col">Sửa lúc</th>
                  <th scope="col">Bảng giá</th>
                  <th scope="col">Thay đổi</th>
                  <th scope="col" class="num">Giá bán</th>
                  <th scope="col" class="num">Giá sàn</th>
                  <th scope="col">Áp dụng từ</th>
                  <th scope="col">Người sửa</th>
                </tr>
              </thead>
              <tbody>
                <c:if test="${empty changes}">
                  <tr><td colspan="7" class="text-muted">Sản phẩm này chưa có lần đổi giá nào được ghi lại.</td></tr>
                </c:if>
                <c:forEach var="ch" items="${changes}">
                  <tr>
                    <td class="nowrap"><c:out value="${ch.changedAtText}"/></td>
                    <td>
                      <div class="table__cell-main"><c:out value="${ch.priceListCode} · phiên bản ${ch.priceListVersion}"/></div>
                      <div class="table__cell-sub"><c:out value="${ch.groupName}"/></div>
                    </td>
                    <td><span class="badge ${ch.type == 'REMOVE' ? 'badge--danger' : ch.type == 'ADD' ? 'badge--success' : 'badge--primary'}"><c:out value="${ch.type.label}"/></span></td>
                    <td class="num nowrap">
                      <span class="price-history__old"><c:choose><c:when test="${empty ch.oldSalePrice}">—</c:when><c:otherwise><fmt:formatNumber value="${ch.oldSalePrice}" maxFractionDigits="2"/></c:otherwise></c:choose></span>
                      →
                      <strong><c:choose><c:when test="${empty ch.newSalePrice}">—</c:when><c:otherwise><fmt:formatNumber value="${ch.newSalePrice}" maxFractionDigits="2"/> ₫</c:otherwise></c:choose></strong>
                    </td>
                    <td class="num nowrap">
                      <span class="price-history__old"><c:choose><c:when test="${empty ch.oldFloorPrice}">—</c:when><c:otherwise><fmt:formatNumber value="${ch.oldFloorPrice}" maxFractionDigits="2"/></c:otherwise></c:choose></span>
                      →
                      <strong><c:choose><c:when test="${empty ch.newFloorPrice}">—</c:when><c:otherwise><fmt:formatNumber value="${ch.newFloorPrice}" maxFractionDigits="2"/> ₫</c:otherwise></c:choose></strong>
                    </td>
                    <td class="nowrap"><c:out value="${ch.effectiveText}"/></td>
                    <td class="text-muted"><c:out value="${ch.changedByName}"/></td>
                  </tr>
                </c:forEach>
              </tbody>
            </table>
          </div>

          <%-- Điện thoại: mỗi lần đổi giá một thẻ --%>
          <div class="product-cards">
            <c:if test="${empty changes}">
              <p class="text-muted">Sản phẩm này chưa có lần đổi giá nào được ghi lại.</p>
            </c:if>
            <c:forEach var="ch" items="${changes}">
              <article class="product-card">
                <div class="product-card__head">
                  <div class="product-card__title">
                    <p class="product-card__name"><c:out value="${ch.type.label}"/></p>
                    <p class="product-card__sku"><c:out value="${ch.priceListCode} · phiên bản ${ch.priceListVersion} · ${ch.groupName}"/></p>
                  </div>
                </div>
                <div class="product-card__meta">
                  <p>Giá bán:
                    <c:choose><c:when test="${empty ch.oldSalePrice}">—</c:when><c:otherwise><fmt:formatNumber value="${ch.oldSalePrice}" maxFractionDigits="2"/></c:otherwise></c:choose>
                    → <strong><c:choose><c:when test="${empty ch.newSalePrice}">—</c:when><c:otherwise><fmt:formatNumber value="${ch.newSalePrice}" maxFractionDigits="2"/> ₫</c:otherwise></c:choose></strong></p>
                  <p>Giá sàn:
                    <c:choose><c:when test="${empty ch.oldFloorPrice}">—</c:when><c:otherwise><fmt:formatNumber value="${ch.oldFloorPrice}" maxFractionDigits="2"/></c:otherwise></c:choose>
                    → <strong><c:choose><c:when test="${empty ch.newFloorPrice}">—</c:when><c:otherwise><fmt:formatNumber value="${ch.newFloorPrice}" maxFractionDigits="2"/> ₫</c:otherwise></c:choose></strong></p>
                  <p>Áp dụng từ <c:out value="${ch.effectiveText}"/></p>
                </div>
                <div class="product-card__foot">
                  <span class="text-muted price-history__who"><c:out value="${ch.changedByName} · ${ch.changedAtText}"/></span>
                </div>
              </article>
            </c:forEach>
          </div>
        </section>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
