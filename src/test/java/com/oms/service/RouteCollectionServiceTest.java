package com.oms.service;

import com.oms.dao.RouteCollectionDao;

import com.oms.model.Role;
import com.oms.model.User;
import com.oms.support.DatabaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RouteCollectionServiceTest extends DatabaseTest {

    private final RouteCollectionService collections = new RouteCollectionService();
    private User rep;
    private User accountant;

    @BeforeEach
    void prepareRouteCollectionData() throws Exception {
        rep = user("route-sales", Role.SALES_REP);
        accountant = user("route-accountant", Role.ACCOUNTANT);
        sql("INSERT INTO customers (id, code, name, group_id, sales_rep_id, credit_limit, credit_days) VALUES "
                + "(900, 'DL900', 'Đại lý thu tiền', 1, " + rep.getId() + ", 100000, 30)");
        sql("INSERT INTO invoices (id, code, customer_id, total_amount, issued_at, due_date, status, issued_by, paid_amount, credited_amount) "
                + "VALUES (1000, 'HD000001', 900, 1000, CURRENT_TIMESTAMP, CURRENT_DATE, 'ISSUED', "
                + accountant.getId() + ", 0, 0)");
        sql("INSERT INTO customer_receivables (customer_id, document_code, issued_on, outstanding) "
                + "VALUES (900, 'HD000001', CURRENT_DATE, 1000)");
    }

    @Test
    void createsPendingCollectionAndAccountantConfirmsOnce() throws Exception {
        long id = collections.record(900, new BigDecimal("400"), LocalDate.now(), "receipts/proof.png", rep);

        RouteCollectionDao.PageData pending = collections.page(rep);
        assertEquals(1, pending.records().size());
        assertEquals("PENDING", pending.records().get(0).status());
        assertEquals("receipts/proof.png", pending.records().get(0).receiptPath());
        assertEquals(0, new BigDecimal("600.00").compareTo(pending.customers().get(0).outstanding()));

        collections.confirm(id, accountant);
        assertEquals("CONFIRMED", collections.page(accountant).records().get(0).status());
        assertThrows(ServiceException.class, () -> collections.confirm(id, accountant));
        assertEquals(1, count("audit_logs", "action='ROUTE_COLLECTION_CONFIRM'"));
    }

    @Test
    void salespersonCannotRecordForAnotherSalespersonsCustomer() throws Exception {
        User otherRep = user("route-other-sales", Role.SALES_REP);
        sql("INSERT INTO customers (id, code, name, group_id, sales_rep_id, credit_limit, credit_days) VALUES "
                + "(901, 'DL901', 'Đại lý nhân viên khác', 1, " + otherRep.getId() + ", 100000, 30)");

        assertThrows(ForbiddenException.class, () -> collections.record(901, new BigDecimal("100"),
                LocalDate.now(), "receipts/proof.png", rep));
        assertEquals(0, count("route_collections", "1=1"));
    }

    @Test
    void requiresReceiptAndOnlyAccountantCanConfirm() {
        assertThrows(ServiceException.class, () -> collections.record(900, new BigDecimal("100"),
                LocalDate.now(), " ", rep));
        assertThrows(ForbiddenException.class, () -> collections.confirm(1, rep));
    }
}
