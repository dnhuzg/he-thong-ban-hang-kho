package com.oms.service;
import com.oms.model.*;
import com.oms.support.DatabaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class OrderWorkflowServiceTest extends DatabaseTest {
    protected User rep,manager,other;
    protected long id;
    protected final OrderService orders=new OrderService();
    protected final OrderWorkflowService workflow=new OrderWorkflowService();
    @BeforeEach void setup() throws Exception {
        rep=user("rep",Role.SALES_REP); manager=user("manager",Role.SALES_MANAGER); other=user("other",Role.SALES_REP);
        sql("INSERT INTO customers(id,code,name,group_id,sales_rep_id,credit_limit,credit_days,serving_warehouse_id) VALUES(900,'DL','Đại lý',1,"+rep.getId()+",1000000,30,10)");
        sql("INSERT INTO customer_delivery_points(id,customer_id,address,receiver_name,is_default) VALUES(800,900,'Kho A','Anh A',TRUE)");
        sql("INSERT INTO products(id,sku,name,category_id,base_unit_id) VALUES(500,'A','Hàng',3,1)");
        sql("INSERT INTO price_lists(id,code,name,customer_group_id,valid_from) VALUES(100,'BG','Giá',1,'2020-01-01')");
        sql("INSERT INTO price_list_items VALUES(100,500,10000,9000)");
        sql("INSERT INTO warehouse_inventory VALUES(10,500,100,0)");
        id=orders.create(900,rep); orders.addLine(id,500,1,"5",rep);
    }
    @Test void normalSubmissionReservesAndFreezesPrice() throws Exception {
        assertEquals(Order.Status.APPROVED,workflow.submit(id,rep));
        assertEquals(1,count("order_inventory_reservations","order_id="+id));
        sql("UPDATE price_list_items SET sale_price=12000");
        assertEquals(0,new BigDecimal("50000").compareTo(orders.get(id,rep).getPayable()));
        assertThrows(ServiceException.class,()->orders.addLine(id,500,1,"1",rep));
        assertThrows(ServiceException.class,()->orders.delete(id,rep));
        assertThrows(ServiceException.class,()->workflow.submit(id,rep));
    }
    @Test void screenShowsWhoDecidesAndReadableHistory() throws Exception {
        sql("UPDATE customers SET credit_limit=1 WHERE id=900");
        workflow.submit(id,rep);
        Order order=orders.get(id,manager);
        assertTrue(OrderWorkflowService.canDecide(order,manager));
        assertFalse(OrderWorkflowService.canDecide(order,rep));
        assertTrue(order.isCancellable());
        workflow.decide(id,"REJECT","Vượt hạn mức quá nhiều",manager);
        var last=workflow.history(id,manager).get(1);
        assertEquals("Từ chối",last.getActionLabel());
        assertEquals("Bị từ chối",last.getToLabel());
        assertEquals("manager",last.getActorName());
    }
    @Test void violationWaitsForManagerAndHistoryIsAppendOnly() throws Exception {
        sql("UPDATE customers SET credit_limit=1 WHERE id=900");
        assertEquals(Order.Status.PENDING_APPROVAL,workflow.submit(id,rep));
        assertEquals(0,count("order_inventory_reservations","1=1"));
        assertTrue(workflow.pending(manager).get(0).reason().contains("49999"));
        assertThrows(ForbiddenException.class,()->workflow.decide(id,"APPROVE",null,rep));
        assertThrows(ServiceException.class,()->workflow.decide(id,"RETURN"," ",manager));
        workflow.decide(id,"RETURN","Sửa số lượng",manager);
        assertEquals(Order.Status.DRAFT,orders.get(id,rep).getStatus());
        assertThrows(ServiceException.class,()->orders.delete(id,rep));
        workflow.submit(id,rep); workflow.decide(id,"APPROVE",null,manager);
        assertEquals(4,workflow.history(id,rep).size());
        assertEquals(1,count("order_inventory_reservations","1=1"));
        assertThrows(ServiceException.class,()->workflow.decide(id,"APPROVE",null,manager));
    }
    @Test void discountBelowFloorNeedsApproval() throws Exception {
        sql("INSERT INTO discount_policies(id,name,product_id,discount_type) VALUES(20,'Giảm 20%',500,'PERCENT')");
        sql("INSERT INTO discount_tiers VALUES(20,1,20)");
        assertEquals(Order.Status.PENDING_APPROVAL,workflow.submit(id,rep));
        assertTrue(orders.get(id,rep).getApprovalReason().contains("giá sàn"));
    }
    @Test void changedDebtOrStockBlocksAndRollsBack() throws Exception {
        sql("UPDATE warehouse_inventory SET on_hand=4 WHERE product_id=500");
        assertThrows(ServiceException.class,()->workflow.submit(id,rep));
        assertEquals(Order.Status.DRAFT,orders.get(id,rep).getStatus());
        sql("UPDATE warehouse_inventory SET on_hand=100 WHERE product_id=500");
        sql("UPDATE customers SET status='LOCKED' WHERE id=900");
        assertThrows(ServiceException.class,()->workflow.submit(id,rep));
        assertEquals(0,count("order_status_history","1=1"));
        assertThrows(OrderService.NotFoundException.class,()->workflow.submit(id,other));
    }
}
