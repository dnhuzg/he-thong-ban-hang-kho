(() => {
  'use strict';

  const screen = document.getElementById('stock-screen');
  if (!screen) return;

  // GHTK-57: dữ liệu minh họa phía frontend.
  // Backend sẽ thay nguồn dữ liệu này bằng tồn kho và mức tối thiểu theo SKU + kho.
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
  const modal = document.getElementById('minimum-modal');
  const minimumForm = document.getElementById('minimum-form');
  const minimumInput = document.getElementById('minimum-value');

  let editingItem = null;

  const format = value => new Intl.NumberFormat('vi-VN').format(value);

  const el = (tag, className, content) => {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (content !== undefined) node.textContent = content;
    return node;
  };

  const normalize = text =>
    text
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/đ/g, 'd')
      .trim();

  const available = item => Math.max(0, item.actual - item.reserved);

  const state = item => {
    if (available(item) === 0) return 'out';
    if (available(item) <= item.minimum) return 'low';
    return 'available';
  };

  const labels = {
    out: 'Hết hàng',
    low: 'Dưới mức tối thiểu',
    available: 'Đủ tồn'
  };

  const badges = {
    out: 'badge--danger',
    low: 'badge--warning',
    available: 'badge--success'
  };

  function statusBadge(item) {
    return el('span', `badge ${badges[state(item)]}`, labels[state(item)]);
  }

  function createMinimumButton(item) {
    const button = el('button', 'btn btn--ghost stock-minimum-button', 'Thiết lập');
    button.type = 'button';
    button.dataset.sku = item.sku;
    button.dataset.warehouse = item.warehouse;
    button.setAttribute(
      'aria-label',
      `Thiết lập tồn tối thiểu cho ${item.sku} tại ${item.warehouseName}`
    );
    return button;
  }

  function renderRow(item) {
    const itemState = state(item);
    const row = el(
      'tr',
      itemState === 'low'
        ? 'stock-row--low'
        : itemState === 'out'
          ? 'stock-row--out'
          : ''
    );

    const product = el('td');
    product.append(
      el('div', 'stock-table__name', item.name),
      el('div', 'stock-table__meta', item.sku)
    );

    row.append(
      product,
      el('td', '', item.warehouseName),
      el('td', '', item.category),
      el('td', 'stock-number', `${format(item.actual)} ${item.unit}`),
      el('td', 'stock-number', `${format(item.reserved)} ${item.unit}`)
    );

    row.append(
      el(
        'td',
        `stock-number stock-table__available${available(item) === 0 ? ' is-zero' : ''}`,
        `${format(available(item))} ${item.unit}`
      )
    );

    row.append(
      el(
        'td',
        'stock-number stock-table__minimum',
        `${format(item.minimum)} ${item.unit}`
      )
    );

    const statusCell = el('td');
    statusCell.append(statusBadge(item));

    const actionCell = el('td', 'stock-table__action');
    actionCell.append(createMinimumButton(item));

    row.append(statusCell, actionCell);
    return row;
  }

  function renderCard(item) {
    const itemState = state(item);
    const card = el(
      'article',
      `stock-mobile__card${
        itemState === 'low'
          ? ' stock-mobile__card--low'
          : itemState === 'out'
            ? ' stock-mobile__card--out'
            : ''
      }`
    );

    const header = el('div', 'stock-mobile__header');
    const title = el('div');

    title.append(
      el('strong', '', item.name),
      el('span', 'stock-table__meta', item.sku)
    );

    header.append(title, statusBadge(item));

    card.append(
      header,
      el(
        'span',
        'stock-mobile__subtitle',
        `${item.warehouseName} · ${item.category}`
      )
    );

    const values = el('div', 'stock-mobile__values');

    [
      ['Thực tế', item.actual],
      ['Giữ chỗ', item.reserved],
      ['Khả dụng', available(item)],
      ['Tối thiểu', item.minimum]
    ].forEach(([label, value]) => {
      const box = el('div');
      box.append(
        el('span', '', label),
        el('strong', '', `${format(value)} ${item.unit}`)
      );
      values.append(box);
    });

    const actions = el('div', 'stock-mobile__actions');
    actions.append(createMinimumButton(item));

    card.append(values, actions);
    return card;
  }

  function getMatchingItems() {
    const search = normalize(document.getElementById('stock-search').value);
    const warehouse = document.getElementById('stock-warehouse').value;
    const category = document.getElementById('stock-category').value;
    const status = document.getElementById('stock-status').value;

    return stock.filter(item =>
      (!search ||
        normalize(item.sku).includes(search) ||
        normalize(item.name).includes(search)) &&
      (!warehouse || item.warehouse === warehouse) &&
      (!category || item.category === category) &&
      (!status || state(item) === status)
    );
  }

  function renderAlertSummary(items) {
    const summary = document.getElementById('stock-alert-summary');
    const lowItems = items.filter(item => state(item) === 'low');
    const outItems = items.filter(item => state(item) === 'out');
    const warningCount = lowItems.length + outItems.length;

    document.getElementById('stock-alert-count').textContent =
      `${warningCount} cảnh báo`;

    summary.replaceChildren();

    if (warningCount === 0) {
      const message = el(
        'div',
        'alert alert--success',
        'Không có SKU nào trong phạm vi đang lọc ở mức cần cảnh báo.'
      );
      summary.append(message);
      return;
    }

    const message = el('div', 'alert alert--warning');
    const strong = el(
      'strong',
      '',
      `${warningCount} dòng tồn kho cần chú ý. `
    );

    const detail = document.createTextNode(
      `${lowItems.length} dòng dưới mức tối thiểu và ${outItems.length} dòng đã hết hàng khả dụng.`
    );

    message.append(strong, detail);
    summary.append(message);
  }

  function render() {
    const matching = getMatchingItems();

    body.replaceChildren(...matching.map(renderRow));
    mobile.replaceChildren(...matching.map(renderCard));

    document.getElementById('stock-empty').hidden = matching.length !== 0;
    document.getElementById('stock-count').textContent =
      `${matching.length} kết quả`;
    document.getElementById('stock-footer-count').textContent =
      `Hiển thị ${matching.length} / ${stock.length} dòng`;

    document.getElementById('stat-lines').textContent = matching.length;
    document.getElementById('stat-low').textContent =
      matching.filter(item => state(item) === 'low').length;
    document.getElementById('stat-out').textContent =
      matching.filter(item => state(item) === 'out').length;

    renderAlertSummary(matching);
  }

  function findItem(button) {
    return stock.find(
      item =>
        item.sku === button.dataset.sku &&
        item.warehouse === button.dataset.warehouse
    );
  }

  function openMinimumModal(item) {
    editingItem = item;

    document.getElementById('minimum-product').textContent =
      `${item.name} · ${item.sku}`;
    document.getElementById('minimum-warehouse').textContent =
      item.warehouseName;

    minimumInput.value = item.minimum;
    modal.hidden = false;
    document.body.classList.add('stock-modal-open');

    requestAnimationFrame(() => minimumInput.focus());
  }

  function closeMinimumModal() {
    modal.hidden = true;
    document.body.classList.remove('stock-modal-open');
    editingItem = null;
    minimumForm.reset();
  }

  function handleMinimumButton(event) {
    const button = event.target.closest('.stock-minimum-button');
    if (!button) return;

    const item = findItem(button);
    if (item) openMinimumModal(item);
  }

  form.addEventListener('input', render);
  form.addEventListener('change', render);

  form.addEventListener('reset', () => {
    requestAnimationFrame(render);
  });

  document.getElementById('stock-empty-reset').addEventListener('click', () => {
    form.reset();
    requestAnimationFrame(render);
  });

  body.addEventListener('click', handleMinimumButton);
  mobile.addEventListener('click', handleMinimumButton);

  modal.addEventListener('click', event => {
    if (event.target.closest('[data-close-minimum]')) {
      closeMinimumModal();
    }
  });

  minimumForm.addEventListener('submit', event => {
    event.preventDefault();

    if (!editingItem) return;

    const value = Number.parseInt(minimumInput.value, 10);

    if (!Number.isInteger(value) || value < 0) {
      minimumInput.setCustomValidity(
        'Mức tồn tối thiểu phải là số nguyên từ 0 trở lên.'
      );
      minimumInput.reportValidity();
      return;
    }

    minimumInput.setCustomValidity('');
    editingItem.minimum = value;

    closeMinimumModal();
    render();
  });

  minimumInput.addEventListener('input', () => {
    minimumInput.setCustomValidity('');
  });

  document.addEventListener('keydown', event => {
    if (event.key === 'Escape' && !modal.hidden) {
      closeMinimumModal();
    }
  });

  render();
})();