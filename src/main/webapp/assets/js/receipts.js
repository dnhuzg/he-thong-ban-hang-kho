(() => {
  'use strict';

  const screen = document.getElementById('receipt-screen');
  if (!screen) return;

  const writable = screen.dataset.canWrite === 'true';
  const storageKey = `oms-ghtk52-demo-${screen.dataset.userId || 'guest'}`;
  const products = {
    'SP-001': { base: 'chai', lot: true, units: [{ name: 'chai', factor: 1 }, { name: 'thùng', factor: 24 }] },
    'SP-002': { base: 'gói', lot: true, units: [{ name: 'gói', factor: 1 }, { name: 'thùng', factor: 20 }] },
    'SP-003': { base: 'cuộn', lot: false, units: [{ name: 'cuộn', factor: 1 }, { name: 'lốc', factor: 10 }] }
  };
  const form = document.getElementById('receipt-form');
  const lines = document.getElementById('receipt-lines');
  const template = document.getElementById('receipt-line-template');
  const error = document.getElementById('receipt-error');
  const dialog = document.getElementById('confirm-dialog');
  const toast = document.getElementById('receipt-toast');
  const today = () => {
    const date = new Date();
    return [date.getFullYear(), String(date.getMonth() + 1).padStart(2, '0'), String(date.getDate()).padStart(2, '0')].join('-');
  };
  const field = (row, key) => row.querySelector(`[data-field="${key}"]`);
  let saved = [];
  let currentId = null;
  let toastTimer;

  try {
    const parsed = JSON.parse(localStorage.getItem(storageKey) || '[]');
    if (Array.isArray(parsed)) saved = parsed.filter(item => item && typeof item === 'object');
  } catch (_) { /* Trình duyệt có thể tắt lưu trữ cục bộ. */ }

  function notice(message) {
    toast.textContent = message;
    toast.hidden = false;
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => { toast.hidden = true; }, 4000);
  }

  function persist() {
    try {
      localStorage.setItem(storageKey, JSON.stringify(saved.slice(0, 30)));
      return true;
    } catch (_) {
      notice('Trình duyệt không cho lưu dữ liệu mẫu.');
      return false;
    }
  }

  function updateUnits(row, selectedUnit = '') {
    const product = products[field(row, 'product').value];
    const unit = field(row, 'unit');
    unit.replaceChildren(new Option(product ? '— Chọn đơn vị —' : 'Chọn sản phẩm trước', ''));
    if (product) product.units.forEach(item => unit.add(new Option(`${item.name} (1 ${item.name} = ${item.factor} ${product.base})`, item.name)));
    unit.value = selectedUnit;
    field(row, 'lot').required = Boolean(product?.lot);
    field(row, 'expiry').required = Boolean(product?.lot);
    row.querySelector('[data-lot-required]').hidden = !product?.lot;
    row.querySelector('[data-expiry-required]').hidden = !product?.lot;
    updateConverted(row);
  }

  function updateConverted(row) {
    const product = products[field(row, 'product').value];
    const selected = product?.units.find(item => item.name === field(row, 'unit').value);
    const quantity = Number(field(row, 'quantity').value);
    row.querySelector('[data-converted]').textContent = selected && quantity > 0 && Number.isFinite(quantity)
      ? `Quy đổi: ${format(quantity * selected.factor)} ${product.base} (${format(quantity)} × ${selected.factor})`
      : 'Quy đổi: —';
    updateSummary();
  }

  const format = number => new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 3 }).format(number);

  function addLine(item = {}) {
    const row = template.content.firstElementChild.cloneNode(true);
    lines.append(row);
    field(row, 'product').value = item.product || '';
    updateUnits(row, item.unit || '');
    field(row, 'quantity').value = item.quantity ?? '';
    field(row, 'lot').value = item.lot || '';
    field(row, 'expiry').value = item.expiry || '';
    updateConverted(row);
    renumber();
  }

  function renumber() {
    [...lines.children].forEach((row, index) => {
      row.querySelector('[data-line-number]').textContent = index + 1;
      row.querySelector('[data-remove]').disabled = !writable || lines.children.length === 1 || Boolean(confirmed());
    });
    updateSummary();
  }

  function confirmed() { return saved.find(item => item.id === currentId)?.status === 'CONFIRMED'; }

  function updateSummary() {
    const supplier = document.getElementById('supplier');
    const warehouse = document.getElementById('warehouse');
    document.getElementById('summary-supplier').textContent = supplier.selectedOptions[0]?.textContent || 'Chưa chọn';
    if (!supplier.value) document.getElementById('summary-supplier').textContent = 'Chưa chọn';
    document.getElementById('summary-warehouse').textContent = warehouse.value ? warehouse.selectedOptions[0].textContent : 'Chưa chọn';
    let converted = 0;
    let count = 0;
    for (const row of lines.children) {
      const product = products[field(row, 'product').value];
      const selected = product?.units.find(item => item.name === field(row, 'unit').value);
      const quantity = Number(field(row, 'quantity').value);
      if (product) count++;
      if (selected && Number.isFinite(quantity) && quantity > 0) converted++;
    }
    document.getElementById('summary-items').textContent = count;
    document.getElementById('summary-units').textContent = converted;
    document.getElementById('summary-status').textContent = confirmed() ? 'Đã xác nhận' : currentId ? 'Bản nháp' : 'Đang lập';
  }

  function readForm(status) {
    return {
      id: currentId || String(Date.now()),
      status,
      supplier: document.getElementById('supplier').value,
      document: document.getElementById('document').value.trim(),
      date: document.getElementById('received-date').value,
      warehouse: document.getElementById('warehouse').value,
      note: document.getElementById('note').value.trim(),
      items: [...lines.children].map(row => ({
        product: field(row, 'product').value,
        quantity: field(row, 'quantity').value,
        unit: field(row, 'unit').value,
        factor: products[field(row, 'product').value]?.units.find(unit => unit.name === field(row, 'unit').value)?.factor || 0,
        lot: field(row, 'lot').value.trim(),
        expiry: field(row, 'expiry').value
      }))
    };
  }

  function validate() {
    error.hidden = true;
    if (!form.reportValidity()) return false;
    const entry = readForm('CONFIRMED');
    if (!entry.items.length || entry.items.some(item => !item.product || !item.unit || !Number.isFinite(Number(item.quantity)) || Number(item.quantity) <= 0 || Number(item.quantity) * item.factor > 1e9)) {
      error.textContent = 'Chọn sản phẩm, đơn vị và số lượng hợp lệ cho từng dòng.';
    } else if (entry.items.some(item => products[item.product]?.lot && (!item.lot || !item.expiry))) {
      error.textContent = 'Điền số lô và hạn dùng cho các sản phẩm quản lý theo lô.';
    } else if (entry.items.some(item => item.expiry && item.expiry < entry.date)) {
      error.textContent = 'Hạn dùng không được trước ngày nhập.';
    } else if (saved.some(item => item.id !== entry.id && item.status === 'CONFIRMED' && item.supplier === entry.supplier && item.document.toLowerCase() === entry.document.toLowerCase())) {
      error.textContent = 'Số chứng từ này đã có phiếu xác nhận cho nhà cung cấp đã chọn.';
    } else {
      return true;
    }
    error.hidden = false;
    error.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    return false;
  }

  function renderHistory() {
    const container = document.getElementById('receipt-history');
    container.replaceChildren();
    if (!saved.length) {
      const empty = document.createElement('p');
      empty.className = 'text-muted';
      empty.textContent = 'Chưa có phiếu nào.';
      container.append(empty);
      return;
    }
    saved.slice().reverse().forEach(entry => {
      const button = document.createElement('button');
      button.type = 'button';
      button.className = `receipt-history__item${entry.id === currentId ? ' is-selected' : ''}`;
      const title = document.createElement('strong');
      title.textContent = entry.document || 'Bản nháp chưa có chứng từ';
      const meta = document.createElement('span');
      meta.textContent = `${entry.date || 'Chưa chọn ngày'} · ${entry.status === 'CONFIRMED' ? 'Đã xác nhận' : 'Bản nháp'}`;
      button.append(title, meta);
      button.addEventListener('click', () => loadEntry(entry));
      container.append(button);
    });
  }

  function lockForm() {
    const locked = !writable || Boolean(confirmed());
    form.querySelectorAll('input, select, textarea').forEach(input => { input.disabled = locked; });
    document.getElementById('add-line').hidden = locked;
    document.getElementById('save-draft').hidden = locked;
    form.querySelector('[type="submit"]').hidden = locked;
    document.getElementById('receipt-status').textContent = confirmed() ? 'Đã xác nhận' : currentId ? 'Bản nháp' : 'Phiếu mới';
    document.getElementById('reset-form').textContent = locked && writable ? 'Lập phiếu mới' : 'Làm mới';
    document.getElementById('reset-form').hidden = !writable;
    renumber();
  }

  function loadEntry(entry) {
    currentId = entry.id;
    document.getElementById('supplier').value = entry.supplier || '';
    document.getElementById('document').value = entry.document || '';
    document.getElementById('received-date').value = entry.date || today();
    document.getElementById('warehouse').value = entry.warehouse || '';
    document.getElementById('note').value = entry.note || '';
    lines.replaceChildren();
    (Array.isArray(entry.items) && entry.items.length ? entry.items : [{}]).forEach(addLine);
    error.hidden = true;
    lockForm();
    renderHistory();
    updateSummary();
  }

  function newEntry() {
    currentId = null;
    form.reset();
    document.getElementById('received-date').value = today();
    lines.replaceChildren();
    addLine();
    error.hidden = true;
    lockForm();
    renderHistory();
    updateSummary();
  }

  function save(status) {
    if (!writable || confirmed()) return;
    const entry = readForm(status);
    saved = saved.filter(item => item.id !== entry.id);
    saved.push(entry);
    currentId = entry.id;
    if (persist()) notice(status === 'CONFIRMED' ? 'Đã xác nhận phiếu mẫu; tồn kho thật chưa thay đổi.' : 'Đã lưu bản nháp trên trình duyệt.');
    lockForm();
    renderHistory();
    updateSummary();
  }

  lines.addEventListener('change', event => {
    const row = event.target.closest('.receipt-line');
    if (!row) return;
    if (event.target.dataset.field === 'product') updateUnits(row);
    updateConverted(row);
  });
  lines.addEventListener('input', event => {
    const row = event.target.closest('.receipt-line');
    if (row) updateConverted(row);
  });
  lines.addEventListener('click', event => {
    if (!event.target.matches('[data-remove]') || !writable || confirmed() || lines.children.length === 1) return;
    event.target.closest('.receipt-line').remove();
    renumber();
  });
  form.addEventListener('change', updateSummary);
  document.getElementById('add-line').addEventListener('click', () => addLine());
  document.getElementById('reset-form').addEventListener('click', newEntry);
  document.getElementById('save-draft').addEventListener('click', () => save('DRAFT'));
  form.addEventListener('submit', event => {
    event.preventDefault();
    if (writable && !confirmed() && validate()) dialog.showModal();
  });
  dialog.addEventListener('close', () => { if (dialog.returnValue === 'confirm' && validate()) save('CONFIRMED'); });

  newEntry();
})();
