<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Đăng nhập"/>
<%@ include file="/WEB-INF/views/layout/auth-top.jspf" %>

      <h1 class="auth-card__title">Đăng nhập</h1>

      <c:if test="${not empty param.expired}">
        <div class="alert alert--info" role="status">Phiên đăng nhập đã hết hạn hoặc đã bị thu hồi. Vui lòng đăng nhập lại để tiếp tục.</div>
      </c:if>
      <c:if test="${not empty param.logout}">
        <div class="alert alert--success" role="status">Bạn đã đăng xuất.</div>
      </c:if>
      <c:if test="${not empty param.reset}">
        <div class="alert alert--success" role="status">Đã đặt lại mật khẩu. Hãy đăng nhập bằng mật khẩu mới.</div>
      </c:if>
      <c:if test="${not empty error}">
        <div class="alert alert--danger" role="alert"><c:out value="${error}"/></div>
      </c:if>

      <form action="${ctx}/login" method="post" novalidate>
        <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
        <div class="form-field">
          <label class="form-label" for="username">Tài khoản</label>
          <input class="input" id="username" name="username" type="text" autocomplete="username" required autofocus
                 value="<c:out value='${username}'/>">
        </div>

        <div class="form-field">
          <label class="form-label" for="password">Mật khẩu</label>
          <div class="input-group">
            <input class="input" id="password" name="password" type="password" autocomplete="current-password" required>
            <button class="input-group__action" type="button" data-toggle-password="password" aria-pressed="false">Hiện</button>
          </div>
        </div>

        <div class="auth-card__row">
          <a href="${ctx}/forgot-password">Quên mật khẩu?</a>
        </div>

        <button class="btn btn--primary btn--block" type="submit">Đăng nhập</button>
      </form>

<%@ include file="/WEB-INF/views/layout/auth-bottom.jspf" %>
