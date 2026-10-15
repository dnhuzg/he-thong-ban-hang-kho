package com.oms.service;

import com.oms.config.Db;
import com.oms.dao.AuditLogDao;
import com.oms.dao.CreditHistoryDao;
import com.oms.dao.CustomerDao;
import com.oms.model.CreditChange;
import com.oms.model.Customer;
import com.oms.model.User;
import com.oms.security.PermissionPolicy;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Hạn mức công nợ và số ngày nợ của đại lý (S3-05). Chỉ Kế toán công nợ, Quản lý kinh doanh (và Admin) sửa được;
 * mọi lần đổi bắt buộc có lý do, ghi vào lịch sử (chỉ thêm) và nhật ký thao tác. EP-04 dùng hai số này khi chốt đơn.
 */
public class CreditService {

    public static final int MAX_DAYS = 365;
    private static final int HISTORY_SIZE = 10;

    private final CustomerDao customerDao = new CustomerDao();
    private final CreditHistoryDao historyDao = new CreditHistoryDao();
    private final AuditLogDao auditDao = new AuditLogDao();

    /** Lịch sử gần đây của một đại lý; người xem phải thấy được đại lý đó (kiểm bằng CustomerService.get trước). */
    public List<CreditChange> history(long customerId) throws SQLException {
        try (Connection conn = Db.getConnection()) {
            return historyDao.list(conn, customerId, HISTORY_SIZE);
        }
    }

    /** Các lần đổi hạn mức mới nhất của mọi đại lý; chỉ người được sửa hạn mức mới xem toàn bộ. */
    public List<CreditChange> recentChanges(User viewer, int limit) throws SQLException {
        if (!PermissionPolicy.canManageCredit(viewer.getRoles())) {
            throw new ForbiddenException("Chỉ Kế toán công nợ và Quản lý kinh doanh được xem lịch sử hạn mức.");
        }
        try (Connection conn = Db.getConnection()) {
            return historyDao.recent(conn, limit);
        }
    }

    public void update(long customerId, String limitText, String daysText, String reason, User actor) throws SQLException {
        if (!PermissionPolicy.canManageCredit(actor.getRoles())) {
            throw new ForbiddenException("Chỉ Kế toán công nợ và Quản lý kinh doanh được sửa hạn mức công nợ.");
        }
        BigDecimal limit = Money.parse(limitText, "Hạn mức công nợ");
        if (limit == null) {
            throw new ServiceException("Vui lòng nhập hạn mức công nợ (nhập 0 nếu không cho nợ).");
        }
        int days = parseDays(daysText);
        String why = reason == null ? "" : reason.trim();
        if (why.isEmpty()) {
            throw new ServiceException("Vui lòng nhập lý do thay đổi hạn mức.");
        }
        if (why.length() > 500) {
            throw new ServiceException("Lý do tối đa 500 ký tự.");
        }
        Db.inTransaction(conn -> {
            Customer c = customerDao.findByIdForUpdate(conn, customerId);
            if (c == null) {
                throw new CustomerService.NotFoundException();
            }
            if (c.getCreditLimit().compareTo(limit) == 0 && c.getCreditDays() == days) {
                throw new ServiceException("Hạn mức và số ngày nợ không thay đổi so với hiện tại.");
            }
            customerDao.updateCredit(conn, customerId, limit, days);
            historyDao.insert(conn, customerId, c.getCreditLimit(), limit, c.getCreditDays(), days, why, actor.getId());
            auditDao.log(conn, actor.getId(), "CUSTOMER_CREDIT_UPDATE", "CUSTOMER", customerId, "Lý do: " + why,
                    describe(c.getCreditLimit(), c.getCreditDays()), describe(limit, days));
            return null;
        });
    }

    private static int parseDays(String raw) {
        String v = raw == null ? "" : raw.trim();
        if (v.isEmpty()) {
            throw new ServiceException("Vui lòng nhập số ngày nợ tối đa (nhập 0 nếu phải thanh toán ngay).");
        }
        try {
            int days = Integer.parseInt(v);
            if (days < 0 || days > MAX_DAYS) {
                throw new NumberFormatException();
            }
            return days;
        } catch (NumberFormatException e) {
            throw new ServiceException("Số ngày nợ là số nguyên từ 0 đến " + MAX_DAYS + ".");
        }
    }

    private static String describe(BigDecimal limit, int days) {
        return "Hạn mức: " + Money.text(limit) + " ₫\nSố ngày nợ: " + days;
    }
}
