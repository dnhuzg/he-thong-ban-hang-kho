package com.oms.service;
import com.oms.model.Order;
import org.junit.jupiter.api.Test;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class OrderSubmissionConcurrencyTest extends OrderWorkflowServiceTest {
    @Test void twoSimultaneousSubmissionsShareTheCreditLimitCorrectly() throws Exception {
        sql("UPDATE customers SET credit_limit=60000 WHERE id=900");
        long second=orders.create(900,rep); orders.addLine(second,500,1,"5",rep);
        var start=new CountDownLatch(1); var pool=Executors.newFixedThreadPool(2);
        try {
            var a=pool.submit(()->{start.await();return workflow.submit(id,rep);});
            var b=pool.submit(()->{start.await();return workflow.submit(second,rep);});
            start.countDown(); var firstStatus=a.get(10,TimeUnit.SECONDS); var secondStatus=b.get(10,TimeUnit.SECONDS);
            assertNotEquals(firstStatus,secondStatus);
            assertTrue(firstStatus==Order.Status.APPROVED || secondStatus==Order.Status.APPROVED);
            assertTrue(firstStatus==Order.Status.PENDING_APPROVAL || secondStatus==Order.Status.PENDING_APPROVAL);
            assertEquals(1,count("order_credit_reservations","1=1"));
            assertEquals(1,count("warehouse_inventory","on_hand=100 AND reserved=5"));
        } finally { pool.shutdownNow(); }
    }
}
