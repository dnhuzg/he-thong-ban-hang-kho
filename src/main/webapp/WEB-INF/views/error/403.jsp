<%@ page contentType="text/html;charset=UTF-8" isErrorPage="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Không đủ quyền"/>
<c:choose>
<c:when test="${not empty currentUser}">
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>
        <section class="card error-state">
          <p class="error-state__code">403</p>
          <h1>Bạn không có quyền truy cập trang này</h1>
          <p>Chức năng này không thuộc vai trò của bạn. Nếu cần dùng, hãy liên hệ quản trị hệ thống để được cấp quyền.</p>
          <div class="form-actions">
            <a class="btn btn--primary" href="${ctx}/home">Về trang chủ</a>
          </div>
        </section>
<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
</c:when>
<c:otherwise>
<%@ include file="/WEB-INF/views/layout/auth-top.jspf" %>
      <p class="error-state__code">403</p>
      <h1 class="auth-card__title">Bạn không có quyền truy cập trang này</h1>
      <p class="auth-card__desc">Chức năng này không thuộc vai trò của bạn. Nếu cần dùng, hãy liên hệ quản trị hệ thống để được cấp quyền.</p>
      <a class="btn btn--primary btn--block" href="${ctx}/login">Đến trang đăng nhập</a>
<%@ include file="/WEB-INF/views/layout/auth-bottom.jspf" %>
</c:otherwise>
</c:choose>