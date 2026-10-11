(() => {
  const now = new Date();

  const trips = [
    {
      id: "CG-2026-1006-01",
      driver: "Nguyễn Văn Minh",
      vehicle: "29H-482.16",
      delivered: 5,
      total: 8,
      etaOffsetMinutes: 95,
      value: 18450000,
      cargo: [
        ["SKU-NUOC-01", "Nước khoáng 500ml", 36],
        ["SKU-TP-014", "Mì thùng", 18],
        ["SKU-GD-008", "Nước giặt 3L", 8]
      ]
    },
    {
      id: "CG-2026-1006-02",
      driver: "Trần Quốc Huy",
      vehicle: "99C-215.08",
      delivered: 3,
      total: 7,
      etaOffsetMinutes: -35,
      value: 26780000,
      cargo: [
        ["SKU-TP-021", "Dầu ăn 1L", 24],
        ["SKU-NUOC-09", "Nước ngọt lon", 48]
      ]
    },
    {
      id: "CG-2026-1006-03",
      driver: "Lê Hoàng Nam",
      vehicle: "20C-178.42",
      delivered: 6,
      total: 9,
      etaOffsetMinutes: 180,
      value: 12900000,
      cargo: [
        ["SKU-GD-011", "Giấy vệ sinh", 30],
        ["SKU-TP-033", "Bánh quy hộp", 16]
      ]
    },
    {
      id: "CG-2026-1006-04",
      driver: "Phạm Đức Anh",
      vehicle: "30G-661.25",
      delivered: 2,
      total: 6,
      etaOffsetMinutes: -80,
      value: 32150000,
      cargo: [
        ["SKU-NUOC-15", "Sữa hộp", 40],
        ["SKU-TP-042", "Gạo 5kg", 12],
        ["SKU-GD-019", "Nước rửa chén", 20]
      ]
    }
  ].map((trip) => ({
    ...trip,
    eta: new Date(now.getTime() + trip.etaOffsetMinutes * 60000)
  }));

  const rows = document.getElementById("delivery-rows");
  const mobile = document.getElementById("delivery-mobile");
  const search = document.getElementById("delivery-search");
  const status = document.getElementById("delivery-status");
  const filters = document.getElementById("delivery-filters");
  const empty = document.getElementById("delivery-empty");
  const modal = document.getElementById("delivery-modal");
  const detailTitle = document.getElementById("delivery-detail-title");
  const detailContent = document.getElementById("delivery-detail-content");

  const money = new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 0
  });

  const dateTime = new Intl.DateTimeFormat("vi-VN", {
    hour: "2-digit",
    minute: "2-digit",
    day: "2-digit",
    month: "2-digit",
    year: "numeric"
  });

  const isOverdue = (trip) => trip.eta.getTime() < now.getTime() && trip.delivered < trip.total;
  const remaining = (trip) => Math.max(0, trip.total - trip.delivered);

  function escapeHtml(value) {
    return String(value)
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll('"', "&quot;")
      .replaceAll("'", "&#039;");
  }

  function filteredTrips() {
    const keyword = search.value.trim().toLocaleLowerCase("vi");
    const state = status.value;

    return trips.filter((trip) => {
      const text = `${trip.id} ${trip.driver} ${trip.vehicle}`.toLocaleLowerCase("vi");
      const overdue = isOverdue(trip);

      return (!keyword || text.includes(keyword))
        && (!state || (state === "overdue" ? overdue : !overdue));
    });
  }

  function progress(trip) {
    return Math.round((trip.delivered / trip.total) * 100);
  }

  function statusMarkup(trip) {
    return isOverdue(trip)
      ? '<span class="delivery-status delivery-status--overdue">Quá ETA</span>'
      : '<span class="delivery-status delivery-status--normal">Đúng tiến độ</span>';
  }

  function render() {
    const data = filteredTrips();

    rows.innerHTML = data.map((trip) => `
      <tr class="${isOverdue(trip) ? "delivery-row--overdue" : ""}">
        <td class="delivery-trip"><strong>${escapeHtml(trip.id)}</strong><small>Đang giao</small></td>
        <td class="delivery-driver"><strong>${escapeHtml(trip.driver)}</strong><small>${escapeHtml(trip.vehicle)}</small></td>
        <td>
          <div class="delivery-progress">
            <div class="delivery-progress__label"><span>${trip.delivered}/${trip.total} điểm</span><strong>${progress(trip)}%</strong></div>
            <div class="delivery-progress__track"><div class="delivery-progress__bar" style="width:${progress(trip)}%"></div></div>
          </div>
        </td>
        <td class="delivery-responsibility">
          <strong>${remaining(trip)} điểm · ${money.format(trip.value)}</strong>
          <small>Vẫn thuộc chuyến</small>
        </td>
        <td>${dateTime.format(trip.eta)}</td>
        <td>${statusMarkup(trip)}</td>
        <td><button class="btn btn--secondary" type="button" data-trip="${escapeHtml(trip.id)}">Xem</button></td>
      </tr>
    `).join("");

    mobile.innerHTML = data.map((trip) => `
      <article class="delivery-card ${isOverdue(trip) ? "delivery-card--overdue" : ""}">
        <div class="delivery-card__top"><div><strong>${escapeHtml(trip.id)}</strong><br><small>${escapeHtml(trip.driver)} · ${escapeHtml(trip.vehicle)}</small></div>${statusMarkup(trip)}</div>
        <div class="delivery-progress">
          <div class="delivery-progress__label"><span>Đã giao ${trip.delivered}/${trip.total} điểm</span><strong>${progress(trip)}%</strong></div>
          <div class="delivery-progress__track"><div class="delivery-progress__bar" style="width:${progress(trip)}%"></div></div>
        </div>
        <dl>
          <div><dt>Còn lại</dt><dd>${remaining(trip)} điểm</dd></div>
          <div><dt>Hàng trên chuyến</dt><dd>${money.format(trip.value)}</dd></div>
          <div><dt>ETA</dt><dd>${dateTime.format(trip.eta)}</dd></div>
          <div><dt>Trách nhiệm</dt><dd>Vẫn thuộc chuyến</dd></div>
        </dl>
        <button class="btn btn--secondary" type="button" data-trip="${escapeHtml(trip.id)}">Xem chi tiết</button>
      </article>
    `).join("");

    const overdueCount = data.filter(isOverdue).length;
    const remainingCount = data.reduce((sum, trip) => sum + remaining(trip), 0);
    const totalValue = data.reduce((sum, trip) => sum + trip.value, 0);

    document.getElementById("stat-running").textContent = data.length;
    document.getElementById("stat-overdue").textContent = overdueCount;
    document.getElementById("stat-remaining").textContent = remainingCount;
    document.getElementById("stat-value").textContent = money.format(totalValue);
    document.getElementById("delivery-count").textContent = `${data.length} chuyến`;
    document.getElementById("delivery-alert-summary").textContent =
      overdueCount > 0
        ? `Có ${overdueCount} chuyến đã quá thời gian dự kiến. Cần kiểm tra tiến độ giao hàng.`
        : "";

    empty.hidden = data.length !== 0;
  }

  function openDetail(id) {
    const trip = trips.find((item) => item.id === id);
    if (!trip) return;

    detailTitle.textContent = trip.id;
    detailContent.innerHTML = `
      <div class="delivery-detail-grid">
        <div class="delivery-detail-box"><span>Tiến độ</span><strong>${trip.delivered}/${trip.total} điểm đã giao</strong></div>
        <div class="delivery-detail-box"><span>Điểm còn lại</span><strong>${remaining(trip)} điểm</strong></div>
        <div class="delivery-detail-box"><span>Giá trị đang trên đường</span><strong>${money.format(trip.value)}</strong></div>
      </div>
      <table class="delivery-cargo">
        <thead><tr><th>SKU</th><th>Hàng chưa giao</th><th>Số lượng</th></tr></thead>
        <tbody>
          ${trip.cargo.map((item) => `<tr><td>${escapeHtml(item[0])}</td><td>${escapeHtml(item[1])}</td><td>${item[2]}</td></tr>`).join("")}
        </tbody>
      </table>
      <div class="delivery-responsibility-note">
        <strong>Trách nhiệm hàng hóa:</strong>
        hàng chưa giao vẫn thuộc chuyến ${escapeHtml(trip.id)} và không được frontend tự động hoàn về kho.
      </div>
    `;
    modal.hidden = false;
    document.body.style.overflow = "hidden";
  }

  function closeDetail() {
    modal.hidden = true;
    document.body.style.overflow = "";
  }

  document.addEventListener("click", (event) => {
    const detailButton = event.target.closest("[data-trip]");
    if (detailButton) openDetail(detailButton.dataset.trip);
    if (event.target.closest("[data-close-detail]")) closeDetail();
  });

  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape" && !modal.hidden) closeDetail();
  });

  search.addEventListener("input", render);
  status.addEventListener("change", render);
  filters.addEventListener("reset", () => setTimeout(render, 0));

  render();
})();