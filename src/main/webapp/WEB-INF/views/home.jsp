<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="pageTitle" value="Trang chủ"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

        <div class="page-header">
          <div>
            <h1>Xin chào, <c:out value="${currentUser.fullName}"/></h1>
            <p class="page-header__desc"><c:out value="${currentUser.admin ? 'Tổng quan tài khoản người dùng và hoạt động gần đây của hệ thống.' : 'Việc cần làm hôm nay và lối tắt tới các mục bạn được dùng.'}"/></p>
          </div>
        </div>

        <c:choose>
          <c:when test="${currentUser.admin}">
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
            <c:if test="${dash.portalUnlinked}">
              <div class="alert alert--warning mb-4" role="status">Tài khoản của bạn chưa được gắn với đại lý nào. Vui lòng liên hệ nhân viên kinh doanh phụ trách.</div>
            </c:if>
            <c:if test="${dash.noWarehouse}">
              <div class="alert alert--warning mb-4" role="status">Tài khoản của bạn chưa được gắn kho nào nên chưa có số liệu kho. Vui lòng liên hệ quản trị hệ thống.</div>
            </c:if>

            <c:if test="${not empty dash.cards}">
              <section class="card-grid" aria-label="Số liệu chính">
                <c:forEach var="s" items="${dash.cards}">
                  <c:set var="statClass" value="card card__body home-stat${not empty s.url ? ' home-stat--link' : ''}${s.tone eq 'warning' and s.count > 0 ? ' home-stat--warning' : ''}"/>
                  <c:choose>
                    <c:when test="${not empty s.url}"><a class="${statClass}" href="${ctx}${s.url}"></c:when>
                    <c:otherwise><div class="${statClass}"></c:otherwise>
                  </c:choose>
                    <div class="home-stat__head">
                      <p class="stat__label"><c:out value="${s.label}"/></p>
                      <span class="home-stat__icon${empty s.tone ? '' : ' home-stat__icon--'.concat(s.tone)}"><span class="icon icon--sm icon--${s.icon}" aria-hidden="true"></span></span>
                    </div>
                    <c:choose>
                      <c:when test="${not empty s.money}">
                        <p class="home-stat__value"><span class="stat__value stat__value--money"><fmt:formatNumber value="${s.money}" maxFractionDigits="0"/></span> ₫</p>
                      </c:when>
                      <c:otherwise>
                        <p class="home-stat__value"><span class="stat__value"><fmt:formatNumber value="${s.count}"/></span> <c:out value="${s.unit}"/></p>
                      </c:otherwise>
                    </c:choose>
                    <c:if test="${not empty s.note}"><p class="home-stat__note"><c:out value="${s.note}"/></p></c:if>
                  <c:choose>
                    <c:when test="${not empty s.url}"></a></c:when>
                    <c:otherwise></div></c:otherwise>
                  </c:choose>
                </c:forEach>
              </section>
            </c:if>

            <div class="home-columns mt-4">
              <div>
                <c:set var="hasTodo" value="${not empty dash.handover or not empty dash.pendingTitle or not empty dash.lockedCustomers or not empty dash.creditChanges or not empty dash.pickQueue or not empty dash.myOrders}"/>

                <c:if test="${not empty dash.handover}">
                  <%-- Đại lý của nhân viên đã bị khoá (S1-10); chuyển giao ở trang Chuyển giao đại lý (S3-06) --%>
                  <section class="card" aria-labelledby="handover-title">
                    <div class="card__header">
                      <h2 id="handover-title">Đại lý cần bàn giao (<c:out value="${dash.handover.size()}"/>)</h2>
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
                          <c:forEach var="h" items="${dash.handover}">
                            <tr>
                              <td>
                                <div class="table__cell-main"><c:out value="${h.customerName}"/></div>
                                <div class="table__cell-sub"><c:out value="${h.customerCode}"/></div>
                              </td>
                              <td>
                                <div class="table__cell-main"><c:out value="${h.repFullName}"/></div>
                                <div class="table__cell-sub"><c:out value="${h.repUsername}"/></div>
                                <c:if test="${dash.canAssign}"><a class="table__cell-sub" href="${ctx}/customers/transfer?from=${h.repId}">Chuyển giao</a></c:if>
                              </td>
                            </tr>
                          </c:forEach>
                        </tbody>
                      </table>
                    </div>
                  </section>
                </c:if>

                <c:if test="${not empty dash.pendingTitle}">
                  <section class="card" aria-labelledby="pending-title">
                    <div class="card__header home-card__header">
                      <h2 id="pending-title"><c:out value="${dash.pendingTitle}"/> (<c:out value="${dash.pending.size()}"/>)</h2>
                      <a href="${ctx}/orders/approvals">Mở trang duyệt</a>
                    </div>
                    <c:choose>
                      <c:when test="${empty dash.pending}">
                        <div class="card__body"><p class="text-muted">Không có đơn nào đang chờ.</p></div>
                      </c:when>
                      <c:otherwise>
                        <div class="table-wrap">
                          <table class="table">
                            <thead>
                              <tr>
                                <th scope="col">Mã đơn</th>
                                <th scope="col">Đại lý</th>
                                <th scope="col" class="num">Tổng</th>
                                <th scope="col">Lý do</th>
                              </tr>
                            </thead>
                            <tbody>
                              <c:forEach var="p" items="${dash.pending}" end="4">
                                <tr>
                                  <td class="nowrap table__cell-code"><a href="${ctx}/orders/edit?id=${p.id}#duyet-don"><c:out value="${p.code}"/></a></td>
                                  <td class="table__cell-main"><c:out value="${p.customer}"/></td>
                                  <td class="num nowrap"><fmt:formatNumber value="${p.total}" maxFractionDigits="0"/> ₫</td>
                                  <td><c:out value="${p.reason}"/></td>
                                </tr>
                              </c:forEach>
                            </tbody>
                          </table>
                        </div>
                      </c:otherwise>
                    </c:choose>
                  </section>
                </c:if>

                <c:if test="${not empty dash.lockedCustomers}">
                  <section class="card" aria-labelledby="locked-title">
                    <div class="card__header home-card__header">
                      <h2 id="locked-title">Đại lý của tôi đang bị khoá</h2>
                      <a href="${ctx}/customers?status=LOCKED">Xem tất cả (<c:out value="${dash.lockedTotal}"/>)</a>
                    </div>
                    <div class="table-wrap">
                      <table class="table">
                        <thead>
                          <tr>
                            <th scope="col">Đại lý</th>
                            <th scope="col">Lý do khoá</th>
                          </tr>
                        </thead>
                        <tbody>
                          <c:forEach var="lc" items="${dash.lockedCustomers}">
                            <tr>
                              <td>
                                <a class="table__cell-main" href="${ctx}/customers/view?id=${lc.id}"><c:out value="${lc.name}"/></a>
                                <div class="table__cell-sub"><c:out value="${lc.code}"/></div>
                              </td>
                              <td><c:out value="${lc.lockReason}"/></td>
                            </tr>
                          </c:forEach>
                        </tbody>
                      </table>
                    </div>
                  </section>
                </c:if>

                <c:if test="${not empty dash.creditChanges}">
                  <section class="card" aria-labelledby="credit-title">
                    <div class="card__header">
                      <h2 id="credit-title">Lần sửa hạn mức gần đây</h2>
                    </div>
                    <div class="table-wrap">
                      <table class="table">
                        <thead>
                          <tr>
                            <th scope="col">Đại lý</th>
                            <th scope="col" class="num">Hạn mức cũ → mới</th>
                            <th scope="col">Lý do</th>
                            <th scope="col">Người sửa</th>
                          </tr>
                        </thead>
                        <tbody>
                          <c:forEach var="ch" items="${dash.creditChanges}">
                            <tr>
                              <td>
                                <a class="table__cell-main" href="${ctx}/customers/view?id=${ch.customerId}"><c:out value="${ch.customerName}"/></a>
                                <div class="table__cell-sub"><c:out value="${ch.customerCode}"/></div>
                              </td>
                              <td class="num nowrap"><fmt:formatNumber value="${ch.oldLimit}" maxFractionDigits="0"/> → <fmt:formatNumber value="${ch.newLimit}" maxFractionDigits="0"/> ₫</td>
                              <td><c:out value="${ch.reason}"/></td>
                              <td>
                                <div class="table__cell-main"><c:out value="${ch.changedByName}"/></div>
                                <div class="table__cell-sub"><c:out value="${ch.changedAtText}"/></div>
                              </td>
                            </tr>
                          </c:forEach>
                        </tbody>
                      </table>
                    </div>
                  </section>
                </c:if>

                <c:if test="${not empty dash.pickQueue}">
                  <section class="card" aria-labelledby="pick-title">
                    <div class="card__header">
                      <h2 id="pick-title">Đơn cần soạn sớm nhất</h2>
                      <p>Theo ngày giao mong muốn; đơn chưa hẹn ngày xếp cuối.</p>
                    </div>
                    <div class="table-wrap">
                      <table class="table">
                        <thead>
                          <tr>
                            <th scope="col">Mã đơn</th>
                            <th scope="col">Đại lý</th>
                            <th scope="col">Ngày giao</th>
                            <th scope="col">Trạng thái</th>
                          </tr>
                        </thead>
                        <tbody>
                          <c:forEach var="o" items="${dash.pickQueue}">
                            <tr>
                              <td class="nowrap table__cell-code"><c:out value="${o.code}"/></td>
                              <td class="table__cell-main"><c:out value="${o.customerName}"/></td>
                              <td class="nowrap"><c:out value="${empty o.deliveryDateText ? 'Chưa hẹn' : o.deliveryDateText}"/></td>
                              <td><span class="badge${empty o.status.tone ? '' : ' badge--'.concat(o.status.tone)}"><c:out value="${o.status.label}"/></span></td>
                            </tr>
                          </c:forEach>
                        </tbody>
                      </table>
                    </div>
                  </section>
                </c:if>

                <c:if test="${not empty dash.myOrders}">
                  <section class="card" aria-labelledby="my-orders-title">
                    <div class="card__header">
                      <h2 id="my-orders-title">Đơn gần đây</h2>
                    </div>
                    <div class="table-wrap">
                      <table class="table">
                        <thead>
                          <tr>
                            <th scope="col">Mã đơn</th>
                            <th scope="col">Cập nhật</th>
                            <th scope="col" class="num">Tổng</th>
                            <th scope="col">Trạng thái</th>
                          </tr>
                        </thead>
                        <tbody>
                          <c:forEach var="o" items="${dash.myOrders}">
                            <tr>
                              <td class="nowrap table__cell-code"><c:out value="${o.code}"/></td>
                              <td class="nowrap"><c:out value="${o.updatedAtText}"/></td>
                              <td class="num nowrap"><fmt:formatNumber value="${o.payable}" maxFractionDigits="0"/> ₫</td>
                              <td><span class="badge${empty o.status.tone ? '' : ' badge--'.concat(o.status.tone)}"><c:out value="${o.status.label}"/></span></td>
                            </tr>
                          </c:forEach>
                        </tbody>
                      </table>
                    </div>
                  </section>
                </c:if>

                <c:if test="${not hasTodo}">
                  <section class="card">
                    <div class="card__body"><p class="text-muted">Chưa có việc nào cần bạn xử lý.</p></div>
                  </section>
                </c:if>
              </div>

              <section class="card" aria-labelledby="activity-title">
                <div class="card__header home-card__header">
                  <h2 id="activity-title">Hoạt động gần đây của tôi</h2>
                  <c:if test="${canViewAuditLog}"><a href="${ctx}/admin/audit-logs?actor=${currentUser.id}">Xem tất cả</a></c:if>
                </div>
                <div class="card__body">
                  <c:choose>
                    <c:when test="${empty dash.myActivity}">
                      <p class="text-muted">Bạn chưa có thao tác nào được ghi lại.</p>
                    </c:when>
                    <c:otherwise>
                      <ul class="home-activity">
                        <c:forEach var="e" items="${dash.myActivity}">
                          <li class="home-activity__item">
                            <span class="home-activity__icon"><span class="icon icon--sm icon--history" aria-hidden="true"></span></span>
                            <div class="home-activity__body">
                              <p class="home-activity__title"><c:out value="${e.actionLabel}"/><c:if test="${not empty e.targetName}"> — <c:out value="${e.targetName}"/></c:if></p>
                              <p class="home-activity__meta"><c:out value="${e.createdAtText}"/></p>
                            </div>
                            <span class="badge"><c:out value="${e.targetLabel}"/></span>
                          </li>
                        </c:forEach>
                      </ul>
                    </c:otherwise>
                  </c:choose>
                </div>
              </section>
            </div>

            <%-- Lối tắt tới các mục người dùng được phép dùng (lấy từ menu đã lọc theo quyền) --%>
            <section class="home-shortcuts mt-4" aria-label="Lối tắt">
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
