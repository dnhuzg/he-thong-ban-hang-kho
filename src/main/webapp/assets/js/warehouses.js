(function () {
  'use strict';
  const root = document.getElementById('warehouse-screen');
  if (!root) return;
  const $ = id => root.querySelector('#' + id);
  const canWrite = root.dataset.canWrite === 'true';
  const repo = window.WarehouseData;
  const pages = {
    warehouses: { title: 'Danh sách kho', description: 'Khai báo thông tin và quản lý trạng thái các kho.', heading: 'Thông tin kho', hint: 'Mỗi kho có mã định danh riêng.', action: '+ Thêm kho', search: 'Tìm mã kho, tên kho hoặc địa chỉ…', heads: ['Mã kho','Tên kho','Địa chỉ','Người phụ trách','Trạng thái','Thao tác'] },
    locations: { title: 'Vị trí lưu trữ', description: 'Khai báo khu và kệ để xác định vị trí lưu hàng trong từng kho.', heading: 'Vị trí trong kho', hint: 'Kệ có thể nằm trong một khu hoặc được khai báo độc lập.', action: '+ Thêm vị trí', search: 'Tìm mã hoặc tên vị trí…', heads: ['Mã vị trí','Tên vị trí','Loại vị trí','Khu chứa','Kho','Thao tác'] },
    dealers: { title: 'Kho phục vụ đại lý', description: 'Thiết lập một kho phục vụ mặc định cho mỗi đại lý.', heading: 'Phân công kho phục vụ', hint: 'Chọn “Gán kho” hoặc “Đổi kho” tại đại lý cần cập nhật.', search: 'Tìm mã, tên hoặc địa chỉ đại lý…', heads: ['Mã đại lý','Tên đại lý','Địa chỉ','Kho mặc định','Tình trạng gán','Thao tác'] }
  };
  let page = 'warehouses', state = repo.load(), currentEdit = null, noticeTimer;
  function node(tag, text, className) {
    const el = document.createElement(tag);
    if (text !== undefined) el.textContent = text;
    if (className) el.className = className;
    return el;
  }
  function options(select, values, selected = '') {
    select.replaceChildren();
    values.forEach(([value, label]) => { const option = node('option', label); option.value = value; select.append(option); });
    select.value = selected;
  }
  function badge(label, kind) { return node('span', label, 'badge badge--' + kind); }
  function cell(text, className) { return node('td', text, className); }
  function action(label, handler, accessibleLabel) {
    const button = node('button', label, 'wh-link'); button.type = 'button';
    if (accessibleLabel) button.setAttribute('aria-label', accessibleLabel);
    button.addEventListener('click', handler); return button;
  }
  function actions(...buttons) { const td = cell(); const wrapper = node('div', undefined, 'wh-row-actions'); wrapper.append(...buttons.filter(Boolean)); td.append(wrapper); return td; }
  function showNotice(message) {
    $('notice').textContent = message + (repo.isPersistent() ? '' : ' Trình duyệt không cho lưu; thay đổi chỉ giữ trong lần mở này.');
    $('notice').hidden = false; clearTimeout(noticeTimer); noticeTimer = setTimeout(() => { $('notice').hidden = true; }, 5500);
  }
  const fold = text => String(text).normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd').replace(/Đ/g, 'D').toLowerCase();
  const warehouse = id => state.warehouses.find(w => w.id === id);
  function renderStats() {
    const values = page === 'warehouses' ? [ ['KHO','Tổng số kho',state.warehouses.length], ['ON','Kho đang hoạt động',state.warehouses.filter(w => w.status === 'active').length], ['OFF','Kho tạm ngừng',state.warehouses.filter(w => w.status === 'inactive').length] ] : page === 'locations' ? [ ['VT','Tổng vị trí',state.locations.length], ['KHU','Khu lưu trữ',state.locations.filter(l => l.type === 'zone').length], ['KỆ','Kệ lưu trữ',state.locations.filter(l => l.type === 'shelf').length] ] : [ ['ĐL','Tổng số đại lý',state.dealers.length], ['OK','Đã gán kho',state.dealers.filter(d => d.warehouseId).length], ['—','Chưa gán kho',state.dealers.filter(d => !d.warehouseId).length] ];
    $('stats').replaceChildren();
    values.forEach(([icon,label,value]) => { const item = node('div', undefined, 'wh-stat'); const copy = node('div'); copy.append(node('span',label,'wh-stat-label'),node('strong',String(value),'wh-stat-value')); item.append(node('span',icon,'wh-stat-icon'),copy); $('stats').append(item); });
  }
  function render() {
    state = repo.load(); renderStats();
    const q = fold($('search').value.trim()), filter = $('filter').value, selectedWarehouse = $('warehouse-filter').value;
    const source = state[page];
    const records = source.filter(item => {
      const matches = fold([item.code,item.name,item.address || ''].join(' ')).includes(q);
      if (page === 'warehouses') return matches && (!filter || item.status === filter);
      if (page === 'locations') return matches && (!filter || item.type === filter) && (!selectedWarehouse || item.warehouseId === selectedWarehouse);
      return matches && (!selectedWarehouse || item.warehouseId === selectedWarehouse) && (!filter || (filter === 'assigned' ? !!item.warehouseId : !item.warehouseId));
    });
    $('tbody').replaceChildren();
    for (const item of records) {
      const tr = node('tr'); tr.append(cell(item.code));
      if (page === 'warehouses') {
        const name = cell(item.name,'wh-cell-main');
        const count = state.locations.filter(l => l.warehouseId === item.id).length;
        name.append(node('span', count + ' vị trí lưu trữ','wh-cell-sub'));
        const status = cell(); status.append(badge(item.status === 'active' ? 'Hoạt động' : 'Tạm ngừng', item.status === 'active' ? 'success' : 'warning'));
        tr.append(name,cell(item.address),cell(item.manager),status,actions(action('Vị trí',() => { go('locations'); $('warehouse-filter').value = item.id; render(); },'Xem vị trí '+item.name),canWrite ? action('Sửa',() => openWarehouse(item),'Sửa '+item.name) : null));
      } else if (page === 'locations') {
        const type = cell(); type.append(badge(item.type === 'zone' ? 'Khu' : 'Kệ',item.type === 'zone' ? 'primary' : 'success'));
        tr.append(cell(item.name,'wh-cell-main'),type,cell(state.locations.find(l => l.id === item.parentId)?.name || '—'),cell(warehouse(item.warehouseId)?.code || '—'),actions(canWrite ? action('Sửa',() => openLocation(item),'Sửa vị trí '+item.code) : null));
      } else {
        const w = warehouse(item.warehouseId), wCell = cell(w?.name || 'Chưa gán kho');
        if (w) wCell.append(node('span',w.code + (w.status === 'inactive' ? ' · Kho tạm ngừng' : ''),'wh-cell-sub'));
        const status = cell(); status.append(badge(w ? 'Đã gán' : 'Chưa gán',w ? 'success' : 'warning'));
        tr.append(cell(item.name,'wh-cell-main'),cell(item.address),wCell,status,actions(canWrite ? action(w ? 'Đổi kho' : 'Gán kho',() => openDealer(item),'Gán kho cho '+item.name) : null));
      }
      $('tbody').append(tr);
    }
    $('empty').hidden = records.length !== 0;
    $('result-count').textContent = records.length + ' kết quả';
    $('row-count').textContent = 'Hiển thị ' + records.length + ' / ' + source.length + (page === 'warehouses' ? ' kho' : page === 'locations' ? ' vị trí' : ' đại lý');
    $('table-note').textContent = page === 'warehouses' ? 'Chọn “Vị trí” để khai báo khu và kệ của kho.' : page === 'locations' ? 'Mã vị trí là duy nhất trong từng kho.' : 'Mỗi đại lý có một kho phục vụ mặc định.';
    const w = warehouse(selectedWarehouse);
    $('warehouse-summary').hidden = !(page === 'locations' && w);
    $('warehouse-summary').textContent = w ? w.name + ' · ' + w.address : '';
  }
  function go(next, updateHash = true) {
    page = pages[next] ? next : 'warehouses'; const config = pages[page];
    if (updateHash) history.replaceState(null, '', '#' + page);
    root.querySelectorAll('[data-page]').forEach(link => {
      const active = link.dataset.page === page;
      link.classList.toggle('is-active', active);
      if (active) link.setAttribute('aria-current','page'); else link.removeAttribute('aria-current');
    });
    $('page-title').textContent = config.title; $('page-description').textContent = config.description;
    $('breadcrumb').textContent = 'Kho & tồn kho / ' + config.title;
    $('table-title').textContent = config.heading; $('table-description').textContent = config.hint;
    $('table-caption').textContent = config.title; $('search-label').textContent = config.search;
    $('add').hidden = !config.action || !canWrite; $('add').textContent = config.action || '';
    $('search').value = ''; $('search').placeholder = config.search;
    $('filter-label').textContent = page === 'warehouses' ? 'Trạng thái' : page === 'locations' ? 'Loại vị trí' : 'Tình trạng gán';
    options($('filter'), page === 'warehouses' ? [['','Tất cả trạng thái'],['active','Hoạt động'],['inactive','Tạm ngừng']] : page === 'locations' ? [['','Tất cả vị trí'],['zone','Khu'],['shelf','Kệ']] : [['','Tất cả đại lý'],['assigned','Đã gán kho'],['unassigned','Chưa gán kho']]);
    $('warehouse-filter-wrap').hidden = page === 'warehouses';
    options($('warehouse-filter'), [['','Tất cả kho'],...repo.load().warehouses.map(w => [w.id,w.code+' · '+w.name])]);
    const row = node('tr'); config.heads.forEach(label => { const th = node('th',label); th.scope = 'col'; row.append(th); }); $('thead').replaceChildren(row);
    closeMenu(); render();
  }
  function field(name, label, value = '', settings = {}) {
    const wrapper = node('label',undefined,'wh-field' + (settings.full ? ' wh-field--full' : ''));
    wrapper.append(node('span',label+(settings.required === false ? '' : ' *')));
    const input = node(settings.options ? 'select' : settings.multiline ? 'textarea' : 'input');
    input.id = 'field-'+name; input.name = name; input.className = settings.options ? 'select' : 'input'; input.required = settings.required !== false;
    if (settings.options) options(input,settings.options,value); else { input.value = value; input.maxLength = settings.max || 100; }
    if (settings.disabled) input.disabled = true;
    wrapper.append(input);
    if (settings.hint) wrapper.append(node('small',settings.hint));
    $('fields').append(wrapper); return input;
  }
  function startEditor(kind, item, title, submitLabel) {
    state = repo.load(); currentEdit = {kind,id:item?.id}; $('fields').replaceChildren(); $('form-error').hidden = true;
    $('editor-title').textContent = title; $('save').textContent = submitLabel;
    $('editor-eyebrow').textContent = kind === 'warehouse' ? 'THÔNG TIN KHO' : kind === 'location' ? 'VỊ TRÍ LƯU TRỮ' : 'KHO PHỤC VỤ MẶC ĐỊNH';
  }
  function openWarehouse(item) {
    if (!canWrite) return;
    startEditor('warehouse',item,item ? 'Sửa thông tin kho' : 'Thêm kho','Lưu kho');
    field('code','Mã kho',item?.code,{max:30,hint:'Ví dụ: KHO-TN01. Không trùng với kho khác.'});
    field('status','Trạng thái',item?.status || 'active',{options:[['active','Hoạt động'],['inactive','Tạm ngừng']]});
    field('name','Tên kho',item?.name,{full:true}); field('address','Địa chỉ',item?.address,{full:true,multiline:true,max:255});
    field('manager','Người phụ trách',item?.manager,{full:true,hint:'Nhập họ tên người phụ trách kho.'});
    if (item && state.dealers.some(d => d.warehouseId === item.id)) $('fields').append(node('p','Kho đang được gán cho đại lý. Đổi trạng thái không tự thay đổi kho mặc định của đại lý.','wh-field--full wh-description'));
    $('editor').showModal();
  }
  function openLocation(item) {
    if (!canWrite) return;
    startEditor('location',item,item ? 'Sửa vị trí lưu trữ' : 'Thêm vị trí lưu trữ','Lưu vị trí');
    const initial = item?.warehouseId || $('warehouse-filter').value || state.warehouses[0]?.id || '';
    const wh = field('warehouseId','Kho',initial,{full:true,disabled:!!item,options:[['','Chọn kho'],...state.warehouses.map(w => [w.id,w.code+' · '+w.name])]});
    field('code','Mã vị trí',item?.code,{max:30,hint:'Ví dụ: KHU-A hoặc KE-A01.'});
    const type = field('type','Loại vị trí',item?.type || 'zone',{options:[['zone','Khu'],['shelf','Kệ']]});
    field('name','Tên vị trí',item?.name,{full:true});
    const parent = field('parentId','Khu chứa',item?.parentId || '',{full:true,required:false,options:[['','Kệ độc lập']],hint:'Chọn khu nếu kệ nằm trong một khu đã khai báo.'});
    function refreshParent() {
      const previous = parent.value || item?.parentId || '';
      options(parent,[['','Kệ độc lập'],...state.locations.filter(l => l.warehouseId === wh.value && l.type === 'zone' && l.id !== item?.id).map(l => [l.id,l.code+' · '+l.name])],previous);
      if (parent.selectedIndex < 0) parent.value = '';
      parent.parentElement.hidden = type.value !== 'shelf';
    }
    wh.addEventListener('change',refreshParent); type.addEventListener('change',refreshParent); refreshParent(); $('editor').showModal();
  }
  function openDealer(item) {
    if (!canWrite) return;
    startEditor('dealer',item,item.warehouseId ? 'Đổi kho phục vụ mặc định' : 'Gán kho phục vụ mặc định','Lưu kho mặc định');
    field('dealer','Đại lý',item.code+' · '+item.name,{full:true,disabled:true});
    const w = warehouse(item.warehouseId);
    $('fields').append(node('p','Kho hiện tại: '+(w ? w.name : 'Chưa gán kho'),'wh-field--full wh-description'));
    field('warehouseId','Kho phục vụ mặc định',w?.status === 'active' ? w.id : '',{full:true,options:[['','Chọn kho phục vụ'],...state.warehouses.filter(x => x.status === 'active').map(x => [x.id,x.code+' · '+x.name])],hint:'Bản thử chỉ cho gán mới vào kho đang hoạt động.'});
    $('editor').showModal();
  }
  $('editor-form').addEventListener('submit',async event => {
    event.preventDefault();
    if (!canWrite) return;
    const values = Object.fromEntries(new FormData(event.currentTarget));
    try {
      $('save').disabled = true;
      if (currentEdit.kind === 'warehouse') await repo.saveWarehouse(values,currentEdit.id);
      else if (currentEdit.kind === 'location') { if(currentEdit.id) values.warehouseId = state.locations.find(l => l.id === currentEdit.id).warehouseId; await repo.saveLocation(values,currentEdit.id); }
      else await repo.assignDealer(currentEdit.id,values.warehouseId);
    } catch (error) { $('form-error').textContent = error.message; $('form-error').hidden = false; $('save').disabled = false; }
  });
  root.querySelectorAll('[data-close]').forEach(button => button.addEventListener('click',() => $('editor').close()));
  root.querySelectorAll('[data-page]').forEach(link => link.addEventListener('click',event => { event.preventDefault(); go(link.dataset.page); }));
  $('add').addEventListener('click',() => page === 'warehouses' ? openWarehouse() : openLocation());
  $('search').addEventListener('input',render); $('filter').addEventListener('change',render); $('warehouse-filter').addEventListener('change',render);
  $('clear-filter').addEventListener('click',() => { $('search').value = ''; $('filter').value = ''; $('warehouse-filter').value = ''; render(); });
  function closeMenu() {
    document.querySelector('.app')?.classList.remove('is-sidebar-open');
    document.querySelectorAll('[data-sidebar-toggle]').forEach(button => button.setAttribute('aria-expanded', 'false'));
  }
  document.addEventListener('keydown',event => { if (event.key === 'Escape') closeMenu(); });
  window.addEventListener('hashchange',() => go(location.hash.slice(1),false));
  go(location.hash.slice(1),false);
})();
