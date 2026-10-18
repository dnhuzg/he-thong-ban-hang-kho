<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="pageTitle" value="Báo cáo doanh số đa chiều"/>
<c:set var="pageStylesheet" value="sales-report.css"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

<c:set var="f" value="${sales.filter}"/>
<c:set var="t" value="${sales.total}"/>
<c:url var="exportUrl" value="/reports/sales/export">
    <c:param name="dimension" value="${f.dimension}"/>
    <c:param name="from" value="${f.from}"/>
    <c:param name="to" value="${f.to}"/>
</c:url>

<main class="sales-report">

    <header class="sales-report__header">
        <div>
            <p class="sales-report__eyebrow">BÁO CÁO KINH DOANH</p>
            <h1>Báo cáo doanh số đa chiều</h1>
            <p class="sales-report__subtitle">
                Doanh số theo hoá đơn đã phát hành (số lượng thực giao),
                so với kỳ liền trước cùng độ dài.
            </p>
        </div>
        <a class="sales-report__export" href="${exportUrl}">Xuất Excel</a>
    </header>

    <section class="sales-report__panel" aria-labelledby="sales-filter-title">
        <div class="sales-report__section-title">
            <h2 id="sales-filter-title">Bộ lọc báo cáo</h2>
            <p>Lựa chọn chiều phân tích và khoảng thời gian cần xem.</p>
        </div>

        <form class="sales-report__filters" method="get" action="${ctx}/reports/sales">
            <div class="sales-report__field">
                <label for="sales-group">Nhóm theo</label>
                <select id="sales-group" name="dimension" data-autosubmit>
                    <c:forEach var="d" items="${dimensions}">
                        <option value="${d.code}" ${d == f.dimension ? 'selected' : ''}><c:out value="${d.label}"/></option>
                    </c:forEach>
                </select>
            </div>

            <div class="sales-report__field">
                <label for="sales-start">Từ ngày</label>
                <input id="sales-start" name="from" type="date" value="<c:out value='${f.from}'/>"/>
            </div>

            <div class="sales-report__field">
                <label for="sales-end">Đến ngày</label>
                <input id="sales-end" name="to" type="date" value="<c:out value='${f.to}'/>"/>
            </div>

            <div class="sales-report__field sales-report__field--action">
                <button type="submit">Áp dụng bộ lọc</button>
            </div>
        </form>
    </section>

    <section class="sales-report__metrics" aria-label="Tổng quan báo cáo">

        <article class="sales-report__metric">
            <span>Doanh số kỳ này</span>
            <strong><fmt:formatNumber value="${t.revenue}" maxFractionDigits="0"/> ₫</strong>
            <small><c:out value="${f.from}"/> → <c:out value="${f.to}"/></small>
        </article>

        <article class="sales-report__metric">
            <span>Sản lượng kỳ này</span>
            <strong><fmt:formatNumber value="${t.quantity}" maxFractionDigits="0"/></strong>
            <small>Theo đơn vị cơ sở</small>
        </article>

        <article class="sales-report__metric">
            <span>So với cùng kỳ trước</span>
            <strong><c:choose><c:when test="${t.changePercent == null}">—</c:when><c:otherwise>${t.changePercent.signum() > 0 ? '+' : ''}<fmt:formatNumber value="${t.changePercent}" maxFractionDigits="1"/>%</c:otherwise></c:choose></strong>
            <small>Kỳ trước: <fmt:formatNumber value="${t.previousRevenue}" maxFractionDigits="0"/> ₫ (<c:out value="${sales.previousFrom}"/> → <c:out value="${sales.previousTo}"/>)</small>
        </article>

        <c:if test="${sales.showMargin}">
            <article class="sales-report__metric">
                <span>Biên lợi nhuận gộp</span>
                <strong><c:choose><c:when test="${t.marginPercent == null}">—</c:when><c:otherwise><fmt:formatNumber value="${t.marginPercent}" maxFractionDigits="1"/>%</c:otherwise></c:choose></strong>
                <small>Theo giá vốn hiện tại của sản phẩm</small>
            </article>
        </c:if>

    </section>

    <section class="sales-report__panel" aria-labelledby="sales-table-title">

        <div class="sales-report__section-title">
            <h2 id="sales-table-title">Chi tiết doanh số</h2>
            <p>Chiều phân tích: <c:out value="${f.dimension.label}"/></p>
        </div>

        <div class="sales-report__table-wrap">
            <table>
                <thead>
                    <tr>
                        <th scope="col"><c:out value="${f.dimension.label}"/></th>
                        <th scope="col">Doanh số</th>
                        <th scope="col">Sản lượng</th>
                        <th scope="col">Doanh số cùng kỳ</th>
                        <th scope="col">Chênh lệch (%)</th>
                        <c:if test="${sales.showMargin}">
                            <th scope="col">Biên LN gộp (%)</th>
                        </c:if>
                    </tr>
                </thead>

                <tbody>
                    <c:if test="${empty sales.rows}">
                        <tr>
                            <td class="sales-report__empty" colspan="${sales.showMargin ? 6 : 5}">
                                Không có hoá đơn nào trong kỳ này và kỳ trước.
                            </td>
                        </tr>
                    </c:if>
                    <c:forEach var="r" items="${sales.rows}">
                        <tr>
                            <td><c:out value="${r.label}"/></td>
                            <td><fmt:formatNumber value="${r.revenue}" maxFractionDigits="0"/></td>
                            <td><fmt:formatNumber value="${r.quantity}" maxFractionDigits="0"/></td>
                            <td><fmt:formatNumber value="${r.previousRevenue}" maxFractionDigits="0"/></td>
                            <td><c:choose><c:when test="${r.changePercent == null}">—</c:when><c:otherwise><fmt:formatNumber value="${r.changePercent}" maxFractionDigits="1"/></c:otherwise></c:choose></td>
                            <c:if test="${sales.showMargin}">
                                <td><c:choose><c:when test="${r.marginPercent == null}">—</c:when><c:otherwise><fmt:formatNumber value="${r.marginPercent}" maxFractionDigits="1"/></c:otherwise></c:choose></td>
                            </c:if>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </div>
    </section>

</main>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
