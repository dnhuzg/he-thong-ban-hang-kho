
package com.oms.service;

import com.oms.config.Db;
import com.oms.dao.AuditLogDao;
import com.oms.dao.FefoLotDao;
import com.oms.dao.OrderDao;
import com.oms.dao.OrderWorkflowDao;
import com.oms.dao.PickingDao;
import com.oms.dao.PickingLotDao;
import com.oms.model.FefoLot;
import com.oms.model.NamedItem;
import com.oms.model.Order;
import com.oms.model.PickingList;
import com.oms.model.Role;
import com.oms.model.User;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Phiếu soạn hàng (S6-03) và lựa chọn lô xuất theo FEFO (S6-04).
 * Thủ kho chỉ thao tác trong phạm vi kho được phân quyền.
 */
public class PickingService {

    private static final int MAX_ORDERS = 50;
    private static final int RECENT = 20;

    public record Page(List<PickingList.QueueItem> queue, List<PickingList> recent) {
        public List<PickingList.QueueItem> getQueue() {
            return queue;
        }

        public List<PickingList> getRecent() {
            return recent;
        }
    }

    public static class NotFoundException extends ServiceException {
        public NotFoundException() {
            super("Không tìm thấy phiếu soạn.");
        }
    }

    private final PickingDao dao = new PickingDao();
    private final PickingLotDao pickingLotDao = new PickingLotDao();
    private final FefoLotDao fefoLotDao = new FefoLotDao();
    private final AuditLogDao auditDao = new AuditLogDao();

    public Page page(User viewer) throws SQLException {
        Set<Long> warehouseScope = scope(viewer);

        try (Connection conn = Db.getConnection()) {
            return new Page(
                    dao.queue(conn, warehouseScope),
                    dao.recent(conn, warehouseScope, RECENT)
            );
        }
    }

    public PickingList get(long id, User viewer) throws SQLException {
        Set<Long> warehouseScope = scope(viewer);

        try (Connection conn = Db.getConnection()) {
            PickingList list = dao.find(conn, id);

            if (list == null
                    || !inScope(warehouseScope, list.getWarehouseId())) {
                throw new NotFoundException();
            }

            return list;
        }
    }

    public long create(List<Long> orderIds, User actor) throws SQLException {
        Set<Long> warehouseScope = scope(actor);
        TreeSet<Long> ids = new TreeSet<>(orderIds);

        if (ids.isEmpty()) {
            throw new ServiceException(
                    "Chọn ít nhất một đơn đã duyệt để soạn.");
        }

        if (ids.size() > MAX_ORDERS) {
            throw new ServiceException(
                    "Một phiếu soạn tối đa " + MAX_ORDERS + " đơn.");
        }

        return Db.inTransaction(conn -> {
            OrderDao orders = new OrderDao();
            List<Order> locked = new ArrayList<>();
            Long warehouseId = null;

            for (long id : ids) {
                Order order = orders.findByIdForUpdate(conn, id);

                if (order == null
                        || order.getWarehouseId() == null
                        || !inScope(warehouseScope, order.getWarehouseId())) {
                    throw new ServiceException(
                            "Không tìm thấy đơn #" + id
                                    + " trong kho bạn phụ trách.");
                }

                if (order.getStatus() != Order.Status.APPROVED) {
                    throw new ServiceException(
                            "Đơn " + order.getCode()
                                    + " không còn ở trạng thái Đã duyệt.");
                }

                if (warehouseId != null
                        && !warehouseId.equals(order.getWarehouseId())) {
                    throw new ServiceException(
                            "Chỉ gộp được các đơn cùng một kho vào một phiếu soạn.");
                }

                warehouseId = order.getWarehouseId();
                locked.add(order);
            }

            long listId = dao.insertList(
                    conn, warehouseId, actor.getId());

            OrderWorkflowDao workflow = new OrderWorkflowDao();

            for (Order order : locked) {
                dao.addOrderLines(conn, listId, order.getId());

                workflow.transition(
                        conn,
                        order,
                        Order.Status.PICKING,
                        "ADVANCE",
                        null,
                        actor.getId(),
                        order.getWarehouseId(),
                        order.getApprovalReason()
                );
            }

            auditDao.log(
                    conn,
                    actor.getId(),
                    "PICKING_LIST_CREATE",
                    "PICKING_LIST",
                    listId,
                    String.format("PS%06d", listId)
                            + " · " + locked.size()
                            + " đơn: "
                            + locked.stream()
                            .map(Order::getCode)
                            .collect(Collectors.joining(", "))
            );

            return listId;
        });
    }

