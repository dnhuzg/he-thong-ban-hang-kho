package com.oms.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Các màn báo cáo tài chính chỉ đọc, tổng hợp trực tiếp từ hóa đơn, thu tiền và giảm trừ. */
public class FinanceOverviewDao {
    public record CreditAlert(long id,String code,String name,BigDecimal limit,int allowedDays,BigDecimal balance,int oldestDays) { }
    public record CustomerOption(long id,String code,String name) { }
    public record LedgerEntry(long customerId,LocalDate date,String reference,String type,String note,BigDecimal increase,BigDecimal decrease) { }
    public record LedgerData(List<CustomerOption> customers,List<LedgerEntry> entries) { }

    public List<CreditAlert> creditAlerts(Connection conn) throws SQLException {
        List<CreditAlert> list=new ArrayList<>();String sql="SELECT c.id,c.code,c.name,c.credit_limit,c.credit_days,"+
                "COALESCE(SUM(r.outstanding),0),COALESCE(MAX(CASE WHEN r.outstanding>0 THEN DATEDIFF(CURRENT_DATE,r.issued_on) ELSE 0 END),0) " +
                "FROM customers c LEFT JOIN customer_receivables r ON r.customer_id=c.id WHERE c.status='ACTIVE' GROUP BY c.id ORDER BY c.code";
        try(PreparedStatement ps=conn.prepareStatement(sql);ResultSet rs=ps.executeQuery()){while(rs.next())list.add(new CreditAlert(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getBigDecimal(4),rs.getInt(5),rs.getBigDecimal(6),rs.getInt(7)));}return list;
    }
    public LedgerData ledger(Connection conn) throws SQLException {
        List<CustomerOption> customers=new ArrayList<>();try(PreparedStatement ps=conn.prepareStatement("SELECT id,code,name FROM customers ORDER BY code");ResultSet rs=ps.executeQuery()){while(rs.next())customers.add(new CustomerOption(rs.getLong(1),rs.getString(2),rs.getString(3)));}
        List<LedgerEntry> entries=new ArrayList<>();String sql="SELECT customer_id,entry_date,reference,type,note,increase_amount,decrease_amount FROM ("+
                "SELECT i.customer_id,DATE(i.issued_at) entry_date,i.code reference,'Bán hàng' type,'Hóa đơn bán hàng' note,i.total_amount increase_amount,0 decrease_amount FROM invoices i WHERE i.status<>'CANCELLED' " +
                "UNION ALL SELECT p.customer_id,p.paid_on,p.code,'Thu tiền',CONCAT('Thanh toán · ',CASE p.method WHEN 'CASH' THEN 'tiền mặt' WHEN 'BANK_TRANSFER' THEN 'chuyển khoản' ELSE 'khác' END),0,p.amount FROM payments p " +
                "UNION ALL SELECT n.customer_id,DATE(n.created_at),n.code,'Giảm trừ trả hàng',CONCAT('Tham chiếu phiếu trả ',r.code),0,n.amount FROM return_credit_notes n JOIN sales_returns r ON r.id=n.return_id"+
                ") x ORDER BY customer_id,entry_date,reference";
        try(PreparedStatement ps=conn.prepareStatement(sql);ResultSet rs=ps.executeQuery()){while(rs.next())entries.add(new LedgerEntry(rs.getLong(1),rs.getDate(2).toLocalDate(),rs.getString(3),rs.getString(4),rs.getString(5),rs.getBigDecimal(6),rs.getBigDecimal(7)));}return new LedgerData(customers,entries);
    }
}
