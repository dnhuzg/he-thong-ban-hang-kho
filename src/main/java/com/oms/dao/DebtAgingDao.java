package com.oms.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Hoá đơn phục vụ sổ công nợ và phân tích tuổi nợ (S7-03). */
public class DebtAgingDao {

    public record InvoiceLine(long customerId, String customerCode, String customerName, String invoiceCode,
                              BigDecimal total, BigDecimal paid, LocalDate issuedOn, LocalDate dueDate) {
    }

    /** Hoá đơn chưa huỷ phát hành tới hết ngày đối chiếu, theo đại lý rồi ngày phát hành. */
    public List<InvoiceLine> lines(Connection conn, LocalDate asOf) throws SQLException {
        List<InvoiceLine> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT c.id, c.code, c.name, i.code, i.total_amount, i.paid_amount + i.credited_amount, i.issued_at, i.due_date "
                        + "FROM invoices i JOIN customers c ON c.id = i.customer_id "
                        + "WHERE i.status <> 'CANCELLED' AND i.issued_at < ? ORDER BY c.name, c.id, i.issued_at, i.id")) {
            ps.setDate(1, Date.valueOf(asOf.plusDays(1)));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new InvoiceLine(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                            rs.getBigDecimal(5), rs.getBigDecimal(6), rs.getTimestamp(7).toLocalDateTime().toLocalDate(),
                            rs.getDate(8).toLocalDate()));
                }
            }
        }
        return list;
    }
}
