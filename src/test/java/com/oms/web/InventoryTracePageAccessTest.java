package com.oms.web;

import com.oms.model.Module;
import com.oms.model.PermissionLevel;
import com.oms.model.Role;
import com.oms.model.User;
import com.oms.security.PermissionPolicy;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InventoryTracePageAccessTest {
    @Test
    void pagesAndMenuRequireInventoryReadPermission() {
        User user = new User();
        user.setRoles(EnumSet.of(Role.WAREHOUSE));
        var denied = new PermissionPolicy(Map.of());
        var allowed = new PermissionPolicy(Map.of(Role.WAREHOUSE, Map.of(Module.INVENTORY, PermissionLevel.R)));
        for (String path : new String[] {"/inventory/lots", "/inventory/stock-card"}) {
            var rule = AccessRules.find(path);
            assertNotNull(rule);
            assertFalse(AccessRules.isPublic(path));
            assertFalse(AccessRules.isAllowed(rule, user, denied));
            assertTrue(AccessRules.isAllowed(rule, user, allowed));
            assertTrue(Menu.build(user, denied, path).stream().flatMap(group -> group.items().stream())
                    .noneMatch(item -> path.equals(item.url())));
            assertTrue(Menu.build(user, allowed, path).stream().flatMap(group -> group.items().stream())
                    .anyMatch(item -> path.equals(item.url()) && item.active() && !item.comingSoon()));
            assertNull(AccessRules.find(path + "/save"));
            assertNull(AccessRules.find(path + "/export"));
        }
        assertFalse(allowed.canWrite(user.getRoles(), Module.INVENTORY));
    }
}
