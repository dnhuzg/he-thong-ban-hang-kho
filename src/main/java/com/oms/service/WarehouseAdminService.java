package com.oms.service;

import com.oms.config.Db;
import com.oms.dao.AuditLogDao;
import com.oms.dao.WarehouseAdminDao;
import com.oms.model.User;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

public class WarehouseAdminService {
    private final WarehouseAdminDao dao=new WarehouseAdminDao();private final AuditLogDao audit=new AuditLogDao();
    public WarehouseAdminDao.PageData page()throws SQLException{try(Connection c=Db.getConnection()){return dao.page(c);}}
    public long saveWarehouse(Long id,String code,String name,String address,String manager,String status,User actor)throws SQLException{code=req(code,"Mã kho",20).toUpperCase();name=req(name,"Tên kho",150);address=req(address,"Địa chỉ",255);manager=req(manager,"Người phụ trách",150);if(!Set.of("ACTIVE","INACTIVE").contains(status))throw new ServiceException("Trạng thái kho không hợp lệ.");String finalCode=code,finalName=name,finalAddress=address,finalManager=manager;return Db.inTransaction(c->{long saved=dao.saveWarehouse(c,id,finalCode,finalName,finalAddress,finalManager,status);audit.log(c,actor.getId(),id==null?"WAREHOUSE_CREATE":"WAREHOUSE_UPDATE","WAREHOUSE",saved,finalCode+" · "+finalName);return saved;});}
    public long saveLocation(Long id,long warehouseId,String code,String name,String type,Long parentId,User actor)throws SQLException{code=req(code,"Mã vị trí",30).toUpperCase();name=req(name,"Tên vị trí",150);if(!Set.of("ZONE","SHELF").contains(type))throw new ServiceException("Loại vị trí không hợp lệ.");if(!daoExistsWarehouse(warehouseId))throw new ServiceException("Kho không tồn tại.");if("ZONE".equals(type))parentId=null;Long finalParentId=parentId;if(finalParentId!=null&&!daoExistsParent(finalParentId,warehouseId))throw new ServiceException("Khu chứa không thuộc kho đã chọn.");String finalCode=code,finalName=name;return Db.inTransaction(c->{long saved=dao.saveLocation(c,id,warehouseId,finalCode,finalName,type,finalParentId);audit.log(c,actor.getId(),id==null?"WAREHOUSE_LOCATION_CREATE":"WAREHOUSE_LOCATION_UPDATE","WAREHOUSE_LOCATION",saved,finalCode+" · "+finalName);return saved;});}
    public void assign(long dealerId,long warehouseId,User actor)throws SQLException{Db.inTransaction(c->{if(!dao.exists(c,"SELECT 1 FROM warehouses WHERE id=? AND status='ACTIVE'",warehouseId))throw new ServiceException("Chọn kho đang hoạt động.");dao.assignDealer(c,dealerId,warehouseId);audit.log(c,actor.getId(),"CUSTOMER_WAREHOUSE_ASSIGN","CUSTOMER",dealerId,"Kho phục vụ #"+warehouseId);return null;});}
    private boolean daoExistsWarehouse(long id)throws SQLException{try(Connection c=Db.getConnection()){return dao.exists(c,"SELECT 1 FROM warehouses WHERE id=?",id);}}
    private boolean daoExistsParent(long id,long warehouse)throws SQLException{try(Connection c=Db.getConnection();var ps=c.prepareStatement("SELECT 1 FROM warehouse_locations WHERE id=? AND warehouse_id=? AND type='ZONE'")){ps.setLong(1,id);ps.setLong(2,warehouse);try(var rs=ps.executeQuery()){return rs.next();}}}
    private static String req(String value,String label,int max){String x=value==null?"":value.strip();if(x.isEmpty())throw new ServiceException("Nhập "+label.toLowerCase()+".");if(x.length()>max)throw new ServiceException(label+" tối đa "+max+" ký tự.");return x;}
}
