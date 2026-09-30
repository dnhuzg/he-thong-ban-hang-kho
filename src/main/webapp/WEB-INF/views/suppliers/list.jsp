<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Nhà cung cấp"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

        <div class="page-header">
          <div>
            <p class="breadcrumb">Tồn kho &amp; nhập kho</p>
            <h1>Nhà cung cấp</h1>
          </div>
          <c:if test="${canWrite}">
            <a class="btn btn--primary" href="${ctx}/suppliers/new">+ Thêm nhà cung cấp</a>
          </c:if>
        </div>

        <section class="card">
          <form class="toolbar" action="${ctx}/suppliers" method="get" role="search">
            <label class="visually-hidden" for="q">Tìm nhà cung cấp</label>
            <input class="input toolbar__search" id="q" name="q" type="search" placeholder="Tìm theo mã, tên hoặc mã số thuế"
                   value="<c:out value='${q}'/>">
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
                  <th scope="col">Nhà cung cấp</th>
                  <th scope="col">Mã số thuế</th>
                  <th scope="col">Người liên hệ</th>
                  <th scope="col">Điều khoản thanh toán</th>
                  <th scope="col">Trạng thái</th>
                  <c:if test="${canWrite}"><th scope="col"><span class="visually-hidden">Thao tác</span></th></c:if>
                </tr>
              </thead>
              <tbody>
                <c:if test="${empty result.items}">
                  <tr><td colspan="6" class="text-muted">Không có nhà cung cấp nào khớp điều kiện tìm kiếm.</td></tr>
                </c:if>
                <c:forEach var="s" items="${result.items}">
                  <tr>
                    <td>
                      <div class="table__cell-main"><c:out value="${s.name}"/></div>
                      <div class="table__cell-sub"><c:out value="${s.code}"/></div>
                    </td>
                    <td class="nowrap"><c:out value="${empty s.taxCode ? '—' : s.taxCode}"/></td>
                    <td>
                      <div><c:out value="${empty s.contactName ? '—' : s.contactName}"/></div>
                      <c:if test="${not empty s.contactPhone}"><div class="table__cell-sub"><c:out value="${s.contactPhone}"/></div></c:if>
                    </td>
                    <td><c:out value="${empty s.paymentTerms ? '—' : s.paymentTerms}"/></td>
                    <td><span class="badge ${s.status == 'ACTIVE' ? 'badge--success' : ''}"><c:out value="${s.status.label}"/></span></td>
                    <c:if test="${canWrite}">
                      <td>
                        <div class="table__actions">
                          <a class="btn btn--ghost btn--sm" href="${ctx}/suppliers/edit?id=${s.id}">Sửa</a>
                          <button class="btn btn--ghost btn--sm" type="button" data-dialog-open="delete-${s.id}">Xoá</button>
                        </div>
                        <dialog class="dialog" id="delete-${s.id}" aria-labelledby="delete-title-${s.id}">
                          <form action="${ctx}/suppliers/delete" method="post">
                            <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                            <input type="hidden" name="id" value="${s.id}">
                            <div class="dialog__header"><h2 id="delete-title-${s.id}">Xoá nhà cung cấp <c:out value="${s.code}"/>?</h2></div>
                            <div class="dialog__body">
                              <p>Chỉ xoá được nhà cung cấp chưa có phiếu nhập. Đã có phiếu nhập thì hãy chuyển sang "Ngừng giao dịch".</p>
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
            <span class="text-muted">Hiển thị ${result.from}–${result.to} trên ${result.total} nhà cung cấp</span>
            <div class="pagination__pages">
              <c:forEach var="pg" begin="1" end="${result.totalPages}">
                <c:url var="pageUrl" value="/suppliers">
                  <c:param name="q" value="${q}"/>
                  <c:param name="status" value="${selectedStatus}"/>
                  <c:param name="page" value="${pg}"/>
                </c:url>
                <a class="pagination__page ${pg == result.page ? 'is-current' : ''}" href="${pageUrl}" ${pg == result.page ? 'aria-current="page"' : ''}>${pg}</a>
              </c:forEach>
            </div>
          </nav>
        </section>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
