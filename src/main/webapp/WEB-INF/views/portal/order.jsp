<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%
    String ctx = request.getContextPath();
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Cổng đặt hàng đại lý</title>

    <link rel="stylesheet" href="<%= ctx %>/assets/css/base.css">
    <link rel="stylesheet" href="<%= ctx %>/assets/css/components.css">
    <link rel="stylesheet" href="<%= ctx %>/assets/css/portal.css">
</head>

<body>
<main class="portal-page"
      id="portalOrderApp"
      data-context-path="<%= ctx %>"
      data-csrf="<c:out value='${csrfToken}'/>">

    <header class="portal-header">
        <div>
            <p class="portal-eyebrow">CỔNG DÀNH CHO ĐẠI LÝ</p>
            <h1>Đặt hàng</h1>
            <p class="portal-subtitle">
                Chọn sản phẩm theo bảng giá của đại lý và gửi đơn để nhân viên phụ trách xác nhận.
            </p>
        </div>

        <button type="button"
                class="portal-cart-button"
                id="cartShortcut">
            Giỏ hàng
            <span id="cartCount">0</span>
        </button>
    </header>

    <div id="portalMessage"
         class="portal-message"
         role="status"
         aria-live="polite"
         hidden></div>

    <section class="portal-toolbar" aria-label="Tìm kiếm sản phẩm">
        <form id="catalogSearchForm" class="portal-search">
            <label class="sr-only" for="catalogSearch">
                Tìm kiếm sản phẩm
            </label>

            <input
                id="catalogSearch"
                name="q"
                type="search"
                autocomplete="off"
                placeholder="Tìm theo tên hoặc mã sản phẩm">

            <button type="submit" class="portal-primary-button">
                Tìm kiếm
            </button>
        </form>
    </section>

    <div class="portal-layout">
        <section class="portal-catalog" aria-labelledby="catalogTitle">
            <div class="portal-section-heading">
                <div>
                    <p class="portal-section-label">SẢN PHẨM</p>
                    <h2 id="catalogTitle">Danh mục dành cho đại lý</h2>
                </div>

                <span id="catalogStatus" class="portal-muted">
                    Đang tải...
                </span>
            </div>

            <div id="catalogLoading" class="portal-state">
                Đang tải sản phẩm...
            </div>

            <div id="catalogEmpty" class="portal-state" hidden>
                Không tìm thấy sản phẩm phù hợp.
            </div>

            <div id="catalogGrid"
                 class="portal-product-grid"
                 aria-live="polite"></div>

            <div class="portal-pagination">
                <button type="button"
                        id="previousPage"
                        class="portal-secondary-button"
                        disabled>
                    Trang trước
                </button>

                <span id="currentPage">Trang 1</span>

                <button type="button"
                        id="nextPage"
                        class="portal-secondary-button">
                    Trang sau
                </button>
            </div>
        </section>

        <aside class="portal-cart"
               id="portalCart"
               aria-labelledby="cartTitle">

            <div class="portal-section-heading">
                <div>
                    <p class="portal-section-label">ĐƠN HÀNG</p>
                    <h2 id="cartTitle">Giỏ hàng của bạn</h2>
                </div>

                <span id="orderCode"
                      class="portal-order-code"></span>
            </div>

            <div id="cartInitialState" class="portal-state">
                Đang chuẩn bị giỏ hàng...
            </div>

            <div id="cartContent" hidden>
                <div id="cartEmpty" class="portal-state" hidden>
                    Giỏ hàng chưa có sản phẩm.
                </div>

                <div id="cartLines"
                     class="portal-cart-lines"></div>

                <section class="portal-summary"
                         aria-label="Tổng tiền đơn hàng">

                    <div class="portal-summary-row">
                        <span>Tiền hàng</span>
                        <strong id="grossTotal">0 ₫</strong>
                    </div>

                    <div class="portal-summary-row">
                        <span>Chiết khấu</span>
                        <strong id="discountTotal">0 ₫</strong>
                    </div>

                    <div class="portal-summary-row portal-summary-total">
                        <span>Tổng thanh toán</span>
                        <strong id="payableTotal">0 ₫</strong>
                    </div>
                </section>

                <section class="portal-credit"
                         aria-labelledby="creditTitle">

                    <div class="portal-section-heading portal-section-heading-small">
                        <h3 id="creditTitle">Thông tin công nợ</h3>
                    </div>

                    <div id="creditSummary"
                         class="portal-credit-content">
                        Đang cập nhật...
                    </div>
                </section>

                <section class="portal-delivery"
                         aria-labelledby="deliveryTitle">

                    <h3 id="deliveryTitle">Thông tin giao hàng</h3>

                    <label for="deliveryPoint">
                        Điểm giao hàng
                    </label>
                    <select id="deliveryPoint">
                        <option value="">Chọn điểm giao hàng</option>
                    </select>

                    <label for="deliveryDate">
                        Ngày giao dự kiến
                    </label>
                    <input id="deliveryDate" type="date">

                    <label for="orderNote">
                        Ghi chú
                    </label>
                    <textarea id="orderNote"
                              rows="3"
                              maxlength="500"
                              placeholder="Ghi chú cho đơn hàng"></textarea>

                    <button type="button"
                            id="saveHeaderButton"
                            class="portal-secondary-button portal-full-button">
                        Lưu thông tin giao hàng
                    </button>
                </section>

                <div class="portal-submit-area">
                    <p class="portal-submit-note">
                        Sau khi gửi, đơn hàng sẽ chuyển sang trạng thái
                        <strong>Chờ duyệt</strong> để nhân viên phụ trách xác nhận.
                    </p>

                    <button type="button"
                            id="submitOrderButton"
                            class="portal-primary-button portal-full-button">
                        Gửi đơn hàng
                    </button>
                </div>
            </div>
        </aside>
    </div>
</main>

<script src="<%= ctx %>/assets/js/portal.js"></script>
</body>
</html>