const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const context = vm.createContext({ window: {}, Date, Intl });
vm.runInContext(fs.readFileSync(path.join(__dirname, '../../main/webapp/assets/js/inventory-trace-demo.js'), 'utf8'), context);
const demo = context.window.InventoryTraceDemo;

test('Tồn cuối theo kho khớp dữ liệu mẫu GHTK-53 và tổng lô', () => {
  for (const [sku, warehouse, expected] of [
    ['SP-001', 'KHO-HN', 480], ['SP-001', 'KHO-TN', 72], ['SP-001', 'KHO-BN', 0],
    ['SP-002', 'KHO-HN', 160], ['SP-002', 'KHO-TN', 18], ['SP-003', 'KHO-HN', 300], ['SP-003', 'KHO-BN', 80]
  ]) {
    const card = demo.stockCard(sku, warehouse);
    assert.equal(card.closing, expected);
    assert.equal(card.opening + card.increase - card.decrease, expected);
    assert.ok(card.rows.every(row => row.balance >= 0));
    if (demo.product(sku).trackLots) assert.equal(demo.lots.filter(item => item.sku === sku && item.warehouse === warehouse).reduce((sum, item) => sum + item.remaining, 0), expected);
  }
});

test('Lọc khoảng ngày giữ tồn đầu kỳ, bao gồm cả ngày kết thúc', () => {
  const card = demo.stockCard('SP-001', 'KHO-HN', demo.offsetDate(-20), demo.offsetDate(-5));
  assert.equal(card.opening, 456);
  assert.equal(card.rows.length, 2);
  assert.equal(card.rows[0].balance, 384);
  assert.equal(card.increase, 96);
  assert.equal(card.decrease, 72);
  assert.equal(card.closing, 480);
  const empty = demo.stockCard('SP-001', 'KHO-HN', demo.today, demo.today);
  assert.equal(empty.rows.length, 0);
  assert.equal(empty.opening, 480);
  assert.equal(empty.closing, 480);
});

test('Chuyển kho nội bộ có cặp chứng từ và không thay đổi tổng tồn', () => {
  const card = demo.stockCard('SP-001', '', demo.offsetDate(-20), demo.offsetDate(-20));
  assert.equal(card.rows.length, 2);
  assert.equal(card.increase, card.decrease);
  assert.equal(card.closing, card.opening);
  assert.equal(card.rows[0].document, card.rows[1].document);
  assert.ok(card.rows[0].id < card.rows[1].id);
});

test('Kho và ngày không hợp lệ bị từ chối', () => {
  assert.throws(() => demo.stockCard('SP-001', 'KHONG-CO'));
  assert.throws(() => demo.stockCard('SP-001', '', '2026-02-30', '2026-03-05'));
  assert.throws(() => demo.stockCard('SP-001', '', '2026-10-10', '2026-10-01'));
});

test('Cảnh báo tính cả hôm nay và ngưỡng; lô hết hàng không đếm quá hạn', () => {
  const item = { remaining: 1, expiry: demo.today };
  assert.equal(demo.lotStatus(item, 0), 'soon');
  assert.equal(demo.lotStatus({ ...item, expiry: demo.offsetDate(30) }, 30), 'soon');
  assert.equal(demo.lotStatus({ ...item, expiry: demo.offsetDate(31) }, 30), 'valid');
  assert.equal(demo.lotStatus({ ...item, expiry: demo.offsetDate(-1) }, 30), 'expired');
  assert.equal(demo.lotStatus({ remaining: 0, expiry: demo.offsetDate(-1) }, 30), 'empty');
});

test('Xuất lô quá hạn cần quyền, lý do và xác nhận; không sửa tồn mẫu', () => {
  const expired = demo.lots.find(item => item.id === 'L3');
  assert.throws(() => demo.checkExport(expired, 1, false, true, 'Kiểm thử', true));
  assert.throws(() => demo.checkExport(expired, 1, true, false, 'Kiểm thử', true));
  assert.throws(() => demo.checkExport(expired, 1, true, true, '   ', true));
  assert.throws(() => demo.checkExport(expired, 1, true, true, 'Kiểm thử', false));
  assert.throws(() => demo.checkExport(expired, 49, true, true, 'Kiểm thử', true));
  assert.throws(() => demo.checkExport(expired, NaN, true, true, 'Kiểm thử', true));
  assert.equal(demo.checkExport(expired, 1, true, true, ' Kiểm thử ', true).reason, 'Kiểm thử');
  assert.equal(expired.remaining, 48);
  assert.equal(demo.checkExport(demo.lots.find(item => item.id === 'L4'), 1, true, false).overridden, false);
});