    /**
     * S6-03: ghi số lượng thực soạn theo quy trình cũ.
     * Giữ phương thức này để tương thích với các test hiện có.
     */
    public void pick(long listId, long lineId, BigDecimal picked,
                     String shortReason, User actor) throws SQLException {
        Set<Long> warehouseScope = scope(actor);
        String reason = normalizeReason(shortReason);

        Db.inTransaction(conn -> {
            requireOpen(conn, listId, warehouseScope);

            PickingDao.LineLock line =
                    dao.findLineForUpdate(conn, lineId);

            if (line == null
                    || line.pickingListId() != listId) {
                throw new NotFoundException();
            }

            validatePicked(line.required(), picked, reason);

            boolean shortPick =
                    picked.compareTo(line.required()) < 0;

            dao.updatePicked(
                    conn,
                    lineId,
                    picked,
                    shortPick ? reason : null,
                    actor.getId()
            );

            return null;
        });
    }

    /**
     * S6-04 / AC1:
     * Tự động đề xuất số lượng lấy từ từng lô theo thứ tự FEFO.
     *
     * Ví dụ cần 8, lô đầu còn 3, lô sau còn 10:
     * - Lô đầu: suggestedQuantity = 3
     * - Lô sau: suggestedQuantity = 5
     *
     * Tồn khả dụng được tính sau khi trừ lượng đã phân bổ
     * cho các dòng soạn hàng khác.
     *
     * Đây là gợi ý tại thời điểm đọc; việc lưu lựa chọn
     * vẫn phải được kiểm tra lại trong pickWithLots().
     */
    public List<FefoLot> suggestLots(
            long listId, long lineId, User viewer) throws SQLException {

        Set<Long> warehouseScope = scope(viewer);

        try (Connection conn = Db.getConnection()) {
            PickingList list = dao.find(conn, listId);

            if (list == null
                    || !inScope(
                            warehouseScope,
                            list.getWarehouseId())) {
                throw new NotFoundException();
            }

            long productId =
                    productOfLine(conn, listId, lineId);

            BigDecimal required =
                    requiredOfLine(conn, listId, lineId);

            List<FefoLot> lots = fefoLotDao.availableLots(
                    conn,
                    list.getWarehouseId(),
                    productId
            );

            Map<Long, BigDecimal> reservedByOthers =
                    reservedByOtherLines(
                            conn,
                            list.getWarehouseId(),
                            productId,
                            lineId
                    );

            BigDecimal stillNeeded = required;
            List<FefoLot> suggestions = new ArrayList<>();

            for (FefoLot lot : lots) {
                BigDecimal reserved =
                        reservedByOthers.getOrDefault(
                                lot.id(),
                                BigDecimal.ZERO
                        );

                BigDecimal available =
                        lot.remainingQuantity()
                                .subtract(reserved)
                                .max(BigDecimal.ZERO);

                BigDecimal suggested =
                        stillNeeded.min(available);

                stillNeeded =
                        stillNeeded.subtract(suggested);

                suggestions.add(new FefoLot(
                        lot.id(),
                        lot.warehouseId(),
                        lot.productId(),
                        lot.lotNo(),
                        lot.expiryDate(),
                        available,
                        suggested.signum() > 0,
                        suggested
                ));
            }

            return suggestions;
        }
    }

