
package com.oms.dao;

import com.oms.model.Shipment;
import com.oms.model.ShipmentExport;
import com.oms.service.ServiceException;

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
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Xác nhận xuất kho (S6-05), bổ sung xuất kho theo lô FEFO (S6-04).
 * warehouseIds = null nghĩa là không giới hạn kho (Admin).
 */
public class ShipmentExportDao {

    /** Một dòng đơn đã soạn, số lượng theo đơn vị cơ sở. */
    public record PickedLine(long orderLineId, long productId, long baseUnitId,
                             BigDecimal required, BigDecimal picked,
                             String shortReason) {
    }

    /** Phân bổ lô đã chọn cho một dòng phiếu soạn. */
    public record PickedLot(long pickingLineId, long lotId, long productId,
                            BigDecimal quantity) {
    }

    /** Đơn đang soạn mà mọi dòng đã nằm trong phiếu soạn hoàn tất. */
    private static final String READY = "o.status = 'PICKING' "
            + "AND EXISTS (SELECT 1 FROM picking_list_lines l WHERE l.order_id = o.id) "
            + "AND NOT EXISTS (SELECT 1 FROM picking_list_lines l JOIN picking_lists p ON p.id = l.picking_list_id "
            + "WHERE l.order_id = o.id AND p.status <> 'DONE')";

