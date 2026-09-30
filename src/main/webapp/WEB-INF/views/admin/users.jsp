<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Người dùng"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

        <div class="page-header">
          <div>
            <p class="breadcrumb">Quản trị</p>
            <h1>Người dùng</h1>
          </div>
          <c:if test="${canWrite}">
            <a class="btn btn--primary" href="${ctx}/admin/users/new">+ Tạo tài khoản</a>
          </c:if>
        </div>

        <section class="card">
          <form class="toolbar" action="${ctx}/admin/users" method="get" role="search">
            <label class="visually-hidden" for="q">Tìm người dùng</label>
            <input class="input toolbar__search" id="q" name="q" type="search" placeholder="Tìm theo tên, tài khoản, số điện thoại"
                   value="<c:out value='${q}'/>">

            <label class="visually-hidden" for="role">Vai trò</label>
            <select class="select" id="role" name="role">
              <option value="">Tất cả vai trò</option>
              <c:forEach var="r" items="${roles}">
                <option value="${r.code}" ${r.code == selectedRole ? 'selected' : ''}><c:out value="${r.label}"/></option>
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
                  <th scope="col">Người dùng</th>
                  <th scope="col">Số điện thoại</th>
                  <th scope="col">Vai trò</th>
                  <th scope="col">Kho / Địa bàn</th>
                  <th scope="col">Trạng thái</th>
                  <c:if test="${canWrite}"><th scope="col"><span class="visually-hidden">Thao tác</span></th></c:if>
                </tr>
              </thead>
              <tbody>
                <c:if test="${empty result.items}">
                  <tr><td colspan="6" class="text-muted">Không có người dùng nào khớp điều kiện tìm kiếm.</td></tr>
                </c:if>
                <c:forEach var="u" items="${result.items}">
                  <tr>
                    <td>
                      <div class="table__cell-main"><c:out value="${u.fullName}"/></div>
                      <div class="table__cell-sub"><c:out value="${u.username}"/></div>
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
                        <div class="table__actions">
                          <a class="btn btn--ghost btn--sm" href="${ctx}/admin/users/edit?id=${u.id}">Sửa</a>
                          <c:if test="${u.id != currentUser.id}">
                            <c:choose>
                              <c:when test="${u.status == 'LOCKED'}">
                                <button class="btn btn--ghost btn--sm" type="button" data-dialog-open="unlock-${u.id}">Mở khoá</button>
                              </c:when>
                              <c:otherwise>
                                <button class="btn btn--ghost btn--sm" type="button" data-dialog-open="lock-${u.id}">Khoá</button>
                              </c:otherwise>
                            </c:choose>
                          </c:if>
                        </div>

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
                      </td>
                    </c:if>
                  </tr>
                </c:forEach>
              </tbody>
            </table>
          </div>

          <%-- 20 dòng / trang (S1-08) --%>
          <nav class="pagination" aria-label="Phân trang">
            <span class="text-muted">Hiển thị ${result.from}–${result.to} trên ${result.total} người dùng</span>
            <div class="pagination__pages">
              <c:forEach var="p" begin="1" end="${result.totalPages}">
                <c:url var="pageUrl" value="/admin/users">
                  <c:param name="q" value="${q}"/>
                  <c:param name="role" value="${selectedRole}"/>
                  <c:param name="status" value="${selectedStatus}"/>
                  <c:param name="page" value="${p}"/>
                </c:url>
                <c:choose>
                  <c:when test="${p == result.page}">
                    <a class="pagination__page is-current" href="${pageUrl}" aria-current="page">${p}</a>
                  </c:when>
                  <c:otherwise>
                    <a class="pagination__page" href="${pageUrl}">${p}</a>
                  </c:otherwise>
                </c:choose>
              </c:forEach>
            </div>
          </nav>
        </section>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
