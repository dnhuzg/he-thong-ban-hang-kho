
package com.oms.service;

import com.oms.config.Db;
import com.oms.dao.AuditLogDao;
import com.oms.dao.CustomerDao;
import com.oms.dao.OrderDao;
import com.oms.dao.OrderWorkflowDao;
import com.oms.dao.ShipmentExportDao;
import com.oms.model.Order;
import com.oms.model.ShipmentExport;
import com.oms.model.User;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Xác nhận xuất kho (S6-05) từ phiếu soạn đã hoàn tất.
 *
 * S6-04: Trừ tồn đúng từng lô FEFO đã chọn.
 * S6-05: Chuyển giữ chỗ thành xuất thực, lập phiếu xuất,
 * ghi nhận hàng soạn thiếu và cập nhật trạng thái đơn.
 */
public class ShipmentExportService {

    private static final int RECENT = 20;

    public record Page(
            List<ShipmentExport.Ready> ready,
            List<ShipmentExport.Backorder> backorders,
            List<ShipmentExport.Recent> recent) {

        public List<ShipmentExport.Ready> getReady() {
            return ready;
        }

        public List<ShipmentExport.Backorder> getBackorders() {
            return backorders;
        }

        public List<ShipmentExport.Recent> getRecent() {
            return recent;
        }
    }

    private final ShipmentExportDao dao = new ShipmentExportDao();
    private final AuditLogDao auditDao = new AuditLogDao();

    public Page page(User viewer) throws SQLException {
        Set<Long> scope = PickingService.scope(viewer);

        try (Connection conn = Db.getConnection()) {
            return new Page(
                    dao.ready(conn, scope),
                    dao.backorders(conn, scope),
                    dao.recent(conn, scope, RECENT)
            );
        }
    }

    /** Trả về ID phiếu xuất vừa lập. */
    public long export(long orderId, User actor) throws SQLException {
        Set<Long> scope = PickingService.scope(actor);

        return Db.inTransaction(conn -> {
            Order order = new OrderDao().findByIdForUpdate(conn, orderId);

            if (order == null
                    || order.getWarehouseId() == null
                    || !PickingService.inScope(scope, order.getWarehouseId())) {
                throw new OrderService.NotFoundException();
            }

            if (order.getStatus() != Order.Status.PICKING
                    || !dao.isReady(conn, orderId)) {
                throw new ServiceException(
                        "Đơn " + order.getCode()
                                + " chưa có phiếu soạn hoàn tất nên chưa xuất được.");
            }

            new CustomerDao().findByIdForUpdate(
                    conn, order.getCustomerId());

            List<ShipmentExportDao.PickedLine> lines =
                    dao.pickedLines(conn, orderId);

            Map<Long, BigDecimal> shipped = new HashMap<>();

            for (ShipmentExportDao.PickedLine line : lines) {
                shipped.merge(
                        line.productId(),
                        line.picked(),
                        BigDecimal::add
                );
            }

            if (shipped.values().stream()
                    .allMatch(q -> q.signum() == 0)) {
                throw new ServiceException(
                        "Đơn " + order.getCode()
                                + " không soạn được mặt hàng nào; "
                                + "huỷ đơn hoặc soạn lại thay vì xuất kho.");
            }

            /*
             * S6-04: Trừ đúng tồn của từng lô đã chọn.
             *
             * S6-05: Trừ tồn tổng và giải phóng phần giữ chỗ.
             *
             * Cả hai thao tác nằm trong cùng transaction.
             * Nếu một thao tác lỗi, toàn bộ thay đổi sẽ rollback.
             */
            dao.consumePickedLots(
                    conn,
                    orderId,
                    order.getWarehouseId()
            );

            dao.consumeReservations(
                    conn,
                    orderId,
                    shipped
            );

            long shipmentId = dao.insertShipment(
                    conn,
                    order.getWarehouseId(),
                    order.getCustomerId(),
                    orderId
            );

            int shortLines = 0;

            for (ShipmentExportDao.PickedLine line : lines) {
                if (line.picked().signum() > 0) {
                    dao.insertItem(
                            conn,
                            shipmentId,
                            line.productId(),
                            line.baseUnitId(),
                            line.picked()
                    );

                    dao.insertMovement(
                            conn,
                            order.getWarehouseId(),
                            line.productId(),
                            line.baseUnitId(),
                            line.picked(),
                            shipmentId,
                            actor.getId()
                    );
                }

                BigDecimal missing =
                        line.required().subtract(line.picked());

                if (missing.signum() > 0) {
                    dao.insertBackorder(
                            conn,
                            orderId,
                            line,
                            missing,
                            shipmentId
                    );
                    shortLines++;
                }
            }

            String code = String.format("PX%06d", shipmentId);

            new OrderWorkflowDao().transition(
                    conn,
                    order,
                    Order.Status.SHIPPED,
                    "ADVANCE",
                    "Phiếu xuất " + code
                            + (shortLines > 0
                                    ? ", còn nợ " + shortLines + " dòng hàng"
                                    : ""),
                    actor.getId(),
                    order.getWarehouseId(),
                    order.getApprovalReason()
            );

            auditDao.log(
                    conn,
                    actor.getId(),
                    "SHIPMENT_EXPORT",
                    "SHIPMENT",
                    shipmentId,
                    code + " · đơn " + order.getCode()
                            + (shortLines > 0
                                    ? " · xuất thiếu " + shortLines + " dòng"
                                    : "")
            );

            return shipmentId;
        });
    }
}
