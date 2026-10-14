package com.oms.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Sổ tồn theo SKU và kho (S5-05): tồn thực tế, đang giữ chỗ, khả dụng. */
public class StockLedgerDao {

    public record Row(long productId, String sku, String productName, String categoryName, String baseUnit,
                      long warehouseId, String warehouseCode, String warehouseName,
                      BigDecimal onHand, BigDecimal reserved) {
    }

    /**
     * Dòng tồn theo bộ lọc; categoryIds = null là mọi nhóm. status: AVAILABLE còn khả dụng, RESERVED_OUT còn hàng
     * nhưng đã giữ chỗ hết, EMPTY hết hàng thực tế; null là tất cả.
     */
    public List<Row> rows(Connection conn, String keyword, Long warehouseId, Collection<Long> categoryIds, String status)
            throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT p.id, p.sku, p.name, c.name, u.name, w.id, w.code, w.name, i.on_hand, i.reserved "
                        + "FROM warehouse_inventory i JOIN products p ON p.id = i.product_id "
                        + "JOIN product_categories c ON c.id = p.category_id JOIN units u ON u.id = p.base_unit_id "
                        + "JOIN warehouses w ON w.id = i.warehouse_id WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (p.sku LIKE ? OR p.name LIKE ?)");
            String like = "%" + keyword.strip() + "%";
            params.add(like);
            params.add(like);
        }
        if (warehouseId != null) {
            sql.append(" AND i.warehouse_id = ?");
            params.add(warehouseId);
        }
        if (categoryIds != null) {
            sql.append(" AND p.category_id IN (").append("?,".repeat(categoryIds.size() - 1)).append("?)");
            params.addAll(categoryIds);
        }
        if ("AVAILABLE".equals(status)) {
            sql.append(" AND i.on_hand - i.reserved > 0");
        } else if ("RESERVED_OUT".equals(status)) {
            sql.append(" AND i.on_hand > 0 AND i.on_hand - i.reserved <= 0");
        } else if ("EMPTY".equals(status)) {
            sql.append(" AND i.on_hand <= 0");
        }
        sql.append(" ORDER BY p.sku, w.name");
        List<Row> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Row(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5),
                            rs.getLong(6), rs.getString(7), rs.getString(8), rs.getBigDecimal(9), rs.getBigDecimal(10)));
                }
            }
        }
        return list;
    }

    public String warehouseCode(Connection conn, long warehouseId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT code FROM warehouses WHERE id = ?")) {
            ps.setLong(1, warehouseId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }
}
