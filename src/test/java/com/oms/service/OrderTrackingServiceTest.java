package com.oms.service;

import com.oms.model.PickingList;
import com.oms.model.Role;
import com.oms.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderTrackingServiceTest extends PickingServiceTest {

    private final OrderTrackingService tracking = new OrderTrackingService();
    private User dealer;
    private User stranger;

    @BeforeEach
    void dealers() throws Exception {
        dealer = user("daily", Role.CUSTOMER);
        stranger = user("dailykhac", Role.CUSTOMER);
        sql("INSERT INTO customers(id,code,name,group_id) VALUES(901,'DL2','Đại lý khác',1)");
        sql("INSERT INTO customer_accounts VALUES(900," + dealer.getId() + "),(901," + stranger.getId() + ")");
    }

    @Test
    void dealerSeesOwnSubmittedOrdersOnly() throws Exception {
        orders.create(900, rep);

        assertEquals(2, tracking.list(dealer).size());
        assertTrue(tracking.list(stranger).isEmpty());
        assertThrows(OrderService.NotFoundException.class, () -> tracking.detail(first, stranger));
        assertThrows(ForbiddenException.class, () -> tracking.list(rep));
    }

    @Test
    void detailShowsShippedAndDeliveredQuantitiesAndShortDelivery() throws Exception {
        OrderTrackingService.Detail before = tracking.detail(first, dealer);
        assertFalse(before.exported());
        // Đơn vừa duyệt: chỉ mốc Đã duyệt có thời gian
        assertEquals(5, before.milestones().size());
        assertTrue(before.milestones().get(0).isDone());
        assertFalse(before.milestones().get(1).isDone());

        long list = picking.create(List.of(first), keeper);
        for (PickingList.Line line : picking.get(list, keeper).getLines()) {
            boolean a = "A".equals(line.getSku());
            picking.pick(list, line.getId(), a ? new BigDecimal("3") : line.getRequired(), a ? "Thiếu" : null, keeper);
        }
        picking.complete(list, keeper);
        long shipment = new ShipmentExportService().export(first, keeper);

        OrderTrackingService.Detail shipped = tracking.detail(first, dealer);
        assertTrue(shipped.exported());
        // Đã soạn, đã xuất, đang giao; chưa giao xong
        assertTrue(shipped.milestones().get(3).isDone());
        assertFalse(shipped.milestones().get(4).isDone());
        assertTrue(shipped.isShortShipped());
        OrderTrackingService.LineStatus a = shipped.lines().stream()
                .filter(l -> "A".equals(l.getLine().getSku())).findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("3").compareTo(a.getShipped()));
        assertEquals(0, new BigDecimal("2").compareTo(a.getMissing()));
        assertEquals(0, BigDecimal.ZERO.compareTo(a.getDelivered()));

        sql("UPDATE shipments SET status='DELIVERED', delivered_at=CURRENT_TIMESTAMP WHERE id=" + shipment);
        assertTrue(tracking.detail(first, dealer).milestones().get(4).isDone());
        a = tracking.detail(first, dealer).lines().stream()
                .filter(l -> "A".equals(l.getLine().getSku())).findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("3").compareTo(a.getDelivered()));
    }
}