    public List<ShipmentExport.Ready> ready(Connection conn, Collection<Long> warehouseIds)
            throws SQLException {
        List<ShipmentExport.Ready> list = new ArrayList<>();
        if (warehouseIds != null && warehouseIds.isEmpty()) {
            return list;
        }

        String sql = "SELECT o.id, o.code, c.name, w.name, "
                + "(SELECT MAX(p.code) FROM picking_list_lines l JOIN picking_lists p ON p.id = l.picking_list_id WHERE l.order_id = o.id), "
                + "(SELECT COUNT(*) FROM picking_list_lines l WHERE l.order_id = o.id), "
                + "(SELECT COUNT(*) FROM picking_list_lines l WHERE l.order_id = o.id AND l.picked_quantity < l.required_quantity) "
                + "FROM orders o JOIN customers c ON c.id = o.customer_id "
                + "JOIN warehouses w ON w.id = o.warehouse_id "
                + "WHERE " + READY + in(" AND o.warehouse_id", warehouseIds)
                + " ORDER BY o.id";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, 1, warehouseIds);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ShipmentExport.Ready item = new ShipmentExport.Ready();
                    item.setOrderId(rs.getLong(1));
                    item.setOrderCode(rs.getString(2));
                    item.setCustomerName(rs.getString(3));
                    item.setWarehouseName(rs.getString(4));
                    item.setPickingCode(rs.getString(5));
                    item.setLineCount(rs.getInt(6));
                    item.setShortCount(rs.getInt(7));
                    list.add(item);
                }
            }
        }
        return list;
    }

    public boolean isReady(Connection conn, long orderId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM orders o WHERE o.id = ? AND " + READY)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public List<PickedLine> pickedLines(Connection conn, long orderId)
            throws SQLException {
        List<PickedLine> list = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT l.order_line_id, l.product_id, p.base_unit_id, "
                        + "l.required_quantity, l.picked_quantity, l.short_reason "
                        + "FROM picking_list_lines l "
                        + "JOIN products p ON p.id = l.product_id "
                        + "WHERE l.order_id = ? ORDER BY l.order_line_id")) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new PickedLine(
                            rs.getLong(1),
                            rs.getLong(2),
                            rs.getLong(3),
                            rs.getBigDecimal(4),
                            rs.getBigDecimal(5),
                            rs.getString(6)
                    ));
                }
            }
        }
        return list;
    }

    /**
     * S6-04: Trừ tồn kho theo đúng lô đã chọn.
     *
     * Phương thức này phải được gọi bên trong transaction xuất kho.
     * Không cập nhật warehouse_inventory ở đây vì consumeReservations()
     * đã xử lý tồn kho tổng.
     *
     * Mỗi dòng có số lượng thực soạn > 0 phải có phân bổ lô tương ứng.
     * Nếu thiếu phân bổ, lô hết hạn, sai kho hoặc không đủ tồn thì
     * giao dịch xuất kho phải rollback.
     */
    public void consumePickedLots(Connection conn, long orderId, long warehouseId)
            throws SQLException {

        List<long[]> lines = new ArrayList<>();
        List<BigDecimal> pickedQuantities = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, picked_quantity "
                        + "FROM picking_list_lines "
                        + "WHERE order_id = ? ORDER BY id FOR UPDATE")) {
            ps.setLong(1, orderId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lines.add(new long[] {rs.getLong(1)});
                    pickedQuantities.add(rs.getBigDecimal(2));
                }
            }
        }

        for (int i = 0; i < lines.size(); i++) {
            long pickingLineId = lines.get(i)[0];
            BigDecimal picked = pickedQuantities.get(i);

            if (picked == null) {
                throw new ServiceException(
                        "Còn dòng phiếu soạn chưa ghi số lượng thực soạn.");
            }

            List<PickedLot> allocations = new ArrayList<>();

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT pll.picking_line_id, pll.inventory_lot_id, "
                            + "l.product_id, pll.quantity "
                            + "FROM picking_line_lots pll "
                            + "JOIN inventory_lots l ON l.id = pll.inventory_lot_id "
                            + "WHERE pll.picking_line_id = ? "
                            + "ORDER BY pll.inventory_lot_id")) {
                ps.setLong(1, pickingLineId);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        allocations.add(new PickedLot(
                                rs.getLong(1),
                                rs.getLong(2),
                                rs.getLong(3),
                                rs.getBigDecimal(4)
                        ));
                    }
                }
            }

            BigDecimal allocated = BigDecimal.ZERO;
            for (PickedLot lot : allocations) {
                if (lot.quantity() == null || lot.quantity().signum() <= 0) {
                    throw new ServiceException(
                            "Số lượng phân bổ lô không hợp lệ ở dòng #"
                                    + pickingLineId);
                }
                allocated = allocated.add(lot.quantity());
            }

            if (allocated.compareTo(picked) != 0) {
                throw new ServiceException(
                        "Dòng soạn #" + pickingLineId
                                + " chưa phân bổ đủ số lượng theo lô FEFO.");
            }

            for (PickedLot allocation : allocations) {
                consumeOneLot(
                        conn,
                        warehouseId,
                        pickingLineId,
                        allocation.lotId(),
                        allocation.quantity()
                );
            }
        }
    }

    /**
     * Khóa và trừ đúng một lô.
     * Kiểm tra lại tại thời điểm xuất để tránh sử dụng tồn đã thay đổi.
     */
    private void consumeOneLot(Connection conn, long warehouseId,
                               long pickingLineId, long lotId,
                               BigDecimal quantity) throws SQLException {

        long productId;

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT product_id FROM picking_list_lines WHERE id = ?")) {
            ps.setLong(1, pickingLineId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new ServiceException(
                            "Không tìm thấy dòng phiếu soạn #" + pickingLineId);
                }
                productId = rs.getLong(1);
            }
        }

        BigDecimal remaining;
        LocalDate expiryDate;

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT remaining_quantity, expiry_date "
                        + "FROM inventory_lots "
                        + "WHERE id = ? AND warehouse_id = ? AND product_id = ? "
                        + "FOR UPDATE")) {
            ps.setLong(1, lotId);
            ps.setLong(2, warehouseId);
            ps.setLong(3, productId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new ServiceException(
                            "Lô #" + lotId
                                    + " không thuộc đúng sản phẩm hoặc kho xuất.");
                }

                remaining = rs.getBigDecimal(1);
                Date expiry = rs.getDate(2);
                expiryDate = expiry == null ? null : expiry.toLocalDate();
            }
        }

        if (expiryDate != null && expiryDate.isBefore(LocalDate.now())) {
            throw new ServiceException(
                    "Lô #" + lotId + " đã hết hạn sử dụng, không thể xuất kho.");
        }

        if (remaining.compareTo(quantity) < 0) {
            throw new ServiceException(
                    "Lô #" + lotId + " không đủ tồn kho để xuất. "
                            + "Vui lòng kiểm tra và chọn lại lô.");
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE inventory_lots "
                        + "SET remaining_quantity = remaining_quantity - ? "
                        + "WHERE id = ? AND warehouse_id = ? AND product_id = ? "
                        + "AND remaining_quantity >= ?")) {
            ps.setBigDecimal(1, quantity);
            ps.setLong(2, lotId);
            ps.setLong(3, warehouseId);
            ps.setLong(4, productId);
            ps.setBigDecimal(5, quantity);

            if (ps.executeUpdate() != 1) {
                throw new ServiceException(
                        "Không thể trừ tồn lô #" + lotId
                                + ". Tồn kho có thể đã thay đổi.");
            }
        }
    }

    /**
     * Chuyển giữ chỗ của đơn thành xuất thực: nhả toàn bộ phần giữ chỗ,
     * trừ tồn thực tế đúng số đã xuất.
     * Phần soạn thiếu được nhả luôn vì hàng thực tế không có trên kệ.
     */
    public void consumeReservations(Connection conn, long orderId,
                                    Map<Long, BigDecimal> shippedByProduct)
            throws SQLException {
        List<long[]> keys = new ArrayList<>();
        List<BigDecimal> reserved = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT warehouse_id, product_id, quantity "
                        + "FROM order_inventory_reservations WHERE order_id = ? "
                        + "ORDER BY warehouse_id, product_id FOR UPDATE")) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    keys.add(new long[] {rs.getLong(1), rs.getLong(2)});
                    reserved.add(rs.getBigDecimal(3));
                }
            }
        }

        for (int i = 0; i < keys.size(); i++) {
            BigDecimal shipped = shippedByProduct.getOrDefault(
                    keys.get(i)[1], BigDecimal.ZERO);

            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE warehouse_inventory "
                            + "SET reserved = reserved - ?, on_hand = on_hand - ? "
                            + "WHERE warehouse_id = ? AND product_id = ? "
                            + "AND reserved >= ? AND on_hand >= ?")) {
                ps.setBigDecimal(1, reserved.get(i));
                ps.setBigDecimal(2, shipped);
                ps.setLong(3, keys.get(i)[0]);
                ps.setLong(4, keys.get(i)[1]);
                ps.setBigDecimal(5, reserved.get(i));
                ps.setBigDecimal(6, shipped);

                if (ps.executeUpdate() != 1) {
                    throw new ServiceException(
                            "Tồn kho của sản phẩm #" + keys.get(i)[1]
                                    + " không đủ để xuất. Vui lòng kiểm tra lại sổ tồn.");
                }
            }
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM order_inventory_reservations WHERE order_id = ?")) {
            ps.setLong(1, orderId);
            ps.executeUpdate();
        }
    }

    public long insertShipment(Connection conn, long warehouseId,
                               long customerId, long orderId) throws SQLException {
        long id;

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO shipments "
                        + "(code, warehouse_id, customer_id, order_id, status) "
                        + "VALUES (?, ?, ?, ?, 'READY')",
                Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1,
                    "T" + UUID.randomUUID().toString().replace("-", "")
                            .substring(0, 28));
            ps.setLong(2, warehouseId);
            ps.setLong(3, customerId);
            ps.setLong(4, orderId);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Không lấy được ID phiếu xuất.");
                }
                id = keys.getLong(1);
            }
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE shipments SET code = ? WHERE id = ?")) {
            ps.setString(1, String.format("PX%06d", id));
            ps.setLong(2, id);
            ps.executeUpdate();
        }

        return id;
    }

    public void insertItem(Connection conn, long shipmentId, long productId,
                           long unitId, BigDecimal quantity)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO shipment_items "
                        + "(shipment_id, product_id, unit_id, quantity) "
                        + "VALUES (?, ?, ?, ?)")) {
            ps.setLong(1, shipmentId);
            ps.setLong(2, productId);
            ps.setLong(3, unitId);
            ps.setBigDecimal(4, quantity);
            ps.executeUpdate();
        }
    }

    public void insertMovement(Connection conn, long warehouseId,
                               long productId, long unitId,
                               BigDecimal quantity, long shipmentId,
                               long actorId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO inventory_movements "
                        + "(warehouse_id, product_id, unit_id, quantity, "
                        + "movement_type, reference_type, reference_id, created_by) "
                        + "VALUES (?, ?, ?, ?, 'SHIPMENT_OUT', 'SHIPMENT', ?, ?)")) {
            ps.setLong(1, warehouseId);
            ps.setLong(2, productId);
            ps.setLong(3, unitId);
            ps.setBigDecimal(4, quantity);
            ps.setLong(5, shipmentId);
            ps.setLong(6, actorId);
            ps.executeUpdate();
        }
    }

    public void insertBackorder(Connection conn, long orderId, PickedLine line,
                                BigDecimal quantity, long shipmentId)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO order_backorders "
                        + "(order_id, order_line_id, product_id, quantity, reason, shipment_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?)")) {
            ps.setLong(1, orderId);
            ps.setLong(2, line.orderLineId());
            ps.setLong(3, line.productId());
            ps.setBigDecimal(4, quantity);

            if (line.shortReason() == null) {
                ps.setNull(5, Types.VARCHAR);
            } else {
                ps.setString(5, line.shortReason());
            }

            ps.setLong(6, shipmentId);
            ps.executeUpdate();
        }
    }

    public List<ShipmentExport.Backorder> backorders(
            Connection conn, Collection<Long> warehouseIds)
            throws SQLException {
        List<ShipmentExport.Backorder> list = new ArrayList<>();
        if (warehouseIds != null && warehouseIds.isEmpty()) {
            return list;
        }

        String sql = "SELECT b.order_id, o.code, c.name, p.sku, p.name, "
                + "u.name, b.quantity, b.reason, b.created_at "
                + "FROM order_backorders b "
                + "JOIN orders o ON o.id = b.order_id "
                + "JOIN customers c ON c.id = o.customer_id "
                + "JOIN products p ON p.id = b.product_id "
                + "JOIN units u ON u.id = p.base_unit_id"
                + in(" WHERE o.warehouse_id", warehouseIds)
                + " ORDER BY b.id DESC";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, 1, warehouseIds);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ShipmentExport.Backorder item = new ShipmentExport.Backorder();
                    item.setOrderId(rs.getLong(1));
                    item.setOrderCode(rs.getString(2));
                    item.setCustomerName(rs.getString(3));
                    item.setSku(rs.getString(4));
                    item.setProductName(rs.getString(5));
                    item.setBaseUnit(rs.getString(6));
                    item.setQuantity(plain(rs.getBigDecimal(7)));
                    item.setReason(rs.getString(8));
                    item.setCreatedAt(time(rs.getTimestamp(9)));
                    list.add(item);
                }
            }
        }
        return list;
    }

    public List<ShipmentExport.Recent> recent(
            Connection conn, Collection<Long> warehouseIds, int limit)
            throws SQLException {
        List<ShipmentExport.Recent> list = new ArrayList<>();
        if (warehouseIds != null && warehouseIds.isEmpty()) {
            return list;
        }

        String sql = "SELECT s.id, s.code, o.code, c.name, s.status, s.created_at "
                + "FROM shipments s "
                + "JOIN orders o ON o.id = s.order_id "
                + "JOIN customers c ON c.id = s.customer_id"
                + in(" WHERE s.warehouse_id", warehouseIds)
                + " ORDER BY s.id DESC LIMIT ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int i = bind(ps, 1, warehouseIds);
            ps.setInt(i, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ShipmentExport.Recent item = new ShipmentExport.Recent();
                    item.setId(rs.getLong(1));
                    item.setCode(rs.getString(2));
                    item.setOrderCode(rs.getString(3));
                    item.setCustomerName(rs.getString(4));
                    item.setStatus(Shipment.Status.valueOf(rs.getString(5)));
                    item.setCreatedAt(time(rs.getTimestamp(6)));
                    list.add(item);
                }
            }
        }
        return list;
    }

    private static java.time.LocalDateTime time(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    private static String in(String column, Collection<Long> ids) {
        return ids == null ? ""
                : column + " IN (" + "?,".repeat(ids.size() - 1) + "?)";
    }

    private static int bind(PreparedStatement ps, int start,
                            Collection<Long> ids) throws SQLException {
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
