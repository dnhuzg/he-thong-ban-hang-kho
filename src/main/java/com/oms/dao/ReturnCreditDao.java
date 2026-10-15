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
import java.util.List;

/** Chứng từ giảm công nợ từ phiếu trả đã nhận. */
public class ReturnCreditDao {
    public record Line(String sku,String product,BigDecimal quantity,BigDecimal unitPrice,BigDecimal amount) { }
    public record Eligible(long returnId,String returnCode,long invoiceId,String invoiceCode,long customerId,
                           String customer,BigDecimal debt,BigDecimal creditLimit,LocalDateTime receivedAt,
                           BigDecimal amount,List<Line> lines) { }
    public record Document(long id,String code,String returnCode,String invoiceCode,String customer,
                           BigDecimal amount,LocalDateTime createdAt) { }

    public List<Eligible> eligible(Connection conn) throws SQLException {
        List<Eligible> list=new ArrayList<>();
        String sql="SELECT r.id,r.code,i.id,i.code,c.id,c.name,COALESCE(SUM(cr.outstanding),0),c.credit_limit,r.received_at,"+
                "(SELECT SUM(amount) FROM sales_return_lines WHERE return_id=r.id) amount FROM sales_returns r " +
                "JOIN invoices i ON i.id=r.invoice_id JOIN customers c ON c.id=r.customer_id " +
                "LEFT JOIN customer_receivables cr ON cr.customer_id=c.id WHERE r.status='RECEIVED' " +
                "AND NOT EXISTS(SELECT 1 FROM return_credit_notes n WHERE n.return_id=r.id) GROUP BY r.id ORDER BY r.received_at";
        try(PreparedStatement ps=conn.prepareStatement(sql);ResultSet rs=ps.executeQuery()){while(rs.next()){
            List<Line> lines=lines(conn,rs.getLong(1));list.add(new Eligible(rs.getLong(1),rs.getString(2),rs.getLong(3),rs.getString(4),rs.getLong(5),rs.getString(6),rs.getBigDecimal(7),rs.getBigDecimal(8),time(rs.getTimestamp(9)),rs.getBigDecimal(10),lines));}}
        return list;
    }
    public List<Document> recent(Connection conn,int limit) throws SQLException {
        List<Document> list=new ArrayList<>();try(PreparedStatement ps=conn.prepareStatement("SELECT n.id,n.code,r.code,i.code,c.name,n.amount,n.created_at FROM return_credit_notes n JOIN sales_returns r ON r.id=n.return_id JOIN invoices i ON i.id=n.invoice_id JOIN customers c ON c.id=n.customer_id ORDER BY n.id DESC LIMIT ?")){ps.setInt(1,limit);try(ResultSet rs=ps.executeQuery()){while(rs.next())list.add(new Document(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getBigDecimal(6),time(rs.getTimestamp(7))));}}return list;
    }
    public Eligible lockEligible(Connection conn,long returnId) throws SQLException {
        String sql="SELECT r.id,r.code,i.id,i.code,c.id,c.name,COALESCE((SELECT SUM(outstanding) FROM customer_receivables WHERE customer_id=c.id),0),c.credit_limit,r.received_at,(SELECT SUM(amount) FROM sales_return_lines WHERE return_id=r.id) FROM sales_returns r JOIN invoices i ON i.id=r.invoice_id JOIN customers c ON c.id=r.customer_id WHERE r.id=? AND r.status='RECEIVED' AND NOT EXISTS(SELECT 1 FROM return_credit_notes n WHERE n.return_id=r.id) FOR UPDATE";
        try(PreparedStatement ps=conn.prepareStatement(sql)){ps.setLong(1,returnId);try(ResultSet rs=ps.executeQuery()){return rs.next()?new Eligible(rs.getLong(1),rs.getString(2),rs.getLong(3),rs.getString(4),rs.getLong(5),rs.getString(6),rs.getBigDecimal(7),rs.getBigDecimal(8),time(rs.getTimestamp(9)),rs.getBigDecimal(10),lines(conn,returnId)):null;}}
    }
    private List<Line> lines(Connection conn,long id) throws SQLException {List<Line> list=new ArrayList<>();try(PreparedStatement ps=conn.prepareStatement("SELECT p.sku,p.name,l.quantity,l.unit_price,l.amount FROM sales_return_lines l JOIN products p ON p.id=l.product_id WHERE l.return_id=? ORDER BY l.id")){ps.setLong(1,id);try(ResultSet rs=ps.executeQuery()){while(rs.next())list.add(new Line(rs.getString(1),rs.getString(2),rs.getBigDecimal(3),rs.getBigDecimal(4),rs.getBigDecimal(5)));}}return list;}
    public long nextNumber(Connection conn) throws SQLException {long n;try(PreparedStatement ps=conn.prepareStatement("SELECT last_number FROM document_sequences WHERE name='RETURN_CREDIT' FOR UPDATE");ResultSet rs=ps.executeQuery()){if(!rs.next())throw new SQLException("Thiếu dãy số RETURN_CREDIT");n=rs.getLong(1)+1;}try(PreparedStatement ps=conn.prepareStatement("UPDATE document_sequences SET last_number=? WHERE name='RETURN_CREDIT'")){ps.setLong(1,n);ps.executeUpdate();}return n;}
    public long insert(Connection conn,String code,Eligible item,long actorId) throws SQLException {try(PreparedStatement ps=conn.prepareStatement("INSERT INTO return_credit_notes(code,return_id,invoice_id,customer_id,amount,created_by) VALUES(?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){ps.setString(1,code);ps.setLong(2,item.returnId());ps.setLong(3,item.invoiceId());ps.setLong(4,item.customerId());ps.setBigDecimal(5,item.amount());ps.setLong(6,actorId);ps.executeUpdate();try(ResultSet rs=ps.getGeneratedKeys()){if(!rs.next())throw new SQLException("Không lấy được ID chứng từ");return rs.getLong(1);}}
    }
    public void reduceDebt(Connection conn,long customerId,BigDecimal amount) throws SQLException {
        BigDecimal left=amount;try(PreparedStatement select=conn.prepareStatement("SELECT id,outstanding FROM customer_receivables WHERE customer_id=? AND outstanding>0 ORDER BY issued_on,id FOR UPDATE")){select.setLong(1,customerId);try(ResultSet rs=select.executeQuery()){while(rs.next()&&left.signum()>0){BigDecimal current=rs.getBigDecimal(2),used=current.min(left);try(PreparedStatement update=conn.prepareStatement("UPDATE customer_receivables SET outstanding=outstanding-? WHERE id=?")){update.setBigDecimal(1,used);update.setLong(2,rs.getLong(1));update.executeUpdate();}left=left.subtract(used);}}}if(left.signum()>0)throw new IllegalStateException("Giá trị giảm vượt công nợ hiện tại.");
    }
    public void applyToInvoice(Connection conn,long invoiceId,BigDecimal amount) throws SQLException {
        try(PreparedStatement ps=conn.prepareStatement("UPDATE invoices SET credited_amount=credited_amount+?, status=CASE WHEN paid_amount+credited_amount+?>=total_amount THEN 'PAID' WHEN paid_amount+credited_amount+?>0 THEN 'PARTIAL' ELSE status END WHERE id=? AND paid_amount+credited_amount+?<=total_amount")){
            ps.setBigDecimal(1,amount);ps.setBigDecimal(2,amount);ps.setBigDecimal(3,amount);ps.setLong(4,invoiceId);ps.setBigDecimal(5,amount);if(ps.executeUpdate()!=1)throw new SQLException("Giá trị giảm vượt số còn nợ của hóa đơn.");
        }
    }
    private static LocalDateTime time(Timestamp value){return value==null?null:value.toLocalDateTime();}
}
