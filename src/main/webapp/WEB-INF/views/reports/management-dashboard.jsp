<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Dashboard điều hành"/>
<c:set var="pageStylesheet" value="management-dashboard.css"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

<div class="mdash">
    <header class="mdash-header">
        <div>
            <p class="mdash-eyebrow">TỔNG QUAN ĐIỀU HÀNH</p>
            <h1>Dashboard bán hàng &amp; kho</h1>
            <p class="mdash-subtitle">Theo dõi doanh số, đơn hàng, công nợ và tồn kho trong một màn hình.</p>
        </div>
        <span class="mdash-demo" role="status">DỮ LIỆU MINH HỌA — KHÔNG PHẢI SỐ LIỆU THỰC TẾ</span>
    </header>

    <section aria-labelledby="mdash-kpi-title">
        <div class="mdash-section-heading">
            <h2 id="mdash-kpi-title">Chỉ số tổng quan</h2>
            <p>6 chỉ số điều hành trọng tâm</p>
        </div>

        <div class="mdash-kpis">
            <article class="mdash-kpi">
                <p>Doanh số hôm nay</p>
                <strong id="mdash-today">—</strong>
                <span>VNĐ · Minh họa</span>
            </article>
            <article class="mdash-kpi">
                <p>Doanh số tháng này</p>
                <strong id="mdash-month">—</strong>
                <span>VNĐ · Minh họa</span>
            </article>
            <article class="mdash-kpi">
                <p>Đơn chờ duyệt</p>
                <strong id="mdash-approval">—</strong>
                <span>Đơn hàng · Minh họa</span>
            </article>
            <article class="mdash-kpi">
                <p>Đơn chờ xuất</p>
                <strong id="mdash-dispatch">—</strong>
                <span>Đơn hàng · Minh họa</span>
            </article>
            <article class="mdash-kpi mdash-kpi--danger">
                <p>Công nợ quá hạn</p>
                <strong id="mdash-debt">—</strong>
                <span>VNĐ · Minh họa</span>
            </article>
            <article class="mdash-kpi mdash-kpi--warning">
                <p>SKU dưới tồn tối thiểu</p>
                <strong id="mdash-stock">—</strong>
                <span>SKU · Minh họa</span>
            </article>
        </div>
    </section>

    <section class="mdash-charts" aria-label="Biểu đồ doanh số">
        <article class="mdash-panel">
            <div class="mdash-panel-heading">
                <div>
                    <h2>Doanh số theo ngày</h2>
                    <p>Dữ liệu mô phỏng trong tháng</p>
                </div>
                <span class="mdash-panel-tag">VNĐ</span>
            </div>
            <div id="mdash-daily-chart" class="mdash-chart" role="img"
                 aria-label="Biểu đồ đường doanh số theo ngày, dữ liệu minh họa"></div>
        </article>

        <article class="mdash-panel">
            <div class="mdash-panel-heading">
                <div>
                    <h2>Doanh số theo khu vực</h2>
                    <p>So sánh các vùng kinh doanh minh họa</p>
                </div>
                <span class="mdash-panel-tag">VNĐ</span>
            </div>
            <div id="mdash-region-chart" class="mdash-chart" role="img"
                 aria-label="Biểu đồ cột doanh số theo khu vực, dữ liệu minh họa"></div>
        </article>
    </section>

    <section class="mdash-panel" aria-labelledby="mdash-overdue-title">
        <div class="mdash-panel-heading">
            <div>
                <h2 id="mdash-overdue-title">Đại lý quá hạn công nợ</h2>
                <p>Sắp xếp theo số tiền quá hạn giảm dần · Dữ liệu minh họa</p>
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
                <tbody id="mdash-overdue-body">
                    <tr><td colspan="4">Đang chuẩn bị dữ liệu minh họa...</td></tr>
                </tbody>
            </table>
        </div>
    </section>

    <p class="mdash-note">
        Đây là bản trình diễn frontend GHTK-80. Chưa tích hợp API tổng hợp,
        chưa kiểm chứng số liệu thực tế và chưa xác nhận thời gian tải dưới 2 giây.
    </p>
</div>

<script defer src="${ctx}/assets/js/management-dashboard.js?v=${assetVersion}"></script>
<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>