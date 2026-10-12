(() => {
  const root = document.getElementById('credit-screen');
  if (!root) return;
  const returnKey = 'oms.ghtk76.return-demo.v1';
  const creditKey = 'oms.ghtk77.return-credit-demo.v1';
  const $ = id => document.getElementById(id);
  const money = value => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(value);
  const esc = value => String(value).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  const catalog = {
    'HD-260901': { customer: 'Đại lý Minh Anh', debt: 12850000, limit: 30000000, lines: { a1: { sku: 'SKU-NUOC-01', product: 'Nước khoáng 500ml', price: 6500 }, a2: { sku: 'SKU-BANH-02', product: 'Bánh quy bơ hộp 300g', price: 42000 } } },
    'HD-260903': { customer: 'Tạp hóa Hồng Phúc', debt: 8600000, limit: 20000000, lines: { b1: { sku: 'SKU-SUA-03', product: 'Sữa tươi hộp 1L', price: 34000 }, b2: { sku: 'SKU-DAU-05', product: 'Dầu ăn chai 1L', price: 56000 } } }
  };
  const sample = { id: 'RET-DEMO-01', code: 'TH-0008', invoiceId: 'HD-260901', date: '10/10/2026', receivedDate: '11/10/2026', status: 'received', lines: [{ invoiceId: 'HD-260901', lineId: 'a1', product: 'Nước khoáng 500ml', sku: 'SKU-NUOC-01', quantity: 6 }, { invoiceId: 'HD-260901', lineId: 'a2', product: 'Bánh quy bơ hộp 300g', sku: 'SKU-BANH-02', quantity: 2 }] };
  const loadJson = (key, fallback) => { try { return JSON.parse(localStorage.getItem(key)) || fallback; } catch { return fallback; } };
  const returnState = loadJson(returnKey, { vouchers: [] });
  let state = loadJson(creditKey, { documents: [], debtAdjustments: {} });
  let selectedId = null;
  const confirmed = (returnState.vouchers || []).filter(v => v.status === 'received');
  const returns = confirmed.length ? confirmed : [sample];
  const save = () => localStorage.setItem(creditKey, JSON.stringify(state));
  const lineInfo = item => catalog[item.invoiceId]?.lines[item.lineId] || { sku: item.sku || 'SKU', product: item.product || 'Sản phẩm trả', price: 0 };
  const returnTotal = voucher => voucher.lines.reduce((sum, item) => sum + lineInfo(item).price * Number(item.quantity || 0), 0);
  const adjustedIds = () => new Set(state.documents.map(doc => doc.returnId));
  const eligible = () => returns.filter(voucher => !adjustedIds().has(voucher.id));
  const selected = () => returns.find(voucher => voucher.id === selectedId);
  const customerData = invoiceId => catalog[invoiceId] || { customer: 'Đại lý', debt: 0, limit: 0, lines: {} };
  const debtAfterDocuments = invoiceId => {
    const base = customerData(invoiceId).debt;
    const reduced = state.documents.filter(doc => doc.invoiceId === invoiceId).reduce((sum, doc) => sum + doc.amount, 0);
    return Math.max(0, base - reduced);
  };
  function renderEligible() {
    const list = eligible();
    $('eligible-count').textContent = `${list.length} phiếu`;
    $('eligible-empty').hidden = list.length > 0;
    $('eligible-returns').innerHTML = list.map(voucher => {
      const invoice = customerData(voucher.invoiceId);
      return `<button type="button" class="credit-return ${voucher.id === selectedId ? 'is-active' : ''}" data-return-id="${esc(voucher.id)}"><span class="credit-return__top"><span class="credit-return__code">${esc(voucher.code)}</span><span class="credit-return__amount">${money(returnTotal(voucher))}</span></span><span class="credit-return__customer">${esc(invoice.customer)}</span><span class="credit-return__meta"><span>HĐ ${esc(voucher.invoiceId)}</span><span>Kho nhận ${esc(voucher.receivedDate || voucher.date || '—')}</span></span></button>`;
    }).join('');
  }
  function renderStats(voucher) {
    if (!voucher) { $('credit-debt').textContent = '—'; $('credit-limit').textContent = '—'; $('credit-available').textContent = '—'; return; }
    const invoice = customerData(voucher.invoiceId), debt = debtAfterDocuments(voucher.invoiceId), after = Math.max(0, debt - returnTotal(voucher));
    $('credit-debt').textContent = money(debt); $('credit-limit').textContent = money(invoice.limit); $('credit-available').textContent = money(Math.max(0, invoice.limit - after));
  }
  function renderDetail() {
    const voucher = selected();
    $('credit-detail-empty').hidden = Boolean(voucher); $('credit-detail-body').hidden = !voucher;
    if (!voucher) { $('credit-status').textContent = 'Chưa chọn'; $('credit-detail-note').textContent = 'Chọn một phiếu trả hàng ở bên trái.'; renderStats(null); return; }
    const invoice = customerData(voucher.invoiceId);
    $('credit-status').textContent = 'Đủ điều kiện'; $('credit-detail-note').textContent = 'Đơn giá được khóa theo hóa đơn gốc.';
    $('detail-return-code').textContent = voucher.code; $('detail-invoice').textContent = voucher.invoiceId; $('detail-customer').textContent = invoice.customer; $('detail-received').textContent = voucher.receivedDate || voucher.date || '—';
    $('credit-lines').innerHTML = voucher.lines.map(item => { const info = lineInfo(item), total = info.price * Number(item.quantity || 0); return `<tr><td class="credit-product"><strong>${esc(info.product)}</strong><small>${esc(info.sku)}</small></td><td>${item.quantity}</td><td>${money(info.price)}</td><td><strong>${money(total)}</strong></td></tr>`; }).join('');
    $('credit-total').textContent = money(returnTotal(voucher)); $('credit-error').hidden = true; renderStats(voucher);
  }
  function renderDocuments() {
    $('document-count').textContent = `${state.documents.length} chứng từ`; $('document-empty').hidden = state.documents.length > 0;
    $('credit-documents').innerHTML = state.documents.slice().reverse().map(doc => `<tr><td><span class="credit-doc-code">${esc(doc.code)}</span></td><td>${esc(doc.returnCode)}</td><td>${esc(doc.invoiceId)}</td><td>${esc(doc.customer)}</td><td class="credit-money">${money(doc.amount)}</td><td><span class="credit-done">Đã ghi nhận</span></td></tr>`).join('');
  }
  $('eligible-returns').addEventListener('click', event => { const button = event.target.closest('[data-return-id]'); if (!button) return; selectedId = button.dataset.returnId; renderEligible(); renderDetail(); });
  $('credit-create').addEventListener('click', () => {
    const voucher = selected(); if (!voucher || voucher.status !== 'received') { $('credit-error').textContent = 'Chỉ lập chứng từ từ phiếu trả đã được kho xác nhận.'; $('credit-error').hidden = false; return; }
    if (adjustedIds().has(voucher.id)) { $('credit-error').textContent = 'Phiếu trả này đã có chứng từ giảm trừ.'; $('credit-error').hidden = false; return; }
    const invoice = customerData(voucher.invoiceId), amount = returnTotal(voucher);
    if (amount <= 0) { $('credit-error').textContent = 'Không tìm thấy đơn giá trên hóa đơn gốc.'; $('credit-error').hidden = false; return; }
    state.documents.push({ id: `RC-${Date.now()}`, code: `DC-${String(state.documents.length + 1).padStart(4, '0')}`, returnId: voucher.id, returnCode: voucher.code, invoiceId: voucher.invoiceId, customer: invoice.customer, amount, createdAt: new Date().toLocaleDateString('vi-VN'), status: 'posted' });
    save(); selectedId = null; renderEligible(); renderDetail(); renderDocuments();
  });
  renderEligible(); renderDetail(); renderDocuments();
})();
