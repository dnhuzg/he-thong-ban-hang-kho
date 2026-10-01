<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="${isNew ? 'Thêm đại lý' : 'Sửa hồ sơ đại lý'}"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

        <div class="page-header">
          <div>
            <p class="breadcrumb"><a href="${ctx}/customers">Đại lý &amp; hạn mức công nợ</a> / Đại lý</p>
            <h1><c:out value="${pageTitle}"/></h1>
            <p class="page-header__desc">
              <c:choose>
                <c:when test="${isNew}">Khai báo đại lý mới: nhóm khách hàng quyết định bảng giá áp dụng.</c:when>
                <c:otherwise><c:out value="${customer.name} · ${customer.code}"/></c:otherwise>
              </c:choose>
            </p>
          </div>
          <a class="btn btn--secondary" href="${ctx}/customers"><span class="icon icon--sm icon--arrow-left" aria-hidden="true"></span>Quay lại danh sách</a>
        </div>

        <c:if test="${not empty error}">
          <div class="alert alert--danger" role="alert"><c:out value="${error}"/></div>
        </c:if>

        <c:choose>
          <c:when test="${isNew}"><c:url var="formAction" value="/customers/new"/></c:when>
          <c:otherwise><c:url var="formAction" value="/customers/edit"><c:param name="id" value="${customer.id}"/></c:url></c:otherwise>
        </c:choose>

        <form action="${formAction}" method="post" novalidate>
          <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">

          <div class="form-layout">
            <div class="form-layout__main">
              <section class="card form-card">
                <div class="form-card__head">
                  <span class="form-card__icon"><span class="icon icon--building-2" aria-hidden="true"></span></span>
                  <div>
                    <h2>Thông tin chung</h2>
                    <p>Mã, tên và thông tin pháp lý của đại lý.</p>
                  </div>
                </div>
                <div class="form-card__row">
                  <div class="form-field">
                    <label class="form-label" for="code">Mã đại lý <span class="required">*</span></label>
                    <c:choose>
                      <c:when test="${isNew}">
                        <input class="input" id="code" name="code" type="text" maxlength="20" required autocomplete="off"
                               value="<c:out value='${customer.code}'/>" aria-describedby="code-hint">
                        <p class="form-hint" id="code-hint">Duy nhất trong hệ thống, không đổi được sau khi tạo.</p>
                      </c:when>
                      <c:otherwise>
                        <input class="input" id="code" type="text" value="<c:out value='${customer.code}'/>" readonly>
                      </c:otherwise>
                    </c:choose>
                  </div>
                  <div class="form-field">
                    <label class="form-label" for="name">Tên đại lý <span class="required">*</span></label>
                    <input class="input" id="name" name="name" type="text" maxlength="200" required value="<c:out value='${customer.name}'/>">
                  </div>
                </div>
                <div class="form-card__row">
                  <div class="form-field">
                    <label class="form-label" for="taxCode">Mã số thuế</label>
                    <input class="input" id="taxCode" name="taxCode" type="text" maxlength="14" inputmode="numeric" placeholder="Ví dụ: 0101234567"
                           value="<c:out value='${customer.taxCode}'/>">
                  </div>
                  <div class="form-field">
                    <label class="form-label" for="phone">Số điện thoại</label>
                    <input class="input" id="phone" name="phone" type="tel" maxlength="20" value="<c:out value='${customer.phone}'/>">
                  </div>
                </div>
              </section>
            </div>

            <div class="form-layout__side">
              <section class="card form-card">
                <div class="form-card__head">
                  <span class="form-card__icon"><span class="icon icon--tags" aria-hidden="true"></span></span>
                  <div>
                    <h2>Phân loại &amp; phụ trách</h2>
                    <p>Nhóm khách hàng quyết định bảng giá; người phụ trách chỉ xem đại lý của mình.</p>
                  </div>
                </div>
                <div class="form-field">
                  <label class="form-label" for="groupId">Nhóm khách hàng <span class="required">*</span></label>
                  <select class="select" id="groupId" name="groupId" required>
                    <option value="">— Chọn nhóm —</option>
                    <c:forEach var="g" items="${groups}">
                      <option value="${g.id}" ${g.id == customer.groupId ? 'selected' : ''}><c:out value="${g.name}"/></option>
                    </c:forEach>
                  </select>
                </div>
                <div class="form-field">
                  <label class="form-label" for="areaId">Khu vực</label>
                  <select class="select" id="areaId" name="areaId">
                    <option value="">— Chưa chọn —</option>
                    <c:forEach var="a" items="${areas}">
                      <option value="${a.id}" ${a.id == customer.areaId ? 'selected' : ''}><c:out value="${a.name}"/></option>
                    </c:forEach>
                  </select>
                </div>
                <div class="form-field">
                  <label class="form-label" for="salesRepId">Người phụ trách</label>
                  <select class="select" id="salesRepId" name="salesRepId" ${canAssign ? '' : 'disabled aria-describedby="rep-hint"'}>
                    <option value="">— Chưa phân công —</option>
                    <c:forEach var="r" items="${reps}">
                      <option value="${r.id}" ${r.id == customer.salesRepId ? 'selected' : ''}><c:out value="${r.name}"/></option>
                    </c:forEach>
                  </select>
                  <c:if test="${not canAssign}"><p class="form-hint" id="rep-hint">Chỉ Quản lý kinh doanh phân công người phụ trách.</p></c:if>
                </div>
              </section>
            </div>
          </div>

          <div class="card form-footer">
            <p class="form-footer__note">* Trường bắt buộc</p>
            <div class="form-footer__actions">
              <a class="btn btn--secondary" href="${ctx}/customers">Huỷ</a>
              <button class="btn btn--primary" type="submit"><span class="icon icon--sm icon--check" aria-hidden="true"></span><c:out value="${isNew ? 'Thêm đại lý' : 'Lưu hồ sơ'}"/></button>
            </div>
          </div>
        </form>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
