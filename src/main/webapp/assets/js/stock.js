(() => {
  'use strict';
  const screen = document.getElementById('stock-screen');
  if (!screen) return;

  // Dữ liệu minh họa riêng cho giao diện. Backend thay danh sách này bằng số tồn theo SKU và kho.
  const stock = [
    { sku: 'SP-001', name: 'Nước khoáng 500ml', category: 'Đồ uống', warehouse: 'KHO-HN', warehouseName: 'Kho Hà Nội', unit: 'chai', actual: 480, reserved: 96, minimum: 30 },
    { sku: 'SP-001', name: 'Nước khoáng 500ml', category: 'Đồ uống', warehouse: 'KHO-TN', warehouseName: 'Kho Thái Nguyên', unit: 'chai', actual: 72, reserved: 48, minimum: 30 },
    { sku: 'SP-001', name: 'Nước khoáng 500ml', category: 'Đồ uống', warehouse: 'KHO-BN', warehouseName: 'Kho Bắc Ninh', unit: 'chai', actual: 0, reserved: 0, minimum: 30 },
    { sku: 'SP-002', name: 'Bánh quy bơ 200g', category: 'Thực phẩm', warehouse: 'KHO-HN', warehouseName: 'Kho Hà Nội', unit: 'gói', actual: 160, reserved: 20, minimum: 20 },
    { sku: 'SP-002', name: 'Bánh quy bơ 200g', category: 'Thực phẩm', warehouse: 'KHO-TN', warehouseName: 'Kho Thái Nguyên', unit: 'gói', actual: 18, reserved: 10, minimum: 20 },
    { sku: 'SP-003', name: 'Giấy vệ sinh 10 cuộn', category: 'Gia dụng', warehouse: 'KHO-HN', warehouseName: 'Kho Hà Nội', unit: 'cuộn', actual: 300, reserved: 40, minimum: 25 },
    { sku: 'SP-003', name: 'Giấy vệ sinh 10 cuộn', category: 'Gia dụng', warehouse: 'KHO-BN', warehouseName: 'Kho Bắc Ninh', unit: 'cuộn', actual: 80, reserved: 80, minimum: 25 },
    { sku: 'SP-004', name: 'Sữa tươi hộp 180ml', category: 'Đồ uống', warehouse: 'KHO-TN', warehouseName: 'Kho Thái Nguyên', unit: 'hộp', actual: 240, reserved: 60, minimum: 24 },
    { sku: 'SP-005', name: 'Mì ăn liền vị bò', category: 'Thực phẩm', warehouse: 'KHO-BN', warehouseName: 'Kho Bắc Ninh', unit: 'gói', actual: 64, reserved: 48, minimum: 20 }
  ];
  const form = document.getElementById('stock-filters');
  const body = document.getElementById('stock-rows');
  const mobile = document.getElementById('stock-mobile');
  const exportButton = document.getElementById('stock-export');
  const exportFeedback = document.getElementById('stock-export-feedback');
  let filteredStock = [];
  const format = value => new Intl.NumberFormat('vi-VN').format(value);
  const el = (tag, className, content) => {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (content !== undefined) node.textContent = content;
    return node;
  };
  const normalize = text => text.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd').trim();
  const available = item => item.actual - item.reserved;
  const state = item => available(item) === 0 ? 'out' : available(item) <= item.minimum ? 'low' : 'available';
  const labels = { out: 'Hết hàng', low: 'Sắp hết', available: 'Còn hàng' };
  const badges = { out: 'badge--danger', low: 'badge--warning', available: 'badge--success' };

  function statusBadge(item) { return el('span', `badge ${badges[state(item)]}`, labels[state(item)]); }

  function renderRow(item) {
    const row = el('tr');
    const product = el('td');
    product.append(el('div', 'stock-table__name', item.name), el('div', 'stock-table__meta', item.sku));
    row.append(product, el('td', '', item.warehouseName), el('td', '', item.category));
    row.append(el('td', 'stock-number', `${format(item.actual)} ${item.unit}`));
    row.append(el('td', 'stock-number', `${format(item.reserved)} ${item.unit}`));
    row.append(el('td', `stock-number stock-table__available${available(item) === 0 ? ' is-zero' : ''}`, `${format(available(item))} ${item.unit}`));
    const status = el('td');
    status.append(statusBadge(item));
    row.append(status);
    return row;
  }

  function renderCard(item) {
    const card = el('article', 'stock-mobile__card');
    const header = el('div', 'stock-mobile__header');
    const title = el('div');
    title.append(el('strong', '', item.name), el('span', 'stock-table__meta', item.sku));
    header.append(title, statusBadge(item));
    card.append(header, el('span', 'stock-mobile__subtitle', `${item.warehouseName} · ${item.category}`));
    const values = el('div', 'stock-mobile__values');
    [['Thực tế', item.actual], ['Giữ chỗ', item.reserved], ['Khả dụng', available(item)]].forEach(([label, value]) => {
      const box = el('div');
      box.append(el('span', '', label), el('strong', '', `${format(value)} ${item.unit}`));
      values.append(box);
    });
    card.append(values);
    return card;
  }

  function render() {
    const search = normalize(document.getElementById('stock-search').value);
    const warehouse = document.getElementById('stock-warehouse').value;
    const category = document.getElementById('stock-category').value;
    const status = document.getElementById('stock-status').value;
    const matching = stock.filter(item =>
      (!search || normalize(item.sku).includes(search) || normalize(item.name).includes(search)) &&
      (!warehouse || item.warehouse === warehouse) &&
      (!category || item.category === category) &&
      (!status || state(item) === status)
    );
    filteredStock = matching;
    exportButton.disabled = filteredStock.length === 0;
    exportFeedback.hidden = true;
    exportFeedback.textContent = '';
    body.replaceChildren(...matching.map(renderRow));
    mobile.replaceChildren(...matching.map(renderCard));
    document.getElementById('stock-empty').hidden = matching.length !== 0;
    document.getElementById('stock-count').textContent = `${matching.length} kết quả`;
    document.getElementById('stock-footer-count').textContent = `Hiển thị ${matching.length} / ${stock.length} dòng`;
    document.getElementById('stat-lines').textContent = matching.length;
    document.getElementById('stat-low').textContent = matching.filter(item => state(item) === 'low').length;
    document.getElementById('stat-out').textContent = matching.filter(item => state(item) === 'out').length;
  }

  form.addEventListener('input', render);
  form.addEventListener('change', render);
  form.addEventListener('reset', () => requestAnimationFrame(render));
  document.getElementById('stock-empty-reset').addEventListener('click', () => form.reset());
  exportButton.addEventListener('click', () => {
    if (filteredStock.length === 0) return;
    if (!window.StockXlsx || typeof window.StockXlsx.build !== 'function') {
      exportFeedback.hidden = false;
      exportFeedback.textContent = 'Không thể tạo tệp Excel. Hãy tải lại trang và thử lại.';
      return;
    }

    const warehouseSelect = document.getElementById('stock-warehouse');
    const selectedWarehouse = warehouseSelect.value;
    const warehouseCode = selectedWarehouse || 'TAT-CA-KHO';
    const warehouseLabel = warehouseSelect.options[warehouseSelect.selectedIndex].textContent.trim();
    const category = document.getElementById('stock-category').value;
    const status = document.getElementById('stock-status').value;
    const statusLabels = { available: 'Còn hàng', low: 'Sắp hết', out: 'Hết hàng' };
    const search = document.getElementById('stock-search').value.trim();
    const now = new Date();
    const exportDate = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
    const rows = [
      ['BÁO CÁO TỒN KHO'],
      ['Mã kho', warehouseCode],
      ['Kho', warehouseLabel],
      ['Ngày xuất', exportDate],
      ['Tìm sản phẩm', search || 'Tất cả'],
      ['Nhóm hàng', category || 'Tất cả'],
      ['Trạng thái tồn', statusLabels[status] || 'Tất cả trạng thái'],
      [],
      ['Sản phẩm', 'SKU', 'Mã kho', 'Kho', 'Nhóm hàng', 'Tồn thực tế', 'Đang giữ chỗ', 'Khả dụng', 'Đơn vị', 'Trạng thái'],
      ...filteredStock.map(item => [
        item.name, item.sku, item.warehouse, item.warehouseName, item.category,
        item.actual, item.reserved, available(item), item.unit, labels[state(item)]
      ])
    ];
    const bytes = window.StockXlsx.build(rows);
    const blob = new Blob([bytes], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `bao-cao-ton-kho-${warehouseCode}-${exportDate}.xlsx`;
    document.body.append(link);
    link.click();
    link.remove();
    window.setTimeout(() => URL.revokeObjectURL(url), 60000);
    exportFeedback.hidden = false;
    exportFeedback.textContent = `Đã xuất ${filteredStock.length} dòng tồn kho theo bộ lọc hiện tại.`;
  });
  render();
})();
