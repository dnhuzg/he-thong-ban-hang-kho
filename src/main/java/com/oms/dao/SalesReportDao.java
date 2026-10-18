package com.oms.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Doanh số tổng hợp từ dòng hoá đơn đã phát hành (số lượng thực giao), theo một chiều phân tích (S8-06). */
public class SalesReportDao {

    /** Chiều phân tích: biểu thức khoá và nhãn là SQL cố định, không ghép từ dữ liệu người dùng. */
    public enum Dimension {
        EMPLOYEE("Nhân viên", "c.sales_rep_id", "COALESCE(u.full_name, 'Chưa phân công')"),
        REGION("Khu vực", "c.area_id", "COALESCE(a.name, 'Chưa có khu vực')"),
        CUSTOMER("Đại lý", "c.id", "CONCAT(c.code, ' – ', c.name)"),
        CATEGORY("Nhóm hàng", "p.category_id", "COALESCE(pc.name, 'Chưa phân nhóm')"),
        SKU("SKU", "p.id", "CONCAT(p.sku, ' – ', p.name)");

        private final String label;
        private final String keySql;
        private final String labelSql;

        Dimension(String label, String keySql, String labelSql) {
            this.label = label;
            this.keySql = keySql;
            this.labelSql = labelSql;
        }

        public String getLabel() { return label; }
        public String getCode() { return name(); }
    }

    /** cost = số lượng × giá vốn hiện tại của sản phẩm. */
    public record Line(String key, String label, BigDecimal quantity, BigDecimal revenue, BigDecimal cost) {
    }

    /** Hoá đơn phát hành trong [from, to] (tính cả ngày to), trừ hoá đơn đã huỷ. */
    public List<Line> totals(Connection conn, Dimension dimension, LocalDate from, LocalDate to) throws SQLException {
        String sql = "SELECT " + dimension.keySql + ", " + dimension.labelSql + ", SUM(il.quantity), SUM(il.amount), "
                + "SUM(il.quantity * COALESCE(p.cost_price, 0)) "
                + "FROM invoice_lines il JOIN invoices i ON i.id = il.invoice_id "
                + "JOIN customers c ON c.id = i.customer_id JOIN products p ON p.id = il.product_id "
                + "LEFT JOIN users u ON u.id = c.sales_rep_id LEFT JOIN areas a ON a.id = c.area_id "
                + "LEFT JOIN product_categories pc ON pc.id = p.category_id "
                + "WHERE i.status <> 'CANCELLED' AND i.issued_at >= ? AND i.issued_at < ? "
                + "GROUP BY " + dimension.keySql + ", " + dimension.labelSql + " ORDER BY SUM(il.amount) DESC";
        List<Line> lines = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(from.atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(to.plusDays(1).atStartOfDay()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lines.add(new Line(String.valueOf(rs.getObject(1)), rs.getString(2), rs.getBigDecimal(3),
                            rs.getBigDecimal(4), rs.getBigDecimal(5)));
                }
            }
        }
        return lines;
    }
}
