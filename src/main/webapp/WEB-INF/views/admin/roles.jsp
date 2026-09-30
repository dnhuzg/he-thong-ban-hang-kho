<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Vai trò & quyền"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

        <div class="page-header">
          <div>
            <p class="breadcrumb">Quản trị</p>
            <h1>Vai trò &amp; quyền</h1>
          </div>
        </div>

        <form action="${ctx}/admin/roles" method="post">
          <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
          <section class="card">
            <div class="card__header">
              <div class="perm-legend">
                <span><strong>F</strong> toàn quyền</span>
                <span><strong>W</strong> ghi trong phạm vi được giao</span>
                <span><strong>R</strong> chỉ xem</span>
                <span><strong>–</strong> không truy cập</span>
                <span><strong>*</strong> chỉ dữ liệu của mình / nhóm đại lý mình phụ trách</span>
              </div>
            </div>

            <div class="table-wrap">
              <table class="table">
                <thead>
                  <tr>
                    <th scope="col">Module</th>
                    <c:forEach var="r" items="${roles}">
                      <th scope="col"><c:out value="${r.label}"/></th>
                    </c:forEach>
                  </tr>
                </thead>
                <tbody>
                  <c:forEach var="m" items="${modules}">
                    <tr>
                      <th scope="row"><c:out value="${m.label}"/></th>
                      <c:forEach var="r" items="${roles}">
                        <td>
                          <select class="perm-select" name="perm.${m.code}.${r.code}" aria-label="<c:out value='${m.label} – ${r.label}'/>">
                            <c:forEach var="lv" items="${levels}">
                              <option value="${lv.code}" ${matrix[r][m] == lv ? 'selected' : ''}>${lv.symbol}</option>
                            </c:forEach>
                          </select>
                        </td>
                      </c:forEach>
                    </tr>
                  </c:forEach>
                </tbody>
              </table>
            </div>

            <div class="card__body">
              <%-- Ràng buộc bắt buộc của dự án, không cho cấu hình (S1-05) --%>
              <div class="alert alert--info" role="note">
                <strong>Giá vốn và biên lợi nhuận</strong> chỉ hiển thị cho Quản lý kinh doanh. Quyền này cố định, không thay đổi được ở đây.
              </div>
            </div>
          </section>

          <div class="form-actions">
            <button class="btn btn--secondary" type="reset">Hoàn tác</button>
            <button class="btn btn--primary" type="submit">Lưu phân quyền</button>
          </div>
        </form>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
