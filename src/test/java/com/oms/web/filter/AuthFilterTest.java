package com.oms.web.filter;

import com.oms.model.Module;
import com.oms.model.PermissionLevel;
import com.oms.model.Role;
import com.oms.model.User;
import com.oms.security.PermissionPolicy;
import com.oms.service.AuthService;
import com.oms.service.PermissionService;
import com.oms.support.HttpFixture;
import com.oms.web.SessionKeys;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.EnumSet;
import java.util.Map;
import java.util.stream.Stream;

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
        return filter(user, new PermissionPolicy(Map.of()));
    }

    private AuthFilter filter(User user, PermissionPolicy policy) {
        return new AuthFilter(new AuthService() {
            @Override public User findActiveSessionUser(long id) { return user; }
        }, new PermissionService() {
            @Override public PermissionPolicy policy() { return policy; }
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

    private static Stream<Arguments> sprint4Endpoints() {
        return Stream.of(
                Arguments.of("/api/orders/credit", "GET"),
                Arguments.of("/api/orders/stock", "GET"),
                Arguments.of("/api/orders/purchase-history", "GET"),
                Arguments.of("/api/orders/search", "GET"),
                Arguments.of("/api/orders/pdf", "GET"),
                Arguments.of("/api/orders/pending", "GET"),
                Arguments.of("/api/orders/history", "GET"),
                Arguments.of("/api/orders/submit", "POST"),
                Arguments.of("/api/orders/decide", "POST"),
                Arguments.of("/api/orders/cancel", "POST"),
                Arguments.of("/api/orders/copy", "POST"),
                Arguments.of("/api/orders/add-last-purchase", "POST"),
                Arguments.of("/api/portal/catalog", "GET"),
                Arguments.of("/api/portal/cart", "GET"),
                Arguments.of("/api/portal/create", "POST"),
                Arguments.of("/api/portal/add", "POST"),
                Arguments.of("/api/portal/update", "POST"),
                Arguments.of("/api/portal/remove", "POST"),
                Arguments.of("/api/portal/header", "POST"),
                Arguments.of("/api/portal/submit", "POST"));
    }

    @ParameterizedTest(name = "{1} {0} thiếu quyền ORDER phải bị 403")
    @MethodSource("sprint4Endpoints")
    void missingOrderPermissionIsRejectedByFilter(String path, String method) throws Exception {
        HttpFixture http = fixture(3);
        http.path = path;
        http.method = method;
        // Có quyền ở module khác vẫn không được đi qua filter của API đơn hàng.
        var policy = new PermissionPolicy(Map.of(Role.SALES_REP, Map.of(Module.PRODUCT, PermissionLevel.F)));
        filter(user(), policy).doFilter(http.request, http.response,
                (req, resp) -> fail("Thiếu quyền ORDER nhưng đã tới servlet"));
        assertEquals(403, http.status);
        assertNull(http.redirect);
        assertEquals("no-store", http.headers.get("Cache-Control"));
    }

    @ParameterizedTest(name = "{1} {0} tuân thủ quyền ORDER chỉ đọc")
    @MethodSource("sprint4Endpoints")
    void orderReadPermissionAllowsReadsButRejectsMutations(String path, String method) throws Exception {
        HttpFixture http = fixture(3);
        http.path = path;
        http.method = method;
        boolean[] passed = {false};
        var policy = new PermissionPolicy(Map.of(Role.SALES_REP, Map.of(Module.ORDER, PermissionLevel.R)));
        filter(user(), policy).doFilter(http.request, http.response, (req, resp) -> passed[0] = true);
        assertEquals(method.equals("GET"), passed[0]);
        assertEquals(method.equals("GET") ? 200 : 403, http.status);
    }

    @Test
    void customerWithOwnOrderWritePermissionCanReachPortalSubmission() throws Exception {
        HttpFixture http = fixture(3);
        http.path = "/api/portal/submit";
        http.method = "POST";
        User customer = user();
        customer.setRoles(EnumSet.of(Role.CUSTOMER));
        var policy = new PermissionPolicy(Map.of(Role.CUSTOMER, Map.of(Module.ORDER, PermissionLevel.W_OWN)));
        boolean[] passed = {false};
        filter(customer, policy).doFilter(http.request, http.response, (req, resp) -> passed[0] = true);
        assertTrue(passed[0]);
        assertEquals(200, http.status);
    }

    @Test
    void warehouseWithReadOnlyOrderPermissionCanReachLifecycleService() throws Exception {
        HttpFixture http = fixture(3);
        http.path = "/api/orders/advance";
        http.method = "POST";
        User warehouse = user();
        warehouse.setRoles(EnumSet.of(Role.WAREHOUSE));
        var policy = new PermissionPolicy(Map.of(Role.WAREHOUSE, Map.of(Module.ORDER, PermissionLevel.R)));
        boolean[] passed = {false};
        filter(warehouse, policy).doFilter(http.request, http.response, (req, resp) -> passed[0] = true);
        assertTrue(passed[0]);
        assertEquals(200, http.status);
    }
}
