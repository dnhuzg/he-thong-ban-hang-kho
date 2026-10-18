package com.oms.web;

import com.oms.model.Module;
import com.oms.model.PermissionLevel;
import com.oms.model.Role;
import com.oms.model.User;
import com.oms.security.PermissionPolicy;
import com.oms.support.HttpFixture;
import com.oms.web.filter.CsrfFilter;
import org.junit.jupiter.api.Test;
import java.util.EnumSet;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CompletedStoryAccessTest {
    @Test void copyAndLotMutationsRequireWriteAndCsrf() throws Exception {
        User user=new User(); user.setRoles(EnumSet.of(Role.WH_MANAGER,Role.SALES_REP));
        var policy=new PermissionPolicy(Map.of(Role.WH_MANAGER,Map.of(Module.INVENTORY,PermissionLevel.R),Role.SALES_REP,Map.of(Module.ORDER,PermissionLevel.R)));
        assertTrue(AccessRules.isAllowed(AccessRules.find("/orders/pdf"),user,policy));
        assertTrue(AccessRules.isAllowed(AccessRules.find("/inventory/lots"),user,policy));
        for (String path:new String[]{"/orders/copy","/inventory/lots/configure","/inventory/lots/metadata"}) {
            assertFalse(AccessRules.isPublic(path));
            assertFalse(AccessRules.isAllowed(AccessRules.find(path),user,policy));
            var http=new HttpFixture(); http.path=path; http.method="POST";
            http.sessionAttributes.put(SessionKeys.USER_ID,1L); http.sessionAttributes.put(SessionKeys.CSRF_TOKEN,"valid");
            http.parameters.put("_csrf","invalid"); boolean[] reached={false};
            new CsrfFilter().doFilter(http.request,http.response,(req,resp)->reached[0]=true);
            assertEquals(403,http.status); assertFalse(reached[0]);
        }
    }
}
