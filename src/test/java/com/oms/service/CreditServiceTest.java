package com.oms.service;

import com.oms.model.CreditChange;
import com.oms.model.Role;
import com.oms.model.User;
import com.oms.support.DatabaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** S3-05: hạn mức công nợ và số ngày nợ — bắt buộc lý do, ghi lịch sử + nhật ký, chỉ Kế toán / Quản lý kinh doanh sửa. */
class CreditServiceTest extends DatabaseTest {

    private final CreditService service = new CreditService();
    private User accountant;
    private User manager;
    private User rep;

    @BeforeEach
    void setUp() throws Exception {
        accountant = user("ketoan", Role.ACCOUNTANT);
        manager = user("qlkd", Role.SALES_MANAGER);
        rep = user("nvkd", Role.SALES_REP);
        sql("INSERT INTO customers (id, code, name, group_id, sales_rep_id) VALUES (900, 'DL900', 'Đại lý thử', 1, " + rep.getId() + ")");
    }

    @Test
    void newCustomersStartWithNoCredit() throws Exception {
        assertEquals(1, count("customers", "id = 900 AND credit_limit = 0 AND credit_days = 0"));
    }

    @Test
    void accountantSetsLimitAndDaysWithReasonAndHistory() throws Exception {
        service.update(900, "500.000.000", "30", "Đại lý lâu năm, thanh toán đúng hạn", accountant);

        assertEquals(1, count("customers", "id = 900 AND credit_limit = 500000000 AND credit_days = 30"));
        List<CreditChange> history = service.history(900);
        assertEquals(1, history.size());
        CreditChange c = history.get(0);
        assertEquals(0, BigDecimal.ZERO.compareTo(c.getOldLimit()));
        assertEquals(0, new BigDecimal("500000000").compareTo(c.getNewLimit()));
        assertEquals(30, c.getNewDays());
        assertEquals("Đại lý lâu năm, thanh toán đúng hạn", c.getReason());
        assertEquals("ketoan", c.getChangedByName());
        assertEquals(1, count("audit_logs", "action = 'CUSTOMER_CREDIT_UPDATE' AND detail LIKE '%thanh toán đúng hạn%' "
                + "AND old_value LIKE '%Số ngày nợ: 0%' AND new_value LIKE '%Số ngày nợ: 30%'"));
    }

    @Test
    void historyKeepsEveryChangeNewestFirst() throws Exception {
        service.update(900, "100000000", "15", "Lần đầu", manager);
        service.update(900, "200000000", "15", "Tăng hạn mức", manager);
        service.update(900, "200000000", "30", "Cho nợ lâu hơn", accountant);

        List<CreditChange> history = service.history(900);
        assertEquals(3, history.size());
        assertEquals("Cho nợ lâu hơn", history.get(0).getReason());
        assertEquals(15, history.get(0).getOldDays());
        assertEquals("Lần đầu", history.get(2).getReason());
        assertEquals(3, count("audit_logs", "action = 'CUSTOMER_CREDIT_UPDATE'"));
    }

    @Test
    void reasonIsRequired() throws Exception {
        assertThrows(ServiceException.class, () -> service.update(900, "100000000", "15", "   ", accountant));
        assertThrows(ServiceException.class, () -> service.update(900, "100000000", "15", null, accountant));
        assertThrows(ServiceException.class, () -> service.update(900, "100000000", "15", "x".repeat(501), accountant));
        assertEquals(0, count("customer_credit_history", "1 = 1"));
        assertEquals(1, count("customers", "id = 900 AND credit_limit = 0"));
    }

    @Test
    void validatesLimitAndDays() throws Exception {
        assertThrows(ServiceException.class, () -> service.update(900, "", "15", "Lý do", accountant));
        assertThrows(ServiceException.class, () -> service.update(900, "abc", "15", "Lý do", accountant));
        assertThrows(ServiceException.class, () -> service.update(900, "-1", "15", "Lý do", accountant));
        assertThrows(ServiceException.class, () -> service.update(900, "100000", "", "Lý do", accountant));
        assertThrows(ServiceException.class, () -> service.update(900, "100000", "-5", "Lý do", accountant));
        assertThrows(ServiceException.class, () -> service.update(900, "100000", "366", "Lý do", accountant));
        assertThrows(ServiceException.class, () -> service.update(900, "100000", "1,5", "Lý do", accountant));
        assertEquals(0, count("customer_credit_history", "1 = 1"));
    }

    @Test
    void unchangedValuesAreRejected() throws Exception {
        service.update(900, "100000000", "15", "Lần đầu", accountant);
        ServiceException e = assertThrows(ServiceException.class, () -> service.update(900, "100.000.000", "15", "Không đổi", accountant));
        assertTrue(e.getMessage().contains("không thay đổi"));
        assertEquals(1, service.history(900).size());
    }

    @Test
    void zeroMeansNoCreditAndIsAllowed() throws Exception {
        service.update(900, "100000000", "15", "Cho nợ", accountant);
        service.update(900, "0", "0", "Ngừng cho nợ do trễ hạn", accountant);
        assertEquals(1, count("customers", "id = 900 AND credit_limit = 0 AND credit_days = 0"));
    }

    @Test
    void onlyAccountantAndManagersMayEdit() throws Exception {
        assertThrows(ForbiddenException.class, () -> service.update(900, "100000000", "15", "Lý do", rep));
        User warehouse = user("kho", Role.WAREHOUSE);
        assertThrows(ForbiddenException.class, () -> service.update(900, "100000000", "15", "Lý do", warehouse));
        assertEquals(0, count("customer_credit_history", "1 = 1"));
        service.update(900, "100000000", "15", "Quản lý kinh doanh được sửa", manager);
        assertEquals(1, count("customer_credit_history", "1 = 1"));
    }

    @Test
    void unknownCustomerIsNotFound() throws Exception {
        assertThrows(CustomerService.NotFoundException.class, () -> service.update(12345, "100000000", "15", "Lý do", accountant));
    }
}
