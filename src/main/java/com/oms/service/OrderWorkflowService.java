package com.oms.service;
import com.oms.config.Db;
import com.oms.dao.*;
import com.oms.model.Order;
import com.oms.model.User;
import com.oms.security.PermissionPolicy;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class OrderWorkflowService {
    private final OrderDao orderDao=new OrderDao();
    private final OrderService orders=new OrderService();
    private final OrderWorkflowDao workflow=new OrderWorkflowDao();
    private final OrderStockDao stock=new OrderStockDao();
    private final OrderCreditDao credit=new OrderCreditDao();

    public Order.Status submit(long id,User actor) throws SQLException {
        OrderService.requireOrders(actor);
        return Db.inTransaction(conn->{
            Order order=orders.lockVisible(conn,id,actor);
            var customer=new CustomerDao().findByIdForUpdate(conn,order.getCustomerId());
            CustomerLockService.requireTradable(customer);
            new OrderCreditService().requireNotOverdue(conn,customer);
            if(order.getDeliveryPointId()==null) throw new ServiceException("Vui lòng chọn điểm giao hàng trước khi chốt đơn.");
            if(order.getDeliveryDate()!=null && order.getDeliveryDate().isBefore(LocalDate.now())) throw new ServiceException("Ngày giao mong muốn đã ở quá khứ.");
            order.setLines(orderDao.lines(conn,id)); orders.price(conn,order);
            if(order.getLines().isEmpty() || order.getUnpricedLines()>0) throw new ServiceException("Đơn phải có hàng và giá hợp lệ trước khi chốt.");
            var reasons=new ArrayList<String>();
            var quote=new OrderCreditService().evaluate(conn,customer,id,order.getPayable());
            if(quote.requiresApproval()) reasons.add("Vượt hạn mức công nợ: "+quote.exceededBy().toPlainString()+" ₫");
            for(var line:order.getLines()) {
                var product=new ProductDao().findById(conn,line.getProductId(),false);
                if(product==null || product.getStatus()!=com.oms.model.Product.Status.ACTIVE) throw new ServiceException("Sản phẩm đã ngừng kinh doanh.");
                var price=new PriceListService().effectivePrice(conn,order.getCustomerGroupId(),line.getProductId(),LocalDate.now());
                var floorTotal=price.getFloorPrice().multiply(line.getBaseQuantity());
                if(line.getNetAmount().compareTo(floorTotal)<0) reasons.add("SKU "+line.getSku()+" dưới giá sàn: "+floorTotal.subtract(line.getNetAmount()).toPlainString()+" ₫");
            }
            long warehouse=stock.warehouse(conn,customer.getId());
            stock.check(conn,warehouse,OrderStockService.quantities(order));
            workflow.snapshot(conn,order);
            boolean portal=actor.hasRole(com.oms.model.Role.CUSTOMER);
            if(portal) {
                reasons.add(0,"Đơn đại lý cần nhân viên phụ trách xác nhận");
                try(var ps=conn.prepareStatement("UPDATE orders SET customer_submitted=TRUE WHERE id=?")) { ps.setLong(1,id); ps.executeUpdate(); }
            }
            Order.Status next=reasons.isEmpty()?Order.Status.APPROVED:Order.Status.PENDING_APPROVAL;
            if(next==Order.Status.APPROVED) reserve(conn,order,warehouse);
            String reason=String.join("; ",reasons);
            if(reason.length()>1000) reason=reason.substring(0,1000);
            workflow.transition(conn,order,next,"SUBMIT",null,actor.getId(),warehouse,reason);
            return next;
        });
    }

    public record Pending(long id,String code,String customer,java.math.BigDecimal total,String reason) { }
    public List<Pending> pending(User viewer) throws SQLException {
        if(!PermissionPolicy.canTakeOrders(viewer.getRoles())) throw new ForbiddenException("Bạn không được xem hàng đợi xác nhận đơn.");
        var result=new ArrayList<Pending>();
        try(var conn=Db.getConnection()) {
            for(long id:workflow.pending(conn)) {
                Order order=orderDao.findById(conn,id);
                if(!PermissionPolicy.canSeeAllOrders(viewer.getRoles()) && (!order.isCustomerSubmitted() || order.getCustomerSalesRepId()==null || order.getCustomerSalesRepId()!=viewer.getId())) continue;
                order.setLines(orderDao.lines(conn,id)); orders.price(conn,order);
                result.add(new Pending(id,order.getCode(),order.getCustomerName(),order.getPayable(),order.getApprovalReason()));
            }
        }
        return result;
    }

    public void decide(long id,String action,String rawOpinion,User actor) throws SQLException {
        if(!PermissionPolicy.canTakeOrders(actor.getRoles())) throw new ForbiddenException("Bạn không được xác nhận hoặc duyệt đơn.");
        String opinion=opinion(rawOpinion,!"APPROVE".equals(action));
        if(!List.of("APPROVE","REJECT","RETURN").contains(action)) throw new ServiceException("Hành động duyệt không hợp lệ.");
        Db.inTransaction(conn->{
            Order order=locked(conn,id,actor);
            boolean manager=PermissionPolicy.canSeeAllOrders(actor.getRoles());
            if(!manager && (!order.isCustomerSubmitted() || order.getCustomerSalesRepId()==null || order.getCustomerSalesRepId()!=actor.getId())) throw new ForbiddenException("Chỉ nhân viên phụ trách được xác nhận đơn đại lý.");
            if(!manager && order.getApprovalReason()!=null && (order.getApprovalReason().contains("Vượt hạn mức") || order.getApprovalReason().contains("giá sàn"))) throw new ForbiddenException("Đơn vi phạm phải do Quản lý kinh doanh duyệt.");
            if(order.getStatus()!=Order.Status.PENDING_APPROVAL) throw new ServiceException("Chỉ xử lý được đơn đang chờ duyệt.");
            Order.Status next=switch(action) { case "APPROVE" -> Order.Status.APPROVED; case "REJECT" -> Order.Status.REJECTED; default -> Order.Status.DRAFT; };
            if(next==Order.Status.APPROVED) {
                var customer=new CustomerDao().findByIdForUpdate(conn,order.getCustomerId());
                CustomerLockService.requireTradable(customer);
                new OrderCreditService().requireNotOverdue(conn,customer);
                if(!manager) {
                    if(new OrderCreditService().evaluate(conn,customer,id,order.getPayable()).requiresApproval()) throw new ForbiddenException("Hạn mức đã thay đổi; cần Quản lý kinh doanh duyệt.");
                    for(var line:order.getLines()) {
                        var price=new PriceListService().effectivePrice(conn,order.getCustomerGroupId(),line.getProductId(),LocalDate.now());
                        if(price==null || line.getNetAmount().compareTo(price.getFloorPrice().multiply(line.getBaseQuantity()))<0) throw new ForbiddenException("Giá sàn đã thay đổi; cần Quản lý kinh doanh duyệt.");
                    }
                }
                reserve(conn,order,order.getWarehouseId());
            }
            workflow.transition(conn,order,next,action,opinion,actor.getId(),order.getWarehouseId(),order.getApprovalReason());
            return null;
        });
    }
    public List<OrderWorkflowDao.Event> history(long id,User viewer) throws SQLException {
        orders.get(id,viewer);
        try(var conn=Db.getConnection()) { return workflow.history(conn,id); }
    }
    private void reserve(Connection conn,Order order,long warehouse) throws SQLException {
        stock.reserve(conn,order.getId(),warehouse,OrderStockService.quantities(order));
        credit.reserve(conn,order.getId(),order.getCustomerId(),order.getPayable());
    }
    Order locked(Connection conn,long id,User actor) throws SQLException {
        Order order=orderDao.findByIdForUpdate(conn,id);
        OrderService.requireVisible(order,actor); order.setLines(orderDao.lines(conn,id)); orders.price(conn,order);
        return order;
    }
    static void requireManager(User user) {
        if(!PermissionPolicy.canSeeAllOrders(user.getRoles())) throw new ForbiddenException("Chỉ Quản lý kinh doanh được duyệt đơn.");
    }
    static String opinion(String value,boolean required) {
        String text=value==null?"":value.trim();
        if((required && text.isEmpty()) || text.length()>500) throw new ServiceException("Vui lòng nhập ý kiến từ 1 đến 500 ký tự.");
        return text.isEmpty()?null:text;
    }
}
