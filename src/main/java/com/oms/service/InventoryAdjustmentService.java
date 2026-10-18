
package com.oms.service;

import com.oms.config.Db;
import com.oms.dao.AuditLogDao;
import com.oms.dao.InventoryAdjustmentDao;
import com.oms.model.Role;
import com.oms.model.User;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * GHTK-78 / S8-03: Lập phiếu điều chỉnh tồn kho có phê duyệt.
 */
public class InventoryAdjustmentService {

    private final InventoryAdjustmentDao dao =
            new InventoryAdjustmentDao();

    private final AuditLogDao auditDao =
            new AuditLogDao();

    public record LineInput(
            long productId,
            String reason,
            String reasonDetail,
            BigDecimal quantity,
            BigDecimal lossValue
    ) {}

    public record Detail(
            InventoryAdjustmentDao.Adjustment adjustment,
            List<InventoryAdjustmentDao.AdjustmentLine> lines
    ) {}

    public static class NotFoundException extends ServiceException {
        public NotFoundException() {
            super("Không tìm thấy phiếu điều chỉnh trong kho được phân quyền.");
        }
    }

    /**
     * AC1 + AC2:
     * Lập phiếu điều chỉnh hàng hỏng, mất hoặc hết hạn.
     *
     * Chỉ tạo phiếu DRAFT, chưa tác động tồn kho.
     */
    public long create(
            long warehouseId,
            String note,
            List<LineInput> lines,
            User actor
    ) throws SQLException {

        requireWarehouseAccess(actor, warehouseId);

        if (lines == null || lines.isEmpty()) {
            throw new ServiceException(
                    "Phiếu điều chỉnh phải có ít nhất một dòng hàng.");
        }

        if (lines.size() > 100) {
            throw new ServiceException(
                    "Một phiếu chỉ được có tối đa 100 dòng hàng.");
        }

        for (LineInput line : lines) {
            validateLine(line);
        }

        return Db.inTransaction(conn -> {
            String temporaryCode = "TMP-"
                    + UUID.randomUUID().toString().substring(0, 24);

            long adjustmentId = dao.insert(
                    conn,
                    temporaryCode,
                    warehouseId,
                    trimToNull(note),
                    actor.getId()
            );

            String code = String.format("DC%06d", adjustmentId);
            dao.updateCode(conn, adjustmentId, code);

            for (LineInput line : lines) {
                dao.addLine(
                        conn,
                        adjustmentId,
                        line.productId(),
                        line.reason(),
                        line.reasonDetail().trim(),
                        line.quantity(),
                        line.lossValue()
                );
            }

            dao.updateTotalLoss(conn, adjustmentId);

            auditDao.log(
                    conn,
                    actor.getId(),
                    "INVENTORY_ADJUSTMENT_CREATE",
                    "INVENTORY_ADJUSTMENT",
                    adjustmentId,
                    "Lập phiếu " + code
                            + ", kho #" + warehouseId
                            + ", số dòng: " + lines.size()
            );

            return adjustmentId;
        });
    }

    /**
     * Chuyển phiếu DRAFT sang PENDING để chờ phê duyệt.
     */
    public void submit(long adjustmentId, User actor)
            throws SQLException {

        Db.inTransaction(conn -> {
            InventoryAdjustmentDao.Adjustment adjustment =
                    requireAdjustment(conn, adjustmentId, actor);

            if (adjustment.createdBy() != actor.getId()) {
                throw new ForbiddenException(
                        "Chỉ người lập phiếu mới được gửi phê duyệt.");
            }

            if (!"DRAFT".equals(adjustment.status())) {
                throw new ServiceException(
                        "Chỉ được gửi duyệt phiếu đang ở trạng thái nháp.");
            }

            if (dao.lines(conn, adjustmentId).isEmpty()) {
                throw new ServiceException(
                        "Không thể gửi duyệt phiếu không có dòng hàng.");
            }

            if (!dao.submit(conn, adjustmentId)) {
                throw new ServiceException(
                        "Không thể gửi phiếu để phê duyệt.");
            }

            auditDao.log(
                    conn,
                    actor.getId(),
                    "INVENTORY_ADJUSTMENT_SUBMIT",
                    "INVENTORY_ADJUSTMENT",
                    adjustmentId,
                    "Gửi phê duyệt phiếu " + adjustment.code()
            );

            return null;
        });
    }

