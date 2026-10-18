package com.oms.dao;
import com.oms.model.Order;
import com.oms.model.OrderLine;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Lịch sử chỉ có INSERT/SELECT; không cung cấp đường sửa hoặc xoá. */
public class OrderWorkflowDao {
    public record Event(long id,String fromStatus,String toStatus,String action,String opinion,long actorId,String actorName,LocalDateTime at) {
        // Getter kiểu bean để JSP đọc được (EL của Tomcat 10.1 chưa hỗ trợ accessor của record).
        public String getActionLabel() {
            return switch(action) {
                case "SUBMIT" -> "Chốt đơn";
                case "APPROVE" -> "Duyệt";
                case "REJECT" -> "Từ chối";
                case "RETURN" -> "Trả lại sửa";
                case "CANCEL" -> "Huỷ đơn";
                default -> "Chuyển bước";
            };
        }
        public String getToLabel() { return Order.Status.valueOf(toStatus).getLabel(); }
        public String getOpinion() { return opinion; }
        public String getActorName() { return actorName; }
        public String getAtText() { return at.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")); }
    }
    public void snapshot(Connection conn,Order order) throws SQLException {
        for(OrderLine line:order.getLines()) {
            try(var ps=conn.prepareStatement("UPDATE order_lines SET unit_price=?,gross_amount=?,discount_amount=?,discount_note=? WHERE id=? AND order_id=?")) {
                ps.setBigDecimal(1,line.getUnitPrice()); ps.setBigDecimal(2,line.getGrossAmount());
                ps.setBigDecimal(3,line.getDiscountAmount()); ps.setString(4,line.getDiscountNote());
                ps.setLong(5,line.getId()); ps.setLong(6,order.getId()); ps.executeUpdate();
            }
        }
    }
    public void transition(Connection conn,Order order,Order.Status next,String action,String opinion,long actorId,Long warehouse,String reason) throws SQLException {
        try(var ps=conn.prepareStatement("UPDATE orders SET status=?,warehouse_id=?,approval_reason=?,updated_at=CURRENT_TIMESTAMP WHERE id=? AND status=?")) {
            ps.setString(1,next.name()); ps.setObject(2,warehouse); ps.setString(3,reason); ps.setLong(4,order.getId()); ps.setString(5,order.getStatus().name());
            if(ps.executeUpdate()!=1) throw new com.oms.service.ServiceException("Trạng thái đơn đã thay đổi. Vui lòng tải lại.");
        }
        try(var ps=conn.prepareStatement("INSERT INTO order_status_history(order_id,from_status,to_status,action,opinion,actor_id) VALUES(?,?,?,?,?,?)")) {
            ps.setLong(1,order.getId()); ps.setString(2,order.getStatus().name()); ps.setString(3,next.name());
            ps.setString(4,action); ps.setString(5,opinion); ps.setLong(6,actorId); ps.executeUpdate();
        }
        new AuditLogDao().log(conn,actorId,"ORDER_"+action,"ORDER",order.getId(),opinion,order.getStatus().name(),next.name());
    }
    public boolean hasHistory(Connection conn,long orderId) throws SQLException {
        try(var ps=conn.prepareStatement("SELECT 1 FROM order_status_history WHERE order_id=? LIMIT 1")) {
            ps.setLong(1,orderId); try(var rs=ps.executeQuery()) { return rs.next(); }
        }
    }
    public List<Event> history(Connection conn,long orderId) throws SQLException {
        var result=new ArrayList<Event>();
        try(var ps=conn.prepareStatement("SELECT h.id,h.from_status,h.to_status,h.action,h.opinion,h.actor_id,u.full_name,h.created_at FROM order_status_history h LEFT JOIN users u ON u.id=h.actor_id WHERE h.order_id=? ORDER BY h.id")) {
            ps.setLong(1,orderId);
            try(var rs=ps.executeQuery()) { while(rs.next()) result.add(new Event(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getLong(6),rs.getString(7),rs.getTimestamp(8).toLocalDateTime())); }
        }
        return result;
    }
    /** Đơn còn Đã duyệt mà lần duyệt cuối trước mốc; mỗi dòng là [mã đơn, người duyệt]. */
    public List<long[]> approvedBefore(Connection conn,LocalDateTime cutoff) throws SQLException {
        var result=new ArrayList<long[]>();
        try(var ps=conn.prepareStatement("SELECT o.id,h.actor_id FROM orders o JOIN order_status_history h ON h.order_id=o.id"
                +" AND h.id=(SELECT MAX(x.id) FROM order_status_history x WHERE x.order_id=o.id AND x.to_status='APPROVED')"
                +" WHERE o.status='APPROVED' AND h.created_at<? ORDER BY o.id LIMIT 200")) {
            ps.setTimestamp(1,java.sql.Timestamp.valueOf(cutoff));
            try(var rs=ps.executeQuery()) { while(rs.next()) result.add(new long[] {rs.getLong(1),rs.getLong(2)}); }
        }
        return result;
    }
    public List<Long> pending(Connection conn) throws SQLException {
        var result=new ArrayList<Long>();
        try(var ps=conn.prepareStatement("SELECT id FROM orders WHERE status='PENDING_APPROVAL' ORDER BY created_at,id LIMIT 200");var rs=ps.executeQuery()) {
            while(rs.next()) result.add(rs.getLong(1));
        }
        return result;
    }
}
