<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="pageTitle" value="Đơn hàng"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

        <div class="page-header">
          <div>
            <p class="breadcrumb">Bán hàng</p>
            <h1>Đơn hàng</h1>
            <p class="page-header__desc">Đơn nháp đang gõ dở: mở lại để tiếp tục thêm hàng. Chốt đơn, kiểm tồn kho và hạn mức công nợ làm ở bước sau.</p>
          </div>
          <div class="page-header__actions">
            <a class="btn btn--primary" href="${ctx}/orders/new"><span class="icon icon--sm icon--plus" aria-hidden="true"></span>Tạo đơn</a>
          </div>
        </div>

        <section class="card-grid mb-4" aria-label="Tổng quan đơn hàng">
          <div class="card card__body home-stat">
            <div class="home-stat__head">
              <p class="stat__label">Tổng đơn nháp</p>
              <span class="home-stat__icon home-stat__icon--primary"><span class="icon icon--sm icon--clipboard-list" aria-hidden="true"></span></span>
            </div>
            <p class="home-stat__value"><span class="stat__value"><fmt:formatNumber value="${stats.total}"/></span> đơn</p>
            <p class="home-stat__note home-stat__note--up">+<fmt:formatNumber value="${stats.thisMonth}"/> đơn tạo trong tháng này</p>
          </div>
          <div class="card card__body home-stat">
            <div class="home-stat__head">
              <p class="stat__label">Đại lý có đơn</p>
              <span class="home-stat__icon home-stat__icon--primary"><span class="icon icon--sm icon--building-2" aria-hidden="true"></span></span>
            </div>
            <p class="home-stat__value"><span class="stat__value"><fmt:formatNumber value="${stats.customers}"/></span> đại lý</p>
            <p class="home-stat__note">Đang có ít nhất một đơn nháp</p>
          </div>
          <div class="card card__body home-stat">
            <div class="home-stat__head">
              <p class="stat__label">Giao trong 7 ngày tới</p>
              <span class="home-stat__icon home-stat__icon--primary"><span class="icon icon--sm icon--truck" aria-hidden="true"></span></span>
            </div>
            <p class="home-stat__value"><span class="stat__value"><fmt:formatNumber value="${stats.dueSoon}"/></span> đơn</p>
            <p class="home-stat__note">Cần chốt sớm để kịp soạn và giao hàng</p>
          </div>
          <div class="card card__body home-stat${stats.withoutLines > 0 ? ' home-stat--warning' : ''}">
            <div class="home-stat__head">
              <p class="stat__label">Đơn chưa có hàng</p>
              <span class="home-stat__icon home-stat__icon--warning"><span class="icon icon--sm icon--info" aria-hidden="true"></span></span>
            </div>
            <p class="home-stat__value"><span class="stat__value"><fmt:formatNumber value="${stats.withoutLines}"/></span> đơn</p>
            <p class="home-stat__note">Tạo ra nhưng chưa thêm dòng hàng nào: gõ tiếp hoặc xoá</p>
          </div>
        </section>

        <section class="card list-card">
          <form class="toolbar" action="${ctx}/orders" method="get" role="search">
            <label class="visually-hidden" for="q">Tìm đơn</label>
            <div class="search-field toolbar__search">
              <span class="icon icon--search" aria-hidden="true"></span>
              <input id="q" name="q" type="search" placeholder="Tìm theo mã đơn hoặc đại lý" value="<c:out value='${q}'/>">
            </div>
            <button class="btn btn--secondary toolbar__submit toolbar__submit--show" type="submit">Tìm</button>
          </form>

          <c:if test="${not empty q}">
            <div class="filter-chips">
              <span class="filter-chips__label">Bộ lọc đang áp dụng:</span>
              <a class="filter-chip" href="${ctx}/orders" title="Bỏ bộ lọc này">Từ khoá: <c:out value="${q}"/> <span aria-hidden="true">×</span></a>
            </div>
          </c:if>

          <div class="list-summary">
            <span class="list-summary__title">Tất cả đơn</span>
            <span class="badge badge--primary"><c:out value="${result.total}"/></span>
          </div>

          <div class="table-wrap list-table">
            <table class="table">
              <thead>
                <tr>
                  <th scope="col" class="col-sku">Mã đơn</th>
                  <th scope="col">Đại lý</th>
                  <th scope="col">Giao ngày</th>
                  <th scope="col" class="num">Số dòng</th>
                  <th scope="col" class="num">Tổng phải thu</th>
                  <th scope="col">Cập nhật</th>
                  <th scope="col" class="col-status">Trạng thái</th>
                  <th scope="col" class="col-menu"><span class="visually-hidden">Thao tác</span></th>
                </tr>
              </thead>
              <tbody>
                <c:if test="${empty result.items}">
                  <tr><td colspan="8" class="text-muted">Chưa có đơn nào.</td></tr>
                </c:if>
                <c:forEach var="o" items="${result.items}">
                  <tr>
                    <td class="nowrap table__cell-code"><a href="${ctx}/orders/edit?id=${o.id}"><c:out value="${o.code}"/></a></td>
                    <td>
                      <div class="product-cell">
                        <span class="initials-tile" aria-hidden="true"><c:out value="${o.customerInitials}"/></span>
                        <div>
                          <div class="table__cell-main"><c:out value="${o.customerName}"/></div>
                          <div class="table__cell-sub"><c:out value="${o.customerCode}"/><c:if test="${o.customerLocked}"> · Đang bị khoá giao dịch</c:if></div>
                        </div>
                      </div>
                    </td>
                    <td class="nowrap"><c:out value="${empty o.deliveryDateText ? '—' : o.deliveryDateText}"/></td>
                    <td class="num"><c:out value="${o.lineCount}"/></td>
                    <td class="num nowrap table__cell-main"><fmt:formatNumber value="${o.payable}" maxFractionDigits="2"/> ₫</td>
                    <td class="nowrap text-muted"><c:out value="${o.updatedAtText}"/></td>
                    <td><span class="badge">Nháp</span></td>
                    <td>
                      <details class="row-menu">
                        <summary aria-label="Thao tác với <c:out value='${o.code}'/>"><span class="icon icon--md icon--ellipsis" aria-hidden="true"></span></summary>
                        <div class="row-menu__panel">
                          <a href="${ctx}/orders/edit?id=${o.id}">Mở để gõ tiếp</a>
                          <button type="button" class="is-danger" data-dialog-open="delete-${o.id}">Xoá nháp</button>
                        </div>
                      </details>
                    </td>
                  </tr>
                </c:forEach>
              </tbody>
            </table>
          </div>

          <%-- Điện thoại: mỗi đơn một thẻ --%>
          <div class="product-cards">
            <c:if test="${empty result.items}">
              <p class="text-muted">Chưa có đơn nào.</p>
            </c:if>
            <c:forEach var="o" items="${result.items}">
              <article class="product-card">
                <div class="product-card__head">
                  <div class="product-card__title">
                    <a class="product-card__name" href="${ctx}/orders/edit?id=${o.id}"><c:out value="${o.customerName}"/></a>
                    <p class="product-card__sku"><c:out value="${o.code} · ${o.customerCode}"/></p>
                  </div>
                  <details class="row-menu">
                    <summary aria-label="Thao tác với <c:out value='${o.code}'/>"><span class="icon icon--md icon--ellipsis" aria-hidden="true"></span></summary>
                    <div class="row-menu__panel">
                      <a href="${ctx}/orders/edit?id=${o.id}">Mở để gõ tiếp</a>
                      <button type="button" class="is-danger" data-dialog-open="delete-${o.id}">Xoá nháp</button>
                    </div>
                  </details>
                </div>
                <div class="product-card__meta">
                  <p><c:out value="${o.lineCount}"/> dòng hàng · Giao <c:out value="${empty o.deliveryDateText ? 'chưa chọn ngày' : o.deliveryDateText}"/></p>
                  <p>Phải thu <strong><fmt:formatNumber value="${o.payable}" maxFractionDigits="2"/> ₫</strong></p>
                  <c:if test="${o.customerLocked}"><p>Đại lý đang bị khoá giao dịch</p></c:if>
                </div>
                <div class="product-card__foot">
                  <span class="badge">Nháp</span>
                  <span class="text-muted order-card__time"><c:out value="${o.updatedAtText}"/></span>
                </div>
              </article>
            </c:forEach>
          </div>

          <c:forEach var="o" items="${result.items}">
            <dialog class="dialog" id="delete-${o.id}" aria-labelledby="delete-title-${o.id}">
              <form action="${ctx}/orders/delete" method="post">
                <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                <input type="hidden" name="id" value="${o.id}">
                <div class="dialog__header"><h2 id="delete-title-${o.id}">Xoá đơn nháp <c:out value="${o.code}"/>?</h2></div>
                <div class="dialog__body"><p>Các dòng hàng đã gõ sẽ mất, không khôi phục được.</p></div>
                <div class="dialog__footer">
                  <button class="btn btn--secondary" type="button" data-dialog-close>Huỷ</button>
                  <button class="btn btn--danger" type="submit">Xoá</button>
                </div>
              </form>
            </dialog>
          </c:forEach>

          <nav class="pagination" aria-label="Phân trang">
            <span class="text-muted">Hiển thị ${result.from}–${result.to} trên ${result.total} đơn</span>
            <div class="pagination__pages">
              <c:url var="prevUrl" value="/orders"><c:param name="q" value="${q}"/><c:param name="page" value="${result.page - 1}"/></c:url>
              <c:url var="nextUrl" value="/orders"><c:param name="q" value="${q}"/><c:param name="page" value="${result.page + 1}"/></c:url>
              <a class="pagination__page ${result.page <= 1 ? 'is-disabled' : ''}" href="${prevUrl}" rel="prev">Trước</a>
              <c:forEach var="pg" begin="1" end="${result.totalPages}">
                <c:url var="pageUrl" value="/orders"><c:param name="q" value="${q}"/><c:param name="page" value="${pg}"/></c:url>
                <a class="pagination__page ${pg == result.page ? 'is-current' : ''}" href="${pageUrl}" ${pg == result.page ? 'aria-current="page"' : ''}>${pg}</a>
              </c:forEach>
              <a class="pagination__page ${result.page >= result.totalPages ? 'is-disabled' : ''}" href="${nextUrl}" rel="next">Sau</a>
            </div>
          </nav>
        </section>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
