package com.oms.service;
import com.oms.config.Db;
import com.oms.dao.OrderDao;
import com.oms.dao.OrderSearchDao;
import com.oms.model.Order;
import com.oms.model.User;
import com.oms.security.PermissionPolicy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrderSearchService {
    public record Item(long id,String code,String customer,Order.Status status,BigDecimal total,LocalDateTime createdAt) { }
    public record Result(List<Item> items,long count,BigDecimal total,int page,int pageSize) { }
    public Result search(OrderSearchDao.Filter filter,int page,int size,User viewer) throws SQLException {
        if (!PermissionPolicy.canTakeOrders(viewer.getRoles())) throw new ForbiddenException("Danh sách này chỉ dành cho nhân viên kinh doanh.");
        if(page<1 || page>100000 || size<1 || size>100) throw new ServiceException("Phân trang không hợp lệ.");
        if(filter.from()!=null && filter.to()!=null && filter.from().isAfter(filter.to())) throw new ServiceException("Ngày bắt đầu phải trước ngày kết thúc.");
        Long own=PermissionPolicy.canSeeAllOrders(viewer.getRoles())?null:viewer.getId();
        try(Connection conn=Db.getConnection()) {
            conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ); conn.setAutoCommit(false);
            try {
                var ids=new OrderSearchDao().ids(conn,filter,own);
                var items=new ArrayList<Item>();
                BigDecimal total=BigDecimal.ZERO;
                int index=0,offset=(page-1)*size;
                var dao=new OrderDao(); var orders=new OrderService();
                // Tính cả tập lọc, không chỉ trang hiện tại; đơn nháp còn dùng bảng giá đang hiệu lực.
                for(long id:ids) {
                    Order order=dao.findById(conn,id); order.setLines(dao.lines(conn,id)); orders.price(conn,order);
                    total=total.add(order.getPayable());
                    if(index>=offset && index<offset+size) items.add(new Item(id,order.getCode(),order.getCustomerName(),order.getStatus(),order.getPayable(),order.getCreatedAt()));
                    index++;
                }
                conn.commit(); return new Result(List.copyOf(items),ids.size(),total,page,size);
            } catch(SQLException | RuntimeException e) { conn.rollback(); throw e; }
        }
    }
}
