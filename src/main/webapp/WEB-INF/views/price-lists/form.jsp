<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Tạo bảng giá"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

        <div class="page-header">
          <div>
            <p class="breadcrumb"><a href="${ctx}/products/price-lists">Bảng giá</a> / Tạo mới</p>
            <h1>Tạo bảng giá</h1>
          </div>
        </div>

        <c:if test="${not empty error}">
          <div class="alert alert--danger" role="alert"><c:out value="${error}"/></div>
        </c:if>

        <form action="${ctx}/products/price-lists/new" method="post" novalidate>
          <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
          <section class="card">
            <div class="card__header">
              <h2>Thông tin bảng giá</h2>
              <p>Các bảng giá của cùng một nhóm khách hàng không được trùng thời gian hiệu lực. Giá tính theo đơn vị cơ sở của sản phẩm.</p>
            </div>
            <div class="card__body form-grid">
              <div class="form-field">
                <label class="form-label" for="code">Mã bảng giá <span class="required">*</span></label>
                <input class="input" id="code" name="code" maxlength="30" required placeholder="Ví dụ: BG-DL1-2027" value="<c:out value='${form.code}'/>">
              </div>
              <div class="form-field">
                <label class="form-label" for="name">Tên bảng giá <span class="required">*</span></label>
                <input class="input" id="name" name="name" maxlength="150" required value="<c:out value='${form.name}'/>">
              </div>
              <div class="form-field">
                <label class="form-label" for="group">Nhóm khách hàng <span class="required">*</span></label>
                <select class="select" id="group" name="customerGroupId" required>
                  <option value="">— Chọn nhóm —</option>
                  <c:forEach var="g" items="${groups}">
                    <option value="${g.id}" ${g.id == form.customerGroupId ? 'selected' : ''}><c:out value="${g.name}"/></option>
                  </c:forEach>
                </select>
              </div>
              <div class="form-field">
                <label class="form-label" for="from">Hiệu lực từ ngày <span class="required">*</span></label>
                <input class="input" id="from" name="validFrom" type="date" required value="${form.validFrom}">
              </div>
              <div class="form-field">
                <label class="form-label" for="to">Đến ngày</label>
                <input class="input" id="to" name="validTo" type="date" value="${form.validTo}" aria-describedby="to-hint">
                <p class="form-hint" id="to-hint">Để trống nếu chưa biết ngày kết thúc.</p>
              </div>
            </div>
          </section>
          <div class="form-actions">
            <a class="btn btn--secondary" href="${ctx}/products/price-lists">Huỷ</a>
            <button class="btn btn--primary" type="submit">Tạo bảng giá</button>
          </div>
        </form>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
