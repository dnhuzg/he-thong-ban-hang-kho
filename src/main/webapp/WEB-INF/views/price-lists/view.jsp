<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="pageTitle" value="${priceList.name}"/>
<c:set var="canEdit" value="${canWrite and priceList.editable}"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

        <div class="page-header">
          <div>
            <p class="breadcrumb"><a href="${ctx}/products/price-lists">Bảng giá</a> / <c:out value="${priceList.code}"/></p>
            <h1><c:out value="${priceList.name}"/></h1>
          </div>
          <div class="badge-list">
            <span class="badge ${priceList.validityLabel == 'Đang áp dụng' ? 'badge--success' : ''}"><c:out value="${priceList.validityLabel}"/></span>
            <c:if test="${priceList.locked}"><span class="badge badge--warning">Đã phát sinh đơn</span></c:if>
          </div>
        </div>

        <c:if test="${canWrite and not priceList.editable}">
          <div class="alert alert--warning" role="status">
            <c:choose>
              <c:when test="${priceList.locked}">Bảng giá đã phát sinh đơn hàng nên không sửa được.</c:when>
              <c:otherwise>Bảng giá đã có phiên bản mới hơn nên không sửa được bản này.</c:otherwise>
            </c:choose>
            <c:if test="${not priceList.hasNewerVersion}">Muốn đổi giá, hãy tạo phiên bản mới ở cuối trang.</c:if>
          </div>
        </c:if>

        <section class="card">
          <div class="card__header"><h2>Thông tin</h2></div>
          <c:choose>
            <c:when test="${canEdit}">
              <form class="card__body form-grid" action="${ctx}/products/price-lists/header" method="post">
                <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                <input type="hidden" name="id" value="${priceList.id}">
                <div class="form-field">
                  <label class="form-label" for="name">Tên bảng giá</label>
                  <input class="input" id="name" name="name" maxlength="150" required value="<c:out value='${priceList.name}'/>">
                </div>
                <div class="form-field">
                  <label class="form-label" for="group">Nhóm khách hàng</label>
                  <select class="select" id="group" name="customerGroupId">
                    <c:forEach var="g" items="${groups}">
                      <option value="${g.id}" ${g.id == priceList.customerGroupId ? 'selected' : ''}><c:out value="${g.name}"/></option>
                    </c:forEach>
                  </select>
                </div>
                <div class="form-field">
                  <label class="form-label" for="from">Hiệu lực từ ngày</label>
                  <input class="input" id="from" name="validFrom" type="date" required value="${priceList.validFrom}">
                </div>
                <div class="form-field">
                  <label class="form-label" for="to">Đến ngày</label>
                  <input class="input" id="to" name="validTo" type="date" value="${priceList.validTo}">
                </div>
                <div class="form-actions form-actions--start">
                  <button class="btn btn--secondary" type="submit">Lưu thông tin</button>
                </div>
              </form>
            </c:when>
            <c:otherwise>
              <dl class="card__body info-list">
                <div><dt>Mã / phiên bản</dt><dd><c:out value="${priceList.code} · phiên bản ${priceList.version}"/></dd></div>
                <div><dt>Nhóm khách hàng</dt><dd><c:out value="${priceList.customerGroupName}"/></dd></div>
                <div><dt>Hiệu lực</dt><dd><c:out value="${priceList.validFromText} → ${priceList.validToText}"/></dd></div>
              </dl>
            </c:otherwise>
          </c:choose>
        </section>

        <section class="card">
          <div class="card__header">
            <h2>Dòng giá (${priceList.items.size()})</h2>
            <p>Giá theo đơn vị cơ sở. Bán dưới giá sàn sẽ phải qua duyệt của Quản lý kinh doanh.</p>
          </div>
          <div class="table-wrap">
            <table class="table">
              <thead>
                <tr>
                  <th scope="col">Sản phẩm</th>
                  <th scope="col" class="num">Giá bán</th>
                  <th scope="col" class="num">Giá sàn</th>
                  <c:if test="${canViewCostPrice}"><th scope="col" class="num">Biên lợi nhuận</th></c:if>
                  <c:if test="${canEdit}"><th scope="col"><span class="visually-hidden">Thao tác</span></th></c:if>
                </tr>
              </thead>
              <tbody>
                <c:if test="${empty priceList.items}">
                  <tr><td colspan="5" class="text-muted">Chưa có dòng giá nào.</td></tr>
                </c:if>
                <c:forEach var="it" items="${priceList.items}">
                  <tr>
                    <td>
                      <div class="table__cell-main"><c:out value="${it.productName}"/></div>
                      <div class="table__cell-sub"><c:out value="${it.sku} · /${it.baseUnitName}"/></div>
                    </td>
                    <c:choose>
                      <c:when test="${canEdit}">
                        <td colspan="2" class="num">
                          <form class="inline-form inline-form--end" action="${ctx}/products/price-lists/items/save" method="post">
                            <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                            <input type="hidden" name="id" value="${priceList.id}">
                            <input type="hidden" name="productId" value="${it.productId}">
                            <label class="visually-hidden" for="sale-${it.productId}">Giá bán</label>
                            <input class="input input--narrow" id="sale-${it.productId}" name="salePrice" inputmode="decimal" value="${it.salePrice.stripTrailingZeros().toPlainString()}">
                            <label class="visually-hidden" for="floor-${it.productId}">Giá sàn</label>
                            <input class="input input--narrow" id="floor-${it.productId}" name="floorPrice" inputmode="decimal" value="${it.floorPrice.stripTrailingZeros().toPlainString()}">
                            <button class="btn btn--secondary btn--sm" type="submit">Lưu</button>
                          </form>
                        </td>
                      </c:when>
                      <c:otherwise>
                        <td class="num nowrap"><fmt:formatNumber value="${it.salePrice}" maxFractionDigits="2"/> ₫</td>
                        <td class="num nowrap"><fmt:formatNumber value="${it.floorPrice}" maxFractionDigits="2"/> ₫</td>
                      </c:otherwise>
                    </c:choose>
                    <c:if test="${canViewCostPrice}">
                      <td class="num nowrap">
                        <c:choose>
                          <c:when test="${empty it.marginPercent}">—</c:when>
                          <c:otherwise><fmt:formatNumber value="${it.marginPercent}" minFractionDigits="1" maxFractionDigits="1"/> %</c:otherwise>
                        </c:choose>
                      </td>
                    </c:if>
                    <c:if test="${canEdit}">
                      <td>
                        <form action="${ctx}/products/price-lists/items/delete" method="post">
                          <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                          <input type="hidden" name="id" value="${priceList.id}">
                          <input type="hidden" name="productId" value="${it.productId}">
                          <button class="btn btn--ghost btn--sm" type="submit">Xoá</button>
                        </form>
                      </td>
                    </c:if>
                  </tr>
                </c:forEach>
              </tbody>
            </table>
          </div>
          <c:if test="${canEdit}">
            <div class="card__body">
              <form class="inline-form" action="${ctx}/products/price-lists/items/save" method="post">
                <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                <input type="hidden" name="id" value="${priceList.id}">
                <label class="visually-hidden" for="new-product">Sản phẩm</label>
                <select class="select" id="new-product" name="productId" required>
                  <option value="">— Chọn sản phẩm —</option>
                  <c:forEach var="p" items="${products}">
                    <option value="${p.id}"><c:out value="${p.name}"/></option>
                  </c:forEach>
                </select>
                <label class="visually-hidden" for="new-sale">Giá bán</label>
                <input class="input input--narrow" id="new-sale" name="salePrice" inputmode="decimal" placeholder="Giá bán" required>
                <label class="visually-hidden" for="new-floor">Giá sàn</label>
                <input class="input input--narrow" id="new-floor" name="floorPrice" inputmode="decimal" placeholder="Giá sàn" required>
                <button class="btn btn--primary btn--sm" type="submit">Thêm / cập nhật dòng giá</button>
              </form>
            </div>
          </c:if>
        </section>

        <section class="card">
          <div class="card__header">
            <h2>Phiên bản</h2>
            <p>Phiên bản mới chép toàn bộ dòng giá; phiên bản cũ tự kết thúc hôm trước ngày bắt đầu của bản mới.</p>
          </div>
          <div class="table-wrap">
            <table class="table">
              <tbody>
                <c:forEach var="v" items="${versions}">
                  <tr>
                    <td>
                      <c:choose>
                        <c:when test="${v.id == priceList.id}"><strong><c:out value="Phiên bản ${v.version} (đang xem)"/></strong></c:when>
                        <c:otherwise><a href="${ctx}/products/price-lists/view?id=${v.id}"><c:out value="Phiên bản ${v.version}"/></a></c:otherwise>
                      </c:choose>
                    </td>
                    <td class="nowrap"><c:out value="${v.validFromText} → ${v.validToText}"/></td>
                    <td><span class="badge ${v.validityLabel == 'Đang áp dụng' ? 'badge--success' : ''}"><c:out value="${v.validityLabel}"/></span></td>
                  </tr>
                </c:forEach>
              </tbody>
            </table>
          </div>
          <c:if test="${canWrite and not priceList.hasNewerVersion}">
            <div class="card__body">
              <form class="inline-form" action="${ctx}/products/price-lists/new-version" method="post">
                <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                <input type="hidden" name="id" value="${priceList.id}">
                <label for="nv-from">Tạo phiên bản mới, hiệu lực từ</label>
                <input class="input input--narrow input--date" id="nv-from" name="validFrom" type="date" required>
                <button class="btn btn--primary btn--sm" type="submit">Tạo phiên bản mới</button>
              </form>
            </div>
          </c:if>
        </section>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
