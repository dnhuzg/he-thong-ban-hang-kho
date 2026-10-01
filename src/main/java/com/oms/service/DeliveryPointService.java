package com.oms.service;

import com.oms.config.Db;
import com.oms.dao.AuditLogDao;
import com.oms.dao.CustomerDao;
import com.oms.dao.DeliveryPointDao;
import com.oms.model.Customer;
import com.oms.model.DeliveryPoint;
import com.oms.model.Role;
import com.oms.model.User;
import com.oms.security.PermissionPolicy;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;

/**
 * Điểm giao hàng của đại lý (S3-04). Admin, Quản lý kinh doanh, Kế toán công nợ sửa cho mọi đại lý; Nhân viên kinh doanh
 * chỉ sửa điểm giao của đại lý mình phụ trách. Mỗi đại lý luôn có đúng một điểm mặc định khi đã có điểm giao nào.
 */
public class DeliveryPointService {

    private final CustomerDao customerDao = new CustomerDao();
    private final DeliveryPointDao pointDao = new DeliveryPointDao();
    private final AuditLogDao auditDao = new AuditLogDao();

    public List<DeliveryPoint> list(long customerId, User viewer) throws SQLException {
        try (Connection conn = Db.getConnection()) {
            requireAccess(customerDao.findById(conn, customerId), viewer, false);
            return pointDao.listByCustomer(conn, customerId);
        }
    }

    /** Người được sửa điểm giao của đại lý này (dùng ẩn / hiện nút; server vẫn kiểm lại ở mỗi thao tác ghi). */
    public static boolean canEdit(User user, Customer customer) {
        return PermissionPolicy.canManageCustomers(user.getRoles())
                || (user.getRoles().contains(Role.SALES_REP) && customer.getSalesRepId() != null
                && customer.getSalesRepId() == user.getId());
    }

    /** pointId = 0 là thêm mới. Điểm đầu tiên của đại lý tự thành mặc định; chọn mặc định sẽ bỏ mặc định của điểm khác. */
    public long save(long customerId, long pointId, DeliveryPoint input, User actor) throws SQLException {
        DeliveryPoint p = validated(input);
        return Db.inTransaction(conn -> {
            Customer customer = customerDao.findByIdForUpdate(conn, customerId);
            requireAccess(customer, actor, true);
            p.setCustomerId(customerId);
            long id = pointId;
            String before = null;
            if (pointId == 0) {
                p.setDefaultPoint(input.isDefaultPoint() || pointDao.count(conn, customerId) == 0);
                id = pointDao.insert(conn, p);
            } else {
                DeliveryPoint existing = ownedPoint(conn, customerId, pointId);
                before = describe(existing);
                p.setId(pointId);
                pointDao.update(conn, p);
                p.setDefaultPoint(existing.isDefaultPoint() || input.isDefaultPoint());
            }
            if (p.isDefaultPoint()) {
                pointDao.makeDefault(conn, customerId, id);
            }
            auditDao.log(conn, actor.getId(), "DELIVERY_POINT_SAVE", "CUSTOMER", customerId, customer.getCode(), before, describe(p));
            return id;
        });
    }

    public void makeDefault(long customerId, long pointId, User actor) throws SQLException {
        Db.inTransaction(conn -> {
            Customer customer = customerDao.findByIdForUpdate(conn, customerId);
            requireAccess(customer, actor, true);
            DeliveryPoint p = ownedPoint(conn, customerId, pointId);
            if (!p.isDefaultPoint()) {
                pointDao.makeDefault(conn, customerId, pointId);
                auditDao.log(conn, actor.getId(), "DELIVERY_POINT_DEFAULT", "CUSTOMER", customerId, customer.getCode(),
                        null, "Điểm giao mặc định: " + p.getAddress());
            }
            return null;
        });
    }

    /** Xoá điểm; nếu là điểm mặc định thì điểm cũ nhất còn lại thành mặc định. Điểm đã dùng trong đơn hàng không xoá được. */
    public void delete(long customerId, long pointId, User actor) throws SQLException {
        try {
            Db.inTransaction(conn -> {
                Customer customer = customerDao.findByIdForUpdate(conn, customerId);
                requireAccess(customer, actor, true);
                DeliveryPoint p = ownedPoint(conn, customerId, pointId);
                pointDao.delete(conn, pointId);
                if (p.isDefaultPoint()) {
                    long next = pointDao.firstId(conn, customerId);
                    if (next > 0) {
                        pointDao.makeDefault(conn, customerId, next);
                    }
                }
                auditDao.log(conn, actor.getId(), "DELIVERY_POINT_DELETE", "CUSTOMER", customerId, customer.getCode(), describe(p), null);
                return null;
            });
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new ServiceException("Điểm giao hàng đã được dùng trong đơn hàng nên không xoá được.");
        }
    }

    private static void requireAccess(Customer customer, User user, boolean write) {
        if (!PermissionPolicy.canBrowseCustomers(user.getRoles())) {
            throw new ForbiddenException("Bạn không có quyền xem đại lý.");
        }
        boolean manager = PermissionPolicy.canManageCustomers(user.getRoles());
        boolean own = customer != null && customer.getSalesRepId() != null && customer.getSalesRepId() == user.getId();
        if (customer == null || (!manager && !own)) {
            throw new CustomerService.NotFoundException();
        }
        if (write && !canEdit(user, customer)) {
            throw new ForbiddenException("Bạn không có quyền sửa điểm giao hàng của đại lý này.");
        }
    }

    private DeliveryPoint ownedPoint(Connection conn, long customerId, long pointId) throws SQLException {
        DeliveryPoint p = pointDao.findById(conn, pointId);
        if (p == null || p.getCustomerId() != customerId) {
            throw new ServiceException("Điểm giao hàng không còn tồn tại hoặc không thuộc đại lý này. Vui lòng tải lại trang.");
        }
        return p;
    }

    private static DeliveryPoint validated(DeliveryPoint in) {
        DeliveryPoint p = new DeliveryPoint();
        p.setAddress(required(in.getAddress(), "địa chỉ giao hàng", 255));
        p.setReceiverName(required(in.getReceiverName(), "tên người nhận", 150));
        String phone = PhoneNumbers.normalize(in.getReceiverPhone());
        if (phone == null) {
            throw new ServiceException("Vui lòng nhập số điện thoại người nhận.");
        }
        p.setReceiverPhone(phone);
        String note = in.getRouteNote() == null ? "" : in.getRouteNote().trim();
        if (note.length() > 500) {
            throw new ServiceException("Ghi chú đường đi tối đa 500 ký tự.");
        }
        p.setRouteNote(note.isEmpty() ? null : note);
        return p;
    }

    private static String required(String raw, String label, int max) {
        String v = raw == null ? "" : raw.trim();
        if (v.isEmpty()) {
            throw new ServiceException("Vui lòng nhập " + label + ".");
        }
        if (v.length() > max) {
            throw new ServiceException("Độ dài " + label + " tối đa " + max + " ký tự.");
        }
        return v;
    }

    private static String describe(DeliveryPoint p) {
        return "Địa chỉ: " + p.getAddress() + "\nNgười nhận: " + p.getReceiverName() + " – " + p.getReceiverPhone()
                + "\nGhi chú đường đi: " + (p.getRouteNote() == null ? "—" : p.getRouteNote())
                + "\nMặc định: " + (p.isDefaultPoint() ? "Có" : "Không");
    }
}
