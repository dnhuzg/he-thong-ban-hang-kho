<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Quên mật khẩu"/>
<%@ include file="/WEB-INF/views/layout/auth-top.jspf" %>

      <h1 class="auth-card__title">Quên mật khẩu</h1>

      <%-- Luôn cùng một thông báo dù email có tồn tại hay không (S1-03) --%>
      <c:if test="${sent}">
        <div class="alert alert--success" role="status">Nếu email này thuộc một tài khoản trong hệ thống, bạn sẽ nhận được liên kết đặt lại mật khẩu trong vài phút.</div>
      </c:if>
      <c:if test="${not empty error}">
        <div class="alert alert--danger" role="alert"><c:out value="${error}"/></div>
      </c:if>

      <form action="${ctx}/forgot-password" method="post" novalidate>
        <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
        <div class="form-field">
          <label class="form-label" for="email">Email</label>
          <input class="input" id="email" name="email" type="email" autocomplete="email" required autofocus>
        </div>

        <button class="btn btn--primary btn--block" type="submit">Gửi liên kết đặt lại</button>
      </form>

      <p class="auth-card__footer"><a href="${ctx}/login">← Quay lại đăng nhập</a></p>

<%@ include file="/WEB-INF/views/layout/auth-bottom.jspf" %>
