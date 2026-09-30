package com.oms.web.filter;

import com.oms.model.Role;
import com.oms.model.User;
import com.oms.security.PermissionPolicy;
import com.oms.service.AuthService;
import com.oms.service.PermissionService;
import com.oms.support.HttpFixture;
import com.oms.web.SessionKeys;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AuthFilterTest {
    private User user() {
        User user = new User();
        user.setId(1);
        user.setStatus(User.Status.ACTIVE);
        user.setRoles(EnumSet.of(Role.SALES_REP));
        user.setSessionVersion(3);
        return user;
    }

    private AuthFilter filter(User user) {
        return new AuthFilter(new AuthService() {
            @Override public User findActiveSessionUser(long id) { return user; }
        }, new PermissionService() {
            @Override public PermissionPolicy policy() { return new PermissionPolicy(Map.of()); }
        });
    }

    private HttpFixture fixture(int version) {
        HttpFixture http = new HttpFixture();
        http.sessionAttributes.put(SessionKeys.USER_ID, 1L);
        http.sessionAttributes.put(SessionKeys.SESSION_VERSION, version);
        return http;
    }

    @Test
    void validSessionPassesAndSensitiveResponseIsNotCached() throws Exception {
        HttpFixture http = fixture(3);
        boolean[] passed = {false};
        filter(user()).doFilter(http.request, http.response, (req, resp) -> passed[0] = true);
        assertTrue(passed[0]);
        assertEquals("no-store", http.headers.get("Cache-Control"));
        assertNotNull(http.attributes.get(SessionKeys.CURRENT_USER));
    }

    @Test
    void revokedSessionCannotAccessProtectedPage() throws Exception {
        HttpFixture http = fixture(2);
        filter(user()).doFilter(http.request, http.response, (req, resp) -> fail("Phiên đã thu hồi"));
        assertTrue(http.invalidated);
        assertEquals("/oms/login?expired=1", http.redirect);
    }

    @Test
    void deletedUserAlsoInvalidatesServerSession() throws Exception {
        HttpFixture http = fixture(3);
        filter(null).doFilter(http.request, http.response, (req, resp) -> fail("Tài khoản không tồn tại"));
        assertTrue(http.invalidated);
        assertEquals("/oms/login?expired=1", http.redirect);
    }

    @Test
    void lockedUserPingReturns401WithoutRedirect() throws Exception {
        HttpFixture http = fixture(3);
        http.path = "/session/ping";
        User user = user();
        user.setStatus(User.Status.LOCKED);
        filter(user).doFilter(http.request, http.response, (req, resp) -> fail("Tài khoản bị khoá"));
        assertTrue(http.invalidated);
        assertEquals(401, http.status);
        assertNull(http.redirect);
        assertEquals("no-store", http.headers.get("Cache-Control"));
    }

    @Test
    void pendingUserMustChangePasswordBeforeOtherWork() throws Exception {
        HttpFixture http = fixture(3);
        User user = user();
        user.setStatus(User.Status.PENDING);
        user.setMustChangePassword(true);
        filter(user).doFilter(http.request, http.response, (req, resp) -> fail("Chưa đổi mật khẩu tạm"));
        assertEquals("/oms/account/change-password", http.redirect);
    }

    @Test
    void expiredPingDoesNotCreateNewSession() throws Exception {
        HttpFixture http = new HttpFixture();
        http.sessionPresent = false;
        http.path = "/session/ping";
        filter(null).doFilter(http.request, http.response, (req, resp) -> fail("Phiên hết hạn"));
        assertEquals(401, http.status);
        assertFalse(http.sessionPresent);
    }
}
