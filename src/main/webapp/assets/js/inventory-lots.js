(() => {
  'use strict';
  const screen = document.getElementById('lots-screen');
  if (!screen) return;
  const demo = window.InventoryTraceDemo;
  const { node, format, formatDate } = demo;
  const field = id => document.getElementById(id);
  const canWrite = screen.dataset.canWrite === 'true';
  const canOverride = canWrite && screen.dataset.canOverride === 'true';
  const storageKey = `oms.ghtk58.ui.v1:${document.body.dataset.ctx || ''}:${screen.dataset.userId}`;
  const settings = Object.fromEntries(demo.products.map(item => [item.sku, { enabled: item.trackLots, days: item.warningDays }]));
  let storageAvailable = true;
  let selectedLot = null;
  try {
    const saved = JSON.parse(localStorage.getItem(storageKey) || '{}');
    demo.products.forEach(item => {
      const value = saved?.[item.sku];
      if (value && typeof value.enabled === 'boolean' && Number.isInteger(value.days) && value.days >= 0 && value.days <= 365) settings[item.sku] = value;
    });
  } catch (_) { storageAvailable = false; }
  const labels = { valid: 'Còn hạn', soon: 'Sắp hết hạn', expired: 'Đã quá hạn', empty: 'Hết hàng' };
  const badges = { valid: 'badge--success', soon: 'badge--warning', expired: 'badge--danger', empty: '' };
  demo.fillProducts(field('settings-sku'));
  demo.fillWarehouses(field('lots-warehouse'));
  field('lots-mode').textContent = canWrite ? 'Có quyền thao tác mẫu' : 'Chỉ xem';
  field('settings-save').hidden = !canWrite;
  field('settings-days').disabled = !canWrite;
  field('settings-enabled').disabled = !canWrite;

  function notice(message) { field('lots-notice').textContent = message; field('lots-notice').hidden = false; }
  function showSettings() {
    const config = settings[field('settings-sku').value];
    field('settings-enabled').checked = config.enabled;
    field('settings-days').value = config.days;
    field('settings-context').textContent = config.enabled ? `SKU đang quản lý lô. Cảnh báo khi còn từ 0 đến ${config.days} ngày.` : 'SKU không quản lý lô. Lô mẫu được giữ lại và ẩn khỏi danh sách cho đến khi bật lại.';
  }
  function openExport(item) {
    selectedLot = item;
    field('lot-export-form').reset();
    field('lot-export-error').hidden = true;
    const expired = demo.daysRemaining(item.expiry) < 0;
    field('lot-export-context').textContent = `${demo.product(item.sku).name} · ${item.code} · ${demo.warehouseName(item.warehouse)} · còn ${format(item.remaining)} ${demo.product(item.sku).unit} · hạn ${formatDate(item.expiry)}`;
    field('lot-export-quantity').max = item.remaining;
    field('lot-export-quantity').value = 1;
    field('lot-override-fields').hidden = !expired || !canOverride;
    field('lot-override-reason').required = expired && canOverride;
    field('lot-override-confirm').required = expired && canOverride;
    field('lot-export-submit').disabled = !canWrite || (expired && !canOverride) || item.remaining === 0;
    if (expired && !canOverride) {
      field('lot-export-error').textContent = 'Chặn xuất lô quá hạn. Cần Quản lý kho xác nhận ghi đè kèm lý do.';
      field('lot-export-error').hidden = false;
    }
    field('lot-export-dialog').showModal();
  }
  function render() {
    const search = demo.normalize(field('lots-search').value);
    const matching = demo.lots.filter(item => settings[item.sku].enabled &&
      (!search || demo.normalize(`${item.sku} ${demo.product(item.sku).name} ${item.code}`).includes(search)) &&
      (!field('lots-warehouse').value || item.warehouse === field('lots-warehouse').value) &&
      (!field('lots-status').value || demo.lotStatus(item, settings[item.sku].days) === field('lots-status').value));
    const rows = matching.map(item => {
      const product = demo.product(item.sku);
      const status = demo.lotStatus(item, settings[item.sku].days);
      const row = node('tr'), name = node('td'), location = node('td'), state = node('td'), actions = node('td');
      name.append(node('strong', 'trace-table__name', product.name), node('span', 'trace-table__meta', item.sku));
      location.append(node('span', '', demo.warehouseName(item.warehouse)), node('span', 'trace-table__meta', item.code));
      state.append(node('span', `badge ${badges[status]}`, labels[status]));
      const days = demo.daysRemaining(item.expiry);
      state.append(node('small', 'trace-table__meta', days < 0 ? `Quá hạn ${-days} ngày` : days === 0 ? 'Hết hạn hôm nay' : `Còn ${days} ngày`));
      if (canWrite && item.remaining > 0) {
        const button = node('button', 'btn btn--secondary btn--sm', 'Kiểm tra xuất');
        button.type = 'button'; button.setAttribute('aria-label', `Kiểm tra xuất lô ${item.code}`);
        button.addEventListener('click', () => openExport(item)); actions.append(button);
      } else actions.append(node('span', 'text-muted', item.remaining === 0 ? 'Không còn hàng' : 'Chỉ xem'));
      row.append(name, location, node('td', '', formatDate(item.manufactured)), node('td', '', formatDate(item.expiry)), node('td', 'trace-number', `${format(item.remaining)} ${product.unit}`), state, actions);
      return row;
    });
    field('lots-rows').replaceChildren(...rows);
    field('lots-count').textContent = `${matching.length} lô`;
    field('lots-empty').hidden = matching.length > 0;
    field('lots-total').textContent = matching.filter(item => item.remaining > 0).length;
    field('lots-soon').textContent = matching.filter(item => demo.lotStatus(item, settings[item.sku].days) === 'soon').length;
    field('lots-expired').textContent = matching.filter(item => demo.lotStatus(item, settings[item.sku].days) === 'expired').length;
  }
  field('settings-sku').addEventListener('change', showSettings);
  field('lot-settings').addEventListener('submit', event => {
    event.preventDefault();
    if (!canWrite) return;
    const days = Number(field('settings-days').value);
    if (!Number.isInteger(days) || days < 0 || days > 365 || !field('settings-days').value.trim()) { notice('Ngưỡng cảnh báo phải là số nguyên từ 0 đến 365 ngày.'); return; }
    settings[field('settings-sku').value] = { enabled: field('settings-enabled').checked, days };
    try { localStorage.setItem(storageKey, JSON.stringify(settings)); storageAvailable = true; } catch (_) { storageAvailable = false; }
    showSettings(); render();
    notice(storageAvailable ? 'Đã lưu cấu hình mẫu trên trình duyệt này.' : 'Cấu hình mẫu chỉ giữ trong phiên hiện tại vì trình duyệt không cho lưu.');
  });
  field('lots-filters').addEventListener('submit', event => event.preventDefault());
  field('lots-filters').addEventListener('input', render);
  field('lots-filters').addEventListener('change', render);
  field('lots-filters').addEventListener('reset', () => requestAnimationFrame(render));
  field('lot-export-close').addEventListener('click', () => field('lot-export-dialog').close());
  field('lot-export-form').addEventListener('submit', event => {
    event.preventDefault();
    if (!selectedLot) return;
    try {
      const result = demo.checkExport(selectedLot, Number(field('lot-export-quantity').value), canWrite, canOverride, field('lot-override-reason').value, field('lot-override-confirm').checked);
      field('lot-export-dialog').close();
      notice(result.overridden ? `Đã xác nhận ghi đè mẫu lô ${selectedLot.code}. Lý do: ${result.reason}. Chưa xuất hàng hoặc thay đổi tồn kho thật.` : `Lô ${selectedLot.code} đáp ứng kiểm tra hạn dùng và số lượng trong bản mẫu. Chưa xuất hàng hoặc thay đổi tồn kho thật.`);
    } catch (error) { field('lot-export-error').textContent = error.message; field('lot-export-error').hidden = false; }
  });
  showSettings(); render();
})();
