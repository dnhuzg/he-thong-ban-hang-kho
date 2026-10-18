<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="pageTitle" value="Dashboard điều hành"/>
<c:set var="pageStylesheet" value="management-dashboard.css"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>
<c:set var="d" value="${dashboard}"/>

<div class="mdash">
    <header class="mdash-header">
        <div>
            <p class="mdash-eyebrow">TỔNG QUAN ĐIỀU HÀNH</p>
            <h1>Dashboard bán hàng &amp; kho</h1>
            <p class="mdash-subtitle">Theo dõi doanh số, đơn hàng, công nợ và tồn kho trong một màn hình.</p>
        </div>
    </header>

    <section aria-labelledby="mdash-kpi-title">
        <div class="mdash-section-heading">
            <h2 id="mdash-kpi-title">Chỉ số tổng quan</h2>
            <p>6 chỉ số điều hành trọng tâm</p>
        </div>

        <div class="mdash-kpis">
            <article class="mdash-kpi">
                <p>Doanh số hôm nay</p>
                <strong><fmt:formatNumber value="${d.today}" maxFractionDigits="0"/> ₫</strong>
                <span>Theo hoá đơn phát hành hôm nay</span>
            </article>
            <article class="mdash-kpi">
                <p>Doanh số tháng này</p>
                <strong><fmt:formatNumber value="${d.month}" maxFractionDigits="0"/> ₫</strong>
                <span>Từ đầu tháng tới hôm nay</span>
            </article>
            <article class="mdash-kpi">
                <p>Đơn chờ duyệt</p>
                <strong><fmt:formatNumber value="${d.pendingApproval}"/></strong>
                <span>Đơn vượt hạn mức / dưới giá sàn</span>
            </article>
            <article class="mdash-kpi">
                <p>Đơn chờ xuất</p>
                <strong><fmt:formatNumber value="${d.pendingDispatch}"/></strong>
                <span>Đã duyệt hoặc đang soạn hàng</span>
            </article>
            <article class="mdash-kpi mdash-kpi--danger">
                <p>Công nợ quá hạn</p>
                <strong><fmt:formatNumber value="${d.overdueTotal}" maxFractionDigits="0"/> ₫</strong>
                <span>Còn nợ của hoá đơn đã qua hạn</span>
            </article>
            <article class="mdash-kpi mdash-kpi--warning">
                <p>SKU hết hàng khả dụng</p>
                <strong><fmt:formatNumber value="${d.outOfStockSkus}"/></strong>
                <span>Khả dụng bằng 0 ở ít nhất một kho</span>
            </article>
        </div>
    </section>

    <section class="mdash-charts" aria-label="Biểu đồ doanh số">
        <article class="mdash-panel">
            <div class="mdash-panel-heading">
                <div>
                    <h2>Doanh số theo ngày</h2>
                    <p>Từ đầu tháng tới hôm nay</p>
                </div>
                <span class="mdash-panel-tag">Triệu ₫</span>
            </div>
            <div id="mdash-daily-chart" class="mdash-chart" role="img" data-values="${d.dailyValues}"
                 aria-label="Biểu đồ đường doanh số theo ngày trong tháng"></div>
        </article>

        <article class="mdash-panel">
            <div class="mdash-panel-heading">
                <div>
                    <h2>Doanh số theo khu vực</h2>
                    <p>Theo khu vực của đại lý, từ đầu tháng</p>
                </div>
                <span class="mdash-panel-tag">Triệu ₫</span>
            </div>
            <div id="mdash-region-chart" class="mdash-chart" role="img" data-values="${d.areaValues}"
                 data-labels="<c:out value='${d.areaNames}'/>" aria-label="Biểu đồ cột doanh số theo khu vực"></div>
        </article>
    </section>

    <section class="mdash-panel" aria-labelledby="mdash-overdue-title">
        <div class="mdash-panel-heading">
            <div>
                <h2 id="mdash-overdue-title">Đại lý quá hạn công nợ</h2>
                <p>Sắp xếp theo số tiền quá hạn giảm dần</p>
            </div>
            <span class="mdash-panel-tag">Ưu tiên xử lý</span>
        </div>

        <div class="mdash-table-wrap">
            <table class="mdash-table">
                <thead>
                    <tr>
                        <th scope="col">Đại lý</th>
                        <th scope="col">Khu vực</th>
                        <th scope="col">Số ngày quá hạn</th>
                        <th scope="col">Số tiền quá hạn</th>
                    </tr>
                </thead>
                <tbody>
                    <c:if test="${empty d.overdue}">
                        <tr><td colspan="4">Không có đại lý nào quá hạn công nợ.</td></tr>
                    </c:if>
                    <c:forEach var="o" items="${d.overdue}">
                        <tr>
                            <td><c:out value="${o.code}"/> – <c:out value="${o.name}"/></td>
                            <td><c:out value="${o.area}"/></td>
                            <td><c:out value="${o.days}"/> ngày</td>
                            <td><fmt:formatNumber value="${o.amount}" maxFractionDigits="0"/> ₫</td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </div>
    </section>

</div>

<script defer src="${ctx}/assets/js/management-dashboard.js?v=${assetVersion}"></script>
<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>