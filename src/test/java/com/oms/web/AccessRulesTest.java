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

class AccessRulesTest {
    @Test
    void accountRoleMutationRemainsAdminOnlyEvenWithCustomMatrix() {
        var policy = new PermissionPolicy(Map.of(Role.SALES_MANAGER, Map.of(Module.USER_AUDIT, PermissionLevel.F)));
        User manager = new User();
        manager.setRoles(EnumSet.of(Role.SALES_MANAGER));
        assertFalse(AccessRules.isAllowed(AccessRules.find("/admin/users/new"), manager, policy));
        assertFalse(AccessRules.isAllowed(AccessRules.find("/admin/users/edit"), manager, policy));
        assertFalse(AccessRules.isAllowed(AccessRules.find("/admin/users/lock"), manager, policy));
        assertFalse(AccessRules.isAllowed(AccessRules.find("/admin/users/unlock"), manager, policy));
        manager.setRoles(EnumSet.of(Role.ADMIN));
        assertTrue(AccessRules.isAllowed(AccessRules.find("/admin/users/edit"), manager, policy));
    }
}
