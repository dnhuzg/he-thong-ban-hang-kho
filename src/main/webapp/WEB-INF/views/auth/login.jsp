<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Đăng nhập"/>
<c:set var="authSplit" value="${true}"/>
<%@ include file="/WEB-INF/views/layout/auth-top.jspf" %>

      <div class="auth-card__head">
        <h1 class="auth-card__title">Đăng nhập</h1>
        <p class="auth-card__subtitle">Chào mừng bạn trở lại hệ thống.</p>
      </div>

      <c:if test="${not empty param.expired}">
        <div class="alert alert--info" role="status">Phiên đăng nhập đã hết hạn hoặc đã bị thu hồi. Vui lòng đăng nhập lại để tiếp tục.</div>
      </c:if>
      <c:if test="${not empty param.logout}">
        <div class="alert alert--success alert--icon" role="status"><img src="${ctx}/assets/img/icon-circle-check.svg" alt="" width="16" height="16"><span>Bạn đã đăng xuất.</span></div>
      </c:if>
      <c:if test="${not empty param.reset}">
        <div class="alert alert--success alert--icon" role="status"><img src="${ctx}/assets/img/icon-circle-check.svg" alt="" width="16" height="16"><span>Đã đặt lại mật khẩu. Hãy đăng nhập bằng mật khẩu mới.</span></div>
      </c:if>
      <c:if test="${not empty error}">
        <div class="alert alert--danger" role="alert"><c:out value="${error}"/></div>
      </c:if>

      <form action="${ctx}/login" method="post" novalidate>
        <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
        <div class="float-field">
          <input class="float-field__input" id="username" name="username" type="text" autocomplete="username" required autofocus
                 placeholder=" " value="<c:out value='${username}'/>">
          <label class="float-field__label" for="username">Tài khoản</label>
          <i class="float-field__line" aria-hidden="true"></i>
        </div>

        <div class="float-field float-field--action">
          <input class="float-field__input" id="password" name="password" type="password" autocomplete="current-password" required placeholder=" ">
          <label class="float-field__label" for="password">Mật khẩu</label>
          <i class="float-field__line" aria-hidden="true"></i>
          <button class="input-group__action" type="button" data-toggle-password="password" aria-pressed="false"><img src="${ctx}/assets/img/icon-eye.svg" alt="" width="16" height="16"><span data-toggle-label>Hiện</span></button>
        </div>

        <div class="auth-card__row">
          <a href="${ctx}/forgot-password">Quên mật khẩu?</a>
        </div>

        <button class="btn btn--primary btn--block" type="submit">Đăng nhập</button>
      </form>

<%@ include file="/WEB-INF/views/layout/auth-bottom.jspf" %>
