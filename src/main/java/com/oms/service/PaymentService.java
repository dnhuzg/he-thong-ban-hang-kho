package com.oms.service;

import com.oms.config.Db;
import com.oms.dao.AuditLogDao;
import com.oms.dao.CustomerDao;
import com.oms.dao.InvoiceDao;
import com.oms.dao.LookupDao;
import com.oms.dao.PaymentDao;
import com.oms.model.Customer;
import com.oms.model.NamedItem;
import com.oms.model.Payment;
import com.oms.model.Role;
import com.oms.model.User;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Ghi nhận thanh toán (S7-02): đối trừ vào hoá đơn chỉ định hoặc tự động theo hoá đơn cũ nhất trước,
 * cho phép thu một phần, không thu vượt công nợ còn lại. Phiếu thu giảm khoản phải thu trong cùng transaction
 * nên dư nợ và hạn mức khả dụng cập nhật ngay. Đã ghi nhận thì không xoá.
 */
public class PaymentService {

    private static final int RECENT = 30;

    public record Form(Long customerId, Long invoiceId, BigDecimal amount, Payment.Method method, LocalDate paidOn,
                       Long collectedBy, String note) {
    }

    /** Đại lý còn nợ kèm tổng số còn phải thu, cho ô chọn đại lý. */
    public record Debtor(long id, String name, BigDecimal outstanding) {
        public long getId() { return id; }
        public String getName() { return name; }
        public BigDecimal getOutstanding() { return outstanding; }
    }

    public record Page(List<Debtor> debtors, List<Payment.OpenInvoice> openInvoices, List<NamedItem> collectors,
                       List<Payment> recent) {
        public List<Debtor> getDebtors() { return debtors; }
        public List<Payment.OpenInvoice> getOpenInvoices() { return openInvoices; }
        public List<NamedItem> getCollectors() { return collectors; }
        public List<Payment> getRecent() { return recent; }
        public Payment.Method[] getMethods() { return Payment.Method.values(); }
    }

    private final PaymentDao dao = new PaymentDao();
    private final InvoiceDao invoiceDao = new InvoiceDao();
    private final AuditLogDao auditDao = new AuditLogDao();

    public Page page(User viewer) throws SQLException {
        requireAccountant(viewer);
        try (Connection conn = Db.getConnection()) {
            List<Payment.OpenInvoice> open = dao.openInvoices(conn, null, false);
            Map<Long, Debtor> debtors = new LinkedHashMap<>();
            for (Payment.OpenInvoice invoice : open) {
                debtors.merge(invoice.getCustomerId(),
                        new Debtor(invoice.getCustomerId(), invoice.getCustomerName(), invoice.getOutstanding()),
                        (a, b) -> new Debtor(a.id(), a.name(), a.outstanding().add(b.outstanding())));
            }
            return new Page(new ArrayList<>(debtors.values()), open, new LookupDao().users(conn), dao.recent(conn, RECENT));
        }
    }

    /** Trả về ID phiếu thu vừa ghi nhận. */
    public long record(Form form, User actor) throws SQLException {
        requireAccountant(actor);
        if (form.customerId() == null) {
            throw new ServiceException("Chọn đại lý nộp tiền.");
        }
        if (form.amount() == null || form.amount().signum() <= 0) {
            throw new ServiceException("Số tiền thu phải lớn hơn 0.");
        }
        if (form.amount().stripTrailingZeros().scale() > 0) {
            throw new ServiceException("Số tiền thu tính theo đồng, không có phần lẻ.");
        }
        if (form.method() == null) {
            throw new ServiceException("Chọn hình thức thu.");
        }
        if (form.paidOn() == null) {
            throw new ServiceException("Chọn ngày thu.");
        }
        if (form.paidOn().isAfter(LocalDate.now())) {
            throw new ServiceException("Ngày thu không được sau hôm nay.");
        }
        if (form.collectedBy() == null) {
            throw new ServiceException("Chọn người thu.");
        }
        String note = form.note() == null || form.note().isBlank() ? null : form.note().strip();
        if (note != null && note.length() > 500) {
            throw new ServiceException("Ghi chú tối đa 500 ký tự.");
        }
        return Db.inTransaction(conn -> {
            Customer customer = new CustomerDao().findByIdForUpdate(conn, form.customerId());
            if (customer == null) {
                throw new ServiceException("Không tìm thấy đại lý.");
            }
            if (!dao.isActiveUser(conn, form.collectedBy())) {
                throw new ServiceException("Người thu không tồn tại hoặc đã bị khoá.");
            }
            List<Payment.OpenInvoice> open = dao.openInvoices(conn, form.customerId(), true);
            BigDecimal totalDebt = open.stream().map(Payment.OpenInvoice::getOutstanding)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (form.amount().compareTo(totalDebt) > 0) {
                throw new ServiceException("Không thu vượt tổng công nợ còn lại của đại lý (" + money(totalDebt) + " ₫).");
            }
            List<Payment.OpenInvoice> targets = open;
            if (form.invoiceId() != null) {
                targets = open.stream().filter(i -> i.getId() == form.invoiceId()).toList();
                if (targets.isEmpty()) {
                    throw new ServiceException("Hoá đơn được chọn không thuộc đại lý này hoặc đã thanh toán xong.");
                }
                if (form.amount().compareTo(targets.get(0).getOutstanding()) > 0) {
                    throw new ServiceException("Số tiền vượt số còn nợ của hoá đơn " + targets.get(0).getCode() + " ("
                            + money(targets.get(0).getOutstanding()) + " ₫).");
                }
            }

            String code = String.format("PT%06d", invoiceDao.nextNumber(conn, "PAYMENT"));
            long paymentId = dao.insert(conn, code, form.customerId(), form.amount(), form.method(), form.paidOn(),
                    form.collectedBy(), note, actor.getId());
            // Đối trừ lần lượt từ hoá đơn cũ nhất (hoặc chỉ hoá đơn đã chọn) cho tới hết số tiền
            BigDecimal remaining = form.amount();
            List<String> applied = new ArrayList<>();
            for (Payment.OpenInvoice invoice : targets) {
                if (remaining.signum() == 0) {
                    break;
                }
                BigDecimal part = remaining.min(invoice.getOutstanding());
                dao.insertAllocation(conn, paymentId, invoice.getId(), part);
                dao.applyToInvoice(conn, invoice.getId(), part);
                dao.reduceReceivable(conn, invoice.getCode(), part);
                applied.add(invoice.getCode());
                remaining = remaining.subtract(part);
            }
            auditDao.log(conn, actor.getId(), "PAYMENT_RECORD", "PAYMENT", paymentId,
                    code + " · " + customer.getCode() + " · " + money(form.amount()) + " ₫ · đối trừ " + String.join(", ", applied));
            return paymentId;
        });
    }

    private static void requireAccountant(User user) {
        if (!user.isAdmin() && !user.hasRole(Role.ACCOUNTANT)) {
            throw new ForbiddenException("Chỉ kế toán công nợ được ghi nhận thanh toán.");
        }
    }

    private static String money(BigDecimal value) {
        return NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(value);
    }
}
