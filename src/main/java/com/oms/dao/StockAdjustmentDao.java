package com.oms.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Phiếu điều chỉnh giảm tồn có phê duyệt (S8-03). */
public class StockAdjustmentDao {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public record Adjustment(long id, String code, long warehouseId, String warehouseName, long productId, String sku,
                             String productName, String unit, Long lotId, String lotNo, BigDecimal quantity,
                             String reason, String note, BigDecimal unitCost, BigDecimal lossValue, String status,
                             long createdBy, String createdByName, LocalDateTime createdAt, String decidedByName,
                             String rejectReason) {
        public long getId() { return id; }
        public String getCode() { return code; }
        public String getWarehouseName() { return warehouseName; }
        public String getSku() { return sku; }
        public String getProductName() { return productName; }
        public String getUnit() { return unit; }
        public String getLotNo() { return lotNo; }
        public BigDecimal getQuantity() { return quantity; }
        public String getReason() { return reason; }
        public String getNote() { return note; }
        public BigDecimal getLossValue() { return lossValue; }
        public String getStatus() { return status; }
        public long getCreatedBy() { return createdBy; }
        public String getCreatedByName() { return createdByName; }
        public String getCreatedAtText() { return createdAt.format(TIME); }
        public String getDecidedByName() { return decidedByName; }
        public String getRejectReason() { return rejectReason; }
    }

    /** SKU đang có tồn ở kho trong phạm vi; dùng cho ô chọn khi lập phiếu. */
    public record StockOption(long warehouseId, String warehouseName, long productId, String sku, String name,
                              String unit, BigDecimal available, BigDecimal costPrice, boolean tracked) {
        public long getWarehouseId() { return warehouseId; }
        public String getWarehouseName() { return warehouseName; }
        public long getProductId() { return productId; }
        public String getSku() { return sku; }
        public String getName() { return name; }
        public String getUnit() { return unit; }
        public BigDecimal getAvailable() { return available; }
        public boolean isTracked() { return tracked; }
    }

    private static final String SELECT = "SELECT a.id, a.code, a.warehouse_id, w.name, a.product_id, p.sku, p.name, u.name, "
            + "a.lot_id, l.lot_no, a.quantity, a.reason, a.note, a.unit_cost, a.loss_value, a.status, a.created_by, cu.full_name, "
            + "a.created_at, du.full_name, a.reject_reason FROM stock_adjustments a "
            + "JOIN warehouses w ON w.id = a.warehouse_id JOIN products p ON p.id = a.product_id "
            + "LEFT JOIN units u ON u.id = p.base_unit_id LEFT JOIN inventory_lots l ON l.id = a.lot_id "
            + "JOIN users cu ON cu.id = a.created_by LEFT JOIN users du ON du.id = a.decided_by ";

