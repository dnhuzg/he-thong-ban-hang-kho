<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="pageTitle" value="Chỉ tiêu của tôi"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

        <div class="page-header">
          <div>
            <p class="breadcrumb">Bán hàng</p>
            <h1>Chỉ tiêu doanh số của tôi</h1>
            <p class="page-header__desc">Chỉ tiêu do Quản lý kinh doanh đặt theo tháng và khu vực. Doanh số tính từ hoá đơn đã phát hành của đại lý bạn phụ trách, cập nhật tới hôm nay.</p>
          </div>
        </div>

        <section class="card list-card">
          <div class="table-wrap list-table">
            <table class="table">
              <thead>
                <tr>
                  <th>Tháng</th>
                  <th>Khu vực</th>
                  <th>Chỉ tiêu (₫)</th>
                  <th>Doanh số (₫)</th>
                  <th>Tiến độ</th>
                </tr>
              </thead>
              <tbody>
                <c:if test="${empty rows}">
                  <tr><td colspan="5" class="text-muted">Bạn chưa được giao chỉ tiêu nào.</td></tr>
                </c:if>
                <c:forEach var="r" items="${rows}">
                  <tr>
                    <td><c:out value="${r.target.targetMonth.monthValue}"/>/<c:out value="${r.target.targetMonth.year}"/></td>
                    <td><c:out value="${r.areaName}"/></td>
                    <td><fmt:formatNumber value="${r.target.targetAmount}" maxFractionDigits="0"/></td>
                    <td><fmt:formatNumber value="${r.revenue}" maxFractionDigits="0"/></td>
                    <td>
                      <div class="meter">
                        <div class="meter__head"><span>Hoàn thành</span><span class="meter__value"><fmt:formatNumber value="${r.percentage}" maxFractionDigits="1"/>%</span></div>
                        <div class="meter__track"><span class="meter__fill meter__fill--${r.step}"></span></div>
                      </div>
                    </td>
                  </tr>
                </c:forEach>
              </tbody>
            </table>
          </div>
        </section>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