    /**
     * AC3 + AC4:
     * Chỉ Quản lý kho được duyệt.
     * Người duyệt phải khác người lập.
     *
     * Trừ tồn kho và ghi thẻ kho trong cùng transaction.
     */
    public void approve(long adjustmentId, User actor)
            throws SQLException {

        requireManager(actor);

        Db.inTransaction(conn -> {
            InventoryAdjustmentDao.Adjustment adjustment =
                    dao.lock(conn, adjustmentId);

            if (adjustment == null) {
                throw new NotFoundException();
            }

            requireWarehouseAccess(actor, adjustment.warehouseId());

            if (!"PENDING".equals(adjustment.status())) {
                throw new ServiceException(
                        "Chỉ được duyệt phiếu đang chờ phê duyệt.");
            }

            if (adjustment.createdBy() == actor.getId()) {
                throw new ForbiddenException(
                        "Người lập phiếu không được tự phê duyệt.");
            }

            List<InventoryAdjustmentDao.AdjustmentLine> lines =
                    dao.lines(conn, adjustmentId);

            if (lines.isEmpty()) {
                throw new ServiceException(
                        "Phiếu điều chỉnh không có dòng hàng.");
            }

            /*
             * Cộng lượng điều chỉnh theo sản phẩm.
             * Một sản phẩm có thể có nhiều lý do tổn thất.
             */
            Map<Long, BigDecimal> quantityByProduct =
                    new HashMap<>();

            BigDecimal totalLoss = BigDecimal.ZERO;

            for (InventoryAdjustmentDao.AdjustmentLine line : lines) {
                quantityByProduct.merge(
                        line.productId(),
                        line.quantity(),
                        BigDecimal::add
                );

                totalLoss = totalLoss.add(line.lossValue());
            }

            /*
             * Kiểm tra và trừ kho bằng UPDATE có điều kiện.
             * Không cho tồn kho xuống âm hoặc thấp hơn số đã giữ chỗ.
             */
            for (Map.Entry<Long, BigDecimal> entry
                    : quantityByProduct.entrySet()) {

                deductInventory(
                        conn,
                        adjustment.warehouseId(),
                        entry.getKey(),
                        entry.getValue()
                );
            }

            /*
             * Ghi nhận từng dòng điều chỉnh trên thẻ kho.
             *
             * reference_type và reference_id giúp truy vết
             * về phiếu điều chỉnh đã được phê duyệt.
             */
            for (InventoryAdjustmentDao.AdjustmentLine line : lines) {
                insertMovement(
                        conn,
                        adjustment.warehouseId(),
                        line,
                        adjustmentId,
                        actor.getId()
                );
            }

            if (!dao.approve(conn, adjustmentId, actor.getId())) {
                throw new ServiceException(
                        "Không thể phê duyệt phiếu điều chỉnh.");
            }

            auditDao.log(
                    conn,
                    actor.getId(),
                    "INVENTORY_ADJUSTMENT_APPROVE",
                    "INVENTORY_ADJUSTMENT",
                    adjustmentId,
                    "Phê duyệt " + adjustment.code()
                            + ", giá trị tổn thất: "
                            + totalLoss.toPlainString()
            );

            return null;
        });
    }

    /**
     * Từ chối phiếu đang chờ duyệt.
     * Không làm thay đổi số lượng tồn kho.
     */
    public void reject(
            long adjustmentId,
            String rejectionReason,
            User actor
    ) throws SQLException {

        requireManager(actor);

        if (rejectionReason == null || rejectionReason.isBlank()) {
            throw new ServiceException(
                    "Bắt buộc nhập lý do từ chối.");
        }

        if (rejectionReason.trim().length() > 500) {
            throw new ServiceException(
                    "Lý do từ chối không được vượt quá 500 ký tự.");
        }

        Db.inTransaction(conn -> {
            InventoryAdjustmentDao.Adjustment adjustment =
                    dao.lock(conn, adjustmentId);

            if (adjustment == null) {
                throw new NotFoundException();
            }

            requireWarehouseAccess(actor, adjustment.warehouseId());

            if (!"PENDING".equals(adjustment.status())) {
                throw new ServiceException(
                        "Chỉ được từ chối phiếu đang chờ duyệt.");
            }

            if (adjustment.createdBy() == actor.getId()) {
                throw new ForbiddenException(
                        "Người lập phiếu không được tự xử lý phê duyệt.");
            }

            if (!dao.reject(
                    conn,
                    adjustmentId,
                    actor.getId(),
                    rejectionReason.trim()
            )) {
                throw new ServiceException(
                        "Không thể từ chối phiếu điều chỉnh.");
            }

            auditDao.log(
                    conn,
                    actor.getId(),
                    "INVENTORY_ADJUSTMENT_REJECT",
                    "INVENTORY_ADJUSTMENT",
                    adjustmentId,
                    "Từ chối " + adjustment.code()
                            + ": " + rejectionReason.trim()
            );

            return null;
        });
    }

    /**
     * Lấy chi tiết phiếu trong phạm vi kho được phân quyền.
     */
    public Detail get(long adjustmentId, User viewer)
            throws SQLException {

        if (viewer == null) {
            throw new ForbiddenException("Vui lòng đăng nhập.");
        }

        try (Connection conn = Db.getConnection()) {
            InventoryAdjustmentDao.Adjustment adjustment =
                    requireAdjustment(conn, adjustmentId, viewer);

            return new Detail(
                    adjustment,
                    dao.lines(conn, adjustmentId)
            );
        }
    }

