package com.oms.web.servlet;

import com.oms.model.Role;
import com.oms.model.User;
import com.oms.support.HttpFixture;
import com.oms.web.SessionKeys;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RolesServletTest {

    /** Form thiếu ô (tải trang dở, request bị cắt) phải bị từ chối, không được hiểu là "xoá hết quyền". */
    @Test
    void incompleteFormIsRejectedInsteadOfWipingMatrix() throws Exception {
        HttpFixture http = new HttpFixture();
        http.method = "POST";
        http.path = "/admin/roles";
        User admin = new User();
        admin.setId(1);
        admin.setRoles(EnumSet.of(Role.ADMIN));
        http.attributes.put(SessionKeys.CURRENT_USER, admin);

        new RolesServlet().doPost(http.request, http.response);

        assertEquals("/oms/admin/roles", http.redirect);
        assertTrue(((String) http.sessionAttributes.get(SessionKeys.FLASH)).contains("không đầy đủ"));
    }
}
