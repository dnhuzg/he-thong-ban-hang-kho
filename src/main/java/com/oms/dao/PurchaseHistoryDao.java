package com.oms.dao;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PurchaseHistoryDao {
    public record Item(long productId,String sku,String name,String baseUnit,BigDecimal averageQuantity,int purchases) { }
    public List<Item> list(Connection conn,long customerId,LocalDate since) throws SQLException {
        var items=new ArrayList<Item>();
        try(var ps=conn.prepareStatement("SELECT p.id,p.sku,p.name,u.name,AVG(h.qty),COUNT(*) "
                + "FROM (SELECT l.product_id,o.id,SUM(l.base_quantity) qty FROM order_lines l JOIN orders o ON o.id=l.order_id "
                + "WHERE o.customer_id=? AND o.status IN ('SHIPPED','DELIVERED','CLOSED') AND o.created_at>=? "
                + "GROUP BY l.product_id,o.id) h JOIN products p ON p.id=h.product_id JOIN units u ON u.id=p.base_unit_id "
                + "GROUP BY p.id,p.sku,p.name,u.name ORDER BY p.sku")) {
            ps.setLong(1,customerId); ps.setTimestamp(2,Timestamp.valueOf(since.atStartOfDay()));
            try(var rs=ps.executeQuery()) {
                while(rs.next()) items.add(new Item(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getBigDecimal(5),rs.getInt(6)));
            }
        }
        return items;
    }
    public Long lastOrder(Connection conn,long customerId) throws SQLException {
        try(var ps=conn.prepareStatement("SELECT id FROM orders WHERE customer_id=? AND status IN ('SHIPPED','DELIVERED','CLOSED') ORDER BY created_at DESC,id DESC LIMIT 1")) {
            ps.setLong(1,customerId);
            try(var rs=ps.executeQuery()) { return rs.next()?rs.getLong(1):null; }
        }
    }
}