    /** Kho được gắn cho người dùng (user_warehouses). */
    public Set<Long> userWarehouses(Connection conn, long userId) throws SQLException {
        Set<Long> ids = new HashSet<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT warehouse_id FROM user_warehouses WHERE user_id = ?")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getLong(1));
                }
            }
        }
        return ids;
    }

    /** scope null = mọi kho. */
    public List<Adjustment> list(Connection conn, Collection<Long> scope, int limit) throws SQLException {
        String where = scope == null ? "" : scope.isEmpty() ? "WHERE 1=0 "
                : "WHERE a.warehouse_id IN (" + String.join(",", java.util.Collections.nCopies(scope.size(), "?")) + ") ";
        List<Adjustment> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT + where + "ORDER BY a.id DESC LIMIT ?")) {
            int i = 1;
            if (scope != null) {
                for (long id : scope) {
                    ps.setLong(i++, id);
                }
            }
            ps.setInt(i, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(adjustment(rs));
                }
            }
        }
        return list;
    }

    public Adjustment find(Connection conn, long id, boolean lock) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE a.id = ?" + (lock ? " FOR UPDATE" : ""))) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? adjustment(rs) : null;
            }
        }
    }

    public List<StockOption> stockOptions(Connection conn, Collection<Long> scope) throws SQLException {
        String where = scope == null ? "" : scope.isEmpty() ? "AND 1=0 "
                : "AND wi.warehouse_id IN (" + String.join(",", java.util.Collections.nCopies(scope.size(), "?")) + ") ";
        List<StockOption> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT wi.warehouse_id, w.name, p.id, p.sku, p.name, u.name, "
                + "wi.on_hand - wi.reserved, COALESCE(p.cost_price, 0), p.track_lots FROM warehouse_inventory wi "
                + "JOIN warehouses w ON w.id = wi.warehouse_id JOIN products p ON p.id = wi.product_id "
                + "LEFT JOIN units u ON u.id = p.base_unit_id WHERE wi.on_hand - wi.reserved > 0 " + where
                + "ORDER BY w.name, p.sku")) {
            int i = 1;
            if (scope != null) {
                for (long id : scope) {
                    ps.setLong(i++, id);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new StockOption(rs.getLong(1), rs.getString(2), rs.getLong(3), rs.getString(4), rs.getString(5),
                            rs.getString(6), rs.getBigDecimal(7), rs.getBigDecimal(8), rs.getBoolean(9)));
                }
            }
        }
        return list;
    }

    /** Một dòng tồn có khoá, null khi SKU chưa có tồn ở kho. */
    public StockOption stock(Connection conn, long warehouseId, long productId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT wi.warehouse_id, w.name, p.id, p.sku, p.name, u.name, "
                + "wi.on_hand - wi.reserved, COALESCE(p.cost_price, 0), p.track_lots FROM warehouse_inventory wi "
                + "JOIN warehouses w ON w.id = wi.warehouse_id JOIN products p ON p.id = wi.product_id "
                + "LEFT JOIN units u ON u.id = p.base_unit_id WHERE wi.warehouse_id = ? AND wi.product_id = ? FOR UPDATE")) {
            ps.setLong(1, warehouseId);
            ps.setLong(2, productId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new StockOption(rs.getLong(1), rs.getString(2), rs.getLong(3), rs.getString(4),
                        rs.getString(5), rs.getString(6), rs.getBigDecimal(7), rs.getBigDecimal(8), rs.getBoolean(9)) : null;
            }
        }
    }

    public long insert(Connection conn, String code, long warehouseId, long productId, Long lotId, BigDecimal quantity,
                       String reason, String note, BigDecimal unitCost, long actor) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO stock_adjustments (code, warehouse_id, product_id, "
                + "lot_id, quantity, reason, note, unit_cost, loss_value, created_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, code);
            ps.setLong(2, warehouseId);
            ps.setLong(3, productId);
            if (lotId == null) {
                ps.setNull(4, java.sql.Types.BIGINT);
            } else {
                ps.setLong(4, lotId);
            }
            ps.setBigDecimal(5, quantity);
            ps.setString(6, reason);
            ps.setString(7, note);
            ps.setBigDecimal(8, unitCost);
            ps.setBigDecimal(9, unitCost.multiply(quantity).setScale(2, java.math.RoundingMode.HALF_UP));
            ps.setLong(10, actor);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    public void decide(Connection conn, long id, String status, String rejectReason, long actor) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE stock_adjustments SET status = ?, reject_reason = ?, "
                + "decided_by = ?, decided_at = CURRENT_TIMESTAMP WHERE id = ? AND status = 'PENDING'")) {
            ps.setString(1, status);
            ps.setString(2, rejectReason);
            ps.setLong(3, actor);
            ps.setLong(4, id);
            if (ps.executeUpdate() != 1) {
                throw new SQLException("Phiếu điều chỉnh đã được xử lý.");
            }
        }
    }

    /** Trừ tồn thực tế, không đụng phần đang giữ chỗ cho đơn; false khi không đủ tồn khả dụng. */
    public boolean deduct(Connection conn, long warehouseId, long productId, BigDecimal quantity) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE warehouse_inventory SET on_hand = on_hand - ? "
                + "WHERE warehouse_id = ? AND product_id = ? AND on_hand - reserved >= ?")) {
            ps.setBigDecimal(1, quantity);
            ps.setLong(2, warehouseId);
            ps.setLong(3, productId);
            ps.setBigDecimal(4, quantity);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean deductLot(Connection conn, long lotId, long warehouseId, long productId, BigDecimal quantity)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE inventory_lots SET remaining_quantity = remaining_quantity - ? "
                + "WHERE id = ? AND warehouse_id = ? AND product_id = ? AND remaining_quantity >= ?")) {
            ps.setBigDecimal(1, quantity);
            ps.setLong(2, lotId);
            ps.setLong(3, warehouseId);
            ps.setLong(4, productId);
            ps.setBigDecimal(5, quantity);
            return ps.executeUpdate() == 1;
        }
    }

    public void insertMovement(Connection conn, long warehouseId, long productId, BigDecimal quantity, long adjustmentId,
                               long actor) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO inventory_movements (warehouse_id, product_id, unit_id, "
                + "quantity, movement_type, reference_type, reference_id, created_by) SELECT ?, id, base_unit_id, ?, "
                + "'ADJUSTMENT_OUT', 'STOCK_ADJUSTMENT', ?, ? FROM products WHERE id = ?")) {
            ps.setLong(1, warehouseId);
            ps.setBigDecimal(2, quantity);
            ps.setLong(3, adjustmentId);
            ps.setLong(4, actor);
            ps.setLong(5, productId);
            ps.executeUpdate();
        }
    }

    private static Adjustment adjustment(ResultSet rs) throws SQLException {
        long lot = rs.getLong(9);
        Long lotId = rs.wasNull() ? null : lot;
        return new Adjustment(rs.getLong(1), rs.getString(2), rs.getLong(3), rs.getString(4), rs.getLong(5), rs.getString(6),
                rs.getString(7), rs.getString(8), lotId, rs.getString(10), rs.getBigDecimal(11), rs.getString(12),
                rs.getString(13), rs.getBigDecimal(14), rs.getBigDecimal(15), rs.getString(16), rs.getLong(17),
                rs.getString(18), rs.getTimestamp(19).toLocalDateTime(), rs.getString(20), rs.getString(21));
    }
}
