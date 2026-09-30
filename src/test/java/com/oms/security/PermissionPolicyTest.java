package com.oms.security;

import com.oms.model.Module;
import com.oms.model.PermissionLevel;
import com.oms.model.Role;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** S1-05: kiểm thử tự động cho ít nhất ba vai trò. Ma trận lấy theo seed.sql. */
class PermissionPolicyTest {

    private final PermissionPolicy policy = new PermissionPolicy(seedMatrix());

    @Test
    void salesRepCannotWriteInventory() {
        Set<Role> roles = EnumSet.of(Role.SALES_REP);
        assertTrue(policy.canRead(roles, Module.INVENTORY));
        assertFalse(policy.canWrite(roles, Module.INVENTORY));
        assertTrue(policy.isOwnScopeOnly(roles, Module.ORDER));
        assertFalse(policy.canRead(roles, Module.USER_AUDIT));
    }

    @Test
    void warehouseCannotSeeCostPriceNorUsers() {
        Set<Role> roles = EnumSet.of(Role.WAREHOUSE);
        assertTrue(policy.canWrite(roles, Module.INVENTORY));
        assertFalse(PermissionPolicy.canViewCostPrice(roles));
        assertFalse(policy.canRead(roles, Module.INVOICE));
        assertFalse(policy.canRead(roles, Module.USER_AUDIT));
    }

    @Test
    void onlySalesManagerAndAdminSeeCostPrice() {
        for (Role r : Role.values()) {
            boolean expected = r == Role.SALES_MANAGER || r == Role.ADMIN;
            assertTrue(PermissionPolicy.canViewCostPrice(EnumSet.of(r)) == expected, r.name());
        }
    }

    @Test
    void salesManagerReadsUsersButCannotManageThem() {
        Set<Role> roles = EnumSet.of(Role.SALES_MANAGER);
        assertTrue(policy.canRead(roles, Module.USER_AUDIT));
        assertFalse(policy.canWrite(roles, Module.USER_AUDIT));
        assertFalse(PermissionPolicy.canManagePermissions(roles));
    }

    @Test
    void adminHasFullAccessEvenWithoutMatrixRows() {
        Set<Role> roles = EnumSet.of(Role.ADMIN);
        for (Module m : Module.values()) {
            assertTrue(policy.canWrite(roles, m), m.name());
        }
        assertTrue(PermissionPolicy.canManagePermissions(roles));
    }

    @Test
    void defaultDenyWhenMatrixIsEmpty() {
        PermissionPolicy empty = new PermissionPolicy(new EnumMap<>(Role.class));
        for (Role r : Role.configurable()) {
            for (Module m : Module.values()) {
                assertFalse(empty.canRead(EnumSet.of(r), m), r + "/" + m);
            }
        }
    }

    @Test
    void multipleRolesCombinePermissions() {
        Set<Role> roles = EnumSet.of(Role.WAREHOUSE, Role.ACCOUNTANT);
        assertTrue(policy.canWrite(roles, Module.INVENTORY));
        assertTrue(policy.canWrite(roles, Module.INVOICE));
        assertFalse(policy.isOwnScopeOnly(roles, Module.SHIPMENT));
    }

    private static Map<Role, Map<Module, PermissionLevel>> seedMatrix() {
        Map<Role, Map<Module, PermissionLevel>> m = new EnumMap<>(Role.class);
        put(m, Role.SALES_REP, Module.PRODUCT, PermissionLevel.R);
        put(m, Role.SALES_REP, Module.ORDER, PermissionLevel.W_OWN);
        put(m, Role.SALES_REP, Module.INVENTORY, PermissionLevel.R);
        put(m, Role.WAREHOUSE, Module.INVENTORY, PermissionLevel.W);
        put(m, Role.WAREHOUSE, Module.SHIPMENT, PermissionLevel.W);
        put(m, Role.ACCOUNTANT, Module.INVOICE, PermissionLevel.F);
        put(m, Role.ACCOUNTANT, Module.SHIPMENT, PermissionLevel.R);
        put(m, Role.SALES_MANAGER, Module.USER_AUDIT, PermissionLevel.R);
        return m;
    }

    private static void put(Map<Role, Map<Module, PermissionLevel>> m, Role r, Module mod, PermissionLevel lv) {
        m.computeIfAbsent(r, k -> new EnumMap<>(Module.class)).put(mod, lv);
    }
}
