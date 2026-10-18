<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="pageTitle" value="${order.draft ? 'Đơn nháp' : 'Đơn hàng'} ${order.code}"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

        <div class="page-header">
          <div>
            <p class="breadcrumb"><a href="${ctx}/orders">Đơn hàng</a> / <c:out value="${order.code}"/></p>
            <h1><c:out value="${pageTitle}"/></h1>
            <p class="page-header__desc"><a href="${ctx}/customers/view?id=${order.customerId}"><c:out value="${order.customerName}"/></a> · <c:out value="${order.customerCode} · ${order.customerGroupName}"/></p>
          </div>
          <div class="page-header__actions">
            <a class="btn btn--secondary" href="${ctx}/orders/pdf?id=${order.id}" target="_blank" rel="noopener">In / xem PDF</a>
            <a class="btn btn--secondary" href="${ctx}/orders/pdf?id=${order.id}&amp;download=true">Tải PDF</a>
            <c:if test="${canCopyOrder}"><button class="btn btn--secondary" type="button" data-dialog-open="copy-order">Sao chép đơn</button></c:if>
            <span class="badge${empty order.status.tone ? '' : ' badge--'.concat(order.status.tone)}"><c:out value="${order.status.label}"/></span>
            <c:choose>
              <c:when test="${order.draft and empty history}">
                <button class="btn btn--secondary" type="button" data-dialog-open="delete-order">Xoá nháp</button>
              </c:when>
              <c:when test="${order.cancellable}">
                <button class="btn btn--secondary" type="button" data-dialog-open="cancel-order">Huỷ đơn</button>
              </c:when>
            </c:choose>
            <c:if test="${order.draft and not empty order.lines}">
              <button class="btn btn--primary" type="submit" form="submit-order">Chốt đơn</button>
            </c:if>
          </div>
        </div>

        <c:if test="${order.customerLocked}">
          <div class="alert alert--danger alert--icon" role="alert">
            <span class="icon icon--sm icon--lock-keyhole" aria-hidden="true"></span>
            <span>Đại lý này đang bị khoá giao dịch<c:if test="${not empty order.customerLockReason}"><c:out value=" (${order.customerLockReason})"/></c:if>. Bạn vẫn gõ tiếp được đơn nháp này nhưng không tạo được đơn mới cho đại lý.</span>
          </div>
        </c:if>

        <c:if test="${order.status.code == 'PENDING_APPROVAL' and not empty order.approvalReason}">
          <div class="alert alert--warning alert--icon" role="alert">
            <span class="icon icon--sm icon--info" aria-hidden="true"></span>
            <span>Đơn đang chờ duyệt: <c:out value="${order.approvalReason}"/></span>
          </div>
        </c:if>
        <%-- Đơn bị trả lại hoặc từ chối: hiện ý kiến của lần quyết định gần nhất để người gõ đơn biết phải sửa gì --%>
        <c:forEach var="ev" items="${history}" varStatus="loop">
          <c:if test="${loop.last and not empty ev.opinion and (order.status.code == 'REJECTED' or (order.draft and ev.actionLabel == 'Trả lại sửa'))}">
            <div class="alert ${order.draft ? 'alert--info' : 'alert--danger'} alert--icon" role="alert">
              <span class="icon icon--sm icon--info" aria-hidden="true"></span>
              <span><c:out value="${ev.actionLabel}"/> bởi <c:out value="${ev.actorName}"/>: <c:out value="${ev.opinion}"/></span>
            </div>
          </c:if>
        </c:forEach>

        <c:if test="${order.draft}">
        <c:if test="${order.unpricedLines > 0}">
          <div class="alert alert--warning alert--icon" role="alert">
            <span class="icon icon--sm icon--info" aria-hidden="true"></span>
            <span><c:out value="${order.unpricedLines}"/> dòng hàng chưa có giá trong bảng giá đang hiệu lực của nhóm <c:out value="${order.customerGroupName}"/>, nên chưa được tính vào tổng tiền. Hãy báo Quản lý kinh doanh bổ sung bảng giá hoặc xoá dòng đó.</span>
          </div>
        </c:if>

        <c:if test="${order.requiresApproval}">
          <div class="alert alert--warning alert--icon" role="alert">
            <span class="icon icon--sm icon--info" aria-hidden="true"></span>
            <span>Đơn hiện có dòng hàng có thành tiền sau chiết khấu thấp hơn giá sàn. Khi chốt đơn, đơn sẽ cần Quản lý kinh doanh duyệt.</span>
          </div>
        </c:if>

        <section class="card">
          <div class="card__header">
            <h2>Giao hàng</h2>
            <p>Có thể để trống khi còn nháp, nhưng cần đủ trước khi chốt đơn.</p>
          </div>
          <form action="${ctx}/orders/save" method="post" novalidate>
            <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
            <input type="hidden" name="id" value="${order.id}">
            <div class="card__body form-grid">
              <div class="form-field">
                <label class="form-label" for="point">Điểm giao hàng</label>
                <select class="select" id="point" name="deliveryPointId">
                  <option value="">— Chưa chọn —</option>
                  <c:forEach var="pt" items="${points}">
                    <option value="${pt.id}" ${pt.id == order.deliveryPointId ? 'selected' : ''}><c:out value="${pt.receiverName} – ${pt.address}"/></option>
                  </c:forEach>
                </select>
                <c:if test="${empty points}">
                  <p class="form-hint">Đại lý chưa có điểm giao hàng. Thêm trong <a href="${ctx}/customers/view?id=${order.customerId}">hồ sơ đại lý</a>.</p>
                </c:if>
              </div>
              <div class="form-field">
                <label class="form-label" for="date">Ngày giao mong muốn</label>
                <input class="input" id="date" name="deliveryDate" type="date" min="${today}" value="${order.deliveryDate}">
              </div>
              <div class="form-field order-note">
                <label class="form-label" for="note">Ghi chú</label>
                <textarea class="textarea" id="note" name="note" maxlength="500" rows="2"><c:out value="${order.note}"/></textarea>
              </div>
            </div>
            <div class="form-actions form-actions--start order-actions">
              <button class="btn btn--primary" type="submit">Lưu nháp</button>
            </div>
          </form>
        </section>

        <section class="card" id="them-hang">
          <div class="card__header">
            <h2>Thêm hàng</h2>
            <p>Tìm theo mã hoặc tên sản phẩm, chọn đơn vị tính và số lượng.</p>
          </div>
          <div class="card__body">
            <form class="toolbar order-search" action="${ctx}/orders/edit#them-hang" method="get" role="search">
              <input type="hidden" name="id" value="${order.id}">
              <label class="visually-hidden" for="pq">Tìm sản phẩm</label>
              <div class="search-field toolbar__search">
                <span class="icon icon--search" aria-hidden="true"></span>
                <input id="pq" name="q" type="search" placeholder="Mã hoặc tên sản phẩm" autocomplete="off" data-live-search="product-results" value="<c:out value='${q}'/>">
              </div>
              <button class="btn btn--secondary toolbar__submit toolbar__submit--show" type="submit">Tìm</button>
            </form>

            <div id="product-results" data-url="${ctx}/orders/products?id=${order.id}">
