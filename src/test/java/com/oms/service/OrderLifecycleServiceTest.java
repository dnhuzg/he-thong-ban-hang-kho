package com.oms.service;
import com.oms.model.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class OrderLifecycleServiceTest extends OrderWorkflowServiceTest {
    @Test void cancelRequiresReasonReleasesBothReservationsAndKeepsHistory() throws Exception {
        workflow.submit(id,rep);
        var lifecycle=new OrderLifecycleService();
        assertThrows(ServiceException.class,()->lifecycle.cancel(id," ",rep));
        lifecycle.cancel(id,"Khách đổi nhu cầu",rep);
        assertEquals(Order.Status.CANCELLED,orders.get(id,rep).getStatus());
        assertEquals(0,count("order_inventory_reservations","1=1"));
        assertEquals(0,count("order_credit_reservations","1=1"));
        assertEquals(2,workflow.history(id,rep).size());
        assertThrows(ServiceException.class,()->lifecycle.cancel(id,"Lặp lại",rep));
    }
    @Test void shippedOrderCannotCancelAndWarehouseScopeIsChecked() throws Exception {
        var wh=user("kho",Role.WAREHOUSE); wh.setWarehouses(List.of(new NamedItem(10,"Kho A")));
        var foreign=user("kho2",Role.WAREHOUSE); foreign.setWarehouses(List.of(new NamedItem(20,"Kho B")));
        workflow.submit(id,rep);
        var lifecycle=new OrderLifecycleService();
        assertThrows(ForbiddenException.class,()->lifecycle.advance(id,Order.Status.PICKING,foreign));
        assertThrows(ServiceException.class,()->lifecycle.advance(id,Order.Status.SHIPPED,wh));
        lifecycle.advance(id,Order.Status.PICKING,wh); lifecycle.advance(id,Order.Status.SHIPPED,wh);
        assertThrows(ServiceException.class,()->lifecycle.cancel(id,"Huỷ",rep));
        assertEquals(1,count("warehouse_inventory","on_hand=95 AND reserved=0"));
        lifecycle.advance(id,Order.Status.DELIVERED,rep);
        var accountant=user("kt",Role.ACCOUNTANT); lifecycle.advance(id,Order.Status.CLOSED,accountant);
        assertEquals(Order.Status.CLOSED,orders.get(id,rep).getStatus());
        assertEquals(5,workflow.history(id,rep).size());
    }
    @Test void approvedOrderPastReservationDaysIsCancelledAndReleasesStock() throws Exception {
        workflow.submit(id,rep);
        var lifecycle=new OrderLifecycleService();
        LocalDateTime cutoff=LocalDateTime.now().minusDays(3);
        assertEquals(0,lifecycle.expireReservations(cutoff));      // vừa duyệt: còn trong hạn
        assertEquals(Order.Status.APPROVED,orders.get(id,rep).getStatus());
        sql("UPDATE order_status_history SET created_at=DATEADD('DAY',-4,CURRENT_TIMESTAMP)");
        assertEquals(1,lifecycle.expireReservations(cutoff));
        assertEquals(Order.Status.CANCELLED,orders.get(id,rep).getStatus());
        assertEquals(0,count("order_inventory_reservations","1=1"));
        assertEquals(0,count("order_credit_reservations","1=1"));
        assertEquals(1,count("warehouse_inventory","on_hand=100 AND reserved=0"));
        assertEquals(0,lifecycle.expireReservations(cutoff));      // chạy lại không huỷ lần hai
    }
    @Test void reservationExpiryLeavesOrdersAlreadyBeingPicked() throws Exception {
        var wh=user("kho",Role.WAREHOUSE); wh.setWarehouses(List.of(new NamedItem(10,"Kho A")));
        workflow.submit(id,rep);
        var lifecycle=new OrderLifecycleService();
        lifecycle.advance(id,Order.Status.PICKING,wh);
        sql("UPDATE order_status_history SET created_at=DATEADD('DAY',-4,CURRENT_TIMESTAMP)");
        assertEquals(0,lifecycle.expireReservations(LocalDateTime.now().minusDays(3)));
        assertEquals(Order.Status.PICKING,orders.get(id,rep).getStatus());
    }
    @Test void unpricedDraftCanCancelWithoutLosingHistory() throws Exception {
        sql("DELETE FROM price_list_items");
        new OrderLifecycleService().cancel(id,"Chưa có giá",rep);
        assertEquals(Order.Status.CANCELLED,orders.get(id,rep).getStatus());
        assertEquals(1,orders.get(id,rep).getUnpricedLines());
    }
}
