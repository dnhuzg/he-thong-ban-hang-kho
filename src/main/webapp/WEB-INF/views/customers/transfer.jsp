<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Chuyển giao đại lý"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

        <div class="page-header">
          <div>
            <p class="breadcrumb"><a href="${ctx}/customers">Đại lý &amp; hạn mức công nợ</a> / Đại lý</p>
            <h1>Chuyển giao đại lý</h1>
            <p class="page-header__desc">Chuyển các đại lý của một nhân viên sang người kế nhiệm khi nhân viên nghỉ; mỗi đại lý được ghi lịch sử.</p>
          </div>
          <a class="btn btn--secondary" href="${ctx}/customers"><span class="icon icon--sm icon--arrow-left" aria-hidden="true"></span>Quay lại danh sách</a>
        </div>

        <section class="card">
          <div class="card__header">
            <h2>1. Chọn nhân viên cần chuyển giao</h2>
            <p>Chỉ hiện nhân viên đang phụ trách ít nhất một đại lý. Nhân viên đã bị khoá được đánh dấu.</p>
          </div>
          <form class="toolbar" action="${ctx}/customers/transfer" method="get">
            <label class="visually-hidden" for="from">Nhân viên cần chuyển giao</label>
            <select class="select toolbar__wide" id="from" name="from" required data-autosubmit>
              <option value="">— Chọn nhân viên —</option>
              <c:forEach var="r" items="${reps}">
                <option value="${r.id}" ${r.id == selectedFrom ? 'selected' : ''}><c:out value="${r.name}${r.locked ? ' — đã khoá' : ''} · ${r.customers} đại lý"/></option>
              </c:forEach>
            </select>
            <label class="visually-hidden" for="area">Khu vực</label>
            <select class="select toolbar__wide" id="area" name="area" data-autosubmit>
              <option value="">Tất cả khu vực</option>
              <c:forEach var="a" items="${areas}">
                <option value="${a.id}" ${a.id == selectedArea ? 'selected' : ''}><c:out value="${a.name}"/></option>
              </c:forEach>
            </select>
            <button class="btn btn--secondary toolbar__submit" type="submit">Xem đại lý</button>
          </form>
        </section>

        <c:if test="${selectedFrom > 0}">
          <form action="${ctx}/customers/transfer/apply" method="post" class="transfer-form">
            <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
            <input type="hidden" name="from" value="${selectedFrom}">
            <input type="hidden" name="area" value="${selectedArea}">

            <section class="card">
              <div class="card__header">
                <h2>2. Chọn đại lý và người nhận</h2>
                <p><c:out value="${candidates.size()}"/> đại lý <c:if test="${selectedArea > 0}">trong khu vực đã chọn</c:if>. Bỏ chọn những đại lý không chuyển.</p>
              </div>
              <c:choose>
                <c:when test="${empty candidates}">
                  <p class="card__body text-muted">Nhân viên này không có đại lý nào trong phạm vi đã chọn.</p>
                </c:when>
                <c:otherwise>
                  <div class="table-wrap">
                    <table class="table">
                      <thead>
                        <tr>
                          <th scope="col" class="col-menu"><input type="checkbox" data-check-all="customerId" checked aria-label="Chọn tất cả đại lý"></th>
                          <th scope="col">Đại lý</th>
                          <th scope="col">Khu vực</th>
                          <th scope="col">Nhóm khách hàng</th>
                        </tr>
                      </thead>
                      <tbody>
                        <c:forEach var="cu" items="${candidates}">
                          <tr>
                            <td><input type="checkbox" name="customerId" value="${cu.id}" checked aria-label="Chuyển <c:out value='${cu.name}'/>"></td>
                            <td>
                              <div class="table__cell-main"><c:out value="${cu.name}"/></div>
                              <div class="table__cell-sub"><c:out value="${cu.code}"/></div>
                            </td>
                            <td class="text-muted"><c:out value="${empty cu.areaName ? '—' : cu.areaName}"/></td>
                            <td class="text-muted"><c:out value="${empty cu.groupName ? '—' : cu.groupName}"/></td>
                          </tr>
                        </c:forEach>
                      </tbody>
                    </table>
                  </div>
                  <div class="card__body dp-form">
                    <div class="form-card__row">
                      <div class="form-field">
                        <label class="form-label" for="to">Người nhận bàn giao <span class="required">*</span></label>
                        <select class="select" id="to" name="to" required>
                          <option value="">— Chọn người nhận —</option>
                          <c:forEach var="t" items="${targets}">
                            <c:if test="${t.id != selectedFrom}"><option value="${t.id}"><c:out value="${t.name}"/></option></c:if>
                          </c:forEach>
                        </select>
                      </div>
                      <div class="form-field">
                        <label class="form-label" for="reason">Lý do chuyển giao <span class="required">*</span></label>
                        <input class="input" id="reason" name="reason" maxlength="500" required placeholder="Ví dụ: nhân viên nghỉ việc">
                      </div>
                    </div>
                    <div class="form-footer__actions">
                      <button class="btn btn--primary" type="submit"><span class="icon icon--sm icon--check" aria-hidden="true"></span>Chuyển giao</button>
                    </div>
                  </div>
                </c:otherwise>
              </c:choose>
            </section>
          </form>
        </c:if>

        <section class="card dp-card">
          <div class="card__header">
            <h2>Lịch sử phân công gần đây</h2>
            <p>Mỗi lần đổi người phụ trách đều được ghi lại, không sửa hay xoá được.</p>
          </div>
          <div class="credit-history">
            <c:if test="${empty recent}"><p class="text-muted credit-history__empty">Chưa có lần phân công nào.</p></c:if>
            <c:forEach var="ch" items="${recent}">
              <article class="credit-change">
                <p class="credit-change__what">
                  <a href="${ctx}/customers/view?id=${ch.customerId}"><c:out value="${ch.customerName}"/></a>:
                  <c:out value="${ch.fromLabel}"/> <span aria-hidden="true">→</span><span class="visually-hidden">sang</span> <strong><c:out value="${ch.toLabel}"/></strong>
                </p>
                <c:if test="${not empty ch.reason}"><p class="credit-change__reason">Lý do: <c:out value="${ch.reason}"/></p></c:if>
                <p class="credit-change__meta"><c:out value="${ch.changedByName}"/> · <c:out value="${ch.timeText}"/></p>
              </article>
            </c:forEach>
          </div>
        </section>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
