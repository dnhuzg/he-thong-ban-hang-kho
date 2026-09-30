<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="${form['new'] ? 'Thêm sản phẩm' : 'Sửa sản phẩm'}"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

        <div class="page-header">
          <div>
            <p class="breadcrumb"><a href="${ctx}/products">Sản phẩm</a> / <c:out value="${form['new'] ? 'Thêm mới' : form.sku}"/></p>
            <h1><c:out value="${pageTitle}"/></h1>
          </div>
        </div>

        <c:if test="${not empty error}">
          <div class="alert alert--danger" role="alert"><c:out value="${error}"/></div>
        </c:if>

        <c:choose>
          <c:when test="${form['new']}"><c:url var="formAction" value="/products/new"/></c:when>
          <c:otherwise><c:url var="formAction" value="/products/edit"><c:param name="id" value="${form.id}"/></c:url></c:otherwise>
        </c:choose>

        <form action="${formAction}" method="post" enctype="multipart/form-data" novalidate>
          <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">

          <section class="card">
            <div class="card__header">
              <h2>Thông tin sản phẩm</h2>
            </div>
            <div class="card__body form-grid">
              <div class="form-field">
                <label class="form-label" for="sku">Mã SKU <span class="required">*</span></label>
                <c:choose>
                  <c:when test="${form['new']}">
                    <input class="input" id="sku" name="sku" type="text" maxlength="50" required autocomplete="off"
                           value="<c:out value='${form.sku}'/>" aria-describedby="sku-hint">
                    <p class="form-hint" id="sku-hint">Duy nhất trong hệ thống, không đổi được sau khi tạo.</p>
                  </c:when>
                  <c:otherwise>
                    <input class="input" id="sku" type="text" value="<c:out value='${form.sku}'/>" readonly>
                  </c:otherwise>
                </c:choose>
              </div>
              <div class="form-field">
                <label class="form-label" for="name">Tên sản phẩm <span class="required">*</span></label>
                <input class="input" id="name" name="name" type="text" maxlength="255" required value="<c:out value='${form.name}'/>">
              </div>
              <div class="form-field">
                <label class="form-label" for="category">Nhóm hàng <span class="required">*</span></label>
                <select class="select" id="category" name="categoryId" required>
                  <option value="">— Chọn nhóm hàng —</option>
                  <c:forEach var="cat" items="${categories}">
                    <option value="${cat.id}" ${cat.id == form.categoryId ? 'selected' : ''}><c:forEach begin="2" end="${cat.level}">— </c:forEach><c:out value="${cat.name}"/></option>
                  </c:forEach>
                </select>
              </div>
              <div class="form-field">
                <label class="form-label" for="unit">Đơn vị tính cơ sở <span class="required">*</span></label>
                <select class="select" id="unit" name="baseUnitId" required>
                  <option value="">— Chọn đơn vị —</option>
                  <c:forEach var="u" items="${units}">
                    <option value="${u.id}" ${u.id == form.baseUnitId ? 'selected' : ''}><c:out value="${u.name}"/></option>
                  </c:forEach>
                </select>
              </div>
              <div class="form-field">
                <label class="form-label" for="packing">Quy cách đóng gói</label>
                <input class="input" id="packing" name="packingSpec" type="text" maxlength="255" placeholder="Ví dụ: 24 lon/thùng"
                       value="<c:out value='${form.packingSpec}'/>">
              </div>
              <%-- Giá vốn chỉ Quản lý kinh doanh xem / sửa; server cũng bỏ qua trường này với người khác --%>
              <c:if test="${canViewCostPrice}">
                <div class="form-field">
                  <label class="form-label" for="cost">Giá vốn (₫)</label>
                  <input class="input" id="cost" name="costPrice" type="text" inputmode="decimal" placeholder="Ví dụ: 7200"
                         value="<c:out value='${form.costPrice}'/>">
                </div>
              </c:if>
              <div class="form-field">
                <label class="form-label" for="status">Trạng thái</label>
                <select class="select" id="status" name="status">
                  <c:forEach var="s" items="${statuses}">
                    <option value="${s.code}" ${s.code == form.status ? 'selected' : ''}><c:out value="${s.label}"/></option>
                  </c:forEach>
                </select>
              </div>
            </div>
          </section>

          <section class="card">
            <div class="card__header">
              <h2>Ảnh sản phẩm</h2>
            </div>
            <div class="card__body avatar-upload">
              <c:if test="${not empty product.imagePath}">
                <img class="product-image" src="${ctx}/files/<c:out value='${product.imagePath}'/>" alt="Ảnh sản phẩm hiện tại" width="120" height="120">
              </c:if>
              <div class="form-field">
                <label class="form-label" for="image">Chọn ảnh</label>
                <input class="input" id="image" name="image" type="file" accept="image/png,image/jpeg" aria-describedby="image-hint">
                <p class="form-hint" id="image-hint">JPG hoặc PNG, tối đa 2MB. Ảnh được cắt vuông ở giữa.</p>
                <c:if test="${not empty product.imagePath}">
                  <label class="choice"><input type="checkbox" name="removeImage"> <span class="choice__title">Xoá ảnh hiện tại</span></label>
                </c:if>
              </div>
            </div>
          </section>

          <div class="form-actions">
            <a class="btn btn--secondary" href="${ctx}/products">Huỷ</a>
            <button class="btn btn--primary" type="submit"><c:out value="${form['new'] ? 'Thêm sản phẩm' : 'Lưu thay đổi'}"/></button>
          </div>
        </form>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
