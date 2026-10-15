<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="pageTitle" value="Đại lý"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

        <div class="page-header">
          <div>
            <p class="breadcrumb">Đại lý &amp; hạn mức công nợ</p>
            <h1>Đại lý</h1>
            <p class="page-header__desc">Danh sách khách hàng chuẩn của công ty: nhóm giá, khu vực và người phụ trách.</p>
          </div>
          <c:if test="${canManage}">
            <div class="page-header__actions">
              <c:if test="${canAssign}"><a class="btn btn--secondary" href="${ctx}/customers/transfer">Chuyển giao đại lý</a></c:if>
              <a class="btn btn--primary" href="${ctx}/customers/new"><span class="icon icon--sm icon--plus" aria-hidden="true"></span>Thêm đại lý</a>
            </div>
          </c:if>
        </div>

        <section class="card-grid card-grid--5 mb-4" aria-label="Tổng quan đại lý">
          <a class="card card__body home-stat home-stat--link" href="${ctx}/customers">
            <div class="home-stat__head">
              <p class="stat__label">Tổng số đại lý</p>
              <span class="home-stat__icon home-stat__icon--primary"><span class="icon icon--sm icon--building-2" aria-hidden="true"></span></span>
            </div>
            <p class="home-stat__value"><span class="stat__value"><fmt:formatNumber value="${stats.total}"/></span> đối tác</p>
            <p class="home-stat__note"><span class="dot dot--success" aria-hidden="true"></span><fmt:formatNumber value="${stats.active}"/> đang giao dịch · <span class="dot dot--danger" aria-hidden="true"></span><fmt:formatNumber value="${stats.locked}"/> tạm khoá</p>
          </a>
          <a class="card card__body home-stat home-stat--link" href="${ctx}/customers?status=LOCKED">
            <div class="home-stat__head">
              <p class="stat__label">Đang khoá giao dịch</p>
              <span class="home-stat__icon${stats.locked > 0 ? ' home-stat__icon--danger' : ''}"><span class="icon icon--sm icon--lock-keyhole" aria-hidden="true"></span></span>
            </div>
            <p class="home-stat__value"><span class="stat__value"><fmt:formatNumber value="${stats.locked}"/></span> đại lý</p>
            <p class="home-stat__note">Không tạo được đơn mới cho tới khi mở khoá</p>
          </a>
          <div class="card card__body home-stat">
            <div class="home-stat__head">
              <p class="stat__label">Tổng hạn mức cấp phát</p>
              <span class="home-stat__icon home-stat__icon--primary"><span class="icon icon--sm icon--wallet" aria-hidden="true"></span></span>
            </div>
            <p class="home-stat__value"><span class="stat__value stat__value--money"><fmt:formatNumber value="${stats.totalCreditLimit}" maxFractionDigits="0"/></span> ₫</p>
            <p class="home-stat__note">Cho nợ: <fmt:formatNumber value="${stats.withCredit}"/> đại lý · Thanh toán ngay: <fmt:formatNumber value="${stats.payNow}"/></p>
          </div>
          <div class="card card__body home-stat${stats.nearLimit > 0 ? ' home-stat--warning' : ''}">
            <div class="home-stat__head">
              <p class="stat__label">Dư nợ hiện tại</p>
              <span class="home-stat__icon home-stat__icon--${stats.nearLimit > 0 ? 'warning' : 'primary'}"><span class="icon icon--sm icon--history" aria-hidden="true"></span></span>
            </div>
            <p class="home-stat__value"><span class="stat__value stat__value--money"><fmt:formatNumber value="${stats.totalDebt}" maxFractionDigits="0"/></span> ₫</p>
            <p class="home-stat__note">Đã dùng <fmt:formatNumber value="${stats.usagePercent}"/>% tổng hạn mức · <fmt:formatNumber value="${stats.nearLimit}"/> đại lý từ 90% hạn mức</p>
          </div>
          <c:if test="${canManage}">
          <div class="card card__body home-stat${stats.unassigned > 0 ? ' home-stat--warning' : ''}">
            <div class="home-stat__head">
              <p class="stat__label">Chưa phân công</p>
              <span class="home-stat__icon home-stat__icon--warning"><span class="icon icon--sm icon--users" aria-hidden="true"></span></span>
            </div>
            <p class="home-stat__value"><span class="stat__value"><fmt:formatNumber value="${stats.unassigned}"/></span> đại lý</p>
            <p class="home-stat__note">Chưa có nhân viên kinh doanh phụ trách</p>
          </div>
          </c:if>
        </section>

        <section class="card list-card">
          <form class="toolbar" action="${ctx}/customers" method="get" role="search">
            <label class="visually-hidden" for="q">Tìm đại lý</label>
            <div class="search-field toolbar__search">
              <span class="icon icon--search" aria-hidden="true"></span>
              <input id="q" name="q" type="search" placeholder="Tìm theo mã, tên hoặc số điện thoại" value="<c:out value='${q}'/>">
            </div>

            <label class="visually-hidden" for="group">Nhóm khách hàng</label>
            <select class="select" id="group" name="group" data-autosubmit>
              <option value="">Tất cả nhóm</option>
              <c:forEach var="g" items="${groups}">
                <option value="${g.id}" ${g.id == selectedGroup ? 'selected' : ''}><c:out value="${g.name}"/></option>
              </c:forEach>
            </select>

            <label class="visually-hidden" for="area">Khu vực</label>
            <select class="select" id="area" name="area" data-autosubmit>
              <option value="">Tất cả khu vực</option>
              <c:forEach var="a" items="${areas}">
                <option value="${a.id}" ${a.id == selectedArea ? 'selected' : ''}><c:out value="${a.name}"/></option>
              </c:forEach>
            </select>

            <c:if test="${canManage}">
              <label class="visually-hidden" for="rep">Người phụ trách</label>
              <select class="select" id="rep" name="rep" data-autosubmit>
                <option value="">Tất cả người phụ trách</option>
                <c:forEach var="r" items="${reps}">
                  <option value="${r.id}" ${r.id == selectedRep ? 'selected' : ''}><c:out value="${r.name}"/></option>
                </c:forEach>
              </select>
            </c:if>

            <label class="visually-hidden" for="status">Trạng thái</label>
            <select class="select" id="status" name="status" data-autosubmit>
              <option value="">Tất cả trạng thái</option>
              <c:forEach var="st" items="${statuses}">
                <option value="${st.name()}" ${st.name() == selectedStatus ? 'selected' : ''}><c:out value="${st.label}"/></option>
              </c:forEach>
            </select>

            <button class="btn btn--secondary toolbar__submit" type="submit">Lọc</button>
          </form>

          <c:if test="${not empty q or not empty selectedGroup or not empty selectedArea or not empty selectedRep or not empty selectedStatus}">
            <div class="filter-chips">
              <span class="filter-chips__label">Bộ lọc đang áp dụng:</span>
              <c:if test="${not empty q}">
                <c:url var="u" value="/customers"><c:param name="group" value="${selectedGroup}"/><c:param name="area" value="${selectedArea}"/><c:param name="rep" value="${selectedRep}"/><c:param name="status" value="${selectedStatus}"/></c:url>
                <a class="filter-chip" href="${u}" title="Bỏ bộ lọc này">Từ khoá: <c:out value="${q}"/> <span aria-hidden="true">×</span></a>
              </c:if>
              <c:if test="${not empty selectedGroup}">
                <c:forEach var="g" items="${groups}"><c:if test="${g.id == selectedGroup}"><c:set var="chipLabel" value="${g.name}"/></c:if></c:forEach>
                <c:url var="u" value="/customers"><c:param name="q" value="${q}"/><c:param name="area" value="${selectedArea}"/><c:param name="rep" value="${selectedRep}"/><c:param name="status" value="${selectedStatus}"/></c:url>
                <a class="filter-chip" href="${u}" title="Bỏ bộ lọc này">Nhóm: <c:out value="${chipLabel}"/> <span aria-hidden="true">×</span></a>
              </c:if>
              <c:if test="${not empty selectedArea}">
                <c:forEach var="a" items="${areas}"><c:if test="${a.id == selectedArea}"><c:set var="chipLabel" value="${a.name}"/></c:if></c:forEach>
                <c:url var="u" value="/customers"><c:param name="q" value="${q}"/><c:param name="group" value="${selectedGroup}"/><c:param name="rep" value="${selectedRep}"/><c:param name="status" value="${selectedStatus}"/></c:url>
                <a class="filter-chip" href="${u}" title="Bỏ bộ lọc này">Khu vực: <c:out value="${chipLabel}"/> <span aria-hidden="true">×</span></a>
              </c:if>
              <c:if test="${not empty selectedRep}">
                <c:forEach var="r" items="${reps}"><c:if test="${r.id == selectedRep}"><c:set var="chipLabel" value="${r.name}"/></c:if></c:forEach>
                <c:url var="u" value="/customers"><c:param name="q" value="${q}"/><c:param name="group" value="${selectedGroup}"/><c:param name="area" value="${selectedArea}"/><c:param name="status" value="${selectedStatus}"/></c:url>
                <a class="filter-chip" href="${u}" title="Bỏ bộ lọc này">Phụ trách: <c:out value="${chipLabel}"/> <span aria-hidden="true">×</span></a>
              </c:if>
              <c:if test="${not empty selectedStatus}">
                <c:forEach var="st" items="${statuses}"><c:if test="${st.name() == selectedStatus}"><c:set var="chipLabel" value="${st.label}"/></c:if></c:forEach>
                <c:url var="u" value="/customers"><c:param name="q" value="${q}"/><c:param name="group" value="${selectedGroup}"/><c:param name="area" value="${selectedArea}"/><c:param name="rep" value="${selectedRep}"/></c:url>
                <a class="filter-chip" href="${u}" title="Bỏ bộ lọc này">Trạng thái: <c:out value="${chipLabel}"/> <span aria-hidden="true">×</span></a>
              </c:if>
              <a class="filter-chips__clear" href="${ctx}/customers">Xoá tất cả</a>
            </div>
          </c:if>

          <div class="list-summary">
            <span class="list-summary__title">Tất cả đại lý</span>
            <span class="badge badge--primary"><c:out value="${result.total}"/></span>
          </div>

          <div class="table-wrap list-table">
            <table class="table">
              <thead>
                <tr>
                  <th scope="col" class="col-sku">Mã</th>
                  <th scope="col">Đại lý</th>
                  <th scope="col">Nhóm khách hàng</th>
                  <th scope="col">Khu vực</th>
                  <th scope="col">Người phụ trách</th>
                  <th scope="col" class="num">Hạn mức tín dụng</th>
                  <th scope="col" class="num">Kỳ hạn nợ</th>
                  <th scope="col" class="col-status">Trạng thái</th>
                  <th scope="col" class="col-menu"><span class="visually-hidden">Thao tác</span></th>
                </tr>
              </thead>
              <tbody>
                <c:if test="${empty result.items}">
                  <tr><td colspan="9" class="text-muted">Không có đại lý nào khớp.</td></tr>
                </c:if>
                <c:forEach var="cu" items="${result.items}">
                  <tr>
                    <td class="nowrap table__cell-code"><c:out value="${cu.code}"/></td>
                    <td>
                      <div class="product-cell">
                        <span class="initials-tile" aria-hidden="true"><c:out value="${cu.initials}"/></span>
                        <div>
                          <a class="table__cell-main" href="${ctx}/customers/view?id=${cu.id}"><c:out value="${cu.name}"/></a>
                          <c:if test="${not empty cu.phone}"><div class="table__cell-sub"><c:out value="${cu.phone}"/></div></c:if>
                        </div>
                      </div>
                    </td>
                    <td><c:out value="${empty cu.groupName ? '—' : cu.groupName}"/></td>
                    <td class="text-muted"><c:out value="${empty cu.areaName ? '—' : cu.areaName}"/></td>
                    <td class="text-muted"><c:out value="${empty cu.salesRepName ? 'Chưa phân công' : cu.salesRepName}"/></td>
                    <td class="num nowrap">
                      <c:choose>
                        <c:when test="${cu.creditLimit.signum() > 0}">
                          <span class="table__cell-main"><fmt:formatNumber value="${cu.creditLimit}" maxFractionDigits="0"/> ₫</span>
                          <div class="meter meter--${cu.usageTone}">
                            <div class="meter__head"><span>Dư nợ <fmt:formatNumber value="${cu.debt}" maxFractionDigits="0"/> ₫</span><span class="meter__value">${cu.usagePercent}%</span></div>
                            <div class="meter__track"><span class="meter__fill meter__fill--${cu.usageStep}"></span></div>
                          </div>
                        </c:when>
                        <c:otherwise><span class="text-muted">Không cho nợ</span></c:otherwise>
                      </c:choose>
                    </td>
                    <td class="num nowrap">
                      <c:choose>
                        <c:when test="${cu.creditDays > 0}"><span class="badge">${cu.creditDays} ngày</span></c:when>
                        <c:otherwise><span class="text-muted">Trả ngay</span></c:otherwise>
                      </c:choose>
                    </td>
                    <td><span class="badge ${cu.locked ? 'badge--danger' : 'badge--success'}"><c:out value="${cu.status.label}"/></span></td>
                    <td>
                      <details class="row-menu">
                        <summary aria-label="Thao tác với <c:out value='${cu.code}'/>"><span class="icon icon--md icon--ellipsis" aria-hidden="true"></span></summary>
                        <div class="row-menu__panel">
                          <a href="${ctx}/customers/view?id=${cu.id}">Xem chi tiết</a>
                          <c:if test="${canManage}">
                            <a href="${ctx}/customers/edit?id=${cu.id}">Sửa</a>
                            <button type="button" class="is-danger" data-dialog-open="delete-${cu.id}">Xoá</button>
                          </c:if>
                        </div>
                      </details>
                    </td>
                  </tr>
                </c:forEach>
              </tbody>
            </table>
          </div>

          <%-- Điện thoại: mỗi đại lý một thẻ --%>
          <div class="product-cards">
            <c:if test="${empty result.items}">
              <p class="text-muted">Không có đại lý nào khớp.</p>
            </c:if>
            <c:forEach var="cu" items="${result.items}">
              <article class="product-card">
                <div class="product-card__head">
                  <div class="product-card__title">
                    <a class="product-card__name" href="${ctx}/customers/view?id=${cu.id}"><c:out value="${cu.name}"/></a>
                    <p class="product-card__sku"><c:out value="${cu.code}"/><c:if test="${not empty cu.phone}"><c:out value=" · ${cu.phone}"/></c:if></p>
                  </div>
                  <details class="row-menu">
                    <summary aria-label="Thao tác với <c:out value='${cu.code}'/>"><span class="icon icon--md icon--ellipsis" aria-hidden="true"></span></summary>
                    <div class="row-menu__panel">
                      <a href="${ctx}/customers/view?id=${cu.id}">Xem chi tiết</a>
                      <c:if test="${canManage}">
                        <a href="${ctx}/customers/edit?id=${cu.id}">Sửa</a>
                        <button type="button" class="is-danger" data-dialog-open="delete-${cu.id}">Xoá</button>
                      </c:if>
                    </div>
                  </details>
                </div>
                <div class="product-card__meta">
                  <p><c:out value="${empty cu.groupName ? '—' : cu.groupName}"/> · <c:out value="${empty cu.areaName ? 'Chưa có khu vực' : cu.areaName}"/></p>
                  <p>Phụ trách: <c:out value="${empty cu.salesRepName ? 'chưa phân công' : cu.salesRepName}"/></p>
                </div>
                <div class="product-card__foot">
                  <span class="badge ${cu.locked ? 'badge--danger' : 'badge--success'}"><c:out value="${cu.status.label}"/></span>
                </div>
              </article>
            </c:forEach>
          </div>

          <c:if test="${canManage}">
            <c:forEach var="cu" items="${result.items}">
              <dialog class="dialog" id="delete-${cu.id}" aria-labelledby="delete-title-${cu.id}">
                <form action="${ctx}/customers/delete" method="post">
                  <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                  <input type="hidden" name="id" value="${cu.id}">
                  <div class="dialog__header">
                    <h2 id="delete-title-${cu.id}">Xoá đại lý <c:out value="${cu.code}"/>?</h2>
                  </div>
                  <div class="dialog__body">
                    <p>Chỉ xoá được đại lý chưa phát sinh đơn hàng. Đại lý đã có giao dịch hãy khoá giao dịch thay vì xoá.</p>
                  </div>
                  <div class="dialog__footer">
                    <button class="btn btn--secondary" type="button" data-dialog-close>Huỷ</button>
                    <button class="btn btn--danger" type="submit">Xoá</button>
                  </div>
                </form>
              </dialog>
            </c:forEach>
          </c:if>

          <nav class="pagination" aria-label="Phân trang">
            <span class="text-muted">Hiển thị ${result.from}–${result.to} trên ${result.total} đại lý</span>
            <div class="pagination__pages">
              <c:url var="prevUrl" value="/customers">
                <c:param name="q" value="${q}"/>
                <c:param name="group" value="${selectedGroup}"/>
                <c:param name="area" value="${selectedArea}"/>
                <c:param name="rep" value="${selectedRep}"/>
                <c:param name="status" value="${selectedStatus}"/>
                <c:param name="page" value="${result.page - 1}"/>
              </c:url>
              <c:url var="nextUrl" value="/customers">
                <c:param name="q" value="${q}"/>
                <c:param name="group" value="${selectedGroup}"/>
                <c:param name="area" value="${selectedArea}"/>
                <c:param name="rep" value="${selectedRep}"/>
                <c:param name="status" value="${selectedStatus}"/>
                <c:param name="page" value="${result.page + 1}"/>
              </c:url>
              <a class="pagination__page ${result.page <= 1 ? 'is-disabled' : ''}" href="${prevUrl}" rel="prev">Trước</a>
              <c:forEach var="pg" begin="1" end="${result.totalPages}">
                <c:url var="pageUrl" value="/customers">
                <c:param name="q" value="${q}"/>
                <c:param name="group" value="${selectedGroup}"/>
                <c:param name="area" value="${selectedArea}"/>
                <c:param name="rep" value="${selectedRep}"/>
                <c:param name="status" value="${selectedStatus}"/>
                <c:param name="page" value="${pg}"/>
                </c:url>
                <a class="pagination__page ${pg == result.page ? 'is-current' : ''}" href="${pageUrl}" ${pg == result.page ? 'aria-current="page"' : ''}>${pg}</a>
              </c:forEach>
              <a class="pagination__page ${result.page >= result.totalPages ? 'is-disabled' : ''}" href="${nextUrl}" rel="next">Sau</a>
            </div>
          </nav>
        </section>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
