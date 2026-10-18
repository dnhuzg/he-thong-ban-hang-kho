package com.oms.service;

import com.oms.dao.FinanceOverviewDao;
import com.oms.model.Role;
import com.oms.model.User;
import com.oms.support.DatabaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ReconciliationServiceTest extends DatabaseTest {

    private final ReconciliationService service = new ReconciliationService();
    private User accountant;
    private User agencyUser;
    private User otherAgencyUser;

    @BeforeEach
    void prepareReconciliationSchema() throws Exception {
        sql("CREATE TABLE sales_returns (id BIGINT PRIMARY KEY, code VARCHAR(30), customer_id BIGINT, created_at TIMESTAMP)");
        sql("CREATE TABLE return_credit_notes (id BIGINT PRIMARY KEY, code VARCHAR(30), return_id BIGINT, customer_id BIGINT, amount DECIMAL(15,2), created_at TIMESTAMP)");
        sql("CREATE TABLE debt_reconciliations (id BIGINT AUTO_INCREMENT PRIMARY KEY, code VARCHAR(20) NOT NULL UNIQUE, customer_id BIGINT NOT NULL, period_start DATE NOT NULL, period_end DATE NOT NULL, opening_balance DECIMAL(15,2) NOT NULL, increase_amount DECIMAL(15,2) NOT NULL, decrease_amount DECIMAL(15,2) NOT NULL, closing_balance DECIMAL(15,2) NOT NULL, status VARCHAR(20) DEFAULT 'PENDING', dispute_reason VARCHAR(1000), created_by BIGINT NOT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, details_snapshotted_at TIMESTAMP NULL, responded_at TIMESTAMP NULL, UNIQUE(customer_id,period_start,period_end))");
        sql("CREATE TABLE debt_reconciliation_lines (id BIGINT AUTO_INCREMENT PRIMARY KEY, reconciliation_id BIGINT NOT NULL REFERENCES debt_reconciliations(id) ON DELETE CASCADE, line_no INT NOT NULL, entry_date DATE NOT NULL, reference VARCHAR(50) NOT NULL, entry_type VARCHAR(80) NOT NULL, note VARCHAR(500), increase_amount DECIMAL(15,2) DEFAULT 0 NOT NULL, decrease_amount DECIMAL(15,2) DEFAULT 0 NOT NULL, UNIQUE(reconciliation_id,line_no))");
        sql("INSERT INTO document_sequences(name,last_number) VALUES ('RECONCILIATION',0)");

        accountant = user("recon-accountant", Role.ACCOUNTANT);
        agencyUser = user("recon-agency", Role.CUSTOMER);
        otherAgencyUser = user("recon-other-agency", Role.CUSTOMER);
        sql("INSERT INTO customers(id,code,name,group_id,status,credit_limit,credit_days) VALUES (900,'DL-900','Đại lý Chín Trăm',1,'ACTIVE',1000000,30)");
        sql("INSERT INTO customers(id,code,name,group_id,status,credit_limit,credit_days) VALUES (901,'DL-901','Đại lý Chín Trăm Một',1,'ACTIVE',1000000,30)");
        sql("INSERT INTO customer_accounts(customer_id,user_id) VALUES (900," + agencyUser.getId() + "),(901," + otherAgencyUser.getId() + ")");
    }

    @Test
    void generatesCorrectOpeningActivityAndClosingAndFreezesPeriodLines() throws Exception {
        insertInvoice(1, "HD-OLD", 900, "2025-12-15", "5000.00");
        insertInvoice(2, "HD-JAN", 900, "2026-01-05", "12000.00");
        insertPayment(1, "PT-JAN", 900, "2026-01-15", "3000.00", accountant.getId());

        long id = service.generate(900, LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-31"), accountant);
        var page = service.page(accountant, false);
        var doc = page.documents().stream().filter(x -> x.id() == id).findFirst().orElseThrow();

        assertEquals("DCN000001", doc.code());
        assertEquals(0, new BigDecimal("5000.00").compareTo(doc.opening()));
        assertEquals(0, new BigDecimal("12000.00").compareTo(doc.increase()));
        assertEquals(0, new BigDecimal("3000.00").compareTo(doc.decrease()));
        assertEquals(0, new BigDecimal("14000.00").compareTo(doc.closing()));
        assertEquals("PENDING", doc.status());
        assertTrue(doc.detailsSnapshotted());

        // Hóa đơn ghi lùi ngày sau khi chốt không được thay đổi các dòng của biên bản đã lập.
        insertInvoice(3, "HD-BACKDATED", 900, "2026-01-20", "2000.00");
        var afterChange = service.page(accountant, false);
        var periodLines = afterChange.ledger().entries().stream()
                .filter(x -> x.customerId() == 900 && !x.date().isBefore(LocalDate.parse("2026-01-01"))
                        && !x.date().isAfter(LocalDate.parse("2026-01-31"))).toList();
        assertEquals(2, periodLines.size());
        assertEquals(0, new BigDecimal("12000.00").compareTo(periodLines.stream()
                .map(FinanceOverviewDao.LedgerEntry::increase).reduce(BigDecimal.ZERO, BigDecimal::add)));
        assertThrows(ServiceException.class, () -> service.generate(900, LocalDate.parse("2026-01-01"),
                LocalDate.parse("2026-01-31"), accountant));
    }

    @Test
    void portalIsScopedToMappedAgencyAndCannotRespondToAnotherAgencysDocument() throws Exception {
        insertInvoice(1, "HD-900", 900, "2026-01-05", "12000.00");
        insertInvoice(2, "HD-901", 901, "2026-01-08", "7000.00");
        long agencyDoc = service.generate(900, LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-31"), accountant);
        long otherDoc = service.generate(901, LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-31"), accountant);

        var ownPage = service.page(agencyUser, true);
        assertEquals(900L, ownPage.portalCustomerId().longValue());
        assertTrue(ownPage.ledger().customers().stream().allMatch(x -> x.id() == 900));
        assertTrue(ownPage.documents().stream().allMatch(x -> x.customerId() == 900));
        assertThrows(ServiceException.class, () -> service.respond(otherDoc, "ACCEPTED", "", agencyUser));
        assertEquals(1, count("debt_reconciliations", "id=" + otherDoc + " AND status='PENDING'"));

        service.respond(agencyDoc, "ACCEPTED", "", agencyUser);
        assertEquals(1, count("debt_reconciliations", "id=" + agencyDoc + " AND status='ACCEPTED' AND responded_at IS NOT NULL"));
        assertThrows(ServiceException.class, () -> service.respond(agencyDoc, "ACCEPTED", "", agencyUser));
    }

    @Test
    void disputeRequiresReasonAndIsAudited() throws Exception {
        insertInvoice(1, "HD-DISPUTE", 900, "2026-02-05", "9000.00");
        long id = service.generate(900, LocalDate.parse("2026-02-01"), LocalDate.parse("2026-02-28"), accountant);
        assertThrows(ServiceException.class, () -> service.respond(id, "DISPUTED", "Sai số", agencyUser));
        service.respond(id, "DISPUTED", "Sai số tiền theo chứng từ số CT-01", agencyUser);
        assertEquals(1, count("debt_reconciliations", "id=" + id + " AND status='DISPUTED' AND responded_at IS NOT NULL"));
        assertEquals(1, count("audit_logs", "action='RECONCILIATION_DISPUTED' AND target_id=" + id));
    }

    @Test
    void onlyAccountantOrAdminCanGenerateAndViewStaffPage() {
        assertThrows(ForbiddenException.class, () -> service.generate(900, LocalDate.parse("2026-01-01"),
                LocalDate.parse("2026-01-31"), agencyUser));
        assertThrows(ForbiddenException.class, () -> service.page(agencyUser, false));
    }

    private void insertInvoice(long id, String code, long customerId, String issueDate, String amount) throws Exception {
        sql("INSERT INTO invoices(id,code,customer_id,total_amount,issued_at,due_date,status,paid_at,issued_by,paid_amount,credited_amount) VALUES ("
                + id + ", '" + code + "', " + customerId + ", " + amount + ", TIMESTAMP '" + issueDate
                + " 00:00:00', DATE '" + issueDate + "', 'ISSUED', NULL, " + accountant.getId() + ", 0, 0)");
    }

    private void insertPayment(long id, String code, long customerId, String paidOn, String amount, long userId) throws Exception {
        sql("INSERT INTO payments(id,code,customer_id,amount,method,paid_on,collected_by,note,created_by) VALUES ("
                + id + ", '" + code + "', " + customerId + ", " + amount + ", 'CASH', DATE '" + paidOn
                + "', " + userId + ", NULL, " + userId + ")");
    }
}
