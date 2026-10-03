/* Dữ liệu minh họa dành riêng cho FE GHTK-51, không phải dữ liệu tồn kho. */
(function () {
  'use strict';
  const root = document.getElementById('warehouse-screen');
  if (!root) return;
  // Tách bản mẫu theo tài khoản và context ứng dụng, không trộn với dữ liệu kho thật.
  const key = 'oms.ghtk51.ui.v1:' + (document.body.dataset.ctx || '') + ':' + root.dataset.userId;
  const seed = {
    warehouses: [
      { id: 'w1', code: 'KHO-TN01', name: 'Kho trung tâm Thái Nguyên', address: 'Đường Cách Mạng Tháng Tám, Thái Nguyên', manager: 'Nguyễn Văn An', status: 'active' },
      { id: 'w2', code: 'KHO-HN01', name: 'Kho phân phối Hà Nội', address: 'Đường Ngọc Hồi, Hà Nội', manager: 'Trần Thu Hà', status: 'active' },
      { id: 'w3', code: 'KHO-BN01', name: 'Kho Bắc Ninh', address: 'Đường Lý Thái Tổ, Bắc Ninh', manager: 'Lê Minh Đức', status: 'inactive' }
    ],
    locations: [
      { id: 'l1', warehouseId: 'w1', code: 'KHU-A', name: 'Khu hàng đồ uống', type: 'zone', parentId: '' },
      { id: 'l2', warehouseId: 'w1', code: 'KE-A01', name: 'Kệ cà phê', type: 'shelf', parentId: 'l1' },
      { id: 'l3', warehouseId: 'w1', code: 'KE-A02', name: 'Kệ trà và nước giải khát', type: 'shelf', parentId: 'l1' },
      { id: 'l4', warehouseId: 'w1', code: 'KHU-B', name: 'Khu thực phẩm khô', type: 'zone', parentId: '' },
      { id: 'l5', warehouseId: 'w1', code: 'KE-B01', name: 'Kệ bánh kẹo', type: 'shelf', parentId: 'l4' },
      { id: 'l6', warehouseId: 'w2', code: 'KHU-A', name: 'Khu hàng tổng hợp', type: 'zone', parentId: '' },
      { id: 'l7', warehouseId: 'w2', code: 'KE-A01', name: 'Kệ hàng đóng hộp', type: 'shelf', parentId: 'l6' }
    ],
    dealers: [
      { id: 'd1', code: 'DL001', name: 'Đại lý Minh Anh', address: 'Thái Nguyên', warehouseId: 'w1' },
      { id: 'd2', code: 'DL002', name: 'Đại lý Hoàng Gia', address: 'Hà Nội', warehouseId: 'w2' },
      { id: 'd3', code: 'DL003', name: 'Đại lý An Phát', address: 'Bắc Ninh', warehouseId: 'w2' },
      { id: 'd4', code: 'DL004', name: 'Đại lý Hồng Hà', address: 'Thái Nguyên', warehouseId: '' }
    ]
  };
  const copy = value => JSON.parse(JSON.stringify(value));
  let state = copy(seed), persistent = true;
  try {
    const saved = JSON.parse(localStorage.getItem(key));
    if (saved && ['warehouses','locations','dealers'].every(k => Array.isArray(saved[k]))) state = saved;
  } catch (_) { persistent = false; }
  const normalize = value => String(value || '').trim();
  function required(value, label, limit) {
    const text = normalize(value);
    if (!text) throw new Error('Vui lòng nhập ' + label + '.');
    if (text.length > limit) throw new Error(label + ' không được quá ' + limit + ' ký tự.');
    return text;
  }
  function persist(next) {
    try { localStorage.setItem(key, JSON.stringify(next)); persistent = true; } catch (_) { persistent = false; }
    state = next;
  }
  function getWarehouse(id) {
    const warehouse = state.warehouses.find(w => w.id === id);
    if (!warehouse) throw new Error('Kho không còn tồn tại. Vui lòng chọn lại.');
    return warehouse;
  }
  function update(collection, record, id) {
    const next = copy(state);
    if (id) {
      const index = next[collection].findIndex(x => x.id === id);
      if (index < 0) throw new Error('Dữ liệu không còn tồn tại.');
      next[collection][index] = { ...record, id };
    } else next[collection].push({ ...record, id: collection + Date.now() + Math.random().toString(36).slice(2,8) });
    persist(next);
  }
  window.WarehouseDemo = {
    load: () => copy(state),
    isPersistent: () => persistent,
    reset: () => persist(copy(seed)),
    saveWarehouse(value, id) {
      const record = { code: required(value.code, 'mã kho', 30).toUpperCase(), name: required(value.name, 'tên kho', 100), address: required(value.address, 'địa chỉ', 255), manager: required(value.manager, 'người phụ trách', 100), status: value.status };
      if (!['active','inactive'].includes(record.status)) throw new Error('Chọn trạng thái hợp lệ.');
      if (state.warehouses.some(w => w.id !== id && w.code.toUpperCase() === record.code)) throw new Error('Mã kho đã tồn tại. Hãy sử dụng mã khác.');
      update('warehouses', record, id);
    },
    saveLocation(value, id) {
      getWarehouse(value.warehouseId);
      const record = { warehouseId: value.warehouseId, code: required(value.code, 'mã vị trí', 30).toUpperCase(), name: required(value.name, 'tên vị trí', 100), type: value.type, parentId: value.type === 'zone' ? '' : value.parentId || '' };
      if (!['zone','shelf'].includes(record.type)) throw new Error('Chọn loại vị trí hợp lệ.');
      if (state.locations.some(l => l.id !== id && l.warehouseId === record.warehouseId && l.code.toUpperCase() === record.code)) throw new Error('Mã vị trí đã tồn tại trong kho này.');
      if (record.parentId && !state.locations.some(l => l.id === record.parentId && l.id !== id && l.warehouseId === record.warehouseId && l.type === 'zone')) throw new Error('Khu chứa phải thuộc cùng kho.');
      if (id && state.locations.some(l => l.parentId === id) && record.type !== 'zone') throw new Error('Khu này đang có kệ. Giữ loại vị trí là Khu.');
      const previous = state.locations.find(l => l.id === id);
      if (previous && previous.warehouseId !== record.warehouseId) throw new Error('Không thay đổi kho của vị trí trong màn hình này.');
      update('locations', record, id);
    },
    assignDealer(id, warehouseId) {
      const warehouse = getWarehouse(warehouseId);
      if (warehouse.status !== 'active') throw new Error('Chọn một kho đang hoạt động để gán mới.');
      const dealer = state.dealers.find(d => d.id === id);
      if (!dealer) throw new Error('Đại lý không còn tồn tại.');
      update('dealers', { ...dealer, warehouseId }, id);
    }
  };
})();
