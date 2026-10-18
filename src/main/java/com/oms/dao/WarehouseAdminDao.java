package com.oms.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class WarehouseAdminDao {
    public record Warehouse(long id,String code,String name,String address,String manager,String status) { }
    public record Location(long id,long warehouseId,String code,String name,String type,Long parentId) { }
    public record Dealer(long id,String code,String name,String address,Long warehouseId) { }
    public record PageData(List<Warehouse> warehouses,List<Location> locations,List<Dealer> dealers) { }
    public PageData page(Connection conn)throws SQLException{return new PageData(warehouses(conn),locations(conn),dealers(conn));}
    public List<Warehouse> warehouses(Connection conn)throws SQLException{List<Warehouse> x=new ArrayList<>();try(var ps=conn.prepareStatement("SELECT id,code,name,address,manager_name,status FROM warehouses ORDER BY code");var rs=ps.executeQuery()){while(rs.next())x.add(new Warehouse(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6)));}return x;}
    public List<Location> locations(Connection conn)throws SQLException{List<Location>x=new ArrayList<>();try(var ps=conn.prepareStatement("SELECT id,warehouse_id,code,name,type,parent_id FROM warehouse_locations ORDER BY warehouse_id,type,code");var rs=ps.executeQuery()){while(rs.next()){long parent=rs.getLong(6);x.add(new Location(rs.getLong(1),rs.getLong(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.wasNull()?null:parent));}}return x;}
    public List<Dealer> dealers(Connection conn)throws SQLException{List<Dealer>x=new ArrayList<>();try(var ps=conn.prepareStatement("SELECT c.id,c.code,c.name,COALESCE((SELECT address FROM customer_delivery_points d WHERE d.customer_id=c.id ORDER BY d.is_default DESC,d.id LIMIT 1),''),c.serving_warehouse_id FROM customers c ORDER BY c.code");var rs=ps.executeQuery()){while(rs.next()){long w=rs.getLong(5);x.add(new Dealer(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.wasNull()?null:w));}}return x;}
    public long saveWarehouse(Connection conn,Long id,String code,String name,String address,String manager,String status)throws SQLException{if(id==null){try(var ps=conn.prepareStatement("INSERT INTO warehouses(code,name,address,manager_name,status) VALUES(?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){bindWarehouse(ps,code,name,address,manager,status);ps.executeUpdate();try(var rs=ps.getGeneratedKeys()){rs.next();return rs.getLong(1);}}}try(var ps=conn.prepareStatement("UPDATE warehouses SET code=?,name=?,address=?,manager_name=?,status=? WHERE id=?")){bindWarehouse(ps,code,name,address,manager,status);ps.setLong(6,id);if(ps.executeUpdate()!=1)throw new SQLException("Kho không tồn tại");return id;}}
    private static void bindWarehouse(PreparedStatement ps,String code,String name,String address,String manager,String status)throws SQLException{ps.setString(1,code);ps.setString(2,name);ps.setString(3,address);ps.setString(4,manager);ps.setString(5,status);}
    public long saveLocation(Connection conn,Long id,long warehouseId,String code,String name,String type,Long parentId)throws SQLException{if(id==null){try(var ps=conn.prepareStatement("INSERT INTO warehouse_locations(warehouse_id,code,name,type,parent_id) VALUES(?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){bindLocation(ps,warehouseId,code,name,type,parentId);ps.executeUpdate();try(var rs=ps.getGeneratedKeys()){rs.next();return rs.getLong(1);}}}try(var ps=conn.prepareStatement("UPDATE warehouse_locations SET code=?,name=?,type=?,parent_id=? WHERE id=? AND warehouse_id=?")){ps.setString(1,code);ps.setString(2,name);ps.setString(3,type);if(parentId==null)ps.setNull(4,Types.BIGINT);else ps.setLong(4,parentId);ps.setLong(5,id);ps.setLong(6,warehouseId);if(ps.executeUpdate()!=1)throw new SQLException("Vị trí không tồn tại");return id;}}
    private static void bindLocation(PreparedStatement ps,long warehouseId,String code,String name,String type,Long parentId)throws SQLException{ps.setLong(1,warehouseId);ps.setString(2,code);ps.setString(3,name);ps.setString(4,type);if(parentId==null)ps.setNull(5,Types.BIGINT);else ps.setLong(5,parentId);}
    public void assignDealer(Connection conn,long dealerId,long warehouseId)throws SQLException{try(var ps=conn.prepareStatement("UPDATE customers SET serving_warehouse_id=? WHERE id=?")){ps.setLong(1,warehouseId);ps.setLong(2,dealerId);if(ps.executeUpdate()!=1)throw new SQLException("Đại lý không tồn tại");}}
    public boolean exists(Connection conn,String sql,long id)throws SQLException{try(var ps=conn.prepareStatement(sql)){ps.setLong(1,id);try(var rs=ps.executeQuery()){return rs.next();}}}
}
