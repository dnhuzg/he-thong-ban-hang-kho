package com.oms.dao;

import com.oms.model.InventoryLotAgeRow;
import com.oms.model.InventoryValueRow;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class InventoryReportDao {

    public List<InventoryValueRow> findValueRows(Connection conn, Long warehouseId, Long categoryId)
            throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT w.id AS warehouse_id,
                       w.name AS warehouse_name,
                       c.id AS category_id,
                       c.name AS category_name,
                       SUM(l.remaining_quantity) AS quantity,
                       SUM(l.remaining_quantity * l.unit_cost) AS stock_value
                FROM inventory_lots l
                JOIN warehouses w ON w.id = l.warehouse_id
                JOIN products p ON p.id = l.product_id
                JOIN product_categories c ON c.id = p.category_id
                WHERE l.remaining_quantity > 0
                """);

        List<Long> params = new ArrayList<>();
        if (warehouseId != null) {
            sql.append(" AND l.warehouse_id = ?");
            params.add(warehouseId);
        }
        if (categoryId != null) {
            sql.append(" AND p.category_id = ?");
            params.add(categoryId);
        }

        sql.append("""
                 GROUP BY w.id, w.name, c.id, c.name
                 ORDER BY w.name, c.name
                """);

        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setLong(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<InventoryValueRow> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(new InventoryValueRow(
                            rs.getLong("warehouse_id"),
                            rs.getString("warehouse_name"),
                            rs.getLong("category_id"),
                            rs.getString("category_name"),
                            rs.getBigDecimal("quantity"),
                            rs.getBigDecimal("stock_value")));
                }
                return rows;
            }
        }
    }

    public List<InventoryLotAgeRow> findLotAges(Connection conn, Long warehouseId, Long categoryId)
            throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT p.id AS product_id,
                       p.sku,
                       p.name AS product_name,
                       l.remaining_quantity AS quantity,
                       l.received_at
                FROM inventory_lots l
                JOIN products p ON p.id = l.product_id
                WHERE l.remaining_quantity > 0
                """);

        List<Long> params = new ArrayList<>();
        if (warehouseId != null) {
            sql.append(" AND l.warehouse_id = ?");
            params.add(warehouseId);
        }
        if (categoryId != null) {
            sql.append(" AND p.category_id = ?");
            params.add(categoryId);
        }

        sql.append(" ORDER BY p.sku, l.received_at");

        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setLong(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<InventoryLotAgeRow> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(new InventoryLotAgeRow(
                            rs.getLong("product_id"),
                            rs.getString("sku"),
                            rs.getString("product_name"),
                            rs.getBigDecimal("quantity"),
                            rs.getTimestamp("received_at").toLocalDateTime()));
                }
                return rows;
            }
        }
    }
}