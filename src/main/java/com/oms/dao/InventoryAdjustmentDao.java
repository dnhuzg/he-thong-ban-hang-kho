
package com.oms.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class InventoryAdjustmentDao {

    public record Adjustment(
            long id,
            String code,
            long warehouseId,
            String status,
            String note,
            BigDecimal totalLossValue,
            long createdBy,
            Long approvedBy
    ) {}

    public record AdjustmentLine(
            long id,
            long adjustmentId,
            long productId,
            String reason,
            String reasonDetail,
            BigDecimal quantity,
            BigDecimal lossValue
    ) {}

    public long insert(
            Connection conn,
            String code,
            long warehouseId,
            String note,
            long createdBy
    ) throws SQLException {
        String sql = """
                INSERT INTO inventory_adjustments
                    (code, warehouse_id, status, note, total_loss_value, created_by)
                VALUES (?, ?, 'DRAFT', ?, 0, ?)
                """;

        try (PreparedStatement ps = conn.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, code);
            ps.setLong(2, warehouseId);
            ps.setString(3, note);
            ps.setLong(4, createdBy);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        throw new SQLException("Không lấy được ID phiếu điều chỉnh.");
    }

    public void updateCode(
            Connection conn,
            long adjustmentId,
            String code
    ) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE inventory_adjustments SET code = ? WHERE id = ?")) {
            ps.setString(1, code);
            ps.setLong(2, adjustmentId);
            ps.executeUpdate();
        }
    }

    public void addLine(
            Connection conn,
            long adjustmentId,
            long productId,
            String reason,
            String reasonDetail,
            BigDecimal quantity,
            BigDecimal lossValue
    ) throws SQLException {
        String sql = """
                INSERT INTO inventory_adjustment_lines
                    (adjustment_id, product_id, reason, reason_detail,
                     quantity, loss_value)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, adjustmentId);
            ps.setLong(2, productId);
            ps.setString(3, reason);
            ps.setString(4, reasonDetail);
            ps.setBigDecimal(5, quantity);
            ps.setBigDecimal(6, lossValue);
            ps.executeUpdate();
        }
    }

    public Adjustment find(
            Connection conn,
            long adjustmentId
    ) throws SQLException {
        return findInternal(conn, adjustmentId, false);
    }

    public Adjustment lock(
            Connection conn,
            long adjustmentId
    ) throws SQLException {
        return findInternal(conn, adjustmentId, true);
    }

    private Adjustment findInternal(
            Connection conn,
            long adjustmentId,
            boolean lock
    ) throws SQLException {
        String sql = """
                SELECT id, code, warehouse_id, status, note,
                       total_loss_value, created_by, approved_by
                FROM inventory_adjustments
                WHERE id = ?
                """ + (lock ? " FOR UPDATE" : "");

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, adjustmentId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                long approvedById = rs.getLong("approved_by");
                Long approvedBy = rs.wasNull()
                        ? null
                        : approvedById;

                return new Adjustment(
                        rs.getLong("id"),
                        rs.getString("code"),
                        rs.getLong("warehouse_id"),
                        rs.getString("status"),
                        rs.getString("note"),
                        rs.getBigDecimal("total_loss_value"),
                        rs.getLong("created_by"),
                        approvedBy
                );
            }
        }
    }

    public List<AdjustmentLine> lines(
            Connection conn,
            long adjustmentId
    ) throws SQLException {
        List<AdjustmentLine> result = new ArrayList<>();

        String sql = """
                SELECT id, adjustment_id, product_id, reason,
                       reason_detail, quantity, loss_value
                FROM inventory_adjustment_lines
                WHERE adjustment_id = ?
                ORDER BY id
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, adjustmentId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new AdjustmentLine(
                            rs.getLong("id"),
                            rs.getLong("adjustment_id"),
                            rs.getLong("product_id"),
                            rs.getString("reason"),
                            rs.getString("reason_detail"),
                            rs.getBigDecimal("quantity"),
                            rs.getBigDecimal("loss_value")
                    ));
                }
            }
        }

        return result;
    }

    public List<Adjustment> list(
            Connection conn
    ) throws SQLException {
        List<Adjustment> result = new ArrayList<>();

        String sql = """
                SELECT id, code, warehouse_id, status, note,
                       total_loss_value, created_by, approved_by
                FROM inventory_adjustments
                ORDER BY id DESC
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                long approvedById = rs.getLong("approved_by");

                result.add(new Adjustment(
                        rs.getLong("id"),
                        rs.getString("code"),
                        rs.getLong("warehouse_id"),
                        rs.getString("status"),
                        rs.getString("note"),
                        rs.getBigDecimal("total_loss_value"),
                        rs.getLong("created_by"),
                        rs.wasNull() ? null : approvedById
                ));
            }
        }

        return result;
    }

    public void updateTotalLoss(
            Connection conn,
            long adjustmentId
    ) throws SQLException {
        String sql = """
                UPDATE inventory_adjustments
                SET total_loss_value = (
                    SELECT COALESCE(SUM(loss_value), 0)
                    FROM inventory_adjustment_lines
                    WHERE adjustment_id = ?
                )
                WHERE id = ?
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, adjustmentId);
            ps.setLong(2, adjustmentId);
            ps.executeUpdate();
        }
    }

    public boolean submit(
            Connection conn,
            long adjustmentId
    ) throws SQLException {
        String sql = """
                UPDATE inventory_adjustments
                SET status = 'PENDING',
                    submitted_at = CURRENT_TIMESTAMP
                WHERE id = ? AND status = 'DRAFT'
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, adjustmentId);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean approve(
            Connection conn,
            long adjustmentId,
            long approverId
    ) throws SQLException {
        String sql = """
                UPDATE inventory_adjustments
                SET status = 'APPROVED',
                    approved_by = ?,
                    approved_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND status = 'PENDING'
                  AND created_by <> ?
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, approverId);
            ps.setLong(2, adjustmentId);
            ps.setLong(3, approverId);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean reject(
            Connection conn,
            long adjustmentId,
            long rejectorId,
            String reason
    ) throws SQLException {
        String sql = """
                UPDATE inventory_adjustments
                SET status = 'REJECTED',
                    rejected_by = ?,
                    rejected_at = CURRENT_TIMESTAMP,
                    rejection_reason = ?
                WHERE id = ? AND status = 'PENDING'
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, rejectorId);
            ps.setString(2, reason);
            ps.setLong(3, adjustmentId);
            return ps.executeUpdate() == 1;
        }
    }
}
