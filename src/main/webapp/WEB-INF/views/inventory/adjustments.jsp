
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<c:set var="pageTitle" value="Điều chỉnh tồn kho"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

<section id="adjustment-screen"
         data-page-url="<c:out value='${ctx}/inventory/adjustments'/>"
         data-detail-url="<c:out value='${ctx}/inventory/adjustments/detail'/>"
         data-create-url="<c:out value='${ctx}/inventory/adjustments/create'/>"
         data-submit-url="<c:out value='${ctx}/inventory/adjustments/submit'/>"
         data-approve-url="<c:out value='${ctx}/inventory/adjustments/approve'/>"
         data-reject-url="<c:out value='${ctx}/inventory/adjustments/reject'/>"
         data-csrf="<c:out value='${csrfToken}'/>" data-user-id="<c:out value='${adjustmentCurrentUserId}'/>">

    <div class="page-header">
        <div>
            <p class="breadcrumb">
                Kho &amp; tồn kho / Điều chỉnh tồn kho
            </p>
            <h1>Điều chỉnh tồn kho</h1>
            <p class="page-header__desc">
                Lập phiếu xử lý hàng hỏng, mất hoặc hết hạn
                và gửi Quản lý kho phê duyệt.
            </p>
        </div>
    </div>

    <p class="alert alert--danger"
       id="adjustment-error"
       role="alert"
       hidden></p>

    <p class="alert alert--success"
       id="adjustment-success"
       role="status"
       hidden></p>

    <div class="grid grid--2">

        <section class="card">
            <div class="card__header">
                <h2>Lập phiếu điều chỉnh</h2>
            </div>

            <form id="adjustment-create" class="stack card__body">

                <label class="form-field">
                    <span class="form-label">Kho</span>
                    <select class="select"
                            id="adjustment-warehouse"
                            name="warehouseId"
                            required>
                        <option value="">— Chọn kho —</option>
                        <c:forEach var="w" items="${adjustmentWarehouses}"><option value="<c:out value='${w.id}'/>"><c:out value="${w.name}"/></option></c:forEach>
                    </select>
                </label>

                <label class="form-field">
                    <span class="form-label">Ghi chú chung</span>
                    <textarea class="textarea"
                              id="adjustment-note"
                              name="note"
                              maxlength="500"
                              rows="2"></textarea>
                </label>

                <div>
                    <h3>Chi tiết hàng điều chỉnh</h3>
                    <p class="text-muted">
                        Mỗi dòng phải có lý do, số lượng
                        và giá trị tổn thất.
                    </p>
                </div>

                <div id="adjustment-input-lines"
                     class="stack"></div>

                <button class="btn btn--secondary"
                        type="button"
                        id="adjustment-add-line">
                    + Thêm dòng hàng
                </button>

                <button class="btn btn--primary"
                        type="submit">
                    Lập phiếu điều chỉnh
                </button>
            </form>
        </section>

        <section class="card">
            <div class="card__header">
                <h2>Danh sách phiếu điều chỉnh</h2>
            </div>

            <div class="table-wrap">
                <table class="table">
                    <thead>
                    <tr>
                        <th>Mã phiếu</th>
                        <th>Kho</th>
                        <th>Trạng thái</th>
                        <th>Giá trị tổn thất</th>
                        <th>Chi tiết</th>
                    </tr>
                    </thead>
                    <tbody id="adjustment-list">
                    <tr>
                        <td colspan="5">
                            Đang tải danh sách...
                        </td>
                    </tr>
                    </tbody>
                </table>
            </div>
        </section>

    </div>

    <section class="card mt-4"
             id="adjustment-detail"
             hidden>

        <div class="card__header">
            <div>
                <h2 id="adjustment-detail-title">
                    Chi tiết phiếu điều chỉnh
                </h2>
                <p class="text-muted"
                   id="adjustment-detail-meta"></p>
            </div>

            <span class="badge"
                  id="adjustment-detail-status"></span>
        </div>

        <div class="table-wrap">
            <table class="table">
                <thead>
                <tr>
                    <th>STT</th>
                    <th>Mã sản phẩm</th>
                    <th>Lý do</th>
                    <th>Diễn giải</th>
                    <th>Số lượng giảm</th>
                    <th>Giá trị tổn thất</th>
                </tr>
                </thead>
                <tbody id="adjustment-detail-lines"></tbody>
            </table>
        </div>

        <div class="card__body">
            <p>
                <strong>Tổng giá trị tổn thất:</strong>
                <span id="adjustment-total-loss">0</span>
            </p>

            <div class="stack">
                <label class="form-field"
                       id="adjustment-rejection-field"
                       hidden>
                    <span class="form-label">
                        Lý do từ chối
                    </span>
                    <textarea class="textarea"
                              id="adjustment-rejection-reason"
                              maxlength="500"
                              rows="2"></textarea>
                </label>

                <div class="form-actions form-actions--start">
                    <button class="btn btn--primary"
                            type="button"
                            id="adjustment-submit"
                            hidden>
                        Gửi phê duyệt
                    </button>

                    <button class="btn btn--primary"
                            type="button"
                            id="adjustment-approve"
                            hidden>
                        Phê duyệt
                    </button>

                    <button class="btn btn--secondary"
                            type="button"
                            id="adjustment-reject"
                            hidden>
                        Từ chối
                    </button>
                </div>
            </div>
        </div>

    </section>

</section>

<script src="${ctx}/assets/js/inventory-adjustments.js?v=${assetVersion}"
        defer></script>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
