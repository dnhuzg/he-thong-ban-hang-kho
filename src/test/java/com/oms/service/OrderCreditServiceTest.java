package com.oms.service;
import com.oms.model.Role;
import com.oms.model.User;
import com.oms.support.DatabaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class OrderCreditServiceTest extends DatabaseTest {
    private User rep;
    private long id;
    @BeforeEach void setUp() throws Exception {
        rep=user("rep",Role.SALES_REP);
        sql("INSERT INTO customers(id,code,name,group_id,sales_rep_id,credit_limit,credit_days) VALUES(900,'DL','Đại lý',1,"+rep.getId()+",100000,30)");
        id=new OrderService().create(900,rep);
    }
    @Test void creditBoundaryAndCommittedAmounts() throws Exception {
        sql("INSERT INTO customer_receivables(customer_id,document_code,issued_on,outstanding) VALUES(900,'HD1',CURRENT_DATE,20000)");
        try(var conn=com.oms.config.Db.getConnection()) {
            var c=new com.oms.dao.CustomerDao().findById(conn,900);
            var service=new OrderCreditService();
            assertFalse(service.evaluate(conn,c,id,new BigDecimal("80000")).requiresApproval());
            assertTrue(service.evaluate(conn,c,id,new BigDecimal("80000.01")).requiresApproval());
            new com.oms.dao.OrderCreditDao().reserve(conn,id,900,new BigDecimal("10000"));
            assertEquals(0,new BigDecimal("70000").compareTo(service.evaluate(conn,c,0,BigDecimal.ZERO).remaining()));
        }
    }
    @Test void overdueUnpaidBlocksCreationButPaidDocumentDoesNot() throws Exception {
        sql("INSERT INTO customer_receivables(customer_id,document_code,issued_on,outstanding) VALUES(900,'OLD','"+LocalDate.now().minusDays(31)+"',1)");
        assertTrue(new OrderCreditService().quote(id,rep).overdue());
        assertThrows(ServiceException.class,()->new OrderService().create(900,rep));
        sql("UPDATE customer_receivables SET outstanding=0");
        assertDoesNotThrow(()->new OrderService().create(900,rep));
    }
    @Test void hidesCreditFromOtherRep() throws Exception {
        User other=user("other",Role.SALES_REP);
        assertThrows(OrderService.NotFoundException.class,()->new OrderCreditService().quote(id,other));
    }
}
