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

class WarehousePageAccessTest {
    private static final String PATH = "/inventory/warehouses";

    @Test
    void warehousePageAndMenuRequireInventoryPermission() {
        User user = new User();
        user.setRoles(EnumSet.of(Role.SALES_REP));
        var denied = new PermissionPolicy(Map.of());
        var allowed = new PermissionPolicy(Map.of(
                Role.SALES_REP, Map.of(Module.INVENTORY, PermissionLevel.R)));

        assertFalse(AccessRules.isPublic(PATH));
        assertFalse(AccessRules.isAllowed(AccessRules.find(PATH), user, denied));
        assertTrue(Menu.build(user, denied, PATH).stream().flatMap(g -> g.items().stream())
                .noneMatch(i -> PATH.equals(i.url())));

        assertTrue(AccessRules.isAllowed(AccessRules.find(PATH), user, allowed));
        assertFalse(allowed.canWrite(user.getRoles(), Module.INVENTORY));
        var items = Menu.build(user, allowed, PATH).stream().flatMap(g -> g.items().stream()).toList();
        assertTrue(items.stream().anyMatch(i -> PATH.equals(i.url()) && i.active()));
        assertTrue(items.stream().anyMatch(i -> "/suppliers".equals(i.url()) && !i.active()));
    }

    @Test
    void warehouseManagerWriteAccessDoesNotOpenUnregisteredMutationUrls() {
        User manager = new User();
        manager.setRoles(EnumSet.of(Role.WH_MANAGER));
        var policy = new PermissionPolicy(Map.of(
                Role.WH_MANAGER, Map.of(Module.INVENTORY, PermissionLevel.F)));

        assertTrue(AccessRules.isAllowed(AccessRules.find(PATH), manager, policy));
        assertTrue(policy.canWrite(manager.getRoles(), Module.INVENTORY));
        assertNull(AccessRules.find(PATH + "/save"));
        assertNull(AccessRules.find(PATH + "/delete"));
    }
}
