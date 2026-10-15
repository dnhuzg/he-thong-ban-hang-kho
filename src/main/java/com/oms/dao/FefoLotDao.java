package com.oms.dao;

import com.oms.model.FefoLot;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Truy vấn lô tồn kho theo nguyên tắc FEFO (S6-04). */
public class FefoLotDao {

    public List<FefoLot> availableLots(Connection conn, long warehouseId, long productId)
            throws SQLException {
        String sql = """
                SELECT id, warehouse_id, product_id, lot_no, expiry_date, remaining_quantity
                FROM inventory_lots
                WHERE warehouse_id = ?
                  AND product_id = ?
                  AND remaining_quantity > 0
                  AND (expiry_date IS NULL OR expiry_date >= CURRENT_DATE())
                ORDER BY
                  CASE WHEN expiry_date IS NULL THEN 1 ELSE 0 END,
                  expiry_date ASC,
                  received_at ASC,
                  id ASC
                """;

        List<FefoLot> lots = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, warehouseId);
            ps.setLong(2, productId);

            try (ResultSet rs = ps.executeQuery()) {
                boolean first = true;

                while (rs.next()) {
                    Date expiry = rs.getDate("expiry_date");

                    lots.add(new FefoLot(
                            rs.getLong("id"),
                            rs.getLong("warehouse_id"),
                            rs.getLong("product_id"),
                            rs.getString("lot_no"),
                            expiry == null ? null : expiry.toLocalDate(),
                            rs.getBigDecimal("remaining_quantity"),
                            first
                    ));

                    first = false;
                }
            }
        }

        return lots;
    }
}
