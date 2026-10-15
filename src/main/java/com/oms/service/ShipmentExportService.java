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
 * Xác nhận xuất kho (S6-05) từ phiếu soạn đã hoàn tất: giữ chỗ chuyển thành xuất thực theo số thực soạn,
 * lập phiếu xuất (chờ giao, xác nhận ở S6-08), phần soạn thiếu ghi nợ hàng; đơn chuyển sang Đã xuất kho.
 * Phiếu xuất đã lập không sửa được, chỉ xác nhận giao thành công hoặc thất bại.
 */
public class ShipmentExportService {

    private static final int RECENT = 20;

    public record Page(List<ShipmentExport.Ready> ready, List<ShipmentExport.Backorder> backorders,
                       List<ShipmentExport.Recent> recent, Map<Long, List<InventoryLotService.LotChoice>> lotChoices) {
        public List<ShipmentExport.Ready> getReady() { return ready; }
        /** Đơn → lô gợi ý của các SKU có quản lý lô (GHTK-61). */
        public Map<Long, List<InventoryLotService.LotChoice>> getLotChoices() { return lotChoices; }
        public List<ShipmentExport.Backorder> getBackorders() { return backorders; }
        public List<ShipmentExport.Recent> getRecent() { return recent; }
    }

    private final ShipmentExportDao dao = new ShipmentExportDao();
    private final AuditLogDao auditDao = new AuditLogDao();

    public Page page(User viewer) throws SQLException {
        Set<Long> scope = PickingService.scope(viewer);
        try (Connection conn = Db.getConnection()) {
            List<ShipmentExport.Ready> ready = dao.ready(conn, scope);
            Map<Long, List<InventoryLotService.LotChoice>> choices = new HashMap<>();
            InventoryLotService lots = new InventoryLotService();
            for (ShipmentExport.Ready r : ready) {
                Order order = new OrderDao().findById(conn, r.getOrderId());
                Map<Long, BigDecimal> picked = new HashMap<>();
                for (ShipmentExportDao.PickedLine line : dao.pickedLines(conn, r.getOrderId())) {
                    picked.merge(line.productId(), line.picked(), BigDecimal::add);
                }
                List<InventoryLotService.LotChoice> list = lots.choices(conn, order.getWarehouseId(), picked);
                if (!list.isEmpty()) {
                    choices.put(r.getOrderId(), list);
                }
            }
            return new Page(ready, dao.backorders(conn, scope), dao.recent(conn, scope, RECENT), choices);
        }
    }

    /** Trả về ID phiếu xuất vừa lập. */
    public long export(long orderId, User actor) throws SQLException {
        return export(orderId,actor,false,null);
    }

    public long export(long orderId, User actor, boolean overrideExpiry, String overrideReason) throws SQLException {
        return export(orderId, actor, overrideExpiry, overrideReason, Map.of(), null);
    }

    /** chosenLots: SKU → lô thủ kho chọn thay lô gợi ý; khác lô gợi ý thì phải có pickReason. */
    public long export(long orderId, User actor, boolean overrideExpiry, String overrideReason,
                       Map<Long, Long> chosenLots, String pickReason) throws SQLException {
        Set<Long> scope = PickingService.scope(actor);
        return Db.inTransaction(conn -> {
            Order order = new OrderDao().findByIdForUpdate(conn, orderId);
            if (order == null || order.getWarehouseId() == null
                    || !PickingService.inScope(scope, order.getWarehouseId())) {
                throw new OrderService.NotFoundException();
            }
            if (order.getStatus() != Order.Status.PICKING || !dao.isReady(conn, orderId)) {
                throw new ServiceException("Đơn " + order.getCode() + " chưa có phiếu soạn hoàn tất nên chưa xuất được.");
            }
            new CustomerDao().findByIdForUpdate(conn, order.getCustomerId());

            List<ShipmentExportDao.PickedLine> lines = dao.pickedLines(conn, orderId);
            Map<Long, BigDecimal> shipped = new HashMap<>();
            for (ShipmentExportDao.PickedLine line : lines) {
                shipped.merge(line.productId(), line.picked(), BigDecimal::add);
            }
            if (shipped.values().stream().allMatch(q -> q.signum() == 0)) {
                throw new ServiceException("Đơn " + order.getCode()
                        + " không soạn được mặt hàng nào; huỷ đơn hoặc soạn lại thay vì xuất kho.");
            }
            for (long productId : new java.util.TreeSet<>(shipped.keySet()))
                new com.oms.dao.InventoryLotDao().product(conn,productId,true);
            dao.consumeReservations(conn, orderId, shipped);

            long shipmentId = dao.insertShipment(conn, order.getWarehouseId(), order.getCustomerId(), orderId);
            new InventoryLotService().consume(conn,shipmentId,order.getWarehouseId(),shipped,overrideExpiry,overrideReason,chosenLots,pickReason,actor);
            int shortLines = 0;
            for (ShipmentExportDao.PickedLine line : lines) {
                if (line.picked().signum() > 0) {
                    dao.insertItem(conn, shipmentId, line.productId(), line.baseUnitId(), line.picked());
                    dao.insertMovement(conn, order.getWarehouseId(), line.productId(), line.baseUnitId(),
                            line.picked(), shipmentId, actor.getId());
                }
                BigDecimal missing = line.required().subtract(line.picked());
                if (missing.signum() > 0) {
                    dao.insertBackorder(conn, orderId, line, missing, shipmentId);
                    shortLines++;
                }
            }
            String code = String.format("PX%06d", shipmentId);
            new OrderWorkflowDao().transition(conn, order, Order.Status.SHIPPED, "ADVANCE",
                    "Phiếu xuất " + code + (shortLines > 0 ? ", còn nợ " + shortLines + " dòng hàng" : ""),
                    actor.getId(), order.getWarehouseId(), order.getApprovalReason());
            auditDao.log(conn, actor.getId(), "SHIPMENT_EXPORT", "SHIPMENT", shipmentId,
                    code + " · đơn " + order.getCode() + (shortLines > 0 ? " · xuất thiếu " + shortLines + " dòng" : ""));
            return shipmentId;
        });
    }
}
