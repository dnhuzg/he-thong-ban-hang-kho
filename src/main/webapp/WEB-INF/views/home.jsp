<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Trang chủ"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

        <div class="page-header">
          <div>
            <h1>Xin chào, <c:out value="${currentUser.fullName}"/></h1>
          </div>
        </div>

        <c:choose>
          <c:when test="${not empty userStats}">
            <section class="card-grid" aria-label="Tổng quan tài khoản">
              <article class="card card__body">
                <p class="stat__label">Tài khoản đang hoạt động</p>
                <p class="stat__value"><c:out value="${userStats['ACTIVE']}"/></p>
              </article>
              <article class="card card__body">
                <p class="stat__label">Chờ kích hoạt</p>
                <p class="stat__value"><c:out value="${userStats['PENDING']}"/></p>
              </article>
              <article class="card card__body">
                <p class="stat__label">Tài khoản bị khoá</p>
                <p class="stat__value"><c:out value="${userStats['LOCKED']}"/></p>
              </article>
            </section>

            <c:if test="${not empty handover}">
              <%-- Đại lý của nhân viên đã bị khoá (S1-10); chuyển giao ở trang Chuyển giao đại lý (S3-06) --%>
              <section class="card mt-4" aria-labelledby="handover-title">
                <div class="card__header">
                  <h2 id="handover-title">Đại lý cần bàn giao (${handover.size()})</h2>
                  <p>Nhân viên phụ trách các đại lý này đã bị khoá tài khoản.</p>
                </div>
                <div class="table-wrap">
                  <table class="table">
                    <thead>
                      <tr>
                        <th scope="col">Đại lý</th>
                        <th scope="col">Nhân viên phụ trách (đã khoá)</th>
                      </tr>
                    </thead>
                    <tbody>
                      <c:forEach var="h" items="${handover}">
                        <tr>
                          <td>
                            <div class="table__cell-main"><c:out value="${h.customerName}"/></div>
                            <div class="table__cell-sub"><c:out value="${h.customerCode}"/></div>
                          </td>
                          <td>
                            <div class="table__cell-main"><c:out value="${h.repFullName}"/></div>
                            <div class="table__cell-sub"><c:out value="${h.repUsername}"/></div>
                            <c:if test="${canAssign}"><a class="table__cell-sub" href="${ctx}/customers/transfer?from=${h.repId}">Chuyển giao</a></c:if>
                          </td>
                        </tr>
                      </c:forEach>
                    </tbody>
                  </table>
                </div>
              </section>
            </c:if>

            <section class="card mt-4" aria-labelledby="quick-title">
              <div class="card__header">
                <h2 id="quick-title">Việc thường làm</h2>
              </div>
              <div class="card__body">
                <div class="button-row">
                  <c:if test="${canManageUsers}">
                    <a class="btn btn--primary" href="${ctx}/admin/users/new">Tạo tài khoản</a>
                  </c:if>
                  <a class="btn btn--secondary" href="${ctx}/admin/users">Danh sách người dùng</a>
                  <c:if test="${currentUser.admin}">
                    <a class="btn btn--secondary" href="${ctx}/admin/roles">Cấu hình phân quyền</a>
                  </c:if>
                </div>
              </div>
            </section>
          </c:when>
          <c:otherwise>
            <section class="card">
              <div class="card__body">
                <p>Các chức năng nghiệp vụ theo vai trò của bạn sẽ lần lượt được mở trong các sprint tới (xem mục "Sắp có" trên menu).</p>
              </div>
            </section>
          </c:otherwise>
        </c:choose>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
