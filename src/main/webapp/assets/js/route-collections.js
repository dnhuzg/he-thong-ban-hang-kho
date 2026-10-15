(() => {
  'use strict';
  const root=document.getElementById('route-screen'),source=document.getElementById('route-data');
  if(!root||!source)return;
  const data=JSON.parse(source.textContent),$=id=>document.getElementById(id),canConfirm=root.dataset.canConfirm==='true';
  const fmt=v=>`${new Intl.NumberFormat('vi-VN').format(Number(v||0))} ₫`;
  const esc=v=>String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  if($('route-agency'))data.customers.forEach(x=>$('route-agency').add(new Option(`${x.code} · ${x.name} · còn nợ ${fmt(x.outstanding)}`,x.id)));
  if($('route-date')){$('route-date').value=new Date().toISOString().slice(0,10);$('route-date').max=$('route-date').value;}
  function render(){
    const records=data.records,sum=x=>x.reduce((s,r)=>s+Number(r.amount),0);
    $('route-total').textContent=fmt(sum(records));
    $('route-pending').textContent=fmt(sum(records.filter(r=>r.status==='PENDING')));
    $('route-confirmed').textContent=fmt(sum(records.filter(r=>r.status==='CONFIRMED')));
    const reps=[...new Set(records.map(r=>r.employee))].sort(),selected=$('route-filter-rep').value;
    $('route-filter-rep').innerHTML='<option value="">Tất cả</option>'+reps.map(x=>`<option>${esc(x)}</option>`).join('');
    $('route-filter-rep').value=reps.includes(selected)?selected:'';
    const day=$('route-filter-day').value,rep=$('route-filter-rep').value,status=$('route-filter-status').value.toUpperCase();
    const shown=records.filter(r=>(!day||r.date===day)&&(!rep||r.employee===rep)&&(!status||r.status===status));
    $('route-count').textContent=`${shown.length} khoản`;$('route-empty').hidden=shown.length>0;
    $('route-rows').innerHTML=shown.map(r=>`<tr><td>${esc(r.date)}<span class="route-table__sub">${esc(r.employee)}</span></td><td>${esc(r.customer)}</td><td>${fmt(r.amount)}</td><td>${r.receiptPath?`<a class="btn btn--ghost" href="${root.dataset.filePrefix}${encodeURI(r.receiptPath)}" target="_blank">Xem ảnh</a>`:'—'}</td><td><span class="route-state route-state--${r.status.toLowerCase()}">${r.status==='PENDING'?'Chờ nộp':'Đã nộp'}</span></td><td>${canConfirm&&r.status==='PENDING'?`<button class="btn btn--secondary" data-confirm="${r.id}">Xác nhận đã nhận</button>`:'—'}</td></tr>`).join('');
    const groups=new Map();records.forEach(r=>{const k=`${r.date}|${r.employee}`,g=groups.get(k)||{date:r.date,employee:r.employee,count:0,total:0,confirmed:0};g.count++;g.total+=Number(r.amount);if(r.status==='CONFIRMED')g.confirmed+=Number(r.amount);groups.set(k,g);});
    $('route-reconcile-empty').hidden=groups.size>0;
    $('route-reconcile').innerHTML=[...groups.values()].map(g=>`<tr><td>${g.date}</td><td>${esc(g.employee)}</td><td>${g.count}</td><td>${fmt(g.total)}</td><td>${fmt(g.confirmed)}</td><td>${fmt(g.total-g.confirmed)}</td></tr>`).join('');
  }
  ['route-filter-day','route-filter-rep','route-filter-status'].forEach(id=>$(id).onchange=render);
  $('route-clear').onclick=()=>{$('route-filter-day').value=$('route-filter-rep').value=$('route-filter-status').value='';render();};
  async function send(url,body){body.set('_csrf',root.dataset.csrf);const r=await fetch(url,{method:'POST',body,headers:{Accept:'application/json'}}),o=await r.json().catch(()=>({}));if(!r.ok)throw new Error(o.error||'Không thể xử lý.');location.assign(root.dataset.pageUrl);}
  $('route-rows').onclick=e=>{const b=e.target.closest('[data-confirm]');if(b&&confirm('Xác nhận đã nhận tiền nộp về công ty?'))send(root.dataset.confirmUrl,new URLSearchParams({id:b.dataset.confirm})).catch(x=>alert(x.message));};
  if($('route-form'))$('route-form').onsubmit=e=>{e.preventDefault();const f=new FormData();f.set('customerId',$('route-agency').value);f.set('date',$('route-date').value);f.set('amount',$('route-amount').value);f.set('receipt',$('route-receipt').files[0]);f.set('_csrf',root.dataset.csrf);fetch(root.dataset.recordUrl,{method:'POST',body:f,headers:{Accept:'application/json'}}).then(async r=>{const o=await r.json().catch(()=>({}));if(!r.ok)throw new Error(o.error||'Không thể ghi khoản thu.');location.assign(root.dataset.pageUrl);}).catch(x=>$('route-feedback').textContent=x.message);};
  render();
})();
