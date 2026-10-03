(() => {
  'use strict';

  const screen = document.getElementById('transfer-screen');
  if (!screen) return;

  const writable = screen.dataset.canWrite === 'true';
  const storageKey = `oms-ghtk55-demo-${screen.dataset.userId || 'guest'}`;
  const products = {
    'SP-001': { name: 'Nước khoáng 500ml', unit: 'chai' },
    'SP-002': { name: 'Bánh quy bơ 200g', unit: 'gói' },
    'SP-003': { name: 'Giấy vệ sinh 10 cuộn', unit: 'cuộn' }
  };
  const labels = { DRAFT: 'Bản nháp', IN_TRANSIT: 'Đang chuyển', RECEIVED: 'Đã nhận đủ', DISCREPANCY: 'Có chênh lệch' };
  const form = document.getElementById('transfer-form');
  const lines = document.getElementById('transfer-lines');
  const receiving = document.getElementById('transfer-receiving');
  const receivedLines = document.getElementById('transfer-received-lines');
  const error = document.getElementById('transfer-error');
  const dialog = document.getElementById('transfer-dialog');
  const toast = document.getElementById('transfer-toast');
  const field = (row, key) => row.querySelector(`[data-field="${key}"]`);
  const format = value => new Intl.NumberFormat('vi-VN').format(value);
  const today = () => {
    const date = new Date();
    return [date.getFullYear(), String(date.getMonth() + 1).padStart(2, '0'), String(date.getDate()).padStart(2, '0')].join('-');
  };
  let saved = [];
  let currentId = null;
  let pendingAction = null;
  let toastTimer;

  try {
    const parsed = JSON.parse(localStorage.getItem(storageKey) || '[]');
    if (Array.isArray(parsed)) {
      saved = parsed.filter(entry => entry && typeof entry.id === 'string' && labels[entry.status] && Array.isArray(entry.items));
    }
  } catch (_) { /* Trình duyệt có thể chặn lưu trữ cục bộ. */ }

  function notice(message) {
    toast.textContent = message;
    toast.hidden = false;
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => { toast.hidden = true; }, 4000);
  }

  function persist(entry) {
    const next = [...saved.filter(item => item.id !== entry.id), entry].slice(-30);
    try {
      localStorage.setItem(storageKey, JSON.stringify(next));
      saved = next;
      currentId = entry.id;
      return true;
    } catch (_) {
      notice('Trình duyệt không lưu được phiếu mẫu.');
      return false;
    }
  }

  function current() { return saved.find(entry => entry.id === currentId); }

  function fail(message) {
    error.textContent = message;
    error.hidden = false;
    error.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    return false;
  }

  function addLine(item = {}) {
    const row = document.getElementById('transfer-line-template').content.firstElementChild.cloneNode(true);
    field(row, 'product').value = products[item.product] ? item.product : '';
    field(row, 'quantity').value = item.quantity ?? '';
    lines.append(row);
    renumber();
  }

  function renumber() {
    [...lines.children].forEach((row, index) => {
      row.querySelector('[data-line-number]').textContent = index + 1;
      row.querySelector('[data-remove]').disabled = !writable || Boolean(current() && current().status !== 'DRAFT') || lines.children.length === 1;
    });
    updateSummary();
  }

  function readDraft() {
    const id = currentId || `${Date.now()}-${Math.random().toString(36).slice(2, 7)}`;
    return {
      id,
      code: current()?.code || `CK-${today().replaceAll('-', '')}-${id.slice(-5).toUpperCase()}`,
      status: 'DRAFT',
      date: document.getElementById('transfer-date').value,
      source: document.getElementById('source-warehouse').value,
      destination: document.getElementById('destination-warehouse').value,
      note: document.getElementById('transfer-note').value.trim(),
      items: [...lines.children].map(row => ({ product: field(row, 'product').value, quantity: field(row, 'quantity').value }))
    };
  }

  function validateTransfer() {
    error.hidden = true;
    if (!form.reportValidity()) return false;
    const entry = readDraft();
    if (entry.source === entry.destination) return fail('Kho đi và kho đến phải khác nhau.');
    if (!entry.items.length || entry.items.some(item => !products[item.product] || !Number.isInteger(Number(item.quantity)) || Number(item.quantity) < 1 || Number(item.quantity) > 1000000)) {
      return fail('Chọn mặt hàng và số lượng nguyên từ 1 đến 1.000.000 cho từng dòng.');
    }
    if (new Set(entry.items.map(item => item.product)).size !== entry.items.length) return fail('Mỗi SKU chỉ được nhập một dòng trong phiếu.');
    return true;
  }

  function renderReceiving(entry) {
    receivedLines.replaceChildren();
    for (const item of entry.items) {
      const row = document.getElementById('transfer-received-template').content.firstElementChild.cloneNode(true);
      const product = products[item.product];
      row.querySelector('[data-product]').textContent = `${item.product} · ${product?.name || 'Sản phẩm'}`;
      row.querySelector('[data-sent]').textContent = `Đã chuyển: ${format(Number(item.quantity))} ${product?.unit || ''}`;
      row.querySelector('[data-received]').value = item.received ?? item.quantity;
      row.dataset.sent = item.quantity;
      row.dataset.unit = product?.unit || '';
      receivedLines.append(row);
    }
    document.getElementById('transfer-reason').value = entry.reason || '';
    updateDifference();
  }

  function readReceipt() {
    return [...receivedLines.children].map(row => Number(row.querySelector('[data-received]').value));
  }

  function updateDifference() {
    let count = 0;
    for (const row of receivedLines.children) {
      const input = row.querySelector('[data-received]');
      const received = Number(input.value);
      const sent = Number(row.dataset.sent);
      const diff = row.querySelector('[data-difference]');
      if (input.value === '' || !Number.isInteger(received) || received < 0) {
        diff.textContent = 'Chưa nhập số thực nhận';
      } else if (received === sent) {
        diff.textContent = 'Nhận đủ';
      } else {
        diff.textContent = `${received < sent ? 'Thiếu' : 'Thừa'} ${format(Math.abs(received - sent))} ${row.dataset.unit}`;
        count++;
      }
    }
    document.getElementById('transfer-reason-required').hidden = count === 0;
    document.getElementById('transfer-reason').required = count > 0 && current()?.status === 'IN_TRANSIT';
    document.getElementById('transfer-summary-difference').textContent = receivedLines.children.length ? `${count} mặt hàng` : '—';
    return count;
  }

  function validateReceipt() {
    error.hidden = true;
    const values = readReceipt();
    if (!values.length || [...receivedLines.children].some((row, index) => {
      const input = row.querySelector('[data-received]');
      return input.value === '' || !Number.isInteger(values[index]) || values[index] < 0 || values[index] > 1000000;
    })) return fail('Nhập số thực nhận nguyên từ 0 đến 1.000.000 cho từng mặt hàng.');
    if (updateDifference() > 0 && !document.getElementById('transfer-reason').value.trim()) return fail('Nhập lý do khi số thực nhận khác số đã chuyển.');
    return true;
  }

  function updateSummary() {
    const entry = current();
    const source = document.getElementById('source-warehouse');
    const destination = document.getElementById('destination-warehouse');
    document.getElementById('transfer-summary-status').textContent = entry ? labels[entry.status] : 'Đang lập';
    document.getElementById('transfer-summary-source').textContent = source.value ? source.selectedOptions[0].textContent : 'Chưa chọn';
    document.getElementById('transfer-summary-destination').textContent = destination.value ? destination.selectedOptions[0].textContent : 'Chưa chọn';
    document.getElementById('transfer-summary-items').textContent = [...lines.children].filter(row => products[field(row, 'product').value]).length;
    if (!entry || entry.status === 'DRAFT') document.getElementById('transfer-summary-difference').textContent = '—';
  }

  function renderHistory() {
    const container = document.getElementById('transfer-history');
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
      button.className = `transfer-history__item${entry.id === currentId ? ' is-selected' : ''}`;
      const title = document.createElement('strong');
      title.textContent = entry.code || 'Phiếu chuyển kho';
      const meta = document.createElement('span');
      meta.textContent = `${entry.source || '—'} → ${entry.destination || '—'} · ${labels[entry.status]}`;
      button.append(title, meta);
      button.addEventListener('click', () => loadEntry(entry));
      container.append(button);
    });
  }

  function renderState() {
    const entry = current();
    const status = entry?.status;
    const editable = writable && (!entry || status === 'DRAFT');
    form.querySelectorAll('#transfer-date, #source-warehouse, #destination-warehouse, #transfer-note, .transfer-line select, .transfer-line input').forEach(input => { input.disabled = !editable; });
    document.getElementById('transfer-add-line').hidden = !editable;
    document.getElementById('transfer-save').hidden = !editable;
    document.getElementById('transfer-send').hidden = !editable;
    document.getElementById('transfer-new').hidden = !writable;
    document.getElementById('transfer-receive').hidden = !writable || status !== 'IN_TRANSIT';
    receiving.hidden = !entry || status === 'DRAFT';
    receiving.querySelectorAll('input, textarea').forEach(input => { input.disabled = !writable || status !== 'IN_TRANSIT'; });
    document.getElementById('transfer-status').textContent = status ? labels[status] : 'Phiếu mới';
    const stage = status === 'IN_TRANSIT' ? 1 : status === 'RECEIVED' || status === 'DISCREPANCY' ? 2 : 0;
    [...document.querySelectorAll('#transfer-progress li')].forEach((item, index) => {
      item.classList.toggle('is-active', index === stage);
      item.classList.toggle('is-complete', index < stage);
    });
    renumber();
    if (entry && status !== 'DRAFT') updateDifference();
    updateSummary();
  }

  function loadEntry(entry) {
    currentId = entry.id;
    document.getElementById('transfer-code').value = entry.code || 'Phiếu chuyển kho';
    document.getElementById('transfer-date').value = entry.date || today();
    document.getElementById('source-warehouse').value = entry.source || '';
    document.getElementById('destination-warehouse').value = entry.destination || '';
    document.getElementById('transfer-note').value = entry.note || '';
    lines.replaceChildren();
    (entry.items.length ? entry.items : [{}]).forEach(addLine);
    receivedLines.replaceChildren();
    if (entry.status !== 'DRAFT') renderReceiving(entry);
    error.hidden = true;
    renderState();
    renderHistory();
  }

  function newEntry() {
    currentId = null;
    form.reset();
    document.getElementById('transfer-code').value = 'Tạo khi lưu';
    document.getElementById('transfer-date').value = today();
    lines.replaceChildren();
    receivedLines.replaceChildren();
    addLine();
    error.hidden = true;
    renderState();
    renderHistory();
  }

  function saveDraft() {
    if (!writable || current()?.status !== undefined && current().status !== 'DRAFT') return;
    const entry = readDraft();
    if (!persist(entry)) return;
    loadEntry(entry);
    notice('Đã lưu bản nháp trên trình duyệt này.');
  }

  function startTransfer() {
    if (!writable || !validateTransfer()) return;
    pendingAction = 'send';
    document.getElementById('transfer-dialog-title').textContent = 'Bắt đầu chuyển kho?';
    document.getElementById('transfer-dialog-message').textContent = 'Phiếu sẽ chuyển sang trạng thái Đang chuyển và khóa thông tin kho, hàng hóa trong bản FE. Tồn kho thật chưa thay đổi.';
    dialog.showModal();
  }

  function finishTransfer() {
    if (!writable || current()?.status !== 'IN_TRANSIT' || !validateReceipt()) return;
    pendingAction = 'receive';
    const hasDifference = updateDifference() > 0;
    document.getElementById('transfer-dialog-title').textContent = hasDifference ? 'Ghi nhận chênh lệch?' : 'Xác nhận nhận đủ hàng?';
    document.getElementById('transfer-dialog-message').textContent = hasDifference
      ? 'Phiếu mẫu sẽ có trạng thái Có chênh lệch và lưu lý do để xử lý sau. Chưa cộng tồn kho đến.'
      : 'Phiếu mẫu sẽ có trạng thái Đã nhận đủ. Tồn kho đến thực tế chưa được cộng vì chưa nối backend.';
    dialog.showModal();
  }

  dialog.addEventListener('close', () => {
    if (dialog.returnValue !== 'confirm') { pendingAction = null; return; }
    if (pendingAction === 'send' && validateTransfer()) {
      const entry = { ...readDraft(), status: 'IN_TRANSIT' };
      if (persist(entry)) { loadEntry(entry); notice('Đã ghi nhận phiếu mẫu đang chuyển.'); }
    } else if (pendingAction === 'receive' && current()?.status === 'IN_TRANSIT' && validateReceipt()) {
      const values = readReceipt();
      const entry = {
        ...current(),
        status: updateDifference() ? 'DISCREPANCY' : 'RECEIVED',
        reason: document.getElementById('transfer-reason').value.trim(),
        items: current().items.map((item, index) => ({ ...item, received: values[index] }))
      };
      if (persist(entry)) { loadEntry(entry); notice(entry.status === 'RECEIVED' ? 'Đã xác nhận nhận đủ trên phiếu mẫu.' : 'Đã lưu chênh lệch và lý do trên phiếu mẫu.'); }
    }
    pendingAction = null;
  });

  lines.addEventListener('click', event => {
    if (!event.target.matches('[data-remove]') || !writable || current()?.status && current().status !== 'DRAFT' || lines.children.length === 1) return;
    event.target.closest('.transfer-line').remove();
    renumber();
  });
  lines.addEventListener('change', updateSummary);
  receivedLines.addEventListener('input', updateDifference);
  form.addEventListener('change', updateSummary);
  form.addEventListener('submit', event => event.preventDefault());
  document.getElementById('transfer-add-line').addEventListener('click', () => addLine());
  document.getElementById('transfer-new').addEventListener('click', newEntry);
  document.getElementById('transfer-save').addEventListener('click', saveDraft);
  document.getElementById('transfer-send').addEventListener('click', startTransfer);
  document.getElementById('transfer-receive').addEventListener('click', finishTransfer);

  newEntry();
})();
