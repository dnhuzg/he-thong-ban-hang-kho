
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<c:set var="pageTitle" value="Phiếu soạn ${list.code}"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

<link rel="stylesheet"
      href="${ctx}/assets/css/picking.css?v=${assetVersion}">

<div class="page-header">
  <div>
    <p class="breadcrumb">
      <a href="${ctx}/shipments/picking">Phiếu soạn hàng</a>
    </p>

    <h1>Phiếu soạn <c:out value="${list.code}"/></h1>

    <p class="page-header__desc">
      <c:out value="${list.warehouseName}"/> ·
      <c:out value="${list.orderCount}"/> đơn ·
      lập bởi <c:out value="${list.createdByName}"/>
      lúc <c:out value="${list.createdAtText}"/>.
      Hàng xếp theo nhóm hàng rồi mã SKU.
    </p>
  </div>

  <span class="badge ${list.open ? 'badge--warning' : 'badge--success'}">
    <c:out value="${list.open ? 'Đang soạn' : 'Đã hoàn tất'}"/>
  </span>
</div>

<section class="card list-card">
  <div class="list-summary">
    <span class="list-summary__title">
      Đã soạn
      <c:out value="${list.pickedCount}"/>/<c:out value="${list.lineCount}"/>
      dòng
    </span>

    <c:if test="${list.open and list.allPicked}">
      <form method="post"
            action="${ctx}/shipments/picking/complete">
        <input type="hidden"
               name="_csrf"
               value="<c:out value='${csrfToken}'/>">
        <input type="hidden" name="id" value="${list.id}">

        <button class="btn btn--primary btn--sm" type="submit">
          Hoàn tất phiếu soạn
        </button>
      </form>
    </c:if>

    <c:if test="${not list.open}">
      <a class="btn btn--secondary btn--sm"
         href="${ctx}/shipments/export">
        Sang xuất kho
      </a>
    </c:if>
  </div>

  <div class="pick-list">
    <c:set var="group" value=""/>

    <c:forEach var="l" items="${list.lines}">
      <c:if test="${l.categoryName ne group}">
        <h2 class="pick-list__group">
          <c:out value="${l.categoryName}"/>
        </h2>
        <c:set var="group" value="${l.categoryName}"/>
      </c:if>

      <article class="pick-line" id="line-${l.id}">
        <div class="pick-line__head">
          <div class="pick-line__title">
            <span class="pick-line__name">
              <c:out value="${l.productName}"/>
            </span>

            <p class="pick-line__sku">
              <c:out value="${l.sku}"/> ·
              đơn <c:out value="${l.orderCode}"/> ·
              <c:out value="${l.customerName}"/>
            </p>
          </div>

          <c:choose>
            <c:when test="${l.shortPicked}">
              <span class="badge badge--danger">Soạn thiếu</span>
            </c:when>
            <c:when test="${l.done}">
              <span class="badge badge--success">Đã soạn</span>
            </c:when>
            <c:otherwise>
              <span class="badge">Chưa soạn</span>
            </c:otherwise>
          </c:choose>
        </div>

        <div class="pick-line__meta">
          <p>
            Cần soạn
            <strong>
              <c:out value="${l.required}"/>
              <c:out value="${l.baseUnit}"/>
            </strong>

            <c:if test="${l.done}">
              · thực soạn
              <strong><c:out value="${l.picked}"/></strong>
            </c:if>
          </p>

          <c:if test="${l.shortPicked}">
            <p>
              Lý do: <c:out value="${l.shortReason}"/>
            </p>
          </c:if>
        </div>

        <c:set var="availableLots"
               value="${fefoLotsByLine[l.id]}"/>

        <div class="pick-line__meta">
          <p><strong>Gợi ý lô xuất theo FEFO</strong></p>

          <c:choose>
            <c:when test="${empty availableLots}">
              <p>
                Chưa có lô khả dụng.
                Không thể chọn lô để xuất hàng.
              </p>
            </c:when>

            <c:otherwise>
              <p>
                Hệ thống ưu tiên lô có hạn dùng gần nhất
                và tự phân bổ số lượng cần soạn.
              </p>

              <c:forEach var="lot" items="${availableLots}">
                <p>
                  <c:if test="${lot.recommended}">
                    <strong>★ Gợi ý FEFO · </strong>
                  </c:if>

                  Lô #<c:out value="${lot.id}"/>

                  <c:if test="${not empty lot.lotNo}">
                    · Mã <c:out value="${lot.lotNo}"/>
                  </c:if>

                  · Hạn dùng:
                  <c:choose>
                    <c:when test="${empty lot.expiryDate}">
                      Chưa xác định
                    </c:when>
                    <c:otherwise>
                      <c:out value="${lot.expiryDate}"/>
                    </c:otherwise>
                  </c:choose>

                  · Còn:
                  <c:out value="${lot.remainingQuantity}"/>
                  <c:out value="${l.baseUnit}"/>

                  <c:if test="${lot.suggestedQuantity > 0}">
                    · <strong>
                      Đề xuất lấy
                      <c:out value="${lot.suggestedQuantity}"/>
                    </strong>
                  </c:if>
                </p>
              </c:forEach>
            </c:otherwise>
          </c:choose>
        </div>

        <c:if test="${list.open}">
          <form class="pick-line__foot fefo-pick-form"
                method="post"
                action="${ctx}/shipments/picking/pick">

            <input type="hidden"
                   name="_csrf"
                   value="<c:out value='${csrfToken}'/>">
            <input type="hidden"
                   name="id"
                   value="${list.id}">
            <input type="hidden"
                   name="lineId"
                   value="${l.id}">

            <label class="form-field">
              <span class="form-label">
                Thực soạn (<c:out value="${l.baseUnit}"/>)
              </span>

              <input class="input fefo-picked"
                     type="number"
                     name="picked"
                     min="0"
                     max="${l.required}"
                     step="0.0001"
                     inputmode="decimal"
                     required
                     value="<c:out value='${l.done ? l.picked : l.required}'/>">
            </label>

            <label class="form-field">
              <span class="form-label">
                Lý do nếu soạn thiếu
              </span>

              <input class="input"
                     type="text"
                     name="shortReason"
                     maxlength="500"
                     value="<c:out value='${l.shortReason}'/>">
            </label>

            <div class="form-field">
              <span class="form-label">
                Chọn lô hàng (FEFO)
              </span>

              <p>
                Các lô FEFO đã được chọn sẵn.
                Bạn có thể thay đổi số lượng hoặc chọn lô khác.
                Nếu bỏ qua lô ưu tiên, phải nhập lý do.
              </p>

              <c:forEach var="lot" items="${availableLots}">
                <div class="form-field fefo-lot-row">
                  <label>
                    <input type="checkbox"
                           class="fefo-lot-check"
                           data-lot-id="${lot.id}"
                           <c:if test="${lot.suggestedQuantity > 0}">checked</c:if>>

                    Chọn lô #<c:out value="${lot.id}"/>

                    <c:if test="${not empty lot.lotNo}">
                      – <c:out value="${lot.lotNo}"/>
                    </c:if>

                    <c:if test="${lot.recommended}">
                      (FEFO đề xuất)
                    </c:if>
                  </label>

                  <div class="fefo-lot-fields"
                       <c:if test="${lot.suggestedQuantity <= 0}">hidden</c:if>>

                    <input type="hidden"
                           class="fefo-lot-id"
                           name="lotId"
                           value="${lot.id}">

                    <label class="form-field">
                      <span class="form-label">
                        Số lượng lấy từ lô
                        #<c:out value="${lot.id}"/>
                      </span>

                      <input class="input fefo-lot-quantity"
                             type="number"
                             name="lotQuantity"
                             min="0.0001"
                             max="${lot.remainingQuantity}"
                             step="0.0001"
                             inputmode="decimal"
                             value="<c:out value='${lot.suggestedQuantity > 0 ? lot.suggestedQuantity : \"\"}'/>">
                    </label>

                    <label class="form-field">
                      <span class="form-label">
                        Lý do chọn khác FEFO (nếu có)
                      </span>

                      <input class="input fefo-lot-reason"
                             type="text"
                             name="overrideReason"
                             maxlength="500">
                    </label>
                  </div>
                </div>
              </c:forEach>

              <p class="fefo-total">
                Tổng đã chọn:
                <strong class="fefo-total-value">0</strong>
                <c:out value="${l.baseUnit}"/>
              </p>

              <p class="fefo-validation-message"
                 role="alert"
                 hidden></p>
            </div>

            <button class="btn ${l.done ? 'btn--secondary' : 'btn--primary'} btn--sm"
                    type="submit">
              <c:out value="${l.done ? 'Cập nhật' : 'Đã soạn'}"/>
            </button>
          </form>
        </c:if>
      </article>
    </c:forEach>
  </div>
