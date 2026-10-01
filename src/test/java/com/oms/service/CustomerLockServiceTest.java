package com.oms.service;

import com.oms.model.Customer;
import com.oms.model.LockChange;
import com.oms.model.Role;
import com.oms.model.User;
import com.oms.support.DatabaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** S3-07: khoá / mở giao dịch với đại lý — bắt buộc lý do khoá, ghi lịch sử + nhật ký, chặn tạo đơn mới. */
class CustomerLockServiceTest extends DatabaseTest {

    private final CustomerLockService service = new CustomerLockService();
    private final CustomerService customers = new CustomerService();
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
    void lockRequiresReasonAndMarksCustomerLocked() throws Exception {
        assertThrows(ServiceException.class, () -> service.lock(900, "  ", accountant));
        assertThrows(ServiceException.class, () -> service.lock(900, null, accountant));
        assertThrows(ServiceException.class, () -> service.lock(900, "x".repeat(501), accountant));
        assertEquals(1, count("customers", "id = 900 AND status = 'ACTIVE'"));

        service.lock(900, "Nợ quá hạn 90 ngày, nghi mất khả năng thanh toán", accountant);

        Customer c = customers.get(900, manager);
        assertTrue(c.isLocked());
        assertEquals("Nợ quá hạn 90 ngày, nghi mất khả năng thanh toán", c.getLockReason());
        assertEquals("ketoan", c.getLockedByName());
        assertFalse(c.getLockedAtText().isEmpty());
        assertEquals(1, count("audit_logs", "action = 'CUSTOMER_LOCK' AND detail LIKE '%mất khả năng%'"));
    }

    @Test
    void lockedCustomerCannotStartNewOrders() throws Exception {
        Customer active = customers.get(900, manager);
        CustomerLockService.requireTradable(active);   // đang giao dịch: cho phép

        service.lock(900, "Nợ xấu", accountant);
        Customer locked = customers.get(900, manager);
        ServiceException e = assertThrows(ServiceException.class, () -> CustomerLockService.requireTradable(locked));
        assertTrue(e.getMessage().contains("DL900") && e.getMessage().contains("Nợ xấu"));
    }

    @Test
    void unlockClearsLockAndKeepsHistory() throws Exception {
        service.lock(900, "Nợ xấu", manager);
        service.unlock(900, "Đã thanh toán đủ công nợ", accountant);

        Customer c = customers.get(900, manager);
        assertFalse(c.isLocked());
        assertEquals(null, c.getLockReason());
        List<LockChange> history = service.history(900);
        assertEquals(2, history.size());
        assertFalse(history.get(0).isLock());
        assertEquals("Đã thanh toán đủ công nợ", history.get(0).getReason());
        assertTrue(history.get(1).isLock());
        assertEquals("Nợ xấu", history.get(1).getReason());
        assertEquals(1, count("audit_logs", "action = 'CUSTOMER_UNLOCK' AND old_value LIKE '%Nợ xấu%'"));
    }

    @Test
    void unlockReasonIsOptional() throws Exception {
        service.lock(900, "Nợ xấu", manager);
        service.unlock(900, null, manager);
        assertFalse(customers.get(900, manager).isLocked());
        assertEquals(null, service.history(900).get(0).getReason());
    }

    @Test
    void rejectsRepeatedOrInvalidTransitions() throws Exception {
        assertThrows(ServiceException.class, () -> service.unlock(900, "Mở", accountant));   // đang giao dịch bình thường
        service.lock(900, "Nợ xấu", accountant);
        assertThrows(ServiceException.class, () -> service.lock(900, "Khoá lại", accountant));
        assertEquals(1, service.history(900).size());
        assertThrows(CustomerService.NotFoundException.class, () -> service.lock(12345, "Lý do", accountant));
    }

    @Test
    void onlyAccountantAndManagersMayLockOrUnlock() throws Exception {
        assertThrows(ForbiddenException.class, () -> service.lock(900, "Lý do", rep));
        User warehouse = user("kho", Role.WAREHOUSE);
        assertThrows(ForbiddenException.class, () -> service.lock(900, "Lý do", warehouse));
        assertEquals(1, count("customers", "id = 900 AND status = 'ACTIVE'"));
        service.lock(900, "Lý do", manager);
        assertThrows(ForbiddenException.class, () -> service.unlock(900, "Mở", rep));
        assertTrue(customers.get(900, manager).isLocked());
    }

    @Test
    void lockedCustomerStillAppearsInListAndCanBeEdited() throws Exception {
        service.lock(900, "Nợ xấu", accountant);
        assertEquals(1, customers.search(null, null, null, null, Customer.Status.LOCKED, 1, manager).getTotal());
        assertEquals(0, customers.search(null, null, null, null, Customer.Status.ACTIVE, 1, manager).getTotal());
        Customer input = new Customer();
        input.setName("Đổi tên khi đang khoá");
        input.setGroupId(1L);
        input.setSalesRepId(rep.getId());
        customers.update(900, input, manager);
        assertTrue(customers.get(900, manager).isLocked());   // sửa hồ sơ không làm mở khoá
    }
}
