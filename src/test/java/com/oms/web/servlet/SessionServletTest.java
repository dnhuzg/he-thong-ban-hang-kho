package com.oms.web.servlet;

import com.oms.support.HttpFixture;
import com.oms.web.SessionKeys;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SessionServletTest {
    @Test
    void logoutDestroysCurrentServerSession() throws Exception {
        HttpFixture http = new HttpFixture();
        http.sessionAttributes.put(SessionKeys.USER_ID, 1L);
        http.sessionAttributes.put(SessionKeys.CSRF_TOKEN, "old-token");
        new LogoutServlet().doPost(http.request, http.response);
        assertTrue(http.invalidated);
        assertTrue(http.sessionAttributes.isEmpty());
        assertEquals("/oms/login?logout=1", http.redirect);
    }

    @Test
    void activePingReturnsCsrfTokenAndNoContent() {
        HttpFixture http = new HttpFixture();
        http.attributes.put(SessionKeys.CSRF_TOKEN, "current-token");
        new SessionPingServlet().doGet(http.request, http.response);
        assertEquals(204, http.status);
        assertEquals("current-token", http.headers.get("X-CSRF-Token"));
        assertEquals("no-store", http.headers.get("Cache-Control"));
    }
}
