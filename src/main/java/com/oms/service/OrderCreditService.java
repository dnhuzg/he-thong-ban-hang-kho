package com.oms.service;

import com.oms.config.Db;
import com.oms.dao.CustomerDao;
import com.oms.dao.OrderCreditDao;
import com.oms.model.Customer;
import com.oms.model.User;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;

/** S4-02: kiểm lại dưới khoá đại lý ở thời điểm chốt; vượt hạn mức cần duyệt, quá ngày nợ bị chặn. */
public class OrderCreditService {
    public record Quote(BigDecimal currentDebt, BigDecimal committed, BigDecimal limit, BigDecimal remaining,
                        BigDecimal orderTotal, BigDecimal exceededBy, boolean overdue, boolean requiresApproval) { }
    private final OrderCreditDao dao = new OrderCreditDao();

    public Quote quote(long orderId, User viewer) throws SQLException {
        var order = new OrderService().get(orderId, viewer);
        try (Connection conn = Db.getConnection()) {
            Customer customer = new CustomerDao().findById(conn, order.getCustomerId());
            return evaluate(conn, customer, orderId, order.getPayable());
        }
    }
    public Quote evaluate(Connection conn, Customer customer, long orderId, BigDecimal total) throws SQLException {
        var balance = dao.balance(conn, customer.getId(), orderId, customer.getCreditDays(), LocalDate.now());
        BigDecimal remaining = customer.getCreditLimit().subtract(balance.debt()).subtract(balance.committed());
        BigDecimal excess = total.subtract(remaining).max(BigDecimal.ZERO);
        return new Quote(balance.debt(), balance.committed(), customer.getCreditLimit(), remaining, total,
                excess, balance.overdue(), excess.signum() > 0);
    }
    public void requireNotOverdue(Connection conn, Customer customer) throws SQLException {
        if (evaluate(conn, customer, 0, BigDecimal.ZERO).overdue()) {
            throw new ServiceException("Đại lý có khoản nợ quá số ngày cho phép, không thể tạo hoặc chốt đơn.");
        }
    }
}
