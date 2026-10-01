package com.oms.service;
import com.oms.config.Db;
import com.oms.dao.OrderStockDao;
import com.oms.model.Order;
import com.oms.model.User;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.ArrayList;

public class OrderStockService {
    public record Availability(long productId, long warehouseId, BigDecimal available, BigDecimal requested, boolean exceeds) { }
    public List<Availability> availability(long orderId,User viewer) throws SQLException {
        Order order=new OrderService().get(orderId,viewer);
        var dao=new OrderStockDao();
        try(var conn=Db.getConnection()) {
            long warehouse=dao.warehouse(conn,order.getCustomerId());
            var result=new ArrayList<Availability>();
            for(var e:quantities(order).entrySet()) {
                var available=dao.available(conn,warehouse,e.getKey(),false);
                result.add(new Availability(e.getKey(),warehouse,available,e.getValue(),e.getValue().compareTo(available)>0));
            }
            return result;
        }
    }
    public static Map<Long,BigDecimal> quantities(Order order) {
        var totals=new TreeMap<Long,BigDecimal>();
        for(var line:order.getLines()) totals.merge(line.getProductId(),line.getBaseQuantity(),BigDecimal::add);
        return totals;
    }
}
