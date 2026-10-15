
package com.oms.service;

import com.oms.dao.PickingLotDao;
import com.oms.model.FefoLot;
import com.oms.model.PickingList;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FefoPickingServiceTest extends PickingServiceTest {

    private final ShipmentExportService exports = new ShipmentExportService();

    private long createPickingList() throws Exception {
        return picking.create(List.of(first), keeper);
    }

    private PickingList.Line productA(long listId) throws Exception {
        return picking.get(listId, keeper).getLines().stream()
                .filter(line -> "A".equals(line.getSku()))
                .findFirst()
                .orElseThrow();
    }

    private PickingList.Line productB(long listId) throws Exception {
        return picking.get(listId, keeper).getLines().stream()
                .filter(line -> "B".equals(line.getSku()))
                .findFirst()
                .orElseThrow();
    }

    private void addEarlierLot(String lotNo, int quantity) throws Exception {
        sql("INSERT INTO inventory_lots "
                + "(warehouse_id, product_id, lot_no, expiry_date, "
                + "remaining_quantity, unit_cost, received_at) "
                + "VALUES (10,500,'" + lotNo + "','2027-01-01',"
                + quantity + ",1000,CURRENT_TIMESTAMP)");
    }

    private static PickingLotDao.Selection select(
            FefoLot lot, String quantity, String reason) {
        return new PickingLotDao.Selection(
                lot.id(), new BigDecimal(quantity), reason);
    }

    @Test
    void suggestsNearestExpiryFirst() throws Exception {
        addEarlierLot("LO-SOM", 3);

        long listId = createPickingList();
        PickingList.Line line = productA(listId);

        List<FefoLot> lots =
                picking.suggestLots(listId, line.getId(), keeper);

        assertEquals(2, lots.size());
        assertEquals("LO-SOM", lots.get(0).lotNo());
        assertEquals("LO-A", lots.get(1).lotNo());
        assertTrue(lots.get(0).recommended());
    }

    @Test
    void automaticallyDistributesRequiredQuantityAcrossFefoLots()
            throws Exception {
        addEarlierLot("LO-SOM", 3);

        long listId = createPickingList();
        PickingList.Line line = productA(listId);

        List<FefoLot> lots =
                picking.suggestLots(listId, line.getId(), keeper);

        assertEquals(2, lots.size());

        // Lô gần hết hạn nhất phải được đề xuất trước.
        assertEquals("LO-SOM", lots.get(0).lotNo());
        assertEquals(0, new BigDecimal("3")
                .compareTo(lots.get(0).suggestedQuantity()));
        assertTrue(lots.get(0).recommended());

        // Lô tiếp theo bổ sung đủ số lượng còn thiếu.
        assertEquals("LO-A", lots.get(1).lotNo());
        assertEquals(0, new BigDecimal("2")
                .compareTo(lots.get(1).suggestedQuantity()));
        assertTrue(lots.get(1).recommended());

        // Tổng đề xuất phải bằng số lượng cần soạn.
        BigDecimal total = lots.stream()
                .map(FefoLot::suggestedQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(0, line.getRequired().compareTo(total));
    }

    @Test
    void allowsPickingFromMultipleLotsInFefoOrder() throws Exception {
        addEarlierLot("LO-SOM", 3);

        long listId = createPickingList();
        PickingList.Line line = productA(listId);

        List<FefoLot> lots =
                picking.suggestLots(listId, line.getId(), keeper);

        picking.pickWithLots(
                listId,
                line.getId(),
                new BigDecimal("5"),
                null,
                List.of(
                        select(lots.get(0), "3", null),
                        select(lots.get(1), "2", null)
                ),
                keeper
        );

        assertEquals(2, count(
                "picking_line_lots",
                "picking_line_id=" + line.getId()
        ));
    }

    @Test
    void requiresReasonWhenSkippingEarlierLot() throws Exception {
        addEarlierLot("LO-SOM", 3);

        long listId = createPickingList();
        PickingList.Line line = productA(listId);

        List<FefoLot> lots =
                picking.suggestLots(listId, line.getId(), keeper);

        assertThrows(ServiceException.class,
                () -> picking.pickWithLots(
                        listId,
                        line.getId(),
                        new BigDecimal("5"),
                        null,
                        List.of(select(lots.get(1), "5", null)),
                        keeper
                ));

        picking.pickWithLots(
                listId,
                line.getId(),
                new BigDecimal("5"),
                null,
                List.of(select(
                        lots.get(1),
                        "5",
                        "Lô đầu bị hỏng bao bì"
                )),
                keeper
        );

        assertEquals(1, count(
                "picking_line_lots",
                "picking_line_id=" + line.getId()
                        + " AND override_reason='Lô đầu bị hỏng bao bì'"
        ));
    }

    @Test
    void exportsExactSelectedLotsAndCanExhaustOneLot() throws Exception {
        addEarlierLot("LO-SOM", 3);

        long listId = createPickingList();
        PickingList.Line lineA = productA(listId);
        PickingList.Line lineB = productB(listId);

        List<FefoLot> aLots =
                picking.suggestLots(listId, lineA.getId(), keeper);

        List<FefoLot> bLots =
                picking.suggestLots(listId, lineB.getId(), keeper);

        picking.pickWithLots(
                listId,
                lineA.getId(),
                new BigDecimal("5"),
                null,
                List.of(
                        select(aLots.get(0), "3", null),
                        select(aLots.get(1), "2", null)
                ),
                keeper
        );

        picking.pickWithLots(
                listId,
                lineB.getId(),
                new BigDecimal("2"),
                null,
                List.of(select(bLots.get(0), "2", null)),
                keeper
        );

        picking.complete(listId, keeper);
        exports.export(first, keeper);

        assertEquals(1, count(
                "inventory_lots",
                "product_id=500 AND lot_no='LO-SOM' "
                        + "AND remaining_quantity=0"
        ));

        assertEquals(1, count(
                "inventory_lots",
                "product_id=500 AND lot_no='LO-A' "
                        + "AND remaining_quantity=98"
        ));

        assertEquals(1, count(
                "inventory_lots",
                "product_id=501 AND lot_no='LO-B' "
                        + "AND remaining_quantity=98"
        ));
    }
}
