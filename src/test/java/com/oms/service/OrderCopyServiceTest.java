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
    @Test void copyDropsManualPriceAndPastDeliveryDateAndPreservesEveryLine() throws Exception {
        sql("INSERT INTO products(id,sku,name,category_id,base_unit_id) VALUES(501,'B','Hàng B',3,1)");
        sql("INSERT INTO price_list_items VALUES(100,501,20000,10000)");
        orders.addLine(id,501,1,"2",rep);
        sql("UPDATE orders SET delivery_date='2020-01-01',note='Giao buổi sáng' WHERE id="+id);
        sql("UPDATE order_lines SET manual_base_price=99000 WHERE order_id="+id);
        long copied=new OrderCopyService().copy(id,rep);
        var order=orders.get(copied,rep);
        assertEquals(2,order.getLines().size());
        assertNull(order.getDeliveryDate());
        assertEquals("Giao buổi sáng",order.getNote());
        assertTrue(order.getLines().stream().allMatch(line->line.getManualBasePrice()==null));
        assertEquals(0,new BigDecimal("90000").compareTo(order.getPayable()));
        assertEquals(0,count("order_inventory_reservations","order_id="+copied));
    }
}
