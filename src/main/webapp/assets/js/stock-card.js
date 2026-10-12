(() => {
  'use strict';
  const screen = document.getElementById('stock-card-screen');
  if (!screen) return;
  const demo = window.InventoryTraceDemo;
  const { node, format, formatDate } = demo;
  const field = id => document.getElementById(id);
  demo.fillProducts(field('card-sku'));
  demo.fillWarehouses(field('card-warehouse'));
  const sku = new URLSearchParams(window.location.search).get('sku');
  if (demo.product(sku)) field('card-sku').value = sku;
  const defaultSku = field('card-sku').value;

  function openDocument(item, product) {
    field('card-document-title').textContent = `Chứng từ mẫu · ${item.document}`;
    const entries = [
      ['Số chứng từ', item.document], ['Sản phẩm', `${product.sku} · ${product.name}`],
      ['Thời điểm', `${formatDate(item.occurredAt)} ${item.occurredAt.slice(11)}`],
      ['Loại phát sinh', demo.labels[item.type]], ['Kho', demo.warehouseName(item.warehouse)],
      ...(item.peerWarehouse ? [['Kho đối ứng', demo.warehouseName(item.peerWarehouse)]] : []),
      ['Số lượng', `${item.delta > 0 ? '+' : ''}${format(item.delta)} ${product.unit}`], ['Diễn giải', item.note]
    ];
    field('card-document-detail').replaceChildren(...entries.map(([label, value]) => {
      const row = node('div'); row.append(node('dt', '', label), node('dd', '', value)); return row;
    }));
    field('card-document').showModal();
  }
  function render() {
    try {
      const product = demo.product(field('card-sku').value);
      const warehouse = field('card-warehouse').value;
      const result = demo.stockCard(product.sku, warehouse, field('card-from').value, field('card-to').value);
      field('card-error').hidden = true;
      [['opening', result.opening], ['in', result.increase], ['out', result.decrease], ['closing', result.closing]].forEach(([id, value]) => { field(`card-${id}`).textContent = `${format(value)} ${product.unit}`; });
      field('card-context').textContent = `${product.sku} · ${product.name} · ${warehouse ? demo.warehouseName(warehouse) : 'Tất cả kho (tồn cộng gộp)'} · đơn vị ${product.unit}`;
      field('card-rows').replaceChildren(...result.rows.map(item => {
        const row = node('tr'), documentCell = node('td');
        const link = node('a', '', item.document);
        link.href = `#document-${item.id}`;
        link.setAttribute('aria-label', `Xem chứng từ mẫu ${item.document}, ${demo.labels[item.type]} tại ${demo.warehouseName(item.warehouse)}`);
        link.addEventListener('click', event => { event.preventDefault(); openDocument(item, product); });
        documentCell.append(link);
        row.append(node('td', '', `${formatDate(item.occurredAt)} ${item.occurredAt.slice(11)}`), documentCell,
          node('td', '', demo.labels[item.type]), node('td', '', demo.warehouseName(item.warehouse)),
          node('td', 'trace-number', item.delta > 0 ? format(item.delta) : '—'), node('td', 'trace-number', item.delta < 0 ? format(-item.delta) : '—'),
          node('td', 'trace-number trace-table__balance', format(item.balance)), node('td', '', item.note));
        return row;
      }));
      field('card-count').textContent = `${result.rows.length} phát sinh`;
      field('card-empty').hidden = result.rows.length > 0;
      field('card-footer').textContent = warehouse ? 'Tồn sau từng phát sinh tại kho đã chọn. Thứ tự cũ → mới; cùng thời điểm thì theo mã phát sinh.' : 'Tồn sau là tổng tồn của SKU ở tất cả kho. Hai dòng chuyển kho nội bộ làm tăng/giảm tại từng kho và không đổi tổng tồn sau khi ghi đủ cặp.';
    } catch (error) {
      field('card-error').textContent = error.message; field('card-error').hidden = false;
      field('card-rows').replaceChildren(); field('card-empty').hidden = true; field('card-count').textContent = 'Bộ lọc chưa hợp lệ';
      ['opening', 'in', 'out', 'closing'].forEach(id => { field(`card-${id}`).textContent = '—'; });
    }
  }
  field('card-filters').addEventListener('submit', event => event.preventDefault());
  field('card-filters').addEventListener('input', render);
  field('card-filters').addEventListener('change', render);
  field('card-filters').addEventListener('reset', () => requestAnimationFrame(() => { field('card-sku').value = defaultSku; render(); }));
  field('card-document-close').addEventListener('click', () => field('card-document').close());
  render();
})();
