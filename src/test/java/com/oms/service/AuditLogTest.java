package com.oms.service;

import com.oms.config.Db;
import com.oms.dao.AuditLogDao;
import com.oms.model.AuditEntry;
import com.oms.model.PageResult;
import com.oms.model.Role;
import com.oms.model.User;
import com.oms.support.DatabaseTest;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** S2-04: nhật ký có người thực hiện, thời điểm, giá trị trước / sau; lọc theo người, đối tượng, thời gian. */
class AuditLogTest extends DatabaseTest {

    private final AuditService auditService = new AuditService();

    @Test
    void profileUpdateRecordsBeforeAndAfter() throws Exception {
        User rep = user("rep01", Role.SALES_REP);
        new UserService().updateOwnProfile(rep, "Trần Bình", "0912345678");

        AuditEntry e = auditService.search(new AuditLogDao.Filter(rep.getId(), null, null, null), 1).getItems().get(0);
        assertEquals("PROFILE_UPDATE", e.getAction());
        assertEquals("Sửa hồ sơ cá nhân", e.getActionLabel());
        assertEquals("Họ tên: rep01\nSĐT: —", e.getOldValue());
        assertEquals("Họ tên: Trần Bình\nSĐT: 0912345678", e.getNewValue());
        assertEquals("rep01", e.getActorUsername());
        assertTrue(e.getCreatedAtText().matches("\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2}"));
    }

    @Test
    void lockRecordsStatusChangeAndTargetName() throws Exception {
        User admin = user("admin01", Role.ADMIN);
        User rep = user("rep02", Role.SALES_REP);
        new UserService().lock(rep.getId(), "Nghỉ việc", admin);

        AuditEntry e = auditService.search(new AuditLogDao.Filter(admin.getId(), "USER", null, null), 1).getItems().get(0);
        assertEquals("USER_LOCK", e.getAction());
        assertEquals("Nghỉ việc", e.getDetail());
        assertEquals("Trạng thái: Đang hoạt động", e.getOldValue());
        assertEquals("Trạng thái: Đã khoá", e.getNewValue());
        assertEquals("rep02", e.getTargetName());
    }

    @Test
    void filtersByActorTypeAndInclusiveDateRange() throws Exception {
        User a = user("a01", Role.ADMIN);
        User b = user("b01", Role.SALES_MANAGER);
        try (Connection conn = Db.getConnection()) {
            AuditLogDao dao = new AuditLogDao();
            dao.log(conn, a.getId(), "LOGIN", "USER", a.getId(), null);
            dao.log(conn, a.getId(), "PERMISSION_UPDATE", "ROLE_PERMISSIONS", null, null, "x: R", "x: W");
            dao.log(conn, b.getId(), "LOGIN", "USER", b.getId(), null);
        }
        sql("UPDATE audit_logs SET created_at = TIMESTAMP '2026-09-01 23:59:00' WHERE action = 'PERMISSION_UPDATE'");
        sql("UPDATE audit_logs SET created_at = TIMESTAMP '2026-09-05 08:00:00' WHERE actor_id = " + b.getId());

        assertEquals(2, total(new AuditLogDao.Filter(a.getId(), null, null, null)));
        assertEquals(1, total(new AuditLogDao.Filter(null, "ROLE_PERMISSIONS", null, null)));
        // "Đến ngày" tính trọn ngày: 23:59 ngày 01/09 vẫn nằm trong khoảng 01/09 → 01/09
        LocalDate day = LocalDate.of(2026, 9, 1);
        assertEquals(1, total(new AuditLogDao.Filter(null, null, day, day)));
        assertEquals(2, total(new AuditLogDao.Filter(null, null, day, LocalDate.of(2026, 9, 5))));
    }

    @Test
    void rejectsReversedDateRangeAndPagesTwentyPerPage() throws Exception {
        assertThrows(ServiceException.class, () -> auditService.search(
                new AuditLogDao.Filter(null, null, LocalDate.of(2026, 9, 5), LocalDate.of(2026, 9, 1)), 1));

        User a = user("a02", Role.ADMIN);
        try (Connection conn = Db.getConnection()) {
            for (int i = 0; i < 25; i++) {
                new AuditLogDao().log(conn, a.getId(), "LOGIN", "USER", a.getId(), null);
            }
        }
        PageResult<AuditEntry> page2 = auditService.search(new AuditLogDao.Filter(null, null, null, null), 2);
        assertEquals(25, page2.getTotal());
        assertEquals(5, page2.getItems().size());
    }

    private long total(AuditLogDao.Filter filter) throws Exception {
        return auditService.search(filter, 1).getTotal();
    }
}
