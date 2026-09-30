<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Đổi mật khẩu"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

        <div class="page-header">
          <div>
            <p class="breadcrumb">Tài khoản</p>
            <h1>Đổi mật khẩu</h1>
          </div>
        </div>

        <section class="card form-narrow">
          <div class="card__body">
            <c:if test="${currentUser.mustChangePassword}">
              <div class="alert alert--warning" role="status">Bạn đang dùng mật khẩu tạm. Hãy đặt mật khẩu mới để tiếp tục sử dụng hệ thống.</div>
            </c:if>
            <c:if test="${not empty error}">
              <div class="alert alert--danger" role="alert"><c:out value="${error}"/></div>
            </c:if>

            <form action="${ctx}/account/change-password" method="post" novalidate>
              <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
              <div class="form-field">
                <label class="form-label" for="current-password">Mật khẩu hiện tại <span class="required">*</span></label>
                <div class="input-group">
                  <input class="input" id="current-password" name="currentPassword" type="password" autocomplete="current-password" required>
                  <button class="input-group__action" type="button" data-toggle-password="current-password" aria-pressed="false">Hiện</button>
                </div>
              </div>

              <div class="form-field">
                <label class="form-label" for="new-password">Mật khẩu mới <span class="required">*</span></label>
                <div class="input-group">
                  <input class="input" id="new-password" name="newPassword" type="password" autocomplete="new-password" minlength="8" required aria-describedby="new-password-hint">
                  <button class="input-group__action" type="button" data-toggle-password="new-password" aria-pressed="false">Hiện</button>
                </div>
                <p class="form-hint" id="new-password-hint">Tối thiểu 8 ký tự, gồm cả chữ và số.</p>
              </div>

              <div class="form-field">
                <label class="form-label" for="confirm-password">Nhập lại mật khẩu mới <span class="required">*</span></label>
                <input class="input" id="confirm-password" name="confirmPassword" type="password" autocomplete="new-password" required>
              </div>

              <div class="form-actions">
                <c:if test="${not currentUser.mustChangePassword}">
                  <a class="btn btn--secondary" href="${ctx}/home">Huỷ</a>
                </c:if>
                <button class="btn btn--primary" type="submit">Đổi mật khẩu</button>
              </div>
            </form>
          </div>
        </section>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
