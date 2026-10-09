<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Tạo bảng giá"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

        <div class="page-header">
          <div>
            <p class="breadcrumb"><a href="${ctx}/products/price-lists">Bảng giá</a> / Tạo mới</p>
            <h1>Tạo bảng giá</h1>
            <p class="page-header__desc">Bảng giá gắn với một nhóm khách hàng; đơn hàng của đại lý thuộc nhóm đó lấy giá từ đây.</p>
          </div>
          <a class="btn btn--secondary" href="${ctx}/products/price-lists"><span class="icon icon--sm icon--arrow-left" aria-hidden="true"></span>Quay lại danh sách</a>
        </div>

        <c:if test="${not empty error}">
          <div class="alert alert--danger" role="alert"><c:out value="${error}"/></div>
        </c:if>

        <form action="${ctx}/products/price-lists/new" method="post" novalidate>
          <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
          <section class="card form-card">
            <div class="form-card__head">
              <span class="form-card__icon"><span class="icon icon--tags" aria-hidden="true"></span></span>
              <div>
                <h2>Thông tin bảng giá</h2>
                <p>Các bảng giá của cùng một nhóm khách hàng không được trùng thời gian hiệu lực. Giá tính theo đơn vị cơ sở của sản phẩm.</p>
              </div>
            </div>
            <div class="form-grid">
              <div class="form-field">
                <label class="form-label" for="code">Mã bảng giá <span class="required">*</span></label>
                <input class="input" id="code" name="code" maxlength="30" required placeholder="Ví dụ: BG-DL1-2027" value="<c:out value='${form.code}'/>" aria-describedby="code-hint">
                <p class="form-hint" id="code-hint">2–30 ký tự: chữ không dấu, số và . _ - (tự viết hoa). Không trùng mã đã có.</p>
              </div>
              <div class="form-field">
                <label class="form-label" for="name">Tên bảng giá <span class="required">*</span></label>
                <input class="input" id="name" name="name" maxlength="150" required placeholder="Ví dụ: Bảng giá đại lý cấp 1 năm 2027" value="<c:out value='${form.name}'/>">
              </div>
              <div class="form-field">
                <label class="form-label" for="group">Nhóm khách hàng <span class="required">*</span></label>
                <select class="select" id="group" name="customerGroupId" required aria-describedby="group-hint">
                  <option value="">— Chọn nhóm —</option>
                  <c:forEach var="g" items="${groups}">
                    <option value="${g.id}" ${g.id == form.customerGroupId ? 'selected' : ''}><c:out value="${g.name}"/></option>
                  </c:forEach>
                </select>
                <p class="form-hint" id="group-hint">Mỗi nhóm chỉ có một bảng giá hiệu lực tại một thời điểm: khoảng ngày không được trùng bảng giá khác của nhóm.</p>
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
          <aside class="form-tip">
            <span class="icon icon--sm icon--info" aria-hidden="true"></span>
            <p><strong>Sau khi tạo:</strong> mở bảng giá để thêm từng sản phẩm với giá bán và giá sàn (giá tính theo đơn vị cơ sở).</p>
          </aside>
          <div class="card form-footer">
            <p class="form-footer__note">* Trường bắt buộc</p>
            <div class="form-footer__actions">
              <a class="btn btn--secondary" href="${ctx}/products/price-lists">Huỷ</a>
              <button class="btn btn--primary" type="submit"><span class="icon icon--sm icon--check" aria-hidden="true"></span>Tạo bảng giá</button>
            </div>
          </div>
        </form>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
