<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="${form['new'] ? 'Tạo tài khoản' : 'Sửa tài khoản'}"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

        <div class="page-header">
          <div>
            <p class="breadcrumb"><a href="${ctx}/admin/users">Người dùng</a> / <c:out value="${form['new'] ? 'Tạo mới' : form.username}"/></p>
            <h1><c:out value="${pageTitle}"/></h1>
          </div>
        </div>

        <c:if test="${not empty error}">
          <div class="alert alert--danger" role="alert"><c:out value="${error}"/></div>
        </c:if>

        <c:choose>
          <c:when test="${form['new']}"><c:url var="formAction" value="/admin/users/new"/></c:when>
          <c:otherwise><c:url var="formAction" value="/admin/users/edit"><c:param name="id" value="${form.id}"/></c:url></c:otherwise>
        </c:choose>

        <form action="${formAction}" method="post" novalidate>
          <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">

          <section class="card">
            <div class="card__header">
              <h2>Thông tin cá nhân</h2>
            </div>
            <div class="card__body form-grid">
              <div class="form-field">
                <label class="form-label" for="full-name">Họ và tên <span class="required">*</span></label>
                <input class="input" id="full-name" name="fullName" type="text" autocomplete="name" required
                       value="<c:out value='${form.fullName}'/>">
              </div>
              <div class="form-field">
                <label class="form-label" for="username">Tài khoản <span class="required">*</span></label>
                <c:choose>
                  <c:when test="${form['new']}">
                    <input class="input" id="username" name="username" type="text" autocomplete="off" required
                           value="<c:out value='${form.username}'/>">
                  </c:when>
                  <c:otherwise>
                    <input class="input" id="username" type="text" value="<c:out value='${form.username}'/>" readonly>
                    <p class="form-hint">Không đổi được tên tài khoản sau khi tạo.</p>
                  </c:otherwise>
                </c:choose>
              </div>
              <div class="form-field">
                <label class="form-label" for="email">Email <span class="required">*</span></label>
                <input class="input" id="email" name="email" type="email" autocomplete="email" required
                       value="<c:out value='${form.email}'/>">
              </div>
              <div class="form-field">
                <label class="form-label" for="phone">Số điện thoại</label>
                <input class="input" id="phone" name="phone" type="tel" autocomplete="tel"
                       value="<c:out value='${form.phone}'/>">
              </div>
            </div>
          </section>

          <section class="card">
            <div class="card__header">
              <h2>Vai trò</h2>
              <p>Một người dùng có thể giữ nhiều vai trò cùng lúc.</p>
            </div>
            <div class="card__body">
              <fieldset class="choice-list">
                <legend class="visually-hidden">Chọn vai trò</legend>
                <c:forEach var="r" items="${roles}">
                  <label class="choice">
                    <input type="checkbox" name="roles" value="${r.code}" ${form.hasRole(r.code) ? 'checked' : ''}>
                    <span class="choice__title"><c:out value="${r.label}"/></span>
                  </label>
                </c:forEach>
              </fieldset>
            </div>
          </section>

          <section class="card">
            <div class="card__header">
              <h2>Phạm vi làm việc</h2>
              <p>Vai trò kho phải gắn với ít nhất một kho. Nhân viên kinh doanh gắn với địa bàn phụ trách.</p>
            </div>
            <div class="card__body">
              <fieldset class="choice-list">
                <legend class="form-label">Kho</legend>
                <c:forEach var="w" items="${warehouses}">
                  <label class="choice">
                    <input type="checkbox" name="warehouses" value="${w.id}" ${form.warehouseIds.contains(w.id) ? 'checked' : ''}>
                    <span class="choice__title"><c:out value="${w.name}"/></span>
                  </label>
                </c:forEach>
              </fieldset>

              <fieldset class="choice-list mt-4">
                <legend class="form-label">Địa bàn</legend>
                <c:forEach var="a" items="${areas}">
                  <label class="choice">
                    <input type="checkbox" name="areas" value="${a.id}" ${form.areaIds.contains(a.id) ? 'checked' : ''}>
                    <span class="choice__title"><c:out value="${a.name}"/></span>
                  </label>
                </c:forEach>
              </fieldset>
            </div>
          </section>

          <div class="form-actions">
            <a class="btn btn--secondary" href="${ctx}/admin/users">Huỷ</a>
            <button class="btn btn--primary" type="submit">
              <c:out value="${form['new'] ? 'Lưu và gửi email kích hoạt' : 'Lưu thay đổi'}"/>
            </button>
          </div>
        </form>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
