(() => {
  "use strict";
  if (!document.getElementById("credit-screen")) return;
  const $ = (id) => document.getElementById(id);
  const money = (value) => `${new Intl.NumberFormat("vi-VN").format(value)} ₫`;
  const escapeHtml = (value) => String(value).replace(/[&<>"']/g, (char) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[char]);
  const dayMs = 86400000;
  const now = new Date();
  const day = Date.UTC(now.getFullYear(), now.getMonth(), now.getDate());
  const sampleDate = (age) => new Date(day - age * dayMs).toISOString().slice(0, 10);
  const agencies = [
    { code: "DL-001", name: "Đại lý Minh Anh", limit: 10000000, allowedDays: 30, debts: [{ issued: sampleDate(20), balance: 12000000 }] },
    { code: "DL-002", name: "Đại lý Thái Nguyên", limit: 15000000, allowedDays: 30, debts: [{ issued: sampleDate(45), balance: 7000000 }] },
    { code: "DL-003", name: "Đại lý Hương Giang", limit: 18000000, allowedDays: 45, debts: [{ issued: sampleDate(70), balance: 11000000 }, { issued: sampleDate(35), balance: 10000000 }] },
    { code: "DL-004", name: "Đại lý Bắc Ninh", limit: 12000000, allowedDays: 30, debts: [{ issued: sampleDate(14), balance: 6000000 }] },
    { code: "DL-005", name: "Đại lý Thành Công", limit: 10000000, allowedDays: 30, debts: [] }
  ];

  $("credit-agency").innerHTML = agencies.map((agency) => `<option value="${escapeHtml(agency.code)}">${escapeHtml(agency.code)} · ${escapeHtml(agency.name)}</option>`).join("");
  let rows = [];

  function evaluate(agency) {
    const debts = agency.debts.filter((debt) => debt.balance > 0);
    const balance = debts.reduce((sum, debt) => sum + debt.balance, 0);
    const oldestDays = debts.length ? Math.max(...debts.map((debt) => Math.max(0, Math.floor((day - Date.parse(`${debt.issued}T00:00:00Z`)) / dayMs)))) : 0;
    const overLimit = balance > agency.limit;
    const overDays = balance > 0 && oldestDays > agency.allowedDays;
    const reasons = [overLimit ? "Vượt hạn mức tiền" : "", overDays ? "Quá số ngày nợ" : ""].filter(Boolean);
    return { ...agency, balance, oldestDays, overLimit, overDays, reasons, flagged: reasons.length > 0 };
  }

  function refresh() {
    rows = agencies.map(evaluate);
    $("credit-flagged").textContent = rows.filter((row) => row.flagged).length;
    $("credit-over-limit").textContent = rows.filter((row) => row.overLimit).length;
    $("credit-over-days").textContent = rows.filter((row) => row.overDays).length;
    $("credit-clear").textContent = rows.filter((row) => !row.flagged).length;
    $("credit-checked-at").textContent = `Đánh giá mẫu lúc ${new Date().toLocaleString("vi-VN")}`;
    render();
  }

  function render() {
    const search = $("credit-search").value.trim().toLocaleLowerCase("vi-VN");
    const status = $("credit-status").value;
    const policy = $("credit-policy").value;
    const visible = rows.filter((row) =>
      (!search || `${row.code} ${row.name}`.toLocaleLowerCase("vi-VN").includes(search)) &&
      (!status || (status === "flagged") === row.flagged));
    $("credit-count").textContent = `${visible.length} đại lý`;
    $("credit-visible").textContent = `Hiển thị ${visible.length} đại lý`;
    $("credit-empty").hidden = visible.length !== 0;
    const orderState = (row) => row.flagged ? (policy === "review" ? "Cần duyệt (mẫu)" : "Chặn (mẫu)") : "Bình thường";
    $("credit-rows").innerHTML = visible.map((row) => `<tr>
      <th scope="row"><span class="credit-table__name">${escapeHtml(row.name)}</span><span class="credit-table__code">${escapeHtml(row.code)}</span></th>
      <td class="credit-number">${money(row.balance)}</td><td class="credit-number">${money(row.limit)}</td>
      <td>${row.balance ? `${row.oldestDays} / ${row.allowedDays} ngày` : `— / ${row.allowedDays} ngày`}</td>
      <td class="${row.flagged ? "credit-reason" : ""}">${row.flagged ? escapeHtml(row.reasons.join("; ")) : "Không có cờ"}</td>
      <td><span class="credit-state ${row.flagged ? "credit-state--flagged" : ""}">${orderState(row)}</span></td>
    </tr>`).join("");
    $("credit-mobile").innerHTML = visible.map((row) => `<article class="credit-mobile__card">
      <strong>${escapeHtml(row.name)}</strong><small>${escapeHtml(row.code)} · ${orderState(row)}</small>
      <p>Còn nợ: <b>${money(row.balance)}</b> / hạn mức ${money(row.limit)}</p>
      <small>Nợ lâu nhất: ${row.oldestDays} / ${row.allowedDays} ngày<br>${row.flagged ? escapeHtml(row.reasons.join("; ")) : "Không có cờ"}</small>
    </article>`).join("");
  }

  const filters = $("credit-filters");
  filters.addEventListener("input", render);
  filters.addEventListener("change", render);
  filters.addEventListener("reset", () => window.setTimeout(render, 0));
  $("credit-recheck").addEventListener("click", () => {
    refresh();
    $("credit-message").textContent = "Đã đánh giá lại dữ liệu mẫu. Chưa có tác vụ nền hoặc thay đổi đơn hàng thật.";
  });
  $("credit-payment-form").addEventListener("submit", (event) => {
    event.preventDefault();
    const agency = agencies.find((entry) => entry.code === $("credit-agency").value);
    const payment = Number($("credit-payment").value);
    const remaining = agency?.debts.reduce((sum, debt) => sum + debt.balance, 0) ?? 0;
    if (!agency || !Number.isSafeInteger(payment) || payment <= 0 || payment > remaining) {
      $("credit-message").textContent = `Nhập số nguyên dương không vượt dư nợ đang mở (${money(remaining)}).`;
      return;
    }
    let left = payment;
    agency.debts.sort((a, b) => a.issued.localeCompare(b.issued)).forEach((debt) => {
      const applied = Math.min(debt.balance, left);
      debt.balance -= applied;
      left -= applied;
    });
    refresh();
    $("credit-payment").value = "";
    $("credit-message").textContent = `Đã mô phỏng thu ${money(payment)} cho ${agency.name} và đánh giá lại cờ. Không ghi dữ liệu thật.`;
  });
  refresh();
})();
