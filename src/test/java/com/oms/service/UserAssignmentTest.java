package com.oms.service;

import com.oms.model.Role;
import com.oms.model.User;
import com.oms.support.DatabaseTest;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserAssignmentTest extends DatabaseTest {
    private final UserService service = new UserService();

    private UserForm form(User user, Role... roles) {
        UserForm form = new UserForm();
        form.setId(user.getId());
        form.setUsername(user.getUsername());
        form.setFullName("Tên cập nhật");
        form.setEmail(user.getEmail());
        form.setRoles(EnumSet.of(roles[0], roles));
        return form;
    }

    @Test
    void adminCanAssignMultipleRolesWarehousesAndAreas() throws Exception {
        User admin = user("admin", Role.ADMIN);
        User target = user("staff", Role.SALES_REP);
        UserForm form = form(target, Role.WAREHOUSE, Role.SALES_REP);
        form.setWarehouseIds(new LinkedHashSet<>(Set.of(10L, 20L)));
        form.setAreaIds(new LinkedHashSet<>(Set.of(30L)));
        service.update(form, admin);
        User saved = reload(target.getId());
        assertEquals(EnumSet.of(Role.WAREHOUSE, Role.SALES_REP), saved.getRoles());
        assertEquals(Set.of(10L, 20L), saved.getWarehouses().stream().map(w -> w.getId()).collect(java.util.stream.Collectors.toSet()));
        assertEquals(30L, saved.getAreas().get(0).getId());
        assertEquals(1, count("audit_logs", "action = 'USER_UPDATE'"));
    }

    @Test
    void warehouseRoleRequiresAtLeastOneWarehouse() throws Exception {
        User admin = user("admin", Role.ADMIN);
        User target = user("staff", Role.SALES_REP);
        assertThrows(ServiceException.class, () -> service.update(form(target, Role.WH_MANAGER), admin));
        assertEquals(EnumSet.of(Role.SALES_REP), reload(target.getId()).getRoles());
    }

    @Test
    void adminCannotRemoveOwnAdminRole() throws Exception {
        User admin = user("admin", Role.ADMIN);
        assertThrows(ServiceException.class, () -> service.update(form(admin, Role.SALES_REP), admin));
        assertTrue(reload(admin.getId()).isAdmin());
    }

    @Test
    void unknownWarehouseRollsBackProfileAndRoles() throws Exception {
        User admin = user("admin", Role.ADMIN);
        User target = user("staff", Role.SALES_REP);
        UserForm form = form(target, Role.WAREHOUSE);
        form.setWarehouseIds(Set.of(999L));
        assertThrows(ServiceException.class, () -> service.update(form, admin));
        User saved = reload(target.getId());
        assertEquals(target.getFullName(), saved.getFullName());
        assertEquals(EnumSet.of(Role.SALES_REP), saved.getRoles());
        assertEquals(0, count("audit_logs", "action = 'USER_UPDATE'"));
    }

    @Test
    void unknownAreaIsRejectedWithoutPartialUpdate() throws Exception {
        User admin = user("admin", Role.ADMIN);
        User target = user("staff", Role.SALES_REP);
        UserForm form = form(target, Role.SALES_REP);
        form.setAreaIds(Set.of(-1L));
        assertThrows(ServiceException.class, () -> service.update(form, admin));
        assertEquals(target.getFullName(), reload(target.getId()).getFullName());
    }

    @Test
    void nonAdminCannotGrantAdminEvenWhenCallingServiceDirectly() throws Exception {
        User sales = user("sales", Role.SALES_REP);
        User target = user("staff", Role.SALES_REP);
        assertThrows(ServiceException.class, () -> service.update(form(target, Role.ADMIN), sales));
        assertFalse(reload(target.getId()).isAdmin());
    }

    @Test
    void staleAdminRoleCannotAuthorizeChanges() throws Exception {
        User admin = user("admin", Role.ADMIN);
        User target = user("staff", Role.SALES_REP);
        sql("DELETE FROM user_roles WHERE user_id = " + admin.getId());
        assertThrows(ServiceException.class, () -> service.update(form(target, Role.ADMIN), admin));
        assertFalse(reload(target.getId()).isAdmin());
    }
}
