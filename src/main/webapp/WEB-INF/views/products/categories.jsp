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
                          <button class="btn btn--ghost btn--sm" type="button" data-dialog-open="add-${cat.id}">+ Nhóm con</button>
                          <button class="btn btn--ghost btn--sm" type="button" data-dialog-open="rename-${cat.id}">Đổi tên</button>
                          <c:if test="${cat.productCount > 0}">
                            <button class="btn btn--ghost btn--sm" type="button" data-dialog-open="move-${cat.id}">Chuyển SP</button>
                          </c:if>
                          <c:if test="${cat.productCount == 0 and not cat.hasChildren}">
                            <button class="btn btn--ghost btn--sm" type="button" data-dialog-open="delete-${cat.id}">Xoá</button>
                          </c:if>
                          <c:if test="${cat.productCount == 0 and cat.hasChildren}"><span></span></c:if>
                        </div>

                        <dialog class="dialog" id="add-${cat.id}" aria-labelledby="add-title-${cat.id}">
                          <form action="${ctx}/products/categories/save" method="post">
                            <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                            <input type="hidden" name="parentId" value="${cat.id}">
                            <div class="dialog__header"><h2 id="add-title-${cat.id}">Thêm nhóm con của "<c:out value="${cat.name}"/>"</h2></div>
                            <div class="dialog__body">
                              <div class="form-field">
                                <label class="form-label" for="add-name-${cat.id}">Tên nhóm <span class="required">*</span></label>
                                <input class="input" id="add-name-${cat.id}" name="name" maxlength="150" required>
                              </div>
                            </div>
                            <div class="dialog__footer">
                              <button class="btn btn--secondary" type="button" data-dialog-close>Huỷ</button>
                              <button class="btn btn--primary" type="submit">Thêm</button>
                            </div>
                          </form>
                        </dialog>

                        <dialog class="dialog" id="rename-${cat.id}" aria-labelledby="rename-title-${cat.id}">
                          <form action="${ctx}/products/categories/save" method="post">
                            <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                            <input type="hidden" name="id" value="${cat.id}">
                            <div class="dialog__header"><h2 id="rename-title-${cat.id}">Đổi tên nhóm hàng</h2></div>
                            <div class="dialog__body">
                              <div class="form-field">
                                <label class="form-label" for="rename-name-${cat.id}">Tên mới <span class="required">*</span></label>
                                <input class="input" id="rename-name-${cat.id}" name="name" maxlength="150" required value="<c:out value='${cat.name}'/>">
                              </div>
                            </div>
                            <div class="dialog__footer">
                              <button class="btn btn--secondary" type="button" data-dialog-close>Huỷ</button>
                              <button class="btn btn--primary" type="submit">Lưu</button>
                            </div>
                          </form>
                        </dialog>

                        <c:if test="${cat.productCount > 0}">
                          <dialog class="dialog" id="move-${cat.id}" aria-labelledby="move-title-${cat.id}">
                            <form action="${ctx}/products/categories/move" method="post">
                              <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                              <input type="hidden" name="id" value="${cat.id}">
                              <div class="dialog__header"><h2 id="move-title-${cat.id}">Chuyển ${cat.productCount} sản phẩm của "<c:out value="${cat.name}"/>"</h2></div>
                              <div class="dialog__body">
                                <div class="form-field">
                                  <label class="form-label" for="move-target-${cat.id}">Sang nhóm <span class="required">*</span></label>
                                  <select class="select" id="move-target-${cat.id}" name="targetId" required>
                                    <option value="">— Chọn nhóm đích —</option>
                                    <c:forEach var="t" items="${categories}">
                                      <c:if test="${t.id != cat.id}">
                                        <option value="${t.id}"><c:forEach begin="2" end="${t.level}">— </c:forEach><c:out value="${t.name}"/></option>
                                      </c:if>
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
                        </c:if>

                        <c:if test="${cat.productCount == 0 and not cat.hasChildren}">
                          <dialog class="dialog" id="delete-${cat.id}" aria-labelledby="delete-title-${cat.id}">
                            <form action="${ctx}/products/categories/delete" method="post">
                              <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                              <input type="hidden" name="id" value="${cat.id}">
                              <div class="dialog__header"><h2 id="delete-title-${cat.id}">Xoá nhóm "<c:out value="${cat.name}"/>"?</h2></div>
                              <div class="dialog__footer">
                                <button class="btn btn--secondary" type="button" data-dialog-close>Huỷ</button>
                                <button class="btn btn--danger" type="submit">Xoá</button>
                              </div>
                            </form>
                          </dialog>
                        </c:if>
                      </td>
                    </c:if>
                  </tr>
                </c:forEach>
              </tbody>
            </table>
          </div>
        </section>

        <c:if test="${canWrite}">
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
