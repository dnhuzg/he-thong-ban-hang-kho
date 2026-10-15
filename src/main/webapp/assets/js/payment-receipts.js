(() => {
  "use strict";

  const screen = document.getElementById("payment-receipts");
  if (!screen) return;

  const $ = id => document.getElementById(id);

  // GHTK-75: Du lieu minh hoa, khong duoc ghi so ke toan.
  // Ma phieu duy nhat trong danh sach mau, khong phai ma backend cap.
  const receipts = [
    {
      code: "PT-MAU-001",
      date: "2026-10-08",
      agency: "Đại lý Minh Anh",
      amount: 2500000,
      method: "Tiền mặt",
      collector: "Nhân viên tuyến A"
    },
    {
      code: "PT-MAU-002",
      date: "2026-10-09",
      agency: "Đại lý Thái Nguyên",
      amount: 1800000,
      method: "Chuyển khoản",
      collector: "Nhân viên tuyến B"
    },
    {
      code: "PT-MAU-003",
      date: "2026-10-09",
      agency: "Đại lý Hương Giang",
      amount: 3200000,
      method: "Tiền mặt",
      collector: "Nhân viên tuyến C"
    }
  ];

  const formatMoney = amount =>
    new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency: "VND",
      maximumFractionDigits: 0
    }).format(amount);

  const formatDate = value => {
    const [year, month, day] = value.split("-");
    return `${day}/${month}/${year}`;
  };

  const escapeHtml = value => String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");

  const codes = receipts.map(item => item.code);
  if (new Set(codes).size !== codes.length) {
    console.error("GHTK-75: Ma phieu mau bi trung.");
    return;
  }

  let selectedReceipt = null;

  function showReceipt(receipt) {
    selectedReceipt = receipt;

    $("receipt-code").textContent = `Mã phiếu: ${receipt.code}`;
    $("receipt-date").textContent = formatDate(receipt.date);
    $("receipt-agency").textContent = receipt.agency;
    $("receipt-amount").textContent = formatMoney(receipt.amount);
    $("receipt-method").textContent = receipt.method;
    $("receipt-collector").textContent = receipt.collector;
    $("receipt-amount-highlight").textContent =
      formatMoney(receipt.amount);
  }

  $("payment-receipt-rows").innerHTML = receipts.map(receipt => `
    <tr>
      <td><strong>${escapeHtml(receipt.code)}</strong></td>
      <td>${escapeHtml(receipt.agency)}</td>
      <td>${formatMoney(receipt.amount)}</td>
      <td>
        <button class="btn btn--secondary payment-receipt-select"
                type="button"
                data-receipt-code="${escapeHtml(receipt.code)}">
          Xem phiếu
        </button>
      </td>
    </tr>
  `).join("");

  $("payment-receipt-rows").addEventListener("click", event => {
    const button = event.target.closest("[data-receipt-code]");
    if (!button) return;

    const receipt = receipts.find(
      item => item.code === button.dataset.receiptCode
    );

    if (receipt) showReceipt(receipt);
  });

  $("receipt-print").addEventListener("click", () => {
    if (!selectedReceipt) return;
    window.print();
  });

  showReceipt(receipts[0]);
})();