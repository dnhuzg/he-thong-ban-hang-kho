package com.oms.dao;

import com.oms.model.Order;
import com.oms.model.OrderLine;
import com.oms.model.OrderStats;
import com.oms.model.PageResult;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class OrderDao {

    private static final String SELECT = "SELECT o.id, o.code, o.customer_id, c.code, c.name, c.group_id, g.name, c.status, "
            + "c.lock_reason, c.sales_rep_id, o.delivery_point_id, CONCAT(dp.receiver_name, ' – ', dp.address), "
            + "o.delivery_date, o.note, u.full_name, o.created_at, o.updated_at, "
            + "(SELECT COUNT(*) FROM order_lines l WHERE l.order_id = o.id) "
            + "FROM orders o JOIN customers c ON c.id = o.customer_id LEFT JOIN customer_groups g ON g.id = c.group_id "
            + "LEFT JOIN customer_delivery_points dp ON dp.id = o.delivery_point_id JOIN users u ON u.id = o.created_by";

    public long insert(Connection conn, long customerId, Long deliveryPointId, long createdBy) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO orders (customer_id, delivery_point_id, created_by) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, customerId);
            setLong(ps, 2, deliveryPointId);
            ps.setLong(3, createdBy);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public void setCode(Connection conn, long id, String code) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE orders SET code = ? WHERE id = ?")) {
            ps.setString(1, code);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    public Order findById(Connection conn, long id) throws SQLException {
        return findOne(conn, SELECT + " WHERE o.id = ?", id);
    }

    public Order findByIdForUpdate(Connection conn, long id) throws SQLException {
        return findOne(conn, SELECT + " WHERE o.id = ? FOR UPDATE", id);
    }

    /** Đếm đơn cho trang danh sách; ownRepId khác null thì chỉ tính đơn của đại lý người đó phụ trách. */
    public OrderStats stats(Connection conn, Long ownRepId, LocalDate today) throws SQLException {
        String sql = "SELECT COUNT(*), "
                + "COALESCE(SUM(CASE WHEN o.created_at >= ? THEN 1 ELSE 0 END), 0), "
                + "COUNT(DISTINCT o.customer_id), "
                + "COALESCE(SUM(CASE WHEN o.delivery_date BETWEEN ? AND ? THEN 1 ELSE 0 END), 0), "
                + "COALESCE(SUM(CASE WHEN NOT EXISTS (SELECT 1 FROM order_lines l WHERE l.order_id = o.id) THEN 1 ELSE 0 END), 0) "
                + "FROM orders o JOIN customers c ON c.id = o.customer_id" + (ownRepId == null ? "" : " WHERE c.sales_rep_id = ?");
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(today.withDayOfMonth(1)));
            ps.setDate(2, Date.valueOf(today));
            ps.setDate(3, Date.valueOf(today.plusDays(7)));
            if (ownRepId != null) {
                ps.setLong(4, ownRepId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                OrderStats s = new OrderStats();
                s.setTotal(rs.getLong(1));
                s.setThisMonth(rs.getLong(2));
                s.setCustomers(rs.getLong(3));
                s.setDueSoon(rs.getLong(4));
                s.setWithoutLines(rs.getLong(5));
                return s;
            }
        }
    }

    /** Mới cập nhật trước; ownRepId chỉ dùng cho người chỉ thấy đơn của đại lý mình phụ trách. */
    public PageResult<Order> search(Connection conn, String keyword, Long ownRepId, int page, int pageSize) throws SQLException {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (o.code LIKE ? OR c.code LIKE ? OR c.name LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (ownRepId != null) {
            where.append(" AND c.sales_rep_id = ?");
            params.add(ownRepId);
        }
        long total;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM orders o JOIN customers c ON c.id = o.customer_id" + where)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                total = rs.getLong(1);
            }
        }
        List<Order> items = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT + where + " ORDER BY o.updated_at DESC, o.id DESC LIMIT ? OFFSET ?")) {
            bind(ps, params);
            ps.setInt(params.size() + 1, pageSize);
            ps.setInt(params.size() + 2, (page - 1) * pageSize);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(map(rs));
                }
            }
        }
        return new PageResult<>(items, page, pageSize, total);
    }

    public void updateHeader(Connection conn, long id, Long deliveryPointId, LocalDate deliveryDate, String note) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE orders SET delivery_point_id = ?, delivery_date = ?, note = ? WHERE id = ?")) {
            setLong(ps, 1, deliveryPointId);
            ps.setDate(2, deliveryDate == null ? null : Date.valueOf(deliveryDate));
            ps.setString(3, note);
            ps.setLong(4, id);
            ps.executeUpdate();
        }
    }

    /** Thêm / sửa / xoá dòng hàng cũng làm đơn "mới cập nhật" để lên đầu danh sách. */
    public void touch(Connection conn, long id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE orders SET updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    public void delete(Connection conn, long id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM orders WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    public List<OrderLine> lines(Connection conn, long orderId) throws SQLException {
        return queryLines(conn, LINES + " WHERE l.order_id = ? ORDER BY l.id", orderId);
    }

    public OrderLine findLine(Connection conn, long orderId, long lineId) throws SQLException {
        List<OrderLine> found = queryLines(conn, LINES + " WHERE l.order_id = ? AND l.id = ?", orderId, lineId);
        return found.isEmpty() ? null : found.get(0);
    }

    public OrderLine findLine(Connection conn, long orderId, long productId, long unitId) throws SQLException {
        List<OrderLine> found = queryLines(conn,
                LINES + " WHERE l.order_id = ? AND l.product_id = ? AND l.unit_id = ?", orderId, productId, unitId);
        return found.isEmpty() ? null : found.get(0);
    }

    public void insertLine(Connection conn, long orderId, long productId, long unitId, int quantity,
                           BigDecimal factor, BigDecimal baseQuantity) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO order_lines "
                + "(order_id, product_id, unit_id, quantity, factor, base_quantity) VALUES (?, ?, ?, ?, ?, ?)")) {
            ps.setLong(1, orderId);
            ps.setLong(2, productId);
            ps.setLong(3, unitId);
            ps.setInt(4, quantity);
            ps.setBigDecimal(5, factor);
            ps.setBigDecimal(6, baseQuantity);
            ps.executeUpdate();
        }
    }

    public void updateLine(Connection conn, long lineId, long unitId, int quantity, BigDecimal factor, BigDecimal baseQuantity)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE order_lines SET unit_id = ?, quantity = ?, factor = ?, base_quantity = ? WHERE id = ?")) {
            ps.setLong(1, unitId);
            ps.setInt(2, quantity);
            ps.setBigDecimal(3, factor);
            ps.setBigDecimal(4, baseQuantity);
            ps.setLong(5, lineId);
            ps.executeUpdate();
        }
    }

    public void deleteLine(Connection conn, long lineId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM order_lines WHERE id = ?")) {
            ps.setLong(1, lineId);
            ps.executeUpdate();
        }
    }

    private static final String LINES = "SELECT l.id, l.order_id, l.product_id, p.sku, p.name, l.unit_id, u.name, bu.name, "
            + "l.quantity, l.factor, l.base_quantity FROM order_lines l JOIN products p ON p.id = l.product_id "
            + "JOIN units u ON u.id = l.unit_id JOIN units bu ON bu.id = p.base_unit_id";

    private List<OrderLine> queryLines(Connection conn, String sql, long... params) throws SQLException {
        List<OrderLine> lines = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setLong(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OrderLine l = new OrderLine();
                    l.setId(rs.getLong(1));
                    l.setOrderId(rs.getLong(2));
                    l.setProductId(rs.getLong(3));
                    l.setSku(rs.getString(4));
                    l.setProductName(rs.getString(5));
                    l.setUnitId(rs.getLong(6));
                    l.setUnitName(rs.getString(7));
                    l.setBaseUnitName(rs.getString(8));
                    l.setQuantity(rs.getInt(9));
                    l.setFactor(rs.getBigDecimal(10));
                    l.setBaseQuantity(rs.getBigDecimal(11));
                    lines.add(l);
                }
            }
        }
        return lines;
    }

    private Order findOne(Connection conn, String sql, long id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    private static Order map(ResultSet rs) throws SQLException {
        Order o = new Order();
        o.setId(rs.getLong(1));
        o.setCode(rs.getString(2));
        o.setCustomerId(rs.getLong(3));
        o.setCustomerCode(rs.getString(4));
        o.setCustomerName(rs.getString(5));
        o.setCustomerGroupId(rs.getLong(6));
        o.setCustomerGroupName(rs.getString(7));
        o.setCustomerLocked("LOCKED".equals(rs.getString(8)));
        o.setCustomerLockReason(rs.getString(9));
        long rep = rs.getLong(10);
        o.setCustomerSalesRepId(rs.wasNull() ? null : rep);
        long point = rs.getLong(11);
        o.setDeliveryPointId(rs.wasNull() ? null : point);
        o.setDeliveryPointText(rs.getString(12));
        Date date = rs.getDate(13);
        o.setDeliveryDate(date == null ? null : date.toLocalDate());
        o.setNote(rs.getString(14));
        o.setCreatedByName(rs.getString(15));
        Timestamp created = rs.getTimestamp(16);
        o.setCreatedAt(created == null ? null : created.toLocalDateTime());
        Timestamp updated = rs.getTimestamp(17);
        o.setUpdatedAt(updated == null ? null : updated.toLocalDateTime());
        o.setLineCount(rs.getLong(18));
        return o;
    }

    private static void setLong(PreparedStatement ps, int index, Long value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.BIGINT);
        } else {
            ps.setLong(index, value);
        }
    }

    private static void bind(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }
}
