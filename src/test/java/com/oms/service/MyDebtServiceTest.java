package com.oms.service;

import com.oms.model.Invoice;
import com.oms.model.Role;
import com.oms.model.User;
import com.oms.support.DatabaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MyDebtServiceTest extends DatabaseTest {

    private final MyDebtService service = new MyDebtService();
    private final InvoiceService invoices = new InvoiceService();
    private User owner;
    private User other;

    @BeforeEach
    void fixture() throws Exception {
        owner = user("daily1", Role.CUSTOMER);
        other = user("daily2", Role.CUSTOMER);
        LocalDate today = LocalDate.now();
        sql("INSERT INTO customers(id,code,name,group_id,credit_limit,credit_days) VALUES(900,'DL1','Đại lý Một',1,1000000,30),(901,'DL2','Đại lý Hai',1,0,0)");
        sql("INSERT INTO customer_accounts VALUES(900," + owner.getId() + "),(901," + other.getId() + ")");
        sql("INSERT INTO invoices(id,code,customer_id,total_amount,paid_amount,issued_at,due_date,status) VALUES"
                + "(1,'HD000001',900,300000,100000,'" + today.minusDays(40) + " 08:00:00','" + today.minusDays(10) + "','PARTIAL'),"
                + "(2,'HD000002',900,150000,0,'" + today.minusDays(2) + " 08:00:00','" + today.plusDays(28) + "','ISSUED'),"
                + "(3,'HD000003',901,90000,0,'" + today.minusDays(2) + " 08:00:00','" + today.plusDays(28) + "','ISSUED')");
        sql("INSERT INTO customer_receivables(customer_id,document_code,issued_on,outstanding) VALUES"
                + "(900,'HD000001','" + today.minusDays(40) + "',200000),(900,'HD000002','" + today.minusDays(2) + "',150000),"
                + "(901,'HD000003','" + today.minusDays(2) + "',90000)");
    }

    @Test
    void showsOnlyOwnInvoicesDebtAndRemainingLimit() throws Exception {
        MyDebtService.View view = service.view(owner);

        assertEquals("DL1", view.customer().getCode());
        assertEquals(2, view.invoices().size());
        assertEquals(0, new BigDecimal("350000").compareTo(view.debt()));
        assertEquals(0, new BigDecimal("650000").compareTo(view.remaining()));
        assertEquals(0, new BigDecimal("200000").compareTo(view.overdue()));
        assertEquals(35, view.getUsagePercent());
        Invoice old = view.invoices().stream().filter(i -> "HD000001".equals(i.getCode())).findFirst().orElseThrow();
        assertTrue(old.isOverdue());
    }

    @Test
    void customerCanOnlyDownloadOwnInvoice() throws Exception {
        assertTrue(invoices.pdfForCustomer(1, owner).length > 1000);
        assertThrows(InvoiceService.NotFoundException.class, () -> invoices.pdfForCustomer(3, owner));
        assertThrows(InvoiceService.NotFoundException.class, () -> invoices.pdfForCustomer(1, other));
    }

    @Test
    void accountWithoutDealerIsRejected() throws Exception {
        User staff = user("nvkd", Role.SALES_REP);
        assertThrows(ForbiddenException.class, () -> service.view(staff));
    }
}
