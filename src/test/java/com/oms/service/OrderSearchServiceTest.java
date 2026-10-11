package com.oms.service;
import com.oms.dao.OrderSearchDao;
import com.oms.model.Order;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class OrderSearchServiceTest extends OrderWorkflowServiceTest {
    @Test void filtersAllFieldsAndTotalIncludesOtherPages() throws Exception {
        sql("UPDATE customers SET area_id=30 WHERE id=900");
        workflow.submit(id,rep);
        long second=orders.create(900,rep); orders.addLine(second,500,1,"3",rep); workflow.submit(second,rep);
        long draft=orders.create(900,rep); orders.addLine(draft,500,1,"2",rep);
        var filter=new OrderSearchDao.Filter(Order.Status.APPROVED,900L,rep.getId(),30L,LocalDate.now(),LocalDate.now());
        var result=new OrderSearchService().search(filter,1,1,manager);
        assertEquals(2,result.count()); assertEquals(1,result.items().size());
        assertEquals(0,new BigDecimal("80000").compareTo(result.total()));
        assertEquals(0,new OrderSearchService().search(filter,1,25,other).count());
        var empty=new OrderSearchDao.Filter(null,null,null,null,null,null);
        assertEquals(3,new OrderSearchService().search(empty,1,25,rep).count());
        assertThrows(ServiceException.class,()->new OrderSearchService().search(empty,0,25,rep));
        var bad=new OrderSearchDao.Filter(null,null,null,null,LocalDate.now(),LocalDate.now().minusDays(1));
        assertThrows(ServiceException.class,()->new OrderSearchService().search(bad,1,25,rep));
    }
}
