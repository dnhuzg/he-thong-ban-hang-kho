/* Dữ liệu mẫu chung cho GHTK-58/GHTK-59. Không đọc hoặc ghi tồn kho thật. */
(() => {
  'use strict';
  const now = new Date();
  const today = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
  // Dùng ngày lịch UTC để cộng ngày không bị lệch do giờ mùa hè hoặc chuyển múi giờ.
  const offsetDate = days => {
    const date = new Date(`${today}T00:00:00Z`);
    date.setUTCDate(date.getUTCDate() + days);
    return date.toISOString().slice(0, 10);
  };
  const warehouses = [
    { code: 'KHO-HN', name: 'Kho Hà Nội' },
    { code: 'KHO-TN', name: 'Kho Thái Nguyên' },
    { code: 'KHO-BN', name: 'Kho Bắc Ninh' }
  ];
  const products = [
    { sku: 'SP-001', name: 'Nước khoáng 500ml', unit: 'chai', trackLots: true, warningDays: 30 },
    { sku: 'SP-002', name: 'Bánh quy bơ 200g', unit: 'gói', trackLots: true, warningDays: 15 },
    { sku: 'SP-003', name: 'Giấy vệ sinh 10 cuộn', unit: 'cuộn', trackLots: false, warningDays: 30 }
  ];
  const lot = (id, sku, warehouse, code, expiryDays, remaining) => ({
    id, sku, warehouse, code, manufactured: offsetDate(-180), expiry: offsetDate(expiryDays), remaining
  });
  const lots = [
    lot('L1', 'SP-001', 'KHO-HN', 'NK-01', 90, 360),
    lot('L2', 'SP-001', 'KHO-HN', 'NK-02', 15, 72),
    lot('L3', 'SP-001', 'KHO-HN', 'NK-03', -5, 48),
    lot('L4', 'SP-001', 'KHO-TN', 'NK-04', 0, 72),
    lot('L5', 'SP-001', 'KHO-BN', 'NK-05', -10, 0),
    lot('L6', 'SP-002', 'KHO-HN', 'BQ-01', 60, 140),
    lot('L7', 'SP-002', 'KHO-HN', 'BQ-02', 7, 20),
    lot('L8', 'SP-002', 'KHO-TN', 'BQ-03', -1, 18)
  ];
  const movement = (id, sku, warehouse, days, time, type, delta, document, note, peerWarehouse = '') => ({
    id, sku, warehouse, occurredAt: `${offsetDate(days)}T${time}`, type, delta, document, note, peerWarehouse
  });
  const movements = [
    movement(1, 'SP-001', 'KHO-HN', -40, '08:00', 'RECEIPT', 600, 'PN-001', 'Nhập từ nhà cung cấp Minh Phát'),
    movement(2, 'SP-001', 'KHO-TN', -38, '08:30', 'RECEIPT', 120, 'PN-002', 'Nhập từ nhà cung cấp An Bình'),
    movement(3, 'SP-001', 'KHO-BN', -35, '09:00', 'RECEIPT', 80, 'PN-003', 'Nhập tại kho Bắc Ninh'),
    movement(4, 'SP-001', 'KHO-HN', -25, '10:00', 'ISSUE', -144, 'PX-001', 'Xuất cho đại lý Minh Anh'),
    movement(5, 'SP-001', 'KHO-HN', -20, '09:00', 'TRANSFER_OUT', -72, 'CK-001', 'Chuyển đi kho Thái Nguyên', 'KHO-TN'),
    movement(6, 'SP-001', 'KHO-TN', -20, '09:00', 'TRANSFER_IN', 72, 'CK-001', 'Nhận từ kho Hà Nội', 'KHO-HN'),
    movement(7, 'SP-001', 'KHO-TN', -15, '14:00', 'ISSUE', -120, 'PX-002', 'Xuất cho đại lý Hoàng Gia'),
    movement(8, 'SP-001', 'KHO-BN', -10, '15:00', 'ISSUE', -80, 'PX-003', 'Xuất hết lô NK-05'),
    movement(9, 'SP-001', 'KHO-HN', -5, '16:00', 'ADJUSTMENT', 96, 'DC-001', 'Điều chỉnh tăng theo biên bản kiểm kê'),
    movement(10, 'SP-002', 'KHO-HN', -35, '08:00', 'RECEIPT', 200, 'PN-004', 'Nhập bánh quy bơ'),
    movement(11, 'SP-002', 'KHO-TN', -34, '08:00', 'RECEIPT', 18, 'PN-005', 'Nhập bánh quy bơ'),
    movement(12, 'SP-002', 'KHO-HN', -22, '11:00', 'ISSUE', -60, 'PX-004', 'Xuất cho đại lý An Phát'),
    movement(13, 'SP-002', 'KHO-TN', -18, '13:00', 'TRANSFER_OUT', -20, 'CK-002', 'Chuyển đi kho Hà Nội', 'KHO-HN'),
    movement(14, 'SP-002', 'KHO-HN', -18, '13:00', 'TRANSFER_IN', 20, 'CK-002', 'Nhận từ kho Thái Nguyên', 'KHO-TN'),
    movement(15, 'SP-002', 'KHO-TN', -18, '09:00', 'RECEIPT', 20, 'PN-006', 'Bổ sung hàng trước khi chuyển kho'),
    movement(16, 'SP-003', 'KHO-HN', -40, '08:00', 'RECEIPT', 340, 'PN-007', 'Nhập giấy vệ sinh'),
    movement(17, 'SP-003', 'KHO-BN', -30, '09:00', 'RECEIPT', 80, 'PN-008', 'Nhập giấy vệ sinh'),
    movement(18, 'SP-003', 'KHO-HN', -3, '16:00', 'ADJUSTMENT', -40, 'DC-002', 'Điều chỉnh giảm hàng hỏng khi kiểm kê')
  ];
  const labels = { RECEIPT: 'Nhập kho', ISSUE: 'Xuất kho', TRANSFER_OUT: 'Chuyển đi', TRANSFER_IN: 'Chuyển đến', ADJUSTMENT: 'Điều chỉnh' };
  const product = sku => products.find(item => item.sku === sku);
  const warehouseName = code => warehouses.find(item => item.code === code)?.name || code;
  const dateValid = value => /^\d{4}-\d{2}-\d{2}$/.test(value) && Number.isFinite(Date.parse(`${value}T00:00:00Z`)) && new Date(`${value}T00:00:00Z`).toISOString().slice(0, 10) === value;
  const daysRemaining = expiry => Math.round((Date.parse(`${expiry}T00:00:00Z`) - Date.parse(`${today}T00:00:00Z`)) / 86400000);
  const lotStatus = (item, warningDays) => item.remaining === 0 ? 'empty' : daysRemaining(item.expiry) < 0 ? 'expired' : daysRemaining(item.expiry) <= warningDays ? 'soon' : 'valid';

  function stockCard(sku, warehouse = '', from = '', to = '') {
    if (!product(sku) || (warehouse && !warehouses.some(item => item.code === warehouse))) throw new Error('Chọn sản phẩm và kho hợp lệ.');
    if ((from && !dateValid(from)) || (to && !dateValid(to))) throw new Error('Ngày lọc không hợp lệ.');
    if (from && to && from > to) throw new Error('Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.');
    const scope = movements.filter(item => item.sku === sku && (!warehouse || item.warehouse === warehouse))
      .sort((a, b) => a.occurredAt.localeCompare(b.occurredAt) || a.id - b.id);
    let opening = 0, increase = 0, decrease = 0, balance = 0;
    const rows = [];
    scope.forEach(item => {
      const date = item.occurredAt.slice(0, 10);
      if (to && date > to) return;
      balance += item.delta;
      if (from && date < from) { opening += item.delta; return; }
      increase += Math.max(0, item.delta);
      decrease += Math.max(0, -item.delta);
      rows.push({ ...item, balance });
    });
    return { opening, increase, decrease, closing: balance, rows };
  }

  function checkExport(item, quantity, canWrite, canOverride, reason = '', confirmed = false) {
    if (!canWrite) throw new Error('Bạn chỉ có quyền xem dữ liệu.');
    if (!Number.isFinite(quantity) || quantity <= 0 || quantity > item.remaining) throw new Error('Số lượng phải lớn hơn 0 và không vượt lượng còn lại.');
    if (daysRemaining(item.expiry) < 0) {
      if (!canOverride) throw new Error('Lô đã quá hạn. Cần Quản lý kho xác nhận ghi đè kèm lý do.');
      if (!reason.trim() || reason.trim().length > 500 || !confirmed) throw new Error('Nhập lý do hợp lệ và đánh dấu xác nhận ghi đè.');
    }
    return { overridden: daysRemaining(item.expiry) < 0, reason: reason.trim() };
  }

  const node = (tag, className = '', content) => {
    const element = document.createElement(tag);
    element.className = className;
    if (content !== undefined) element.textContent = content;
    return element;
  };
  const format = value => new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 3 }).format(value);
  const formatDate = value => value.slice(0, 10).split('-').reverse().join('/');
  const normalize = value => value.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd').trim();
  function fillProducts(select) {
    select.replaceChildren(...products.map(item => {
      const option = node('option', '', `${item.sku} · ${item.name}`);
      option.value = item.sku;
      return option;
    }));
  }
  function fillWarehouses(select) {
    const all = node('option', '', 'Tất cả kho'); all.value = '';
    select.replaceChildren(all, ...warehouses.map(item => {
      const option = node('option', '', item.name); option.value = item.code; return option;
    }));
  }
  window.InventoryTraceDemo = { today, offsetDate, products, warehouses, lots, movements, labels, product, warehouseName, daysRemaining, lotStatus, stockCard, checkExport, node, format, formatDate, normalize, fillProducts, fillWarehouses };
})();
