<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Trang chờ cho khách chưa đăng nhập (theo thiết kế Figma của nhóm) --%>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>OMS Bán hàng &amp; Kho — Quản lý bán hàng và kho trên một nền tảng</title>
  <%@ include file="/WEB-INF/views/effects/theme-init.jspf" %>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;500;600;700;800&amp;display=swap">
  <link rel="stylesheet" href="${ctx}/assets/css/base.css?v=${assetVersion}">
  <link rel="stylesheet" href="${ctx}/assets/css/icons.css?v=${assetVersion}">
  <link rel="stylesheet" href="${ctx}/assets/css/components.css?v=${assetVersion}">
  <link rel="stylesheet" href="${ctx}/assets/css/landing.css?v=${assetVersion}">
  <link rel="stylesheet" href="${ctx}/assets/css/effects.css?v=${assetVersion}">
</head>
<body class="landing">
  <header class="landing-header">
    <div class="landing-container landing-header__inner">
      <a class="landing-brand" href="${ctx}/">
        <span class="icon icon--md icon--package" aria-hidden="true"></span>
        <span>Bán hàng &amp; Kho</span>
      </a>
      <nav class="landing-nav" aria-label="Giới thiệu">
        <a href="#tinh-nang">Tính năng</a>
      </nav>
      <div class="landing-header__actions">
        <%@ include file="/WEB-INF/views/effects/theme-switch.jspf" %>
        <button class="landing-link landing-link--button" type="button" data-dialog-open="dealer-contact">Đăng ký làm đại lý</button>
        <a class="landing-link" href="${ctx}/login">Đăng nhập</a>
        <a class="landing-btn landing-btn--primary" href="${ctx}/login">Bắt đầu</a>
      </div>
    </div>
  </header>

  <main>
    <section class="landing-hero">
      <div class="landing-container landing-hero__inner">
        <div class="landing-hero__copy">
          <p class="landing-chip">Nền tảng quản lý bán hàng &amp; kho</p>
          <h1 class="landing-hero__title">Quản lý bán hàng và kho trên một nền tảng</h1>
          <p class="landing-hero__text">Sản phẩm, đơn hàng, đại lý, tồn kho và công nợ ở cùng một nơi. Đơn chỉ nhập một lần, hệ thống tự kiểm giá, tồn và hạn mức ngay khi chốt.</p>
          <div class="landing-hero__actions">
            <a class="landing-btn landing-btn--primary landing-btn--lg" href="${ctx}/login">Bắt đầu <span class="icon icon--sm icon--chevron-right" aria-hidden="true"></span></a>
            <a class="landing-btn landing-btn--ghost landing-btn--lg" href="#tinh-nang">Xem tính năng</a>
          </div>
        </div>
        <img class="landing-hero__image" src="${ctx}/assets/img/landing-hero.jpg?v=${assetVersion}" alt="Màn hình quản lý sản phẩm và đơn hàng gần đây" width="816" height="596">
      </div>
    </section>

    <section class="landing-features" id="tinh-nang" aria-labelledby="tinh-nang-title">
      <div class="landing-container">
        <h2 class="landing-visually-hidden" id="tinh-nang-title">Tính năng chính</h2>
        <div class="landing-features__grid">
          <article class="landing-feature">
            <span class="landing-feature__icon"><span class="icon icon--md icon--clipboard-list" aria-hidden="true"></span></span>
            <h3 class="landing-feature__title">Quản lý bán hàng</h3>
            <p class="landing-feature__text">Tạo đơn cho đại lý, tự áp giá và chiết khấu, theo dõi công nợ.</p>
          </article>
          <article class="landing-feature">
            <span class="landing-feature__icon"><span class="icon icon--md icon--package" aria-hidden="true"></span></span>
            <h3 class="landing-feature__title">Quản lý tồn kho</h3>
            <p class="landing-feature__text">Tồn kho chính xác theo từng kho, tránh bán vượt số hàng đang có.</p>
          </article>
          <article class="landing-feature">
            <span class="landing-feature__icon"><span class="icon icon--md icon--building-2" aria-hidden="true"></span></span>
            <h3 class="landing-feature__title">Vận hành kho</h3>
            <p class="landing-feature__text">Nhập kho, chuyển kho, soạn và xuất hàng theo đúng đơn đã chốt.</p>
          </article>
          <article class="landing-feature">
            <span class="landing-feature__icon"><span class="icon icon--md icon--truck" aria-hidden="true"></span></span>
            <h3 class="landing-feature__title">Giao hàng</h3>
            <p class="landing-feature__text">Đưa hàng tới đại lý nhanh hơn và biết đơn đang ở bước nào.</p>
          </article>
        </div>
      </div>
    </section>

    <section class="landing-cta" aria-labelledby="dang-ky-title">
      <div class="landing-container landing-cta__inner">
        <div>
          <h2 class="landing-cta__title" id="dang-ky-title">Muốn trở thành đại lý?</h2>
          <p class="landing-cta__text">Liên hệ công ty để được tư vấn và cấp tài khoản đặt hàng.</p>
        </div>
        <button class="landing-btn landing-btn--primary landing-btn--lg" type="button" data-dialog-open="dealer-contact">Đăng ký làm đại lý</button>
      </div>
    </section>
  </main>

  <%@ include file="/WEB-INF/views/layout/dealer-contact-dialog.jspf" %>

  <footer class="landing-footer">
    <div class="landing-container">Hệ thống quản lý bán hàng và kho</div>
  </footer>

  <script src="${ctx}/assets/js/app.js?v=${assetVersion}"></script>
  <script src="${ctx}/assets/js/effects.js?v=${assetVersion}"></script>
</body>
</html>
