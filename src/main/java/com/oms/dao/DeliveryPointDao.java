package com.oms.dao;

import com.oms.model.DeliveryPoint;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DeliveryPointDao {

    private static final String COLUMNS = "id, customer_id, address, receiver_name, receiver_phone, route_note, is_default";

    /** Điểm mặc định đứng đầu, sau đó theo thứ tự tạo. */
    public List<DeliveryPoint> listByCustomer(Connection conn, long customerId) throws SQLException {
        List<DeliveryPoint> points = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT " + COLUMNS
                + " FROM customer_delivery_points WHERE customer_id = ? ORDER BY is_default DESC, id")) {
            ps.setLong(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    points.add(map(rs));
                }
            }
        }
        return points;
    }

    public DeliveryPoint findById(Connection conn, long id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT " + COLUMNS + " FROM customer_delivery_points WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public long insert(Connection conn, DeliveryPoint p) throws SQLException {
        String sql = "INSERT INTO customer_delivery_points (customer_id, address, receiver_name, receiver_phone, route_note, is_default) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, p.getCustomerId());
            ps.setString(2, p.getAddress());
            ps.setString(3, p.getReceiverName());
            ps.setString(4, p.getReceiverPhone());
            ps.setString(5, p.getRouteNote());
            ps.setBoolean(6, p.isDefaultPoint());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public void update(Connection conn, DeliveryPoint p) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE customer_delivery_points SET address = ?, receiver_name = ?, "
                + "receiver_phone = ?, route_note = ? WHERE id = ?")) {
            ps.setString(1, p.getAddress());
            ps.setString(2, p.getReceiverName());
            ps.setString(3, p.getReceiverPhone());
            ps.setString(4, p.getRouteNote());
            ps.setLong(5, p.getId());
            ps.executeUpdate();
        }
    }

    public void delete(Connection conn, long id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM customer_delivery_points WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    /** Đặt một điểm làm mặc định và bỏ cờ mặc định của các điểm còn lại cùng đại lý. */
    public void makeDefault(Connection conn, long customerId, long pointId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE customer_delivery_points SET is_default = (id = ?) WHERE customer_id = ?")) {
            ps.setLong(1, pointId);
            ps.setLong(2, customerId);
            ps.executeUpdate();
        }
    }

    public long count(Connection conn, long customerId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM customer_delivery_points WHERE customer_id = ?")) {
            ps.setLong(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    /** Điểm cũ nhất của đại lý, dùng chọn lại mặc định sau khi xoá điểm mặc định; 0 nếu không còn điểm nào. */
    public long firstId(Connection conn, long customerId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id FROM customer_delivery_points WHERE customer_id = ? ORDER BY id LIMIT 1")) {
            ps.setLong(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        }
    }

    private static DeliveryPoint map(ResultSet rs) throws SQLException {
        DeliveryPoint p = new DeliveryPoint();
        p.setId(rs.getLong("id"));
        p.setCustomerId(rs.getLong("customer_id"));
        p.setAddress(rs.getString("address"));
        p.setReceiverName(rs.getString("receiver_name"));
        p.setReceiverPhone(rs.getString("receiver_phone"));
        p.setRouteNote(rs.getString("route_note"));
        p.setDefaultPoint(rs.getBoolean("is_default"));
        return p;
    }
}
