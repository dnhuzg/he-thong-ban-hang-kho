(() => {
  const stages = [
    { key: "approved", label: "Đã duyệt" },
    { key: "packing", label: "Đang soạn" },
    { key: "dispatched", label: "Đã xuất" },
    { key: "delivering", label: "Đang giao" },
    { key: "delivered", label: "Đã giao" }
  ];

  const orders = [
    {
      code: "DH-2026-1006-021",
      customer: "Đại lý Minh Anh",
      deliveryDate: "06/10/2026",
      current: "delivering",
      times: {
        approved: "05/10 · 08:15",
        packing: "05/10 · 14:20",
        dispatched: "06/10 · 07:10",
        delivering: "06/10 · 08:05"
      },
      lines: [
        { sku:"SKU-NUOC-01", name:"Nước khoáng 500ml", ordered:24, delivered:18 },
        { sku:"SKU-TP-014", name:"Mì thùng", ordered:10, delivered:10 },
        { sku:"SKU-GD-008", name:"Nước giặt 3L", ordered:6, delivered:0 }
      ]
    },
    {
      code: "DH-2026-1005-018",
      customer: "Đại lý Thành Công",
      deliveryDate: "06/10/2026",
      current: "delivered",
      times: {
        approved: "04/10 · 16:40",
        packing: "05/10 · 08:30",
        dispatched: "05/10 · 13:10",
        delivering: "05/10 · 13:45",
        delivered: "05/10 · 16:25"
      },
      lines: [
        { sku:"SKU-TP-021", name:"Dầu ăn 1L", ordered:12, delivered:12 },
        { sku:"SKU-NUOC-09", name:"Nước ngọt lon", ordered:48, delivered:48 },
        { sku:"SKU-TP-033", name:"Bánh quy hộp", ordered:8, delivered:8 }
      ]
    },
    {
      code: "DH-2026-1006-026",
      customer: "Đại lý Phương Nam",
      deliveryDate: "07/10/2026",
      current: "packing",
      times: {
        approved: "06/10 · 09:05",
        packing: "06/10 · 10:30"
      },
      lines: [
        { sku:"SKU-GD-011", name:"Giấy vệ sinh", ordered:30, delivered:0 },
        { sku:"SKU-NUOC-15", name:"Sữa hộp", ordered:20, delivered:0 }
      ]
    }
  ];

  const select = document.getElementById("order-select");
  const timeline = document.getElementById("delivery-timeline");
  const rows = document.getElementById("delivery-lines");
  const mobile = document.getElementById("delivery-lines-mobile");

  function escapeHtml(value) {
    return String(value)
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll('"', "&quot;")
      .replaceAll("'", "&#039;");
  }

  function stageIndex(key) {
    return stages.findIndex((stage) => stage.key === key);
  }

  function lineResult(line, order) {
    if (line.delivered >= line.ordered) {
      return '<span class="line-result line-result--full">Đủ</span>';
    }
    if (line.delivered > 0 || order.current === "delivered") {
      return '<span class="line-result line-result--partial">Giao thiếu</span>';
    }
    return '<span class="line-result line-result--waiting">Chưa giao</span>';
  }

  function render(order) {
    const currentIndex = stageIndex(order.current);
    const currentStage = stages[currentIndex];

    document.getElementById("customer-name").textContent = order.customer;
    document.getElementById("delivery-date").textContent = order.deliveryDate;
    document.getElementById("current-status").textContent = currentStage.label;
    document.getElementById("tracking-code").textContent = order.code;
    document.getElementById("tracking-status").textContent = currentStage.label;
    document.getElementById("line-count").textContent = `${order.lines.length} dòng`;

    timeline.innerHTML = stages.map((stage, index) => {
      let state = "is-pending";
      if (index < currentIndex || (index === currentIndex && order.current === "delivered")) state = "is-done";
      else if (index === currentIndex) state = "is-current";

      const time = order.times[stage.key] || "Chưa tới mốc";
      const marker = state === "is-done" ? "✓" : index + 1;

      return `
        <div class="timeline-step ${state}">
          <span class="timeline-step__dot">${marker}</span>
          <strong>${escapeHtml(stage.label)}</strong>
          <time>${escapeHtml(time)}</time>
        </div>
      `;
    }).join("");

    const note = document.getElementById("arrival-note");
    note.className = "arrival-note";

    if (order.current === "delivering") {
      note.classList.add("is-driving");
      note.innerHTML = "<strong>Xe đang giao hàng.</strong> Đại lý nên chuẩn bị người nhận và vị trí nhận hàng.";
    } else if (order.current === "delivered") {
      note.classList.add("is-delivered");
      note.innerHTML = "<strong>Đơn đã giao.</strong> Có thể đối chiếu số lượng thực giao của từng dòng hàng bên dưới.";
    } else {
      note.innerHTML = "<strong>Đơn chưa lên xe giao.</strong> Theo dõi timeline để biết khi đơn chuyển sang trạng thái Đang giao.";
    }

    rows.innerHTML = order.lines.map((line) => `
      <tr>
        <td class="delivery-product">
          <strong>${escapeHtml(line.name)}</strong>
          <small>${escapeHtml(line.sku)}</small>
        </td>
        <td class="num">${line.ordered}</td>
        <td class="num"><strong>${line.delivered}</strong></td>
        <td>${lineResult(line, order)}</td>
      </tr>
    `).join("");

    mobile.innerHTML = order.lines.map((line) => `
      <article class="delivered-card">
        <div class="delivered-card__head">
          <div><strong>${escapeHtml(line.name)}</strong><br><small>${escapeHtml(line.sku)}</small></div>
          ${lineResult(line, order)}
        </div>
        <dl>
          <div><dt>SL đặt</dt><dd>${line.ordered}</dd></div>
          <div><dt>SL thực giao</dt><dd>${line.delivered}</dd></div>
        </dl>
      </article>
    `).join("");

    const ordered = order.lines.reduce((sum, line) => sum + line.ordered, 0);
    const delivered = order.lines.reduce((sum, line) => sum + line.delivered, 0);
    document.getElementById("delivery-summary").textContent =
      `Tổng số lượng: đặt ${ordered} · thực giao ${delivered}`;
  }

  select.innerHTML = orders.map((order, index) =>
    `<option value="${index}">${escapeHtml(order.code)} · ${escapeHtml(order.customer)}</option>`
  ).join("");

  select.addEventListener("change", () => render(orders[Number(select.value)]));
  render(orders[0]);
})();