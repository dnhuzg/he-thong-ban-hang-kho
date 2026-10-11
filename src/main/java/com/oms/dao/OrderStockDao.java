package com.oms.dao;
import com.oms.service.ServiceException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.TreeMap;

/** Giữ chỗ trong transaction của đơn; khoá SKU theo cùng một thứ tự để tránh deadlock. */
public class OrderStockDao {
    public long warehouse(Connection conn, long customerId) throws SQLException {
        try(var ps=conn.prepareStatement("SELECT serving_warehouse_id FROM customers WHERE id=?")) {
            ps.setLong(1,customerId);
            try(var rs=ps.executeQuery()) {
                if(rs.next()) { long id=rs.getLong(1); if(!rs.wasNull()) return id; }
            }
        }
        throw new ServiceException("Đại lý chưa được gắn kho phục vụ. Vui lòng liên hệ quản lý.");
    }
    public BigDecimal available(Connection conn, long warehouseId, long productId, boolean lock) throws SQLException {
        try(var ps=conn.prepareStatement("SELECT on_hand-reserved FROM warehouse_inventory WHERE warehouse_id=? AND product_id=?"+(lock?" FOR UPDATE":""))) {
            ps.setLong(1,warehouseId); ps.setLong(2,productId);
            try(var rs=ps.executeQuery()) { return rs.next()?rs.getBigDecimal(1):BigDecimal.ZERO; }
        }
    }
    public void check(Connection conn,long warehouseId,Map<Long,BigDecimal> quantities) throws SQLException {
        for(var e:new TreeMap<>(quantities).entrySet()) {
            BigDecimal available=available(conn,warehouseId,e.getKey(),true);
            if(e.getValue().compareTo(available)>0) throw new ServiceException("Sản phẩm #"+e.getKey()+" vượt tồn khả dụng; tối đa "+available.toPlainString()+" đơn vị cơ sở.");
        }
    }
    public void reserve(Connection conn,long orderId,long warehouseId,Map<Long,BigDecimal> quantities) throws SQLException {
        check(conn,warehouseId,quantities);
        for(var e:new TreeMap<>(quantities).entrySet()) {
            try(var ps=conn.prepareStatement("UPDATE warehouse_inventory SET reserved=reserved+? WHERE warehouse_id=? AND product_id=? AND on_hand-reserved>=?")) {
                ps.setBigDecimal(1,e.getValue()); ps.setLong(2,warehouseId); ps.setLong(3,e.getKey()); ps.setBigDecimal(4,e.getValue());
                if(ps.executeUpdate()!=1) throw new ServiceException("Tồn kho đã thay đổi. Vui lòng kiểm tra lại.");
            }
            try(var ps=conn.prepareStatement("INSERT INTO order_inventory_reservations(order_id,warehouse_id,product_id,quantity) VALUES(?,?,?,?)")) {
                ps.setLong(1,orderId); ps.setLong(2,warehouseId); ps.setLong(3,e.getKey()); ps.setBigDecimal(4,e.getValue()); ps.executeUpdate();
            }
        }
    }
    /** Nhả giữ chỗ khi huỷ; lúc xuất thì trừ luôn tồn thực tế trong cùng transaction. */
    public void release(Connection conn,long orderId,boolean shipped) throws SQLException {
        try(var ps=conn.prepareStatement("SELECT warehouse_id,product_id,quantity FROM order_inventory_reservations WHERE order_id=? ORDER BY warehouse_id,product_id FOR UPDATE")) {
            ps.setLong(1,orderId);
            try(var rs=ps.executeQuery()) {
                while(rs.next()) {
                    try(var update=conn.prepareStatement("UPDATE warehouse_inventory SET reserved=reserved-?, on_hand=on_hand-? WHERE warehouse_id=? AND product_id=? AND reserved>=? AND on_hand>=?")) {
                        var quantity=rs.getBigDecimal(3);
                        update.setBigDecimal(1,quantity); update.setBigDecimal(2,shipped?quantity:BigDecimal.ZERO);
                        update.setLong(3,rs.getLong(1)); update.setLong(4,rs.getLong(2)); update.setBigDecimal(5,quantity); update.setBigDecimal(6,shipped?quantity:BigDecimal.ZERO);
                        if(update.executeUpdate()!=1) throw new ServiceException("Giữ chỗ tồn kho không còn hợp lệ.");
                    }
                }
            }
        }
        try(var ps=conn.prepareStatement("DELETE FROM order_inventory_reservations WHERE order_id=?")) {
            ps.setLong(1,orderId); ps.executeUpdate();
        }
    }
}
