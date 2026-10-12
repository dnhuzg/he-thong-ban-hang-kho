package com.oms.service;
import com.oms.model.Order;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class OrderCopyServiceTest extends OrderWorkflowServiceTest {
    @Test void copyIsDraftAndUsesCurrentPriceWithoutChangingSource() throws Exception {
        workflow.submit(id,rep); sql("UPDATE price_list_items SET sale_price=12000");
        long copy=new OrderCopyService().copy(id,rep);
        var order=orders.get(copy,rep);
        assertEquals(Order.Status.DRAFT,order.getStatus()); assertEquals(5,order.getLines().get(0).getQuantity());
        assertEquals(0,new BigDecimal("60000").compareTo(order.getPayable()));
        assertEquals(0,new BigDecimal("50000").compareTo(orders.get(id,rep).getPayable()));
        assertEquals(1,count("order_inventory_reservations","1=1"));
        assertEquals(1,count("audit_logs","action='ORDER_COPY'"));
    }
    @Test void missingPriceOrWrongScopeLeavesNoPartialCopy() throws Exception {
        assertThrows(OrderService.NotFoundException.class,()->new OrderCopyService().copy(id,other));
        sql("DELETE FROM price_list_items");
        assertThrows(ServiceException.class,()->new OrderCopyService().copy(id,rep));
        assertEquals(1,count("orders","1=1")); assertEquals(1,count("order_lines","1=1"));
    }
}
