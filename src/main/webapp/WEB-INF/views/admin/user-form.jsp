<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="${form['new'] ? 'Tạo tài khoản' : 'Sửa tài khoản'}"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

        <div class="page-header">
          <div>
            <p class="breadcrumb"><a href="${ctx}/admin/users">Người dùng</a> / <c:out value="${form['new'] ? 'Tạo mới' : form.username}"/></p>
            <h1><c:out value="${pageTitle}"/></h1>
            <p class="page-header__desc"><c:out value="${form['new'] ? 'Tạo tài khoản đăng nhập cho nhân viên hoặc đại lý; hệ thống gửi email kích hoạt kèm mật khẩu tạm.' : form.fullName}"/></p>
          </div>
          <a class="btn btn--secondary" href="${ctx}/admin/users"><span class="icon icon--sm icon--arrow-left" aria-hidden="true"></span>Quay lại danh sách</a>
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

          <section class="card form-card">
            <div class="form-card__head">
              <span class="form-card__icon"><span class="icon icon--users" aria-hidden="true"></span></span>
              <div>
                <h2>Thông tin cá nhân</h2>
                <p>Thông tin đăng nhập và liên hệ của người dùng.</p>
              </div>
            </div>
            <div class="form-grid">
              <div class="form-field">
                <label class="form-label" for="full-name">Họ và tên <span class="required">*</span></label>
                <input class="input" id="full-name" name="fullName" type="text" autocomplete="name" required placeholder="Ví dụ: Nguyễn Văn An"
                       value="<c:out value='${form.fullName}'/>" aria-describedby="name-hint">
                <p class="form-hint" id="name-hint">Hiện trên thanh trên cùng, trong nhật ký thao tác và các phiếu do người này lập.</p>
              </div>
              <div class="form-field">
                <label class="form-label" for="username">Tài khoản <span class="required">*</span></label>
                <c:choose>
                  <c:when test="${form['new']}">
                    <input class="input" id="username" name="username" type="text" autocomplete="off" required placeholder="Ví dụ: an.nguyen"
                           value="<c:out value='${form.username}'/>" aria-describedby="username-hint">
                    <p class="form-hint" id="username-hint">Dùng để đăng nhập: 3–50 ký tự gồm chữ không dấu, số và . _ - ; không đổi được sau khi tạo.</p>
                  </c:when>
                  <c:otherwise>
                    <input class="input" id="username" type="text" value="<c:out value='${form.username}'/>" readonly>
                    <p class="form-hint">Không đổi được tên tài khoản sau khi tạo.</p>
                  </c:otherwise>
                </c:choose>
              </div>
              <div class="form-field">
                <label class="form-label" for="email">Email <span class="required">*</span></label>
                <input class="input" id="email" name="email" type="email" autocomplete="email" required placeholder="Ví dụ: an.nguyen@congty.vn"
                       value="<c:out value='${form.email}'/>" aria-describedby="email-hint">
                <p class="form-hint" id="email-hint">Nhận email kích hoạt và liên kết đặt lại mật khẩu; không trùng với tài khoản khác.</p>
              </div>
              <div class="form-field">
                <label class="form-label" for="phone">Số điện thoại</label>
                <input class="input" id="phone" name="phone" type="tel" autocomplete="tel" placeholder="Ví dụ: 0912345678"
                       value="<c:out value='${form.phone}'/>" aria-describedby="phone-hint">
                <p class="form-hint" id="phone-hint">Không bắt buộc. Di động 10 số (03, 05, 07, 08, 09) hoặc cố định 11 số (02…).</p>
              </div>
            </div>
          </section>

          <section class="card form-card">
            <div class="form-card__head">
              <span class="form-card__icon"><span class="icon icon--shield-check" aria-hidden="true"></span></span>
              <div>
                <h2>Vai trò</h2>
                <p>Một người dùng có thể giữ nhiều vai trò cùng lúc.</p>
              </div>
            </div>
            <div>
              <fieldset class="choice-list">
                <legend class="visually-hidden">Chọn vai trò</legend>
                <c:forEach var="r" items="${roles}">
                  <label class="choice">
                    <input type="checkbox" name="roles" value="${r.code}" ${form.hasRole(r.code) ? 'checked' : ''}>
                    <span class="choice__text">
                    <span class="choice__title"><c:out value="${r.label}"/></span>
                    <span class="choice__desc">
                      <c:choose>
                        <c:when test="${r.code == 'CUSTOMER'}">Cửa hàng mua sỉ; chỉ xem dữ liệu của chính mình.</c:when>
                        <c:when test="${r.code == 'SALES_REP'}">Lập đơn cho đại lý mình phụ trách; gắn với địa bàn.</c:when>
                        <c:when test="${r.code == 'SALES_MANAGER'}">Duyệt đơn vượt hạn mức / dưới giá sàn; xem giá vốn.</c:when>
                        <c:when test="${r.code == 'WAREHOUSE'}">Nhập, xuất hàng tại kho; phải gắn ít nhất một kho.</c:when>
                        <c:when test="${r.code == 'WH_MANAGER'}">Quản lý tồn và phiếu kho; phải gắn ít nhất một kho.</c:when>
                        <c:when test="${r.code == 'ACCOUNTANT'}">Hạn mức, công nợ, đối chiếu với đại lý.</c:when>
                        <c:when test="${r.code == 'ADMIN'}">Toàn quyền, quản lý tài khoản và phân quyền.</c:when>
                      </c:choose>
                    </span>
                    </span>
                  </label>
                </c:forEach>
              </fieldset>
            </div>
          </section>

          <section class="card form-card">
            <div class="form-card__head">
              <span class="form-card__icon"><span class="icon icon--building-2" aria-hidden="true"></span></span>
              <div>
                <h2>Phạm vi làm việc</h2>
                <p>Vai trò kho phải gắn với ít nhất một kho. Nhân viên kinh doanh gắn với địa bàn phụ trách.</p>
              </div>
            </div>
            <div>
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

          <div class="card form-footer">
            <p class="form-footer__note">* Trường bắt buộc</p>
            <div class="form-footer__actions">
              <a class="btn btn--secondary" href="${ctx}/admin/users">Huỷ</a>
              <button class="btn btn--primary" type="submit"><span class="icon icon--sm icon--check" aria-hidden="true"></span><c:out value="${form['new'] ? 'Lưu và gửi email kích hoạt' : 'Lưu thay đổi'}"/></button>
            </div>
          </div>
        </form>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
