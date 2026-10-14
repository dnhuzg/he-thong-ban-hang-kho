package com.oms.dao;

import com.oms.model.Payment;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Phiếu thu và phân bổ vào hoá đơn (S7-02). */
public class PaymentDao {

    /**
     * Hoá đơn còn nợ, cũ nhất trước (thứ tự đối trừ tự động). customerId = null lấy mọi đại lý;
     * lock = true khoá các hoá đơn tới hết transaction ghi nhận thu tiền.
     */
    public List<Payment.OpenInvoice> openInvoices(Connection conn, Long customerId, boolean lock) throws SQLException {
        List<Payment.OpenInvoice> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT i.id, i.code, i.customer_id, c.name, i.total_amount - i.paid_amount, i.due_date "
                        + "FROM invoices i JOIN customers c ON c.id = i.customer_id "
                        + "WHERE i.status IN ('ISSUED', 'PARTIAL')" + (customerId == null ? "" : " AND i.customer_id = ?")
                        + " ORDER BY " + (customerId == null ? "c.name, " : "") + "i.issued_at, i.id"
                        + (lock ? " FOR UPDATE" : ""))) {
            if (customerId != null) {
                ps.setLong(1, customerId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Payment.OpenInvoice invoice = new Payment.OpenInvoice();
                    invoice.setId(rs.getLong(1));
                    invoice.setCode(rs.getString(2));
                    invoice.setCustomerId(rs.getLong(3));
                    invoice.setCustomerName(rs.getString(4));
                    invoice.setOutstanding(rs.getBigDecimal(5));
                    Date due = rs.getDate(6);
                    invoice.setDueDate(due == null ? null : due.toLocalDate());
                    list.add(invoice);
                }
            }
        }
        return list;
    }

    public boolean isActiveUser(Connection conn, long userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM users WHERE id = ? AND status = 'ACTIVE'")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public long insert(Connection conn, String code, long customerId, BigDecimal amount, Payment.Method method,
                       LocalDate paidOn, long collectedBy, String note, long actorId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO payments (code, customer_id, amount, method, paid_on, collected_by, note, created_by) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, code);
            ps.setLong(2, customerId);
            ps.setBigDecimal(3, amount);
            ps.setString(4, method.name());
            ps.setDate(5, Date.valueOf(paidOn));
            ps.setLong(6, collectedBy);
            if (note == null) {
                ps.setNull(7, Types.VARCHAR);
            } else {
                ps.setString(7, note);
            }
            ps.setLong(8, actorId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Không lấy được ID phiếu thu.");
                }
                return keys.getLong(1);
            }
        }
    }

    public void insertAllocation(Connection conn, long paymentId, long invoiceId, BigDecimal amount) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO payment_allocations (payment_id, invoice_id, amount) VALUES (?, ?, ?)")) {
            ps.setLong(1, paymentId);
            ps.setLong(2, invoiceId);
            ps.setBigDecimal(3, amount);
            ps.executeUpdate();
        }
    }

    /** Cộng số đã thu vào hoá đơn và chuyển Thanh toán một phần / Đã thanh toán. */
    public void applyToInvoice(Connection conn, long invoiceId, BigDecimal amount) throws SQLException {
        // Gán trạng thái trước paid_amount: MySQL tính các phép gán lần lượt bằng giá trị mới
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE invoices SET status = CASE WHEN paid_amount + ? >= total_amount THEN 'PAID' ELSE 'PARTIAL' END, "
                        + "paid_at = CASE WHEN paid_amount + ? >= total_amount THEN CURRENT_TIMESTAMP ELSE paid_at END, "
                        + "paid_amount = paid_amount + ? "
                        + "WHERE id = ? AND status IN ('ISSUED', 'PARTIAL') AND paid_amount + ? <= total_amount")) {
            ps.setBigDecimal(1, amount);
            ps.setBigDecimal(2, amount);
            ps.setBigDecimal(3, amount);
            ps.setLong(4, invoiceId);
            ps.setBigDecimal(5, amount);
            if (ps.executeUpdate() != 1) {
                throw new SQLException("Hoá đơn #" + invoiceId + " không còn đủ số nợ để đối trừ.");
            }
        }
    }

    /** Giảm khoản phải thu của hoá đơn; dư nợ và hạn mức khả dụng của đại lý cập nhật theo. */
    public void reduceReceivable(Connection conn, String invoiceCode, BigDecimal amount) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE customer_receivables SET outstanding = outstanding - ? WHERE document_code = ? AND outstanding >= ?")) {
            ps.setBigDecimal(1, amount);
            ps.setString(2, invoiceCode);
            ps.setBigDecimal(3, amount);
            if (ps.executeUpdate() != 1) {
                throw new SQLException("Khoản phải thu của hoá đơn " + invoiceCode + " không khớp với hoá đơn.");
            }
        }
    }

    public List<Payment> recent(Connection conn, int limit) throws SQLException {
        List<Payment> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT p.id, p.code, c.name, p.amount, p.method, p.paid_on, u.full_name, p.note, "
                        + "(SELECT GROUP_CONCAT(i.code ORDER BY i.id SEPARATOR ', ') FROM payment_allocations a "
                        + "JOIN invoices i ON i.id = a.invoice_id WHERE a.payment_id = p.id) "
                        + "FROM payments p JOIN customers c ON c.id = p.customer_id JOIN users u ON u.id = p.collected_by "
                        + "ORDER BY p.id DESC LIMIT ?")) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Payment payment = new Payment();
                    payment.setId(rs.getLong(1));
                    payment.setCode(rs.getString(2));
                    payment.setCustomerName(rs.getString(3));
                    payment.setAmount(rs.getBigDecimal(4));
                    payment.setMethod(Payment.Method.valueOf(rs.getString(5)));
                    payment.setPaidOn(rs.getDate(6).toLocalDate());
                    payment.setCollectedByName(rs.getString(7));
                    payment.setNote(rs.getString(8));
                    payment.setInvoiceCodes(rs.getString(9));
                    list.add(payment);
                }
            }
        }
        return list;
    }

    public List<Payment.Allocation> allocationsOf(Connection conn, long invoiceId) throws SQLException {
        List<Payment.Allocation> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT p.code, p.paid_on, p.method, a.amount FROM payment_allocations a "
                        + "JOIN payments p ON p.id = a.payment_id WHERE a.invoice_id = ? ORDER BY a.id")) {
            ps.setLong(1, invoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Payment.Allocation allocation = new Payment.Allocation();
                    allocation.setPaymentCode(rs.getString(1));
                    allocation.setPaidOn(rs.getDate(2).toLocalDate());
                    allocation.setMethod(Payment.Method.valueOf(rs.getString(3)));
                    allocation.setAmount(rs.getBigDecimal(4));
                    list.add(allocation);
                }
            }
        }
        return list;
    }
}
