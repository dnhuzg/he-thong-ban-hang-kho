package com.oms.service;

import com.oms.config.Db;
import com.oms.dao.CustomerDao;
import com.oms.dao.InvoiceDao;
import com.oms.dao.PortalAccountDao;
import com.oms.model.Customer;
import com.oms.model.Invoice;
import com.oms.model.User;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Đại lý xem công nợ của chính mình (S7-07): danh sách hoá đơn kèm số tiền, hạn thanh toán, trạng thái;
 * tổng còn nợ và hạn mức còn lại (đã trừ phần đang giữ cho đơn chưa lập hoá đơn). Đại lý xác định theo
 * tài khoản đăng nhập, không nhận từ request.
 */
public class MyDebtService {

    public record View(Customer customer, List<Invoice> invoices, BigDecimal debt, BigDecimal committed,
                       BigDecimal limit, BigDecimal remaining, BigDecimal overdue) {
        public Customer getCustomer() { return customer; }
        public List<Invoice> getInvoices() { return invoices; }
        public BigDecimal getDebt() { return debt; }
        public BigDecimal getCommitted() { return committed; }
        public BigDecimal getLimit() { return limit; }
        public BigDecimal getRemaining() { return remaining; }
        public BigDecimal getOverdue() { return overdue; }

        /** Phần trăm hạn mức đã dùng (dư nợ + đang giữ chỗ), làm tròn; 0 khi không cấp hạn mức. */
        public int getUsagePercent() {
            return limit.signum() > 0
                    ? debt.add(committed).multiply(BigDecimal.valueOf(100)).divide(limit, 0, RoundingMode.HALF_UP).intValue() : 0;
        }

        public int getUsageStep() { return Math.min(100, getUsagePercent() / 10 * 10); }

        public String getUsageTone() {
            int p = getUsagePercent();
            return p > 90 ? "danger" : p >= 70 ? "warning" : "success";
        }
    }

    public View view(User viewer) throws SQLException {
        try (Connection conn = Db.getConnection()) {
            Long customerId = new PortalAccountDao().customerId(conn, viewer.getId());
            if (customerId == null) {
                throw new ForbiddenException("Tài khoản chưa được liên kết với đại lý nào.");
            }
            Customer customer = new CustomerDao().findById(conn, customerId);
            List<Invoice> invoices = new InvoiceDao().forCustomer(conn, customerId);
            OrderCreditService.Quote quote = new OrderCreditService().evaluate(conn, customer, 0, BigDecimal.ZERO);
            BigDecimal overdue = invoices.stream()
                    .filter(Invoice::isOverdue)
                    .map(Invoice::getOutstanding).reduce(BigDecimal.ZERO, BigDecimal::add);
            return new View(customer, invoices, quote.currentDebt(), quote.committed(), quote.limit(),
                    quote.remaining().max(BigDecimal.ZERO), overdue);
        }
    }
}
