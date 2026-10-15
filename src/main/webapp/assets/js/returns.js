(() => {
  const root = document.getElementById('returns-screen');
  if (!root) return;
  const canCreate = root.dataset.canCreate === 'true';
  const canConfirm = root.dataset.canConfirm === 'true';
  const storageKey = 'oms.ghtk76.return-demo.v1';
  const invoices = [
    { id: 'HD-260901', customer: 'Đại lý Minh Anh', date: '01/10/2026', lines: [
      { id: 'a1', sku: 'SKU-NUOC-01', product: 'Nước khoáng 500ml', delivered: 48, returned: 8, lotManaged: true, lot: 'LOT-2608-A', stock: 132, damagedStock: 3 },
      { id: 'a2', sku: 'SKU-BANH-02', product: 'Bánh quy bơ hộp 300g', delivered: 24, returned: 0, lotManaged: false, lot: '', stock: 87, damagedStock: 0 }] },
    { id: 'HD-260903', customer: 'Tạp hóa Hồng Phúc', date: '03/10/2026', lines: [
      { id: 'b1', sku: 'SKU-SUA-03', product: 'Sữa tươi hộp 1L', delivered: 36, returned: 4, lotManaged: true, lot: 'LOT-2609-C', stock: 56, damagedStock: 2 },
      { id: 'b2', sku: 'SKU-DAU-05', product: 'Dầu ăn chai 1L', delivered: 12, returned: 0, lotManaged: false, lot: '', stock: 41, damagedStock: 1 }] }
  ];
  const $ = (id) => document.getElementById(id);
  const invoiceSelect = $('return-invoice'), lineSelect = $('return-line'), quantity = $('return-quantity');
  const reason = $('return-reason'), condition = $('return-condition'), lot = $('return-lot');
  let draft = [];
  const load = () => { try { return JSON.parse(localStorage.getItem(storageKey)) || { vouchers: [], inventory: {} }; } catch { return { vouchers: [], inventory: {} }; } };
  let state = load();
  const save = () => localStorage.setItem(storageKey, JSON.stringify(state));
  const esc = (value) => String(value).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  const findLine = (invoiceId, lineId) => invoices.find(x => x.id === invoiceId)?.lines.find(x => x.id === lineId);
  const reservations = (invoiceId, lineId) => state.vouchers.flatMap(v => v.lines).filter(x => x.invoiceId === invoiceId && x.lineId === lineId).reduce((sum, x) => sum + x.quantity, 0);
  const draftReservations = (invoiceId, lineId) => draft.filter(x => x.invoiceId === invoiceId && x.lineId === lineId).reduce((sum, x) => sum + x.quantity, 0);
  const remaining = (invoiceId, lineId) => { const line = findLine(invoiceId, lineId); return line ? Math.max(0, line.delivered - line.returned - reservations(invoiceId, lineId) - draftReservations(invoiceId, lineId)) : 0; };
  const selectedInvoice = () => invoices.find(x => x.id === invoiceSelect.value);
  function refreshLines() {
    const invoice = selectedInvoice();
    lineSelect.innerHTML = invoice ? '<option value="">— Chọn dòng hàng —</option>' + invoice.lines.map(line => `<option value="${line.id}" ${remaining(invoice.id, line.id) === 0 ? 'disabled' : ''}>${esc(line.sku)} · ${esc(line.product)}</option>`).join('') : '<option value="">Chọn hóa đơn trước</option>';
    lineSelect.disabled = !invoice;
    resetLineDetails();
  }
  function resetLineDetails() { quantity.value = ''; quantity.disabled = true; reason.value = ''; reason.disabled = true; condition.value = 'good'; condition.disabled = true; lot.innerHTML = '<option value="">Chọn dòng hàng trước</option>'; lot.disabled = true; $('return-add').disabled = true; $('return-line-info').textContent = 'Chọn hóa đơn để xem số lượng đã giao và đã trả.'; $('return-remaining').textContent = 'Còn có thể trả: —'; $('return-lot-wrap').classList.remove('is-hidden'); }
  function refreshSelectedLine() {
    const invoice = selectedInvoice(), line = findLine(invoice?.id, lineSelect.value);
    if (!invoice || !line) { resetLineDetails(); return; }
    const available = remaining(invoice.id, line.id);
    quantity.disabled = !available; quantity.max = available; quantity.value = '';
    reason.disabled = !available; condition.disabled = !available;
    lot.disabled = !available || !line.lotManaged;
    lot.innerHTML = line.lotManaged ? `<option value="${esc(line.lot)}">${esc(line.lot)} · Lô đã xuất</option>` : '<option value="">Sản phẩm không quản lý lô</option>';
    $('return-lot-wrap').classList.toggle('is-hidden', !line.lotManaged);
    $('return-add').disabled = !available;
    $('return-remaining').textContent = `Còn có thể trả: ${available} / ${line.delivered - line.returned} đơn vị`;
    $('return-line-info').textContent = `Đã giao ${line.delivered} · Đã trả ${line.returned} · ${line.lotManaged ? `Lô gốc ${line.lot}` : 'Không quản lý lô'}`;
  }
  function renderDraft() {
    const wrap = $('return-draft-wrap'); wrap.hidden = draft.length === 0;
    $('return-draft-lines').innerHTML = draft.map((x, i) => `<div class="returns-draft__line"><div><strong>${esc(x.product)}</strong><small>${esc(x.reason)} · ${x.condition === 'damaged' ? 'Kho hàng lỗi' : 'Hàng tốt'} · ${x.lotManaged ? `Lô ${esc(x.lot)}` : 'Không quản lý lô'}</small></div><span class="returns-draft__qty">${x.quantity} sp</span><button class="returns-draft__remove" type="button" data-remove="${i}" aria-label="Xóa dòng">Xóa</button></div>`).join('');
  }
  function renderVouchers() {
    const tbody = $('return-rows'); $('return-count').textContent = `${state.vouchers.length} phiếu`;
    $('return-empty').hidden = state.vouchers.length > 0;
    tbody.innerHTML = state.vouchers.slice().reverse().map(v => {
      const invoice = invoices.find(x => x.id === v.invoiceId);
      const qty = v.lines.reduce((n, x) => n + x.quantity, 0);
      const names = v.lines.map(x => x.product).join(', ');
      return `<tr><td><span class="returns-code">${esc(v.code)}</span><span class="returns-cell-sub">${v.lines.length} dòng hàng</span></td><td><span class="returns-cell-main">${esc(v.invoiceId)}</span><span class="returns-cell-sub">${esc(invoice?.customer || '')}</span></td><td><span class="returns-cell-main">${esc(names)}</span><span class="returns-cell-sub">Tổng ${qty} sản phẩm</span></td><td>${esc(v.date)}</td><td><span class="returns-badge ${v.status === 'received' ? 'is-done' : ''}">${v.status === 'received' ? 'Đã nhận hàng' : 'Chờ kho nhận'}</span></td><td>${canConfirm && v.status === 'pending' ? `<button class="returns-confirm" data-confirm="${esc(v.id)}">Xác nhận nhận hàng</button>` : ''}</td></tr>`;
    }).join('');
  }
  function renderInventory() {
    const rows = [];
    invoices.forEach(inv => inv.lines.forEach(line => {
      const extra = state.inventory[line.id] || { good: 0, damaged: 0 };
      rows.push(`<div class="returns-stock-row"><span>${esc(line.product)}<small>${line.sku}</small></span><strong>${line.stock + extra.good} tốt<small>${line.damagedStock + extra.damaged} lỗi</small></strong></div>`);
    }));
    $('return-inventory').innerHTML = rows.join('');
  }
  function render() { renderVouchers(); renderInventory(); }
  if (canCreate) {
    invoices.forEach(inv => invoiceSelect.insertAdjacentHTML('beforeend', `<option value="${esc(inv.id)}">${esc(inv.id)} · ${esc(inv.customer)} · ${esc(inv.date)}</option>`));
    invoiceSelect.addEventListener('change', refreshLines);
    lineSelect.addEventListener('change', refreshSelectedLine);
    $('return-add').addEventListener('click', () => {
      const invoice = selectedInvoice(), line = findLine(invoice?.id, lineSelect.value), n = Number(quantity.value);
      $('return-error').hidden = true;
      if (!invoice || !line || !Number.isInteger(n) || n < 1 || n > remaining(invoice.id, line.id) || !reason.value || (line.lotManaged && !lot.value)) {
        $('return-error').textContent = 'Kiểm tra hóa đơn, số lượng còn được trả, lý do và lô hàng trước khi thêm.'; $('return-error').hidden = false; return;
      }
      draft.push({ invoiceId: invoice.id, lineId: line.id, product: line.product, sku: line.sku, quantity: n, reason: reason.value, condition: condition.value, lot: line.lotManaged ? lot.value : '', lotManaged: line.lotManaged });
      renderDraft(); refreshLines();
    });
    $('return-draft-lines').addEventListener('click', e => {
      const button = e.target.closest('[data-remove]'); if (!button) return;
      draft.splice(Number(button.dataset.remove), 1); renderDraft(); refreshLines();
    });
    $('return-submit').addEventListener('click', () => {
      if (!draft.length) return;
      const groups = [...new Set(draft.map(x => x.invoiceId))];
      if (groups.length !== 1) { $('return-error').textContent = 'Mỗi phiếu chỉ áp dụng cho một hóa đơn gốc.'; $('return-error').hidden = false; return; }
      const totals = new Map(); draft.forEach(x => totals.set(`${x.invoiceId}|${x.lineId}`, (totals.get(`${x.invoiceId}|${x.lineId}`) || 0) + x.quantity));
      const conflict = [...totals].some(([key, n]) => { const [invoiceId, lineId] = key.split('|'); return n > remaining(invoiceId, lineId) + draftReservations(invoiceId, lineId); });
      if (conflict) { $('return-error').textContent = 'Số lượng còn được trả đã thay đổi. Hãy kiểm tra lại dòng hàng.'; $('return-error').hidden = false; refreshLines(); return; }
      const voucher = { id: `RET-${Date.now()}`, code: `TH-${String(state.vouchers.length + 1).padStart(4, '0')}`, invoiceId: groups[0], date: new Date().toLocaleDateString('vi-VN'), status: 'pending', lines: draft.slice() };
      state.vouchers.push(voucher); save(); draft = []; renderDraft(); render(); refreshLines();
    });
  }
  $('return-rows').addEventListener('click', e => {
    const button = e.target.closest('[data-confirm]'); if (!button || !canConfirm) return;
    const voucher = state.vouchers.find(v => v.id === button.dataset.confirm && v.status === 'pending'); if (!voucher) return;
    voucher.lines.forEach(item => {
      const line = findLine(item.invoiceId, item.lineId); if (!line) return;
      if (item.condition === 'damaged') state.inventory[line.id] = { ...(state.inventory[line.id] || {}), damaged: (state.inventory[line.id]?.damaged || 0) + item.quantity };
      else state.inventory[line.id] = { ...(state.inventory[line.id] || {}), good: (state.inventory[line.id]?.good || 0) + item.quantity };
    });
    voucher.status = 'received'; voucher.receivedDate = new Date().toLocaleDateString('vi-VN'); save(); render(); if (canCreate) refreshLines();
  });
  render();
})();