    /**
     * S6-04: ghi số lượng thực soạn kèm các lô được chọn.
     *
     * - Tổng số lượng các lô phải bằng số lượng thực soạn.
     * - Lô phải thuộc đúng kho và sản phẩm, chưa hết hạn.
     * - Không được vượt tồn khả dụng.
     * - Bỏ qua thứ tự FEFO phải có lý do.
     * - Tồn lô chỉ được trừ khi thực hiện xuất kho.
     */
    public void pickWithLots(
            long listId,
            long lineId,
            BigDecimal picked,
            String shortReason,
            List<PickingLotDao.Selection> selections,
            User actor) throws SQLException {

        Set<Long> warehouseScope = scope(actor);
        String reason = normalizeReason(shortReason);

        Db.inTransaction(conn -> {
            requireOpen(conn, listId, warehouseScope);

            PickingDao.LineLock line =
                    dao.findLineForUpdate(conn, lineId);

            if (line == null
                    || line.pickingListId() != listId) {
                throw new NotFoundException();
            }

            validatePicked(line.required(), picked, reason);

            long warehouseId =
                    warehouseOfList(conn, listId);

            long productId =
                    productOfLine(conn, listId, lineId);

            List<PickingLotDao.Selection> chosen =
                    selections == null
                            ? List.of()
                            : selections;

            if (picked.signum() == 0) {
                if (!chosen.isEmpty()) {
                    throw new ServiceException(
                            "Số lượng thực soạn bằng 0 thì không được chọn lô.");
                }
            } else if (chosen.isEmpty()) {
                throw new ServiceException(
                        "Phải chọn ít nhất một lô cho số lượng thực soạn.");
            }

            Set<Long> chosenIds = new HashSet<>();
            BigDecimal total = BigDecimal.ZERO;

            for (PickingLotDao.Selection selection : chosen) {
                if (selection.lotId() <= 0
                        || !chosenIds.add(selection.lotId())) {
                    throw new ServiceException(
                            "Lô được chọn không hợp lệ hoặc bị trùng.");
                }

                if (selection.quantity() == null
                        || selection.quantity().signum() <= 0
                        || selection.quantity()
                        .stripTrailingZeros().scale() > 4) {
                    throw new ServiceException(
                            "Số lượng lấy từ lô không hợp lệ.");
                }

                String override =
                        normalizeReason(selection.overrideReason());

                if (override != null
                        && override.length() > 500) {
                    throw new ServiceException(
                            "Lý do đổi lô tối đa 500 ký tự.");
                }

                total = total.add(selection.quantity());
            }

            if (total.compareTo(picked) != 0) {
                throw new ServiceException(
                        "Tổng số lượng của các lô phải bằng số lượng thực soạn.");
            }

            /*
             * Khóa lô theo ID để giảm nguy cơ deadlock.
             */
            Map<Long, BigDecimal> remaining =
                    lockAvailableLots(
                            conn,
                            warehouseId,
                            productId
                    );

            List<FefoLot> fefoOrder =
                    fefoLotDao.availableLots(
                            conn,
                            warehouseId,
                            productId
                    );

            Map<Long, BigDecimal> reservedByOthers =
                    reservedByOtherLines(
                            conn,
                            warehouseId,
                            productId,
                            lineId
                    );

            Map<Long, BigDecimal> available = new HashMap<>();

            for (Map.Entry<Long, BigDecimal> entry :
                    remaining.entrySet()) {

                BigDecimal reserved =
                        reservedByOthers.getOrDefault(
                                entry.getKey(),
                                BigDecimal.ZERO
                        );

                available.put(
                        entry.getKey(),
                        entry.getValue().subtract(reserved)
                );
            }

            Map<Long, PickingLotDao.Selection> selectedById =
                    new HashMap<>();

            for (PickingLotDao.Selection selection : chosen) {
                BigDecimal stock =
                        available.get(selection.lotId());

                if (stock == null) {
                    throw new ServiceException(
                            "Lô #" + selection.lotId()
                                    + " không thuộc kho, sản phẩm hoặc đã hết hạn.");
                }

                if (stock.compareTo(selection.quantity()) < 0) {
                    throw new ServiceException(
                            "Lô #" + selection.lotId()
                                    + " không đủ số lượng tồn khả dụng.");
                }

                selectedById.put(
                        selection.lotId(),
                        selection
                );
            }

            /*
             * Kiểm tra thứ tự FEFO:
             * nếu sử dụng lô sau mà lô trước còn khả dụng,
             * phải nhập lý do đổi lô.
             */
            BigDecimal quantityStillNeeded = picked;

            List<PickingLotDao.Selection> validated =
                    new ArrayList<>();

            for (FefoLot lot : fefoOrder) {
                PickingLotDao.Selection selection =
                        selectedById.get(lot.id());

                BigDecimal selectedQuantity =
                        selection == null
                                ? BigDecimal.ZERO
                                : selection.quantity();

                if (selectedQuantity.signum() > 0) {
                    boolean skippedEarlier = false;

                    for (FefoLot earlier : fefoOrder) {
                        if (earlier.id() == lot.id()) {
                            break;
                        }

                        BigDecimal earlierAvailable =
                                available.getOrDefault(
                                        earlier.id(),
                                        BigDecimal.ZERO
                                ).max(BigDecimal.ZERO);

                        PickingLotDao.Selection earlierSelection =
                                selectedById.get(earlier.id());

                        BigDecimal earlierPicked =
                                earlierSelection == null
                                        ? BigDecimal.ZERO
                                        : earlierSelection.quantity();

                        if (earlierAvailable
                                .compareTo(earlierPicked) > 0) {
                            skippedEarlier = true;
                            break;
                        }
                    }

                    String override =
                            normalizeReason(
                                    selection.overrideReason()
                            );

                    if (skippedEarlier
                            && override == null) {
                        throw new ServiceException(
                                "Chọn lô khác thứ tự FEFO phải nhập lý do.");
                    }

                    validated.add(
                            new PickingLotDao.Selection(
                                    lot.id(),
                                    selectedQuantity,
                                    skippedEarlier ? override : null
                            )
                    );

                    quantityStillNeeded =
                            quantityStillNeeded.subtract(
                                    selectedQuantity
                            );
                }
            }

            if (quantityStillNeeded.signum() != 0) {
                throw new ServiceException(
                        "Số lượng lô được chọn không khớp số lượng thực soạn.");
            }

            pickingLotDao.replaceSelections(
                    conn,
                    lineId,
                    validated
            );

            boolean shortPick =
                    picked.compareTo(line.required()) < 0;

            dao.updatePicked(
                    conn,
                    lineId,
                    picked,
                    shortPick ? reason : null,
                    actor.getId()
            );

            auditDao.log(
                    conn,
                    actor.getId(),
                    "PICKING_FEFO_SELECT",
                    "PICKING_LIST",
                    listId,
                    "Dòng #" + lineId
                            + " · " + validated.size()
                            + " lô · số lượng "
                            + picked.toPlainString()
            );

            return null;
        });
    }

