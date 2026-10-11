package com.oms.service;
import com.oms.config.Db;
import com.oms.dao.OrderDao;
import com.oms.dao.AuditLogDao;
import com.oms.model.User;
import java.sql.SQLException;
import java.time.LocalDate;

/** Sao chép nguyên nhóm dòng trong một transaction; không kế thừa giá đã chốt hoặc trạng thái. */
public class OrderCopyService {
    public long copy(long sourceId,User actor) throws SQLException {
        OrderService.requireOrders(actor);
        return Db.inTransaction(conn->{
            var dao=new OrderDao(); var orders=new OrderService();
            var source=dao.findByIdForUpdate(conn,sourceId); OrderService.requireVisible(source,actor);
            long id=orders.create(conn,source.getCustomerId(),actor);
            var target=dao.findById(conn,id);
            dao.updateHeader(conn,id,source.getDeliveryPointId(),
                    source.getDeliveryDate()!=null && !source.getDeliveryDate().isBefore(LocalDate.now())?source.getDeliveryDate():null,source.getNote());
            for(var line:dao.lines(conn,sourceId)) orders.addLine(conn,target,line.getProductId(),line.getUnitId(),line.getQuantity());
            new AuditLogDao().log(conn,actor.getId(),"ORDER_COPY","ORDER",id,"Sao chép từ "+source.getCode()+"; giá áp lại theo bảng giá hiện hành");
            return id;
        });
    }
}