</section>

<script>
(function () {
  "use strict";

  function asScaledInt(value) {
    var text = String(value).trim().replace(',', '.');

    if (!/^\d+(\.\d{1,4})?$/.test(text)) {
      return null;
    }

    var pieces = text.split('.');
    var whole = Number(pieces[0]);
    var fraction = (pieces[1] || '').padEnd(4, '0');

    if (!Number.isSafeInteger(whole) || whole > 1000000000) {
      return null;
    }

    return whole * 10000 + Number(fraction);
  }

  document.querySelectorAll('.fefo-pick-form').forEach(function (form) {
    var pickedInput = form.querySelector('.fefo-picked');
    var totalElement = form.querySelector('.fefo-total-value');
    var messageElement = form.querySelector('.fefo-validation-message');

    function updateLot(check) {
      var row = check.closest('.fefo-lot-row');
      var fields = row.querySelector('.fefo-lot-fields');
      var enabled = check.checked;

      fields.hidden = !enabled;

      fields.querySelectorAll('input').forEach(function (input) {
        input.disabled = !enabled;
      });

      var quantity = fields.querySelector('.fefo-lot-quantity');
      quantity.required = enabled;

      if (!enabled) {
        quantity.value = '';
        fields.querySelector('.fefo-lot-reason').value = '';
      }
    }

    function calculateTotal() {
      var total = 0;
      var invalid = false;

      form.querySelectorAll('.fefo-lot-check:checked').forEach(function (check) {
        var row = check.closest('.fefo-lot-row');
        var quantityInput = row.querySelector('.fefo-lot-quantity');
        var amount = asScaledInt(quantityInput.value);

        if (amount === null || amount <= 0 || !quantityInput.checkValidity()) {
          invalid = true;
        } else {
          total += amount;
        }
      });

      totalElement.textContent = (total / 10000).toLocaleString('vi-VN', {
        maximumFractionDigits: 4
      });

      return invalid ? null : total;
    }

    function showMessage(text) {
      messageElement.textContent = text;
      messageElement.hidden = !text;
    }

    form.querySelectorAll('.fefo-lot-check').forEach(function (check) {
      updateLot(check);

      check.addEventListener('change', function () {
        updateLot(check);
        calculateTotal();
        showMessage('');
      });
    });

    form.querySelectorAll('.fefo-lot-quantity').forEach(function (input) {
      input.addEventListener('input', function () {
        calculateTotal();
        showMessage('');
      });
    });

    pickedInput.addEventListener('input', function () {
      calculateTotal();
      showMessage('');
    });

    form.addEventListener('submit', function (event) {
      var picked = asScaledInt(pickedInput.value);
      var total = calculateTotal();

      if (picked === null || total === null || total !== picked) {
        event.preventDefault();
        showMessage(
          'Tổng số lượng từ các lô phải bằng số thực soạn. '
          + 'Vui lòng kiểm tra lại trước khi lưu.'
        );
        return;
      }

      showMessage('');
    });

    calculateTotal();
  });
})();
</script>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
