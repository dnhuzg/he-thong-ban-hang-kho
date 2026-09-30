<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Nhật ký thao tác"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

        <div class="page-header">
          <div>
            <p class="breadcrumb">Quản trị</p>
            <h1>Nhật ký thao tác</h1>
          </div>
        </div>

        <c:if test="${not empty error}">
          <div class="alert alert--danger" role="alert"><c:out value="${error}"/></div>
        </c:if>

        <section class="card">
          <form class="toolbar" action="${ctx}/admin/audit-logs" method="get" role="search">
            <label class="visually-hidden" for="actor">Người thực hiện</label>
            <select class="select" id="actor" name="actor">
              <option value="">Tất cả người thực hiện</option>
              <c:forEach var="a" items="${actors}">
                <option value="${a.id}" ${a.id == selectedActor ? 'selected' : ''}><c:out value="${a.name}"/></option>
              </c:forEach>
            </select>

            <label class="visually-hidden" for="type">Loại đối tượng</label>
            <select class="select" id="type" name="type">
              <option value="">Tất cả đối tượng</option>
              <c:forEach var="t" items="${targetTypes}">
                <option value="${t.key}" ${t.key == selectedType ? 'selected' : ''}><c:out value="${t.value}"/></option>
              </c:forEach>
            </select>

            <label class="toolbar__date">
              <span class="form-hint">Từ ngày</span>
              <input class="input" type="date" name="from" value="<c:out value='${from}'/>">
            </label>
            <label class="toolbar__date">
              <span class="form-hint">Đến ngày</span>
              <input class="input" type="date" name="to" value="<c:out value='${to}'/>">
            </label>

            <button class="btn btn--secondary" type="submit">Lọc</button>
          </form>

          <div class="table-wrap">
            <table class="table">
              <thead>
                <tr>
                  <th scope="col">Thời điểm</th>
                  <th scope="col">Người thực hiện</th>
                  <th scope="col">Thao tác</th>
                  <th scope="col">Đối tượng</th>
                  <th scope="col">Trước</th>
                  <th scope="col">Sau</th>
                </tr>
              </thead>
              <tbody>
                <c:if test="${empty result.items}">
                  <tr><td colspan="6" class="text-muted">Không có thao tác nào khớp điều kiện lọc.</td></tr>
                </c:if>
                <c:forEach var="e" items="${result.items}">
                  <tr>
                    <td class="nowrap"><c:out value="${e.createdAtText}"/></td>
                    <td>
                      <div class="actor">
                        <c:choose>
                          <c:when test="${not empty e.actorAvatarThumbPath}">
                            <img class="avatar avatar--sm" src="${ctx}/files/<c:out value='${e.actorAvatarThumbPath}'/>" alt="" width="28" height="28">
                          </c:when>
                          <c:otherwise>
                            <span class="avatar avatar--sm" aria-hidden="true">?</span>
                          </c:otherwise>
                        </c:choose>
                        <div>
                          <div class="table__cell-main"><c:out value="${empty e.actorName ? 'Hệ thống' : e.actorName}"/></div>
                          <div class="table__cell-sub"><c:out value="${e.actorUsername}"/></div>
                        </div>
                      </div>
                    </td>
                    <td>
                      <div class="table__cell-main"><c:out value="${e.actionLabel}"/></div>
                      <c:if test="${not empty e.detail}"><div class="table__cell-sub"><c:out value="${e.detail}"/></div></c:if>
                    </td>
                    <td>
                      <c:out value="${e.targetLabel}"/>
                      <c:if test="${not empty e.targetName}"><div class="table__cell-sub"><c:out value="${e.targetName}"/></div></c:if>
                    </td>
                    <td class="audit-value"><c:out value="${empty e.oldValue ? '—' : e.oldValue}"/></td>
                    <td class="audit-value"><c:out value="${empty e.newValue ? '—' : e.newValue}"/></td>
                  </tr>
                </c:forEach>
              </tbody>
            </table>
          </div>

          <c:if test="${not empty result}">
            <nav class="pagination" aria-label="Phân trang">
              <span class="text-muted">Hiển thị ${result.from}–${result.to} trên ${result.total} thao tác</span>
              <div class="pagination__pages">
                <c:forEach var="p" begin="${result.page > 3 ? result.page - 2 : 1}" end="${result.page + 2 < result.totalPages ? result.page + 2 : result.totalPages}">
                  <c:url var="pageUrl" value="/admin/audit-logs">
                    <c:param name="actor" value="${selectedActor > 0 ? selectedActor : ''}"/>
                    <c:param name="type" value="${selectedType}"/>
                    <c:param name="from" value="${from}"/>
                    <c:param name="to" value="${to}"/>
                    <c:param name="page" value="${p}"/>
                  </c:url>
                  <a class="pagination__page ${p == result.page ? 'is-current' : ''}" href="${pageUrl}" ${p == result.page ? 'aria-current="page"' : ''}>${p}</a>
                </c:forEach>
              </div>
            </nav>
          </c:if>
        </section>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
