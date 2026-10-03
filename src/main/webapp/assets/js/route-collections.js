(() => {
  "use strict";
  const screen = document.getElementById("route-screen");
  if (!screen) return;
  const $ = (id) => document.getElementById(id);
  const canConfirm = screen.dataset.canConfirm === "true";
  const canRecord = screen.dataset.canRecord === "true";
  const employee = screen.dataset.employee || "Nhân viên kinh doanh";
  const today = new Date();
  const isoDay = (date) => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
  const todayKey = isoDay(today);
  const yesterday = new Date(today.getFullYear(), today.getMonth(), today.getDate() - 1);
  const fmt = (amount) => `${new Intl.NumberFormat("vi-VN").format(amount)} ₫`;
  const esc = (text) => String(text).replace(/[&<>"']/g, (char) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[char]);
  const demo = [
    { id: 1, date: isoDay(yesterday), employee, agency: "Đại lý Minh Anh", amount: 2500000, status: "pending", receipt: null },
    { id: 2, date: todayKey, employee, agency: "Đại lý Thái Nguyên", amount: 1800000, status: "confirmed", receipt: null }
  ];
  if (canConfirm) demo.push({ id: 3, date: todayKey, employee: "Nhân viên tuyến B", agency: "Đại lý Hương Giang", amount: 3200000, status: "pending", receipt: null });
  let nextId = 4;
  const records = demo;
  const dialog = document.createElement("dialog");
  dialog.className = "route-dialog";
  const dialogHead = document.createElement("div");
  dialogHead.className = "route-dialog__head";
  const dialogTitle = document.createElement("strong");
  const close = document.createElement("button");
  close.type = "button";
  close.className = "btn btn--secondary";
  close.textContent = "Đóng";
  close.addEventListener("click", () => dialog.close());
  dialogHead.append(dialogTitle, close);
  const preview = document.createElement("img");
  preview.className = "route-image";
  preview.alt = "Ảnh biên lai được chọn";
  dialog.append(dialogHead, preview);
  document.body.append(dialog);
  dialog.addEventListener("close", () => { preview.removeAttribute("src"); });
  if (canRecord) {
    $("route-date").value = todayKey;
    $("route-date").max = todayKey;
  }

  function render() {
    const sum = (items) => items.reduce((acc, row) => acc + row.amount, 0);
    $("route-total").textContent = fmt(sum(records));
    $("route-pending").textContent = fmt(sum(records.filter((r) => r.status === "pending")));
    $("route-confirmed").textContent = fmt(sum(records.filter((r) => r.status === "confirmed")));
    const reps = [...new Set(records.map((r) => r.employee))].sort((a, b) => a.localeCompare(b, "vi"));
    const selected = $("route-filter-rep").value;
    $("route-filter-rep").innerHTML = `<option value="">Tất cả</option>${reps.map((r) => `<option value="${esc(r)}">${esc(r)}</option>`).join("")}`;
    $("route-filter-rep").value = reps.includes(selected) ? selected : "";
    const day = $("route-filter-day").value;
    const rep = $("route-filter-rep").value;
    const status = $("route-filter-status").value;
    const shown = records.filter((r) => (!day || r.date === day) && (!rep || r.employee === rep) && (!status || r.status === status));
    $("route-count").textContent = `${shown.length} khoản`;
    $("route-empty").hidden = shown.length > 0;
    $("route-rows").innerHTML = shown.map((r) => `<tr><td>${esc(r.date)}<span class="route-table__sub">${esc(r.employee)}</span></td><td>${esc(r.agency)}</td><td>${fmt(r.amount)}</td><td>${r.receipt ? `<button class="btn btn--ghost" type="button" data-preview="${r.id}">Xem ảnh</button>` : "Chưa có ảnh (mẫu)"}</td><td><span class="route-state route-state--${r.status}">${r.status === "pending" ? "Chờ nộp" : "Đã nộp"}</span></td><td>${canConfirm && r.status === "pending" ? `<button class="btn btn--secondary" type="button" data-confirm="${r.id}">Xác nhận đã nhận</button>` : "—"}</td></tr>`).join("");
    const groups = new Map();
    for (const r of records) {
      const key = JSON.stringify([r.date, r.employee]);
      if (!groups.has(key)) groups.set(key, { date: r.date, employee: r.employee, count: 0, total: 0, confirmed: 0 });
      const g = groups.get(key);
      g.count++;
      g.total += r.amount;
      if (r.status === "confirmed") g.confirmed += r.amount;
    }
    $("route-reconcile-empty").hidden = groups.size > 0;
    $("route-reconcile").innerHTML = [...groups.values()].sort((a, b) => b.date.localeCompare(a.date) || a.employee.localeCompare(b.employee, "vi")).map((g) => `<tr><td>${esc(g.date)}</td><td>${esc(g.employee)}</td><td>${g.count}</td><td>${fmt(g.total)}</td><td>${fmt(g.confirmed)}</td><td>${fmt(g.total - g.confirmed)}</td></tr>`).join("");
  }

  ["route-filter-day", "route-filter-rep", "route-filter-status"].forEach((id) => $(id).addEventListener("change", render));
  $("route-clear").addEventListener("click", () => {
    $("route-filter-day").value = "";
    $("route-filter-rep").value = "";
    $("route-filter-status").value = "";
    render();
  });
  $("route-rows").addEventListener("click", (event) => {
    const view = event.target.closest("[data-preview]");
    if (view) {
      const record = records.find((r) => r.id === Number(view.dataset.preview));
      if (!record?.receipt) return;
      dialogTitle.textContent = `Biên lai · ${record.agency}`;
      preview.src = record.receipt;
      dialog.showModal();
    }
    const button = event.target.closest("[data-confirm]");
    if (!button || !canConfirm) return;
    const record = records.find((r) => r.id === Number(button.dataset.confirm));
    if (!record || record.status !== "pending") return;
    if (!window.confirm(`Xác nhận đã nhận ${fmt(record.amount)} của ${record.employee} (mẫu)?`)) return;
    record.status = "confirmed";
    render();
  });

  if (canRecord) $("route-form").addEventListener("submit", (event) => {
    event.preventDefault();
    const amount = Number($("route-amount").value);
    const date = $("route-date").value;
    const agency = $("route-agency").value;
    const file = $("route-receipt").files[0];
    const feedback = $("route-feedback");
    if (!agency || !/^\d{4}-\d{2}-\d{2}$/.test(date) || date > todayKey || !Number.isSafeInteger(amount) || amount <= 0) {
      feedback.textContent = "Chọn đại lý, ngày thu hợp lệ và số tiền nguyên dương.";
      return;
    }
    if (!file || !["image/png", "image/jpeg", "image/webp"].includes(file.type) || file.size === 0 || file.size > 2 * 1024 * 1024) {
      feedback.textContent = "Chọn ảnh PNG, JPG hoặc WebP có dung lượng tối đa 2 MB.";
      return;
    }
    const reader = new FileReader();
    reader.onerror = () => { feedback.textContent = "Không đọc được ảnh biên lai. Hãy chọn ảnh khác."; };
    reader.onload = () => {
      records.unshift({ id: nextId++, date, employee, agency, amount, status: "pending", receipt: reader.result });
      $("route-form").reset();
      $("route-date").value = todayKey;
      feedback.textContent = "Đã thêm khoản thu mẫu ở trạng thái Chờ nộp. Làm mới trang sẽ xoá khoản thu này.";
      render();
    };
    reader.readAsDataURL(file);
  });
  render();
})();
