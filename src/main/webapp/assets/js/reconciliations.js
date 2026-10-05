(() => {
  "use strict";
  const root = document.getElementById("recon-screen");
  if (!root) return;
  const $ = (id) => document.getElementById(id);
  const portal = root.dataset.portal === "true";
  const ownName = root.dataset.agency || "Đại lý của tôi";
  const money = (n) => `${new Intl.NumberFormat("vi-VN").format(n)} ₫`;
  const esc = (text) => String(text).replace(/[&<>"']/g, (char) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[char]);
  const now = new Date();
  const todayKey = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-${String(now.getDate()).padStart(2, "0")}`;
  const thisMonth = new Date(now.getFullYear(), now.getMonth(), 1);
  const lastMonth = new Date(now.getFullYear(), now.getMonth() - 1, 1);
  const keyFor = (date) => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}`;
  const periods = [lastMonth, thisMonth];
  $("recon-period").innerHTML = periods.map((date) => `<option value="${keyFor(date)}">Tháng ${String(date.getMonth() + 1).padStart(2, "0")}/${date.getFullYear()}${date === thisMonth ? " (tạm tính)" : ""}</option>`).join("");
  const state = new Map();
  const agencyNames = { "minh-anh": "Đại lý Minh Anh", "thai-nguyen": "Đại lý Thái Nguyên", "huong-giang": "Đại lý Hương Giang" };
  const agencyId = () => portal ? "portal-own" : $("recon-agency").value;
  const agencyName = () => portal ? ownName : agencyNames[agencyId()];
  const factor = () => ({ "minh-anh": 1, "thai-nguyen": 2, "huong-giang": 3, "portal-own": 1 }[agencyId()] || 1);
  const currentKey = () => `${agencyId()}:${$("recon-period").value}`;
  function activity(f, monthDate) {
    const ym = keyFor(monthDate);
    return [
      { date: `${ym}-05`, name: "Hoá đơn hàng bán (mẫu)", increase: 2400000 * f, payment: 0 },
      { date: `${ym}-12`, name: "Đại lý thanh toán (mẫu)", increase: 0, payment: 900000 * f },
      { date: `${ym}-21`, name: "Hoá đơn hàng bán (mẫu)", increase: 1200000 * f, payment: 0 }
    ];
  }
  function getDocument() {
    const date = periods.find((p) => keyFor(p) === $("recon-period").value);
    const f = factor();
    const previous = activity(f, lastMonth);
    const opening = 5000000 * f + (date === thisMonth ? previous.reduce((n, r) => n + r.increase - r.payment, 0) : 0);
    const rows = activity(f, date).filter((r) => r.date <= todayKey);
    const increase = rows.reduce((n, r) => n + r.increase, 0);
    const payment = rows.reduce((n, r) => n + r.payment, 0);
    return { date, opening, rows, increase, payment, closing: opening + increase - payment };
  }
  function render() {
    const data = getDocument();
    const ym = keyFor(data.date);
    const lastDay = data.date === thisMonth ? now.getDate() : new Date(data.date.getFullYear(), data.date.getMonth() + 1, 0).getDate();
    $("recon-subtitle").textContent = "BẢN XEM TRƯỚC · CHƯA CÓ GIÁ TRỊ XÁC NHẬN";
    $("recon-name").textContent = agencyName();
    $("recon-dates").textContent = `01/${ym.slice(5, 7)}/${ym.slice(0, 4)} – ${lastDay}/${ym.slice(5, 7)}/${ym.slice(0, 4)}`;
    $("recon-code").textContent = `DEMO-${ym.replace("-", "")}-${portal ? "OWN" : agencyId().toUpperCase()}`;
    $("recon-opening").textContent = money(data.opening);
    $("recon-increase").textContent = money(data.increase);
    $("recon-payment").textContent = money(data.payment);
    $("recon-closing").textContent = money(data.closing);
    $("recon-rows").innerHTML = data.rows.map((r) => `<tr><td>${esc(r.date)}</td><td>${esc(r.name)}</td><td>${r.increase ? money(r.increase) : "—"}</td><td>${r.payment ? money(r.payment) : "—"}</td></tr>`).join("");
    const response = state.get(currentKey());
    $("recon-status").textContent = response?.type === "accepted" ? "Đại lý đã xác nhận khớp số liệu trên bản mẫu." : response?.type === "disputed" ? `Đại lý phản hồi chênh lệch (mẫu): ${response.reason}` : "Trạng thái mẫu: Chờ đại lý xác nhận hoặc phản hồi.";
    if (portal) {
      $("recon-dispute-form").hidden = true;
      $("recon-reason").value = response?.type === "disputed" ? response.reason : "";
    }
  }
  $("recon-generate").addEventListener("click", render);
  $("recon-print").addEventListener("click", () => window.print());
  if (portal) {
    $("recon-accept").addEventListener("click", () => {
      if (!window.confirm("Xác nhận khớp số liệu của kỳ đang xem (chỉ trên trình duyệt)?")) return;
      state.set(currentKey(), { type: "accepted" });
      render();
    });
    $("recon-dispute-toggle").addEventListener("click", () => { $("recon-dispute-form").hidden = !$("recon-dispute-form").hidden; });
    $("recon-dispute-form").addEventListener("submit", (event) => {
      event.preventDefault();
      const reason = $("recon-reason").value.trim();
      if (reason.length < 10) { $("recon-reason").setCustomValidity("Nhập ít nhất 10 ký tự."); $("recon-reason").reportValidity(); return; }
      $("recon-reason").setCustomValidity("");
      state.set(currentKey(), { type: "disputed", reason });
      render();
    });
    $("recon-reason").addEventListener("input", () => $("recon-reason").setCustomValidity(""));
  }
  render();
})();
