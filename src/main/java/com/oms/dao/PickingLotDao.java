package com.oms.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class PickingLotDao {

    public record Selection(long lotId, BigDecimal quantity, String overrideReason) {
    }

    public void replaceSelections(Connection conn, long pickingLineId, List<Selection> selections)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM picking_line_lots WHERE picking_line_id = ?")) {
            ps.setLong(1, pickingLineId);
            ps.executeUpdate();
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO picking_line_lots "
                        + "(picking_line_id, inventory_lot_id, quantity, override_reason) "
                        + "VALUES (?, ?, ?, ?)")) {
            for (Selection selection : selections) {
                ps.setLong(1, pickingLineId);
                ps.setLong(2, selection.lotId());
                ps.setBigDecimal(3, selection.quantity());
                ps.setString(4, selection.overrideReason());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}