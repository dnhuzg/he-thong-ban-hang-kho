<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="pageTitle" value="In phiếu thu"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

<link rel="stylesheet"
      href="${ctx}/assets/css/payment-receipts.css?v=${assetVersion}">

<section class="payment-receipts">

  <header class="page-header">
    <div>
      <p class="breadcrumb">Hóa đơn &amp; công nợ / Phiếu thu</p>
      <h1>In phiếu thu</h1>
      <p class="page-header__desc">
        Mỗi khoản thu đã ghi nhận có một mã phiếu thu duy nhất; xem trước rồi in hoặc tải PDF.
      </p>
    </div>
  </header>

  <div class="payment-receipts__layout">

    <section class="card payment-receipts__list"
             aria-labelledby="payment-receipts-list-title">
      <div class="payment-receipts__heading">
        <h2 id="payment-receipts-list-title">Khoản thu đã ghi nhận</h2>
        <p>Chọn một khoản để xem phiếu thu tương ứng.</p>
      </div>

      <div class="table-wrap">
        <table class="table">
          <caption class="visually-hidden">Danh sách phiếu thu</caption>
          <thead>
            <tr>
              <th scope="col">Mã phiếu</th>
              <th scope="col">Đại lý</th>
              <th scope="col">Số tiền</th>
              <th scope="col">Thao tác</th>
            </tr>
          </thead>
          <tbody>
            <c:if test="${empty receipts}">
              <tr><td colspan="4" class="text-muted">Chưa có khoản thu nào. Ghi nhận ở màn Ghi nhận thanh toán.</td></tr>
            </c:if>
            <c:forEach var="r" items="${receipts}">
              <tr>
                <td class="nowrap table__cell-code"><c:out value="${r.code}"/></td>
                <td><c:out value="${r.customerName}"/></td>
                <td class="nowrap"><fmt:formatNumber value="${r.amount}" maxFractionDigits="0"/> ₫</td>
                <td><a class="btn btn--secondary btn--sm" href="${ctx}/finance/payment-receipts?id=${r.id}"${r.id == selected.id ? ' aria-current="true"' : ''}>Xem</a></td>
              </tr>
            </c:forEach>
          </tbody>
        </table>
      </div>
    </section>

    <section class="card payment-receipts__preview"
             aria-labelledby="payment-receipt-preview-title">
      <div class="payment-receipts__heading">
        <h2 id="payment-receipt-preview-title">Xem trước phiếu thu</h2>
        <p>Kiểm tra thông tin trước khi in.</p>
      </div>

      <c:choose>
        <c:when test="${empty selected}">
          <p class="text-muted">Chưa có phiếu thu để xem.</p>
        </c:when>
        <c:otherwise>
          <article class="payment-receipt-paper">
            <header class="payment-receipt-paper__header">
              <p>HỆ THỐNG BÁN HÀNG &amp; KHO</p>
              <h2>PHIẾU THU</h2>
              <p>Số: <c:out value="${selected.code}"/></p>
            </header>

            <dl class="payment-receipt-paper__details">
              <div><dt>Ngày thu</dt><dd><c:out value="${selected.paidOnText}"/></dd></div>
              <div><dt>Đại lý</dt><dd><c:out value="${selected.customerCode}"/> – <c:out value="${selected.customerName}"/></dd></div>
              <div><dt>Số tiền</dt><dd><fmt:formatNumber value="${selected.amount}" maxFractionDigits="0"/> ₫</dd></div>
              <div><dt>Hình thức</dt><dd><c:out value="${selected.method.label}"/></dd></div>
              <div><dt>Người thu</dt><dd><c:out value="${selected.collectedByName}"/></dd></div>
              <div><dt>Đối trừ hoá đơn</dt><dd><c:out value="${selected.invoiceCodes}"/></dd></div>
            </dl>

            <p class="payment-receipt-paper__amount">
              Số tiền nhận: <strong><fmt:formatNumber value="${selected.amount}" maxFractionDigits="0"/> ₫</strong>
            </p>

            <div class="payment-receipt-paper__signatures">
              <div>
                <strong>Người nộp tiền</strong>
                <span>(Ký, ghi rõ họ tên)</span>
              </div>
              <div>
                <strong>Người thu tiền</strong>
                <span>(Ký, ghi rõ họ tên)</span>
              </div>
            </div>
          </article>

          <div class="payment-receipts__actions">
            <a class="btn btn--primary" href="${ctx}/finance/payment-receipts/pdf?id=${selected.id}" target="_blank" rel="noopener">In phiếu thu</a>
            <a class="btn btn--secondary" href="${ctx}/finance/payment-receipts/pdf?id=${selected.id}&amp;download=1">Tải PDF</a>
          </div>
        </c:otherwise>
      </c:choose>
    </section>

  </div>

</section>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
