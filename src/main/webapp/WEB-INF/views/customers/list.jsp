<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Đại lý"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

        <div class="page-header">
          <div>
            <p class="breadcrumb">Đại lý &amp; hạn mức công nợ</p>
            <h1>Đại lý</h1>
            <p class="page-header__desc">Danh sách khách hàng chuẩn của công ty: nhóm giá, khu vực và người phụ trách.</p>
          </div>
          <c:if test="${canManage}">
            <div class="page-header__actions">
              <a class="btn btn--primary" href="${ctx}/customers/new"><span class="icon icon--sm icon--plus" aria-hidden="true"></span>Thêm đại lý</a>
            </div>
          </c:if>
        </div>

        <section class="card list-card">
          <div class="list-summary">
            <span class="list-summary__title">Tất cả đại lý</span>
            <span class="badge badge--primary"><c:out value="${result.total}"/></span>
          </div>

          <div class="table-wrap list-table">
            <table class="table">
              <thead>
                <tr>
                  <th scope="col" class="col-sku">Mã</th>
                  <th scope="col">Đại lý</th>
                  <th scope="col">Nhóm khách hàng</th>
                  <th scope="col">Khu vực</th>
                  <th scope="col">Người phụ trách</th>
                  <th scope="col" class="col-status">Trạng thái</th>
                  <th scope="col" class="col-menu"><span class="visually-hidden">Thao tác</span></th>
                </tr>
              </thead>
              <tbody>
                <c:if test="${empty result.items}">
                  <tr><td colspan="7" class="text-muted">Chưa có đại lý nào.</td></tr>
                </c:if>
                <c:forEach var="cu" items="${result.items}">
                  <tr>
                    <td class="nowrap table__cell-code"><c:out value="${cu.code}"/></td>
                    <td>
                      <a class="table__cell-main" href="${ctx}/customers/view?id=${cu.id}"><c:out value="${cu.name}"/></a>
                      <c:if test="${not empty cu.phone}"><div class="table__cell-sub"><c:out value="${cu.phone}"/></div></c:if>
                    </td>
                    <td><c:out value="${empty cu.groupName ? '—' : cu.groupName}"/></td>
                    <td class="text-muted"><c:out value="${empty cu.areaName ? '—' : cu.areaName}"/></td>
                    <td class="text-muted"><c:out value="${empty cu.salesRepName ? 'Chưa phân công' : cu.salesRepName}"/></td>
                    <td><span class="badge ${cu.locked ? 'badge--danger' : 'badge--success'}"><c:out value="${cu.status.label}"/></span></td>
                    <td>
                      <details class="row-menu">
                        <summary aria-label="Thao tác với <c:out value='${cu.code}'/>"><span class="icon icon--md icon--ellipsis" aria-hidden="true"></span></summary>
                        <div class="row-menu__panel">
                          <a href="${ctx}/customers/view?id=${cu.id}">Xem chi tiết</a>
                          <c:if test="${canManage}">
                            <a href="${ctx}/customers/edit?id=${cu.id}">Sửa</a>
                            <button type="button" class="is-danger" data-dialog-open="delete-${cu.id}">Xoá</button>
                          </c:if>
                        </div>
                      </details>
                    </td>
                  </tr>
                </c:forEach>
              </tbody>
            </table>
          </div>

          <%-- Điện thoại: mỗi đại lý một thẻ --%>
          <div class="product-cards">
            <c:if test="${empty result.items}">
              <p class="text-muted">Chưa có đại lý nào.</p>
            </c:if>
            <c:forEach var="cu" items="${result.items}">
              <article class="product-card">
                <div class="product-card__head">
                  <div class="product-card__title">
                    <a class="product-card__name" href="${ctx}/customers/view?id=${cu.id}"><c:out value="${cu.name}"/></a>
                    <p class="product-card__sku"><c:out value="${cu.code}"/><c:if test="${not empty cu.phone}"><c:out value=" · ${cu.phone}"/></c:if></p>
                  </div>
                  <details class="row-menu">
                    <summary aria-label="Thao tác với <c:out value='${cu.code}'/>"><span class="icon icon--md icon--ellipsis" aria-hidden="true"></span></summary>
                    <div class="row-menu__panel">
                      <a href="${ctx}/customers/view?id=${cu.id}">Xem chi tiết</a>
                      <c:if test="${canManage}">
                        <a href="${ctx}/customers/edit?id=${cu.id}">Sửa</a>
                        <button type="button" class="is-danger" data-dialog-open="delete-${cu.id}">Xoá</button>
                      </c:if>
                    </div>
                  </details>
                </div>
                <div class="product-card__meta">
                  <p><c:out value="${empty cu.groupName ? '—' : cu.groupName}"/> · <c:out value="${empty cu.areaName ? 'Chưa có khu vực' : cu.areaName}"/></p>
                  <p>Phụ trách: <c:out value="${empty cu.salesRepName ? 'chưa phân công' : cu.salesRepName}"/></p>
                </div>
                <div class="product-card__foot">
                  <span class="badge ${cu.locked ? 'badge--danger' : 'badge--success'}"><c:out value="${cu.status.label}"/></span>
                </div>
              </article>
            </c:forEach>
          </div>

          <c:if test="${canManage}">
            <c:forEach var="cu" items="${result.items}">
              <dialog class="dialog" id="delete-${cu.id}" aria-labelledby="delete-title-${cu.id}">
                <form action="${ctx}/customers/delete" method="post">
                  <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                  <input type="hidden" name="id" value="${cu.id}">
                  <div class="dialog__header">
                    <h2 id="delete-title-${cu.id}">Xoá đại lý <c:out value="${cu.code}"/>?</h2>
                  </div>
                  <div class="dialog__body">
                    <p>Chỉ xoá được đại lý chưa phát sinh đơn hàng. Đại lý đã có giao dịch hãy khoá giao dịch thay vì xoá.</p>
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
            <span class="text-muted">Hiển thị ${result.from}–${result.to} trên ${result.total} đại lý</span>
            <div class="pagination__pages">
              <a class="pagination__page ${result.page <= 1 ? 'is-disabled' : ''}" href="${ctx}/customers?page=${result.page - 1}" rel="prev">Trước</a>
              <c:forEach var="pg" begin="1" end="${result.totalPages}">
                <a class="pagination__page ${pg == result.page ? 'is-current' : ''}" href="${ctx}/customers?page=${pg}" ${pg == result.page ? 'aria-current="page"' : ''}>${pg}</a>
              </c:forEach>
              <a class="pagination__page ${result.page >= result.totalPages ? 'is-disabled' : ''}" href="${ctx}/customers?page=${result.page + 1}" rel="next">Sau</a>
            </div>
          </nav>
        </section>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
