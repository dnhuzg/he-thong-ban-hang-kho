package com.oms.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Truy vấn trả hàng đại lý; mọi phương thức ghi được gọi trong transaction của service. */
public class SalesReturnDao {
    public record InvoiceOption(long id, String code, String customer, long warehouseId, String warehouse,
                                List<InvoiceLine> lines) { }
    public record InvoiceLine(long id, long productId, String sku, String product, BigDecimal delivered,
                              BigDecimal returned, BigDecimal remaining, BigDecimal unitPrice) { }
    public record ReturnLine(long invoiceLineId, long productId, String sku, String product, BigDecimal quantity,
                             BigDecimal unitPrice, BigDecimal amount, String reason, String condition) { }
    public record ReturnRow(long id, String code, long invoiceId, String invoiceCode, String customer,
                            long warehouseId, String warehouse, String status, LocalDateTime createdAt,
                            LocalDateTime receivedAt, List<ReturnLine> lines) { }
    public record LockedInvoice(long id, long customerId, long warehouseId, String code) { }
    public record LockedLine(long id, long productId, BigDecimal quantity, BigDecimal unitPrice) { }

    public List<InvoiceOption> eligibleInvoices(Connection conn) throws SQLException {
        Map<Long, InvoiceBuilder> map = new LinkedHashMap<>();
        String sql = "SELECT i.id,i.code,c.name,s.warehouse_id,w.name,il.id,il.product_id,p.sku,p.name,il.quantity,il.unit_price," +
                "COALESCE((SELECT SUM(rl.quantity) FROM sales_return_lines rl JOIN sales_returns r ON r.id=rl.return_id " +
                "WHERE rl.invoice_line_id=il.id AND r.status IN ('PENDING','RECEIVED')),0) returned_qty " +
                "FROM invoices i JOIN customers c ON c.id=i.customer_id JOIN invoice_lines il ON il.invoice_id=i.id " +
                "JOIN products p ON p.id=il.product_id JOIN shipments s ON s.id=il.shipment_id " +
                "JOIN warehouses w ON w.id=s.warehouse_id WHERE i.status<>'CANCELLED' ORDER BY i.id DESC,il.id";
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                long id = rs.getLong(1);
                InvoiceBuilder b = map.computeIfAbsent(id, ignored -> new InvoiceBuilder(id, rsString(rs,2),
                        rsString(rs,3), rsLong(rs,4), rsString(rs,5)));
                BigDecimal delivered = rs.getBigDecimal(10), returned = rs.getBigDecimal(12);
                BigDecimal remaining = delivered.subtract(returned).max(BigDecimal.ZERO);
                b.lines.add(new InvoiceLine(rs.getLong(6),rs.getLong(7),rs.getString(8),rs.getString(9),
                        delivered,returned,remaining,rs.getBigDecimal(11)));
            }
        }
        List<InvoiceOption> result = new ArrayList<>();
        map.values().forEach(b -> { if (b.lines.stream().anyMatch(l -> l.remaining.signum()>0)) result.add(b.build()); });
        return result;
    }

    public List<ReturnRow> recent(Connection conn, int limit) throws SQLException {
        List<ReturnRow> rows = new ArrayList<>();
        String heads = "SELECT r.id,r.code,r.invoice_id,i.code,c.name,r.warehouse_id,w.name,r.status,r.created_at,r.received_at " +
                "FROM sales_returns r JOIN invoices i ON i.id=r.invoice_id JOIN customers c ON c.id=r.customer_id " +
                "JOIN warehouses w ON w.id=r.warehouse_id ORDER BY r.id DESC LIMIT ?";
        try (PreparedStatement ps=conn.prepareStatement(heads)) { ps.setInt(1,limit); try(ResultSet rs=ps.executeQuery()) {
            while(rs.next()) rows.add(new ReturnRow(rs.getLong(1),rs.getString(2),rs.getLong(3),rs.getString(4),
                    rs.getString(5),rs.getLong(6),rs.getString(7),rs.getString(8),time(rs.getTimestamp(9)),
                    time(rs.getTimestamp(10)),new ArrayList<>())); }}
        for (ReturnRow row: rows) loadLines(conn,row.id(),row.lines());
        return rows;
    }

    private void loadLines(Connection conn,long returnId,List<ReturnLine> lines) throws SQLException {
        try(PreparedStatement ps=conn.prepareStatement("SELECT rl.invoice_line_id,rl.product_id,p.sku,p.name,rl.quantity,"+
                "rl.unit_price,rl.amount,rl.reason,rl.item_condition FROM sales_return_lines rl JOIN products p ON p.id=rl.product_id WHERE rl.return_id=? ORDER BY rl.id")){
            ps.setLong(1,returnId); try(ResultSet rs=ps.executeQuery()){ while(rs.next()) lines.add(new ReturnLine(
                    rs.getLong(1),rs.getLong(2),rs.getString(3),rs.getString(4),rs.getBigDecimal(5),rs.getBigDecimal(6),
                    rs.getBigDecimal(7),rs.getString(8),rs.getString(9))); }
        }
    }

    public LockedInvoice lockInvoice(Connection conn,long id) throws SQLException {
        try(PreparedStatement ps=conn.prepareStatement("SELECT i.id,i.customer_id,s.warehouse_id,i.code FROM invoices i " +
                "JOIN invoice_lines il ON il.invoice_id=i.id JOIN shipments s ON s.id=il.shipment_id WHERE i.id=? AND i.status<>'CANCELLED' LIMIT 1 FOR UPDATE")){
            ps.setLong(1,id); try(ResultSet rs=ps.executeQuery()){ return rs.next()?new LockedInvoice(rs.getLong(1),rs.getLong(2),rs.getLong(3),rs.getString(4)):null; }
        }
    }
    public LockedLine lockLine(Connection conn,long invoiceId,long lineId) throws SQLException {
        try(PreparedStatement ps=conn.prepareStatement("SELECT id,product_id,quantity,unit_price FROM invoice_lines WHERE id=? AND invoice_id=? FOR UPDATE")){
            ps.setLong(1,lineId);ps.setLong(2,invoiceId);try(ResultSet rs=ps.executeQuery()){return rs.next()?new LockedLine(rs.getLong(1),rs.getLong(2),rs.getBigDecimal(3),rs.getBigDecimal(4)):null;}
        }
    }
    public BigDecimal returnedQuantity(Connection conn,long lineId) throws SQLException {
        try(PreparedStatement ps=conn.prepareStatement("SELECT COALESCE(SUM(rl.quantity),0) FROM sales_return_lines rl JOIN sales_returns r ON r.id=rl.return_id WHERE rl.invoice_line_id=? AND r.status IN ('PENDING','RECEIVED')")){
            ps.setLong(1,lineId);try(ResultSet rs=ps.executeQuery()){rs.next();return rs.getBigDecimal(1);}
        }
    }
    public long nextNumber(Connection conn,String name) throws SQLException {
        long next; try(PreparedStatement ps=conn.prepareStatement("SELECT last_number FROM document_sequences WHERE name=? FOR UPDATE")){ps.setString(1,name);try(ResultSet rs=ps.executeQuery()){if(!rs.next())throw new SQLException("Thiếu dãy số "+name);next=rs.getLong(1)+1;}}
        try(PreparedStatement ps=conn.prepareStatement("UPDATE document_sequences SET last_number=? WHERE name=?")){ps.setLong(1,next);ps.setString(2,name);ps.executeUpdate();} return next;
    }
    public long insertReturn(Connection conn,String code,LockedInvoice invoice,long actorId) throws SQLException {
        try(PreparedStatement ps=conn.prepareStatement("INSERT INTO sales_returns(code,invoice_id,customer_id,warehouse_id,created_by) VALUES(?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){
            ps.setString(1,code);ps.setLong(2,invoice.id());ps.setLong(3,invoice.customerId());ps.setLong(4,invoice.warehouseId());ps.setLong(5,actorId);ps.executeUpdate();
            try(ResultSet rs=ps.getGeneratedKeys()){if(!rs.next())throw new SQLException("Không lấy được ID phiếu trả");return rs.getLong(1);}
        }
    }
    public void insertLine(Connection conn,long returnId,LockedLine line,BigDecimal quantity,String reason,String condition) throws SQLException {
        try(PreparedStatement ps=conn.prepareStatement("INSERT INTO sales_return_lines(return_id,invoice_line_id,product_id,quantity,unit_price,amount,reason,item_condition) VALUES(?,?,?,?,?,?,?,?)")){
            ps.setLong(1,returnId);ps.setLong(2,line.id());ps.setLong(3,line.productId());ps.setBigDecimal(4,quantity);ps.setBigDecimal(5,line.unitPrice());ps.setBigDecimal(6,line.unitPrice().multiply(quantity));ps.setString(7,reason);ps.setString(8,condition);ps.executeUpdate();
        }
    }
    public ReturnRow lockReturn(Connection conn,long id) throws SQLException {
        try(PreparedStatement ps=conn.prepareStatement("SELECT r.id,r.code,r.invoice_id,i.code,c.name,r.warehouse_id,w.name,r.status,r.created_at,r.received_at FROM sales_returns r JOIN invoices i ON i.id=r.invoice_id JOIN customers c ON c.id=r.customer_id JOIN warehouses w ON w.id=r.warehouse_id WHERE r.id=? FOR UPDATE")){
            ps.setLong(1,id);try(ResultSet rs=ps.executeQuery()){if(!rs.next())return null;ReturnRow row=new ReturnRow(rs.getLong(1),rs.getString(2),rs.getLong(3),rs.getString(4),rs.getString(5),rs.getLong(6),rs.getString(7),rs.getString(8),time(rs.getTimestamp(9)),time(rs.getTimestamp(10)),new ArrayList<>());loadLines(conn,id,row.lines());return row;}
        }
    }
    public void receive(Connection conn,ReturnRow row,long actorId) throws SQLException {
        for(ReturnLine line:row.lines()){
            if("GOOD".equals(line.condition())) upsert(conn,"warehouse_inventory","on_hand",row.warehouseId(),line.productId(),line.quantity());
            else upsert(conn,"warehouse_damaged_inventory","on_hand",row.warehouseId(),line.productId(),line.quantity());
            try(PreparedStatement ps=conn.prepareStatement("INSERT INTO inventory_movements(warehouse_id,product_id,unit_id,quantity,movement_type,reference_type,reference_id,created_by) SELECT ?,p.id,p.base_unit_id,?,'CUSTOMER_RETURN','SALES_RETURN',?,? FROM products p WHERE p.id=?")){
                ps.setLong(1,row.warehouseId());ps.setBigDecimal(2,line.quantity());ps.setLong(3,row.id());ps.setLong(4,actorId);ps.setLong(5,line.productId());ps.executeUpdate();
            }
        }
        try(PreparedStatement ps=conn.prepareStatement("UPDATE sales_returns SET status='RECEIVED',received_by=?,received_at=CURRENT_TIMESTAMP WHERE id=? AND status='PENDING'")){ps.setLong(1,actorId);ps.setLong(2,row.id());if(ps.executeUpdate()!=1)throw new SQLException("Phiếu trả đã thay đổi trạng thái");}
    }
    private static void upsert(Connection conn,String table,String column,long warehouseId,long productId,BigDecimal qty) throws SQLException {
        String sql="INSERT INTO "+table+"(warehouse_id,product_id,"+column+") VALUES(?,?,?) ON DUPLICATE KEY UPDATE "+column+"="+column+"+VALUES("+column+")";
        try(PreparedStatement ps=conn.prepareStatement(sql)){ps.setLong(1,warehouseId);ps.setLong(2,productId);ps.setBigDecimal(3,qty);ps.executeUpdate();}
    }
    private static LocalDateTime time(Timestamp value){return value==null?null:value.toLocalDateTime();}
    private static String rsString(ResultSet rs,int i){try{return rs.getString(i);}catch(SQLException e){throw new IllegalStateException(e);}}
    private static long rsLong(ResultSet rs,int i){try{return rs.getLong(i);}catch(SQLException e){throw new IllegalStateException(e);}}
    private static final class InvoiceBuilder {final long id,warehouseId;final String code,customer,warehouse;final List<InvoiceLine> lines=new ArrayList<>();InvoiceBuilder(long id,String code,String customer,long warehouseId,String warehouse){this.id=id;this.code=code;this.customer=customer;this.warehouseId=warehouseId;this.warehouse=warehouse;}InvoiceOption build(){return new InvoiceOption(id,code,customer,warehouseId,warehouse,lines);}}
}
