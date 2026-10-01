package com.oms.service;

import com.oms.config.Db;
import com.oms.dao.AuditLogDao;
import com.oms.dao.CustomerDao;
import com.oms.dao.LockHistoryDao;
import com.oms.model.Customer;
import com.oms.model.LockChange;
import com.oms.model.User;
import com.oms.security.PermissionPolicy;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Khoá / mở giao dịch với đại lý (S3-07), cùng nhóm người với việc sửa hạn mức: Kế toán công nợ, Quản lý kinh doanh,
 * Admin. Đại lý bị khoá không tạo được đơn mới (OrderService gọi {@link #requireTradable}); đơn đang dở vẫn xử lý
 * tiếp được, nơi hiển thị đơn dùng {@link Customer#isLocked()} để cảnh báo.
 */
public class CustomerLockService {

    private static final int HISTORY_SIZE = 10;

    private final CustomerDao customerDao = new CustomerDao();
    private final LockHistoryDao historyDao = new LockHistoryDao();
    private final AuditLogDao auditDao = new AuditLogDao();

    public List<LockChange> history(long customerId) throws SQLException {
        try (Connection conn = Db.getConnection()) {
            return historyDao.list(conn, customerId, HISTORY_SIZE);
        }
    }

    /** Khoá giao dịch: bắt buộc lý do; thu hồi ngay khả năng tạo đơn mới. */
    public void lock(long customerId, String reason, User actor) throws SQLException {
        requireManage(actor);
        String why = reason == null ? "" : reason.trim();
        if (why.isEmpty()) {
            throw new ServiceException("Vui lòng nhập lý do khoá giao dịch.");
        }
        if (why.length() > 500) {
            throw new ServiceException("Lý do tối đa 500 ký tự.");
        }
        Db.inTransaction(conn -> {
            Customer c = customerDao.findByIdForUpdate(conn, customerId);
            if (c == null) {
                throw new CustomerService.NotFoundException();
            }
            if (c.isLocked()) {
                throw new ServiceException("Giao dịch với đại lý này đã bị khoá.");
            }
            customerDao.lock(conn, customerId, why, actor.getId());
            historyDao.insert(conn, customerId, true, why, actor.getId());
            auditDao.log(conn, actor.getId(), "CUSTOMER_LOCK", "CUSTOMER", customerId, "Lý do: " + why,
                    "Trạng thái: " + Customer.Status.ACTIVE.getLabel(), "Trạng thái: " + Customer.Status.LOCKED.getLabel());
            return null;
        });
    }

    /** Mở lại giao dịch; lý do không bắt buộc nhưng nên ghi (ví dụ: đã thanh toán công nợ). */
    public void unlock(long customerId, String reason, User actor) throws SQLException {
        requireManage(actor);
        String why = reason == null ? "" : reason.trim();
        if (why.length() > 500) {
            throw new ServiceException("Lý do tối đa 500 ký tự.");
        }
        Db.inTransaction(conn -> {
            Customer c = customerDao.findByIdForUpdate(conn, customerId);
            if (c == null) {
                throw new CustomerService.NotFoundException();
            }
            if (!c.isLocked()) {
                throw new ServiceException("Đại lý này đang giao dịch bình thường.");
            }
            customerDao.unlock(conn, customerId);
            historyDao.insert(conn, customerId, false, why.isEmpty() ? null : why, actor.getId());
            auditDao.log(conn, actor.getId(), "CUSTOMER_UNLOCK", "CUSTOMER", customerId, why.isEmpty() ? null : "Lý do: " + why,
                    "Trạng thái: " + Customer.Status.LOCKED.getLabel() + "\nLý do khoá: " + c.getLockReason(),
                    "Trạng thái: " + Customer.Status.ACTIVE.getLabel());
            return null;
        });
    }

    /** Dùng khi tạo đơn mới (S3-09, cổng đặt hàng sau này): đại lý bị khoá không được tạo đơn. */
    public static void requireTradable(Customer customer) {
        if (customer.isLocked()) {
            throw new ServiceException("Đại lý " + customer.getCode() + " đang bị khoá giao dịch"
                    + (customer.getLockReason() == null ? "" : " (" + customer.getLockReason() + ")") + " nên không tạo được đơn mới.");
        }
    }

    private static void requireManage(User user) {
        if (!PermissionPolicy.canManageCredit(user.getRoles())) {
            throw new ForbiddenException("Chỉ Kế toán công nợ và Quản lý kinh doanh được khoá hoặc mở giao dịch với đại lý.");
        }
    }
}
