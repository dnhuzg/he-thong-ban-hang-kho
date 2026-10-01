<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Người dùng"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

        <div class="page-header">
          <div>
            <p class="breadcrumb">Quản trị</p>
            <h1>Người dùng</h1>
            <p class="page-header__desc">Quản lý tài khoản, vai trò và phạm vi truy cập.</p>
          </div>
          <c:if test="${canWrite}">
            <div class="page-header__actions">
              <a class="btn btn--secondary" href="${ctx}/admin/users/import"><span class="icon icon--sm icon--upload" aria-hidden="true"></span>Nhập từ Excel</a>
              <a class="btn btn--primary" href="${ctx}/admin/users/new"><span class="icon icon--sm icon--plus" aria-hidden="true"></span>Tạo tài khoản</a>
            </div>
          </c:if>
        </div>

        <section class="card list-card">
          <form class="toolbar" action="${ctx}/admin/users" method="get" role="search">
            <label class="visually-hidden" for="q">Tìm người dùng</label>
            <div class="search-field toolbar__search">
              <span class="icon icon--search" aria-hidden="true"></span>
              <input id="q" name="q" type="search" placeholder="Tìm theo tên, tài khoản, số điện thoại" value="<c:out value='${q}'/>">
            </div>

            <label class="visually-hidden" for="role">Vai trò</label>
            <select class="select" id="role" name="role" data-autosubmit>
              <option value="">Tất cả vai trò</option>
              <c:forEach var="r" items="${roles}">
                <option value="${r.code}" ${r.code == selectedRole ? 'selected' : ''}><c:out value="${r.label}"/></option>
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

          <div class="list-summary">
            <span class="list-summary__title">Tất cả người dùng</span>
            <span class="badge badge--primary"><c:out value="${result.total}"/></span>
          </div>

          <div class="table-wrap list-table">
            <table class="table">
              <thead>
                <tr>
                  <th scope="col">Người dùng</th>
                  <th scope="col">Số điện thoại</th>
                  <th scope="col">Vai trò</th>
                  <th scope="col">Kho / Địa bàn</th>
                  <th scope="col">Trạng thái</th>
                  <c:if test="${canWrite}"><th scope="col" class="col-menu"><span class="visually-hidden">Thao tác</span></th></c:if>
                </tr>
              </thead>
              <tbody>
                <c:if test="${empty result.items}">
                  <tr><td colspan="6" class="text-muted">Không có người dùng nào khớp điều kiện tìm kiếm.</td></tr>
                </c:if>
                <c:forEach var="u" items="${result.items}">
                  <tr>
                    <td>
                      <div class="user-cell">
                        <c:choose>
                          <c:when test="${not empty u.avatarThumbPath}">
                            <img class="avatar" src="${ctx}/files/<c:out value='${u.avatarThumbPath}'/>" alt="" width="40" height="40">
                          </c:when>
                          <c:otherwise><span class="avatar" aria-hidden="true"><c:out value="${u.initials}"/></span></c:otherwise>
                        </c:choose>
                        <div>
                          <div class="table__cell-main"><c:out value="${u.fullName}"/></div>
                          <div class="table__cell-sub"><c:out value="${u.username}"/></div>
                        </div>
                      </div>
                    </td>
                    <td><c:out value="${empty u.phone ? '—' : u.phone}"/></td>
                    <td>
                      <div class="badge-list">
                        <c:forEach var="r" items="${u.roles}">
                          <span class="badge badge--primary"><c:out value="${r.label}"/></span>
                        </c:forEach>
                      </div>
                    </td>
                    <td><c:out value="${u.scopeLabel}"/></td>
                    <td>
                      <c:choose>
                        <c:when test="${u.status == 'ACTIVE'}"><span class="badge badge--success"><c:out value="${u.status.label}"/></span></c:when>
                        <c:when test="${u.status == 'PENDING'}"><span class="badge badge--warning"><c:out value="${u.status.label}"/></span></c:when>
                        <c:otherwise><span class="badge badge--danger"><c:out value="${u.status.label}"/></span></c:otherwise>
                      </c:choose>
                    </td>
                    <c:if test="${canWrite}">
                      <td>
                        <details class="row-menu">
                          <summary aria-label="Thao tác với <c:out value='${u.username}'/>"><span class="icon icon--md icon--ellipsis" aria-hidden="true"></span></summary>
                          <div class="row-menu__panel">
                            <a href="${ctx}/admin/users/edit?id=${u.id}">Sửa</a>
                            <c:if test="${u.id != currentUser.id}">
                              <c:choose>
                                <c:when test="${u.status == 'LOCKED'}">
                                  <button type="button" data-dialog-open="unlock-${u.id}">Mở khoá</button>
                                </c:when>
                                <c:otherwise>
                                  <button type="button" class="is-danger" data-dialog-open="lock-${u.id}">Khoá</button>
                                </c:otherwise>
                              </c:choose>
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

          <%-- Điện thoại: mỗi người dùng một thẻ --%>
          <div class="product-cards">
            <c:if test="${empty result.items}">
              <p class="text-muted">Không có người dùng nào khớp điều kiện tìm kiếm.</p>
            </c:if>
            <c:forEach var="u" items="${result.items}">
              <article class="product-card">
                <div class="product-card__head">
                  <c:choose>
                    <c:when test="${not empty u.avatarThumbPath}">
                      <img class="avatar" src="${ctx}/files/<c:out value='${u.avatarThumbPath}'/>" alt="" width="40" height="40">
                    </c:when>
                    <c:otherwise><span class="avatar" aria-hidden="true"><c:out value="${u.initials}"/></span></c:otherwise>
                  </c:choose>
                  <div class="product-card__title">
                    <p class="product-card__name"><c:out value="${u.fullName}"/></p>
                    <p class="product-card__sku"><c:out value="${u.username}"/><c:if test="${not empty u.phone}"><c:out value=" · ${u.phone}"/></c:if></p>
                  </div>
                  <c:if test="${canWrite}">
                    <details class="row-menu">
                      <summary aria-label="Thao tác với <c:out value='${u.username}'/>"><span class="icon icon--md icon--ellipsis" aria-hidden="true"></span></summary>
                      <div class="row-menu__panel">
                        <a href="${ctx}/admin/users/edit?id=${u.id}">Sửa</a>
                        <c:if test="${u.id != currentUser.id}">
                          <c:choose>
                            <c:when test="${u.status == 'LOCKED'}">
                              <button type="button" data-dialog-open="unlock-${u.id}">Mở khoá</button>
                            </c:when>
                            <c:otherwise>
                              <button type="button" class="is-danger" data-dialog-open="lock-${u.id}">Khoá</button>
                            </c:otherwise>
                          </c:choose>
                        </c:if>
                      </div>
                    </details>
                  </c:if>
                </div>
                <div class="product-card__meta">
                  <div class="badge-list">
                    <c:forEach var="r" items="${u.roles}">
                      <span class="badge badge--primary"><c:out value="${r.label}"/></span>
                    </c:forEach>
                  </div>
                  <p><c:out value="${u.scopeLabel}"/></p>
                </div>
                <div class="product-card__foot">
                  <c:choose>
                    <c:when test="${u.status == 'ACTIVE'}"><span class="badge badge--success"><c:out value="${u.status.label}"/></span></c:when>
                    <c:when test="${u.status == 'PENDING'}"><span class="badge badge--warning"><c:out value="${u.status.label}"/></span></c:when>
                    <c:otherwise><span class="badge badge--danger"><c:out value="${u.status.label}"/></span></c:otherwise>
                  </c:choose>
                </div>
              </article>
            </c:forEach>
          </div>

          <c:if test="${canWrite}">
            <c:forEach var="u" items="${result.items}">
              <c:if test="${u.id != currentUser.id}">
                <%-- Khoá: bắt buộc lý do, thu hồi phiên, nhắc bàn giao đại lý (S1-10) --%>
                <dialog class="dialog" id="lock-${u.id}" aria-labelledby="lock-title-${u.id}">
                  <form action="${ctx}/admin/users/lock" method="post">
                    <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                    <input type="hidden" name="id" value="${u.id}">
                    <div class="dialog__header">
                      <h2 id="lock-title-${u.id}">Khoá tài khoản <c:out value="${u.fullName}"/></h2>
                    </div>
                    <div class="dialog__body">
                      <c:set var="customerCount" value="${customerCounts[u.id]}"/>
                      <c:if test="${customerCount > 0}">
                        <div class="alert alert--warning" role="note">Nhân viên này đang phụ trách <strong>${customerCount} đại lý</strong>. Sau khi khoá, các đại lý này được đánh dấu cần bàn giao cho người khác.</div>
                      </c:if>
                      <div class="form-field">
                        <label class="form-label" for="reason-${u.id}">Lý do khoá <span class="required">*</span></label>
                        <textarea class="textarea" id="reason-${u.id}" name="reason" maxlength="500" required></textarea>
                        <p class="form-hint">Tài khoản sẽ không đăng nhập được và mọi phiên đang mở bị thu hồi ngay.</p>
                      </div>
                    </div>
                    <div class="dialog__footer">
                      <button class="btn btn--secondary" type="button" data-dialog-close>Huỷ</button>
                      <button class="btn btn--danger" type="submit">Khoá tài khoản</button>
                    </div>
                  </form>
                </dialog>

                <dialog class="dialog" id="unlock-${u.id}" aria-labelledby="unlock-title-${u.id}">
                  <form action="${ctx}/admin/users/unlock" method="post">
                    <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                    <input type="hidden" name="id" value="${u.id}">
                    <div class="dialog__header">
                      <h2 id="unlock-title-${u.id}">Mở khoá tài khoản <c:out value="${u.fullName}"/></h2>
                    </div>
                    <div class="dialog__body">
                      <p>Người dùng sẽ đăng nhập lại được với vai trò và phạm vi hiện tại.</p>
                      <c:if test="${not empty u.lockReason}">
                        <p class="form-hint mt-4">Lý do khoá trước đó: <c:out value="${u.lockReason}"/></p>
                      </c:if>
                    </div>
                    <div class="dialog__footer">
                      <button class="btn btn--secondary" type="button" data-dialog-close>Huỷ</button>
                      <button class="btn btn--primary" type="submit">Mở khoá</button>
                    </div>
                  </form>
                </dialog>
              </c:if>
            </c:forEach>
          </c:if>

          <%-- 20 dòng / trang (S1-08) --%>
          <nav class="pagination" aria-label="Phân trang">
            <span class="text-muted">Hiển thị ${result.from}–${result.to} trên ${result.total} người dùng</span>
            <div class="pagination__pages">
              <c:url var="prevUrl" value="/admin/users">
                <c:param name="q" value="${q}"/>
                <c:param name="role" value="${selectedRole}"/>
                <c:param name="status" value="${selectedStatus}"/>
                <c:param name="page" value="${result.page - 1}"/>
              </c:url>
              <c:url var="nextUrl" value="/admin/users">
                <c:param name="q" value="${q}"/>
                <c:param name="role" value="${selectedRole}"/>
                <c:param name="status" value="${selectedStatus}"/>
                <c:param name="page" value="${result.page + 1}"/>
              </c:url>
              <a class="pagination__page ${result.page <= 1 ? 'is-disabled' : ''}" href="${prevUrl}" rel="prev">Trước</a>
              <c:forEach var="p" begin="1" end="${result.totalPages}">
                <c:url var="pageUrl" value="/admin/users">
                  <c:param name="q" value="${q}"/>
                  <c:param name="role" value="${selectedRole}"/>
                  <c:param name="status" value="${selectedStatus}"/>
                  <c:param name="page" value="${p}"/>
                </c:url>
                <a class="pagination__page ${p == result.page ? 'is-current' : ''}" href="${pageUrl}" ${p == result.page ? 'aria-current="page"' : ''}>${p}</a>
              </c:forEach>
              <a class="pagination__page ${result.page >= result.totalPages ? 'is-disabled' : ''}" href="${nextUrl}" rel="next">Sau</a>
            </div>
          </nav>
        </section>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
