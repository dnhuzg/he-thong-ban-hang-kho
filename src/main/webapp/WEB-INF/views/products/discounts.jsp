<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Chiết khấu"/>
<c:set var="catalogTab" value="discounts"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

        <div class="page-header">
          <div>
            <p class="breadcrumb">Sản phẩm &amp; bảng giá</p>
            <h1>Chiết khấu theo sản lượng</h1>
            <p class="page-header__desc">Đại lý mua càng nhiều, giá càng giảm: khai báo bậc theo số lượng cho một sản phẩm hoặc một nhóm hàng.</p>
          </div>
          <div class="page-header__actions">
            <a class="btn btn--primary" href="${ctx}/products/discounts/new"><span class="icon icon--sm icon--plus" aria-hidden="true"></span>Tạo chính sách</a>
          </div>
        </div>

<%@ include file="/WEB-INF/views/products/nav.jspf" %>

        <div class="alert alert--info alert--icon" role="note">
          <span class="icon icon--sm icon--info" aria-hidden="true"></span>
          <span>
            <strong>Quy tắc áp dụng:</strong> số lượng tính theo đơn vị cơ sở, các dòng cùng sản phẩm trong một đơn được cộng lại.
            Mỗi chính sách lấy bậc cao nhất mà số lượng đạt tới. Nhiều chính sách cùng áp dụng cho một sản phẩm thì hệ thống
            chọn chính sách <strong>có lợi nhất cho khách</strong> (giảm nhiều nhất trên mỗi đơn vị), không cộng dồn.
          </span>
        </div>

        <section class="card list-card">
          <div class="list-summary">
            <span class="list-summary__title">Các chính sách chiết khấu</span>
            <span class="badge badge--primary"><c:out value="${policies.size()}"/></span>
          </div>

          <div class="table-wrap list-table">
            <table class="table">
              <thead>
                <tr>
                  <th scope="col">Chính sách</th>
                  <th scope="col">Áp dụng cho</th>
                  <th scope="col">Các bậc</th>
                  <th scope="col">Hiệu lực</th>
                  <th scope="col" class="col-status">Trạng thái</th>
                  <th scope="col" class="col-menu"><span class="visually-hidden">Thao tác</span></th>
                </tr>
              </thead>
              <tbody>
                <c:if test="${empty policies}">
                  <tr><td colspan="6" class="text-muted">Chưa có chính sách chiết khấu nào.</td></tr>
                </c:if>
                <c:forEach var="d" items="${policies}">
                  <tr>
                    <td>
                      <a class="table__cell-main" href="${ctx}/products/discounts/edit?id=${d.id}"><c:out value="${d.name}"/></a>
                      <div class="table__cell-sub"><c:out value="${d.type.label}"/></div>
                    </td>
                    <td>
                      <div class="table__cell-main"><c:out value="${d.scopeKind}"/></div>
                      <div class="table__cell-sub"><c:out value="${d.scopeText}"/></div>
                    </td>
                    <td><c:out value="${d.tiersText}"/></td>
                    <td class="nowrap"><c:out value="${d.validityText}"/></td>
                    <td><span class="badge ${d.stateLabel == 'Đang áp dụng' ? 'badge--success' : ''}"><c:out value="${d.stateLabel}"/></span></td>
                    <td>
                      <details class="row-menu">
                        <summary aria-label="Thao tác với <c:out value='${d.name}'/>"><span class="icon icon--md icon--ellipsis" aria-hidden="true"></span></summary>
                        <div class="row-menu__panel">
                          <a href="${ctx}/products/discounts/edit?id=${d.id}">Sửa</a>
                          <button type="button" class="is-danger" data-dialog-open="delete-${d.id}">Xoá</button>
                        </div>
                      </details>
                    </td>
                  </tr>
                </c:forEach>
              </tbody>
            </table>
          </div>

          <%-- Điện thoại: mỗi chính sách một thẻ --%>
          <div class="product-cards">
            <c:if test="${empty policies}">
              <p class="text-muted">Chưa có chính sách chiết khấu nào.</p>
            </c:if>
            <c:forEach var="d" items="${policies}">
              <article class="product-card">
                <div class="product-card__head">
                  <div class="product-card__title">
                    <a class="product-card__name" href="${ctx}/products/discounts/edit?id=${d.id}"><c:out value="${d.name}"/></a>
                    <p class="product-card__sku"><c:out value="${d.scopeKind}: ${d.scopeText}"/></p>
                  </div>
                  <details class="row-menu">
                    <summary aria-label="Thao tác với <c:out value='${d.name}'/>"><span class="icon icon--md icon--ellipsis" aria-hidden="true"></span></summary>
                    <div class="row-menu__panel">
                      <a href="${ctx}/products/discounts/edit?id=${d.id}">Sửa</a>
                      <button type="button" class="is-danger" data-dialog-open="delete-${d.id}">Xoá</button>
                    </div>
                  </details>
                </div>
                <div class="product-card__meta">
                  <p><c:out value="${d.type.label}"/></p>
                  <p><c:out value="${d.tiersText}"/></p>
                  <p><c:out value="${d.validityText}"/></p>
                </div>
                <div class="product-card__foot">
                  <span class="badge ${d.stateLabel == 'Đang áp dụng' ? 'badge--success' : ''}"><c:out value="${d.stateLabel}"/></span>
                </div>
              </article>
            </c:forEach>
          </div>

          <c:forEach var="d" items="${policies}">
            <dialog class="dialog" id="delete-${d.id}" aria-labelledby="delete-title-${d.id}">
              <form action="${ctx}/products/discounts/delete" method="post">
                <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                <input type="hidden" name="id" value="${d.id}">
                <div class="dialog__header">
                  <h2 id="delete-title-${d.id}">Xoá chính sách "<c:out value="${d.name}"/>"?</h2>
                </div>
                <div class="dialog__body">
                  <p>Đơn mới sẽ không còn được chiết khấu theo chính sách này. Nếu chỉ muốn dừng tạm thời, hãy chuyển trạng thái sang Tạm dừng.</p>
                </div>
                <div class="dialog__footer">
                  <button class="btn btn--secondary" type="button" data-dialog-close>Huỷ</button>
                  <button class="btn btn--danger" type="submit">Xoá</button>
                </div>
              </form>
            </dialog>
          </c:forEach>
        </section>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