    public void complete(long listId, User actor)
            throws SQLException {

        Set<Long> warehouseScope = scope(actor);

        Db.inTransaction(conn -> {
            requireOpen(conn, listId, warehouseScope);

            int remaining =
                    dao.countUnpicked(conn, listId);

            if (remaining > 0) {
                throw new ServiceException(
                        "Còn " + remaining
                                + " dòng chưa đánh dấu đã soạn.");
            }

            dao.markDone(
                    conn,
                    listId,
                    actor.getId()
            );

            auditDao.log(
                    conn,
                    actor.getId(),
                    "PICKING_LIST_DONE",
                    "PICKING_LIST",
                    listId,
                    String.format("PS%06d", listId)
            );

            return null;
        });
    }

    private Map<Long, BigDecimal> lockAvailableLots(
            Connection conn,
            long warehouseId,
            long productId) throws SQLException {

        Map<Long, BigDecimal> result = new HashMap<>();

        String sql = """
                SELECT id, remaining_quantity
                FROM inventory_lots
                WHERE warehouse_id = ?
                  AND product_id = ?
                  AND remaining_quantity > 0
                  AND (expiry_date IS NULL OR expiry_date >= CURRENT_DATE())
                ORDER BY id
                FOR UPDATE
                """;

        try (PreparedStatement ps =
                     conn.prepareStatement(sql)) {

            ps.setLong(1, warehouseId);
            ps.setLong(2, productId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(
                            rs.getLong(1),
                            rs.getBigDecimal(2)
                    );
                }
            }
        }

