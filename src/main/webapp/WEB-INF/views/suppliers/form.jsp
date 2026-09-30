<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="${isNew ? 'Thêm nhà cung cấp' : 'Sửa nhà cung cấp'}"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

        <div class="page-header">
          <div>
            <p class="breadcrumb"><a href="${ctx}/suppliers">Nhà cung cấp</a> / <c:out value="${isNew ? 'Thêm mới' : supplier.code}"/></p>
            <h1><c:out value="${pageTitle}"/></h1>
          </div>
        </div>

        <c:if test="${not empty error}">
          <div class="alert alert--danger" role="alert"><c:out value="${error}"/></div>
        </c:if>

        <c:choose>
          <c:when test="${isNew}"><c:url var="formAction" value="/suppliers/new"/></c:when>
          <c:otherwise><c:url var="formAction" value="/suppliers/edit"><c:param name="id" value="${supplier.id}"/></c:url></c:otherwise>
        </c:choose>

        <form action="${formAction}" method="post" novalidate>
          <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
          <input type="hidden" name="id" value="${supplier.id}">

          <section class="card">
            <div class="card__header"><h2>Thông tin nhà cung cấp</h2></div>
            <div class="card__body form-grid">
              <div class="form-field">
                <label class="form-label" for="code">Mã nhà cung cấp <span class="required">*</span></label>
                <c:choose>
                  <c:when test="${isNew}">
                    <input class="input" id="code" name="code" maxlength="20" required value="<c:out value='${supplier.code}'/>" aria-describedby="code-hint">
                    <p class="form-hint" id="code-hint">Không đổi được sau khi tạo.</p>
                  </c:when>
                  <c:otherwise><input class="input" id="code" value="<c:out value='${supplier.code}'/>" readonly></c:otherwise>
                </c:choose>
              </div>
              <div class="form-field">
                <label class="form-label" for="name">Tên nhà cung cấp <span class="required">*</span></label>
                <input class="input" id="name" name="name" maxlength="255" required value="<c:out value='${supplier.name}'/>">
              </div>
              <div class="form-field">
                <label class="form-label" for="tax">Mã số thuế</label>
                <input class="input" id="tax" name="taxCode" maxlength="14" inputmode="numeric" placeholder="0101234567" value="<c:out value='${supplier.taxCode}'/>">
              </div>
              <div class="form-field">
                <label class="form-label" for="terms">Điều khoản thanh toán</label>
                <input class="input" id="terms" name="paymentTerms" maxlength="255" placeholder="Ví dụ: Công nợ 30 ngày" value="<c:out value='${supplier.paymentTerms}'/>">
              </div>
              <div class="form-field">
                <label class="form-label" for="contact">Người liên hệ</label>
                <input class="input" id="contact" name="contactName" maxlength="150" value="<c:out value='${supplier.contactName}'/>">
              </div>
              <div class="form-field">
                <label class="form-label" for="phone">Số điện thoại liên hệ</label>
                <input class="input" id="phone" name="contactPhone" type="tel" value="<c:out value='${supplier.contactPhone}'/>">
              </div>
              <div class="form-field">
                <label class="form-label" for="email">Email</label>
                <input class="input" id="email" name="email" type="email" maxlength="150" value="<c:out value='${supplier.email}'/>">
              </div>
              <div class="form-field">
                <label class="form-label" for="address">Địa chỉ</label>
                <input class="input" id="address" name="address" maxlength="255" value="<c:out value='${supplier.address}'/>">
              </div>
              <div class="form-field">
                <label class="form-label" for="status">Trạng thái</label>
                <select class="select" id="status" name="status">
                  <c:forEach var="s" items="${statuses}">
                    <option value="${s.code}" ${s == supplier.status ? 'selected' : ''}><c:out value="${s.label}"/></option>
                  </c:forEach>
                </select>
              </div>
            </div>
          </section>

          <div class="form-actions">
            <a class="btn btn--secondary" href="${ctx}/suppliers">Huỷ</a>
            <button class="btn btn--primary" type="submit"><c:out value="${isNew ? 'Thêm nhà cung cấp' : 'Lưu thay đổi'}"/></button>
          </div>
        </form>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
