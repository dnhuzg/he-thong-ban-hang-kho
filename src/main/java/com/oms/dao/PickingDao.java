package com.oms.dao;

import com.oms.model.PickingList;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Phiếu soạn hàng (S6-03). warehouseIds = null nghĩa là không giới hạn kho (Admin). */
public class PickingDao {

    /** Phần đầu phiếu đọc kèm khoá để kiểm trạng thái trước khi ghi. */
    public record Header(long id, String status, long warehouseId) {
    }

    public record LineLock(long id, long pickingListId, BigDecimal required) {
    }

    public List<PickingList.QueueItem> queue(Connection conn, Collection<Long> warehouseIds) throws SQLException {
        List<PickingList.QueueItem> list = new ArrayList<>();
        if (warehouseIds != null && warehouseIds.isEmpty()) {
            return list;
        }
        String sql = "SELECT o.id, o.code, c.name, o.warehouse_id, w.name, o.delivery_date, "
                + "(SELECT COUNT(*) FROM order_lines l WHERE l.order_id = o.id) FROM orders o "
                + "JOIN customers c ON c.id = o.customer_id JOIN warehouses w ON w.id = o.warehouse_id "
                + "WHERE o.status = 'APPROVED'" + in(" AND o.warehouse_id", warehouseIds)
                + " ORDER BY CASE WHEN o.delivery_date IS NULL THEN 1 ELSE 0 END, o.delivery_date, o.id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, 1, warehouseIds);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PickingList.QueueItem item = new PickingList.QueueItem();
                    item.setOrderId(rs.getLong(1));
                    item.setOrderCode(rs.getString(2));
                    item.setCustomerName(rs.getString(3));
                    item.setWarehouseId(rs.getLong(4));
                    item.setWarehouseName(rs.getString(5));
                    Date delivery = rs.getDate(6);
                    item.setDeliveryDate(delivery == null ? null : delivery.toLocalDate());
                    item.setLineCount(rs.getInt(7));
                    list.add(item);
                }
            }
        }
        return list;
    }

    public long insertList(Connection conn, long warehouseId, long actorId) throws SQLException {
        long id;
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO picking_lists (warehouse_id, created_by) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, warehouseId);
            ps.setLong(2, actorId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Không lấy được ID phiếu soạn.");
                }
                id = keys.getLong(1);
            }
        }
        // Mã phiếu suy từ ID nên chỉ gán được sau khi chèn
        try (PreparedStatement ps = conn.prepareStatement("UPDATE picking_lists SET code = ? WHERE id = ?")) {
            ps.setString(1, String.format("PS%06d", id));
            ps.setLong(2, id);
            ps.executeUpdate();
        }
        return id;
    }

    /** Đưa toàn bộ dòng của đơn vào phiếu soạn, số lượng cần soạn theo đơn vị cơ sở. */
    public int addOrderLines(Connection conn, long pickingListId, long orderId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO picking_list_lines (picking_list_id, order_id, order_line_id, product_id, required_quantity) "
                        + "SELECT ?, order_id, id, product_id, base_quantity FROM order_lines WHERE order_id = ?")) {
            ps.setLong(1, pickingListId);
            ps.setLong(2, orderId);
            return ps.executeUpdate();
        }
    }

    public List<PickingList> recent(Connection conn, Collection<Long> warehouseIds, int limit) throws SQLException {
        List<PickingList> list = new ArrayList<>();
        if (warehouseIds != null && warehouseIds.isEmpty()) {
            return list;
        }
        String sql = "SELECT p.id, p.code, p.warehouse_id, w.name, p.status, u.full_name, p.created_at, "
                + "(SELECT COUNT(DISTINCT l.order_id) FROM picking_list_lines l WHERE l.picking_list_id = p.id), "
                + "(SELECT COUNT(*) FROM picking_list_lines l WHERE l.picking_list_id = p.id), "
                + "(SELECT COUNT(*) FROM picking_list_lines l WHERE l.picking_list_id = p.id AND l.picked_quantity IS NOT NULL) "
                + "FROM picking_lists p JOIN warehouses w ON w.id = p.warehouse_id JOIN users u ON u.id = p.created_by"
                + in(" WHERE p.warehouse_id", warehouseIds)
                + " ORDER BY CASE WHEN p.status = 'OPEN' THEN 0 ELSE 1 END, p.id DESC LIMIT ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int i = bind(ps, 1, warehouseIds);
            ps.setInt(i, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(header(rs));
                }
            }
        }
        return list;
    }

    /** Phiếu kèm dòng, xếp theo nhóm hàng rồi SKU để thủ kho đi lấy hàng cùng nhóm một lượt. */
    public PickingList find(Connection conn, long id) throws SQLException {
        PickingList list;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT p.id, p.code, p.warehouse_id, w.name, p.status, u.full_name, p.created_at, "
                        + "(SELECT COUNT(DISTINCT l.order_id) FROM picking_list_lines l WHERE l.picking_list_id = p.id), "
                        + "(SELECT COUNT(*) FROM picking_list_lines l WHERE l.picking_list_id = p.id), "
                        + "(SELECT COUNT(*) FROM picking_list_lines l WHERE l.picking_list_id = p.id "
                        + "AND l.picked_quantity IS NOT NULL) "
                        + "FROM picking_lists p JOIN warehouses w ON w.id = p.warehouse_id "
                        + "JOIN users u ON u.id = p.created_by WHERE p.id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                list = header(rs);
            }
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT l.id, l.order_id, o.code, c.name, p.sku, p.name, g.name, u.name, l.required_quantity, "
                        + "l.picked_quantity, l.short_reason FROM picking_list_lines l "
                        + "JOIN orders o ON o.id = l.order_id JOIN customers c ON c.id = o.customer_id "
                        + "JOIN products p ON p.id = l.product_id JOIN product_categories g ON g.id = p.category_id "
                        + "JOIN units u ON u.id = p.base_unit_id WHERE l.picking_list_id = ? "
                        + "ORDER BY g.name, p.sku, o.code")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PickingList.Line line = new PickingList.Line();
                    line.setId(rs.getLong(1));
                    line.setOrderId(rs.getLong(2));
                    line.setOrderCode(rs.getString(3));
                    line.setCustomerName(rs.getString(4));
                    line.setSku(rs.getString(5));
                    line.setProductName(rs.getString(6));
                    line.setCategoryName(rs.getString(7));
                    line.setBaseUnit(rs.getString(8));
                    line.setRequired(plain(rs.getBigDecimal(9)));
                    BigDecimal picked = rs.getBigDecimal(10);
                    line.setPicked(picked == null ? null : plain(picked));
                    line.setShortReason(rs.getString(11));
                    list.getLines().add(line);
                }
            }
        }
        return list;
    }

    public Header findHeaderForUpdate(Connection conn, long id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, status, warehouse_id FROM picking_lists WHERE id = ? FOR UPDATE")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new Header(rs.getLong(1), rs.getString(2), rs.getLong(3)) : null;
            }
        }
    }

    public LineLock findLineForUpdate(Connection conn, long lineId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, picking_list_id, required_quantity FROM picking_list_lines WHERE id = ? FOR UPDATE")) {
            ps.setLong(1, lineId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new LineLock(rs.getLong(1), rs.getLong(2), rs.getBigDecimal(3)) : null;
            }
        }
    }

    public void updatePicked(Connection conn, long lineId, BigDecimal picked, String shortReason, long actorId)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE picking_list_lines SET picked_quantity = ?, short_reason = ?, picked_by = ?, "
                        + "picked_at = CURRENT_TIMESTAMP WHERE id = ?")) {
            ps.setBigDecimal(1, picked);
            if (shortReason == null) {
                ps.setNull(2, Types.VARCHAR);
            } else {
                ps.setString(2, shortReason);
            }
            ps.setLong(3, actorId);
            ps.setLong(4, lineId);
            ps.executeUpdate();
        }
    }

    public int countUnpicked(Connection conn, long pickingListId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM picking_list_lines WHERE picking_list_id = ? AND picked_quantity IS NULL")) {
            ps.setLong(1, pickingListId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public void markDone(Connection conn, long id, long actorId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE picking_lists SET status = 'DONE', completed_by = ?, completed_at = CURRENT_TIMESTAMP "
                        + "WHERE id = ? AND status = 'OPEN'")) {
            ps.setLong(1, actorId);
            ps.setLong(2, id);
            if (ps.executeUpdate() != 1) {
                throw new SQLException("Phiếu soạn không còn ở trạng thái đang soạn.");
            }
        }
    }

    private static PickingList header(ResultSet rs) throws SQLException {
        PickingList list = new PickingList();
        list.setId(rs.getLong(1));
        list.setCode(rs.getString(2));
        list.setWarehouseId(rs.getLong(3));
        list.setWarehouseName(rs.getString(4));
        list.setStatus(rs.getString(5));
        list.setCreatedByName(rs.getString(6));
        Timestamp created = rs.getTimestamp(7);
        list.setCreatedAt(created == null ? null : created.toLocalDateTime());
        list.setOrderCount(rs.getInt(8));
        list.setLineCount(rs.getInt(9));
        list.setPickedCount(rs.getInt(10));
        return list;
    }

    private static String in(String column, Collection<Long> ids) {
        return ids == null ? "" : column + " IN (" + "?,".repeat(ids.size() - 1) + "?)";
    }

    private static int bind(PreparedStatement ps, int start, Collection<Long> ids) throws SQLException {
        int i = start;
        if (ids != null) {
            for (long id : ids) {
                ps.setLong(i++, id);
            }
        }
        return i;
    }

    /** Bỏ số 0 thừa mà không ra dạng mũ (2.4E+1). */
    private static BigDecimal plain(BigDecimal value) {
        return new BigDecimal(value.stripTrailingZeros().toPlainString());
    }
}
