package com.oms.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/** Số lượng đã xuất / đã giao của một đơn, cộng từ phiếu xuất gắn với đơn (S5-01, S6-09). */
public class OrderTrackingDao {

    /**
     * Mốc thời gian giao hàng của đơn: lần đầu đạt APPROVED / PICKING / SHIPPED theo lịch sử trạng thái;
     * IN_TRANSIT là lúc lập phiếu xuất đầu tiên (hàng rời kho); DELIVERED là lúc phiếu cuối cùng giao xong
     * khi mọi phiếu đã giao thành công.
     */
    public Map<String, LocalDateTime> milestones(Connection conn, long orderId) throws SQLException {
        Map<String, LocalDateTime> map = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT to_status, MIN(created_at) FROM order_status_history WHERE order_id = ? "
                        + "AND to_status IN ('APPROVED', 'PICKING', 'SHIPPED') GROUP BY to_status")) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getString(1), rs.getTimestamp(2).toLocalDateTime());
                }
            }
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT MIN(created_at), MAX(delivered_at), COUNT(*), "
                        + "SUM(CASE WHEN status = 'DELIVERED' THEN 1 ELSE 0 END) FROM shipments WHERE order_id = ?")) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getLong(3) > 0) {
                    Timestamp out = rs.getTimestamp(1);
                    if (out != null) {
                        map.put("IN_TRANSIT", out.toLocalDateTime());
                    }
                    Timestamp delivered = rs.getTimestamp(2);
                    if (delivered != null && rs.getLong(3) == rs.getLong(4)) {
                        map.put("DELIVERED", delivered.toLocalDateTime());
                    }
                }
            }
        }
        return map;
    }

    /** Theo sản phẩm, đơn vị cơ sở: [0] đã xuất (phiếu chưa giao thất bại), [1] đã giao thành công. */
    public Map<Long, BigDecimal[]> shipped(Connection conn, long orderId) throws SQLException {
        Map<Long, BigDecimal[]> map = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT i.product_id, "
                        + "SUM(CASE WHEN s.status <> 'FAILED_RETURNED' THEN i.quantity * f.factor ELSE 0 END), "
                        + "SUM(CASE WHEN s.status = 'DELIVERED' THEN i.quantity * f.factor ELSE 0 END) "
                        + "FROM shipment_items i JOIN shipments s ON s.id = i.shipment_id "
                        + "JOIN (SELECT p.id AS product_id, p.base_unit_id AS unit_id, 1 AS factor FROM products p "
                        + "UNION ALL SELECT pu.product_id, pu.unit_id, pu.factor FROM product_units pu "
                        + "JOIN products p ON p.id = pu.product_id WHERE pu.unit_id <> p.base_unit_id) f "
                        + "ON f.product_id = i.product_id AND f.unit_id = i.unit_id "
                        + "WHERE s.order_id = ? GROUP BY i.product_id")) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getLong(1), new BigDecimal[] {rs.getBigDecimal(2), rs.getBigDecimal(3)});
                }
            }
        }
        return map;
    }
}
