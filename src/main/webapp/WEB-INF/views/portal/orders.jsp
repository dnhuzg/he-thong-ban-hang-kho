
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="pageTitle" value="Đơn hàng của tôi"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

<div class="page-header">
    <div>
        <p class="breadcrumb">Cổng đại lý</p>
        <h1>Đơn hàng của tôi</h1>
        <p class="page-header__desc">
            Các đơn đã gửi: trạng thái, ngày giao dự kiến và tổng tiền.
            Bạn có thể xem chi tiết hoặc đặt lại đơn hàng đã mua.
        </p>
    </div>
    <div class="page-header__actions">
        <a class="btn btn--primary" href="${ctx}/portal">
            Đặt hàng mới
        </a>
    </div>
</div>

<section class="card list-card">

    <div class="list-summary">
        <span class="list-summary__title">Đơn đã gửi</span>
        <span class="badge badge--primary">
            <c:out value="${orders.size()}"/>
        </span>
    </div>

    <!-- Danh sách trên máy tính -->
    <div class="table-wrap list-table">
        <table class="table">
            <thead>
                <tr>
                    <th scope="col" class="col-sku">Mã đơn</th>
                    <th scope="col">Ngày đặt</th>
                    <th scope="col">Ngày giao dự kiến</th>
                    <th scope="col" class="num">Tổng tiền</th>
                    <th scope="col">Trạng thái</th>
                    <th scope="col">Thao tác</th>
                </tr>
            </thead>

            <tbody>
                <c:if test="${empty orders}">
                    <tr>
                        <td colspan="6" class="text-muted">
                            Chưa có đơn nào đã gửi.
                        </td>
                    </tr>
                </c:if>

                <c:forEach var="o" items="${orders}">
                    <tr>
                        <td class="nowrap table__cell-code">
                            <a href="${ctx}/portal/orders/view?id=${o.id}">
                                <c:out value="${o.code}"/>
                            </a>
                        </td>

                        <td class="nowrap">
                            <c:out value="${o.createdAtText}"/>
                        </td>

                        <td class="nowrap">
                            <c:out value="${empty o.deliveryDateText ? '—' : o.deliveryDateText}"/>
                        </td>

                        <td class="num nowrap table__cell-main">
                            <fmt:formatNumber
                                value="${o.payable}"
                                maxFractionDigits="0"/> ₫
                        </td>

                        <td>
                            <span class="badge badge--${o.status.tone}">
                                <c:out value="${o.status.label}"/>
                            </span>
                        </td>

                        <td>
                            <a class="btn btn--secondary"
                               href="${ctx}/orders/reorder?id=${o.id}">
                                Đặt lại đơn
                            </a>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </div>

    <!-- Danh sách trên điện thoại -->
    <div class="product-cards">

        <c:if test="${empty orders}">
            <p class="text-muted">
                Chưa có đơn nào đã gửi.
            </p>
        </c:if>

        <c:forEach var="o" items="${orders}">
            <article class="product-card">

                <div class="product-card__head">
                    <div class="product-card__title">
                        <a class="product-card__name"
                           href="${ctx}/portal/orders/view?id=${o.id}">
                            <c:out value="${o.code}"/>
                        </a>

                        <p class="product-card__sku">
                            Đặt <c:out value="${o.createdAtText}"/>
                        </p>
                    </div>

                    <span class="badge badge--${o.status.tone}">
                        <c:out value="${o.status.label}"/>
                    </span>
                </div>

                <div class="product-card__meta">
                    <p>
                        Giao dự kiến
                        <strong>
                            <c:out value="${empty o.deliveryDateText ? '—' : o.deliveryDateText}"/>
                        </strong>
                        ·
                        <fmt:formatNumber
                            value="${o.payable}"
                            maxFractionDigits="0"/> ₫
                    </p>
                </div>

                <div class="page-header__actions">
                    <a class="btn btn--secondary"
                       href="${ctx}/orders/reorder?id=${o.id}">
                        Đặt lại đơn
                    </a>
                </div>

            </article>
        </c:forEach>
    </div>

</section>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
