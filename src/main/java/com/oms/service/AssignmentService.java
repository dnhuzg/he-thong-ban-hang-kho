package com.oms.service;

import com.oms.config.Db;
import com.oms.dao.AssignmentDao;
import com.oms.dao.AuditLogDao;
import com.oms.dao.CustomerDao;
import com.oms.dao.LookupDao;
import com.oms.model.AssignmentChange;
import com.oms.model.Customer;
import com.oms.model.NamedItem;
import com.oms.model.User;
import com.oms.security.PermissionPolicy;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Chuyển giao người phụ trách đại lý hàng loạt khi nhân viên nghỉ (S3-06), chỉ Quản lý kinh doanh (và Admin).
 * Mỗi đại lý được chuyển ghi một dòng lịch sử; cả lô ghi một dòng nhật ký.
 */
public class AssignmentService {

    public static final int HISTORY_SIZE = 10;
    public static final int RECENT_SIZE = 20;
    public static final int MAX_BATCH = 500;

    private final CustomerDao customerDao = new CustomerDao();
    private final AssignmentDao assignmentDao = new AssignmentDao();
    private final LookupDao lookupDao = new LookupDao();
    private final AuditLogDao auditDao = new AuditLogDao();

    /** Nhân viên đang phụ trách đại lý, kèm số đại lý, để chọn người cần chuyển giao. */
    public List<CustomerDao.RepLoad> repsWithCustomers(User actor) throws SQLException {
        requireAssign(actor);
        try (Connection conn = Db.getConnection()) {
            return customerDao.repsWithCustomers(conn);
        }
    }

    /** Người nhận bàn giao: nhân viên kinh doanh / quản lý kinh doanh còn hoạt động. */
    public List<NamedItem> targetReps(User actor) throws SQLException {
        requireAssign(actor);
        try (Connection conn = Db.getConnection()) {
            return lookupDao.salesReps(conn);
        }
    }

    public List<NamedItem> areas(User actor) throws SQLException {
        requireAssign(actor);
        try (Connection conn = Db.getConnection()) {
            return lookupDao.areas(conn);
        }
    }

    /** Các đại lý của một nhân viên, có thể giới hạn theo khu vực (địa bàn). */
    public List<Customer> customersOf(long repId, Long areaId, User actor) throws SQLException {
        requireAssign(actor);
        try (Connection conn = Db.getConnection()) {
            return customerDao.search(conn, new CustomerDao.Filter(null, null, areaId, repId, null, null), 1, MAX_BATCH).getItems();
        }
    }

    public List<AssignmentChange> historyOf(long customerId) throws SQLException {
        try (Connection conn = Db.getConnection()) {
            return assignmentDao.listByCustomer(conn, customerId, HISTORY_SIZE);
        }
    }

    public List<AssignmentChange> recent(User actor) throws SQLException {
        requireAssign(actor);
        try (Connection conn = Db.getConnection()) {
            return assignmentDao.listRecent(conn, RECENT_SIZE);
        }
    }

    /** Trả về số đại lý đã chuyển. Mọi đại lý trong lô phải đang thuộc nhân viên cần chuyển, nếu không cả lô bị từ chối. */
    public int transfer(long fromRepId, long toRepId, Collection<Long> customerIds, String reason, User actor) throws SQLException {
        requireAssign(actor);
        Set<Long> ids = new LinkedHashSet<>(customerIds);
        if (ids.isEmpty()) {
            throw new ServiceException("Vui lòng chọn ít nhất một đại lý để chuyển giao.");
        }
        if (ids.size() > MAX_BATCH) {
            throw new ServiceException("Mỗi lần chuyển giao tối đa " + MAX_BATCH + " đại lý.");
        }
        if (fromRepId == toRepId) {
            throw new ServiceException("Người nhận bàn giao phải khác người đang phụ trách.");
        }
        String why = reason == null ? "" : reason.trim();
        if (why.isEmpty()) {
            throw new ServiceException("Vui lòng nhập lý do chuyển giao (ví dụ: nhân viên nghỉ việc).");
        }
        if (why.length() > 500) {
            throw new ServiceException("Lý do tối đa 500 ký tự.");
        }
        return Db.inTransaction(conn -> {
            if (lookupDao.salesReps(conn).stream().noneMatch(r -> r.getId() == toRepId)) {
                throw new ServiceException("Người nhận bàn giao phải là nhân viên kinh doanh đang hoạt động.");
            }
            List<String> codes = new ArrayList<>();
            for (long id : ids) {
                Customer c = customerDao.findByIdForUpdate(conn, id);
                if (c == null || c.getSalesRepId() == null || c.getSalesRepId() != fromRepId) {
                    throw new ServiceException("Đại lý " + (c == null ? "#" + id : c.getCode())
                            + " không còn thuộc nhân viên cần chuyển. Vui lòng tải lại trang.");
                }
                customerDao.updateSalesRep(conn, id, toRepId);
                assignmentDao.insert(conn, id, fromRepId, toRepId, why, actor.getId());
                codes.add(c.getCode());
            }
            auditDao.log(conn, actor.getId(), "CUSTOMER_TRANSFER", "USER", fromRepId, why,
                    "Từ: " + nameOf(conn, fromRepId), "Sang: " + nameOf(conn, toRepId) + "\nSố đại lý: " + codes.size()
                            + "\nĐại lý: " + String.join(", ", codes.subList(0, Math.min(codes.size(), 30)))
                            + (codes.size() > 30 ? ", ..." : ""));
            return ids.size();
        });
    }

    private String nameOf(Connection conn, long userId) throws SQLException {
        return lookupDao.users(conn).stream().filter(u -> u.getId() == userId).map(NamedItem::getName).findFirst().orElse("#" + userId);
    }

    private static void requireAssign(User user) {
        if (!PermissionPolicy.canAssignCustomers(user.getRoles())) {
            throw new ForbiddenException("Chỉ Quản lý kinh doanh và Quản trị hệ thống được chuyển giao đại lý.");
        }
    }
}
