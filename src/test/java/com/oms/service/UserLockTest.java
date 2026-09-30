package com.oms.service;

import com.oms.model.Role;
import com.oms.model.User;
import com.oms.support.DatabaseTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserLockTest extends DatabaseTest {
    private final UserService service = new UserService();

    @Test
    void lockRevokesSessionsAndReportsCustomersForHandover() throws Exception {
        User admin = user("admin", Role.ADMIN);
        User target = user("sales", Role.SALES_REP);
        sql("INSERT INTO customers (id, code, name, sales_rep_id) VALUES (1, 'DL001', 'Đại lý A', " + target.getId() + ")");
        service.lock(target.getId(), "  Nghỉ việc  ", admin);
        User locked = reload(target.getId());
        assertEquals(User.Status.LOCKED, locked.getStatus());
        assertEquals(1, locked.getSessionVersion());
        assertEquals("Nghỉ việc", locked.getLockReason());
        assertThrows(ServiceException.class, () -> new AuthService().login("sales", PASSWORD));
        assertEquals(1, service.customersNeedingHandover().size());
        assertEquals("Đại lý A", service.customerNamesOf(target.getId()).get(0));
        assertEquals(1, count("audit_logs", "action = 'USER_LOCK' AND detail = 'Nghỉ việc'"));
    }

    @Test
    void unlockRequiresNewLoginAndNeverRestoresOldVersion() throws Exception {
        User admin = user("admin", Role.ADMIN);
        User target = user("sales", Role.SALES_REP);
        service.lock(target.getId(), "Nghỉ tạm", admin);
        int lockedVersion = reload(target.getId()).getSessionVersion();
        service.unlock(target.getId(), admin);
        User unlocked = reload(target.getId());
        assertEquals(User.Status.ACTIVE, unlocked.getStatus());
        assertNull(unlocked.getLockReason());
        assertEquals(lockedVersion + 1, unlocked.getSessionVersion());
        assertEquals(unlocked.getSessionVersion(), new AuthService().login("sales", PASSWORD).getSessionVersion());
        assertTrue(service.customersNeedingHandover().isEmpty());
        assertEquals(1, count("audit_logs", "action = 'USER_UNLOCK'"));
    }

    @Test
    void unlockPendingUserStillRequiresTemporaryPasswordChange() throws Exception {
        User admin = user("admin", Role.ADMIN);
        User target = user("newuser", Role.SALES_REP);
        sql("UPDATE users SET status = 'PENDING', must_change_password = TRUE WHERE id = " + target.getId());
        service.lock(target.getId(), "Tạm ngưng", admin);
        service.unlock(target.getId(), admin);
        assertEquals(User.Status.PENDING, reload(target.getId()).getStatus());
        assertTrue(reload(target.getId()).isMustChangePassword());
    }

    @Test
    void reasonIsRequiredAndAdminCannotLockSelf() throws Exception {
        User admin = user("admin", Role.ADMIN);
        User target = user("sales", Role.SALES_REP);
        assertThrows(ServiceException.class, () -> service.lock(target.getId(), " ", admin));
        assertThrows(ServiceException.class, () -> service.lock(target.getId(), "x".repeat(501), admin));
        assertThrows(ServiceException.class, () -> service.lock(admin.getId(), "Tự khoá", admin));
        assertEquals(User.Status.ACTIVE, reload(target.getId()).getStatus());
        assertEquals(0, count("audit_logs", "action = 'USER_LOCK'"));
    }

    @Test
    void repeatedStateChangeDoesNotAddAuditOrVersion() throws Exception {
        User admin = user("admin", Role.ADMIN);
        User target = user("sales", Role.SALES_REP);
        assertThrows(ServiceException.class, () -> service.unlock(target.getId(), admin));
        service.lock(target.getId(), "Khoá lần đầu", admin);
        assertThrows(ServiceException.class, () -> service.lock(target.getId(), "Khoá lần nữa", admin));
        assertEquals("Khoá lần đầu", reload(target.getId()).getLockReason());
        assertEquals(1, reload(target.getId()).getSessionVersion());
        assertEquals(1, count("audit_logs", "action = 'USER_LOCK'"));
    }

    @Test
    void nonAdminAndRevokedAdminCannotLockOrUnlock() throws Exception {
        User admin = user("admin", Role.ADMIN);
        User sales = user("sales", Role.SALES_REP);
        User target = user("staff", Role.SALES_REP);
        assertThrows(ServiceException.class, () -> service.lock(target.getId(), "Không có quyền", sales));
        assertThrows(ServiceException.class, () -> service.unlock(target.getId(), sales));
        sql("UPDATE users SET session_version = 1 WHERE id = " + admin.getId());
        assertThrows(ServiceException.class, () -> service.lock(target.getId(), "Phiên cũ", admin));
        assertEquals(User.Status.ACTIVE, reload(target.getId()).getStatus());
    }

    @Test
    void missingTargetReturnsNotFoundWithoutAudit() throws Exception {
        User admin = user("admin", Role.ADMIN);
        assertThrows(UserService.NotFoundException.class, () -> service.lock(999, "Không tồn tại", admin));
        assertThrows(UserService.NotFoundException.class, () -> service.unlock(999, admin));
        assertEquals(0, count("audit_logs", "action IN ('USER_LOCK', 'USER_UNLOCK')"));
    }
}
