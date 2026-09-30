<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="pageTitle" value="Sản phẩm"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

        <div class="page-header">
          <div>
            <p class="breadcrumb">Sản phẩm &amp; bảng giá</p>
            <h1>Sản phẩm</h1>
          </div>
          <c:if test="${canWrite}">
            <a class="btn btn--primary" href="${ctx}/products/new">+ Thêm sản phẩm</a>
          </c:if>
        </div>

        <section class="card">
          <form class="toolbar" action="${ctx}/products" method="get" role="search">
            <label class="visually-hidden" for="q">Tìm sản phẩm</label>
            <input class="input toolbar__search" id="q" name="q" type="search" placeholder="Tìm theo mã SKU hoặc tên"
                   value="<c:out value='${q}'/>">

            <label class="visually-hidden" for="category">Nhóm hàng</label>
            <select class="select" id="category" name="category">
              <option value="">Tất cả nhóm hàng</option>
              <c:forEach var="cat" items="${categories}">
                <option value="${cat.id}" ${cat.id == selectedCategory ? 'selected' : ''}><c:forEach begin="2" end="${cat.level}">— </c:forEach><c:out value="${cat.name}"/></option>
              </c:forEach>
            </select>

            <label class="visually-hidden" for="status">Trạng thái</label>
            <select class="select" id="status" name="status">
              <option value="">Tất cả trạng thái</option>
              <c:forEach var="s" items="${statuses}">
                <option value="${s.code}" ${s.code == selectedStatus ? 'selected' : ''}><c:out value="${s.label}"/></option>
              </c:forEach>
            </select>

            <button class="btn btn--secondary" type="submit">Lọc</button>
          </form>

          <div class="table-wrap">
            <table class="table">
              <thead>
                <tr>
                  <th scope="col"><span class="visually-hidden">Ảnh</span></th>
                  <th scope="col">SKU</th>
                  <th scope="col">Sản phẩm</th>
                  <th scope="col">Nhóm hàng</th>
                  <th scope="col">ĐVT cơ sở</th>
                  <c:if test="${canViewCostPrice}"><th scope="col" class="num">Giá vốn</th></c:if>
                  <th scope="col">Trạng thái</th>
                  <c:if test="${canWrite}"><th scope="col"><span class="visually-hidden">Thao tác</span></th></c:if>
                </tr>
              </thead>
              <tbody>
                <c:if test="${empty result.items}">
                  <tr><td colspan="8" class="text-muted">Không có sản phẩm nào khớp điều kiện tìm kiếm.</td></tr>
                </c:if>
                <c:forEach var="p" items="${result.items}">
                  <tr>
                    <td>
                      <c:choose>
                        <c:when test="${not empty p.imageThumbPath}">
                          <img class="product-thumb" src="${ctx}/files/<c:out value='${p.imageThumbPath}'/>" alt="" width="40" height="40">
                        </c:when>
                        <c:otherwise><span class="product-thumb product-thumb--empty" aria-hidden="true"></span></c:otherwise>
                      </c:choose>
                    </td>
                    <td class="nowrap"><c:out value="${p.sku}"/></td>
                    <td>
                      <div class="table__cell-main"><c:out value="${p.name}"/></div>
                      <c:if test="${not empty p.packingSpec}"><div class="table__cell-sub"><c:out value="${p.packingSpec}"/></div></c:if>
                    </td>
                    <td><c:out value="${p.categoryPath}"/></td>
                    <td><c:out value="${p.baseUnitName}"/></td>
                    <c:if test="${canViewCostPrice}">
                      <td class="num nowrap">
                        <c:choose>
                          <c:when test="${empty p.costPrice}">—</c:when>
                          <c:otherwise><fmt:formatNumber value="${p.costPrice}" maxFractionDigits="2"/> ₫</c:otherwise>
                        </c:choose>
                      </td>
                    </c:if>
                    <td>
                      <span class="badge ${p.status == 'ACTIVE' ? 'badge--success' : ''}"><c:out value="${p.status.label}"/></span>
                    </td>
                    <c:if test="${canWrite}">
                      <td>
                        <div class="table__actions">
                          <a class="btn btn--ghost btn--sm" href="${ctx}/products/edit?id=${p.id}">Sửa</a>
                          <button class="btn btn--ghost btn--sm" type="button" data-dialog-open="delete-${p.id}">Xoá</button>
                        </div>
                        <dialog class="dialog" id="delete-${p.id}" aria-labelledby="delete-title-${p.id}">
                          <form action="${ctx}/products/delete" method="post">
                            <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                            <input type="hidden" name="id" value="${p.id}">
                            <div class="dialog__header">
                              <h2 id="delete-title-${p.id}">Xoá sản phẩm <c:out value="${p.sku}"/>?</h2>
                            </div>
                            <div class="dialog__body">
                              <p>Chỉ xoá được sản phẩm chưa phát sinh giao dịch. Sản phẩm đã có giao dịch hãy chuyển sang "Ngừng kinh doanh".</p>
                            </div>
                            <div class="dialog__footer">
                              <button class="btn btn--secondary" type="button" data-dialog-close>Huỷ</button>
                              <button class="btn btn--danger" type="submit">Xoá</button>
                            </div>
                          </form>
                        </dialog>
                      </td>
                    </c:if>
                  </tr>
                </c:forEach>
              </tbody>
            </table>
          </div>

          <nav class="pagination" aria-label="Phân trang">
            <span class="text-muted">Hiển thị ${result.from}–${result.to} trên ${result.total} sản phẩm</span>
            <div class="pagination__pages">
              <c:forEach var="pg" begin="1" end="${result.totalPages}">
                <c:url var="pageUrl" value="/products">
                  <c:param name="q" value="${q}"/>
                  <c:param name="category" value="${selectedCategory > 0 ? selectedCategory : ''}"/>
                  <c:param name="status" value="${selectedStatus}"/>
                  <c:param name="page" value="${pg}"/>
                </c:url>
                <a class="pagination__page ${pg == result.page ? 'is-current' : ''}" href="${pageUrl}" ${pg == result.page ? 'aria-current="page"' : ''}>${pg}</a>
              </c:forEach>
            </div>
          </nav>
        </section>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
