package com.oms.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/** Hoá đơn phục vụ sổ công nợ và phân tích tuổi nợ (GHTK-69/GHTK-84). */
public class DebtAgingDao {

    public record InvoiceLine(long invoiceId, long customerId, String customerCode, String customerName,
                              String invoiceCode, BigDecimal total, BigDecimal paid, LocalDate issuedOn,
                              LocalDate dueDate, Long salesRepId, String salesRepName) {
    }

    public record OverdueAgingTotals(BigDecimal total, List<BigDecimal> buckets) {
        public BigDecimal getTotal() { return total; }
        public List<BigDecimal> getBuckets() { return buckets; }
    }

    public record OwnerAging(Long salesRepId, String salesRepName, long overdueCustomers,
                             BigDecimal overdueTotal, BigDecimal age0To30, BigDecimal age31To60,
                             BigDecimal age61To90, BigDecimal ageOver90) {
        public Long getSalesRepId() { return salesRepId; }
        public String getSalesRepName() { return salesRepName; }
        public long getOverdueCustomers() { return overdueCustomers; }
        public BigDecimal getOverdueTotal() { return overdueTotal; }
        public List<BigDecimal> getBuckets() { return List.of(age0To30, age31To60, age61To90, ageOver90); }
    }

    public record RiskCustomer(long customerId, String customerCode, String customerName, String salesRepName,
                               long overdueInvoices, BigDecimal overdueTotal, BigDecimal age0To30,
                               BigDecimal age31To60, BigDecimal age61To90, BigDecimal ageOver90,
                               long oldestOverdueDays) {
        public long getCustomerId() { return customerId; }
        public String getCustomerCode() { return customerCode; }
        public String getCustomerName() { return customerName; }
        public String getSalesRepName() { return salesRepName; }
        public long getOverdueInvoices() { return overdueInvoices; }
        public BigDecimal getOverdueTotal() { return overdueTotal; }
        public List<BigDecimal> getBuckets() { return List.of(age0To30, age31To60, age61To90, ageOver90); }
        public long getOldestOverdueDays() { return oldestOverdueDays; }
    }

