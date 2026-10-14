"use strict";

(() => {
    const groupSelect = document.getElementById("sales-group");
    const groupHeading = document.getElementById("sales-group-heading");
    const groupDescription =
        document.getElementById("sales-dimension-description");

    const startInput = document.getElementById("sales-start");
    const endInput = document.getElementById("sales-end");
    const applyButton = document.getElementById("sales-apply");
    const message = document.getElementById("sales-filter-message");

    if (!groupSelect || !groupHeading || !groupDescription ||
        !startInput || !endInput || !applyButton || !message) {
        return;
    }

    const groups = Object.freeze({
        employee: "Nhân viên",
        region: "Khu vực",
        agency: "Đại lý",
        category: "Nhóm hàng",
        sku: "SKU"
    });

    function updateDimension() {
        const label = groups[groupSelect.value];

        if (!label) {
            return;
        }

        groupHeading.textContent = label;
        groupDescription.textContent = "Chiều phân tích: " + label;
        message.textContent = "";
    }

    function applyFilter() {
        const startDate = startInput.value;
        const endDate = endInput.value;

        if (startDate && endDate && startDate > endDate) {
            message.textContent =
                "Ngày bắt đầu không được lớn hơn ngày kết thúc.";
            return;
        }

        message.textContent =
            "Đã chọn bộ lọc. Chưa thể tải báo cáo vì " +
            "backend chưa cung cấp API dữ liệu doanh số.";
    }

    groupSelect.addEventListener("change", updateDimension);
    applyButton.addEventListener("click", applyFilter);

    updateDimension();
})();