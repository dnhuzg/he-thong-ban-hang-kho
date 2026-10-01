package com.oms.service;
import com.oms.config.Db;
import com.oms.dao.CustomerDao;
import com.oms.dao.OrderDao;
import com.oms.dao.PurchaseHistoryDao;
import com.oms.model.User;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class PurchaseHistoryService {
    public List<PurchaseHistoryDao.Item> history(long customerId,User viewer) throws SQLException {
        OrderService.requireOrders(viewer);
        try(var conn=Db.getConnection()) {
            var customer=new CustomerDao().findById(conn,customerId);
            if(customer==null || !OrderService.canSeeCustomer(viewer,customer)) throw new OrderService.NotFoundException();
            return new PurchaseHistoryDao().list(conn,customerId,LocalDate.now().minusMonths(3));
        }
    }
    public void addLastPurchase(long targetId,User actor) throws SQLException {
        OrderService.requireOrders(actor);
        Db.inTransaction(conn->{
            var orders=new OrderService();
            var target=orders.lockVisible(conn,targetId,actor);
            Long last=new PurchaseHistoryDao().lastOrder(conn,target.getCustomerId());
            if(last==null) throw new ServiceException("Đại lý chưa có đơn đã mua để thêm nhanh.");
            for(var line:new OrderDao().lines(conn,last)) orders.addLine(conn,target,line.getProductId(),line.getUnitId(),line.getQuantity());
            new OrderDao().touch(conn,targetId);
            new com.oms.dao.AuditLogDao().log(conn,actor.getId(),"ORDER_ADD_LAST_PURCHASE","ORDER",targetId,"Thêm hàng từ đơn #"+last);
            return null;
        });
    }
}
