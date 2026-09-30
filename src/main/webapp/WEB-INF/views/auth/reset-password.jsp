<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Đặt lại mật khẩu"/>
<%@ include file="/WEB-INF/views/layout/auth-top.jspf" %>

      <h1 class="auth-card__title">Đặt mật khẩu mới</h1>

      <c:choose>
        <c:when test="${not tokenValid}">
          <div class="alert alert--warning" role="alert">
            Liên kết đặt lại mật khẩu đã hết hạn hoặc đã được sử dụng.
            <a href="${ctx}/forgot-password">Gửi lại liên kết mới</a>.
          </div>
        </c:when>
        <c:otherwise>
          <c:if test="${not empty error}">
            <div class="alert alert--danger" role="alert"><c:out value="${error}"/></div>
          </c:if>

          <form action="${ctx}/reset-password" method="post" novalidate>
            <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
            <input type="hidden" name="token" value="<c:out value='${token}'/>">
            <div class="form-field">
              <label class="form-label" for="new-password">Mật khẩu mới</label>
              <div class="input-group">
                <input class="input" id="new-password" name="newPassword" type="password" autocomplete="new-password" minlength="8" required autofocus aria-describedby="new-password-hint">
                <button class="input-group__action" type="button" data-toggle-password="new-password" aria-pressed="false">Hiện</button>
              </div>
              <p class="form-hint" id="new-password-hint">Tối thiểu 8 ký tự, gồm cả chữ và số.</p>
            </div>

            <div class="form-field">
              <label class="form-label" for="confirm-password">Nhập lại mật khẩu mới</label>
              <input class="input" id="confirm-password" name="confirmPassword" type="password" autocomplete="new-password" required>
            </div>

            <button class="btn btn--primary btn--block" type="submit">Đặt lại mật khẩu</button>
          </form>
        </c:otherwise>
      </c:choose>

      <p class="auth-card__footer"><a href="${ctx}/login">← Quay lại đăng nhập</a></p>

<%@ include file="/WEB-INF/views/layout/auth-bottom.jspf" %>
