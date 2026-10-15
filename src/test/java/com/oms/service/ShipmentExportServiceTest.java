
package com.oms.service;

import com.oms.dao.PickingLotDao;
import com.oms.model.NamedItem;
import com.oms.model.Order;
import com.oms.model.PickingList;
import com.oms.model.Role;
import com.oms.model.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShipmentExportServiceTest extends PickingServiceTest {

    private final ShipmentExportService exports = new ShipmentExportService();

    /**
     * Soạn một dòng hàng theo lô FEFO.
     * Dữ liệu test có một lô đủ tồn cho mỗi sản phẩm.
     */
    private void pickFefo(long listId, PickingList.Line line,
                          BigDecimal picked, String shortReason) throws Exception {

        var lots = picking.suggestLots(listId, line.getId(), keeper);

        List<PickingLotDao.Selection> selections = picked.signum() == 0
                ? List.of()
                : List.of(new PickingLotDao.Selection(
                        lots.get(0).id(), picked, null));

        picking.pickWithLots(
                listId,
                line.getId(),
                picked,
                shortReason,
                selections,
                keeper
        );
    }

    /** Soạn đơn đầu: hàng A đủ hoặc thiếu; hàng B soạn đủ 2. */
    private long pickFirst(String pickedA) throws Exception {
        long id = picking.create(List.of(first), keeper);
        List<PickingList.Line> lines = picking.get(id, keeper).getLines();

        BigDecimal a = new BigDecimal(pickedA);

        pickFefo(
                id,
                lines.get(0),
                a,
                a.compareTo(new BigDecimal("5")) < 0 ? "Kệ thiếu" : null
        );

        pickFefo(
                id,
                lines.get(1),
                new BigDecimal("2"),
                null
        );

        return id;
    }

    @Test
    void exportNeedsCompletedPickingList() throws Exception {
        long list = pickFirst("5");

        assertTrue(exports.page(keeper).ready().isEmpty());
        assertThrows(ServiceException.class,
                () -> exports.export(first, keeper));

        picking.complete(list, keeper);

        assertEquals(1, exports.page(keeper).ready().size());
    }

    @Test
    void fullExportTurnsReservationIntoRealStockOut() throws Exception {
        picking.complete(pickFirst("5"), keeper);

        // Đơn thứ hai vẫn giữ chỗ 3 hàng A.
        assertEquals(1, count(
                "warehouse_inventory",
                "product_id=500 AND on_hand=100 AND reserved=8"
        ));

        long shipmentId = exports.export(first, keeper);

        assertEquals(
                Order.Status.SHIPPED,
                orders.get(first, rep).getStatus()
        );

        // Tồn tổng sau xuất kho.
        assertEquals(1, count(
                "warehouse_inventory",
                "product_id=500 AND on_hand=95 AND reserved=3"
        ));

        assertEquals(1, count(
                "warehouse_inventory",
                "product_id=501 AND on_hand=98 AND reserved=0"
        ));

        // S6-04: Tồn của đúng từng lô phải giảm theo số thực xuất.
        assertEquals(1, count(
                "inventory_lots",
                "warehouse_id=10 AND product_id=500 "
                        + "AND lot_no='LO-A' AND remaining_quantity=95"
        ));

        assertEquals(1, count(
                "inventory_lots",
                "warehouse_id=10 AND product_id=501 "
                        + "AND lot_no='LO-B' AND remaining_quantity=98"
        ));

        assertEquals(0, count(
                "order_inventory_reservations",
                "order_id=" + first
        ));

        assertEquals(1, count(
                "shipments",
                "id=" + shipmentId
                        + " AND order_id=" + first
                        + " AND status='READY'"
                        + " AND code='"
                        + String.format("PX%06d", shipmentId) + "'"
        ));

        assertEquals(2, count(
                "shipment_items",
                "shipment_id=" + shipmentId
        ));

        assertEquals(2, count(
                "inventory_movements",
                "movement_type='SHIPMENT_OUT' AND reference_id=" + shipmentId
        ));

        assertEquals(0, count("order_backorders", "1=1"));
        assertEquals(1, count("audit_logs", "action='SHIPMENT_EXPORT'"));

        // Không được xuất lại cùng một đơn.
        assertThrows(ServiceException.class,
                () -> exports.export(first, keeper));

        // Xuất lại bị từ chối, không được trừ tồn lô lần thứ hai.
        assertEquals(1, count(
                "inventory_lots",
                "product_id=500 AND lot_no='LO-A' AND remaining_quantity=95"
        ));
    }

    @Test
    void shortPickIsShippedPartlyAndRestBecomesBackorder() throws Exception {
        picking.complete(pickFirst("3"), keeper);

        long shipmentId = exports.export(first, keeper);

        // Xuất 3/5 hàng A, nhả toàn bộ giữ chỗ, còn nợ 2.
        assertEquals(1, count(
                "warehouse_inventory",
                "product_id=500 AND on_hand=97 AND reserved=3"
        ));

        // S6-04: Lô A chỉ giảm đúng 3 đơn vị.
        assertEquals(1, count(
                "inventory_lots",
                "product_id=500 AND lot_no='LO-A' AND remaining_quantity=97"
        ));

        assertEquals(1, count(
                "inventory_lots",
                "product_id=501 AND lot_no='LO-B' AND remaining_quantity=98"
        ));

        assertEquals(1, count(
                "shipment_items",
                "shipment_id=" + shipmentId
                        + " AND product_id=500 AND quantity=3"
        ));

        assertEquals(1, count(
                "order_backorders",
                "order_id=" + first
                        + " AND product_id=500"
                        + " AND quantity=2 AND reason='Kệ thiếu'"
        ));

        List<com.oms.model.ShipmentExport.Backorder> backorders =
                exports.page(keeper).backorders();

        assertEquals(1, backorders.size());
        assertEquals(0, new BigDecimal("2")
                .compareTo(backorders.get(0).getQuantity()));

        assertEquals(1, exports.page(keeper).recent().size());
    }

    @Test
    void otherWarehousesAndRolesCannotExport() throws Exception {
        picking.complete(pickFirst("5"), keeper);

        User foreign = user("kho3", Role.WAREHOUSE);
        foreign.setWarehouses(List.of(new NamedItem(20, "Kho B")));

        assertThrows(
                OrderService.NotFoundException.class,
                () -> exports.export(first, foreign)
        );

        assertThrows(
                ForbiddenException.class,
                () -> exports.export(first, rep)
        );

        assertTrue(exports.page(foreign).ready().isEmpty());

        assertEquals(
                Order.Status.PICKING,
                orders.get(first, rep).getStatus()
        );

        // Người không có quyền không được làm thay đổi tồn lô.
        assertEquals(1, count(
                "inventory_lots",
                "product_id=500 AND lot_no='LO-A' AND remaining_quantity=100"
        ));
    }
}