        return result;
    }

    private Map<Long, BigDecimal> reservedByOtherLines(
            Connection conn,
            long warehouseId,
            long productId,
            long currentLineId) throws SQLException {

        Map<Long, BigDecimal> reserved = new HashMap<>();

        String sql = """
                SELECT pll.inventory_lot_id, SUM(pll.quantity)
                FROM picking_line_lots pll
                JOIN picking_list_lines pl
                  ON pl.id = pll.picking_line_id
                JOIN picking_lists p
                  ON p.id = pl.picking_list_id
                WHERE p.warehouse_id = ?
                  AND pl.product_id = ?
                  AND p.status = 'OPEN'
                  AND pl.id <> ?
                GROUP BY pll.inventory_lot_id
                """;

        try (PreparedStatement ps =
                     conn.prepareStatement(sql)) {

            ps.setLong(1, warehouseId);
            ps.setLong(2, productId);
            ps.setLong(3, currentLineId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reserved.put(
                            rs.getLong(1),
                            rs.getBigDecimal(2)
                    );
                }
            }
        }

        return reserved;
    }

    private BigDecimal requiredOfLine(
            Connection conn,
            long listId,
            long lineId) throws SQLException {

        try (PreparedStatement ps =
                     conn.prepareStatement(
                             "SELECT required_quantity "
                                     + "FROM picking_list_lines "
                                     + "WHERE picking_list_id = ? AND id = ?"
                     )) {

            ps.setLong(1, listId);
            ps.setLong(2, lineId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new NotFoundException();
                }

                return rs.getBigDecimal(1);
            }
        }
    }

    private long productOfLine(
            Connection conn,
            long listId,
            long lineId) throws SQLException {

        try (PreparedStatement ps =
                     conn.prepareStatement(
                             "SELECT product_id "
                                     + "FROM picking_list_lines "
                                     + "WHERE picking_list_id = ? AND id = ?"
                     )) {

            ps.setLong(1, listId);
            ps.setLong(2, lineId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new NotFoundException();
                }

                return rs.getLong(1);
            }
        }
    }

    private long warehouseOfList(
            Connection conn,
            long listId) throws SQLException {

        try (PreparedStatement ps =
                     conn.prepareStatement(
                             "SELECT warehouse_id "
                                     + "FROM picking_lists "
                                     + "WHERE id = ?"
                     )) {

            ps.setLong(1, listId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new NotFoundException();
                }

                return rs.getLong(1);
            }
        }
    }

    private static void validatePicked(
            BigDecimal required,
            BigDecimal picked,
            String reason) {

        if (picked == null || picked.signum() < 0) {
            throw new ServiceException(
                    "Nhập số lượng thực soạn "
                            + "(0 nếu không lấy được hàng).");
        }

        if (picked.stripTrailingZeros().scale() > 4) {
            throw new ServiceException(
                    "Số lượng tối đa 4 chữ số thập phân.");
        }

        if (picked.compareTo(required) > 0) {
            throw new ServiceException(
                    "Số thực soạn không được vượt số cần soạn ("
                            + required.stripTrailingZeros().toPlainString()
                            + ").");
        }

        if (picked.compareTo(required) < 0
                && reason == null) {
            throw new ServiceException(
                    "Soạn thiếu phải nhập lý do.");
        }

        if (reason != null
                && reason.length() > 500) {
            throw new ServiceException(
                    "Lý do tối đa 500 ký tự.");
        }
    }

    private static String normalizeReason(String value) {
        return value == null || value.isBlank()
                ? null
                : value.strip();
    }

    private void requireOpen(
            Connection conn,
            long listId,
            Set<Long> warehouseScope) throws SQLException {

        PickingDao.Header header =
                dao.findHeaderForUpdate(conn, listId);

        if (header == null
                || !inScope(
                        warehouseScope,
                        header.warehouseId())) {
            throw new NotFoundException();
        }

        if (!PickingList.OPEN.equals(header.status())) {
            throw new ServiceException(
                    "Phiếu soạn đã hoàn tất, không sửa được.");
        }
    }

    /**
     * null = mọi kho (Admin).
     * Nhân viên và quản lý kho chỉ thao tác kho được gắn.
     */
    static Set<Long> scope(User user) {
        if (user.isAdmin()) {
            return null;
        }

        if (!user.hasRole(Role.WAREHOUSE)
                && !user.hasRole(Role.WH_MANAGER)) {
            throw new ForbiddenException(
                    "Chỉ nhân viên kho và quản lý kho được soạn hàng.");
        }

        return user.getWarehouses()
                .stream()
                .map(NamedItem::getId)
                .collect(Collectors.toSet());
    }

    static boolean inScope(
            Set<Long> warehouseScope,
            long warehouseId) {

        return warehouseScope == null
                || warehouseScope.contains(warehouseId);
    }
}
