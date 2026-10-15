
package com.oms.dao;

import com.oms.model.GoodsReceipt;

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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Phiếu nhập kho (S5-04) và các ghi chép tồn kho phát sinh khi xác nhận phiếu. */
public class GoodsReceiptDao {

    public record Option(long id, String label) {
    }

    public record UnitOption(long id, String name, BigDecimal factor) {
    }

    public record ProductOption(long id, String label, String baseUnit, List<UnitOption> units) {
    }

    /** Phần đầu phiếu đọc kèm khoá để kiểm trạng thái trước khi sửa hoặc xác nhận. */
    public record Header(long id, String status, long warehouseId) {
    }

    public List<Option> activeSuppliers(Connection conn) throws SQLException {
        List<Option> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, code, name FROM suppliers WHERE status = 'ACTIVE' ORDER BY code");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Option(rs.getLong(1), rs.getString(2) + " · " + rs.getString(3)));
            }
        }
        return list;
    }

    public boolean isActiveSupplier(Connection conn, long id) throws SQLException {
        return exists(conn, "SELECT 1 FROM suppliers WHERE id = ? AND status = 'ACTIVE'", id);
    }

    public boolean isActiveProduct(Connection conn, long id) throws SQLException {
        return exists(conn, "SELECT 1 FROM products WHERE id = ? AND status = 'ACTIVE'", id);
    }

    /** Sản phẩm đang kinh doanh kèm đơn vị cơ sở và các đơn vị quy đổi, cho ô chọn trên phiếu. */
    public List<ProductOption> activeProducts(Connection conn) throws SQLException {
        Map<Long, String[]> products = new LinkedHashMap<>();
        Map<Long, List<UnitOption>> units = new LinkedHashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT p.id, p.sku, p.name, p.base_unit_id, u.name FROM products p "
                        + "JOIN units u ON u.id = p.base_unit_id WHERE p.status = 'ACTIVE' ORDER BY p.sku");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                long id = rs.getLong(1);
                products.put(id, new String[] {rs.getString(2) + " · " + rs.getString(3), rs.getString(5)});
                List<UnitOption> list = new ArrayList<>();
                list.add(new UnitOption(rs.getLong(4), rs.getString(5), BigDecimal.ONE));
                units.put(id, list);
            }
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT pu.product_id, pu.unit_id, u.name, pu.factor FROM product_units pu "
                        + "JOIN units u ON u.id = pu.unit_id JOIN products p ON p.id = pu.product_id "
                        + "WHERE p.status = 'ACTIVE' AND pu.unit_id <> p.base_unit_id ORDER BY pu.product_id, pu.factor");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                List<UnitOption> list = units.get(rs.getLong(1));
                if (list != null) {
                    list.add(new UnitOption(rs.getLong(2), rs.getString(3), plain(rs.getBigDecimal(4))));
                }
            }
        }
        List<ProductOption> list = new ArrayList<>();
        products.forEach((id, p) -> list.add(new ProductOption(id, p[0], p[1], units.get(id))));
        return list;
    }

    public long insert(Connection conn, long supplierId, String documentNo, LocalDate receivedDate,
                       long warehouseId, String note, long actorId) throws SQLException {
        long id;
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO goods_receipts (supplier_id, document_no, received_date, warehouse_id, note, created_by) "
                        + "VALUES (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, supplierId);
            ps.setString(2, documentNo);
            ps.setDate(3, Date.valueOf(receivedDate));
            ps.setLong(4, warehouseId);
            setNullable(ps, 5, note);
            ps.setLong(6, actorId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Không lấy được ID phiếu nhập.");
                }
                id = keys.getLong(1);
            }
        }
        // Mã phiếu suy từ ID nên chỉ gán được sau khi chèn
        try (PreparedStatement ps = conn.prepareStatement("UPDATE goods_receipts SET code = ? WHERE id = ?")) {
            ps.setString(1, String.format("PN%06d", id));
            ps.setLong(2, id);
            ps.executeUpdate();
        }
        return id;
    }

    public void updateHeader(Connection conn, long id, long supplierId, String documentNo, LocalDate receivedDate,
                             long warehouseId, String note) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE goods_receipts SET supplier_id = ?, document_no = ?, received_date = ?, warehouse_id = ?, "
                        + "note = ? WHERE id = ? AND status = 'DRAFT'")) {
            ps.setLong(1, supplierId);
            ps.setString(2, documentNo);
            ps.setDate(3, Date.valueOf(receivedDate));
            ps.setLong(4, warehouseId);
            setNullable(ps, 5, note);
            ps.setLong(6, id);
            ps.executeUpdate();
        }
    }

    public void replaceLines(Connection conn, long receiptId, List<GoodsReceipt.Line> lines) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM goods_receipt_lines WHERE receipt_id = ?")) {
            ps.setLong(1, receiptId);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO goods_receipt_lines (receipt_id, line_no, product_id, unit_id, quantity, factor, "
                        + "base_quantity, lot_no, expiry_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            int no = 1;
            for (GoodsReceipt.Line line : lines) {
                ps.setLong(1, receiptId);
                ps.setInt(2, no++);
                ps.setLong(3, line.productId());
                ps.setLong(4, line.unitId());
                ps.setBigDecimal(5, line.quantity());
                ps.setBigDecimal(6, line.factor());
                ps.setBigDecimal(7, line.baseQuantity());
                setNullable(ps, 8, line.lotNo());
                if (line.expiryDate() == null) {
                    ps.setNull(9, Types.DATE);
                } else {
                    ps.setDate(9, Date.valueOf(line.expiryDate()));
                }
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public Header findHeaderForUpdate(Connection conn, long id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, status, warehouse_id FROM goods_receipts WHERE id = ? FOR UPDATE")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new Header(rs.getLong(1), rs.getString(2), rs.getLong(3)) : null;
            }
        }
    }

    public boolean documentTaken(Connection conn, long supplierId, String documentNo, long excludeId)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM goods_receipts WHERE supplier_id = ? AND document_no = ? AND id <> ?")) {
            ps.setLong(1, supplierId);
            ps.setString(2, documentNo);
            ps.setLong(3, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public GoodsReceipt find(Connection conn, long id) throws SQLException {
        GoodsReceipt head;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT r.id, r.code, r.supplier_id, s.name, r.document_no, r.received_date, r.warehouse_id, w.name, "
                        + "r.note, r.status FROM goods_receipts r JOIN suppliers s ON s.id = r.supplier_id "
                        + "JOIN warehouses w ON w.id = r.warehouse_id WHERE r.id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                head = new GoodsReceipt(rs.getLong(1), rs.getString(2), rs.getLong(3), rs.getString(4),
                        rs.getString(5), rs.getDate(6).toLocalDate(), rs.getLong(7), rs.getString(8),
                        rs.getString(9), rs.getString(10), List.of());
            }
        }
        List<GoodsReceipt.Line> lines = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT product_id, unit_id, quantity, factor, base_quantity, lot_no, expiry_date "
                        + "FROM goods_receipt_lines WHERE receipt_id = ? ORDER BY line_no")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date expiry = rs.getDate(7);
                    lines.add(new GoodsReceipt.Line(rs.getLong(1), rs.getLong(2),
                            plain(rs.getBigDecimal(3)), plain(rs.getBigDecimal(4)),
                            plain(rs.getBigDecimal(5)), rs.getString(6),
                            expiry == null ? null : expiry.toLocalDate()));
                }
            }
        }
        return new GoodsReceipt(head.id(), head.code(), head.supplierId(), head.supplierName(), head.documentNo(),
                head.receivedDate(), head.warehouseId(), head.warehouseName(), head.note(), head.status(), lines);
    }

    /** Phiếu gần đây; warehouseIds null nghĩa là không giới hạn kho (Admin). */
    public List<GoodsReceipt.Summary> recent(Connection conn, Collection<Long> warehouseIds, int limit)
            throws SQLException {
        if (warehouseIds != null && warehouseIds.isEmpty()) {
            return List.of();
        }
        StringBuilder sql = new StringBuilder(
                "SELECT r.id, r.code, r.document_no, r.received_date, s.name, w.name, r.status FROM goods_receipts r "
                        + "JOIN suppliers s ON s.id = r.supplier_id JOIN warehouses w ON w.id = r.warehouse_id");
        if (warehouseIds != null) {
            sql.append(" WHERE r.warehouse_id IN (").append("?,".repeat(warehouseIds.size() - 1)).append("?)");
        }
        sql.append(" ORDER BY r.id DESC LIMIT ?");
        List<GoodsReceipt.Summary> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int i = 1;
            if (warehouseIds != null) {
                for (long id : warehouseIds) {
                    ps.setLong(i++, id);
                }
            }
            ps.setInt(i, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new GoodsReceipt.Summary(rs.getLong(1), rs.getString(2), rs.getString(3),
                            rs.getDate(4).toLocalDate(), rs.getString(5), rs.getString(6), rs.getString(7)));
                }
            }
        }
        return list;
    }

    public void markConfirmed(Connection conn, long id, long actorId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE goods_receipts SET status = 'CONFIRMED', confirmed_by = ?, confirmed_at = CURRENT_TIMESTAMP "
                        + "WHERE id = ? AND status = 'DRAFT'")) {
            ps.setLong(1, actorId);
            ps.setLong(2, id);
            if (ps.executeUpdate() != 1) {
                throw new SQLException("Phiếu nhập không còn ở trạng thái nháp.");
            }
        }
    }

    public void addOnHand(Connection conn, long warehouseId, long productId, BigDecimal quantity)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO warehouse_inventory (warehouse_id, product_id, on_hand, reserved) VALUES (?, ?, ?, 0) "
                        + "ON DUPLICATE KEY UPDATE on_hand = on_hand + VALUES(on_hand)")) {
            ps.setLong(1, warehouseId);
            ps.setLong(2, productId);
            ps.setBigDecimal(3, quantity);
            ps.executeUpdate();
        }
    }

    /** Lưu lô tồn kho kèm mã lô và hạn sử dụng để hỗ trợ FEFO (S6-04). */
    public void insertLot(Connection conn, long warehouseId, long productId, BigDecimal quantity,
                          LocalDate receivedDate, long receiptId, String lotNo, LocalDate expiryDate)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO inventory_lots (warehouse_id, product_id, lot_no, expiry_date, "
                        + "remaining_quantity, unit_cost, received_at, source_type, source_id) "
                        + "SELECT ?, id, ?, ?, ?, COALESCE(cost_price, 0), ?, 'GOODS_RECEIPT', ? "
                        + "FROM products WHERE id = ?")) {
            ps.setLong(1, warehouseId);
            setNullable(ps, 2, lotNo);
            if (expiryDate == null) {
                ps.setNull(3, Types.DATE);
            } else {
                ps.setDate(3, Date.valueOf(expiryDate));
            }
            ps.setBigDecimal(4, quantity);
            ps.setTimestamp(5, Timestamp.valueOf(receivedDate.atStartOfDay()));
            ps.setLong(6, receiptId);
            ps.setLong(7, productId);
            ps.executeUpdate();
        }
    }

    /** Bút toán nhập kho theo đơn vị cơ sở của sản phẩm, liên kết về phiếu nhập. */
    public void insertMovement(Connection conn, long warehouseId, long productId, BigDecimal quantity,
                               long receiptId, long actorId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO inventory_movements (warehouse_id, product_id, unit_id, quantity, movement_type, "
                        + "reference_type, reference_id, created_by) SELECT ?, id, base_unit_id, ?, 'GOODS_RECEIPT', "
                        + "'GOODS_RECEIPT', ?, ? FROM products WHERE id = ?")) {
            ps.setLong(1, warehouseId);
            ps.setBigDecimal(2, quantity);
            ps.setLong(3, receiptId);
            ps.setLong(4, actorId);
            ps.setLong(5, productId);
            ps.executeUpdate();
        }
    }

    private static boolean exists(Connection conn, String sql, long id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Bỏ số 0 thừa mà không ra dạng mũ (2.4E+1), để JSON và ô nhập hiển thị gọn. */
    private static BigDecimal plain(BigDecimal value) {
        return new BigDecimal(value.stripTrailingZeros().toPlainString());
    }

    private static void setNullable(PreparedStatement ps, int index, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            ps.setNull(index, Types.VARCHAR);
        } else {
            ps.setString(index, value);
        }
    }
}
