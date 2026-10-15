package com.oms.service;
import com.oms.model.*;
import org.junit.jupiter.api.Test;
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
    @Test void unpricedDraftCanCancelWithoutLosingHistory() throws Exception {
        sql("DELETE FROM price_list_items");
        new OrderLifecycleService().cancel(id,"Chưa có giá",rep);
        assertEquals(Order.Status.CANCELLED,orders.get(id,rep).getStatus());
        assertEquals(1,orders.get(id,rep).getUnpricedLines());
    }
}
