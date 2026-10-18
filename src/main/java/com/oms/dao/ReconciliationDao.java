package com.oms.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Lưu biên bản đối chiếu và ảnh chụp các dòng công nợ tại thời điểm lập. */
public class ReconciliationDao {

    public record Document(long id, String code, long customerId, LocalDate start, LocalDate end,
                           BigDecimal opening, BigDecimal increase, BigDecimal decrease,
                           BigDecimal closing, String status, String reason, boolean detailsSnapshotted) { }

    public List<Document> list(Connection conn, Long customerId) throws SQLException {
        List<Document> documents = new ArrayList<>();
        String sql = "SELECT id,code,customer_id,period_start,period_end,opening_balance,increase_amount,"
                + "decrease_amount,closing_balance,status,dispute_reason,details_snapshotted_at "
                + "FROM debt_reconciliations" + (customerId == null ? "" : " WHERE customer_id=?")
                + " ORDER BY period_start DESC,id DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (customerId != null) ps.setLong(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    documents.add(new Document(rs.getLong("id"), rs.getString("code"), rs.getLong("customer_id"),
                            rs.getDate("period_start").toLocalDate(), rs.getDate("period_end").toLocalDate(),
                            rs.getBigDecimal("opening_balance"), rs.getBigDecimal("increase_amount"),
                            rs.getBigDecimal("decrease_amount"), rs.getBigDecimal("closing_balance"),
                            rs.getString("status"), rs.getString("dispute_reason"),
                            rs.getTimestamp("details_snapshotted_at") != null));
                }
            }
        }
        return documents;
    }

    public List<FinanceOverviewDao.LedgerEntry> lines(Connection conn, long reconciliationId, long customerId)
            throws SQLException {
        List<FinanceOverviewDao.LedgerEntry> lines = new ArrayList<>();
        String sql = "SELECT entry_date,reference,entry_type,note,increase_amount,decrease_amount "
                + "FROM debt_reconciliation_lines WHERE reconciliation_id=? ORDER BY line_no";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, reconciliationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lines.add(new FinanceOverviewDao.LedgerEntry(customerId, rs.getDate("entry_date").toLocalDate(),
                            rs.getString("reference"), rs.getString("entry_type"), rs.getString("note"),
                            rs.getBigDecimal("increase_amount"), rs.getBigDecimal("decrease_amount")));
                }
            }
        }
        return lines;
    }

    public long next(Connection conn) throws SQLException {
        long next;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT last_number FROM document_sequences WHERE name='RECONCILIATION' FOR UPDATE");
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) throw new SQLException("Thiếu dãy số biên bản RECONCILIATION.");
            next = rs.getLong(1) + 1;
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE document_sequences SET last_number=? WHERE name='RECONCILIATION'")) {
            ps.setLong(1, next);
            if (ps.executeUpdate() != 1) throw new SQLException("Không cập nhật được dãy số biên bản.");
        }
        return next;
    }

    public long insert(Connection conn, String code, long customerId, LocalDate start, LocalDate end,
                       BigDecimal opening, BigDecimal increase, BigDecimal decrease, long actorId) throws SQLException {
        String sql = "INSERT INTO debt_reconciliations(code,customer_id,period_start,period_end,opening_balance,"
                + "increase_amount,decrease_amount,closing_balance,created_by,details_snapshotted_at) "
                + "VALUES(?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, code);
            ps.setLong(2, customerId);
            ps.setDate(3, java.sql.Date.valueOf(start));
            ps.setDate(4, java.sql.Date.valueOf(end));
            ps.setBigDecimal(5, opening);
            ps.setBigDecimal(6, increase);
            ps.setBigDecimal(7, decrease);
            ps.setBigDecimal(8, opening.add(increase).subtract(decrease));
            ps.setLong(9, actorId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Không nhận được ID biên bản vừa tạo.");
                return keys.getLong(1);
            }
        }
    }

    public void insertLines(Connection conn, long reconciliationId,
                            List<FinanceOverviewDao.LedgerEntry> lines) throws SQLException {
        String sql = "INSERT INTO debt_reconciliation_lines(reconciliation_id,line_no,entry_date,reference,"
                + "entry_type,note,increase_amount,decrease_amount) VALUES(?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int lineNo = 1;
            for (FinanceOverviewDao.LedgerEntry line : lines) {
                ps.setLong(1, reconciliationId);
                ps.setInt(2, lineNo++);
                ps.setDate(3, java.sql.Date.valueOf(line.date()));
                ps.setString(4, line.reference());
                ps.setString(5, line.type());
                ps.setString(6, line.note());
                ps.setBigDecimal(7, line.increase());
                ps.setBigDecimal(8, line.decrease());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /** False nếu biên bản không thuộc đại lý này, không tồn tại hoặc đã được phản hồi. */
    public boolean respond(Connection conn, long id, long customerId, String status, String reason)
            throws SQLException {
        String sql = "UPDATE debt_reconciliations SET status=?,dispute_reason=?,responded_at=CURRENT_TIMESTAMP "
                + "WHERE id=? AND customer_id=? AND status='PENDING'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, reason);
            ps.setLong(3, id);
            ps.setLong(4, customerId);
            return ps.executeUpdate() == 1;
        }
    }

    public Long portalCustomer(Connection conn, long userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT customer_id FROM customer_accounts WHERE user_id=?")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : null;
            }
        }
    }
}
