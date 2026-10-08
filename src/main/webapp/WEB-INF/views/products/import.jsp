<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Nhập sản phẩm từ Excel"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

        <div class="page-header">
          <div>
            <p class="breadcrumb"><a href="${ctx}/products">Sản phẩm</a> / Nhập từ Excel</p>
            <h1>Nhập sản phẩm từ Excel</h1>
          </div>
          <a class="btn btn--secondary" href="${ctx}/products/import/template" data-no-loading>Tải tệp mẫu</a>
        </div>

        <section class="card">
          <div class="card__header">
            <h2>1. Chọn tệp</h2>
            <p>Tệp .xlsx theo mẫu, tối đa 5 MB và 5.000 dòng. SKU đã có trong hệ thống sẽ được cập nhật thay vì tạo mới. Chưa có gì được ghi cho tới khi bạn bấm Nhập.</p>
          </div>
          <form class="card__body inline-form" action="${ctx}/products/import" method="post" enctype="multipart/form-data">
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
              <c:forEach var="n" items="${batch.notes}"><p><c:out value="${n}"/></p></c:forEach>
            </div>
            <div class="card__body">
              <c:choose>
                <c:when test="${batch.applied}">
                  <p><c:out value="Tạo mới ${batch.createdCount} · Cập nhật ${batch.updatedCount} · Không thay đổi ${batch.unchangedCount} · Bỏ qua ${batch.skippedCount} dòng lỗi"/></p>
                </c:when>
                <c:otherwise>
                  <p><c:out value="${batch.rows.size()} dòng: ${batch.validCount} hợp lệ, ${batch.errorCount} lỗi (sẽ bị bỏ qua)."/></p>
                  <c:if test="${batch.validCount > 0}">
                    <form action="${ctx}/products/import/apply" method="post">
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
                    <th scope="col">SKU</th>
                    <th scope="col">Tên sản phẩm</th>
                    <th scope="col">Nhóm hàng</th>
                    <th scope="col">Đơn vị</th>
                    <th scope="col">Kết quả</th>
                  </tr>
                </thead>
                <tbody>
                  <c:forEach var="r" items="${batch.rows}" end="499">
                    <tr>
                      <td class="num">${r.rowNumber}</td>
                      <td class="nowrap"><c:out value="${r.sku}"/></td>
                      <td><c:out value="${r.name}"/></td>
                      <td><c:out value="${r.category}"/></td>
                      <td><c:out value="${r.unit}"/></td>
                      <td>
                        <c:choose>
                          <c:when test="${not r.valid}"><span class="badge badge--danger"><c:out value="Lỗi: ${r.errorText}"/></span></c:when>
                          <c:when test="${not empty r.outcome}"><span class="badge badge--success"><c:out value="${r.outcome.label}"/></span></c:when>
                          <c:otherwise><span class="badge ${r.existing ? 'badge--warning' : 'badge--success'}"><c:out value="${r.actionLabel}"/></span></c:otherwise>
                        </c:choose>
                      </td>
                    </tr>
                  </c:forEach>
                </tbody>
              </table>
            </div>
            <c:if test="${batch.rows.size() > 500}">
              <p class="card__body text-muted"><c:out value="Chỉ hiện 500 dòng đầu; toàn bộ ${batch.rows.size()} dòng vẫn được xử lý."/></p>
            </c:if>
          </section>
        </c:if>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
