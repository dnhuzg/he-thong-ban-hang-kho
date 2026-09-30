package com.oms.service;

import com.oms.model.Role;
import com.oms.model.User;
import com.oms.support.DatabaseTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class AuthLoginTest extends DatabaseTest {
    private final AuthService service = new AuthService();

    @Test
    void successfulLoginClearsFailuresAndWritesAudit() throws Exception {
        User user = user("sales", Role.SALES_REP);
        sql("UPDATE users SET failed_login_count = 2 WHERE id = " + user.getId());
        assertEquals(user.getId(), service.login(" sales ", PASSWORD).getId());
        assertEquals(0, reload(user.getId()).getFailedLoginCount());
        assertEquals(1, count("audit_logs", "action = 'LOGIN'"));
    }

    @Test
    void unknownUserAndWrongPasswordShareMessage() throws Exception {
        User user = user("sales", Role.SALES_REP);
        String wrong = assertThrows(ServiceException.class, () -> service.login("sales", "wrong")).getMessage();
        String unknown = assertThrows(ServiceException.class, () -> service.login("missing", "wrong")).getMessage();
        assertEquals(wrong, unknown);
        assertEquals(1, reload(user.getId()).getFailedLoginCount());
        assertEquals(0, count("audit_logs", "action = 'LOGIN'"));
    }

    @Test
    void concurrentFailuresCannotBypassFiveAttemptLock() throws Exception {
        User user = user("sales", Role.SALES_REP);
        var executor = Executors.newFixedThreadPool(5);
        try {
            var tasks = new ArrayList<Callable<String>>();
            for (int i = 0; i < 5; i++) {
                tasks.add(() -> assertThrows(ServiceException.class, () -> service.login("sales", "wrong")).getMessage());
            }
            for (var result : executor.invokeAll(tasks)) {
                assertEquals(AuthService.GENERIC_LOGIN_ERROR, result.get());
            }
            User locked = reload(user.getId());
            assertNotNull(locked.getLoginLockedUntil());
            assertEquals(0, locked.getFailedLoginCount());
            assertThrows(ServiceException.class, () -> service.login("sales", PASSWORD));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void lockedAccountCannotLoginEvenWithCorrectPassword() throws Exception {
        User user = user("sales", Role.SALES_REP);
        sql("UPDATE users SET status = 'LOCKED' WHERE id = " + user.getId());
        assertThrows(ServiceException.class, () -> service.login("sales", PASSWORD));
        assertEquals(0, count("audit_logs", "action = 'LOGIN'"));
    }
}
