<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="isEdit" value="${form.id > 0}"/>
<c:set var="pageTitle" value="${isEdit ? 'Sửa chính sách chiết khấu' : 'Tạo chính sách chiết khấu'}"/>
<c:set var="isProduct" value="${empty scopeIsProduct ? empty form.categoryId : scopeIsProduct}"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

        <div class="page-header">
          <div>
            <p class="breadcrumb"><a href="${ctx}/products/discounts">Chiết khấu</a> / <c:out value="${isEdit ? 'Sửa' : 'Tạo mới'}"/></p>
            <h1><c:out value="${pageTitle}"/></h1>
            <p class="page-header__desc">Đại lý mua càng nhiều càng được giảm; khi nhiều chính sách cùng khớp, hệ thống tự chọn mức có lợi nhất cho khách.</p>
          </div>
          <a class="btn btn--secondary" href="${ctx}/products/discounts"><span class="icon icon--sm icon--arrow-left" aria-hidden="true"></span>Quay lại danh sách</a>
        </div>

        <c:if test="${not empty error}">
          <div class="alert alert--danger" role="alert"><c:out value="${error}"/></div>
        </c:if>

        <form action="${ctx}/products/discounts/${isEdit ? 'edit' : 'new'}" method="post" novalidate>
          <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
          <c:if test="${isEdit}"><input type="hidden" name="id" value="${form.id}"></c:if>

          <section class="card form-card">
            <div class="form-card__head">
              <span class="form-card__icon"><span class="icon icon--percent" aria-hidden="true"></span></span>
              <div>
                <h2>Thông tin chính sách</h2>
                <p>Chọn áp dụng cho một sản phẩm hoặc cho cả một nhóm hàng (gồm các nhóm con).</p>
              </div>
            </div>
            <div class="form-grid">
              <div class="form-field">
                <label class="form-label" for="name">Tên chính sách <span class="required">*</span></label>
                <input class="input" id="name" name="name" maxlength="150" required placeholder="Ví dụ: Chiết khấu nước ngọt mùa hè" value="<c:out value='${form.name}'/>">
              </div>
              <div class="form-field">
                <label class="form-label" for="scope">Áp dụng cho <span class="required">*</span></label>
                <select class="select" id="scope" name="scope" data-scope-switch>
                  <option value="category" ${isProduct ? '' : 'selected'}>Một nhóm hàng</option>
                  <option value="product" ${isProduct ? 'selected' : ''}>Một sản phẩm (SKU)</option>
                </select>
              </div>
              <div class="form-field" data-scope-field="product">
                <label class="form-label" for="productId">Sản phẩm <span class="required">*</span></label>
                <select class="select" id="productId" name="productId">
                  <option value="">— Chọn sản phẩm —</option>
                  <c:forEach var="p" items="${products}">
                    <option value="${p.id}" ${p.id == form.productId ? 'selected' : ''}><c:out value="${p.name}"/></option>
                  </c:forEach>
                </select>
              </div>
              <div class="form-field" data-scope-field="category">
                <label class="form-label" for="categoryId">Nhóm hàng <span class="required">*</span></label>
                <select class="select" id="categoryId" name="categoryId">
                  <option value="">— Chọn nhóm hàng —</option>
                  <c:forEach var="cat" items="${categories}">
                    <option value="${cat.id}" ${cat.id == form.categoryId ? 'selected' : ''}><c:out value="${fn:replace(cat.path, ' › ', ' • ')}"/></option>
                  </c:forEach>
                </select>
              </div>
              <div class="form-field">
                <label class="form-label" for="type">Cách tính chiết khấu <span class="required">*</span></label>
                <select class="select" id="type" name="type" aria-describedby="type-hint">
                  <c:forEach var="t" items="${types}">
                    <option value="${t.name()}" ${t == form.type ? 'selected' : ''}><c:out value="${t.label}"/></option>
                  </c:forEach>
                </select>
                <p class="form-hint" id="type-hint">Phần trăm: giảm % trên giá bán. Số tiền: giảm số đồng trên mỗi đơn vị cơ sở.</p>
              </div>
              <div class="form-field">
                <label class="form-label" for="status">Trạng thái</label>
                <select class="select" id="status" name="status">
                  <c:forEach var="s" items="${statuses}">
                    <option value="${s.name()}" ${s == form.status ? 'selected' : ''}><c:out value="${s.label}"/></option>
                  </c:forEach>
                </select>
              </div>
              <div class="form-field">
                <label class="form-label" for="from">Hiệu lực từ ngày</label>
                <input class="input" id="from" name="validFrom" type="date" value="${form.validFrom}" aria-describedby="from-hint">
                <p class="form-hint" id="from-hint">Để trống thì áp dụng ngay khi lưu.</p>
              </div>
              <div class="form-field">
                <label class="form-label" for="to">Đến ngày</label>
                <input class="input" id="to" name="validTo" type="date" value="${form.validTo}" aria-describedby="to-hint">
                <p class="form-hint" id="to-hint">Để trống cả hai ngày nếu chính sách không có thời hạn.</p>
              </div>
            </div>
          </section>

          <section class="card form-card">
            <div class="form-card__head">
              <span class="form-card__icon"><span class="icon icon--layers" aria-hidden="true"></span></span>
              <div>
                <h2>Các bậc chiết khấu</h2>
                <p>Số lượng tính theo đơn vị cơ sở của sản phẩm. Mức chiết khấu là % hoặc số tiền giảm trên mỗi đơn vị, tuỳ cách tính đã chọn. Bậc cao hơn phải giảm nhiều hơn bậc thấp. Để trống các hàng không dùng.</p>
              </div>
            </div>
            <div class="tier-grid">
              <span class="form-label tier-grid__head">Mua từ (đơn vị cơ sở)</span>
              <span class="form-label tier-grid__head">Mức chiết khấu</span>
              <c:forEach var="row" items="${tierRows}" varStatus="st">
                <div class="form-field">
                  <label class="visually-hidden" for="tq-${st.index}">Bậc ${st.index + 1}: số lượng tối thiểu</label>
                  <input class="input" id="tq-${st.index}" name="tierQty" inputmode="numeric" placeholder="Ví dụ: 100" value="<c:out value='${row[0]}'/>">
                </div>
                <div class="form-field">
                  <label class="visually-hidden" for="tv-${st.index}">Bậc ${st.index + 1}: mức chiết khấu</label>
                  <input class="input" id="tv-${st.index}" name="tierValue" inputmode="decimal" placeholder="Ví dụ: 3 hoặc 500" value="<c:out value='${row[1]}'/>">
                </div>
              </c:forEach>
            </div>
          </section>

          <div class="card form-footer">
            <p class="form-footer__note">* Trường bắt buộc</p>
            <div class="form-footer__actions">
              <a class="btn btn--secondary" href="${ctx}/products/discounts">Huỷ</a>
              <button class="btn btn--primary" type="submit"><span class="icon icon--sm icon--check" aria-hidden="true"></span><c:out value="${isEdit ? 'Lưu thay đổi' : 'Tạo chính sách'}"/></button>
            </div>
          </div>
        </form>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
