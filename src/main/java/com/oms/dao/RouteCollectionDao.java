package com.oms.dao;
import java.math.BigDecimal;import java.sql.*;import java.time.LocalDate;import java.util.*;
public class RouteCollectionDao {
 public record Customer(long id,String code,String name,BigDecimal outstanding){} public record Row(long id,LocalDate date,String employee,String customer,BigDecimal amount,String status,String receiptPath){} public record PageData(List<Customer> customers,List<Row> records){}
 public PageData page(Connection c,Long salesRepId)throws SQLException{
  List<Customer> customers=new ArrayList<>();String customerSql="SELECT c.id,c.code,c.name,COALESCE(SUM(r.outstanding),0) FROM customers c JOIN customer_receivables r ON r.customer_id=c.id WHERE r.outstanding>0"+(salesRepId==null?"":" AND c.sales_rep_id=?")+" GROUP BY c.id ORDER BY c.code";
  try(var p=c.prepareStatement(customerSql)){if(salesRepId!=null)p.setLong(1,salesRepId);try(var r=p.executeQuery()){while(r.next())customers.add(new Customer(r.getLong(1),r.getString(2),r.getString(3),r.getBigDecimal(4)));}}
  List<Row> records=new ArrayList<>();String rowSql="SELECT rc.id,p.paid_on,u.full_name,c.name,p.amount,rc.status,rc.receipt_path FROM route_collections rc JOIN payments p ON p.id=rc.payment_id JOIN users u ON u.id=p.collected_by JOIN customers c ON c.id=p.customer_id"+(salesRepId==null?"":" WHERE p.collected_by=?")+" ORDER BY rc.id DESC";
  try(var p=c.prepareStatement(rowSql)){if(salesRepId!=null)p.setLong(1,salesRepId);try(var r=p.executeQuery()){while(r.next())records.add(new Row(r.getLong(1),r.getDate(2).toLocalDate(),r.getString(3),r.getString(4),r.getBigDecimal(5),r.getString(6),r.getString(7)));}}
  return new PageData(customers,records);
 }
 public long insert(Connection c,long payment,String path)throws SQLException{try(var p=c.prepareStatement("INSERT INTO route_collections(payment_id,receipt_path) VALUES(?,?)",Statement.RETURN_GENERATED_KEYS)){p.setLong(1,payment);p.setString(2,path);p.executeUpdate();try(var r=p.getGeneratedKeys()){r.next();return r.getLong(1);}}}
 public void confirm(Connection c,long id,long actor)throws SQLException{try(var p=c.prepareStatement("UPDATE route_collections SET status='CONFIRMED',confirmed_by=?,confirmed_at=CURRENT_TIMESTAMP WHERE id=? AND status='PENDING'")){p.setLong(1,actor);p.setLong(2,id);if(p.executeUpdate()!=1)throw new IllegalStateException("Khoản thu đã được xác nhận hoặc không tồn tại.");}}
}
