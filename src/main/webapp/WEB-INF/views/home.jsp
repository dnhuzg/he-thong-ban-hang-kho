<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Trang chủ"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

        <div class="page-header">
          <div>
            <h1>Xin chào, <c:out value="${currentUser.fullName}"/></h1>
            <p class="page-header__desc"><c:out value="${not empty userStats ? 'Tổng quan tài khoản người dùng và hoạt động gần đây của hệ thống.' : 'Chọn một mục bên dưới để bắt đầu công việc.'}"/></p>
          </div>
        </div>

        <c:choose>
          <c:when test="${not empty userStats}">
            <section class="card-grid" aria-label="Tổng quan tài khoản">
              <a class="card card__body home-stat home-stat--link" href="${ctx}/admin/users?status=ACTIVE" title="Xem danh sách tài khoản: tài khoản đang hoạt động">
                <div class="home-stat__head">
                  <p class="stat__label">Tài khoản đang hoạt động</p>
                  <span class="badge badge--success">Hoạt động</span>
                </div>
                <p class="home-stat__value"><span class="stat__value"><c:out value="${userStats['ACTIVE']}"/></span> tài khoản</p>
              </a>
              <a class="card card__body home-stat home-stat--link" href="${ctx}/admin/users?status=PENDING" title="Xem danh sách tài khoản: chờ kích hoạt">
                <div class="home-stat__head">
                  <p class="stat__label">Chờ kích hoạt</p>
                  <span class="home-stat__icon home-stat__icon--warning"><span class="icon icon--sm icon--history" aria-hidden="true"></span></span>
                </div>
                <p class="home-stat__value"><span class="stat__value"><c:out value="${userStats['PENDING']}"/></span> tài khoản</p>
              </a>
              <a class="card card__body home-stat home-stat--link" href="${ctx}/admin/users?status=LOCKED" title="Xem danh sách tài khoản: tài khoản bị khoá">
                <div class="home-stat__head">
                  <p class="stat__label">Tài khoản bị khoá</p>
                  <span class="home-stat__icon"><span class="icon icon--sm icon--lock-keyhole" aria-hidden="true"></span></span>
                </div>
                <p class="home-stat__value"><span class="stat__value"><c:out value="${userStats['LOCKED']}"/></span> tài khoản</p>
              </a>
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
                    <a class="btn btn--primary" href="${ctx}/admin/users/new"><span class="icon icon--sm icon--plus" aria-hidden="true"></span>Tạo tài khoản</a>
                  </c:if>
                  <a class="btn btn--secondary" href="${ctx}/admin/users"><span class="icon icon--sm icon--users" aria-hidden="true"></span>Danh sách người dùng</a>
                  <c:if test="${currentUser.admin}">
                    <a class="btn btn--secondary" href="${ctx}/admin/roles"><span class="icon icon--sm icon--shield-check" aria-hidden="true"></span>Cấu hình phân quyền</a>
                  </c:if>
                </div>
              </div>
            </section>

            <div class="home-columns mt-4">
              <section class="card" aria-labelledby="activity-title">
                <div class="card__header home-card__header">
                  <h2 id="activity-title">Nhật ký hoạt động gần đây</h2>
                  <a href="${ctx}/admin/audit-logs">Xem tất cả</a>
                </div>
                <div class="card__body">
                  <c:choose>
                    <c:when test="${empty recentActivity}">
                      <p class="text-muted">Chưa có thao tác nào được ghi lại.</p>
                    </c:when>
                    <c:otherwise>
                      <ul class="home-activity">
                        <c:forEach var="e" items="${recentActivity}">
                          <li class="home-activity__item">
                            <span class="home-activity__icon"><span class="icon icon--sm icon--history" aria-hidden="true"></span></span>
                            <div class="home-activity__body">
                              <p class="home-activity__title"><c:out value="${e.actionLabel}"/><c:if test="${not empty e.targetName}"> — <c:out value="${e.targetName}"/></c:if></p>
                              <p class="home-activity__meta"><c:out value="${empty e.actorName ? 'Hệ thống' : e.actorName}"/> • <c:out value="${e.createdAtText}"/></p>
                            </div>
                            <span class="badge"><c:out value="${e.targetLabel}"/></span>
                          </li>
                        </c:forEach>
                      </ul>
                    </c:otherwise>
                  </c:choose>
                </div>
              </section>

              <section class="card" aria-labelledby="status-title">
                <div class="card__header">
                  <h2 id="status-title">Tình trạng hệ thống</h2>
                </div>
                <div class="card__body">
                  <ul class="home-status">
                    <li><span>Cơ sở dữ liệu</span><span class="home-status__value home-status__value--ok">Kết nối tốt</span></li>
                    <li>
                      <span>Gửi email thông báo</span>
                      <c:choose>
                        <c:when test="${mailConfigured}"><span class="home-status__value home-status__value--ok">Đang hoạt động</span></c:when>
                        <c:otherwise><span class="home-status__value home-status__value--warning">Chỉ ghi log</span></c:otherwise>
                      </c:choose>
                    </li>
                  </ul>
                  <p class="home-note"><span class="icon icon--sm icon--info" aria-hidden="true"></span>Các chức năng nghiệp vụ còn lại sẽ lần lượt được mở theo kế hoạch từng sprint.</p>
                </div>
              </section>
            </div>
          </c:when>
          <c:otherwise>
            <%-- Lối tắt tới các mục người dùng được phép dùng (lấy từ menu đã lọc theo quyền) --%>
            <section class="home-shortcuts" aria-label="Lối tắt">
              <c:forEach var="group" items="${menu}">
                <c:forEach var="item" items="${group.items}">
                  <c:if test="${not item.comingSoon and not empty item.url and item.url ne '/home'}">
                    <a class="card home-shortcut" href="${ctx}${item.url}">
                      <span class="home-shortcut__icon"><span class="icon icon--md icon--${item.icon}" aria-hidden="true"></span></span>
                      <span class="home-shortcut__label"><c:out value="${item.label}"/></span>
                      <span class="icon icon--sm icon--chevron-right home-shortcut__arrow" aria-hidden="true"></span>
                    </a>
                  </c:if>
                </c:forEach>
              </c:forEach>
            </section>
          </c:otherwise>
        </c:choose>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
