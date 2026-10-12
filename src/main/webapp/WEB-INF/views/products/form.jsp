<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="pageTitle" value="${form['new'] ? 'Thêm sản phẩm' : 'Sửa sản phẩm'}"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

        <div class="page-header">
          <div>
            <p class="breadcrumb"><a href="${ctx}/products">Sản phẩm &amp; bảng giá</a> / Sản phẩm</p>
            <h1><c:out value="${pageTitle}"/></h1>
            <p class="page-header__desc">
              <c:choose>
                <c:when test="${form['new']}">Khai báo sản phẩm mới trong danh mục.</c:when>
                <c:otherwise><c:out value="${form.name} · ${form.sku}"/></c:otherwise>
              </c:choose>
            </p>
          </div>
          <a class="btn btn--secondary" href="${ctx}/products"><span class="icon icon--sm icon--arrow-left" aria-hidden="true"></span>Quay lại danh sách</a>
        </div>

        <c:if test="${not empty error}">
          <div class="alert alert--danger" role="alert"><c:out value="${error}"/></div>
        </c:if>

        <c:choose>
          <c:when test="${form['new']}"><c:url var="formAction" value="/products/new"/></c:when>
          <c:otherwise><c:url var="formAction" value="/products/edit"><c:param name="id" value="${form.id}"/></c:url></c:otherwise>
        </c:choose>

        <%-- Form chính để trống; các ô nhập ở dưới gắn vào bằng thuộc tính form="product-form",
             nhờ vậy khối đơn vị quy đổi (có form riêng) nằm giữa trang mà không lồng form --%>
        <form id="product-form" action="${formAction}" method="post" enctype="multipart/form-data" novalidate></form>
        <input type="hidden" form="product-form" name="_csrf" value="<c:out value='${csrfToken}'/>">

        <div class="form-layout">
          <div class="form-layout__main">
            <section class="card form-card">
              <div class="form-card__head">
                <span class="form-card__icon"><span class="icon icon--package" aria-hidden="true"></span></span>
                <div>
                  <h2>Thông tin chung</h2>
                  <p>Thông tin nhận diện và phân loại sản phẩm.</p>
                </div>
              </div>
              <div class="form-card__row">
                <div class="form-field">
                  <label class="form-label" for="sku">Mã SKU <span class="required">*</span></label>
                  <c:choose>
                    <c:when test="${form['new']}">
                      <input class="input" form="product-form" id="sku" name="sku" type="text" maxlength="50" required autocomplete="off"
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
                  <input class="input" form="product-form" id="name" name="name" type="text" maxlength="255" required value="<c:out value='${form.name}'/>">
                </div>
              </div>
              <div class="form-field">
                <label class="form-label" for="category">Nhóm hàng <span class="required">*</span></label>
                <select class="select" form="product-form" id="category" name="categoryId" required>
                  <option value="">— Chọn nhóm hàng —</option>
                  <c:forEach var="cat" items="${categories}">
                    <option value="${cat.id}" ${cat.id == form.categoryId ? 'selected' : ''}><c:out value="${fn:replace(cat.path, ' › ', ' • ')}"/></option>
                  </c:forEach>
                </select>
              </div>
            </section>

            <section class="card form-card">
              <div class="form-card__head">
                <span class="form-card__icon"><span class="icon icon--layers" aria-hidden="true"></span></span>
                <div>
                  <h2>Đơn vị &amp; quy cách</h2>
                  <p>Quy đổi về đơn vị cơ sở để quản lý nhất quán.</p>
                </div>
              </div>
              <div class="form-card__row">
                <div class="form-field">
                  <label class="form-label" for="unit">ĐVT cơ sở <span class="required">*</span></label>
                  <select class="select" form="product-form" id="unit" name="baseUnitId" required>
                    <option value="">— Chọn đơn vị —</option>
                    <c:forEach var="u" items="${units}">
                      <option value="${u.id}" ${u.id == form.baseUnitId ? 'selected' : ''}><c:out value="${u.name}"/></option>
                    </c:forEach>
                  </select>
                </div>
                <div class="form-field">
                  <label class="form-label" for="packing">Quy cách đóng gói</label>
                  <input class="input" form="product-form" id="packing" name="packingSpec" type="text" maxlength="255" placeholder="Ví dụ: 24 lon/thùng"
                         value="<c:out value='${form.packingSpec}'/>">
                </div>
              </div>

              <%-- Đơn vị quy đổi (S2-07): mỗi dòng là form riêng nên đặt ngoài form chính --%>
              <c:if test="${not form['new']}">
                <div class="conv" id="don-vi-quy-doi" aria-labelledby="units-title">
                  <p class="form-label" id="units-title">Quy đổi đơn vị</p>
                  <p class="form-hint">Hệ số quy về đơn vị cơ sở <strong><c:out value="${product.baseUnitName}"/></strong>. Đơn hàng và phiếu kho nhập theo đơn vị nào cũng được quy về đơn vị cơ sở khi ghi sổ.</p>
                  <div class="conv__table">
                    <div class="conv__head"><span>Đơn vị</span><span>Số lượng cơ sở</span></div>
                    <div class="conv__row">
                      <span class="conv__unit"><c:out value="${product.baseUnitName}"/> <span class="badge">Cơ sở</span></span>
                      <span class="conv__fixed">1 <c:out value="${product.baseUnitName}"/></span>
                    </div>
                    <c:forEach var="pu" items="${conversions}">
                      <div class="conv__row">
                        <span class="conv__unit"><c:out value="${pu.unitName}"/></span>
                        <form class="conv__form" action="${ctx}/products/units/save" method="post">
                          <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                          <input type="hidden" name="productId" value="${product.id}">
                          <input type="hidden" name="unitId" value="${pu.unitId}">
                          <label class="visually-hidden" for="factor-${pu.unitId}">Hệ số của <c:out value="${pu.unitName}"/></label>
                          <input class="input" id="factor-${pu.unitId}" name="factor" inputmode="decimal" required value="<c:out value='${pu.factorText}'/>">
                          <span class="conv__suffix"><c:out value="${product.baseUnitName}"/></span>
                          <button class="btn btn--secondary btn--sm" type="submit">Lưu</button>
                        </form>
                        <form action="${ctx}/products/units/delete" method="post">
                          <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                          <input type="hidden" name="productId" value="${product.id}">
                          <input type="hidden" name="unitId" value="${pu.unitId}">
                          <button class="btn btn--ghost btn--sm" type="submit">Xoá</button>
                        </form>
                      </div>
                    </c:forEach>
                    <form class="conv__row conv__row--add" action="${ctx}/products/units/save" method="post">
                      <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                      <input type="hidden" name="productId" value="${product.id}">
                      <label class="visually-hidden" for="new-unit">Đơn vị</label>
                      <select class="select" id="new-unit" name="unitId" required>
                        <option value="">— Thêm đơn vị —</option>
                        <c:forEach var="u" items="${units}">
                          <c:if test="${u.id != product.baseUnitId}">
                            <option value="${u.id}"><c:out value="${u.name}"/></option>
                          </c:if>
                        </c:forEach>
                      </select>
                      <label class="visually-hidden" for="new-factor">Hệ số</label>
                      <input class="input" id="new-factor" name="factor" inputmode="decimal" placeholder="Ví dụ: 24" required>
                      <button class="btn btn--primary btn--sm" type="submit">Thêm quy đổi</button>
                    </form>
                  </div>
                  <c:if test="${not empty conversions}">
                    <p class="form-hint"><c:forEach var="pu" items="${conversions}" varStatus="st"><c:out value="1 ${pu.unitName} = ${pu.factorText} ${product.baseUnitName}${st.last ? '' : ' · '}"/></c:forEach></p>
                  </c:if>
                </div>
              </c:if>
            </section>
          </div>

          <div class="form-layout__side">
            <%-- Giá vốn chỉ Quản lý kinh doanh xem / sửa; server cũng bỏ qua trường này với người khác --%>
            <c:if test="${canViewCostPrice}">
              <section class="card form-card">
                <div class="form-card__head">
                  <span class="form-card__icon"><span class="icon icon--wallet" aria-hidden="true"></span></span>
                  <div>
                    <h2>Giá vốn</h2>
                    <p>Giá theo đơn vị cơ sở.</p>
                  </div>
                </div>
                <div class="form-field">
                  <label class="form-label" for="cost">Giá vốn (₫<c:if test="${not form['new']}"> / <c:out value="${product.baseUnitName}"/></c:if>)</label>
                  <input class="input" form="product-form" id="cost" name="costPrice" type="text" inputmode="decimal" placeholder="Ví dụ: 7200"
                         value="<c:out value='${form.costPrice}'/>">
                </div>
                <c:if test="${not form['new'] and not empty product.costPrice}">
                  <p class="form-hint">Giá vốn hiện có: <fmt:formatNumber value="${product.costPrice}" maxFractionDigits="2"/> ₫ / <c:out value="${product.baseUnitName}"/>.</p>
                </c:if>
              </section>
            </c:if>

            <section class="card form-card">
              <div class="form-card__head">
                <span class="form-card__icon"><span class="icon icon--circle-check" aria-hidden="true"></span></span>
                <div>
                  <h2>Trạng thái</h2>
                  <p>Trạng thái kinh doanh của sản phẩm.</p>
                </div>
              </div>
              <div class="form-field">
                <label class="form-label" for="status">Trạng thái sản phẩm</label>
                <select class="select" form="product-form" id="status" name="status" data-badge-target="status-badge">
                  <c:forEach var="s" items="${statuses}">
                    <option value="${s.code}" ${s.code == form.status ? 'selected' : ''}><c:out value="${s.label}"/></option>
                  </c:forEach>
                </select>
              </div>
              <span class="badge ${form.status == 'ACTIVE' ? 'badge--success' : ''}" id="status-badge" aria-live="polite">
                <c:forEach var="s" items="${statuses}"><c:if test="${s.code == form.status}"><c:out value="${s.label}"/></c:if></c:forEach>
              </span>
            </section>

            <section class="card form-card">
              <div class="form-card__head">
                <span class="form-card__icon"><span class="icon icon--package" aria-hidden="true"></span></span>
                <div>
                  <h2>Ảnh sản phẩm</h2>
                  <p>JPG hoặc PNG, tối đa 2MB. Ảnh được cắt vuông ở giữa.</p>
                </div>
              </div>
              <div class="avatar-upload">
                <c:if test="${not empty product.imagePath}">
                  <img class="product-image" src="${ctx}/files/<c:out value='${product.imagePath}'/>" alt="Ảnh sản phẩm hiện tại" width="120" height="120">
                </c:if>
                <div class="form-field">
                  <label class="form-label" for="image">Chọn ảnh</label>
                  <input class="input" form="product-form" id="image" name="image" type="file" accept="image/png,image/jpeg">
                  <c:if test="${not empty product.imagePath}">
                    <label class="choice"><input type="checkbox" form="product-form" name="removeImage"> <span class="choice__title">Xoá ảnh hiện tại</span></label>
                  </c:if>
                </div>
              </div>
            </section>
          </div>
        </div>

        <div class="card form-footer">
          <p class="form-footer__note">* Trường bắt buộc</p>
          <div class="form-footer__actions">
            <a class="btn btn--secondary" href="${ctx}/products">Huỷ</a>
            <button class="btn btn--primary" type="submit" form="product-form"><span class="icon icon--sm icon--check" aria-hidden="true"></span><c:out value="${form['new'] ? 'Thêm sản phẩm' : 'Lưu sản phẩm'}"/></button>
          </div>
        </div>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
