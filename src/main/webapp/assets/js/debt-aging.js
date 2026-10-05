(() => {
  "use strict";

  const root = document.getElementById("aging-screen");
  if (!root) return;

  const $ = (id) => document.getElementById(id);
  const dayMs = 86400000;
  const today = new Date();
  const todayUTC = Date.UTC(today.getFullYear(), today.getMonth(), today.getDate());
  const iso = (offset) => new Date(todayUTC + offset * dayMs).toISOString().slice(0, 10);
  const dateUTC = (value) => {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return NaN;
    const stamp = Date.parse(`${value}T00:00:00Z`);
    return Number.isFinite(stamp) && new Date(stamp).toISOString().slice(0, 10) === value ? stamp : NaN;
  };
  const money = (value) => `${new Intl.NumberFormat("vi-VN").format(value)} ₫`;
  const displayDate = (value) => value.split("-").reverse().join("/");
  const escapeHtml = (value) => String(value).replace(/[&<>"']/g, (char) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[char]);

  // Dữ liệu mẫu cố định theo ngày mở màn hình; không đọc hoặc ghi công nợ thật.
  const invoices = [
    { agency: "DL-001", name: "Đại lý Minh Anh", invoice: "HD-2401", issued: iso(-12), due: iso(5), amount: 8200000, paid: 3000000 },
    { agency: "DL-001", name: "Đại lý Minh Anh", invoice: "HD-2367", issued: iso(-48), due: iso(-18), amount: 11500000, paid: 5000000 },
    { agency: "DL-002", name: "Đại lý Thái Nguyên", invoice: "HD-2314", issued: iso(-77), due: iso(-46), amount: 9600000, paid: 2400000 },
    { agency: "DL-002", name: "Đại lý Thái Nguyên", invoice: "HD-2408", issued: iso(-8), due: iso(22), amount: 4200000, paid: 0 },
    { agency: "DL-003", name: "Đại lý Hương Giang", invoice: "HD-2278", issued: iso(-112), due: iso(-82), amount: 14500000, paid: 4500000 },
    { agency: "DL-004", name: "Đại lý Bắc Ninh", invoice: "HD-2355", issued: iso(-34), due: iso(-4), amount: 6800000, paid: 1800000 },
    { agency: "DL-005", name: "Đại lý Thành Công", invoice: "HD-2202", issued: iso(-126), due: iso(-96), amount: 7200000, paid: 7200000 }
  ];

  const filters = $("aging-filters");
  const asOf = $("aging-as-of");
  asOf.value = iso(0);
  let displayed = [];
  let displayTotals = null;

  const groupFor = (days) => days <= 30 ? 0 : days <= 60 ? 1 : days <= 90 ? 2 : 3;
  const emptyTotals = () => ({ amount: 0, paid: 0, balance: 0, buckets: [0, 0, 0, 0] });

  function render() {
    const stamp = dateUTC(asOf.value);
    if (!Number.isFinite(stamp)) return;
    const search = $("aging-search").value.trim().toLocaleLowerCase("vi-VN");
    const selectedBucket = $("aging-bucket").value;
    const overdueOnly = $("aging-overdue").checked;
    const grouped = new Map();
    const totals = emptyTotals();

    invoices.forEach((invoice) => {
      const issued = dateUTC(invoice.issued);
      const due = dateUTC(invoice.due);
      const balance = invoice.amount - invoice.paid;
      if (issued > stamp || balance <= 0) return;
      const days = Math.max(0, Math.floor((stamp - issued) / dayMs));
      const bucket = groupFor(days);
      const isOverdue = due < stamp;
      if (search && !`${invoice.agency} ${invoice.name} ${invoice.invoice}`.toLocaleLowerCase("vi-VN").includes(search)) return;
      if (selectedBucket !== "" && bucket !== Number(selectedBucket)) return;
      if (overdueOnly && !isOverdue) return;

      if (!grouped.has(invoice.agency)) grouped.set(invoice.agency, { agency: invoice.agency, name: invoice.name, ...emptyTotals(), overdue: false, invoices: [] });
      const agency = grouped.get(invoice.agency);
      agency.amount += invoice.amount;
      agency.paid += invoice.paid;
      agency.balance += balance;
      agency.buckets[bucket] += balance;
      agency.overdue ||= isOverdue;
      agency.invoices.push(invoice.invoice);
      totals.amount += invoice.amount;
      totals.paid += invoice.paid;
      totals.balance += balance;
      totals.buckets[bucket] += balance;
    });

    displayed = [...grouped.values()].sort((a, b) => b.buckets[3] - a.buckets[3] || b.balance - a.balance);
    displayTotals = totals;
    $("aging-total").textContent = money(totals.balance);
    totals.buckets.forEach((value, i) => {
      $(`aging-bucket-${i}`).textContent = money(value);
      $(`aging-table-${i}`).textContent = money(value);
    });
    $("aging-table-total").textContent = money(totals.balance);
    $("aging-count").textContent = `${displayed.length} đại lý`;
    $("aging-footer-count").textContent = `Hiển thị ${displayed.length} đại lý`;
    $("aging-balance").textContent = `Đối chiếu: ${money(totals.balance)} = ${money(totals.buckets.reduce((sum, value) => sum + value, 0))}`;
    $("aging-empty").hidden = displayed.length !== 0;

    $("aging-rows").innerHTML = displayed.map((agency) => `<tr>
      <th scope="row"><span class="aging-table__name">${escapeHtml(agency.name)}</span><span class="aging-table__meta">${escapeHtml(agency.agency)} · ${escapeHtml(agency.invoices.join(", "))}</span></th>
      <td class="aging-number">${money(agency.amount)}</td><td class="aging-number">${money(agency.paid)}</td>
      <td class="aging-number aging-table__balance">${money(agency.balance)}</td>
      ${agency.buckets.map((value) => `<td class="aging-number">${money(value)}</td>`).join("")}
      <td class="${agency.overdue ? "aging-table__late" : ""}">${agency.overdue ? "Có khoản quá hạn" : "Chưa quá hạn"}</td>
    </tr>`).join("");
    $("aging-mobile").innerHTML = displayed.map((agency) => `<article class="aging-mobile__card">
      <strong>${escapeHtml(agency.name)}</strong><small>${escapeHtml(agency.agency)} · ${agency.overdue ? "Có khoản quá hạn" : "Chưa quá hạn"}</small>
      <div class="aging-mobile__values"><span>Còn nợ</span><strong>${money(agency.balance)}</strong></div>
      <small>0–30: ${money(agency.buckets[0])} · 31–60: ${money(agency.buckets[1])}<br>61–90: ${money(agency.buckets[2])} · &gt;90: ${money(agency.buckets[3])}</small>
    </article>`).join("");
  }

  filters.addEventListener("input", render);
  filters.addEventListener("change", render);
  filters.addEventListener("reset", () => { window.setTimeout(() => { asOf.value = iso(0); render(); }, 0); });

  // CSV UTF-8 với BOM: Excel mở trực tiếp; mọi dòng và tổng dùng cùng bộ lọc trên màn hình.
  $("aging-export").addEventListener("click", () => {
    if (!displayTotals) return;
    const safe = (value) => {
      const text = String(value);
      const neutral = /^[\s]*[=+\-@]/.test(text) ? `'${text}` : text;
      return `"${neutral.replace(/"/g, '""')}"`;
    };
    const lines = [
      ["Ngày đối chiếu", displayDate(asOf.value)],
      ["Đại lý", "Mã đại lý", "Hoá đơn", "Phải thu (VND)", "Đã thu (VND)", "Còn nợ (VND)", "0–30 (VND)", "31–60 (VND)", "61–90 (VND)", ">90 (VND)", "Quá hạn"]
    ];
    displayed.forEach((agency) => lines.push([agency.name, agency.agency, agency.invoices.join(", "), agency.amount, agency.paid, agency.balance, ...agency.buckets, agency.overdue ? "Có" : "Không"]));
    lines.push(["TỔNG", "", "", displayTotals.amount, displayTotals.paid, displayTotals.balance, ...displayTotals.buckets, ""]);
    const csv = `\uFEFF${lines.map((line) => line.map(safe).join(",")).join("\r\n")}`;
    const url = URL.createObjectURL(new Blob([csv], { type: "text/csv;charset=utf-8" }));
    const link = document.createElement("a");
    link.href = url;
    link.download = `phan-tich-tuoi-no-${asOf.value}.csv`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.setTimeout(() => URL.revokeObjectURL(url), 1000);
  });

  render();
})();
