<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="pageTitle" value="Sản phẩm"/>
<c:set var="catalogTab" value="products"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

        <div class="page-header">
          <div>
            <p class="breadcrumb">Sản phẩm &amp; bảng giá</p>
            <h1>Sản phẩm</h1>
            <p class="page-header__desc">Danh mục sản phẩm và thông tin quy đổi đơn vị.</p>
          </div>
          <c:if test="${canWrite}">
            <div class="page-header__actions">
              <c:if test="${canViewPriceLists}"><a class="btn btn--secondary" href="${ctx}/products/price-lists/new"><span class="icon icon--sm icon--tags" aria-hidden="true"></span>Tạo bảng giá</a></c:if>
              <a class="btn btn--secondary" href="${ctx}/products/import"><span class="icon icon--sm icon--upload" aria-hidden="true"></span>Nhập từ Excel</a>
              <a class="btn btn--primary" href="${ctx}/products/new"><span class="icon icon--sm icon--plus" aria-hidden="true"></span>Thêm sản phẩm</a>
            </div>
          </c:if>
        </div>

<%@ include file="/WEB-INF/views/products/nav.jspf" %>

        <section class="card-grid mb-4" aria-label="Tổng quan sản phẩm">
          <a class="card card__body home-stat home-stat--link" href="${ctx}/products">
            <div class="home-stat__head">
              <p class="stat__label">Tổng số sản phẩm</p>
              <span class="home-stat__icon home-stat__icon--primary"><span class="icon icon--sm icon--package" aria-hidden="true"></span></span>
            </div>
            <p class="home-stat__value"><span class="stat__value"><fmt:formatNumber value="${stats.totalProducts}"/></span> SKU</p>
            <p class="home-stat__note home-stat__note--up">+<fmt:formatNumber value="${stats.newThisMonth}"/> sản phẩm mới tháng này</p>
          </a>
          <a class="card card__body home-stat home-stat--link" href="${ctx}/products?status=ACTIVE">
            <div class="home-stat__head">
              <p class="stat__label">Đang kinh doanh</p>
              <span class="badge badge--success">Đang bán</span>
            </div>
            <p class="home-stat__value"><span class="stat__value"><fmt:formatNumber value="${stats.activeProducts}"/></span> SKU</p>
            <p class="home-stat__note">Ngừng kinh doanh: <fmt:formatNumber value="${stats.discontinuedProducts}"/></p>
          </a>
          <c:choose>
            <c:when test="${canViewPriceLists}">
              <a class="card card__body home-stat home-stat--link" href="${ctx}/products/price-lists">
                <%@ include file="/WEB-INF/views/products/price-list-stat.jspf" %>
              </a>
            </c:when>
            <c:otherwise>
              <div class="card card__body home-stat">
                <%@ include file="/WEB-INF/views/products/price-list-stat.jspf" %>
              </div>
            </c:otherwise>
          </c:choose>
          <div class="card card__body home-stat${stats.activeWithoutPrice > 0 ? ' home-stat--warning' : ''}">
            <div class="home-stat__head">
              <p class="stat__label">Chưa có giá bán</p>
              <span class="home-stat__icon home-stat__icon--warning"><span class="icon icon--sm icon--info" aria-hidden="true"></span></span>
            </div>
            <p class="home-stat__value"><span class="stat__value"><fmt:formatNumber value="${stats.activeWithoutPrice}"/></span> SKU</p>
            <p class="home-stat__note">Đang bán nhưng chưa có trong bảng giá hiệu lực, chưa thêm được vào đơn</p>
          </div>
        </section>

        <section class="card list-card">
          <form class="toolbar" action="${ctx}/products" method="get" role="search">
            <label class="visually-hidden" for="q">Tìm sản phẩm</label>
            <div class="search-field toolbar__search">
              <span class="icon icon--search" aria-hidden="true"></span>
              <input id="q" name="q" type="search" placeholder="Tìm theo mã SKU hoặc tên" value="<c:out value='${q}'/>">
            </div>

            <label class="visually-hidden" for="category">Nhóm hàng</label>
            <select class="select" id="category" name="category" data-autosubmit>
              <option value="">Tất cả nhóm hàng</option>
              <c:forEach var="cat" items="${categories}">
                <option value="${cat.id}" ${cat.id == selectedCategory ? 'selected' : ''}><c:forEach begin="2" end="${cat.level}">— </c:forEach><c:out value="${cat.name}"/></option>
              </c:forEach>
            </select>

            <label class="visually-hidden" for="status">Trạng thái</label>
            <select class="select" id="status" name="status" data-autosubmit>
              <option value="">Tất cả trạng thái</option>
              <c:forEach var="s" items="${statuses}">
                <option value="${s.code}" ${s.code == selectedStatus ? 'selected' : ''}><c:out value="${s.label}"/></option>
              </c:forEach>
            </select>

            <button class="btn btn--secondary toolbar__submit" type="submit">Lọc</button>
          </form>

          <c:if test="${not empty q or selectedCategory > 0 or not empty selectedStatus}">
            <div class="filter-chips">
              <span class="filter-chips__label">Bộ lọc đang áp dụng:</span>
              <c:if test="${not empty q}">
                <c:url var="noQ" value="/products"><c:if test="${selectedCategory > 0}"><c:param name="category" value="${selectedCategory}"/></c:if><c:param name="status" value="${selectedStatus}"/></c:url>
                <a class="filter-chip" href="${noQ}" title="Bỏ bộ lọc này">Từ khoá: <c:out value="${q}"/> <span aria-hidden="true">×</span></a>
              </c:if>
              <c:if test="${selectedCategory > 0}">
                <c:forEach var="cat" items="${categories}"><c:if test="${cat.id == selectedCategory}"><c:set var="selectedCategoryName" value="${cat.name}"/></c:if></c:forEach>
                <c:url var="noCat" value="/products"><c:param name="q" value="${q}"/><c:param name="status" value="${selectedStatus}"/></c:url>
                <a class="filter-chip" href="${noCat}" title="Bỏ bộ lọc này">Nhóm: <c:out value="${selectedCategoryName}"/> <span aria-hidden="true">×</span></a>
              </c:if>
              <c:if test="${not empty selectedStatus}">
                <c:forEach var="st" items="${statuses}"><c:if test="${st.code == selectedStatus}"><c:set var="selectedStatusLabel" value="${st.label}"/></c:if></c:forEach>
                <c:url var="noStatus" value="/products"><c:param name="q" value="${q}"/><c:if test="${selectedCategory > 0}"><c:param name="category" value="${selectedCategory}"/></c:if></c:url>
                <a class="filter-chip" href="${noStatus}" title="Bỏ bộ lọc này">Trạng thái: <c:out value="${selectedStatusLabel}"/> <span aria-hidden="true">×</span></a>
              </c:if>
              <a class="filter-chips__clear" href="${ctx}/products">Xoá tất cả</a>
            </div>
          </c:if>

          <div class="list-summary">
            <span class="list-summary__title">Tất cả sản phẩm</span>
            <span class="badge badge--primary"><c:out value="${result.total}"/></span>
          </div>

          <div class="table-wrap list-table">
            <table class="table">
              <thead>
                <tr>
                  <th scope="col" class="col-sku">SKU</th>
                  <th scope="col">Sản phẩm</th>
                  <th scope="col">Nhóm hàng</th>
                  <th scope="col" class="col-unit">ĐVT cơ sở</th>
                  <th scope="col" class="num">Tồn kho</th>
                  <th scope="col" class="num">Khả dụng</th>
                  <c:if test="${canViewCostPrice}"><th scope="col" class="num col-cost">Giá vốn</th></c:if>
                  <th scope="col" class="col-status">Trạng thái</th>
                  <c:if test="${canWrite or canViewPriceLists}"><th scope="col" class="col-menu"><span class="visually-hidden">Thao tác</span></th></c:if>
                </tr>
              </thead>
              <tbody>
                <c:if test="${empty result.items}">
                  <tr><td colspan="9" class="text-muted">Không có sản phẩm nào khớp điều kiện tìm kiếm.</td></tr>
                </c:if>
                <c:forEach var="p" items="${result.items}">
                  <tr>
                    <td class="nowrap table__cell-code"><c:out value="${p.sku}"/></td>
                    <td>
                      <div class="product-cell">
                        <c:choose>
                          <c:when test="${not empty p.imageThumbPath}">
                            <img class="product-thumb" src="${ctx}/files/<c:out value='${p.imageThumbPath}'/>" alt="" width="40" height="40">
                          </c:when>
                          <c:otherwise><span class="product-thumb product-thumb--icon" aria-hidden="true"><span class="icon icon--md icon--package"></span></span></c:otherwise>
                        </c:choose>
                        <div>
                          <div class="table__cell-main"><c:out value="${p.name}"/></div>
                          <c:if test="${not empty p.packingSpec}"><div class="table__cell-sub"><c:out value="${p.packingSpec}"/></div></c:if>
                          <c:if test="${not empty p.conversions}">
                            <div class="table__cell-hint"><c:forEach var="pu" items="${p.conversions}" varStatus="st"><c:out value="1 ${pu.unitName} = ${pu.factorText} ${p.baseUnitName}${st.last ? '' : ' • '}"/></c:forEach></div>
                          </c:if>
                        </div>
                      </div>
                    </td>
                    <td class="text-muted"><c:out value="${fn:replace(p.categoryPath, ' › ', ' • ')}"/></td>
                    <td><c:out value="${p.baseUnitName}"/></td>
                    <td class="num nowrap"><fmt:formatNumber value="${p.stockOnHand}" maxFractionDigits="2"/></td>
                    <td class="num nowrap ${p.stockAvailable.signum() > 0 ? 'text-success' : 'text-muted'}"><fmt:formatNumber value="${p.stockAvailable}" maxFractionDigits="2"/></td>
                    <c:if test="${canViewCostPrice}">
                      <td class="num nowrap table__cell-main">
                        <c:choose>
                          <c:when test="${empty p.costPrice}">—</c:when>
                          <c:otherwise><fmt:formatNumber value="${p.costPrice}" maxFractionDigits="2"/> ₫</c:otherwise>
                        </c:choose>
                      </td>
                    </c:if>
                    <td>
                      <span class="badge ${p.status == 'ACTIVE' ? 'badge--success' : ''}"><c:out value="${p.status.label}"/></span>
                    </td>
                    <c:if test="${canWrite or canViewPriceLists}">
                      <td>
                        <details class="row-menu">
                          <summary aria-label="Thao tác với <c:out value='${p.sku}'/>"><span class="icon icon--md icon--ellipsis" aria-hidden="true"></span></summary>
                          <div class="row-menu__panel">
                            <c:if test="${canViewPriceLists}"><a href="${ctx}/products/price-history?id=${p.id}">Lịch sử giá</a></c:if>
                            <c:if test="${canWrite}">
                              <a href="${ctx}/products/edit?id=${p.id}">Sửa</a>
                              <button type="button" class="is-danger" data-dialog-open="delete-${p.id}">Xoá</button>
                            </c:if>
                          </div>
                        </details>
                      </td>
                    </c:if>
                  </tr>
                </c:forEach>
              </tbody>
            </table>
          </div>

          <%-- Điện thoại: mỗi sản phẩm một thẻ --%>
          <div class="product-cards">
            <c:if test="${empty result.items}">
              <p class="text-muted">Không có sản phẩm nào khớp điều kiện tìm kiếm.</p>
            </c:if>
            <c:forEach var="p" items="${result.items}">
              <article class="product-card">
                <div class="product-card__head">
                  <c:if test="${not empty p.imageThumbPath}">
                    <img class="product-thumb" src="${ctx}/files/<c:out value='${p.imageThumbPath}'/>" alt="" width="40" height="40">
                  </c:if>
                  <div class="product-card__title">
                    <p class="product-card__name"><c:out value="${p.name}"/></p>
                    <p class="product-card__sku"><c:out value="${p.sku}"/></p>
                  </div>
                  <c:if test="${canWrite or canViewPriceLists}">
                    <details class="row-menu">
                      <summary aria-label="Thao tác với <c:out value='${p.sku}'/>"><span class="icon icon--md icon--ellipsis" aria-hidden="true"></span></summary>
                      <div class="row-menu__panel">
                        <c:if test="${canViewPriceLists}"><a href="${ctx}/products/price-history?id=${p.id}">Lịch sử giá</a></c:if>
                        <c:if test="${canWrite}">
                          <a href="${ctx}/products/edit?id=${p.id}">Sửa</a>
                          <button type="button" class="is-danger" data-dialog-open="delete-${p.id}">Xoá</button>
                        </c:if>
                      </div>
                    </details>
                  </c:if>
                </div>
                <div class="product-card__meta">
                  <p><c:out value="${fn:replace(p.categoryPath, ' › ', ' • ')}"/></p>
                  <p>
                    <c:if test="${not empty p.packingSpec}"><c:out value="${p.packingSpec}"/></c:if>
                    <c:if test="${not empty p.packingSpec and not empty p.conversions}"> · </c:if>
                    <c:forEach var="pu" items="${p.conversions}" varStatus="st"><c:out value="1 ${pu.unitName} = ${pu.factorText} ${p.baseUnitName}${st.last ? '' : ' • '}"/></c:forEach>
                  </p>
                </div>
                <div class="product-card__foot">
                  <c:if test="${canViewCostPrice}">
                    <div>
                      <p class="product-card__label"><c:out value="Giá vốn / ${fn:toLowerCase(p.baseUnitName)}"/></p>
                      <p class="product-card__price">
                        <c:choose>
                          <c:when test="${empty p.costPrice}">—</c:when>
                          <c:otherwise><fmt:formatNumber value="${p.costPrice}" maxFractionDigits="2"/> ₫</c:otherwise>
                        </c:choose>
                      </p>
                    </div>
                  </c:if>
                  <span class="badge ${p.status == 'ACTIVE' ? 'badge--success' : ''}"><c:out value="${p.status.label}"/></span>
                </div>
              </article>
            </c:forEach>
          </div>

          <c:if test="${canWrite}">
            <c:forEach var="p" items="${result.items}">
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
            </c:forEach>
          </c:if>

          <nav class="pagination" aria-label="Phân trang">
            <span class="text-muted">Hiển thị ${result.from}–${result.to} trên ${result.total} sản phẩm</span>
            <div class="pagination__pages">
              <c:url var="prevUrl" value="/products">
                <c:param name="q" value="${q}"/>
                <c:param name="category" value="${selectedCategory > 0 ? selectedCategory : ''}"/>
                <c:param name="status" value="${selectedStatus}"/>
                <c:param name="page" value="${result.page - 1}"/>
              </c:url>
              <c:url var="nextUrl" value="/products">
                <c:param name="q" value="${q}"/>
                <c:param name="category" value="${selectedCategory > 0 ? selectedCategory : ''}"/>
                <c:param name="status" value="${selectedStatus}"/>
                <c:param name="page" value="${result.page + 1}"/>
              </c:url>
              <a class="pagination__page ${result.page <= 1 ? 'is-disabled' : ''}" href="${prevUrl}" rel="prev">Trước</a>
              <c:forEach var="pg" begin="1" end="${result.totalPages}">
                <c:url var="pageUrl" value="/products">
                  <c:param name="q" value="${q}"/>
                  <c:param name="category" value="${selectedCategory > 0 ? selectedCategory : ''}"/>
                  <c:param name="status" value="${selectedStatus}"/>
                  <c:param name="page" value="${pg}"/>
                </c:url>
                <a class="pagination__page ${pg == result.page ? 'is-current' : ''}" href="${pageUrl}" ${pg == result.page ? 'aria-current="page"' : ''}>${pg}</a>
              </c:forEach>
              <a class="pagination__page ${result.page >= result.totalPages ? 'is-disabled' : ''}" href="${nextUrl}" rel="next">Sau</a>
            </div>
          </nav>
        </section>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