<%@ include file="/WEB-INF/views/orders/product-results.jspf" %>
            </div>
          </div>
        </section>
        </c:if>

        <c:if test="${not order.draft}">
        <section class="card">
          <div class="card__header">
            <h2>Giao hàng</h2>
          </div>
          <dl class="card__body info-list">
            <div><dt>Điểm giao hàng</dt><dd><c:out value="${empty order.deliveryPointText ? 'Chưa chọn' : order.deliveryPointText}"/></dd></div>
            <div><dt>Ngày giao mong muốn</dt><dd><c:out value="${empty order.deliveryDateText ? 'Chưa chọn' : order.deliveryDateText}"/></dd></div>
            <c:if test="${not empty order.note}"><div><dt>Ghi chú</dt><dd><c:out value="${order.note}"/></dd></div></c:if>
          </dl>
        </section>
        </c:if>

        <section class="card" id="dong-hang">
          <div class="card__header">
            <h2>Dòng hàng (<c:out value="${order.lines.size()}"/>)</h2>
            <p><c:out value="${order.draft ? 'Giá theo bảng giá hiện hành của nhóm '.concat(order.customerGroupName).concat('. Chiết khấu tính trên tổng số lượng cùng sản phẩm trong đơn.') : 'Giá và chiết khấu đã chốt lúc gửi đơn, không đổi theo bảng giá sau này.'}"/></p>
          </div>
          <dl class="order-totals">
            <div><dt>Tổng tiền hàng</dt><dd><fmt:formatNumber value="${order.grossTotal}" maxFractionDigits="2"/> ₫</dd></div>
            <div><dt>Chiết khấu</dt><dd class="order-totals__discount">− <fmt:formatNumber value="${order.discountTotal}" maxFractionDigits="2"/> ₫</dd></div>
            <div class="order-totals__payable"><dt>Tổng phải thu</dt><dd><fmt:formatNumber value="${order.payable}" maxFractionDigits="2"/> ₫</dd></div>
          </dl>

          <div class="card__body order-lines">
            <c:if test="${empty order.lines}">
              <p class="text-muted">Chưa có dòng hàng nào. Tìm sản phẩm ở phần "Thêm hàng" phía trên.</p>
            </c:if>
            <c:forEach var="ln" items="${order.lines}">
              <article class="order-line">
                <div class="order-line__head">
                  <p class="order-line__name"><c:out value="${ln.productName}"/></p>
                  <p class="order-line__meta"><c:out value="${ln.sku}"/><c:if test="${ln.priced}"> · <fmt:formatNumber value="${ln.unitPrice}" maxFractionDigits="2"/> ₫/<c:out value="${ln.unitName}"/></c:if><c:if test="${ln.manualPrice}"> · <span class="badge badge--warning">Giá chỉnh tay</span></c:if></p>
                </div>
                <c:choose>
                  <c:when test="${order.draft}">
                <form class="order-line__form" action="${ctx}/orders/lines/save" method="post">
                  <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                  <input type="hidden" name="id" value="${order.id}">
                  <input type="hidden" name="lineId" value="${ln.id}">
                  <label class="visually-hidden" for="lq-${ln.id}">Số lượng</label>
                  <input class="input order-qty" id="lq-${ln.id}" name="quantity" type="number" min="1" step="1" inputmode="numeric" value="${ln.quantity}" data-order-qty-autosubmit required>
                  <label class="visually-hidden" for="lu-${ln.id}">Đơn vị tính</label>
                  <select class="select" id="lu-${ln.id}" name="unitId">
                    <c:forEach var="u" items="${unitOptions[ln.productId]}"><option value="${u.id}" ${u.id == ln.unitId ? 'selected' : ''}><c:out value="${u.name}"/></option></c:forEach>
                  </select>
                  <label class="visually-hidden" for="lp-${ln.id}">Giá chỉnh thủ công theo <c:out value="${ln.baseUnitName}"/></label>
                  <input class="input" id="lp-${ln.id}" name="manualPrice" type="number" min="0" step="0.01" inputmode="decimal" value="${ln.manualBasePrice}" placeholder="Giá tự động / ${ln.baseUnitName}">
                  <button class="btn btn--secondary" type="submit">Cập nhật</button>
                </form>
                <p class="order-line__meta">Giá chỉnh thủ công nhập theo <c:out value="${ln.baseUnitName}"/>. Để trống để dùng lại giá tự động.</p>
                  </c:when>
                  <c:otherwise>
                <p class="order-line__meta"><c:out value="${ln.quantity} ${ln.unitName}"/></p>
                  </c:otherwise>
                </c:choose>
                <c:if test="${ln.converted}">
                  <fmt:formatNumber var="baseQtyText" value="${ln.baseQuantity}" maxFractionDigits="2"/>
                  <p class="order-line__meta"><c:out value="= ${baseQtyText} ${ln.baseUnitName}"/></p>
                </c:if>
                <div class="order-line__money">
                  <c:choose>
                    <c:when test="${ln.priced}">
                      <p>Tiền hàng <strong><fmt:formatNumber value="${ln.grossAmount}" maxFractionDigits="2"/> ₫</strong></p>
                      <c:if test="${ln.discounted}">
                        <p class="order-line__discount">Chiết khấu − <fmt:formatNumber value="${ln.discountAmount}" maxFractionDigits="2"/> ₫ <span class="order-line__meta">(<c:out value="${ln.discountNote}"/>)</span></p>
                      </c:if>
                      <p>Thành tiền <strong><fmt:formatNumber value="${ln.netAmount}" maxFractionDigits="2"/> ₫</strong></p>
                    </c:when>
                    <c:otherwise><p><span class="badge badge--warning">Chưa có giá</span></p></c:otherwise>
                  </c:choose>
                </div>
                <c:if test="${order.draft}">
                <form class="order-line__remove" action="${ctx}/orders/lines/delete" method="post">
                  <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                  <input type="hidden" name="id" value="${order.id}">
                  <input type="hidden" name="lineId" value="${ln.id}">
                  <button class="btn btn--ghost btn--sm" type="submit" aria-label="Xoá dòng <c:out value='${ln.sku}'/>">Xoá dòng</button>
                </form>
                </c:if>
              </article>
            </c:forEach>
          </div>
        </section>

        <c:if test="${order.draft}">
        <section class="card" id="chot-don">
          <div class="card__header">
            <h2>Chốt đơn</h2>
            <p>Hệ thống kiểm hạn mức công nợ, tồn khả dụng và giá sàn ngay lúc chốt. Đơn hợp lệ được duyệt và giữ hàng luôn; đơn vượt hạn mức hoặc dưới giá sàn chuyển sang chờ Quản lý kinh doanh duyệt.</p>
          </div>
          <form class="card__body" id="submit-order" action="${ctx}/orders/submit" method="post">
            <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
            <input type="hidden" name="id" value="${order.id}">
            <button class="btn btn--primary" type="submit" ${empty order.lines ? 'disabled' : ''}>Chốt đơn</button>
            <c:if test="${empty order.lines}"><p class="form-hint">Thêm ít nhất một dòng hàng trước khi chốt.</p></c:if>
          </form>
        </section>
        </c:if>

        <c:if test="${canDecide}">
        <section class="card" id="duyet-don">
          <div class="card__header">
            <h2>Duyệt đơn</h2>
            <p>Từ chối hoặc trả lại sửa bắt buộc nhập ý kiến. Đơn được duyệt mới giữ hàng và chờ xuất kho.</p>
          </div>
          <form class="card__body" action="${ctx}/orders/decide" method="post">
            <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
            <input type="hidden" name="id" value="${order.id}">
            <div class="form-field">
              <label class="form-label" for="opinion">Ý kiến</label>
              <textarea class="textarea" id="opinion" name="opinion" maxlength="500" rows="2"></textarea>
            </div>
            <div class="form-actions form-actions--start">
              <button class="btn btn--primary" type="submit" name="action" value="APPROVE">Duyệt</button>
              <button class="btn btn--secondary" type="submit" name="action" value="RETURN">Trả lại sửa</button>
              <button class="btn btn--danger" type="submit" name="action" value="REJECT">Từ chối</button>
            </div>
          </form>
        </section>
        </c:if>

        <c:if test="${not empty history}">
        <section class="card">
          <div class="card__header">
            <h2>Lịch sử đơn</h2>
            <p>Ghi lại mọi lần chốt, duyệt, trả lại và huỷ; không sửa hay xoá được.</p>
          </div>
          <div class="card__body">
            <ul class="home-activity">
              <c:forEach var="ev" items="${history}">
                <li class="home-activity__item">
                  <span class="home-activity__icon"><span class="icon icon--sm icon--history" aria-hidden="true"></span></span>
                  <div class="home-activity__body">
                    <p class="home-activity__title"><c:out value="${ev.actionLabel} → ${ev.toLabel}"/><c:if test="${not empty ev.opinion}"> — <c:out value="${ev.opinion}"/></c:if></p>
                    <p class="home-activity__meta"><c:out value="${empty ev.actorName ? 'Hệ thống' : ev.actorName}"/> • <c:out value="${ev.atText}"/></p>
                  </div>
                </li>
              </c:forEach>
            </ul>
          </div>
        </section>
        </c:if>

        <dialog class="dialog" id="delete-order" aria-labelledby="delete-order-title">
          <form action="${ctx}/orders/delete" method="post">
            <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
            <input type="hidden" name="id" value="${order.id}">
            <div class="dialog__header"><h2 id="delete-order-title">Xoá đơn nháp <c:out value="${order.code}"/>?</h2></div>
            <div class="dialog__body"><p>Các dòng hàng đã gõ sẽ mất, không khôi phục được.</p></div>
            <div class="dialog__footer">
              <button class="btn btn--secondary" type="button" data-dialog-close>Huỷ</button>
              <button class="btn btn--danger" type="submit">Xoá</button>
            </div>
          </form>
        </dialog>

        <dialog class="dialog" id="cancel-order" aria-labelledby="cancel-order-title">
          <form action="${ctx}/orders/cancel" method="post">
            <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
            <input type="hidden" name="id" value="${order.id}">
            <div class="dialog__header"><h2 id="cancel-order-title">Huỷ đơn <c:out value="${order.code}"/>?</h2></div>
            <div class="dialog__body">
              <p>Hàng và hạn mức đang giữ cho đơn sẽ được nhả. Đơn đã huỷ không mở lại được.</p>
              <div class="form-field">
                <label class="form-label" for="reason">Lý do huỷ</label>
                <textarea class="textarea" id="reason" name="reason" maxlength="500" rows="2" required></textarea>
              </div>
            </div>
            <div class="dialog__footer">
              <button class="btn btn--secondary" type="button" data-dialog-close>Đóng</button>
              <button class="btn btn--danger" type="submit">Huỷ đơn</button>
            </div>
          </form>
        </dialog>

<c:if test="${canCopyOrder}">
<dialog class="dialog" id="copy-order" aria-labelledby="copy-order-title">
  <form method="post" action="${ctx}/orders/copy">
    <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
    <input type="hidden" name="id" value="${order.id}">
    <header class="dialog__header"><h2 id="copy-order-title">Sao chép đơn <c:out value="${order.code}"/>?</h2></header>
    <div class="dialog__body"><p>Tạo một đơn nháp mới cho cùng đại lý, giữ các mặt hàng và số lượng. Giá, chiết khấu được tính lại theo chính sách hiện hành. Ngày giao đã qua được bỏ trống.</p></div>
    <footer class="dialog__footer"><button class="btn btn--secondary" type="button" data-dialog-close>Hủy</button><button class="btn btn--primary" type="submit">Tạo đơn nháp mới</button></footer>
  </form>
</dialog>
</c:if>
<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
