<%@ page contentType="text/html;charset=UTF-8" isErrorPage="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Lỗi hệ thống"/>
<c:choose>
<c:when test="${not empty currentUser}">
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>
        <section class="card error-state">
          <p class="error-state__code">500</p>
          <h1>Hệ thống đang gặp sự cố</h1>
          <p>Thao tác chưa được thực hiện. Vui lòng thử lại sau ít phút; nếu vẫn lỗi, báo cho quản trị hệ thống.</p>
          <div class="form-actions">
            <a class="btn btn--primary" href="${ctx}/home">Về trang chủ</a>
          </div>
        </section>
<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
</c:when>
<c:otherwise>
<%@ include file="/WEB-INF/views/layout/auth-top.jspf" %>
      <p class="error-state__code">500</p>
      <h1 class="auth-card__title">Hệ thống đang gặp sự cố</h1>
      <p class="auth-card__desc">Thao tác chưa được thực hiện. Vui lòng thử lại sau ít phút; nếu vẫn lỗi, báo cho quản trị hệ thống.</p>
      <a class="btn btn--primary btn--block" href="${ctx}/login">Đến trang đăng nhập</a>
<%@ include file="/WEB-INF/views/layout/auth-bottom.jspf" %>
</c:otherwise>
</c:choose>