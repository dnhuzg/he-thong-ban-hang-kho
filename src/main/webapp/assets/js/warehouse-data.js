(() => {
  'use strict';
  const root=document.getElementById('warehouse-screen'),source=document.getElementById('warehouse-data');if(!root||!source)return;
  const raw=JSON.parse(source.textContent),copy=v=>JSON.parse(JSON.stringify(v));
  const state={warehouses:raw.warehouses.map(x=>({...x,id:String(x.id),status:x.status.toLowerCase(),address:x.address||'',manager:x.manager||''})),locations:raw.locations.map(x=>({...x,id:String(x.id),warehouseId:String(x.warehouseId),parentId:x.parentId==null?'':String(x.parentId),type:x.type.toLowerCase()})),dealers:raw.dealers.map(x=>({...x,id:String(x.id),warehouseId:x.warehouseId==null?'':String(x.warehouseId)}))};
  const required=(value,label,max)=>{const text=String(value||'').trim();if(!text)throw new Error(`Vui lòng nhập ${label}.`);if(text.length>max)throw new Error(`${label} không được quá ${max} ký tự.`);return text;};
  async function post(url,values){const body=new URLSearchParams(values);body.set('_csrf',root.dataset.csrf);const res=await fetch(url,{method:'POST',body,headers:{Accept:'application/json'}}),out=await res.json().catch(()=>({}));if(!res.ok)throw new Error(out.error||'Không thể lưu dữ liệu.');location.reload();}
  window.WarehouseData={load:()=>copy(state),isPersistent:()=>true,
    saveWarehouse(value,id){const record={id:id||'',code:required(value.code,'mã kho',20).toUpperCase(),name:required(value.name,'tên kho',150),address:required(value.address,'địa chỉ',255),manager:required(value.manager,'người phụ trách',150),status:String(value.status||'').toUpperCase()};return post(root.dataset.warehouseUrl,record);},
    saveLocation(value,id){const record={id:id||'',warehouseId:value.warehouseId,code:required(value.code,'mã vị trí',30).toUpperCase(),name:required(value.name,'tên vị trí',150),type:String(value.type||'').toUpperCase(),parentId:value.parentId||''};return post(root.dataset.locationUrl,record);},
    assignDealer(id,warehouseId){return post(root.dataset.assignUrl,{dealerId:id,warehouseId});}
  };
})();
