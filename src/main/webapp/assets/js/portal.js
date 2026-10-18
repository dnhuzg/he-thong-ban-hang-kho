
(() => {
    'use strict';

    const app = document.getElementById('portalOrderApp');
    if (!app) return;

    const contextPath = app.dataset.contextPath || '';
    // CsrfFilter chặn mọi POST không kèm _csrf của phiên
    const csrfToken = app.dataset.csrf || '';

    const elements = {
        message: document.getElementById('portalMessage'),
        searchForm: document.getElementById('catalogSearchForm'),
        searchInput: document.getElementById('catalogSearch'),
        catalogStatus: document.getElementById('catalogStatus'),
        catalogLoading: document.getElementById('catalogLoading'),
        catalogEmpty: document.getElementById('catalogEmpty'),
        catalogGrid: document.getElementById('catalogGrid'),
        previousPage: document.getElementById('previousPage'),
        nextPage: document.getElementById('nextPage'),
        currentPage: document.getElementById('currentPage'),

        cartShortcut: document.getElementById('cartShortcut'),
        cartCount: document.getElementById('cartCount'),
        cartInitialState: document.getElementById('cartInitialState'),
        cartContent: document.getElementById('cartContent'),
        cartEmpty: document.getElementById('cartEmpty'),
        cartLines: document.getElementById('cartLines'),
        orderCode: document.getElementById('orderCode'),

        grossTotal: document.getElementById('grossTotal'),
        discountTotal: document.getElementById('discountTotal'),
        payableTotal: document.getElementById('payableTotal'),
        creditSummary: document.getElementById('creditSummary'),

        deliveryPoint: document.getElementById('deliveryPoint'),
        deliveryDate: document.getElementById('deliveryDate'),
        orderNote: document.getElementById('orderNote'),
        saveHeaderButton: document.getElementById('saveHeaderButton'),
        submitOrderButton: document.getElementById('submitOrderButton')
    };

    // S5-02: Đọc ID đơn hàng từ URL /portal?id=...
    const urlOrderId = new URLSearchParams(window.location.search).get('id');

    const initialOrderId =
        urlOrderId && /^[1-9]\d*$/.test(urlOrderId)
            ? urlOrderId
            : null;

    const state = {
        orderId: initialOrderId,
        query: '',
        page: 1,
        catalog: [],
        cart: null
    };

    const moneyFormatter = new Intl.NumberFormat('vi-VN', {
        style: 'currency',
        currency: 'VND',
        maximumFractionDigits: 0
    });

    function formatMoney(value) {
        const number = Number(value);
        return moneyFormatter.format(Number.isFinite(number) ? number : 0);
    }

    function escapeHtml(value) {
        return String(value ?? '')
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#039;');
    }

    function showMessage(message, isError = false) {
        elements.message.textContent = message;
        elements.message.classList.toggle('is-error', isError);
        elements.message.hidden = false;
    }

    function clearMessage() {
        elements.message.hidden = true;
        elements.message.textContent = '';
        elements.message.classList.remove('is-error');
    }

    async function request(path, options = {}) {
        const response = await fetch(`${contextPath}${path}`, {
            credentials: 'same-origin',
            ...options
        });

        let data = null;

        try {
            data = await response.json();
        } catch (error) {
            data = null;
        }

        if (!response.ok) {
            throw new Error(
                data?.error ||
                `Không thể xử lý yêu cầu (${response.status}).`
            );
        }

        return data;
    }

    function post(path, parameters = {}) {
        const body = new URLSearchParams();
        body.set('_csrf', csrfToken);

        Object.entries(parameters).forEach(([key, value]) => {
            if (value !== null && value !== undefined) {
                body.set(key, String(value));
            }
        });

        return request(path, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
            },
            body
        });
    }

    async function ensureCart() {
        if (state.orderId) return state.orderId;

        const result = await post('/api/portal/create');

        if (!result?.id) {
            throw new Error('Không thể khởi tạo giỏ hàng.');
        }

        state.orderId = result.id;
        return state.orderId;
    }

    async function loadCatalog() {
        elements.catalogLoading.hidden = false;
        elements.catalogEmpty.hidden = true;
        elements.catalogGrid.innerHTML = '';
        elements.catalogStatus.textContent = 'Đang tải...';

        try {
            const params = new URLSearchParams({
                q: state.query,
                page: String(state.page)
            });

            const result = await request(
                `/api/portal/catalog?${params.toString()}`
            );

            state.catalog = Array.isArray(result) ? result : [];
            renderCatalog();
        } catch (error) {
            elements.catalogLoading.hidden = true;
            elements.catalogStatus.textContent = 'Không thể tải sản phẩm';
            showMessage(error.message, true);
        }
    }

    function renderCatalog() {
        elements.catalogLoading.hidden = true;
        elements.catalogGrid.innerHTML = '';

        elements.catalogStatus.textContent =
            `${state.catalog.length} sản phẩm`;

        elements.currentPage.textContent = `Trang ${state.page}`;
        elements.previousPage.disabled = state.page <= 1;
        elements.nextPage.disabled = state.catalog.length < 25;

        if (state.catalog.length === 0) {
            elements.catalogEmpty.hidden = false;
            return;
        }

        elements.catalogEmpty.hidden = true;

        state.catalog.forEach(product => {
            const card = document.createElement('article');
            card.className = 'portal-product-card';

            const units = Array.isArray(product.units)
                ? product.units
                : [];

            const unitOptions = units.map(unit => `
                <option value="${escapeHtml(unit.id)}">
                    ${escapeHtml(unit.name)}
                </option>
            `).join('');

            card.innerHTML = `
                <div class="portal-muted">${escapeHtml(product.sku)}</div>
                <h3>${escapeHtml(product.name)}</h3>

                <div class="portal-product-price">
                    ${formatMoney(product.basePrice)}
                </div>

                <label>
                    Đơn vị tính
                    <select class="js-product-unit">
                        ${unitOptions}
                    </select>
                </label>

                <label>
                    Số lượng
                    <input class="js-product-quantity"
                           type="number"
                           min="1"
                           step="1"
                           value="1">
                </label>

                <button type="button"
                        class="portal-primary-button js-add-product">
                    Thêm vào giỏ
                </button>
            `;

            const addButton = card.querySelector('.js-add-product');

            addButton.addEventListener('click', async () => {
                const unitId =
                    card.querySelector('.js-product-unit').value;

                const quantity =
                    card.querySelector('.js-product-quantity').value;

                await addProduct(
                    product.productId,
                    unitId,
                    quantity,
                    addButton
                );
            });

            elements.catalogGrid.appendChild(card);
        });
    }

    async function addProduct(productId, unitId, quantity, button) {
        clearMessage();

        if (!unitId) {
            showMessage('Sản phẩm chưa có đơn vị tính hợp lệ.', true);
            return;
        }

        if (!quantity || Number(quantity) <= 0) {
            showMessage('Số lượng phải lớn hơn 0.', true);
            return;
        }

        button.disabled = true;

        try {
            const orderId = await ensureCart();

            await post('/api/portal/add', {
                id: orderId,
                productId,
                unitId,
                quantity
            });

            await loadCart();
            showMessage('Đã thêm sản phẩm vào giỏ hàng.');
        } catch (error) {
            showMessage(error.message, true);
        } finally {
            button.disabled = false;
        }
    }

    async function loadCart() {
        if (!state.orderId) {
            elements.cartInitialState.hidden = false;
            elements.cartInitialState.textContent =
                'Giỏ hàng chưa được khởi tạo.';
            return;
        }

        try {
            const params = new URLSearchParams({
                id: String(state.orderId)
            });

            state.cart = await request(
                `/api/portal/cart?${params.toString()}`
            );

            renderCart();
        } catch (error) {
            elements.cartInitialState.hidden = false;
            elements.cartInitialState.textContent =
                'Không thể tải giỏ hàng.';
            showMessage(error.message, true);
        }
    }

    function renderCart() {
        const cart = state.cart;
        if (!cart) return;

        elements.cartInitialState.hidden = true;
        elements.cartContent.hidden = false;

        elements.orderCode.textContent =
            cart.code ? `Mã đơn: ${cart.code}` : '';

        const lines = Array.isArray(cart.lines) ? cart.lines : [];

        elements.cartCount.textContent = String(lines.length);
        elements.cartLines.innerHTML = '';
        elements.cartEmpty.hidden = lines.length !== 0;

        lines.forEach(line => {
            const row = document.createElement('article');
            row.className = 'portal-cart-line';

            row.innerHTML = `
                <h3>${escapeHtml(line.name)}</h3>

                <div class="portal-muted">
                    ${escapeHtml(line.sku)}
                    · ${formatMoney(line.unitPrice)}
                </div>

                <div class="portal-cart-line-controls">
                    <input class="js-line-quantity"
                           type="number"
                           min="1"
                           step="1"
                           value="${escapeHtml(line.quantity)}">

                    <span>${escapeHtml(line.unit)}</span>

                    <button type="button"
                            class="portal-secondary-button js-update-line">
                        Cập nhật
                    </button>

                    <button type="button"
                            class="portal-secondary-button js-remove-line">
                        Xóa
                    </button>
                </div>

                <div class="portal-summary-row">
                    <span>Thành tiền</span>
                    <strong>${formatMoney(line.total)}</strong>
                </div>
            `;

            row.querySelector('.js-update-line')
                .addEventListener('click', async event => {
                    const quantity =
                        row.querySelector('.js-line-quantity').value;

                    await updateLine(
                        line.id,
                        line.unitId,
                        quantity,
                        event.currentTarget
                    );
                });

            row.querySelector('.js-remove-line')
                .addEventListener('click', async event => {
                    await removeLine(line.id, event.currentTarget);
                });

            elements.cartLines.appendChild(row);
        });

        elements.grossTotal.textContent = formatMoney(cart.gross);
        elements.discountTotal.textContent = formatMoney(cart.discount);
        elements.payableTotal.textContent = formatMoney(cart.total);

        renderCredit(cart.credit);
        renderDelivery(cart);

        // Chỉ gửi đơn đang ở trạng thái nháp và có sản phẩm.
        elements.submitOrderButton.disabled =
            lines.length === 0 || cart.status !== 'DRAFT';
    }

    function renderCredit(credit) {
        if (!credit) {
            elements.creditSummary.textContent =
                'Chưa có thông tin công nợ.';
            return;
        }

        elements.creditSummary.innerHTML = `
            <div class="portal-summary-row">
                <span>Công nợ hiện tại</span>
                <strong>${formatMoney(credit.currentDebt)}</strong>
            </div>

            <div class="portal-summary-row">
                <span>Đã cam kết</span>
                <strong>${formatMoney(credit.committed)}</strong>
            </div>

            <div class="portal-summary-row">
                <span>Hạn mức công nợ</span>
                <strong>${formatMoney(credit.limit)}</strong>
            </div>

            <div class="portal-summary-row">
                <span>Công nợ còn lại</span>
                <strong>${formatMoney(credit.remaining)}</strong>
            </div>

            ${
                credit.requiresApproval
                    ? `<p class="portal-submit-note">
                           Đơn hàng cần được nhân viên phụ trách duyệt.
                       </p>`
                    : ''
            }
        `;
    }

    function renderDelivery(cart) {
        const points = Array.isArray(cart.deliveryPoints)
            ? cart.deliveryPoints
            : [];

        elements.deliveryPoint.innerHTML =
            '<option value="">Chọn điểm giao hàng</option>';

        points.forEach(point => {
            const option = document.createElement('option');

            option.value = point.id;
            option.textContent = [
                point.address,
                point.receiver,
                point.phone
            ].filter(Boolean).join(' - ');

            if (
                cart.deliveryPointId !== null &&
                String(point.id) === String(cart.deliveryPointId)
            ) {
                option.selected = true;
            }

            elements.deliveryPoint.appendChild(option);
        });

        elements.deliveryDate.value = cart.deliveryDate || '';
        elements.orderNote.value = cart.note || '';
    }

    async function updateLine(lineId, unitId, quantity, button) {
        clearMessage();

        if (!quantity || Number(quantity) <= 0) {
            showMessage('Số lượng phải lớn hơn 0.', true);
            return;
        }

        button.disabled = true;

        try {
            await post('/api/portal/update', {
                id: state.orderId,
                lineId,
                unitId,
                quantity
            });

            await loadCart();
            showMessage('Đã cập nhật giỏ hàng.');
        } catch (error) {
            showMessage(error.message, true);
        } finally {
            button.disabled = false;
        }
    }

    async function removeLine(lineId, button) {
        clearMessage();
        button.disabled = true;

        try {
            await post('/api/portal/remove', {
                id: state.orderId,
                lineId
            });

            await loadCart();
            showMessage('Đã xóa sản phẩm khỏi giỏ hàng.');
        } catch (error) {
            showMessage(error.message, true);
            button.disabled = false;
        }
    }

    async function saveHeader() {
        if (!state.orderId) {
            showMessage('Giỏ hàng chưa được khởi tạo.', true);
            return;
        }

        clearMessage();
        elements.saveHeaderButton.disabled = true;

        try {
            await post('/api/portal/header', {
                id: state.orderId,
                deliveryPointId: elements.deliveryPoint.value,
                deliveryDate: elements.deliveryDate.value,
                note: elements.orderNote.value.trim()
            });

            await loadCart();
            showMessage('Đã lưu thông tin giao hàng.');
        } catch (error) {
            showMessage(error.message, true);
        } finally {
            elements.saveHeaderButton.disabled = false;
        }
    }

    async function submitOrder() {
        if (!state.orderId) {
            showMessage('Giỏ hàng chưa được khởi tạo.', true);
            return;
        }

        clearMessage();

        const confirmed = window.confirm(
            'Bạn có chắc muốn gửi đơn hàng để nhân viên phụ trách duyệt?'
        );

        if (!confirmed) return;

        elements.submitOrderButton.disabled = true;

        try {
            await post('/api/portal/header', {
                id: state.orderId,
                deliveryPointId: elements.deliveryPoint.value,
                deliveryDate: elements.deliveryDate.value,
                note: elements.orderNote.value.trim()
            });

            const result = await post('/api/portal/submit', {
                id: state.orderId
            });

            await loadCart();

            showMessage(
                `Đã gửi đơn hàng thành công. Trạng thái: ${
                    result?.status || 'Chờ duyệt'
                }.`
            );
        } catch (error) {
            showMessage(error.message, true);
            elements.submitOrderButton.disabled = false;
        }
    }

    elements.searchForm.addEventListener('submit', event => {
        event.preventDefault();

        state.query = elements.searchInput.value.trim();
        state.page = 1;

        clearMessage();
        loadCatalog();
    });

    elements.previousPage.addEventListener('click', () => {
        if (state.page <= 1) return;

        state.page -= 1;
        clearMessage();
        loadCatalog();
    });

    elements.nextPage.addEventListener('click', () => {
        state.page += 1;
        clearMessage();
        loadCatalog();
    });

    elements.cartShortcut.addEventListener('click', () => {
        document.getElementById('portalCart')?.scrollIntoView({
            behavior: 'smooth',
            block: 'start'
        });
    });

    elements.saveHeaderButton.addEventListener('click', saveHeader);
    elements.submitOrderButton.addEventListener('click', submitOrder);

    loadCatalog();

    // S5-02: Tự tải đơn đặt lại nếu URL có ID.
    if (state.orderId) {
        elements.cartInitialState.textContent =
            'Đang tải đơn hàng...';
        loadCart();
    } else {
        elements.cartInitialState.textContent =
            'Giỏ hàng chưa có sản phẩm.';
    }
})();
