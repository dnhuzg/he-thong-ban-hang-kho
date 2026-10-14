(() => {
  'use strict';

  const screen = document.getElementById('receipt-screen');
  const source = document.getElementById('receipt-data');
  if (!screen || !source) return;

  const writable = screen.dataset.canWrite === 'true';
  const data = JSON.parse(source.textContent);
  const current = data.current;
  const products = new Map(data.products.map(item => [String(item.id), item]));
  const form = document.getElementById('receipt-form');
  const lines = document.getElementById('receipt-lines');
  const template = document.getElementById('receipt-line-template');
  const error = document.getElementById('receipt-error');
  const dialog = document.getElementById('confirm-dialog');
  const supplier = document.getElementById('supplier');
  const warehouse = document.getElementById('warehouse');
  const field = (row, key) => row.querySelector(`[data-field="${key}"]`);
  const confirmed = Boolean(current && current.status === 'CONFIRMED');
  const statusText = status => (status === 'CONFIRMED' ? 'Đã xác nhận' : 'Bản nháp');
  const format = number => new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 4 }).format(number);
  let busy = false;

  const today = () => {
    const date = new Date();
    return [date.getFullYear(), String(date.getMonth() + 1).padStart(2, '0'), String(date.getDate()).padStart(2, '0')].join('-');
  };

  // Phiếu cũ có thể tham chiếu nhà cung cấp / sản phẩm đã ngừng: vẫn hiện để xem lại phiếu
  if (current) {
    if (!data.suppliers.some(item => item.id === current.supplierId)) {
      data.suppliers.push({ id: current.supplierId, label: current.supplierName });
    }
    if (!data.warehouses.some(item => item.id === current.warehouseId)) {
      data.warehouses.push({ id: current.warehouseId, label: current.warehouseName });
    }
    current.lines.forEach(line => {
      if (!products.has(String(line.productId))) {
        products.set(String(line.productId), {
          id: line.productId,
          label: `Sản phẩm #${line.productId} (ngừng kinh doanh)`,
          baseUnit: '',
          units: [{ id: line.unitId, name: `đơn vị #${line.unitId}`, factor: line.factor }]
        });
      }
    });
  }

  data.suppliers.forEach(item => supplier.add(new Option(item.label, item.id)));
  data.warehouses.forEach(item => warehouse.add(new Option(item.label, item.id)));
  const productSelect = template.content.querySelector('[data-field="product"]');
  products.forEach(item => productSelect.add(new Option(item.label, item.id)));

  function updateUnits(row, selectedUnit = '') {
    const product = products.get(field(row, 'product').value);
    const unit = field(row, 'unit');
    unit.replaceChildren(new Option(product ? '— Chọn đơn vị —' : 'Chọn sản phẩm trước', ''));
    if (product) {
      product.units.forEach(item => unit.add(new Option(
        item.factor === 1 ? `${item.name} (đơn vị cơ sở)` : `${item.name} (1 ${item.name} = ${format(item.factor)} ${product.baseUnit})`,
        item.id)));
    }
    unit.value = String(selectedUnit);
    updateConverted(row);
  }

  function selectedUnit(row) {
    const product = products.get(field(row, 'product').value);
    return product?.units.find(item => String(item.id) === field(row, 'unit').value);
  }

  function updateConverted(row) {
    const product = products.get(field(row, 'product').value);
    const unit = selectedUnit(row);
    const quantity = Number(field(row, 'quantity').value);
    row.querySelector('[data-converted]').textContent = unit && quantity > 0 && Number.isFinite(quantity)
      ? `Quy đổi: ${format(quantity * unit.factor)} ${product.baseUnit} (${format(quantity)} × ${format(unit.factor)})`
      : 'Quy đổi: —';
    updateSummary();
  }

  function addLine(item = {}) {
    const row = template.content.firstElementChild.cloneNode(true);
    lines.append(row);
    field(row, 'product').value = item.productId ? String(item.productId) : '';
    updateUnits(row, item.unitId || '');
    field(row, 'quantity').value = item.quantity ?? '';
    field(row, 'lot').value = item.lotNo || '';
    field(row, 'expiry').value = item.expiryDate || '';
    updateConverted(row);
    renumber();
  }

  function renumber() {
    [...lines.children].forEach((row, index) => {
      row.querySelector('[data-line-number]').textContent = index + 1;
      row.querySelector('[data-remove]').disabled = !writable || confirmed || lines.children.length === 1;
    });
    updateSummary();
  }

  function updateSummary() {
    document.getElementById('summary-supplier').textContent = supplier.value ? supplier.selectedOptions[0].textContent : 'Chưa chọn';
    document.getElementById('summary-warehouse').textContent = warehouse.value ? warehouse.selectedOptions[0].textContent : 'Chưa chọn';
    let count = 0;
    let converted = 0;
    for (const row of lines.children) {
      const quantity = Number(field(row, 'quantity').value);
      if (field(row, 'product').value) count++;
      if (selectedUnit(row) && Number.isFinite(quantity) && quantity > 0) converted++;
    }
    document.getElementById('summary-items').textContent = count;
    document.getElementById('summary-units').textContent = converted;
    document.getElementById('summary-status').textContent = current ? statusText(current.status) : 'Đang lập';
  }

  function showError(message) {
    error.textContent = message;
    error.hidden = false;
    error.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  }

  function validate() {
    error.hidden = true;
    if (!form.reportValidity()) return false;
    const date = document.getElementById('received-date').value;
    const rows = [...lines.children];
    if (!rows.length || rows.some(row => {
      const quantity = Number(field(row, 'quantity').value);
      return !selectedUnit(row) || !Number.isFinite(quantity) || quantity <= 0;
    })) {
      showError('Chọn sản phẩm, đơn vị và số lượng hợp lệ cho từng dòng.');
      return false;
    }
    if (rows.some(row => field(row, 'expiry').value && field(row, 'expiry').value < date)) {
      showError('Hạn dùng không được trước ngày nhập.');
      return false;
    }
    return true;
  }

  async function save(confirm) {
    if (!writable || confirmed || busy) return;
    const body = new URLSearchParams();
    body.set('_csrf', screen.dataset.csrf || '');
    if (current) body.set('id', current.id);
    body.set('supplierId', supplier.value);
    body.set('documentNo', document.getElementById('document').value.trim());
    body.set('receivedDate', document.getElementById('received-date').value);
    body.set('warehouseId', warehouse.value);
    body.set('note', document.getElementById('note').value.trim());
    body.set('confirm', String(confirm));
    for (const row of lines.children) {
      body.append('productId', field(row, 'product').value);
      body.append('unitId', field(row, 'unit').value);
      body.append('quantity', field(row, 'quantity').value);
      body.append('lotNo', field(row, 'lot').value.trim());
      body.append('expiryDate', field(row, 'expiry').value);
    }
    busy = true;
    error.hidden = true;
    try {
      const response = await fetch(screen.dataset.saveUrl, { method: 'POST', body, credentials: 'same-origin' });
      const result = await response.json().catch(() => ({}));
      if (response.ok) {
        window.location.href = `${screen.dataset.pageUrl}?id=${encodeURIComponent(result.id)}`;
        return;
      }
      showError(result.error || (response.status === 401
        ? 'Phiên đăng nhập đã hết hạn. Đăng nhập lại ở tab khác rồi lưu lại.'
        : 'Không thể lưu phiếu nhập. Vui lòng thử lại.'));
    } catch (_) {
      showError('Không kết nối được máy chủ. Kiểm tra mạng rồi thử lại.');
    } finally {
      busy = false;
    }
  }

  function renderHistory() {
    const container = document.getElementById('receipt-history');
    container.replaceChildren();
    if (!data.recent.length) {
      const empty = document.createElement('p');
      empty.className = 'text-muted';
      empty.textContent = 'Chưa có phiếu nào.';
      container.append(empty);
      return;
    }
    data.recent.forEach(entry => {
      const link = document.createElement('a');
      link.href = `${screen.dataset.pageUrl}?id=${encodeURIComponent(entry.id)}`;
      link.className = `receipt-history__item${current && entry.id === current.id ? ' is-selected' : ''}`;
      const title = document.createElement('strong');
      title.textContent = `${entry.code} · ${entry.documentNo}`;
      const meta = document.createElement('span');
      meta.textContent = `${entry.receivedDate} · ${entry.warehouseName} · ${statusText(entry.status)}`;
      link.append(title, meta);
      container.append(link);
    });
  }

  function lockForm() {
    const locked = !writable || confirmed;
    form.querySelectorAll('input, select, textarea').forEach(input => { input.disabled = locked; });
    document.getElementById('add-line').hidden = locked;
    document.getElementById('save-draft').hidden = locked;
    form.querySelector('[type="submit"]').hidden = locked;
    document.getElementById('receipt-status').textContent = current ? `${current.code} · ${statusText(current.status)}` : 'Phiếu mới';
    const reset = document.getElementById('reset-form');
    reset.textContent = current ? 'Lập phiếu mới' : 'Làm mới';
    reset.hidden = !writable;
    renumber();
  }

  function fill() {
    supplier.value = current ? String(current.supplierId) : '';
    document.getElementById('document').value = current ? current.documentNo : '';
    document.getElementById('received-date').value = current ? current.receivedDate : today();
    warehouse.value = current ? String(current.warehouseId) : (data.warehouses.length === 1 ? String(data.warehouses[0].id) : '');
    document.getElementById('note').value = current?.note || '';
    lines.replaceChildren();
    (current && current.lines.length ? current.lines : [{}]).forEach(addLine);
    error.hidden = true;
    lockForm();
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
    if (!event.target.matches('[data-remove]') || !writable || confirmed || lines.children.length === 1) return;
    event.target.closest('.receipt-line').remove();
    renumber();
  });
  form.addEventListener('change', updateSummary);
  document.getElementById('add-line').addEventListener('click', () => addLine());
  document.getElementById('reset-form').addEventListener('click', () => {
    if (current) window.location.href = screen.dataset.pageUrl;
    else fill();
  });
  document.getElementById('save-draft').addEventListener('click', () => {
    error.hidden = true;
    if (form.reportValidity()) save(false);
  });
  form.addEventListener('submit', event => {
    event.preventDefault();
    if (writable && !confirmed && validate()) dialog.showModal();
  });
  dialog.addEventListener('close', () => { if (dialog.returnValue === 'confirm' && validate()) save(true); });

  renderHistory();
  fill();
  if (writable && !data.warehouses.length) {
    showError('Tài khoản chưa được gắn kho nào nên chưa lập được phiếu nhập. Nhờ Admin gắn kho cho tài khoản.');
  }
})();
