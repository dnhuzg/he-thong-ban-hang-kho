package com.oms.service;

import com.oms.model.Role;
import com.oms.model.User;
import com.oms.security.Passwords;
import com.oms.support.DatabaseTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class ChangePasswordTest extends DatabaseTest {
    private static final String NEW_PASSWORD = "MoiMatkhau2026";
    private final AuthService service = new AuthService();

    @Test
    void changePersistsHashAndIncrementsVersionForOtherSessions() throws Exception {
        User user = user("sales", Role.SALES_REP);
        int oldVersion = user.getSessionVersion();
        int version = service.changePassword(user.getId(), PASSWORD, NEW_PASSWORD, NEW_PASSWORD);
        User changed = reload(user.getId());
        assertTrue(Passwords.matches(NEW_PASSWORD, changed.getPasswordHash()));
        assertFalse(Passwords.matches(PASSWORD, changed.getPasswordHash()));
        assertEquals(oldVersion + 1, version);
        assertEquals(version, changed.getSessionVersion());
        assertEquals(1, count("audit_logs", "action = 'PASSWORD_CHANGE'"));
    }

    @Test
    void wrongCurrentPasswordMakesNoChange() throws Exception {
        User user = user("sales", Role.SALES_REP);
        assertThrows(ServiceException.class,
                () -> service.changePassword(user.getId(), "wrong", NEW_PASSWORD, NEW_PASSWORD));
        assertTrue(Passwords.matches(PASSWORD, reload(user.getId()).getPasswordHash()));
        assertEquals(0, reload(user.getId()).getSessionVersion());
        assertEquals(0, count("audit_logs", "action = 'PASSWORD_CHANGE'"));
    }

    @Test
    void weakMismatchAndSamePasswordAreRejected() throws Exception {
        User user = user("sales", Role.SALES_REP);
        assertThrows(ServiceException.class, () -> service.changePassword(user.getId(), PASSWORD, "abc123", "abc123"));
        assertThrows(ServiceException.class, () -> service.changePassword(user.getId(), PASSWORD, NEW_PASSWORD, "different"));
        assertThrows(ServiceException.class, () -> service.changePassword(user.getId(), PASSWORD, PASSWORD, PASSWORD));
        assertEquals(0, reload(user.getId()).getSessionVersion());
    }

    @Test
    void lockedUserCannotChangePasswordAfterAuthenticationRace() throws Exception {
        User user = user("sales", Role.SALES_REP);
        sql("UPDATE users SET status = 'LOCKED' WHERE id = " + user.getId());
        assertThrows(ServiceException.class,
                () -> service.changePassword(user.getId(), PASSWORD, NEW_PASSWORD, NEW_PASSWORD));
        assertEquals(0, reload(user.getId()).getSessionVersion());
    }

    @Test
    void pendingUserBecomesActiveAfterChangingTemporaryPassword() throws Exception {
        User user = user("newuser", Role.SALES_REP);
        sql("UPDATE users SET status = 'PENDING', must_change_password = TRUE WHERE id = " + user.getId());
        service.changePassword(user.getId(), PASSWORD, NEW_PASSWORD, NEW_PASSWORD);
        assertEquals(User.Status.ACTIVE, reload(user.getId()).getStatus());
        assertFalse(reload(user.getId()).isMustChangePassword());
    }

    @Test
    void onlyOneConcurrentChangeCanUseOldPassword() throws Exception {
        User user = user("sales", Role.SALES_REP);
        var executor = Executors.newFixedThreadPool(2);
        Callable<Boolean> change = () -> {
            try {
                service.changePassword(user.getId(), PASSWORD, NEW_PASSWORD, NEW_PASSWORD);
                return true;
            } catch (ServiceException e) {
                return false;
            }
        };
        try {
            var results = executor.invokeAll(List.of(change, change));
            long successes = 0;
            for (var result : results) {
                if (result.get()) successes++;
            }
            assertEquals(1, successes);
            assertEquals(1, reload(user.getId()).getSessionVersion());
            assertEquals(1, count("audit_logs", "action = 'PASSWORD_CHANGE'"));
        } finally {
            executor.shutdownNow();
        }
    }
}
