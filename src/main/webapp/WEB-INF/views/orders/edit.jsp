<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="pageTitle" value="Đơn nháp ${order.code}"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

        <div class="page-header">
          <div>
            <p class="breadcrumb"><a href="${ctx}/orders">Đơn hàng</a> / <c:out value="${order.code}"/></p>
            <h1>Đơn nháp <c:out value="${order.code}"/></h1>
            <p class="page-header__desc"><a href="${ctx}/customers/view?id=${order.customerId}"><c:out value="${order.customerName}"/></a> · <c:out value="${order.customerCode} · ${order.customerGroupName}"/></p>
          </div>
          <div class="page-header__actions">
            <span class="badge">Nháp</span>
            <button class="btn btn--secondary" type="button" data-dialog-open="delete-order">Xoá nháp</button>
          </div>
        </div>

        <c:if test="${order.customerLocked}">
          <div class="alert alert--danger alert--icon" role="alert">
            <span class="icon icon--sm icon--lock-keyhole" aria-hidden="true"></span>
            <span>Đại lý này đang bị khoá giao dịch<c:if test="${not empty order.customerLockReason}"><c:out value=" (${order.customerLockReason})"/></c:if>. Bạn vẫn gõ tiếp được đơn nháp này nhưng không tạo được đơn mới cho đại lý.</span>
          </div>
        </c:if>
        <c:if test="${order.unpricedLines > 0}">
          <div class="alert alert--warning alert--icon" role="alert">
            <span class="icon icon--sm icon--info" aria-hidden="true"></span>
            <span><c:out value="${order.unpricedLines}"/> dòng hàng chưa có giá trong bảng giá đang hiệu lực của nhóm <c:out value="${order.customerGroupName}"/>, nên chưa được tính vào tổng tiền. Hãy báo Quản lý kinh doanh bổ sung bảng giá hoặc xoá dòng đó.</span>
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
                <input id="pq" name="q" type="search" placeholder="Mã hoặc tên sản phẩm" value="<c:out value='${q}'/>">
              </div>
              <button class="btn btn--secondary toolbar__submit" type="submit">Tìm</button>
            </form>

            <c:if test="${not empty q and empty choices}">
              <p class="text-muted">Không có sản phẩm đang kinh doanh nào khớp "<c:out value="${q}"/>".</p>
            </c:if>
            <c:forEach var="ch" items="${choices}">
              <form class="order-add" action="${ctx}/orders/lines/add" method="post">
                <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                <input type="hidden" name="id" value="${order.id}">
                <input type="hidden" name="productId" value="${ch.productId}">
                <div class="order-add__info">
                  <p class="order-add__name"><c:out value="${ch.name}"/></p>
                  <p class="order-add__meta">
                    <c:out value="${ch.sku}"/> ·
                    <c:choose>
                      <c:when test="${empty ch.basePrice}"><span class="badge badge--warning">Chưa có giá</span></c:when>
                      <c:otherwise><fmt:formatNumber value="${ch.basePrice}" maxFractionDigits="2"/> ₫/<c:out value="${ch.baseUnitName}"/></c:otherwise>
                    </c:choose>
                  </p>
                </div>
                <div class="order-add__controls">
                  <label class="visually-hidden" for="au-${ch.productId}">Đơn vị tính</label>
                  <select class="select" id="au-${ch.productId}" name="unitId">
                    <c:forEach var="u" items="${ch.units}"><option value="${u.id}"><c:out value="${u.name}"/></option></c:forEach>
                  </select>
                  <label class="visually-hidden" for="aq-${ch.productId}">Số lượng</label>
                  <input class="input order-qty" id="aq-${ch.productId}" name="quantity" type="number" min="1" step="1" inputmode="numeric" value="1" required>
                  <button class="btn btn--primary" type="submit" ${empty ch.basePrice ? 'disabled' : ''}>Thêm</button>
                </div>
              </form>
            </c:forEach>
          </div>
        </section>

        <section class="card" id="dong-hang">
          <div class="card__header">
            <h2>Dòng hàng (<c:out value="${order.lines.size()}"/>)</h2>
            <p>Giá theo bảng giá hiện hành của nhóm <c:out value="${order.customerGroupName}"/>. Chiết khấu tính trên tổng số lượng cùng sản phẩm trong đơn.</p>
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
                  <p class="order-line__meta"><c:out value="${ln.sku}"/><c:if test="${ln.priced}"> · <fmt:formatNumber value="${ln.unitPrice}" maxFractionDigits="2"/> ₫/<c:out value="${ln.unitName}"/></c:if></p>
                </div>
                <form class="order-line__form" action="${ctx}/orders/lines/save" method="post">
                  <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                  <input type="hidden" name="id" value="${order.id}">
                  <input type="hidden" name="lineId" value="${ln.id}">
                  <label class="visually-hidden" for="lq-${ln.id}">Số lượng</label>
                  <input class="input order-qty" id="lq-${ln.id}" name="quantity" type="number" min="1" step="1" inputmode="numeric" value="${ln.quantity}" required>
                  <label class="visually-hidden" for="lu-${ln.id}">Đơn vị tính</label>
                  <select class="select" id="lu-${ln.id}" name="unitId">
                    <c:forEach var="u" items="${unitOptions[ln.productId]}"><option value="${u.id}" ${u.id == ln.unitId ? 'selected' : ''}><c:out value="${u.name}"/></option></c:forEach>
                  </select>
                  <button class="btn btn--secondary" type="submit">Cập nhật</button>
                </form>
                <c:if test="${ln.converted}">
                  <p class="order-line__meta">= <fmt:formatNumber value="${ln.baseQuantity}" maxFractionDigits="2"/> <c:out value="${ln.baseUnitName}"/></p>
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
                <form class="order-line__remove" action="${ctx}/orders/lines/delete" method="post">
                  <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                  <input type="hidden" name="id" value="${order.id}">
                  <input type="hidden" name="lineId" value="${ln.id}">
                  <button class="btn btn--ghost btn--sm" type="submit" aria-label="Xoá dòng <c:out value='${ln.sku}'/>">Xoá dòng</button>
                </form>
              </article>
            </c:forEach>
          </div>
        </section>

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

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
