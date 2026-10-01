<%@ page contentType="text/html;charset=UTF-8" isErrorPage="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Không tìm thấy trang"/>
<c:choose>
<c:when test="${not empty currentUser}">
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>
        <section class="card error-state">
<%@ include file="/WEB-INF/views/effects/tv-404.jspf" %>
          <h1>Không tìm thấy trang</h1>
          <p>Đường dẫn có thể đã sai hoặc trang đã bị chuyển đi. Hãy quay lại trang chủ hoặc dùng menu.</p>
          <div class="form-actions">
            <a class="btn btn--primary" href="${ctx}/home">Về trang chủ</a>
          </div>
        </section>
<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
</c:when>
<c:otherwise>
<%@ include file="/WEB-INF/views/layout/auth-top.jspf" %>
<%@ include file="/WEB-INF/views/effects/tv-404.jspf" %>
      <h1 class="auth-card__title">Không tìm thấy trang</h1>
      <p class="auth-card__desc">Đường dẫn có thể đã sai hoặc trang đã bị chuyển đi. Hãy quay lại trang chủ hoặc dùng menu.</p>
      <a class="btn btn--primary btn--block" href="${ctx}/login">Đến trang đăng nhập</a>
<%@ include file="/WEB-INF/views/layout/auth-bottom.jspf" %>
</c:otherwise>
</c:choose>