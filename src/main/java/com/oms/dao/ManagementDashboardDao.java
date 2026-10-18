package com.oms.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Số liệu dashboard điều hành (S8-05): doanh số theo hoá đơn đã phát hành, đơn chờ xử lý, công nợ quá hạn, tồn kho. */
public class ManagementDashboardDao {

    private static final String OPEN = "i.total_amount - i.paid_amount - i.credited_amount";

    public record OverdueCustomer(String code, String name, String area, long days, BigDecimal amount) {
        public String getCode() { return code; }
        public String getName() { return name; }
        public String getArea() { return area; }
        public long getDays() { return days; }
        public BigDecimal getAmount() { return amount; }
    }

    /** Doanh số hoá đơn phát hành trong [from, to] (tính cả ngày to), trừ hoá đơn huỷ. */
    public BigDecimal revenue(Connection conn, LocalDate from, LocalDate to) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT COALESCE(SUM(i.total_amount), 0) FROM invoices i "
                + "WHERE i.status <> 'CANCELLED' AND i.issued_at >= ? AND i.issued_at < ?")) {
            ps.setTimestamp(1, Timestamp.valueOf(from.atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(to.plusDays(1).atStartOfDay()));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBigDecimal(1);
            }
        }
    }

    /** Doanh số từng ngày trong [from, to], ngày không có hoá đơn = 0. */
    public Map<LocalDate, BigDecimal> dailyRevenue(Connection conn, LocalDate from, LocalDate to) throws SQLException {
        Map<LocalDate, BigDecimal> days = new LinkedHashMap<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            days.put(d, BigDecimal.ZERO);
        }
        try (PreparedStatement ps = conn.prepareStatement("SELECT CAST(i.issued_at AS DATE), SUM(i.total_amount) "
                + "FROM invoices i WHERE i.status <> 'CANCELLED' AND i.issued_at >= ? AND i.issued_at < ? "
                + "GROUP BY CAST(i.issued_at AS DATE)")) {
            ps.setTimestamp(1, Timestamp.valueOf(from.atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(to.plusDays(1).atStartOfDay()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    days.put(rs.getDate(1).toLocalDate(), rs.getBigDecimal(2));
                }
            }
        }
        return days;
    }

    /** Doanh số theo khu vực của đại lý trong [from, to], giảm dần. */
    public Map<String, BigDecimal> revenueByArea(Connection conn, LocalDate from, LocalDate to) throws SQLException {
        Map<String, BigDecimal> areas = new LinkedHashMap<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT COALESCE(a.name, 'Chưa có khu vực'), SUM(i.total_amount) "
                + "FROM invoices i JOIN customers c ON c.id = i.customer_id LEFT JOIN areas a ON a.id = c.area_id "
                + "WHERE i.status <> 'CANCELLED' AND i.issued_at >= ? AND i.issued_at < ? "
                + "GROUP BY COALESCE(a.name, 'Chưa có khu vực') ORDER BY SUM(i.total_amount) DESC")) {
            ps.setTimestamp(1, Timestamp.valueOf(from.atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(to.plusDays(1).atStartOfDay()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    areas.put(rs.getString(1), rs.getBigDecimal(2));
                }
            }
        }
        return areas;
    }

    public long countOrders(Connection conn, String... statuses) throws SQLException {
        String in = String.join(",", java.util.Collections.nCopies(statuses.length, "?"));
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM orders WHERE status IN (" + in + ")")) {
            for (int i = 0; i < statuses.length; i++) {
                ps.setString(i + 1, statuses[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    /** Đại lý có hoá đơn quá hạn còn nợ tại ngày asOf, xếp theo số tiền quá hạn giảm dần. */
    public List<OverdueCustomer> overdueCustomers(Connection conn, LocalDate asOf) throws SQLException {
        List<OverdueCustomer> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT c.code, c.name, COALESCE(a.name, 'Chưa có khu vực'), "
                + "MIN(i.due_date), SUM(" + OPEN + ") FROM invoices i JOIN customers c ON c.id = i.customer_id "
                + "LEFT JOIN areas a ON a.id = c.area_id "
                + "WHERE i.status NOT IN ('CANCELLED', 'PAID') AND i.due_date < ? AND " + OPEN + " > 0 "
                + "GROUP BY c.id, c.code, c.name, a.name ORDER BY SUM(" + OPEN + ") DESC")) {
            ps.setDate(1, Date.valueOf(asOf));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long days = java.time.temporal.ChronoUnit.DAYS.between(rs.getDate(4).toLocalDate(), asOf);
                    list.add(new OverdueCustomer(rs.getString(1), rs.getString(2), rs.getString(3), days, rs.getBigDecimal(5)));
                }
            }
        }
        return list;
    }

    /** SKU đang kinh doanh có tồn khả dụng (thực tế − giữ chỗ) bằng 0 ở ít nhất một kho đã từng có tồn. */
    public long outOfStockSkus(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(DISTINCT wi.product_id) FROM warehouse_inventory wi "
                + "JOIN products p ON p.id = wi.product_id WHERE p.status = 'ACTIVE' AND wi.on_hand - wi.reserved <= 0");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        }
    }
}
