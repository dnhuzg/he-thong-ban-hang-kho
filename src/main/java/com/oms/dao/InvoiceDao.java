package com.oms.dao;

import com.oms.model.Invoice;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Hoá đơn bán hàng (S7-01) và các ghi chép công nợ phát sinh khi phát hành. */
public class InvoiceDao {

    public record ShipmentLock(long id, long customerId, Long orderId, String status, Long invoiceId) {
    }

    /** Hàng thực giao của một sản phẩm trên phiếu (đơn vị cơ sở) kèm tổng tiền đã chốt và tổng số lượng trên đơn. */
    public record Delivered(long productId, BigDecimal quantity, BigDecimal orderNet, BigDecimal orderQuantity) {
    }

    public List<Invoice.Eligible> eligible(Connection conn) throws SQLException {
        List<Invoice.Eligible> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT s.id, s.code, o.code, s.customer_id, c.name, s.delivered_at, "
                        + "(SELECT COUNT(*) FROM shipment_items i WHERE i.shipment_id = s.id) FROM shipments s "
                        + "JOIN orders o ON o.id = s.order_id JOIN customers c ON c.id = s.customer_id "
                        + "WHERE s.status = 'DELIVERED' AND s.invoice_id IS NULL ORDER BY c.name, s.id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Invoice.Eligible item = new Invoice.Eligible();
                item.setShipmentId(rs.getLong(1));
                item.setShipmentCode(rs.getString(2));
                item.setOrderCode(rs.getString(3));
                item.setCustomerId(rs.getLong(4));
                item.setCustomerName(rs.getString(5));
                item.setDeliveredAt(time(rs.getTimestamp(6)));
                item.setItemCount(rs.getInt(7));
                list.add(item);
            }
        }
        return list;
    }

    public ShipmentLock lockShipment(Connection conn, long id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, customer_id, order_id, status, invoice_id FROM shipments WHERE id = ? FOR UPDATE")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                long orderId = rs.getLong(3);
                Long order = rs.wasNull() ? null : orderId;
                long invoiceId = rs.getLong(5);
                Long invoice = rs.wasNull() ? null : invoiceId;
                return new ShipmentLock(rs.getLong(1), rs.getLong(2), order, rs.getString(4), invoice);
            }
        }
    }

    /** Hàng thực giao của phiếu, quy về đơn vị cơ sở, kèm giá đã chốt của chính sản phẩm đó trên đơn. */
    public List<Delivered> delivered(Connection conn, long shipmentId, long orderId) throws SQLException {
        List<Delivered> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT i.product_id, SUM(i.quantity * CASE WHEN i.unit_id = p.base_unit_id THEN 1 ELSE "
                        + "(SELECT pu.factor FROM product_units pu WHERE pu.product_id = i.product_id AND pu.unit_id = i.unit_id) END), "
                        + "(SELECT SUM(l.gross_amount - COALESCE(l.discount_amount, 0)) FROM order_lines l "
                        + "WHERE l.order_id = ? AND l.product_id = i.product_id), "
                        + "(SELECT SUM(l.base_quantity) FROM order_lines l WHERE l.order_id = ? AND l.product_id = i.product_id) "
                        + "FROM shipment_items i JOIN products p ON p.id = i.product_id WHERE i.shipment_id = ? "
                        + "GROUP BY i.product_id ORDER BY i.product_id")) {
            ps.setLong(1, orderId);
            ps.setLong(2, orderId);
            ps.setLong(3, shipmentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Delivered(rs.getLong(1), rs.getBigDecimal(2), rs.getBigDecimal(3), rs.getBigDecimal(4)));
                }
            }
        }
        return list;
    }

    /** Lấy số kế tiếp của dãy, khoá dòng tới hết transaction để hai người phát hành cùng lúc không trùng số. */
    public long nextNumber(Connection conn, String name) throws SQLException {
        long value;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT last_number FROM document_sequences WHERE name = ? FOR UPDATE")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Chưa khai báo dãy số chứng từ " + name + ".");
                }
                value = rs.getLong(1) + 1;
            }
        }
        try (PreparedStatement ps = conn.prepareStatement("UPDATE document_sequences SET last_number = ? WHERE name = ?")) {
            ps.setLong(1, value);
            ps.setString(2, name);
            ps.executeUpdate();
        }
        return value;
    }

    public Long portalUser(Connection conn, long customerId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT user_id FROM customer_accounts WHERE customer_id = ?")) {
            ps.setLong(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : null;
            }
        }
    }

    public long insert(Connection conn, String code, long customerId, Long recipientUserId, BigDecimal total,
                       LocalDateTime issuedAt, LocalDate dueDate, long actorId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO invoices (code, customer_id, recipient_user_id, total_amount, issued_at, due_date, status, issued_by) "
                        + "VALUES (?, ?, ?, ?, ?, ?, 'ISSUED', ?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, code);
            ps.setLong(2, customerId);
            if (recipientUserId == null) {
                ps.setNull(3, Types.BIGINT);
            } else {
                ps.setLong(3, recipientUserId);
            }
            ps.setBigDecimal(4, total);
            ps.setTimestamp(5, Timestamp.valueOf(issuedAt));
            ps.setDate(6, Date.valueOf(dueDate));
            ps.setLong(7, actorId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Không lấy được ID hoá đơn.");
                }
                return keys.getLong(1);
            }
        }
    }

    public void insertLine(Connection conn, long invoiceId, long shipmentId, long productId, BigDecimal quantity,
                           BigDecimal unitPrice, BigDecimal amount) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO invoice_lines (invoice_id, shipment_id, product_id, quantity, unit_price, amount) "
                        + "VALUES (?, ?, ?, ?, ?, ?)")) {
            ps.setLong(1, invoiceId);
            ps.setLong(2, shipmentId);
            ps.setLong(3, productId);
            ps.setBigDecimal(4, quantity);
            ps.setBigDecimal(5, unitPrice);
            ps.setBigDecimal(6, amount);
            ps.executeUpdate();
        }
    }

    public void linkShipment(Connection conn, long shipmentId, long invoiceId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE shipments SET invoice_id = ? WHERE id = ? AND invoice_id IS NULL")) {
            ps.setLong(1, invoiceId);
            ps.setLong(2, shipmentId);
            if (ps.executeUpdate() != 1) {
                throw new SQLException("Phiếu xuất đã được lập hoá đơn.");
            }
        }
    }

    /** Khoản phải thu của hoá đơn; dư nợ và hạn mức của đại lý (OrderCreditDao) đọc từ bảng này. */
    public void insertReceivable(Connection conn, long customerId, String code, LocalDate issuedOn, BigDecimal amount)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO customer_receivables (customer_id, document_code, issued_on, outstanding) VALUES (?, ?, ?, ?)")) {
            ps.setLong(1, customerId);
            ps.setString(2, code);
            ps.setDate(3, Date.valueOf(issuedOn));
            ps.setBigDecimal(4, amount);
            ps.executeUpdate();
        }
    }

    public Invoice find(Connection conn, long id) throws SQLException {
        List<Invoice> list = select(conn, " WHERE i.id = ?", id, 1);
        if (list.isEmpty()) {
            return null;
        }
        Invoice invoice = list.get(0);
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT s.code, p.sku, p.name, u.name, l.quantity, l.unit_price, l.amount FROM invoice_lines l "
                        + "JOIN shipments s ON s.id = l.shipment_id JOIN products p ON p.id = l.product_id "
                        + "JOIN units u ON u.id = p.base_unit_id WHERE l.invoice_id = ? ORDER BY l.id")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Invoice.Line line = new Invoice.Line();
                    line.setShipmentCode(rs.getString(1));
                    line.setSku(rs.getString(2));
                    line.setProductName(rs.getString(3));
                    line.setBaseUnit(rs.getString(4));
                    line.setQuantity(plain(rs.getBigDecimal(5)));
                    line.setUnitPrice(rs.getBigDecimal(6));
                    line.setAmount(rs.getBigDecimal(7));
                    invoice.getLines().add(line);
                }
            }
        }
        invoice.getPayments().addAll(new PaymentDao().allocationsOf(conn, id));
        return invoice;
    }

    /** Tổng hợp cho thẻ số liệu: số hoá đơn, tổng giá trị, đã thu, còn phải thu và số hoá đơn quá hạn (trừ đã huỷ). */
    public record Summary(long count, BigDecimal total, BigDecimal paid, BigDecimal outstanding, long overdue) {
        public long getCount() { return count; }
        public BigDecimal getTotal() { return total; }
        public BigDecimal getPaid() { return paid; }
        public BigDecimal getOutstanding() { return outstanding; }
        public long getOverdue() { return overdue; }
    }

    /** Công nợ của một đại lý: tổng còn phải thu và phần đã quá hạn. */
    public record Debtor(String code, String name, BigDecimal outstanding, BigDecimal overdue, LocalDate oldestDue) {
        public String getCode() { return code; }
        public String getName() { return name; }
        public BigDecimal getOutstanding() { return outstanding; }
        public BigDecimal getOverdue() { return overdue; }
        public BigDecimal getCurrent() { return outstanding.subtract(overdue); }
        public boolean isLate() { return overdue.signum() > 0; }
        public String getOldestDueText() { return oldestDue == null ? "" : oldestDue.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")); }
    }

    public Summary summary(Connection conn, LocalDate today) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*), COALESCE(SUM(total_amount), 0), COALESCE(SUM(paid_amount + credited_amount), 0), "
                        + "COALESCE(SUM(total_amount - paid_amount - credited_amount), 0), "
                        + "COALESCE(SUM(CASE WHEN status IN ('ISSUED', 'PARTIAL') AND due_date < ? THEN 1 ELSE 0 END), 0) "
                        + "FROM invoices WHERE status <> 'CANCELLED'")) {
            ps.setDate(1, Date.valueOf(today));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new Summary(rs.getLong(1), rs.getBigDecimal(2), rs.getBigDecimal(3), rs.getBigDecimal(4), rs.getLong(5));
            }
        }
    }

    public List<Debtor> debtors(Connection conn, LocalDate today) throws SQLException {
        List<Debtor> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT c.code, c.name, SUM(i.total_amount - i.paid_amount - i.credited_amount), "
                        + "SUM(CASE WHEN i.due_date < ? THEN i.total_amount - i.paid_amount - i.credited_amount ELSE 0 END), "
                        + "MIN(CASE WHEN i.due_date < ? THEN i.due_date END) "
                        + "FROM invoices i JOIN customers c ON c.id = i.customer_id WHERE i.status IN ('ISSUED', 'PARTIAL') "
                        + "GROUP BY c.id, c.code, c.name ORDER BY 4 DESC, 3 DESC")) {
            ps.setDate(1, Date.valueOf(today));
            ps.setDate(2, Date.valueOf(today));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date oldest = rs.getDate(5);
                    list.add(new Debtor(rs.getString(1), rs.getString(2), rs.getBigDecimal(3), rs.getBigDecimal(4),
                            oldest == null ? null : oldest.toLocalDate()));
                }
            }
        }
        return list;
    }

    public List<Invoice> recent(Connection conn, int limit) throws SQLException {
        return select(conn, "", null, limit);
    }

    /** Hoá đơn chưa huỷ của một đại lý, mới nhất trước. */
    public List<Invoice> forCustomer(Connection conn, long customerId) throws SQLException {
        return select(conn, " WHERE i.customer_id = ? AND i.status <> 'CANCELLED'", customerId, 500);
    }

    private List<Invoice> select(Connection conn, String where, Long id, int limit) throws SQLException {
        List<Invoice> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT i.id, i.code, i.customer_id, c.code, c.name, i.total_amount, i.issued_at, i.due_date, i.status, "
                        + "u.full_name, i.paid_amount, i.credited_amount FROM invoices i JOIN customers c ON c.id = i.customer_id "
                        + "LEFT JOIN users u ON u.id = i.issued_by" + where + " ORDER BY i.id DESC LIMIT ?")) {
            int index = 1;
            if (id != null) {
                ps.setLong(index++, id);
            }
            ps.setInt(index, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Invoice invoice = new Invoice();
                    invoice.setId(rs.getLong(1));
                    invoice.setCode(rs.getString(2));
                    invoice.setCustomerId(rs.getLong(3));
                    invoice.setCustomerCode(rs.getString(4));
                    invoice.setCustomerName(rs.getString(5));
                    invoice.setTotalAmount(rs.getBigDecimal(6));
                    invoice.setIssuedAt(time(rs.getTimestamp(7)));
                    Date due = rs.getDate(8);
                    invoice.setDueDate(due == null ? null : due.toLocalDate());
                    invoice.setStatus(Invoice.Status.valueOf(rs.getString(9)));
                    invoice.setIssuedByName(rs.getString(10));
                    invoice.setPaidAmount(rs.getBigDecimal(11));
                    invoice.setCreditedAmount(rs.getBigDecimal(12));
                    list.add(invoice);
                }
            }
        }
        return list;
    }

    private static LocalDateTime time(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    /** Bỏ số 0 thừa mà không ra dạng mũ (2.4E+1). */
    private static BigDecimal plain(BigDecimal value) {
        return new BigDecimal(value.stripTrailingZeros().toPlainString());
    }
}
