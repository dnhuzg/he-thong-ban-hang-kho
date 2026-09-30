<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Hồ sơ cá nhân"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

        <div class="page-header">
          <div>
            <p class="breadcrumb">Tài khoản</p>
            <h1>Hồ sơ cá nhân</h1>
          </div>
        </div>

        <div class="profile-grid">
          <section class="card" aria-labelledby="profile-edit-title">
            <div class="card__header">
              <h2 id="profile-edit-title">Thông tin liên lạc</h2>
              <p>Kho và kinh doanh dùng số điện thoại này để gọi xác nhận đơn.</p>
            </div>
            <div class="card__body">
              <c:if test="${not empty error}">
                <div class="alert alert--danger" role="alert"><c:out value="${error}"/></div>
              </c:if>

              <form action="${ctx}/account/profile" method="post" novalidate>
                <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                <div class="form-field">
                  <label class="form-label" for="full-name">Họ và tên <span class="required">*</span></label>
                  <input class="input" id="full-name" name="fullName" type="text" autocomplete="name" maxlength="150" required
                         value="<c:out value='${fullName}'/>">
                </div>
                <div class="form-field">
                  <label class="form-label" for="phone">Số điện thoại</label>
                  <input class="input" id="phone" name="phone" type="tel" autocomplete="tel" aria-describedby="phone-hint"
                         value="<c:out value='${phone}'/>">
                  <p class="form-hint" id="phone-hint"><c:out value="${phoneHint}"/></p>
                </div>
                <div class="form-actions">
                  <button class="btn btn--primary" type="submit">Lưu hồ sơ</button>
                </div>
              </form>
            </div>
          </section>

          <%-- Các thông tin do quản trị quản lý: chỉ hiển thị, không sửa được ở đây (S2-02) --%>
          <section class="card" aria-labelledby="profile-account-title">
            <div class="card__header">
              <h2 id="profile-account-title">Tài khoản</h2>
              <p>Muốn thay đổi các thông tin dưới đây, liên hệ quản trị hệ thống.</p>
            </div>
            <dl class="card__body info-list">
              <div><dt>Tài khoản</dt><dd><c:out value="${currentUser.username}"/></dd></div>
              <div><dt>Email</dt><dd><c:out value="${currentUser.email}"/></dd></div>
              <div><dt>Vai trò</dt><dd><c:out value="${currentUser.roleLabels}"/></dd></div>
              <div><dt>Kho / Địa bàn</dt><dd><c:out value="${currentUser.scopeLabel}"/></dd></div>
            </dl>
          </section>
        </div>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
