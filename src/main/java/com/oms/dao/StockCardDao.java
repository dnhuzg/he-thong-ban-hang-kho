package com.oms.dao;

import com.oms.model.NamedItem;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Thẻ kho của một sản phẩm (S6-02): các bút toán nhập / xuất / trả về theo thời gian. */
public class StockCardDao {

    public record Movement(long id, LocalDateTime at, String type, String referenceType, long referenceId,
                           String documentCode, String warehouseName, BigDecimal quantity) {
    }

    /** Sản phẩm đã từng có tồn ở kho, cho ô chọn. */
    public List<NamedItem> products(Connection conn) throws SQLException {
        List<NamedItem> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT p.id, CONCAT(p.sku, ' · ', p.name) FROM products p "
                        + "WHERE EXISTS (SELECT 1 FROM warehouse_inventory i WHERE i.product_id = p.id) ORDER BY p.sku");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new NamedItem(rs.getLong(1), rs.getString(2)));
            }
        }
        return list;
    }

    /** Bút toán của sản phẩm (một kho hoặc mọi kho khi warehouseId null), quy về đơn vị cơ sở, cũ nhất trước. */
    public List<Movement> movements(Connection conn, long productId, Long warehouseId) throws SQLException {
        List<Movement> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT m.id, m.created_at, m.movement_type, m.reference_type, m.reference_id, "
                        + "COALESCE(s.code, a.code), w.name, m.quantity * CASE WHEN m.unit_id IS NULL OR m.unit_id = p.base_unit_id THEN 1 ELSE "
                        + "(SELECT pu.factor FROM product_units pu WHERE pu.product_id = m.product_id AND pu.unit_id = m.unit_id) END "
                        + "FROM inventory_movements m JOIN products p ON p.id = m.product_id JOIN warehouses w ON w.id = m.warehouse_id "

                        + "LEFT JOIN shipments s ON m.reference_type = 'SHIPMENT' AND s.id = m.reference_id "
                        + "LEFT JOIN inventory_adjustments a ON m.reference_type = 'INVENTORY_ADJUSTMENT' AND a.id = m.reference_id "
                        + "WHERE m.product_id = ?" + (warehouseId == null ? "" : " AND m.warehouse_id = ?")
                        + " ORDER BY m.created_at, m.id")) {
            ps.setLong(1, productId);
            if (warehouseId != null) {
                ps.setLong(2, warehouseId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Movement(rs.getLong(1), rs.getTimestamp(2).toLocalDateTime(), rs.getString(3),
                            rs.getString(4), rs.getLong(5), rs.getString(6), rs.getString(7), rs.getBigDecimal(8)));
                }
            }
        }
        return list;
    }

    public BigDecimal onHand(Connection conn, long productId, Long warehouseId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COALESCE(SUM(on_hand), 0) FROM warehouse_inventory WHERE product_id = ?"
                        + (warehouseId == null ? "" : " AND warehouse_id = ?"))) {
            ps.setLong(1, productId);
            if (warehouseId != null) {
                ps.setLong(2, warehouseId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBigDecimal(1);
            }
        }
    }
}