    /**
     * Danh sách phiếu điều chỉnh trong phạm vi kho.
     */
    public List<InventoryAdjustmentDao.Adjustment> list(User viewer)
            throws SQLException {

        if (viewer == null) {
            throw new ForbiddenException("Vui lòng đăng nhập.");
        }

        if (!viewer.isAdmin()
                && !viewer.hasRole(Role.WAREHOUSE)
                && !viewer.hasRole(Role.WH_MANAGER)) {
            throw new ForbiddenException(
                    "Không có quyền xem phiếu điều chỉnh tồn kho.");
        }

        try (Connection conn = Db.getConnection()) {
            return dao.list(conn)
                    .stream()
                    .filter(a -> hasWarehouseAccess(
                            viewer, a.warehouseId()))
                    .toList();
        }
    }

    private InventoryAdjustmentDao.Adjustment requireAdjustment(
            Connection conn,
            long adjustmentId,
            User actor
    ) throws SQLException {

        InventoryAdjustmentDao.Adjustment adjustment =
                dao.find(conn, adjustmentId);

        if (adjustment == null
                || !hasWarehouseAccess(
                        actor, adjustment.warehouseId())) {
            throw new NotFoundException();
        }

        return adjustment;
    }

    private void deductInventory(
            Connection conn,
            long warehouseId,
            long productId,
            BigDecimal quantity
    ) throws SQLException {

        String sql = """
                UPDATE warehouse_inventory
                SET on_hand = on_hand - ?
                WHERE warehouse_id = ?
                  AND product_id = ?
                  AND on_hand - ? >= COALESCE(reserved, 0)
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, quantity);
            ps.setLong(2, warehouseId);
            ps.setLong(3, productId);
            ps.setBigDecimal(4, quantity);

            if (ps.executeUpdate() != 1) {
                throw new ServiceException(
                        "Không đủ tồn kho khả dụng cho sản phẩm #"
                                + productId);
            }
        }
    }

    private void insertMovement(
            Connection conn,
            long warehouseId,
            InventoryAdjustmentDao.AdjustmentLine line,
            long adjustmentId,
            long actorId
    ) throws SQLException {

        String sql = """
                INSERT INTO inventory_movements
                    (warehouse_id, product_id, unit_id, quantity,
                     movement_type, reference_type, reference_id,
                     created_by)
                VALUES (?, ?, ?, ?, 'INVENTORY_ADJUSTMENT_LOSS',
                        'INVENTORY_ADJUSTMENT', ?, ?)
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, warehouseId);
            ps.setLong(2, line.productId());
            ps.setLong(3, getBaseUnitId(conn, line.productId()));
            ps.setBigDecimal(4, line.quantity());
            ps.setLong(5, adjustmentId);
            ps.setLong(6, actorId);
            ps.executeUpdate();
        }
    }

    private long getBaseUnitId(Connection conn, long productId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT base_unit_id FROM products WHERE id = ?")) {
            ps.setLong(1, productId);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new ServiceException("Không tìm thấy sản phẩm #" + productId);
                }
                return rs.getLong(1);
            }
        }
    }
    private static void validateLine(LineInput line) {
        if (line == null || line.productId() <= 0) {
            throw new ServiceException(
                    "Sản phẩm điều chỉnh không hợp lệ.");
        }

        if (!"DAMAGED".equals(line.reason())
                && !"LOST".equals(line.reason())
                && !"EXPIRED".equals(line.reason())) {
            throw new ServiceException(
                    "Lý do phải là hàng hỏng, hàng mất hoặc hết hạn.");
        }

        if (line.reasonDetail() == null
                || line.reasonDetail().isBlank()
                || line.reasonDetail().trim().length() > 500) {
            throw new ServiceException(
                    "Bắt buộc ghi rõ lý do điều chỉnh (tối đa 500 ký tự).");
        }

        if (line.quantity() == null
                || line.quantity().signum() <= 0
                || line.quantity().scale() > 4) {
            throw new ServiceException(
                    "Số lượng điều chỉnh phải lớn hơn 0 và tối đa 4 chữ số thập phân.");
        }

        if (line.lossValue() == null
                || line.lossValue().signum() < 0
                || line.lossValue().scale() > 2) {
            throw new ServiceException(
                    "Bắt buộc ghi giá trị tổn thất hợp lệ (không âm).");
        }
    }

    private static void requireManager(User actor) {
        if (actor == null || !actor.hasRole(Role.WH_MANAGER)) {
            throw new ForbiddenException(
                    "Chỉ Quản lý kho được phê duyệt hoặc từ chối phiếu.");
        }
    }

    private static void requireWarehouseAccess(
            User actor, long warehouseId
    ) {
        if (actor == null
                || warehouseId <= 0
                || !hasWarehouseAccess(actor, warehouseId)) {
            throw new ForbiddenException(
                    "Không có quyền thao tác với kho này.");
        }
    }

    private static boolean hasWarehouseAccess(
            User actor, long warehouseId
    ) {
        if (actor == null) {
            return false;
        }

        if (actor.isAdmin()) {
            return true;
        }

        if (!actor.hasRole(Role.WAREHOUSE)
                && !actor.hasRole(Role.WH_MANAGER)) {
            return false;
        }

        return actor.getWarehouses().stream()
                .anyMatch(w -> w.getId() == warehouseId);
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim();
    }
}
