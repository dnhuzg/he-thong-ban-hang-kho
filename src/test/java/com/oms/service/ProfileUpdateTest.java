package com.oms.service;

import com.oms.model.Role;
import com.oms.model.User;
import com.oms.support.DatabaseTest;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** S2-02: người dùng tự sửa họ tên, số điện thoại; không đổi được tài khoản, vai trò, phạm vi. */
class ProfileUpdateTest extends DatabaseTest {

    private final UserService service = new UserService();

    @Test
    void updatesNameAndNormalizedPhone() throws Exception {
        User rep = user("rep01", Role.SALES_REP);
        service.updateOwnProfile(rep, "  Trần Thị Bình  ", "+84 912 345 678");

        User saved = reload(rep.getId());
        assertEquals("Trần Thị Bình", saved.getFullName());
        assertEquals("0912345678", saved.getPhone());
        assertEquals(1, count("audit_logs", "action = 'PROFILE_UPDATE' AND actor_id = " + rep.getId()));
    }

    @Test
    void blankPhoneClearsIt() throws Exception {
        User rep = user("rep02", Role.SALES_REP);
        service.updateOwnProfile(rep, "Bình", "0912345678");
        service.updateOwnProfile(rep, "Bình", "");
        assertNull(reload(rep.getId()).getPhone());
    }

    @Test
    void rejectsInvalidPhoneAndBlankName() throws Exception {
        User rep = user("rep03", Role.SALES_REP);
        assertThrows(ServiceException.class, () -> service.updateOwnProfile(rep, "Bình", "12345"));
        assertThrows(ServiceException.class, () -> service.updateOwnProfile(rep, "   ", "0912345678"));
        assertEquals("rep03", reload(rep.getId()).getFullName());
    }

    @Test
    void doesNotTouchUsernameEmailRolesOrScope() throws Exception {
        User keeper = user("kho01", Role.WAREHOUSE);
        sql("INSERT INTO user_warehouses VALUES (" + keeper.getId() + ", 10)");

        service.updateOwnProfile(keeper, "Lê Văn Kho", "0987111222");

        User saved = reload(keeper.getId());
        assertEquals("kho01", saved.getUsername());
        assertEquals("kho01@example.test", saved.getEmail());
        assertEquals(EnumSet.of(Role.WAREHOUSE), saved.getRoles());
        assertEquals(1, saved.getWarehouses().size());
    }

    @Test
    void lockedAccountCannotUpdate() throws Exception {
        User rep = user("rep04", Role.SALES_REP);
        sql("UPDATE users SET status = 'LOCKED' WHERE id = " + rep.getId());
        assertThrows(ServiceException.class, () -> service.updateOwnProfile(rep, "Tên mới", "0912345678"));
    }
}
