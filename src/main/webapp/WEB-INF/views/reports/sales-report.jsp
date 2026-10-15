<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Báo cáo doanh số đa chiều"/>
<c:set var="pageStylesheet" value="sales-report.css"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

<main class="sales-report">

    <header class="sales-report__header">
        <div>
            <p class="sales-report__eyebrow">BÁO CÁO KINH DOANH</p>
            <h1>Báo cáo doanh số đa chiều</h1>
            <p class="sales-report__subtitle">
                Phân tích doanh số theo nhân viên, khu vực, đại lý,
                nhóm hàng và SKU.
            </p>
        </div>
        <button class="sales-report__export"
                type="button"
                disabled
                title="Chưa có API xuất báo cáo doanh số">
            Xuất Excel
        </button>
    </header>

    <section class="sales-report__notice" role="status">
        <strong>Chờ tích hợp dữ liệu thực tế</strong>
        <p>
            Giao diện đã được chuẩn bị. Doanh số, sản lượng,
            biên lợi nhuận và so sánh cùng kỳ sẽ xuất hiện khi
            backend cung cấp dữ liệu báo cáo hợp lệ.
            Hiện chưa có số liệu để xuất Excel.
        </p>
    </section>

    <section class="sales-report__panel" aria-labelledby="sales-filter-title">
        <div class="sales-report__section-title">
            <h2 id="sales-filter-title">Bộ lọc báo cáo</h2>
            <p>Lựa chọn chiều phân tích và khoảng thời gian cần xem.</p>
        </div>

        <div class="sales-report__filters">

            <div class="sales-report__field">
                <label for="sales-group">Nhóm theo</label>
                <select id="sales-group">
                    <option value="employee">Nhân viên</option>
                    <option value="region">Khu vực</option>
                    <option value="agency">Đại lý</option>
                    <option value="category">Nhóm hàng</option>
                    <option value="sku">SKU</option>
                </select>
            </div>

            <div class="sales-report__field">
                <label for="sales-start">Từ ngày</label>
                <input id="sales-start" type="date"/>
            </div>

            <div class="sales-report__field">
                <label for="sales-end">Đến ngày</label>
                <input id="sales-end" type="date"/>
            </div>

            <div class="sales-report__field sales-report__field--action">
                <button id="sales-apply" type="button">
                    Áp dụng bộ lọc
                </button>
            </div>
        </div>

        <p id="sales-filter-message"
           class="sales-report__filter-message"
           aria-live="polite"></p>
    </section>

    <section class="sales-report__metrics"
             aria-label="Tổng quan báo cáo">

        <article class="sales-report__metric">
            <span>Doanh số kỳ này</span>
            <strong>—</strong>
            <small>Chưa có dữ liệu</small>
        </article>

        <article class="sales-report__metric">
            <span>Sản lượng kỳ này</span>
            <strong>—</strong>
            <small>Theo đơn vị cơ sở</small>
        </article>

        <article class="sales-report__metric">
            <span>So với cùng kỳ trước</span>
            <strong>—</strong>
            <small>Chưa thể tính chênh lệch</small>
        </article>

        <c:if test="${canViewGrossMargin}">
            <article class="sales-report__metric">
                <span>Biên lợi nhuận gộp</span>
                <strong>—</strong>
                <small>Chờ xác minh giá vốn lịch sử</small>
            </article>
        </c:if>

    </section>

    <section class="sales-report__panel"
             aria-labelledby="sales-table-title">

        <div class="sales-report__section-title">
            <h2 id="sales-table-title">Chi tiết doanh số</h2>
            <p id="sales-dimension-description">
                Chiều phân tích: Nhân viên
            </p>
        </div>

        <div class="sales-report__table-wrap">
            <table>
                <thead>
                    <tr>
                        <th scope="col" id="sales-group-heading">Nhân viên</th>
                        <th scope="col">Doanh số</th>
                        <th scope="col">Sản lượng</th>
                        <th scope="col">Doanh số cùng kỳ</th>
                        <th scope="col">Chênh lệch (%)</th>
                        <c:if test="${canViewGrossMargin}">
                            <th scope="col">Biên LN gộp (%)</th>
                        </c:if>
                    </tr>
                </thead>

                <tbody>
                    <tr>
                        <td class="sales-report__empty"
                            colspan="${canViewGrossMargin ? 6 : 5}">
                            Chưa có dữ liệu báo cáo từ backend.
                            Vui lòng tích hợp API tổng hợp doanh số
                            trước khi sử dụng báo cáo.
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>
    </section>

    <section class="sales-report__panel sales-report__integration"
             aria-labelledby="sales-integration-title">
        <h2 id="sales-integration-title">Thông tin tích hợp</h2>
        <p>
            Báo cáo cần dữ liệu đơn hàng đã được ghi nhận doanh số,
            sản lượng quy đổi, chiều phân tích và giá vốn tại thời điểm
            giao dịch. Backend phải kiểm tra quyền khi trả biên lợi
            nhuận và khi xuất Excel.
        </p>
    </section>

</main>

<script defer
        src="${ctx}/assets/js/sales-report.js?v=${assetVersion}"></script>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>