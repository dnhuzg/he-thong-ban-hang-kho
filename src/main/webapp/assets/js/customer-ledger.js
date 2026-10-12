(() => {
  "use strict";
  if (!document.getElementById("ledger-screen")) return;
  const $ = (id) => document.getElementById(id);
  const agencies = { "minh-anh": { name: "Đại lý Minh Anh", factor: 1 }, "thai-nguyen": { name: "Đại lý Thái Nguyên", factor: 2 }, "huong-giang": { name: "Đại lý Hương Giang", factor: 3 } };
  const today = new Date();
  const dateOf = (age) => { const d = new Date(today.getFullYear(), today.getMonth(), today.getDate() - age); return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`; };
  const todayKey = dateOf(0);
  $("ledger-from").value = dateOf(60);
  $("ledger-to").value = todayKey;
  $("ledger-to").max = todayKey;
  const money = (n) => `${new Intl.NumberFormat("vi-VN").format(n)} ₫`;
  const esc = (v) => String(v).replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c]);
  const seed = [
    { age: 90, code: "HD-101", description: "Bán hàng theo hoá đơn", increase: 1800000, decrease: 0 },
    { age: 75, code: "PT-102", description: "Thu tiền đại lý", increase: 0, decrease: 1200000 },
    { age: 52, code: "HD-103", description: "Bán hàng theo hoá đơn", increase: 2600000, decrease: 0 },
    { age: 41, code: "PT-104", description: "Thu tiền đại lý", increase: 0, decrease: 900000 },
    { age: 25, code: "DC-105", description: "Điều chỉnh giảm công nợ", increase: 0, decrease: 300000 },
    { age: 15, code: "HD-106", description: "Bán hàng theo hoá đơn", increase: 2100000, decrease: 0 },
    { age: 4, code: "PT-107", description: "Thu tiền đại lý", increase: 0, decrease: 1500000 }
  ];
  let current = null;
  function render() {
    const from = $("ledger-from").value, to = $("ledger-to").value;
    if (!/^\d{4}-\d{2}-\d{2}$/.test(from) || !/^\d{4}-\d{2}-\d{2}$/.test(to) || from > to || to > todayKey) {
      $("ledger-feedback").textContent = "Chọn khoảng ngày hợp lệ, ngày bắt đầu không sau ngày kết thúc và ngày kết thúc không ở tương lai.";
      $("ledger-pdf").disabled = true;
      $("ledger-excel").disabled = true;
      return;
    }
    $("ledger-feedback").textContent = "";
    const agency = agencies[$("ledger-agency").value];
    const movements = seed.map((row) => ({ ...row, date: dateOf(row.age), increase: row.increase * agency.factor, decrease: row.decrease * agency.factor })).sort((a, b) => a.date.localeCompare(b.date) || a.code.localeCompare(b.code));
    const base = 4000000 * agency.factor;
    const opening = base + movements.filter((r) => r.date < from).reduce((n, r) => n + r.increase - r.decrease, 0);
    const included = movements.filter((r) => r.date >= from && r.date <= to);
    let balance = opening;
    const rows = included.map((row) => { balance += row.increase - row.decrease; return { ...row, balance }; });
    const increase = rows.reduce((n, r) => n + r.increase, 0);
    const decrease = rows.reduce((n, r) => n + r.decrease, 0);
    current = { agency, from, to, opening, rows, increase, decrease, closing: balance };
    $("ledger-meta").textContent = `${agency.name} · ${from} đến ${to}`;
    $("ledger-opening").textContent = money(opening);
    $("ledger-increase").textContent = money(increase);
    $("ledger-decrease").textContent = money(decrease);
    $("ledger-closing").textContent = money(balance);
    $("ledger-total-increase").textContent = money(increase);
    $("ledger-total-decrease").textContent = money(decrease);
    $("ledger-total-closing").textContent = money(balance);
    $("ledger-empty").hidden = rows.length !== 0;
    $("ledger-rows").innerHTML = rows.map((r) => `<tr><td>${esc(r.date)}</td><td>${esc(r.code)}</td><td>${esc(r.description)}</td><td>${r.increase ? money(r.increase) : "—"}</td><td>${r.decrease ? money(r.decrease) : "—"}</td><td>${money(r.balance)}</td></tr>`).join("");
    $("ledger-pdf").disabled = false;
    $("ledger-excel").disabled = false;
  }
  $("ledger-filters").addEventListener("submit", (event) => { event.preventDefault(); render(); });
  $("ledger-agency").addEventListener("change", render);
  ["ledger-from", "ledger-to"].forEach((id) => $(id).addEventListener("change", render));
  $("ledger-pdf").addEventListener("click", () => { if (current) window.print(); });
  $("ledger-excel").addEventListener("click", () => {
    if (!current) return;
    const { agency, from, to, opening, rows, increase, decrease, closing } = current;
    const cells = [
      ["SỔ CHI TIẾT CÔNG NỢ ĐẠI LÝ · BẢN MẪU"],
      ["Đại lý", agency.name], ["Từ ngày", from, "Đến ngày", to],
      ["Số dư đầu khoảng", opening],
      ["Ngày", "Chứng từ", "Diễn giải", "Tăng (VND)", "Giảm (VND)", "Dư lũy kế (VND)"],
      ...rows.map((r) => [r.date, r.code, r.description, r.increase, r.decrease, r.balance]),
      ["Cộng trong khoảng", "", "", increase, decrease, closing]
    ];
    const bytes = window.OMSXlsx.build(cells);
    const url = URL.createObjectURL(new Blob([bytes], { type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" }));
    const a = document.createElement("a");
    a.href = url;
    a.download = `so-cong-no-${$("ledger-agency").value}-${from}-${to}.xlsx`;
    document.body.append(a);
    a.click();
    a.remove();
    window.setTimeout(() => URL.revokeObjectURL(url), 60000);
  });
  render();
})();
