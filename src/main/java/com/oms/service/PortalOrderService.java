package com.oms.service;
import com.oms.config.Db;
import com.oms.dao.*;
import com.oms.model.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PortalOrderService {
    private final OrderService orders=new OrderService();
    public record Unit(long id,String name,BigDecimal factor) { }
    public record CatalogItem(long productId,String sku,String name,BigDecimal basePrice,List<Unit> units) { }
    public record Line(long id,long productId,String sku,String name,long unitId,String unit,int quantity,BigDecimal unitPrice,BigDecimal discount,BigDecimal total) { }
    public record Point(long id,String address,String receiver,String phone) { }
    public record Cart(long id,String code,Order.Status status,List<Line> lines,BigDecimal gross,BigDecimal discount,BigDecimal total,OrderCreditService.Quote credit,List<Point> deliveryPoints,Long deliveryPointId,LocalDate deliveryDate,String note) { }

    public long customerId(User viewer) throws SQLException {
        if(!viewer.hasRole(Role.CUSTOMER)) throw new ForbiddenException("Cổng đặt hàng dành cho tài khoản đại lý.");
        try(var conn=Db.getConnection()) {
            Long id=new PortalAccountDao().customerId(conn,viewer.getId());
            if(id==null) throw new ForbiddenException("Tài khoản chưa được gắn với đại lý. Vui lòng liên hệ quản trị.");
            return id;
        }
    }
    public long create(User actor) throws SQLException { return orders.create(customerId(actor),actor); }
    private Order own(long id,User viewer) throws SQLException {
        long customer=customerId(viewer); Order order=orders.get(id,viewer);
        if(order.getCustomerId()!=customer) throw new OrderService.NotFoundException();
        return order;
    }
    public List<CatalogItem> catalog(String query,int page,User viewer) throws SQLException {
        long id=customerId(viewer);
        if(page<1 || page>100000) throw new ServiceException("Trang không hợp lệ.");
        try(var conn=Db.getConnection()) {
            var customer=new CustomerDao().findById(conn,id);
            if(customer.getGroupId()==null) throw new ServiceException("Đại lý chưa được gắn nhóm bảng giá.");
            var found=new ProductDao().search(conn,query,null,Product.Status.ACTIVE,page,25,false).getItems();
            var result=new ArrayList<CatalogItem>();
            for(var product:found) {
                var price=new PriceListService().effectivePrice(conn,customer.getGroupId(),product.getId(),LocalDate.now());
                if(price==null) continue;
                var units=new ArrayList<Unit>(); units.add(new Unit(product.getBaseUnitId(),product.getBaseUnitName(),BigDecimal.ONE));
                for(var conversion:new ProductUnitDao().listByProducts(conn,List.of(product.getId())).getOrDefault(product.getId(),List.of())) units.add(new Unit(conversion.getUnitId(),conversion.getUnitName(),conversion.getFactor()));
                result.add(new CatalogItem(product.getId(),product.getSku(),product.getName(),price.getSalePrice(),units));
            }
            return result;
        }
    }
    public Cart cart(long id,User viewer) throws SQLException {
        var order=own(id,viewer);
        var lines=new ArrayList<Line>();
        for(var l:order.getLines()) lines.add(new Line(l.getId(),l.getProductId(),l.getSku(),l.getProductName(),l.getUnitId(),l.getUnitName(),l.getQuantity(),l.getUnitPrice(),l.getDiscountAmount(),l.getNetAmount()));
        var points=new ArrayList<Point>();
        for(var p:orders.deliveryPoints(order.getCustomerId())) points.add(new Point(p.getId(),p.getAddress(),p.getReceiverName(),p.getReceiverPhone()));
        return new Cart(id,order.getCode(),order.getStatus(),lines,order.getGrossTotal(),order.getDiscountTotal(),order.getPayable(),new OrderCreditService().quote(id,viewer),points,order.getDeliveryPointId(),order.getDeliveryDate(),order.getNote());
    }
    public void add(long id,long product,long unit,String quantity,User actor) throws SQLException { own(id,actor); orders.addLine(id,product,unit,quantity,actor); }
    public void update(long id,long line,long unit,String quantity,User actor) throws SQLException { own(id,actor); orders.updateLine(id,line,unit,quantity,actor); }
    public void remove(long id,long line,User actor) throws SQLException { own(id,actor); orders.deleteLine(id,line,actor); }
    public void header(long id,Long point,LocalDate date,String note,User actor) throws SQLException { own(id,actor); orders.saveHeader(id,point,date,note,actor); }
    public Order.Status submit(long id,User actor) throws SQLException { own(id,actor); return new OrderWorkflowService().submit(id,actor); }
}
