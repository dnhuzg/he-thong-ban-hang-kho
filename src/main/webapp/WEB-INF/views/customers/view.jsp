<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="pageTitle" value="${customer.name}"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

        <div class="page-header">
          <div>
            <p class="breadcrumb"><a href="${ctx}/customers">Đại lý &amp; hạn mức công nợ</a> / Đại lý</p>
            <h1><c:out value="${customer.name}"/></h1>
            <p class="page-header__desc"><c:out value="${customer.code}"/></p>
          </div>
          <div class="page-header__actions">
            <span class="badge ${customer.locked ? 'badge--danger' : 'badge--success'}"><c:out value="${customer.status.label}"/></span>
            <c:if test="${canManage}">
              <a class="btn btn--secondary" href="${ctx}/customers/edit?id=${customer.id}">Sửa hồ sơ</a>
            </c:if>
            <c:if test="${canManageCredit}">
              <button class="btn ${customer.locked ? 'btn--primary' : 'btn--secondary'}" type="button" data-dialog-open="trade-toggle"><c:out value="${customer.locked ? 'Mở giao dịch' : 'Khoá giao dịch'}"/></button>
            </c:if>
            <a class="btn btn--secondary" href="${ctx}/customers"><span class="icon icon--sm icon--arrow-left" aria-hidden="true"></span>Quay lại danh sách</a>
          </div>
        </div>

        <c:if test="${customer.locked}">
          <div class="alert alert--danger alert--icon" role="alert">
            <span class="icon icon--sm icon--lock-keyhole" aria-hidden="true"></span>
            <span>
              Giao dịch với đại lý này đang bị khoá, không tạo được đơn mới (đơn đang dở vẫn xử lý tiếp được).
              <c:if test="${not empty customer.lockReason}"><c:out value=" Lý do: ${customer.lockReason}."/></c:if>
              <c:if test="${not empty customer.lockedByName}"><c:out value=" Khoá bởi ${customer.lockedByName} lúc ${customer.lockedAtText}."/></c:if>
            </span>
          </div>
        </c:if>

        <section class="card">
          <div class="card__header"><h2>Thông tin đại lý</h2></div>
          <dl class="card__body info-list">
            <div><dt>Mã đại lý</dt><dd><c:out value="${customer.code}"/></dd></div>
            <div><dt>Tên đại lý</dt><dd><c:out value="${customer.name}"/></dd></div>
            <div><dt>Mã số thuế</dt><dd><c:out value="${empty customer.taxCode ? '—' : customer.taxCode}"/></dd></div>
            <div><dt>Số điện thoại</dt><dd><c:out value="${empty customer.phone ? '—' : customer.phone}"/></dd></div>
            <div><dt>Nhóm khách hàng</dt><dd><c:out value="${empty customer.groupName ? '—' : customer.groupName}"/></dd></div>
            <div><dt>Khu vực</dt><dd><c:out value="${empty customer.areaName ? '—' : customer.areaName}"/></dd></div>
            <div><dt>Người phụ trách</dt><dd><c:out value="${empty customer.salesRepName ? 'Chưa phân công' : customer.salesRepName}"/></dd></div>
          </dl>
        </section>

        <section class="card credit-card" id="lich-su-khoa">
          <div class="card__header"><h2>Lịch sử khoá / mở giao dịch</h2></div>
          <div class="credit-history">
            <c:if test="${empty lockHistory}"><p class="text-muted credit-history__empty">Chưa khoá hay mở giao dịch lần nào.</p></c:if>
            <c:forEach var="lc" items="${lockHistory}">
              <article class="credit-change">
                <p class="credit-change__what"><strong><c:out value="${lc.actionLabel}"/></strong></p>
                <c:if test="${not empty lc.reason}"><p class="credit-change__reason">Lý do: <c:out value="${lc.reason}"/></p></c:if>
                <p class="credit-change__meta"><c:out value="${lc.changedByName}"/> · <c:out value="${lc.timeText}"/></p>
              </article>
            </c:forEach>
          </div>
        </section>

        <c:if test="${canManageCredit}">
          <c:choose>
            <c:when test="${customer.locked}">
              <dialog class="dialog" id="trade-toggle" aria-labelledby="trade-toggle-title">
                <form action="${ctx}/customers/unlock" method="post">
                  <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                  <input type="hidden" name="customerId" value="${customer.id}">
                  <div class="dialog__header"><h2 id="trade-toggle-title">Mở lại giao dịch với <c:out value="${customer.name}"/></h2></div>
                  <div class="dialog__body dp-form">
                    <p>Đại lý sẽ tạo được đơn mới trở lại.</p>
                    <div class="form-field">
                      <label class="form-label" for="unlock-reason">Lý do mở lại</label>
                      <textarea class="textarea" id="unlock-reason" name="reason" maxlength="500" placeholder="Ví dụ: đã thanh toán đủ công nợ"></textarea>
                    </div>
                  </div>
                  <div class="dialog__footer">
                    <button class="btn btn--secondary" type="button" data-dialog-close>Huỷ</button>
                    <button class="btn btn--primary" type="submit">Mở giao dịch</button>
                  </div>
                </form>
              </dialog>
            </c:when>
            <c:otherwise>
              <dialog class="dialog" id="trade-toggle" aria-labelledby="trade-toggle-title">
                <form action="${ctx}/customers/lock" method="post">
                  <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                  <input type="hidden" name="customerId" value="${customer.id}">
                  <div class="dialog__header"><h2 id="trade-toggle-title">Khoá giao dịch với <c:out value="${customer.name}"/></h2></div>
                  <div class="dialog__body dp-form">
                    <p>Đại lý sẽ không tạo được đơn mới, kể cả trên cổng đặt hàng. Đơn đang dở vẫn xử lý tiếp được nhưng có cảnh báo.</p>
                    <div class="form-field">
                      <label class="form-label" for="lock-reason">Lý do khoá <span class="required">*</span></label>
                      <textarea class="textarea" id="lock-reason" name="reason" maxlength="500" required></textarea>
                    </div>
                  </div>
                  <div class="dialog__footer">
                    <button class="btn btn--secondary" type="button" data-dialog-close>Huỷ</button>
                    <button class="btn btn--danger" type="submit">Khoá giao dịch</button>
                  </div>
                </form>
              </dialog>
            </c:otherwise>
          </c:choose>
        </c:if>

        <section class="card credit-card" id="lich-su-phan-cong">
          <div class="card__header"><h2>Lịch sử phân công</h2></div>
          <div class="credit-history">
            <c:if test="${empty assignments}"><p class="text-muted credit-history__empty">Chưa có thay đổi người phụ trách nào.</p></c:if>
            <c:forEach var="ch" items="${assignments}">
              <article class="credit-change">
                <p class="credit-change__what">
                  <c:out value="${ch.fromLabel}"/> <span aria-hidden="true">→</span><span class="visually-hidden">sang</span> <strong><c:out value="${ch.toLabel}"/></strong>
                </p>
                <c:if test="${not empty ch.reason}"><p class="credit-change__reason">Lý do: <c:out value="${ch.reason}"/></p></c:if>
                <p class="credit-change__meta"><c:out value="${ch.changedByName}"/> · <c:out value="${ch.timeText}"/></p>
              </article>
            </c:forEach>
          </div>
        </section>

        <section class="card credit-card" id="han-muc-cong-no">
          <div class="card__header dp-card__header">
            <div>
              <h2>Hạn mức công nợ</h2>
              <p>Kiểm tra ngay khi chốt đơn: đơn vượt hạn mức hoặc quá số ngày nợ phải qua duyệt.</p>
            </div>
            <c:if test="${canManageCredit}">
              <button class="btn btn--secondary btn--sm" type="button" data-dialog-open="credit-edit">Sửa hạn mức</button>
            </c:if>
          </div>
          <div class="credit-stats">
            <div class="credit-stat">
              <p class="credit-stat__label">Hạn mức nợ tối đa</p>
              <p class="credit-stat__value">
                <c:choose>
                  <c:when test="${customer.creditLimit.signum() == 0}">Không cho nợ</c:when>
                  <c:otherwise><fmt:formatNumber value="${customer.creditLimit}" maxFractionDigits="2"/> ₫</c:otherwise>
                </c:choose>
              </p>
            </div>
            <div class="credit-stat">
              <p class="credit-stat__label">Số ngày nợ tối đa</p>
              <p class="credit-stat__value">
                <c:choose>
                  <c:when test="${customer.creditDays == 0}">Thanh toán ngay</c:when>
                  <c:otherwise>${customer.creditDays} ngày</c:otherwise>
                </c:choose>
              </p>
            </div>
          </div>
          <div class="credit-history">
            <h3>Lịch sử thay đổi</h3>
            <c:if test="${empty creditHistory}"><p class="text-muted credit-history__empty">Chưa có thay đổi nào.</p></c:if>
            <c:forEach var="ch" items="${creditHistory}">
              <article class="credit-change">
                <p class="credit-change__what">
                  <fmt:formatNumber value="${ch.oldLimit}" maxFractionDigits="2"/> ₫ / ${ch.oldDays} ngày
                  <span aria-hidden="true">→</span><span class="visually-hidden">thành</span>
                  <strong><fmt:formatNumber value="${ch.newLimit}" maxFractionDigits="2"/> ₫ / ${ch.newDays} ngày</strong>
                </p>
                <p class="credit-change__reason">Lý do: <c:out value="${ch.reason}"/></p>
                <p class="credit-change__meta"><c:out value="${ch.changedByName}"/> · <c:out value="${ch.changedAt.toString().replace('T', ' ').substring(0, 16)}"/></p>
              </article>
            </c:forEach>
          </div>
        </section>

        <c:if test="${canManageCredit}">
          <dialog class="dialog" id="credit-edit" aria-labelledby="credit-edit-title">
            <form action="${ctx}/customers/credit/save" method="post">
              <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
              <input type="hidden" name="customerId" value="${customer.id}">
              <div class="dialog__header"><h2 id="credit-edit-title">Sửa hạn mức công nợ</h2></div>
              <div class="dialog__body dp-form">
                <div class="form-card__row">
                  <div class="form-field">
                    <label class="form-label" for="credit-limit">Hạn mức nợ tối đa (₫) <span class="required">*</span></label>
                    <input class="input" id="credit-limit" name="creditLimit" inputmode="decimal" required aria-describedby="credit-limit-hint"
                           value="${customer.creditLimit.stripTrailingZeros().toPlainString()}">
                    <p class="form-hint" id="credit-limit-hint">Nhập 0 nếu không cho nợ.</p>
                  </div>
                  <div class="form-field">
                    <label class="form-label" for="credit-days">Số ngày nợ tối đa <span class="required">*</span></label>
                    <input class="input" id="credit-days" name="creditDays" inputmode="numeric" required aria-describedby="credit-days-hint"
                           value="${customer.creditDays}">
                    <p class="form-hint" id="credit-days-hint">Từ 0 đến 365 ngày.</p>
                  </div>
                </div>
                <div class="form-field">
                  <label class="form-label" for="credit-reason">Lý do thay đổi <span class="required">*</span></label>
                  <textarea class="textarea" id="credit-reason" name="reason" maxlength="500" required></textarea>
                  <p class="form-hint">Lý do được lưu vào lịch sử và nhật ký thao tác, không sửa hay xoá được.</p>
                </div>
              </div>
              <div class="dialog__footer">
                <button class="btn btn--secondary" type="button" data-dialog-close>Huỷ</button>
                <button class="btn btn--primary" type="submit">Lưu hạn mức</button>
              </div>
            </form>
          </dialog>
        </c:if>

        <section class="card dp-card" id="diem-giao-hang">
          <div class="card__header dp-card__header">
            <div>
              <h2>Điểm giao hàng</h2>
              <p>Đơn hàng chọn điểm giao trong danh sách này; một điểm được đặt làm mặc định.</p>
            </div>
            <c:if test="${canEditPoints}">
              <button class="btn btn--primary btn--sm" type="button" data-dialog-open="dp-new"><span class="icon icon--sm icon--plus" aria-hidden="true"></span>Thêm điểm giao</button>
            </c:if>
          </div>
          <div class="dp-list">
            <c:if test="${empty points}">
              <p class="dp-empty text-muted">Đại lý chưa có điểm giao hàng nào.</p>
            </c:if>
            <c:forEach var="pt" items="${points}">
              <article class="dp-item">
                <div class="dp-item__main">
                  <p class="dp-item__address">
                    <c:out value="${pt.address}"/>
                    <c:if test="${pt.defaultPoint}"><span class="badge badge--primary">Mặc định</span></c:if>
                  </p>
                  <p class="dp-item__receiver"><c:out value="${pt.receiverName} · ${pt.receiverPhone}"/></p>
                  <c:if test="${not empty pt.routeNote}"><p class="dp-item__note"><c:out value="${pt.routeNote}"/></p></c:if>
                </div>
                <c:if test="${canEditPoints}">
                  <details class="row-menu">
                    <summary aria-label="Thao tác với điểm giao <c:out value='${pt.address}'/>"><span class="icon icon--md icon--ellipsis" aria-hidden="true"></span></summary>
                    <div class="row-menu__panel">
                      <button type="button" data-dialog-open="dp-edit-${pt.id}">Sửa</button>
                      <c:if test="${not pt.defaultPoint}">
                        <form action="${ctx}/customers/delivery-points/default" method="post">
                          <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                          <input type="hidden" name="customerId" value="${customer.id}">
                          <input type="hidden" name="pointId" value="${pt.id}">
                          <button type="submit">Đặt làm mặc định</button>
                        </form>
                      </c:if>
                      <button type="button" class="is-danger" data-dialog-open="dp-delete-${pt.id}">Xoá</button>
                    </div>
                  </details>
                </c:if>
              </article>
            </c:forEach>
          </div>
        </section>

        <c:if test="${canEditPoints}">
          <%-- Một hộp thoại cho thêm mới; mỗi điểm có hộp thoại sửa và xoá riêng --%>
          <dialog class="dialog" id="dp-new" aria-labelledby="dp-new-title">
            <form action="${ctx}/customers/delivery-points/save" method="post">
              <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
              <input type="hidden" name="customerId" value="${customer.id}">
              <input type="hidden" name="pointId" value="0">
              <div class="dialog__header"><h2 id="dp-new-title">Thêm điểm giao hàng</h2></div>
              <div class="dialog__body dp-form">
                <div class="form-field">
                  <label class="form-label" for="dp-new-address">Địa chỉ giao hàng <span class="required">*</span></label>
                  <input class="input" id="dp-new-address" name="address" maxlength="255" required>
                </div>
                <div class="form-card__row">
                  <div class="form-field">
                    <label class="form-label" for="dp-new-receiver">Người nhận <span class="required">*</span></label>
                    <input class="input" id="dp-new-receiver" name="receiverName" maxlength="150" required>
                  </div>
                  <div class="form-field">
                    <label class="form-label" for="dp-new-phone">Số điện thoại <span class="required">*</span></label>
                    <input class="input" id="dp-new-phone" name="receiverPhone" type="tel" maxlength="20" required>
                  </div>
                </div>
                <div class="form-field">
                  <label class="form-label" for="dp-new-note">Ghi chú đường đi</label>
                  <textarea class="textarea" id="dp-new-note" name="routeNote" maxlength="500"></textarea>
                </div>
                <label class="choice"><input type="checkbox" name="makeDefault"> <span class="choice__title">Đặt làm điểm giao mặc định</span></label>
              </div>
              <div class="dialog__footer">
                <button class="btn btn--secondary" type="button" data-dialog-close>Huỷ</button>
                <button class="btn btn--primary" type="submit">Thêm điểm giao</button>
              </div>
            </form>
          </dialog>

          <c:forEach var="pt" items="${points}">
            <dialog class="dialog" id="dp-edit-${pt.id}" aria-labelledby="dp-edit-title-${pt.id}">
              <form action="${ctx}/customers/delivery-points/save" method="post">
                <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                <input type="hidden" name="customerId" value="${customer.id}">
                <input type="hidden" name="pointId" value="${pt.id}">
                <div class="dialog__header"><h2 id="dp-edit-title-${pt.id}">Sửa điểm giao hàng</h2></div>
                <div class="dialog__body dp-form">
                  <div class="form-field">
                    <label class="form-label" for="dp-address-${pt.id}">Địa chỉ giao hàng <span class="required">*</span></label>
                    <input class="input" id="dp-address-${pt.id}" name="address" maxlength="255" required value="<c:out value='${pt.address}'/>">
                  </div>
                  <div class="form-card__row">
                    <div class="form-field">
                      <label class="form-label" for="dp-receiver-${pt.id}">Người nhận <span class="required">*</span></label>
                      <input class="input" id="dp-receiver-${pt.id}" name="receiverName" maxlength="150" required value="<c:out value='${pt.receiverName}'/>">
                    </div>
                    <div class="form-field">
                      <label class="form-label" for="dp-phone-${pt.id}">Số điện thoại <span class="required">*</span></label>
                      <input class="input" id="dp-phone-${pt.id}" name="receiverPhone" type="tel" maxlength="20" required value="<c:out value='${pt.receiverPhone}'/>">
                    </div>
                  </div>
                  <div class="form-field">
                    <label class="form-label" for="dp-note-${pt.id}">Ghi chú đường đi</label>
                    <textarea class="textarea" id="dp-note-${pt.id}" name="routeNote" maxlength="500"><c:out value="${pt.routeNote}"/></textarea>
                  </div>
                  <c:if test="${not pt.defaultPoint}">
                    <label class="choice"><input type="checkbox" name="makeDefault"> <span class="choice__title">Đặt làm điểm giao mặc định</span></label>
                  </c:if>
                </div>
                <div class="dialog__footer">
                  <button class="btn btn--secondary" type="button" data-dialog-close>Huỷ</button>
                  <button class="btn btn--primary" type="submit">Lưu thay đổi</button>
                </div>
              </form>
            </dialog>

            <dialog class="dialog" id="dp-delete-${pt.id}" aria-labelledby="dp-delete-title-${pt.id}">
              <form action="${ctx}/customers/delivery-points/delete" method="post">
                <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                <input type="hidden" name="customerId" value="${customer.id}">
                <input type="hidden" name="pointId" value="${pt.id}">
                <div class="dialog__header"><h2 id="dp-delete-title-${pt.id}">Xoá điểm giao hàng?</h2></div>
                <div class="dialog__body">
                  <p><c:out value="${pt.address}"/></p>
                  <c:if test="${pt.defaultPoint}"><p class="form-hint mt-4">Đây là điểm mặc định; điểm cũ nhất còn lại sẽ thành mặc định.</p></c:if>
                </div>
                <div class="dialog__footer">
                  <button class="btn btn--secondary" type="button" data-dialog-close>Huỷ</button>
                  <button class="btn btn--danger" type="submit">Xoá</button>
                </div>
              </form>
            </dialog>
          </c:forEach>
        </c:if>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
