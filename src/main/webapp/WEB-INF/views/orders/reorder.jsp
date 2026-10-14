<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<c:set var="pageTitle" value="Đặt lại đơn hàng"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

<div class="page-header">
    <div>
        <p class="breadcrumb">Đơn hàng</p>
        <h1>Đặt lại đơn hàng</h1>
        <p class="page-header__desc">
            Chọn các mặt hàng muốn đặt lại từ đơn cũ.
            Giá sẽ được tính theo bảng giá hiện hành.
        </p>
    </div>
</div>

<section class="card list-card">
    <div class="list-summary">
        <div>
            <span class="list-summary__title">Đơn hàng cũ</span>
            <p>Mã đơn: <strong><c:out value="${order.code}"/></strong></p>
            <p>Đại lý: <c:out value="${order.customerName}"/></p>
        </div>
    </div>

    <c:choose>
        <c:when test="${empty order.lines}">
            <p class="text-muted">
                Đơn hàng này chưa có dòng hàng để đặt lại.
            </p>
            <a class="btn btn--secondary" href="${ctx}/orders">
                Quay lại danh sách đơn hàng
            </a>
        </c:when>

        <c:otherwise>
            <form action="${ctx}/orders/reorder" method="post">
                <input type="hidden" name="_csrf"
                       value="<c:out value='${csrfToken}'/>">
                <input type="hidden" name="id" value="${order.id}">

                <div class="table-wrap list-table">
                    <table class="table">
                        <thead>
                            <tr>
                                <th scope="col">Chọn</th>
                                <th scope="col">Mã hàng</th>
                                <th scope="col">Tên mặt hàng</th>
                                <th scope="col" class="num">Số lượng cũ</th>
                                <th scope="col">Đơn vị</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="line" items="${order.lines}">
                                <tr>
                                    <td>
                                        <input type="checkbox"
                                               name="lineId"
                                               value="${line.id}"
                                               aria-label="Chọn mặt hàng">
                                    </td>
                                    <td class="nowrap">
                                        <c:out value="${line.sku}"/>
                                    </td>
                                    <td>
                                        <c:out value="${line.productName}"/>
                                    </td>
                                    <td class="num">
                                        <c:out value="${line.quantity}"/>
                                    </td>
                                    <td>
                                        <c:out value="${line.unitName}"/>
                                    </td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>

                <p class="text-muted">
                    Mặt hàng đã ngừng kinh doanh hoặc chưa có giá hiện hành
                    sẽ không được thêm vào đơn mới và sẽ có thông báo.
                </p>

                <div class="page-header__actions">
                    <a class="btn btn--secondary" href="${ctx}/orders">
                        Hủy
                    </a>
                    <button class="btn btn--primary" type="submit">
                        Đặt lại các mặt hàng đã chọn
                    </button>
                </div>
            </form>
        </c:otherwise>
    </c:choose>
</section>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>