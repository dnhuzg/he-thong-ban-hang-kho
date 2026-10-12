<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="${isNew ? 'Thêm nhà cung cấp' : 'Sửa nhà cung cấp'}"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

        <div class="page-header">
          <div>
            <p class="breadcrumb"><a href="${ctx}/suppliers">Nhà cung cấp</a> / <c:out value="${isNew ? 'Thêm mới' : supplier.code}"/></p>
            <h1><c:out value="${pageTitle}"/></h1>
            <p class="page-header__desc">
              <c:choose>
                <c:when test="${isNew}">Khai báo nơi công ty nhập hàng; dùng khi lập phiếu nhập kho.</c:when>
                <c:otherwise><c:out value="${supplier.name} · ${supplier.code}"/></c:otherwise>
              </c:choose>
            </p>
          </div>
          <a class="btn btn--secondary" href="${ctx}/suppliers"><span class="icon icon--sm icon--arrow-left" aria-hidden="true"></span>Quay lại danh sách</a>
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

          <div class="form-layout">
            <div class="form-layout__main">
              <section class="card form-card">
                <div class="form-card__head">
                  <span class="form-card__icon"><span class="icon icon--truck" aria-hidden="true"></span></span>
                  <div>
                    <h2>Thông tin chung</h2>
                    <p>Mã, tên và thông tin pháp lý của nhà cung cấp.</p>
                  </div>
                </div>
                <div class="form-card__row">
                  <div class="form-field">
                    <label class="form-label" for="code">Mã nhà cung cấp <span class="required">*</span></label>
                    <c:choose>
                      <c:when test="${isNew}">
                        <input class="input" id="code" name="code" maxlength="20" required autocomplete="off" placeholder="Ví dụ: NCC-VINAMILK"
                               value="<c:out value='${supplier.code}'/>" aria-describedby="code-hint">
                        <p class="form-hint" id="code-hint">2–20 ký tự: chữ không dấu, số và . _ - (tự viết hoa). Duy nhất, không đổi được sau khi tạo.</p>
                      </c:when>
                      <c:otherwise><input class="input" id="code" value="<c:out value='${supplier.code}'/>" readonly></c:otherwise>
                    </c:choose>
                  </div>
                  <div class="form-field">
                    <label class="form-label" for="name">Tên nhà cung cấp <span class="required">*</span></label>
                    <input class="input" id="name" name="name" maxlength="255" required placeholder="Ví dụ: Công ty CP Sữa Việt Nam"
                           value="<c:out value='${supplier.name}'/>">
                  </div>
                </div>
                <div class="form-card__row">
                  <div class="form-field">
                    <label class="form-label" for="tax">Mã số thuế</label>
                    <input class="input" id="tax" name="taxCode" maxlength="14" inputmode="numeric" placeholder="Ví dụ: 0101234567"
                           value="<c:out value='${supplier.taxCode}'/>" aria-describedby="tax-hint">
                    <p class="form-hint" id="tax-hint">Không bắt buộc. 10 chữ số; chi nhánh thêm "-" và 3 số (0101234567-001).</p>
                  </div>
                  <div class="form-field">
                    <label class="form-label" for="status">Trạng thái</label>
                    <select class="select" id="status" name="status" aria-describedby="status-hint">
                      <c:forEach var="s" items="${statuses}">
                        <option value="${s.code}" ${s == supplier.status ? 'selected' : ''}><c:out value="${s.label}"/></option>
                      </c:forEach>
                    </select>
                    <p class="form-hint" id="status-hint">Chọn "Ngừng giao dịch" khi công ty không còn nhập hàng từ nhà cung cấp này.</p>
                  </div>
                </div>
              </section>

              <section class="card form-card">
                <div class="form-card__head">
                  <span class="form-card__icon"><span class="icon icon--users" aria-hidden="true"></span></span>
                  <div>
                    <h2>Liên hệ</h2>
                    <p>Người làm việc trực tiếp khi đặt hàng, đối chiếu và nhận hàng.</p>
                  </div>
                </div>
                <div class="form-card__row">
                  <div class="form-field">
                    <label class="form-label" for="contact">Người liên hệ</label>
                    <input class="input" id="contact" name="contactName" maxlength="150" placeholder="Ví dụ: Chị Lan – phòng kinh doanh"
                           value="<c:out value='${supplier.contactName}'/>">
                  </div>
                  <div class="form-field">
                    <label class="form-label" for="phone">Số điện thoại liên hệ</label>
                    <input class="input" id="phone" name="contactPhone" type="tel" placeholder="Ví dụ: 0912345678"
                           value="<c:out value='${supplier.contactPhone}'/>">
                  </div>
                </div>
                <div class="form-card__row">
                  <div class="form-field">
                    <label class="form-label" for="email">Email</label>
                    <input class="input" id="email" name="email" type="email" maxlength="150" placeholder="Ví dụ: kinhdoanh@nhacungcap.vn"
                           value="<c:out value='${supplier.email}'/>">
                  </div>
                  <div class="form-field">
                    <label class="form-label" for="address">Địa chỉ</label>
                    <input class="input" id="address" name="address" maxlength="255" placeholder="Số nhà, đường, phường / xã, tỉnh / thành"
                           value="<c:out value='${supplier.address}'/>">
                  </div>
                </div>
              </section>
            </div>

            <div class="form-layout__side">
              <section class="card form-card">
                <div class="form-card__head">
                  <span class="form-card__icon"><span class="icon icon--wallet" aria-hidden="true"></span></span>
                  <div>
                    <h2>Thanh toán</h2>
                    <p>Thoả thuận thanh toán với nhà cung cấp.</p>
                  </div>
                </div>
                <div class="form-field">
                  <label class="form-label" for="terms">Điều khoản thanh toán</label>
                  <input class="input" id="terms" name="paymentTerms" maxlength="255" placeholder="Ví dụ: Công nợ 30 ngày"
                         value="<c:out value='${supplier.paymentTerms}'/>" aria-describedby="terms-hint">
                  <p class="form-hint" id="terms-hint">Ghi ngắn gọn: trả ngay, công nợ 15 / 30 ngày, trả theo đợt…</p>
                </div>
              </section>
            </div>
          </div>

          <div class="card form-footer">
            <p class="form-footer__note">* Trường bắt buộc</p>
            <div class="form-footer__actions">
              <a class="btn btn--secondary" href="${ctx}/suppliers">Huỷ</a>
              <button class="btn btn--primary" type="submit"><span class="icon icon--sm icon--check" aria-hidden="true"></span><c:out value="${isNew ? 'Thêm nhà cung cấp' : 'Lưu thay đổi'}"/></button>
            </div>
          </div>
        </form>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
