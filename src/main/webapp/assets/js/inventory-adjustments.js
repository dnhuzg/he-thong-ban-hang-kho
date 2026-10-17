
"use strict";

(() => {
    const screen = document.getElementById("adjustment-screen");
    if (!screen) return;

    const $ = (id) => document.getElementById(id);

    const urls = {
        list: screen.dataset.pageUrl,
        detail: screen.dataset.detailUrl,
        create: screen.dataset.createUrl,
        submit: screen.dataset.submitUrl,
        approve: screen.dataset.approveUrl,
        reject: screen.dataset.rejectUrl
    };

    const csrf = screen.dataset.csrf;
    let selectedId = null;
    let currentUserId = null;

    const reasonLabels = {
        DAMAGED: "Hàng hỏng",
        LOST: "Hàng mất",
        EXPIRED: "Hết hạn phải hủy"
    };

    const statusLabels = {
        DRAFT: "Nháp",
        PENDING: "Chờ duyệt",
        APPROVED: "Đã duyệt",
        REJECTED: "Đã từ chối"
    };

    function showMessage(message, error = false) {
        const box = $(error ? "adjustment-error" : "adjustment-success");
        const other = $(error ? "adjustment-success" : "adjustment-error");

        other.hidden = true;
        box.textContent = message;
        box.hidden = false;
    }

    function formatMoney(value) {
        return new Intl.NumberFormat("vi-VN", {
            maximumFractionDigits: 2
        }).format(Number(value || 0)) + " ₫";
    }

    function cell(text) {
        const td = document.createElement("td");
        td.textContent = text == null ? "" : String(text);
        return td;
    }

    async function request(url, options = {}) {
        const response = await fetch(url, {
            credentials: "same-origin",
            ...options,
            headers: {
                "Accept": "application/json",
                ...(options.headers || {})
            }
        });

        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.error || "Thao tác không thành công.");
        }

        return data;
    }

    async function post(url, values) {
        const body = new URLSearchParams();

        for (const [key, value] of Object.entries(values)) {
            if (Array.isArray(value)) {
                value.forEach(item => body.append(key, item));
            } else {
                body.append(key, value == null ? "" : String(value));
            }
        }

        if (csrf) {
            body.append("_csrf", csrf);
        }

        return request(url, {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8"
            },
            body
        });
    }

    function input(label, type, className, required = true) {
        const wrapper = document.createElement("label");
        wrapper.className = "form-field";

        const caption = document.createElement("span");
        caption.className = "form-label";
        caption.textContent = label;

        const field = document.createElement("input");
        field.type = type;
        field.className = "input " + className;
        field.required = required;

        if (type === "number") {
            field.min = "0";
            field.step = "any";
        }

        wrapper.append(caption, field);
        return wrapper;
    }

    function addLine() {
        const container = document.createElement("div");
        container.className = "card adjustment-input-line";
        container.style.padding = "1rem";

        const product = input("Mã sản phẩm (ID)", "number", "adj-product");
        product.querySelector("input").min = "1";

        const reasonWrapper = document.createElement("label");
        reasonWrapper.className = "form-field";

        const reasonCaption = document.createElement("span");
        reasonCaption.className = "form-label";
        reasonCaption.textContent = "Lý do điều chỉnh";

        const reason = document.createElement("select");
        reason.className = "select adj-reason";
        reason.required = true;

        for (const [value, label] of Object.entries(reasonLabels)) {
            const option = document.createElement("option");
            option.value = value;
            option.textContent = label;
            reason.appendChild(option);
        }

        reasonWrapper.append(reasonCaption, reason);

        const detail = input("Diễn giải lý do", "text", "adj-reason-detail");
        detail.querySelector("input").maxLength = 500;

        const quantity = input("Số lượng điều chỉnh", "number", "adj-quantity");
        quantity.querySelector("input").min = "0.0001";
        quantity.querySelector("input").step = "0.0001";

        const loss = input("Giá trị tổn thất (VNĐ)", "number", "adj-loss");
        loss.querySelector("input").step = "0.01";

        const remove = document.createElement("button");
        remove.type = "button";
        remove.className = "btn btn--secondary";
        remove.textContent = "Xóa dòng";
        remove.addEventListener("click", () => {
            container.remove();
        });

        container.append(
            product,
            reasonWrapper,
            detail,
            quantity,
            loss,
            remove
        );

        $("adjustment-input-lines").appendChild(container);
    }

    async function loadList() {
        const rows = await request(urls.list + "?format=json");
        const tbody = $("adjustment-list");
        tbody.replaceChildren();

        if (!rows.length) {
            const tr = document.createElement("tr");
            const td = cell("Chưa có phiếu điều chỉnh.");
            td.colSpan = 5;
            tr.appendChild(td);
            tbody.appendChild(tr);
            return;
        }

        for (const item of rows) {
            const tr = document.createElement("tr");

            tr.append(
                cell(item.code),
                cell(item.warehouseId),
                cell(statusLabels[item.status] || item.status),
                cell(formatMoney(item.totalLossValue))
            );

            const actionCell = document.createElement("td");
            const button = document.createElement("button");

            button.type = "button";
            button.className = "btn btn--secondary";
            button.textContent = "Xem";

            button.addEventListener("click", () => {
                loadDetail(item.id).catch(error => {
                    showMessage(error.message, true);
                });
            });

            actionCell.appendChild(button);
            tr.appendChild(actionCell);
            tbody.appendChild(tr);
        }
    }

    async function loadDetail(id) {
        const data = await request(
            urls.detail + "?id=" + encodeURIComponent(id)
        );

        selectedId = id;

        const adjustment = data.adjustment;

        $("adjustment-detail").hidden = false;
        $("adjustment-detail-title").textContent =
            "Phiếu điều chỉnh " + adjustment.code;

        $("adjustment-detail-meta").textContent =
            "Kho #" + adjustment.warehouseId
            + " · Người lập #" + adjustment.createdBy;

        $("adjustment-detail-status").textContent =
            statusLabels[adjustment.status] || adjustment.status;

        $("adjustment-total-loss").textContent =
            formatMoney(adjustment.totalLossValue);

        const tbody = $("adjustment-detail-lines");
        tbody.replaceChildren();

        data.lines.forEach((line, index) => {
            const tr = document.createElement("tr");

            tr.append(
                cell(index + 1),
                cell(line.productId),
                cell(reasonLabels[line.reason] || line.reason),
                cell(line.reasonDetail),
                cell(line.quantity),
                cell(formatMoney(line.lossValue))
            );

            tbody.appendChild(tr);
        });

        const isCreator = currentUserId !== null &&
            String(adjustment.createdBy) === String(currentUserId);

        $("adjustment-submit").hidden =
            adjustment.status !== "DRAFT" || !isCreator;

        /*
         * Backend luôn kiểm tra WH_MANAGER và việc
         * người duyệt phải khác người lập.
         */
        const pending = adjustment.status === "PENDING";
        $("adjustment-approve").hidden = !pending || isCreator;
        $("adjustment-reject").hidden = !pending || isCreator;

        $("adjustment-rejection-field").hidden = true;
    }

    async function execute(action, message, values = {}) {
        if (selectedId == null) return;

        const id = selectedId;
        await post(urls[action], { id, ...values });

        showMessage(message);
        await loadList();
        await loadDetail(id);
    }

    $("adjustment-add-line").addEventListener("click", addLine);

    $("adjustment-create").addEventListener("submit", async (event) => {
        event.preventDefault();

        const lineElements = [
            ...document.querySelectorAll(".adjustment-input-line")
        ];

        if (!lineElements.length) {
            showMessage("Vui lòng thêm ít nhất một dòng hàng.", true);
            return;
        }

        const values = {
            warehouseId: $("adjustment-warehouse").value,
            note: $("adjustment-note").value,
            productId: [],
            reason: [],
            reasonDetail: [],
            quantity: [],
            lossValue: []
        };

        for (const row of lineElements) {
            values.productId.push(row.querySelector(".adj-product").value);
            values.reason.push(row.querySelector(".adj-reason").value);
            values.reasonDetail.push(
                row.querySelector(".adj-reason-detail").value
            );
            values.quantity.push(row.querySelector(".adj-quantity").value);
            values.lossValue.push(row.querySelector(".adj-loss").value);
        }

        try {
            const result = await post(urls.create, values);

            showMessage("Đã tạo phiếu điều chỉnh thành công.");
            $("adjustment-create").reset();
            $("adjustment-input-lines").replaceChildren();
            addLine();

            await loadList();
            await loadDetail(result.id);
        } catch (error) {
            showMessage(error.message, true);
        }
    });

    $("adjustment-submit").addEventListener("click", async () => {
        try {
            await execute("submit", "Đã gửi phiếu chờ phê duyệt.");
        } catch (error) {
            showMessage(error.message, true);
        }
    });

    $("adjustment-approve").addEventListener("click", async () => {
        if (!confirm("Xác nhận phê duyệt và trừ tồn kho?")) return;

        try {
            await execute("approve", "Đã phê duyệt và cập nhật tồn kho.");
        } catch (error) {
            showMessage(error.message, true);
        }
    });

    $("adjustment-reject").addEventListener("click", async () => {
        const field = $("adjustment-rejection-field");
        field.hidden = false;

        const reason = $("adjustment-rejection-reason").value.trim();

        if (!reason) {
            $("adjustment-rejection-reason").focus();
            showMessage("Nhập lý do từ chối rồi nhấn Từ chối lần nữa.", true);
            return;
        }

        try {
            await execute(
                "reject",
                "Đã từ chối phiếu điều chỉnh.",
                { rejectionReason: reason }
            );
            $("adjustment-rejection-reason").value = "";
        } catch (error) {
            showMessage(error.message, true);
        }
    });

    /*
     * Giá trị này sẽ được Servlet truyền vào JSP.
     * Nếu chưa có, backend vẫn bảo vệ quyền truy cập.
     */
    currentUserId = screen.dataset.userId || null;

    addLine();

    loadList().catch(error => {
        showMessage(error.message, true);
    });
})();
