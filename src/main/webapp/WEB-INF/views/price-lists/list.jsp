<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Bảng giá"/>
<c:set var="catalogTab" value="prices"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

        <div class="page-header">
          <div>
            <p class="breadcrumb">Sản phẩm &amp; bảng giá</p>
            <h1>Bảng giá</h1>
          </div>
          <c:if test="${canWrite}">
            <a class="btn btn--primary" href="${ctx}/products/price-lists/new">+ Tạo bảng giá</a>
          </c:if>
        </div>

<%@ include file="/WEB-INF/views/products/nav.jspf" %>

        <section class="card">
          <form class="toolbar" action="${ctx}/products/price-lists" method="get">
            <label class="visually-hidden" for="group">Nhóm khách hàng</label>
            <select class="select" id="group" name="group">
              <option value="">Tất cả nhóm khách hàng</option>
              <c:forEach var="g" items="${groups}">
                <option value="${g.id}" ${g.id == selectedGroup ? 'selected' : ''}><c:out value="${g.name}"/></option>
              </c:forEach>
            </select>
            <button class="btn btn--secondary" type="submit">Lọc</button>
          </form>

          <div class="table-wrap">
            <table class="table">
              <thead>
                <tr>
                  <th scope="col">Bảng giá</th>
                  <th scope="col">Nhóm khách hàng</th>
                  <th scope="col">Hiệu lực</th>
                  <th scope="col" class="num">Số dòng giá</th>
                  <th scope="col">Trạng thái</th>
                </tr>
              </thead>
              <tbody>
                <c:if test="${empty lists}">
                  <tr><td colspan="5" class="text-muted">Chưa có bảng giá nào.</td></tr>
                </c:if>
                <c:forEach var="pl" items="${lists}">
                  <tr>
                    <td>
                      <a class="table__cell-main" href="${ctx}/products/price-lists/view?id=${pl.id}"><c:out value="${pl.name}"/></a>
                      <div class="table__cell-sub"><c:out value="${pl.code} · phiên bản ${pl.version}"/></div>
                    </td>
                    <td><c:out value="${pl.customerGroupName}"/></td>
                    <td class="nowrap"><c:out value="${pl.validFromText} → ${pl.validToText}"/></td>
                    <td class="num">${pl.itemCount}</td>
                    <td>
                      <div class="badge-list">
                        <span class="badge ${pl.validityLabel == 'Đang áp dụng' ? 'badge--success' : ''}"><c:out value="${pl.validityLabel}"/></span>
                        <c:if test="${pl.locked}"><span class="badge badge--warning">Đã phát sinh đơn</span></c:if>
                        <c:if test="${pl.hasNewerVersion}"><span class="badge">Đã có bản mới</span></c:if>
                      </div>
                    </td>
                  </tr>
                </c:forEach>
              </tbody>
            </table>
          </div>
        </section>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
