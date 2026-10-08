package com.oms.dao;
import com.oms.model.Order;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class OrderSearchDao {
    public record Filter(Order.Status status,Long customerId,Long creatorId,Long areaId,LocalDate from,LocalDate to) { }
    public List<Long> ids(Connection conn,Filter filter,Long ownRep) throws SQLException {
        var where=new StringBuilder(" WHERE 1=1");
        var params=new ArrayList<Object>();
        if(filter.status()!=null) { where.append(" AND o.status=?"); params.add(filter.status().name()); }
        if(filter.customerId()!=null) { where.append(" AND o.customer_id=?"); params.add(filter.customerId()); }
        if(filter.creatorId()!=null) { where.append(" AND o.created_by=?"); params.add(filter.creatorId()); }
        if(filter.areaId()!=null) { where.append(" AND c.area_id=?"); params.add(filter.areaId()); }
        if(ownRep!=null) { where.append(" AND c.sales_rep_id=?"); params.add(ownRep); }
        if(filter.from()!=null) { where.append(" AND o.created_at>=?"); params.add(Timestamp.valueOf(filter.from().atStartOfDay())); }
        if(filter.to()!=null) { where.append(" AND o.created_at<?"); params.add(Timestamp.valueOf(filter.to().plusDays(1).atStartOfDay())); }
        var result=new ArrayList<Long>();
        try(var ps=conn.prepareStatement("SELECT o.id FROM orders o JOIN customers c ON c.id=o.customer_id"+where+" ORDER BY o.created_at DESC,o.id DESC")) {
            for(int i=0;i<params.size();i++) ps.setObject(i+1,params.get(i));
            try(var rs=ps.executeQuery()) { while(rs.next()) result.add(rs.getLong(1)); }
        }
        return result;
    }
}
