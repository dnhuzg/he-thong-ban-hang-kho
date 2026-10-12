<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Nhóm hàng"/>
<c:set var="catalogTab" value="categories"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

        <div class="page-header">
          <div>
            <p class="breadcrumb">Sản phẩm &amp; bảng giá</p>
            <h1>Nhóm hàng</h1>
            <p class="page-header__desc">Cây nhóm hàng nhiều cấp để phân loại sản phẩm; số lượng tính cả sản phẩm ở các nhóm con.</p>
          </div>
          <c:if test="${canWrite}">
            <button class="btn btn--primary" type="button" data-dialog-open="add-root">+ Thêm nhóm gốc</button>
          </c:if>
        </div>

<%@ include file="/WEB-INF/views/products/nav.jspf" %>

        <section class="card">
          <div class="table-wrap">
            <%-- Chạy trước khi trình duyệt vẽ các dòng: có nhóm đang thu gọn thì tạm che thân bảng cho tới khi app.js áp trạng thái,
                 tránh cảnh nhóm con hiện ra rồi co lại sau mỗi lần gửi form --%>
            <script>try{if(JSON.parse(localStorage.getItem("oms.categoryTree.collapsed")||"[]").length){document.documentElement.classList.add("tree-pending");}}catch(e){}</script>
            <table class="table" data-tree>
              <thead>
                <tr>
                  <th scope="col">Nhóm hàng</th>
                  <th scope="col" class="num col-count">Sản phẩm trực tiếp</th>
                  <th scope="col" class="num col-count">Tổng (gồm nhóm con)</th>
                  <c:if test="${canWrite}"><th scope="col"><span class="visually-hidden">Thao tác</span></th></c:if>
                </tr>
              </thead>
              <tbody>
                <c:if test="${empty categories}">
                  <tr><td colspan="4" class="text-muted">Chưa có nhóm hàng nào.</td></tr>
                </c:if>
                <c:forEach var="cat" items="${categories}">
                  <tr data-cat-id="${cat.id}" data-parent-id="${cat.parentId}">
                    <td class="tree-l${cat.level > 6 ? 6 : cat.level}">
                      <c:if test="${cat.hasChildren}">
                        <button class="tree-toggle" type="button" data-tree-toggle aria-expanded="true"
                                aria-label="Thu gọn hoặc mở rộng nhóm <c:out value='${cat.name}'/>"><span class="icon icon--sm icon--chevron-down" aria-hidden="true"></span></button>
                      </c:if>
                      <span class="table__cell-main"><c:out value="${cat.name}"/></span>
                      <span class="table__cell-sub">Cấp ${cat.level}</span>
                    </td>
                    <td class="num">
                      <c:url var="filterUrl" value="/products"><c:param name="category" value="${cat.id}"/></c:url>
                      <a href="${filterUrl}">${cat.productCount}</a>
                    </td>
                    <td class="num">${cat.totalProductCount}</td>
                    <c:if test="${canWrite}">
                      <td>
                        <div class="table__actions table__actions--slots">
                          <button class="btn btn--ghost btn--sm" type="button" data-cat-dialog="cat-add" data-cat-id="${cat.id}" data-cat-name="<c:out value='${cat.name}'/>">+ Nhóm con</button>
                          <button class="btn btn--ghost btn--sm" type="button" data-cat-dialog="cat-rename" data-cat-id="${cat.id}" data-cat-name="<c:out value='${cat.name}'/>">Đổi tên</button>
                          <c:if test="${cat.productCount > 0}">
                            <button class="btn btn--ghost btn--sm" type="button" data-cat-dialog="cat-move" data-cat-id="${cat.id}" data-cat-name="<c:out value='${cat.name}'/>" data-cat-count="${cat.productCount}">Chuyển SP</button>
                          </c:if>
                          <c:if test="${cat.productCount == 0 and not cat.hasChildren}">
                            <button class="btn btn--ghost btn--sm" type="button" data-cat-dialog="cat-delete" data-cat-id="${cat.id}" data-cat-name="<c:out value='${cat.name}'/>">Xoá</button>
                          </c:if>
                          <c:if test="${cat.productCount == 0 and cat.hasChildren}"><span></span></c:if>
                        </div>
                      </td>
                    </c:if>
                  </tr>
                </c:forEach>
              </tbody>
            </table>
          </div>
        </section>

        <c:if test="${canWrite}">
          <%-- Hộp thoại dùng chung cho mọi dòng: nút data-cat-dialog điền id / tên / số sản phẩm rồi mở (app.js) --%>
          <dialog class="dialog" id="cat-add" aria-labelledby="cat-add-title">
            <form action="${ctx}/products/categories/save" method="post">
              <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
              <input type="hidden" name="parentId" data-cat-field="id">
              <div class="dialog__header"><h2 id="cat-add-title">Thêm nhóm con của "<span data-cat-fill="name"></span>"</h2></div>
              <div class="dialog__body">
                <div class="form-field">
                  <label class="form-label" for="cat-add-name">Tên nhóm <span class="required">*</span></label>
                  <input class="input" id="cat-add-name" name="name" maxlength="150" required>
                </div>
              </div>
              <div class="dialog__footer">
                <button class="btn btn--secondary" type="button" data-dialog-close>Huỷ</button>
                <button class="btn btn--primary" type="submit">Thêm</button>
              </div>
            </form>
          </dialog>

          <dialog class="dialog" id="cat-rename" aria-labelledby="cat-rename-title">
            <form action="${ctx}/products/categories/save" method="post">
              <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
              <input type="hidden" name="id" data-cat-field="id">
              <div class="dialog__header"><h2 id="cat-rename-title">Đổi tên nhóm hàng</h2></div>
              <div class="dialog__body">
                <div class="form-field">
                  <label class="form-label" for="cat-rename-name">Tên mới <span class="required">*</span></label>
                  <input class="input" id="cat-rename-name" name="name" maxlength="150" required data-cat-field="name">
                </div>
              </div>
              <div class="dialog__footer">
                <button class="btn btn--secondary" type="button" data-dialog-close>Huỷ</button>
                <button class="btn btn--primary" type="submit">Lưu</button>
              </div>
            </form>
          </dialog>

          <dialog class="dialog" id="cat-move" aria-labelledby="cat-move-title">
            <form action="${ctx}/products/categories/move" method="post">
              <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
              <input type="hidden" name="id" data-cat-field="id">
              <div class="dialog__header"><h2 id="cat-move-title">Chuyển <span data-cat-fill="count"></span> sản phẩm của "<span data-cat-fill="name"></span>"</h2></div>
              <div class="dialog__body">
                <div class="form-field">
                  <label class="form-label" for="cat-move-target">Sang nhóm <span class="required">*</span></label>
                  <select class="select" id="cat-move-target" name="targetId" required>
                    <option value="">— Chọn nhóm đích —</option>
                    <c:forEach var="t" items="${categories}">
                      <option value="${t.id}"><c:forEach begin="2" end="${t.level}">— </c:forEach><c:out value="${t.name}"/></option>
                    </c:forEach>
                  </select>
                </div>
              </div>
              <div class="dialog__footer">
                <button class="btn btn--secondary" type="button" data-dialog-close>Huỷ</button>
                <button class="btn btn--primary" type="submit">Chuyển</button>
              </div>
            </form>
          </dialog>

          <dialog class="dialog" id="cat-delete" aria-labelledby="cat-delete-title">
            <form action="${ctx}/products/categories/delete" method="post">
              <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
              <input type="hidden" name="id" data-cat-field="id">
              <div class="dialog__header"><h2 id="cat-delete-title">Xoá nhóm "<span data-cat-fill="name"></span>"?</h2></div>
              <div class="dialog__footer">
                <button class="btn btn--secondary" type="button" data-dialog-close>Huỷ</button>
                <button class="btn btn--danger" type="submit">Xoá</button>
              </div>
            </form>
          </dialog>

          <dialog class="dialog" id="add-root" aria-labelledby="add-root-title">
            <form action="${ctx}/products/categories/save" method="post">
              <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
              <div class="dialog__header"><h2 id="add-root-title">Thêm nhóm gốc</h2></div>
              <div class="dialog__body">
                <div class="form-field">
                  <label class="form-label" for="add-root-name">Tên nhóm <span class="required">*</span></label>
                  <input class="input" id="add-root-name" name="name" maxlength="150" required>
                </div>
              </div>
              <div class="dialog__footer">
                <button class="btn btn--secondary" type="button" data-dialog-close>Huỷ</button>
                <button class="btn btn--primary" type="submit">Thêm</button>
              </div>
            </form>
          </dialog>
        </c:if>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
