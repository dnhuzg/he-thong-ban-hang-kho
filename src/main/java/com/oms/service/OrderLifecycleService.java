package com.oms.service;
import com.oms.config.Db;
import com.oms.dao.*;
import com.oms.model.Order;
import com.oms.model.Role;
import com.oms.model.User;
import java.sql.SQLException;

/** Mỗi bước kiểm trạng thái dưới khoá đơn và ghi lịch sử cùng transaction với tồn kho. */
public class OrderLifecycleService {
    public void cancel(long id,String reason,User actor) throws SQLException {
        OrderService.requireOrders(actor);
        String opinion=OrderWorkflowService.opinion(reason,true);
        Db.inTransaction(conn->{
            var order=new OrderWorkflowService().locked(conn,id,actor);
            if(!order.isCancellable()) {
                throw new ServiceException("Đơn đã xuất kho hoặc đã kết thúc không thể huỷ; hàng đã xuất phải đi qua trả hàng.");
            }
            new CustomerDao().findByIdForUpdate(conn,order.getCustomerId());
            new OrderStockDao().release(conn,id,false);
            new OrderCreditDao().release(conn,id);
            var dao=new OrderWorkflowDao();
            if(order.getStatus()==Order.Status.DRAFT) dao.snapshot(conn,order);
            dao.transition(conn,order,Order.Status.CANCELLED,"CANCEL",opinion,actor.getId(),order.getWarehouseId(),order.getApprovalReason());
            return null;
        });
    }

    public void advance(long id,Order.Status next,User actor) throws SQLException {
        Db.inTransaction(conn->{
            var dao=new OrderDao();
            Order order=dao.findByIdForUpdate(conn,id);
            if(order==null) throw new OrderService.NotFoundException();
            boolean warehouseActor=actor.isAdmin() || ((actor.hasRole(Role.WAREHOUSE) || actor.hasRole(Role.WH_MANAGER))
                    && order.getWarehouseId()!=null && actor.getWarehouses().stream().anyMatch(w->w.getId()==order.getWarehouseId()));
            boolean allowed=switch(next) {
                case PICKING,SHIPPED -> warehouseActor;
                case DELIVERED -> warehouseActor || (actor.hasRole(Role.SALES_REP) && order.getCustomerSalesRepId()!=null && order.getCustomerSalesRepId()==actor.getId());
                case CLOSED -> actor.isAdmin() || actor.hasRole(Role.ACCOUNTANT);
                default -> false;
            };
            if(!allowed) throw new ForbiddenException("Bạn không được thực hiện bước này hoặc không thuộc kho phục vụ.");
            boolean valid=switch(order.getStatus()) {
                case APPROVED -> next==Order.Status.PICKING;
                case PICKING -> next==Order.Status.SHIPPED;
                case SHIPPED -> next==Order.Status.DELIVERED;
                case DELIVERED -> next==Order.Status.CLOSED;
                default -> false;
            };
            if(!valid) throw new ServiceException("Không được bỏ qua bước hoặc chuyển ngược vòng đời đơn.");
            if(next==Order.Status.SHIPPED) {
                new CustomerDao().findByIdForUpdate(conn,order.getCustomerId());
                new OrderStockDao().release(conn,id,true);
                // Cam kết hạn mức còn giữ đến khi module hoá đơn chuyển sang công nợ phải thu.
            }
            new OrderWorkflowDao().transition(conn,order,next,"ADVANCE",null,actor.getId(),order.getWarehouseId(),order.getApprovalReason());
            return null;
        });
    }
}
