<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="In phiếu thu"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

<link rel="stylesheet"
      href="${ctx}/assets/css/payment-receipts.css?v=${assetVersion}">

<section class="payment-receipts" id="payment-receipts"
         data-collector="<c:out value='${currentUser.fullName}'/>">

  <header class="page-header">
    <div>
      <p class="breadcrumb">Hóa đơn &amp; công nợ / Phiếu thu</p>
      <h1>In phiếu thu</h1>
      <p class="page-header__desc">
        GHTK-75 · S7-09 · Xem trước và in phiếu thu cho khoản tiền đã ghi nhận.
      </p>
    </div>
  </header>

  <div class="alert alert--info" role="note">
    <strong>Giao diện FE minh họa.</strong>
    Các khoản thu và mã phiếu bên dưới là dữ liệu mẫu,
    không phải giao dịch đã được ghi sổ.
    Bản in không có giá trị chứng từ chính thức.
  </div>

  <div class="payment-receipts__layout">

    <section class="card payment-receipts__list"
             aria-labelledby="payment-receipts-list-title">
      <div class="payment-receipts__heading">
        <h2 id="payment-receipts-list-title">Khoản thu mẫu</h2>
        <p>Chọn một khoản để xem phiếu thu tương ứng.</p>
      </div>

      <div class="table-wrap">
        <table class="table">
          <caption class="visually-hidden">Danh sách khoản thu mẫu</caption>
          <thead>
            <tr>
              <th scope="col">Mã phiếu</th>
              <th scope="col">Đại lý</th>
              <th scope="col">Số tiền</th>
              <th scope="col">Thao tác</th>
            </tr>
          </thead>
          <tbody id="payment-receipt-rows"></tbody>
        </table>
      </div>

      <p class="payment-receipts__note">
        Mã phiếu duy nhất trong bộ dữ liệu minh họa;
        chưa được cấp hoặc lưu bởi hệ thống kế toán.
      </p>
    </section>

    <section class="card payment-receipts__preview"
             aria-labelledby="payment-receipt-preview-title">
      <div class="payment-receipts__heading">
        <h2 id="payment-receipt-preview-title">Xem trước phiếu thu</h2>
        <p>Kiểm tra thông tin trước khi in.</p>
      </div>

      <article class="payment-receipt-paper" id="payment-receipt-paper">
        <div class="payment-receipt-paper__notice">
          BẢN MINH HỌA · KHÔNG CÓ GIÁ TRỊ PHÁP LÝ
        </div>

        <header class="payment-receipt-paper__header">
          <p>HỆ THỐNG QUẢN LÝ BÁN HÀNG VÀ KHO</p>
          <h2>PHIẾU THU</h2>
          <p id="receipt-code"></p>
        </header>

        <dl class="payment-receipt-paper__details">
          <div><dt>Ngày thu</dt><dd id="receipt-date"></dd></div>
          <div><dt>Đại lý</dt><dd id="receipt-agency"></dd></div>
          <div><dt>Số tiền</dt><dd id="receipt-amount"></dd></div>
          <div><dt>Hình thức</dt><dd id="receipt-method"></dd></div>
          <div><dt>Người thu</dt><dd id="receipt-collector"></dd></div>
        </dl>

        <p class="payment-receipt-paper__amount">
          Số tiền nhận: <strong id="receipt-amount-highlight"></strong>
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

        <footer class="payment-receipt-paper__footer">
          TÀI LIỆU DEMO GHTK-75 · KHÔNG PHẢI PHIẾU THU ĐÃ PHÁT HÀNH
        </footer>
      </article>

      <div class="payment-receipts__actions">
        <button class="btn btn--primary" type="button"
                id="receipt-print">
          In / Lưu PDF
        </button>
      </div>

      <p class="payment-receipts__note">
        Chọn máy in để in giấy, hoặc chọn
        <strong>Save as PDF</strong> trong hộp thoại in để lưu PDF.
      </p>
    </section>

  </div>

  <noscript>
    <div class="alert alert--danger">
      Cần bật JavaScript để xem phiếu thu minh họa.
    </div>
  </noscript>

</section>

<script src="${ctx}/assets/js/payment-receipts.js?v=${assetVersion}"
        defer></script>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>