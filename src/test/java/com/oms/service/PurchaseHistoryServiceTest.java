package com.oms.service;
import com.oms.model.*;
import com.oms.support.DatabaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class PurchaseHistoryServiceTest extends DatabaseTest {
    private User rep,other;
    private final OrderService orders=new OrderService();
    private long source;
    @BeforeEach void setup() throws Exception {
        rep=user("rep",Role.SALES_REP); other=user("other",Role.SALES_REP);
        sql("INSERT INTO customers(id,code,name,group_id,sales_rep_id) VALUES(900,'DL','Đại lý',1,"+rep.getId()+")");
        sql("INSERT INTO products(id,sku,name,category_id,base_unit_id) VALUES(500,'A','Hàng',3,1)");
        sql("INSERT INTO price_lists(id,code,name,customer_group_id,valid_from) VALUES(100,'BG','Giá',1,'2020-01-01')");
        sql("INSERT INTO price_list_items VALUES(100,500,10000,9000)");
        source=orders.create(900,rep); orders.addLine(source,500,1,"6",rep);
        sql("UPDATE order_lines SET unit_price=10000,gross_amount=quantity*10000,discount_amount=0 WHERE order_id="+source);
        sql("UPDATE orders SET status='SHIPPED' WHERE id="+source);
    }
    @Test void historyUsesThreeMonthsOnlyAndAveragePerPurchase() throws Exception {
        long next=orders.create(900,rep); orders.addLine(next,500,1,"10",rep);
        sql("UPDATE order_lines SET unit_price=10000,gross_amount=quantity*10000,discount_amount=0 WHERE order_id="+next);
        sql("UPDATE orders SET status='CLOSED' WHERE id="+next);
        var result=new PurchaseHistoryService().history(900,rep);
        assertEquals(1,result.size()); assertEquals(2,result.get(0).purchases());
        assertEquals(0,new BigDecimal("8").compareTo(result.get(0).averageQuantity()));
        sql("UPDATE orders SET created_at='"+LocalDate.now().minusMonths(3).minusDays(1)+"' WHERE id="+source);
        assertEquals(1,new PurchaseHistoryService().history(900,rep).get(0).purchases());
        assertThrows(OrderService.NotFoundException.class,()->new PurchaseHistoryService().history(900,other));
    }
    @Test void addsLastGroupUsingCurrentPriceAndRollsBackIfInactive() throws Exception {
        long target=orders.create(900,rep);
        sql("UPDATE price_list_items SET sale_price=12000");
        new PurchaseHistoryService().addLastPurchase(target,rep);
        assertEquals(0,new BigDecimal("72000").compareTo(orders.get(target,rep).getPayable()));
        assertThrows(OrderService.NotFoundException.class,()->new PurchaseHistoryService().addLastPurchase(target,other));
        sql("UPDATE products SET status='DISCONTINUED' WHERE id=500");
        assertThrows(ServiceException.class,()->new PurchaseHistoryService().addLastPurchase(target,rep));
        assertEquals(6,orders.get(target,rep).getLines().get(0).getQuantity());
    }
}
