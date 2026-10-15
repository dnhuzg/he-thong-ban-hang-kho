
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

/**
 * Hóa đơn phục vụ sổ công nợ và phân tích tuổi nợ (S7-03).
 */
public class DebtAgingDao {

    public record InvoiceLine(
            long customerId,
            String customerCode,
            String customerName,
            String invoiceCode,
            BigDecimal total,
            BigDecimal paid,
            LocalDate issuedOn,
            LocalDate dueDate
    ) {
    }

    /**
     * Lấy hóa đơn đã phát hành đến ngày đối chiếu.
     *
     * Với ngày hiện tại, dùng paid_amount đang lưu trên hóa đơn.
     * Với ngày quá khứ, tính số đã thu từ các khoản phân bổ
     * có ngày thanh toán không muộn hơn ngày đối chiếu.
     */
    public List<InvoiceLine> lines(
            Connection conn,
            LocalDate asOf
    ) throws SQLException {

        List<InvoiceLine> list = new ArrayList<>();

        String sql = """
                SELECT
                    c.id,
                    c.code,
                    c.name,
                    i.code,
                    i.total_amount,
                    CASE
                        WHEN ? = CURRENT_DATE()
                        THEN i.paid_amount
                        ELSE COALESCE(paid_as_of.amount, 0)
                    END AS paid_amount_as_of,
                    i.issued_at,
                    i.due_date
                FROM invoices i
                JOIN customers c
                    ON c.id = i.customer_id
                LEFT JOIN (
                    SELECT
                        a.invoice_id,
                        SUM(a.amount) AS amount
                    FROM payment_allocations a
                    JOIN payments p
                        ON p.id = a.payment_id
                    WHERE p.paid_on <= ?
                    GROUP BY a.invoice_id
                ) paid_as_of
                    ON paid_as_of.invoice_id = i.id
                WHERE i.status <> 'CANCELLED'
                  AND i.issued_at < ?
                ORDER BY
                    c.name,
                    c.id,
                    i.issued_at,
                    i.id
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(asOf));
            ps.setDate(2, Date.valueOf(asOf));
            ps.setDate(3, Date.valueOf(asOf.plusDays(1)));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date dueDate = rs.getDate(8);

                    list.add(new InvoiceLine(
                            rs.getLong(1),
                            rs.getString(2),
                            rs.getString(3),
                            rs.getString(4),
                            rs.getBigDecimal(5),
                            rs.getBigDecimal(6),
                            rs.getTimestamp(7)
                                    .toLocalDateTime()
                                    .toLocalDate(),
                            dueDate == null
                                    ? null
                                    : dueDate.toLocalDate()
                    ));
                }
            }
        }

        return list;
    }
}
