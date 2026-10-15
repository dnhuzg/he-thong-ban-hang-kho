<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="pageTitle" value="Điều chỉnh tồn kho"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>
<c:set var="a" value="${adjust}"/>

<section>
  <header class="page-header">
    <div>
      <p class="breadcrumb">Tồn kho &amp; nhập kho / Điều chỉnh tồn kho</p>
      <h1>Điều chỉnh tồn kho</h1>
      <p class="page-header__desc">Ghi giảm tồn cho hàng hỏng, hàng mất hoặc hết hạn phải huỷ. Phiếu chỉ trừ tồn khi Quản lý kho duyệt; người lập không tự duyệt phiếu của mình. Mọi phiếu đã duyệt hiện trên thẻ kho.</p>
    </div>
  </header>

  <section class="card" aria-labelledby="adjust-new-title">
    <h2 id="adjust-new-title">Lập phiếu điều chỉnh</h2>
    <c:choose>
      <c:when test="${empty a.options}">
        <p class="text-muted">Không có SKU nào còn tồn khả dụng ở kho bạn phụ trách.</p>
      </c:when>
      <c:otherwise>
        <form class="form-grid" method="post" action="${ctx}/inventory/adjustments/create">
          <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
          <div class="form-field">
            <label class="form-label" for="adjust-stock">SKU tại kho *</label>
            <select class="select" id="adjust-stock" name="stock" required>
              <c:forEach var="o" items="${a.options}">
                <option value="${o.warehouseId}:${o.productId}"><c:out value="${o.warehouseName}"/> · <c:out value="${o.sku}"/> – <c:out value="${o.name}"/> (khả dụng <fmt:formatNumber value="${o.available}" maxFractionDigits="4"/> <c:out value="${o.unit}"/>)</option>
              </c:forEach>
            </select>
          </div>
          <c:if test="${not empty a.lots}">
            <div class="form-field">
              <label class="form-label" for="adjust-lot">Lô (bắt buộc với SKU quản lý lô)</label>
              <select class="select" id="adjust-lot" name="lotId">
                <option value="">Không áp dụng</option>
                <c:forEach var="l" items="${a.lots}">
                  <option value="${l.id}"><c:out value="${l.sku}"/> · lô <c:out value="${l.number}"/> · <c:out value="${l.warehouseName}"/> · HSD <c:out value="${l.expiryText}"/> · còn <fmt:formatNumber value="${l.remaining}" maxFractionDigits="4"/></option>
                </c:forEach>
              </select>
            </div>
          </c:if>
          <div class="form-field">
            <label class="form-label" for="adjust-qty">Số lượng giảm (đơn vị cơ sở) *</label>
            <input class="input" id="adjust-qty" name="quantity" type="number" min="0.0001" step="any" required>
          </div>
          <div class="form-field">
            <label class="form-label" for="adjust-reason">Lý do *</label>
            <select class="select" id="adjust-reason" name="reason" required>
              <option value="DAMAGED">Hàng hỏng</option>
              <option value="LOST">Hàng mất</option>
              <option value="EXPIRED">Hết hạn phải huỷ</option>
            </select>
          </div>
          <div class="form-field">
            <label class="form-label" for="adjust-note">Diễn giải *</label>
            <textarea class="textarea" id="adjust-note" name="note" rows="2" maxlength="500" required placeholder="Ví dụ: 2 thùng móp vỡ khi bốc xếp, có biên bản"></textarea>
          </div>
          <div class="form-actions">
            <button class="btn btn--primary" type="submit">Lập phiếu chờ duyệt</button>
          </div>
        </form>
        <p class="text-muted">Giá trị tổn thất tính theo giá vốn hiện tại của sản phẩm × số lượng.</p>
      </c:otherwise>
    </c:choose>
  </section>

  <section class="card" aria-labelledby="adjust-list-title">
    <h2 id="adjust-list-title">Phiếu điều chỉnh</h2>
    <div class="table-wrap">
      <table class="table">
        <caption class="visually-hidden">Danh sách phiếu điều chỉnh tồn kho</caption>
        <thead>
          <tr>
            <th scope="col">Mã phiếu</th>
            <th scope="col">Kho · SKU</th>
            <th scope="col" class="num">Số lượng</th>
            <th scope="col">Lý do</th>
            <th scope="col" class="num">Tổn thất</th>
            <th scope="col">Người lập</th>
            <th scope="col">Trạng thái</th>
            <th scope="col">Thao tác</th>
          </tr>
        </thead>
        <tbody>
          <c:if test="${empty a.adjustments}">
            <tr><td colspan="8" class="text-muted">Chưa có phiếu điều chỉnh nào.</td></tr>
          </c:if>
          <c:forEach var="x" items="${a.adjustments}">
            <tr>
              <td class="nowrap table__cell-code"><c:out value="${x.code}"/><span class="table__cell-sub"><c:out value="${x.createdAtText}"/></span></td>
              <td><span class="table__cell-main"><c:out value="${x.sku}"/> – <c:out value="${x.productName}"/></span><span class="table__cell-sub"><c:out value="${x.warehouseName}"/><c:if test="${not empty x.lotNo}"> · lô <c:out value="${x.lotNo}"/></c:if></span></td>
              <td class="num nowrap"><fmt:formatNumber value="${x.quantity}" maxFractionDigits="4"/> <c:out value="${x.unit}"/></td>
              <td><c:out value="${a.reasons[x.reason]}"/><span class="table__cell-sub"><c:out value="${x.note}"/></span></td>
              <td class="num nowrap"><fmt:formatNumber value="${x.lossValue}" maxFractionDigits="0"/> ₫</td>
              <td><c:out value="${x.createdByName}"/></td>
              <td>
                <c:choose>
                  <c:when test="${x.status == 'PENDING'}"><span class="badge badge--warning">Chờ duyệt</span></c:when>
                  <c:when test="${x.status == 'APPROVED'}"><span class="badge badge--success">Đã duyệt</span><span class="table__cell-sub"><c:out value="${x.decidedByName}"/></span></c:when>
                  <c:otherwise><span class="badge badge--danger">Từ chối</span><span class="table__cell-sub"><c:out value="${x.rejectReason}"/></span></c:otherwise>
                </c:choose>
              </td>
              <td class="nowrap">
                <c:if test="${x.status == 'PENDING' and a.canApprove and x.createdBy != a.viewerId}">
                  <form method="post" action="${ctx}/inventory/adjustments/approve">
                    <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                    <input type="hidden" name="id" value="${x.id}">
                    <button class="btn btn--primary btn--sm" type="submit">Duyệt</button>
                    <button class="btn btn--secondary btn--sm" type="button" data-dialog-open="reject-${x.id}">Từ chối</button>
                  </form>
                </c:if>
              </td>
            </tr>
          </c:forEach>
        </tbody>
      </table>
    </div>
  </section>
</section>

<c:forEach var="x" items="${a.adjustments}"><c:if test="${x.status == 'PENDING' and a.canApprove and x.createdBy != a.viewerId}">
  <dialog class="dialog" id="reject-${x.id}" aria-labelledby="reject-title-${x.id}"><form method="post" action="${ctx}/inventory/adjustments/reject">
    <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>"><input type="hidden" name="id" value="${x.id}">
    <header class="dialog__header"><h2 id="reject-title-${x.id}">Từ chối phiếu <c:out value="${x.code}"/></h2></header>
    <div class="dialog__body"><div class="form-field"><label class="form-label" for="reject-reason-${x.id}">Lý do từ chối *</label><textarea class="textarea" id="reject-reason-${x.id}" name="rejectReason" rows="3" maxlength="500" required></textarea></div></div>
    <footer class="dialog__footer"><button class="btn btn--secondary" type="button" data-dialog-close>Hủy</button><button class="btn btn--primary" type="submit">Từ chối phiếu</button></footer>
  </form></dialog>
</c:if></c:forEach>
<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
