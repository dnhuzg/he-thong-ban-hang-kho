package com.oms.service;
import com.oms.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class PortalOrderServiceTest extends OrderWorkflowServiceTest {
    private User customer,foreign;
    private final PortalOrderService portal=new PortalOrderService();
    @BeforeEach void portalSetup() throws Exception {
        customer=user("customer",Role.CUSTOMER); foreign=user("foreign",Role.CUSTOMER);
        sql("INSERT INTO customer_accounts VALUES(900,"+customer.getId()+")");
        sql("INSERT INTO customers(id,code,name,group_id,sales_rep_id,credit_limit,credit_days,serving_warehouse_id) VALUES(901,'DL2','Đại lý khác',2,"+other.getId()+",1000000,30,10)");
        sql("INSERT INTO customer_accounts VALUES(901,"+foreign.getId()+")");
    }
    @Test void ownCatalogCartAndAssignedRepConfirmation() throws Exception {
        assertEquals(1,portal.catalog("",1,customer).size());
        assertEquals(0,portal.catalog("",1,foreign).size());
        long cart=portal.create(customer); portal.add(cart,500,1,"2",customer);
        assertEquals(0,new BigDecimal("20000").compareTo(portal.cart(cart,customer).total()));
        assertThrows(OrderService.NotFoundException.class,()->portal.cart(cart,foreign));
        assertThrows(OrderService.NotFoundException.class,()->portal.add(cart,500,1,"1",foreign));
        assertEquals(Order.Status.PENDING_APPROVAL,portal.submit(cart,customer));
        assertEquals(0,count("order_inventory_reservations","order_id="+cart));
        assertThrows(ForbiddenException.class,()->workflow.decide(cart,"APPROVE",null,customer));
        assertThrows(OrderService.NotFoundException.class,()->workflow.decide(cart,"APPROVE",null,other));
        workflow.decide(cart,"APPROVE",null,rep);
        assertEquals(Order.Status.APPROVED,orders.get(cart,customer).getStatus());
        assertEquals(1,count("order_inventory_reservations","order_id="+cart));
    }
    @Test void customerCannotSetManualPrice() throws Exception {
        long cart=portal.create(customer); portal.add(cart,500,1,"2",customer);
        long line=orders.get(cart,customer).getLines().get(0).getId();
        assertThrows(ForbiddenException.class,()->orders.updateLine(cart,line,1,"2","1",customer));
        orders.updateLine(cart,line,1,"3","",customer);
        assertEquals(3,orders.get(cart,customer).getLines().get(0).getQuantity());
    }
    @Test void violationsCannotBeApprovedBySalesRepAndUnmappedAccountCannotCreate() throws Exception {
        sql("UPDATE customers SET credit_limit=0 WHERE id=900");
        long cart=portal.create(customer); portal.add(cart,500,1,"2",customer); portal.submit(cart,customer);
        assertThrows(ForbiddenException.class,()->workflow.decide(cart,"APPROVE",null,rep));
        workflow.decide(cart,"APPROVE",null,manager);
        User unmapped=user("unmapped",Role.CUSTOMER);
        assertThrows(ForbiddenException.class,()->portal.create(unmapped));
        assertThrows(OrderService.NotFoundException.class,()->orders.create(901,customer));
    }
}
