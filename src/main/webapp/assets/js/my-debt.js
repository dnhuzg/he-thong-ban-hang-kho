(() => {
  "use strict";

  // GHTK-73: dữ liệu mẫu cố định, KHÔNG phải công nợ của tài khoản thật.
  // Không lấy customerId từ URL hoặc từ người dùng nhập.
  const creditLimit = 150000000;

  const invoices = [
    {
      id: "HD-MAU-001",
      issued: "2026-10-01",
      due: "2026-10-15",
      amount: 45000000,
      remaining: 45000000,
      status: "unpaid"
    },
    {
      id: "HD-MAU-002",
      issued: "2026-09-15",
      due: "2026-09-30",
      amount: 32000000,
      remaining: 12000000,
      status: "overdue"
    },
    {
      id: "HD-MAU-003",
      issued: "2026-09-20",
      due: "2026-10-20",
      amount: 28000000,
      remaining: 8000000,
      status: "partial"
    },
    {
      id: "HD-MAU-004",
      issued: "2026-09-05",
      due: "2026-09-25",
      amount: 20000000,
      remaining: 0,
      status: "paid"
    }
  ];

  const statusLabels = {
    unpaid: "Chưa thanh toán",
    partial: "Thanh toán một phần",
    overdue: "Quá hạn",
    paid: "Đã thanh toán"
  };

  const money = value =>
    new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency: "VND",
      maximumFractionDigits: 0
    }).format(value);

  const date = value => {
    const parts = value.split("-");
    return `${parts[2]}/${parts[1]}/${parts[0]}`;
  };

  const escapeHtml = value => String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");

  const el = id => document.getElementById(id);

  const totalDebt = invoices.reduce(
    (sum, invoice) => sum + invoice.remaining, 0
  );

  const overdueDebt = invoices
    .filter(invoice => invoice.status === "overdue")
    .reduce((sum, invoice) => sum + invoice.remaining, 0);

  const available = Math.max(creditLimit - totalDebt, 0);
  const usedPercent = creditLimit > 0
    ? Math.round(totalDebt / creditLimit * 100)
    : 0;

  el("debt-total").textContent = money(totalDebt);
  el("debt-limit").textContent = money(creditLimit);
  el("debt-available").textContent = money(available);
  el("debt-overdue").textContent = money(overdueDebt);
  el("debt-percent").textContent = `${usedPercent}%`;
  el("debt-progress-fill").style.width =
    `${Math.min(usedPercent, 100)}%`;
  el("debt-progress").setAttribute(
    "aria-valuenow", Math.min(usedPercent, 100)
  );

  el("debt-limit-note").textContent =
    `Đã sử dụng ${money(totalDebt)} trong tổng hạn mức ${money(creditLimit)}.`;

  const rows = el("invoice-rows");
  const mobile = el("invoice-mobile");
  const search = el("invoice-search");
  const filter = el("invoice-filter");
  const dialog = el("invoice-dialog");

  let selectedInvoice = null;

  function statusBadge(invoice) {
    return `<span class="debt-status debt-status--${invoice.status}">
      ${statusLabels[invoice.status]}
    </span>`;
  }

  function actions(invoice) {
    const id = escapeHtml(invoice.id);
    return `<div class="debt-actions">
      <button class="debt-action" type="button"
        data-action="view" data-id="${id}">Xem</button>
      <button class="debt-action" type="button"
        data-action="download" data-id="${id}">Tải mẫu</button>
    </div>`;
  }

  function renderInvoices() {
    const keyword = search.value.trim().toLowerCase();
    const status = filter.value;

    const filtered = invoices.filter(invoice =>
      invoice.id.toLowerCase().includes(keyword) &&
      (status === "all" || invoice.status === status)
    );

    el("invoice-count").textContent = `${filtered.length} hóa đơn`;
    el("invoice-empty").hidden = filtered.length > 0;

    rows.innerHTML = filtered.map(invoice => `
      <tr>
        <td class="debt-code">${escapeHtml(invoice.id)}</td>
        <td>${date(invoice.issued)}</td>
        <td>${date(invoice.due)}</td>
        <td class="num">${money(invoice.amount)}</td>
        <td class="num"><strong>${money(invoice.remaining)}</strong></td>
        <td>${statusBadge(invoice)}</td>
        <td>${actions(invoice)}</td>
      </tr>
    `).join("");

    mobile.innerHTML = filtered.map(invoice => `
      <article class="debt-card">
        <div class="debt-card__head">
          <strong class="debt-code">${escapeHtml(invoice.id)}</strong>
          ${statusBadge(invoice)}
        </div>
        <div class="debt-card__details">
          <div><span>Ngày phát hành</span><strong>${date(invoice.issued)}</strong></div>
          <div><span>Hạn thanh toán</span><strong>${date(invoice.due)}</strong></div>
          <div><span>Số tiền</span><strong>${money(invoice.amount)}</strong></div>
          <div><span>Còn nợ</span><strong>${money(invoice.remaining)}</strong></div>
        </div>
        ${actions(invoice)}
      </article>
    `).join("");
  }

  function invoiceHtml(invoice) {
    return `
      <section class="debt-preview">
        <div class="debt-preview__watermark">
          HÓA ĐƠN MINH HỌA · KHÔNG CÓ GIÁ TRỊ PHÁP LÝ
        </div>
        <h2>Thông tin hóa đơn mẫu</h2>
        <p>Đây là bản xem trước giao diện GHTK-73, không phải hóa đơn điện tử đã phát hành.</p>
        <div class="debt-preview__grid">
          <div><span>Mã hóa đơn</span><strong>${escapeHtml(invoice.id)}</strong></div>
          <div><span>Trạng thái</span><strong>${statusLabels[invoice.status]}</strong></div>
          <div><span>Ngày phát hành</span><strong>${date(invoice.issued)}</strong></div>
          <div><span>Hạn thanh toán</span><strong>${date(invoice.due)}</strong></div>
          <div><span>Tổng tiền</span><strong>${money(invoice.amount)}</strong></div>
          <div><span>Đã thanh toán</span><strong>${money(invoice.amount - invoice.remaining)}</strong></div>
        </div>
        <div class="debt-preview__total">
          <span>Số tiền còn nợ</span>
          <h2>${money(invoice.remaining)}</h2>
        </div>
      </section>
    `;
  }

  function viewInvoice(invoice) {
    selectedInvoice = invoice;
    el("invoice-dialog-title").textContent =
      `Hóa đơn mẫu ${invoice.id}`;
    el("invoice-preview").innerHTML = invoiceHtml(invoice);

    if (!dialog.open) {
      dialog.showModal();
    }
  }

  function downloadInvoice(invoice) {
    // Tệp HTML mẫu; không giả lập hóa đơn PDF/XML thật.
    const documentHtml = `<!DOCTYPE html>
<html lang="vi">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Hoa don minh hoa ${escapeHtml(invoice.id)}</title>
<style>
body{font-family:Arial,sans-serif;max-width:760px;margin:40px auto;
padding:24px;color:#101828;line-height:1.6}
.notice{padding:16px;background:#fffaeb;color:#92400e;
font-weight:bold;text-align:center;border:2px solid #f59e0b}
section{margin-top:24px}strong{font-size:18px}
</style>
</head>
<body>
<div class="notice">DỮ LIỆU MINH HỌA - KHÔNG CÓ GIÁ TRỊ PHÁP LÝ</div>
${invoiceHtml(invoice)}
<p>GHTK-73 · Tài liệu giao diện mẫu, không phải hóa đơn đã phát hành.</p>
</body>
</html>`;

    const blob = new Blob(
      [documentHtml],
      { type: "text/html;charset=utf-8" }
    );

    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `${invoice.id}-MINH-HOA.html`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
  }

  function handleAction(event) {
    const button = event.target.closest("button[data-action]");
    if (!button) return;

    const invoice = invoices.find(item => item.id === button.dataset.id);
    if (!invoice) return;

    if (button.dataset.action === "view") {
      viewInvoice(invoice);
    } else if (button.dataset.action === "download") {
      downloadInvoice(invoice);
    }
  }

  rows.addEventListener("click", handleAction);
  mobile.addEventListener("click", handleAction);

  search.addEventListener("input", renderInvoices);
  filter.addEventListener("change", renderInvoices);

  el("invoice-close").addEventListener("click", () => dialog.close());
  el("invoice-done").addEventListener("click", () => dialog.close());

  el("invoice-download").addEventListener("click", () => {
    if (selectedInvoice) downloadInvoice(selectedInvoice);
  });

  renderInvoices();
})();