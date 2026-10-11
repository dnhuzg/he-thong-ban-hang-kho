package com.oms.service;
import com.oms.config.Db;
import com.oms.dao.OrderStockDao;
import com.oms.model.Role;
import com.oms.support.DatabaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class OrderStockServiceTest extends DatabaseTest {
    private long first,second;
    @BeforeEach void setUp() throws Exception {
        var rep=user("rep",Role.SALES_REP);
        sql("INSERT INTO customers(id,code,name,group_id,sales_rep_id,serving_warehouse_id) VALUES(900,'DL','Đại lý',1,"+rep.getId()+",10)");
        sql("INSERT INTO products(id,sku,name,category_id,base_unit_id) VALUES(500,'A','Hàng',3,1)");
        sql("INSERT INTO warehouse_inventory VALUES(10,500,10,0)");
        first=new OrderService().create(900,rep); second=new OrderService().create(900,rep);
    }
    @Test void twoConcurrentOrdersCannotOverReserve() throws Exception {
        var latch=new CountDownLatch(1);
        var pool=Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> a=()->reserve(first,latch), b=()->reserve(second,latch);
            var f1=pool.submit(a); var f2=pool.submit(b); latch.countDown();
            assertNotEquals(f1.get(10,TimeUnit.SECONDS),f2.get(10,TimeUnit.SECONDS));
        } finally { pool.shutdownNow(); }
        try(var conn=Db.getConnection()) { assertEquals(0,new BigDecimal("3").compareTo(new OrderStockDao().available(conn,10,500,false))); }
        assertEquals(1,count("order_inventory_reservations","1=1"));
    }
    private boolean reserve(long id,CountDownLatch latch) throws Exception {
        latch.await();
        try { Db.inTransaction(c->{new OrderStockDao().reserve(c,id,10,Map.of(500L,new BigDecimal("7")));return null;}); return true; }
        catch(ServiceException e) { return false; }
    }
    @Test void releaseRestoresAvailabilityAndFailureRollsBack() throws Exception {
        Db.inTransaction(c->{new OrderStockDao().reserve(c,first,10,Map.of(500L,new BigDecimal("7")));return null;});
        assertThrows(ServiceException.class,()->Db.inTransaction(c->{new OrderStockDao().reserve(c,second,10,Map.of(500L,new BigDecimal("4")));return null;}));
        Db.inTransaction(c->{new OrderStockDao().release(c,first,false);return null;});
        try(var conn=Db.getConnection()) { assertEquals(0,new BigDecimal("10").compareTo(new OrderStockDao().available(conn,10,500,false))); }
    }
}