    /** Hoá đơn chưa huỷ phát hành tới hết ngày đối chiếu, kèm nhân viên phụ trách hiện tại của đại lý. */
    public List<InvoiceLine> lines(Connection conn, LocalDate asOf) throws SQLException {
        List<InvoiceLine> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT i.id, c.id, c.code, c.name, i.code, i.total_amount, i.paid_amount, i.issued_at, i.due_date, "
                        + "c.sales_rep_id, u.full_name "
                        + "FROM invoices i JOIN customers c ON c.id = i.customer_id "
                        + "LEFT JOIN users u ON u.id = c.sales_rep_id "
                        + "WHERE i.status <> 'CANCELLED' AND i.issued_at < ? "
                        + "ORDER BY c.name, c.id, i.issued_at, i.id")) {
            ps.setDate(1, Date.valueOf(asOf.plusDays(1)));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long repIdValue = rs.getLong(10);
                    Long repId = rs.wasNull() ? null : repIdValue;
                    list.add(new InvoiceLine(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4),
                            rs.getString(5), rs.getBigDecimal(6), rs.getBigDecimal(7),
                            rs.getTimestamp(8).toLocalDateTime().toLocalDate(), rs.getDate(9).toLocalDate(),
                            repId, rs.getString(11)));
                }
            }
        }
        return list;
    }

    /** Tổng nợ quá hạn theo tuổi hoá đơn, tính hoàn toàn từ dữ liệu SQL thực. */
    public OverdueAgingTotals overdueAgingTotals(Connection conn, LocalDate asOf, List<Long> invoiceIds)
            throws SQLException {
        if (invoiceIds.isEmpty()) {
            return new OverdueAgingTotals(BigDecimal.ZERO, List.of(
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        }
        String sql = "SELECT COALESCE(SUM(i.total_amount - i.paid_amount), 0), "
                + bucketSumSql(1) + ", " + bucketSumSql(2) + ", " + bucketSumSql(3) + ", " + bucketSumSql(4) + " "
                + overdueFromWhere(invoiceIds.size());
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int next = bindBucketsAndWhere(ps, asOf, invoiceIds, 1);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new OverdueAgingTotals(rs.getBigDecimal(1), List.of(rs.getBigDecimal(2), rs.getBigDecimal(3),
                        rs.getBigDecimal(4), rs.getBigDecimal(5)));
            }
        }
    }

    /** Tổng hợp công nợ quá hạn theo nhân viên phụ trách; chỉ cộng hoá đơn quá hạn chưa thu đủ. */
    public List<OwnerAging> overdueBySalesRep(Connection conn, LocalDate asOf, List<Long> invoiceIds)
            throws SQLException {
        List<OwnerAging> result = new ArrayList<>();
        if (invoiceIds.isEmpty()) return result;
        String sql = "SELECT c.sales_rep_id, COALESCE(NULLIF(TRIM(u.full_name), ''), 'Chưa phân công') AS rep_name, "
                + "COUNT(DISTINCT c.id), COALESCE(SUM(i.total_amount - i.paid_amount), 0) AS overdue_total, "
                + bucketSumSql(1) + " AS age_0_30, " + bucketSumSql(2) + " AS age_31_60, "
                + bucketSumSql(3) + " AS age_61_90, " + bucketSumSql(4) + " AS age_over_90 "
                + overdueFromWhere(invoiceIds.size())
                + " GROUP BY c.sales_rep_id, u.full_name "
                + "ORDER BY overdue_total DESC, age_over_90 DESC, rep_name ASC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bindBucketsAndWhere(ps, asOf, invoiceIds, 1);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long idValue = rs.getLong(1);
                    Long repId = rs.wasNull() ? null : idValue;
                    result.add(new OwnerAging(repId, rs.getString(2), rs.getLong(3), rs.getBigDecimal(4),
                            rs.getBigDecimal(5), rs.getBigDecimal(6), rs.getBigDecimal(7), rs.getBigDecimal(8)));
                }
            }
        }
        return result;
    }

    /** Đại lý rủi ro cao nhất theo dư nợ quá hạn; hoà thì ưu tiên tuổi nợ cũ hơn. */
    public RiskCustomer highestRiskCustomer(Connection conn, LocalDate asOf, List<Long> invoiceIds)
            throws SQLException {
        if (invoiceIds.isEmpty()) return null;
        String sql = "SELECT c.id, c.code, c.name, "
                + "COALESCE(NULLIF(TRIM(u.full_name), ''), 'Chưa phân công') AS rep_name, COUNT(i.id), "
                + "COALESCE(SUM(i.total_amount - i.paid_amount), 0) AS overdue_total, " + bucketSumSql(1) + " AS age_0_30, "
                + bucketSumSql(2) + " AS age_31_60, " + bucketSumSql(3) + " AS age_61_90, "
                + bucketSumSql(4) + " AS age_over_90, MIN(DATE(i.due_date)) AS oldest_due_date "
                + overdueFromWhere(invoiceIds.size())
                + " GROUP BY c.id, c.code, c.name, c.sales_rep_id, u.full_name "
                + "ORDER BY overdue_total DESC, age_over_90 DESC, age_61_90 DESC, oldest_due_date ASC, c.code ASC LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bindBucketsAndWhere(ps, asOf, invoiceIds, 1);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Date oldestDue = rs.getDate(11);
                long oldestOverdueDays = oldestDue == null ? 0L
                        : Math.max(0L, ChronoUnit.DAYS.between(oldestDue.toLocalDate(), asOf));
                return new RiskCustomer(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getLong(5), rs.getBigDecimal(6), rs.getBigDecimal(7), rs.getBigDecimal(8),
                        rs.getBigDecimal(9), rs.getBigDecimal(10), oldestOverdueDays);
            }
        }
    }

    /**
     * Use date ranges rather than vendor-specific DATEDIFF syntax. MySQL and H2
     * differ in the DATEDIFF signature, while DATE/BETWEEN comparisons are portable.
     */
    private static String bucketSumSql(int bucket) {
        String condition = switch (bucket) {
            case 1, 2, 3 -> "DATE(i.issued_at) BETWEEN ? AND ?";
            default -> "DATE(i.issued_at) < ?";
        };
        return "COALESCE(SUM(CASE WHEN " + condition
                + " THEN i.total_amount - i.paid_amount ELSE 0 END), 0)";
    }

    private static String overdueFromWhere(int invoiceCount) {
        return "FROM invoices i JOIN customers c ON c.id = i.customer_id "
                + "LEFT JOIN users u ON u.id = c.sales_rep_id "
                + "WHERE i.status <> 'CANCELLED' AND i.issued_at < ? AND i.due_date < ? "
                + "AND i.total_amount > i.paid_amount AND i.id IN ("
                + String.join(",", java.util.Collections.nCopies(invoiceCount, "?")) + ")";
    }

    private static int bindBucketsAndWhere(PreparedStatement ps, LocalDate asOf, List<Long> invoiceIds, int start)
            throws SQLException {
        int next = start;
        // Ages are measured from invoice issue date: inclusive ranges avoid SQL-dialect DATEDIFF differences.
        ps.setDate(next++, Date.valueOf(asOf.minusDays(30)));
        ps.setDate(next++, Date.valueOf(asOf));
        ps.setDate(next++, Date.valueOf(asOf.minusDays(60)));
        ps.setDate(next++, Date.valueOf(asOf.minusDays(31)));
        ps.setDate(next++, Date.valueOf(asOf.minusDays(90)));
        ps.setDate(next++, Date.valueOf(asOf.minusDays(61)));
        ps.setDate(next++, Date.valueOf(asOf.minusDays(90)));
        // Shared WHERE: invoices issued through the as-of date and invoices already past due.
        ps.setDate(next++, Date.valueOf(asOf.plusDays(1)));
        ps.setDate(next++, Date.valueOf(asOf));
        for (Long invoiceId : invoiceIds) ps.setLong(next++, invoiceId);
        return next;
    }
}
