package com.oms.service;

import com.oms.config.Db;
import com.oms.dao.AuditLogDao;
import com.oms.dao.OrderDao;
import com.oms.dao.OrderWorkflowDao;
import com.oms.dao.PickingDao;
import com.oms.model.NamedItem;
import com.oms.model.Order;
import com.oms.model.PickingList;
import com.oms.model.Role;
import com.oms.model.User;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Phiếu soạn hàng (S6-03): gom đơn đã duyệt của một kho thành một lượt soạn, đơn chuyển sang Đang soạn hàng.
 * Thủ kho ghi số lượng thực soạn từng dòng; soạn thiếu bắt buộc có lý do. Chỉ nhân viên / quản lý kho
 * của đúng kho và Admin được thao tác.
 */
public class PickingService {

    private static final int MAX_ORDERS = 50;
    private static final int RECENT = 20;

    public record Page(List<PickingList.QueueItem> queue, List<PickingList> recent) {
        public List<PickingList.QueueItem> getQueue() { return queue; }
        public List<PickingList> getRecent() { return recent; }
    }

    public static class NotFoundException extends ServiceException {
        public NotFoundException() {
            super("Không tìm thấy phiếu soạn.");
        }
    }

    private final PickingDao dao = new PickingDao();
    private final AuditLogDao auditDao = new AuditLogDao();

    public Page page(User viewer) throws SQLException {
        Set<Long> scope = scope(viewer);
        try (Connection conn = Db.getConnection()) {
            return new Page(dao.queue(conn, scope), dao.recent(conn, scope, RECENT));
        }
    }

    public PickingList get(long id, User viewer) throws SQLException {
        Set<Long> scope = scope(viewer);
        try (Connection conn = Db.getConnection()) {
            PickingList list = dao.find(conn, id);
            if (list == null || !inScope(scope, list.getWarehouseId())) {
                throw new NotFoundException();
            }
            return list;
        }
    }

    public long create(List<Long> orderIds, User actor) throws SQLException {
        Set<Long> scope = scope(actor);
        // Khoá đơn theo thứ tự ID để hai người gom cùng lúc không bị deadlock
        TreeSet<Long> ids = new TreeSet<>(orderIds);
        if (ids.isEmpty()) {
            throw new ServiceException("Chọn ít nhất một đơn đã duyệt để soạn.");
        }
        if (ids.size() > MAX_ORDERS) {
            throw new ServiceException("Một phiếu soạn tối đa " + MAX_ORDERS + " đơn.");
        }
        return Db.inTransaction(conn -> {
            OrderDao orders = new OrderDao();
            List<Order> locked = new ArrayList<>();
            Long warehouseId = null;
            for (long id : ids) {
                Order order = orders.findByIdForUpdate(conn, id);
                if (order == null || order.getWarehouseId() == null || !inScope(scope, order.getWarehouseId())) {
                    throw new ServiceException("Không tìm thấy đơn #" + id + " trong kho bạn phụ trách.");
                }
                if (order.getStatus() != Order.Status.APPROVED) {
                    throw new ServiceException("Đơn " + order.getCode() + " không còn ở trạng thái Đã duyệt.");
                }
                if (warehouseId != null && !warehouseId.equals(order.getWarehouseId())) {
                    throw new ServiceException("Chỉ gộp được các đơn cùng một kho vào một phiếu soạn.");
                }
                warehouseId = order.getWarehouseId();
                locked.add(order);
            }
            long listId = dao.insertList(conn, warehouseId, actor.getId());
            OrderWorkflowDao workflow = new OrderWorkflowDao();
            for (Order order : locked) {
                dao.addOrderLines(conn, listId, order.getId());
                workflow.transition(conn, order, Order.Status.PICKING, "ADVANCE", null, actor.getId(),
                        order.getWarehouseId(), order.getApprovalReason());
            }
            auditDao.log(conn, actor.getId(), "PICKING_LIST_CREATE", "PICKING_LIST", listId,
                    String.format("PS%06d", listId) + " · " + locked.size() + " đơn: "
                            + locked.stream().map(Order::getCode).collect(Collectors.joining(", ")));
            return listId;
        });
    }

    /** Ghi số lượng thực soạn của một dòng; ghi lại được khi phiếu còn đang soạn. */
    public void pick(long listId, long lineId, BigDecimal picked, String shortReason, User actor)
            throws SQLException {
        Set<Long> scope = scope(actor);
        String reason = shortReason == null || shortReason.isBlank() ? null : shortReason.strip();
        Db.inTransaction(conn -> {
            requireOpen(conn, listId, scope);
            PickingDao.LineLock line = dao.findLineForUpdate(conn, lineId);
            if (line == null || line.pickingListId() != listId) {
                throw new NotFoundException();
            }
            if (picked == null || picked.signum() < 0) {
                throw new ServiceException("Nhập số lượng thực soạn (0 nếu không lấy được hàng).");
            }
            if (picked.stripTrailingZeros().scale() > 4) {
                throw new ServiceException("Số lượng tối đa 4 chữ số thập phân.");
            }
            if (picked.compareTo(line.required()) > 0) {
                throw new ServiceException("Số thực soạn không được vượt số cần soạn ("
                        + line.required().stripTrailingZeros().toPlainString() + ").");
            }
            boolean shortPick = picked.compareTo(line.required()) < 0;
            if (shortPick && reason == null) {
                throw new ServiceException("Soạn thiếu phải nhập lý do.");
            }
            if (reason != null && reason.length() > 500) {
                throw new ServiceException("Lý do tối đa 500 ký tự.");
            }
            dao.updatePicked(conn, lineId, picked, shortPick ? reason : null, actor.getId());
            return null;
        });
    }

    public void complete(long listId, User actor) throws SQLException {
        Set<Long> scope = scope(actor);
        Db.inTransaction(conn -> {
            requireOpen(conn, listId, scope);
            int remaining = dao.countUnpicked(conn, listId);
            if (remaining > 0) {
                throw new ServiceException("Còn " + remaining + " dòng chưa đánh dấu đã soạn.");
            }
            dao.markDone(conn, listId, actor.getId());
            auditDao.log(conn, actor.getId(), "PICKING_LIST_DONE", "PICKING_LIST", listId,
                    String.format("PS%06d", listId));
            return null;
        });
    }

    private void requireOpen(Connection conn, long listId, Set<Long> scope) throws SQLException {
        PickingDao.Header header = dao.findHeaderForUpdate(conn, listId);
        if (header == null || !inScope(scope, header.warehouseId())) {
            throw new NotFoundException();
        }
        if (!PickingList.OPEN.equals(header.status())) {
            throw new ServiceException("Phiếu soạn đã hoàn tất, không sửa được.");
        }
    }

    /** null = mọi kho (Admin); nhân viên / quản lý kho chỉ thấy kho được gắn; vai trò khác không được vào. Dùng chung cho xuất kho. */
    static Set<Long> scope(User user) {
        if (user.isAdmin()) {
            return null;
        }
        if (!user.hasRole(Role.WAREHOUSE) && !user.hasRole(Role.WH_MANAGER)) {
            throw new ForbiddenException("Chỉ nhân viên kho và quản lý kho được soạn hàng.");
        }
        return user.getWarehouses().stream().map(NamedItem::getId).collect(Collectors.toSet());
    }

    static boolean inScope(Set<Long> scope, long warehouseId) {
        return scope == null || scope.contains(warehouseId);
    }
}
