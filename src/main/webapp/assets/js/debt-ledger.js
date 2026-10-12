(() => {
  const root = document.getElementById('ledger-screen');
  if (!root) return;
  const $ = id => document.getElementById(id);
  const money = value => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(value);
  const dateText = value => new Intl.DateTimeFormat('vi-VN').format(new Date(`${value}T00:00:00`));
  const esc = value => String(value).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  const customers = [
    { id: 'DL-001', name: 'Đại lý Minh Anh', code: 'DL-001', opening: 5200000 },
    { id: 'DL-002', name: 'Tạp hóa Hồng Phúc', code: 'DL-002', opening: 3100000 }
  ];
  const entries = [
    { customerId: 'DL-001', date: '2026-09-02', ref: 'HD-260901', type: 'Bán hàng', note: 'Hóa đơn bán hàng', increase: 8450000, decrease: 0 },
    { customerId: 'DL-001', date: '2026-09-08', ref: 'PT-0042', type: 'Thu tiền', note: 'Thanh toán chuyển khoản', increase: 0, decrease: 5200000 },
    { customerId: 'DL-001', date: '2026-09-18', ref: 'HD-260914', type: 'Bán hàng', note: 'Hóa đơn bán hàng', increase: 6900000, decrease: 0 },
    { customerId: 'DL-001', date: '2026-10-11', ref: 'DC-0001', type: 'Giảm trừ trả hàng', note: 'Tham chiếu phiếu trả TH-0008', increase: 0, decrease: 123000 },
    { customerId: 'DL-001', date: '2026-10-20', ref: 'PT-0057', type: 'Thu tiền', note: 'Thanh toán tiền mặt', increase: 0, decrease: 3500000 },
    { customerId: 'DL-002', date: '2026-09-03', ref: 'HD-260903', type: 'Bán hàng', note: 'Hóa đơn bán hàng', increase: 8600000, decrease: 0 },
    { customerId: 'DL-002', date: '2026-09-16', ref: 'PT-0049', type: 'Thu tiền', note: 'Thanh toán chuyển khoản', increase: 0, decrease: 4000000 },
    { customerId: 'DL-002', date: '2026-10-04', ref: 'HD-261004', type: 'Bán hàng', note: 'Hóa đơn bán hàng', increase: 3750000, decrease: 0 },
    { customerId: 'DL-002', date: '2026-10-15', ref: 'PT-0061', type: 'Thu tiền', note: 'Thanh toán chuyển khoản', increase: 0, decrease: 2800000 }
  ];
  let report = null;
  $('ledger-customer').innerHTML = customers.map(customer => `<option value="${customer.id}">${esc(customer.code)} · ${esc(customer.name)}</option>`).join('');

  function buildReport() {
    const customer = customers.find(item => item.id === $('ledger-customer').value);
    const from = $('ledger-from').value, to = $('ledger-to').value;
    if (!from || !to || from > to) {
      $('ledger-error').textContent = 'Khoảng thời gian không hợp lệ. Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.';
      $('ledger-error').hidden = false;
      return null;
    }
    $('ledger-error').hidden = true;
    const all = entries.filter(item => item.customerId === customer.id).sort((a, b) => a.date.localeCompare(b.date));
    const before = all.filter(item => item.date < from);
    const opening = before.reduce((sum, item) => sum + item.increase - item.decrease, customer.opening);
    let balance = opening;
    const rows = all.filter(item => item.date >= from && item.date <= to).map(item => ({ ...item, balance: balance += item.increase - item.decrease }));
    const increase = rows.reduce((sum, item) => sum + item.increase, 0);
    const decrease = rows.reduce((sum, item) => sum + item.decrease, 0);
    return { customer, from, to, opening, increase, decrease, closing: opening + increase - decrease, rows };
  }

  function render() {
    report = buildReport();
    if (!report) return;
    $('ledger-title').textContent = `${report.customer.code} · ${report.customer.name}`;
    $('ledger-period').textContent = `Từ ${dateText(report.from)} đến ${dateText(report.to)}`;
    $('sum-opening').textContent = money(report.opening); $('sum-increase').textContent = money(report.increase); $('sum-decrease').textContent = money(report.decrease); $('sum-closing').textContent = money(report.closing);
    $('foot-increase').textContent = money(report.increase); $('foot-decrease').textContent = money(report.decrease); $('foot-closing').textContent = money(report.closing);
    $('ledger-empty').hidden = report.rows.length > 0; $('ledger-table').hidden = report.rows.length === 0;
    $('ledger-rows').innerHTML = report.rows.map(item => `<tr><td>${dateText(item.date)}</td><td><span class="ledger-doc">${esc(item.ref)}</span></td><td class="ledger-desc"><strong>${esc(item.type)}</strong><small>${esc(item.note)}</small></td><td class="ledger-money-up">${item.increase ? money(item.increase) : '—'}</td><td class="ledger-money-down">${item.decrease ? money(item.decrease) : '—'}</td><td class="ledger-balance">${money(item.balance)}</td></tr>`).join('');
  }

  function exportExcel() {
    if (!report) return;
    const rows = report.rows.map(item => `<tr><td>${dateText(item.date)}</td><td>${esc(item.ref)}</td><td>${esc(item.type)} - ${esc(item.note)}</td><td>${item.increase}</td><td>${item.decrease}</td><td>${item.balance}</td></tr>`).join('');
    const html = `<html><head><meta charset="UTF-8"></head><body><h2>Sổ chi tiết công nợ - ${esc(report.customer.name)}</h2><p>Từ ${dateText(report.from)} đến ${dateText(report.to)}</p><p>Dư đầu kỳ: ${report.opening}</p><table border="1"><tr><th>Ngày</th><th>Chứng từ</th><th>Diễn giải</th><th>Phát sinh tăng</th><th>Phát sinh giảm</th><th>Số dư</th></tr>${rows}</table><p>Dư cuối kỳ: ${report.closing}</p></body></html>`;
    const blob = new Blob(['\ufeff', html], { type: 'application/vnd.ms-excel;charset=utf-8' });
    const link = document.createElement('a'); link.href = URL.createObjectURL(blob); link.download = `so-cong-no-${report.customer.code}-${report.from}-${report.to}.xls`; link.click(); URL.revokeObjectURL(link.href);
  }

  $('ledger-apply').addEventListener('click', render);
  $('ledger-customer').addEventListener('change', render);
  $('export-excel').addEventListener('click', exportExcel);
  $('export-pdf').addEventListener('click', () => { document.title = `So cong no ${report.customer.code}`; window.print(); document.title = 'Sổ chi tiết công nợ'; });
  render();
})();
