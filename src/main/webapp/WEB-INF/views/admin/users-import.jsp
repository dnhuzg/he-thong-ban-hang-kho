<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Nhập người dùng từ Excel"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

        <div class="page-header">
          <div>
            <p class="breadcrumb"><a href="${ctx}/admin/users">Người dùng</a> / Nhập từ Excel</p>
            <h1>Nhập người dùng từ Excel</h1>
          </div>
          <a class="btn btn--secondary" href="${ctx}/admin/users/import/template" data-no-loading>Tải tệp mẫu</a>
        </div>

        <section class="card">
          <div class="card__header">
            <h2>1. Chọn tệp</h2>
            <p>Tệp .xlsx theo mẫu, tối đa 5 MB và 500 dòng. Chưa có tài khoản nào được tạo cho tới khi bạn bấm Nhập. Mỗi tài khoản tạo xong sẽ nhận email kích hoạt kèm mật khẩu tạm.</p>
          </div>
          <form class="card__body inline-form" action="${ctx}/admin/users/import" method="post" enctype="multipart/form-data">
            <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
            <label class="visually-hidden" for="file">Tệp Excel</label>
            <input class="input" id="file" name="file" type="file" accept=".xlsx,.xls" required>
            <button class="btn btn--primary" type="submit">Xem trước</button>
          </form>
        </section>

        <c:if test="${not empty batch}">
          <section class="card">
            <div class="card__header">
              <h2><c:out value="${batch.applied ? '3. Kết quả nhập' : '2. Xem trước'}"/> — <c:out value="${batch.fileName}"/></h2>
            </div>
            <div class="card__body">
              <c:choose>
                <c:when test="${batch.applied}">
                  <p><c:out value="Đã tạo ${batch.createdCount} tài khoản · Bỏ qua ${batch.skippedCount} dòng lỗi"/></p>
                  <c:if test="${emailsNotSent > 0}">
                    <div class="alert alert--warning" role="status"><c:out value="${emailsNotSent} tài khoản chưa gửi được email kích hoạt (chưa cấu hình SMTP hoặc gửi lỗi). Người dùng có thể dùng \"Quên mật khẩu\" để nhận mật khẩu mới."/></div>
                  </c:if>
                </c:when>
                <c:otherwise>
                  <p><c:out value="${batch.rows.size()} dòng: ${batch.validCount} hợp lệ, ${batch.errorCount} lỗi (sẽ bị bỏ qua)."/></p>
                  <c:if test="${batch.validCount > 0}">
                    <form action="${ctx}/admin/users/import/apply" method="post">
                      <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                      <input type="hidden" name="token" value="<c:out value='${batch.token}'/>">
                      <button class="btn btn--primary" type="submit">Nhập <c:out value="${batch.validCount}"/> dòng hợp lệ</button>
                    </form>
                  </c:if>
                </c:otherwise>
              </c:choose>
            </div>
            <div class="table-wrap">
              <table class="table">
                <thead>
                  <tr>
                    <th scope="col" class="num">Dòng</th>
                    <th scope="col">Họ và tên</th>
                    <th scope="col">Tài khoản</th>
                    <th scope="col">Email</th>
                    <th scope="col">Vai trò</th>
                    <th scope="col">Kết quả</th>
                  </tr>
                </thead>
                <tbody>
                  <c:forEach var="r" items="${batch.rows}">
                    <tr>
                      <td class="num">${r.rowNumber}</td>
                      <td><c:out value="${r.fullName}"/></td>
                      <td class="nowrap"><c:out value="${r.username}"/></td>
                      <td><c:out value="${r.email}"/></td>
                      <td><c:out value="${r.roleText}"/></td>
                      <td>
                        <c:choose>
                          <c:when test="${not r.valid}"><span class="badge badge--danger"><c:out value="Lỗi: ${r.errorText}"/></span></c:when>
                          <c:when test="${not empty r.outcome}"><span class="badge badge--success"><c:out value="${r.outcome.label}"/></span></c:when>
                          <c:otherwise><span class="badge badge--success">Tạo mới</span></c:otherwise>
                        </c:choose>
                      </td>
                    </tr>
                  </c:forEach>
                </tbody>
              </table>
            </div>
          </section>
        </c:if>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
