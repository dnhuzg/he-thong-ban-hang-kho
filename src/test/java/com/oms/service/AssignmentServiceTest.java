package com.oms.service;

import com.oms.dao.CustomerDao;
import com.oms.model.AssignmentChange;
import com.oms.model.Customer;
import com.oms.model.Role;
import com.oms.model.User;
import com.oms.support.DatabaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** S3-06: phân công người phụ trách, chuyển giao hàng loạt khi nhân viên nghỉ và lịch sử phân công. */
class AssignmentServiceTest extends DatabaseTest {

    private final AssignmentService service = new AssignmentService();
    private final CustomerService customers = new CustomerService();
    private User manager;
    private User accountant;
    private User leaver;
    private User heir;

    @BeforeEach
    void setUp() throws Exception {
        manager = user("qlkd", Role.SALES_MANAGER);
        accountant = user("ketoan", Role.ACCOUNTANT);
        leaver = user("nghiviec", Role.SALES_REP);
        heir = user("ketiep", Role.SALES_REP);
        sql("INSERT INTO customers (id, code, name, group_id, area_id, sales_rep_id) VALUES "
                + "(1, 'DL1', 'Đại lý 1', 1, 30, " + leaver.getId() + "), "
                + "(2, 'DL2', 'Đại lý 2', 1, 30, " + leaver.getId() + "), "
                + "(3, 'DL3', 'Đại lý 3', 1, NULL, " + leaver.getId() + "), "
                + "(4, 'DL4', 'Đại lý 4', 1, 30, " + heir.getId() + ")");
    }

    @Test
    void transfersSelectedCustomersAndKeepsHistory() throws Exception {
        int moved = service.transfer(leaver.getId(), heir.getId(), List.of(1L, 2L), "Nhân viên nghỉ việc", manager);

        assertEquals(2, moved);
        assertEquals(2, count("customers", "id IN (1, 2) AND sales_rep_id = " + heir.getId()));
        assertEquals(1, count("customers", "id = 3 AND sales_rep_id = " + leaver.getId()));   // đại lý không chọn giữ nguyên
        List<AssignmentChange> history = service.historyOf(1);
        assertEquals(1, history.size());
        assertEquals("nghiviec", history.get(0).getFromName());
        assertEquals("ketiep", history.get(0).getToName());
        assertEquals("Nhân viên nghỉ việc", history.get(0).getReason());
        assertEquals("qlkd", history.get(0).getChangedByName());
        assertEquals(2, service.recent(manager).size());
        assertEquals(1, count("audit_logs", "action = 'CUSTOMER_TRANSFER' AND detail = 'Nhân viên nghỉ việc' AND new_value LIKE '%Số đại lý: 2%'"));
    }

    @Test
    void filtersCandidatesByRepAndArea() throws Exception {
        assertEquals(3, service.customersOf(leaver.getId(), null, manager).size());
        assertEquals(2, service.customersOf(leaver.getId(), 30L, manager).size());
        assertEquals(1, service.customersOf(heir.getId(), null, manager).size());
        List<CustomerDao.RepLoad> loads = service.repsWithCustomers(manager);
        assertEquals(2, loads.size());
        assertEquals(3, loads.stream().filter(r -> r.id() == leaver.getId()).findFirst().orElseThrow().customers());
    }

    @Test
    void validatesBatchBeforeChangingAnything() throws Exception {
        assertThrows(ServiceException.class, () -> service.transfer(leaver.getId(), heir.getId(), List.of(), "Lý do", manager));
        assertThrows(ServiceException.class, () -> service.transfer(leaver.getId(), heir.getId(), List.of(1L), "  ", manager));
        assertThrows(ServiceException.class, () -> service.transfer(leaver.getId(), leaver.getId(), List.of(1L), "Lý do", manager));
        assertThrows(ServiceException.class, () -> service.transfer(leaver.getId(), heir.getId(), List.of(1L, 4L), "Lý do", manager));   // 4 không thuộc người cần chuyển
        assertThrows(ServiceException.class, () -> service.transfer(leaver.getId(), heir.getId(), List.of(1L, 999L), "Lý do", manager));
        User warehouse = user("kho", Role.WAREHOUSE);
        assertThrows(ServiceException.class, () -> service.transfer(leaver.getId(), warehouse.getId(), List.of(1L), "Lý do", manager));   // người nhận không phải nhân viên kinh doanh
        assertEquals(3, count("customers", "sales_rep_id = " + leaver.getId()));   // cả lô bị từ chối, không đổi gì
        assertEquals(0, count("customer_assignments", "1 = 1"));
    }

    @Test
    void lockedLeaverCannotReceiveButCanBeTransferredFrom() throws Exception {
        sql("UPDATE users SET status = 'LOCKED' WHERE id = " + leaver.getId());
        assertThrows(ServiceException.class, () -> service.transfer(heir.getId(), leaver.getId(), List.of(4L), "Lý do", manager));
        assertEquals(3, service.transfer(leaver.getId(), heir.getId(), List.of(1L, 2L, 3L), "Đã khoá tài khoản", manager));
        assertEquals(4, count("customers", "sales_rep_id = " + heir.getId()));
    }

    @Test
    void onlyManagerAndAdminCanTransfer() throws Exception {
        assertThrows(ForbiddenException.class, () -> service.transfer(leaver.getId(), heir.getId(), List.of(1L), "Lý do", accountant));
        assertThrows(ForbiddenException.class, () -> service.transfer(leaver.getId(), heir.getId(), List.of(1L), "Lý do", leaver));
        assertThrows(ForbiddenException.class, () -> service.repsWithCustomers(accountant));
        assertThrows(ForbiddenException.class, () -> service.recent(leaver));
        assertEquals(3, count("customers", "sales_rep_id = " + leaver.getId()));
        User admin = user("admin", Role.ADMIN);
        assertEquals(1, service.transfer(leaver.getId(), heir.getId(), List.of(1L), "Admin chuyển", admin));
    }

    @Test
    void changingRepInProfileIsRecordedAndRestrictedToManagers() throws Exception {
        Customer input = new Customer();
        input.setName("Đại lý 1");
        input.setGroupId(1L);
        input.setAreaId(30L);
        input.setSalesRepId(heir.getId());

        customers.update(1, input, accountant);   // kế toán sửa hồ sơ nhưng không đổi được người phụ trách
        assertEquals(1, count("customers", "id = 1 AND sales_rep_id = " + leaver.getId()));
        assertEquals(0, count("customer_assignments", "1 = 1"));

        customers.update(1, input, manager);
        assertEquals(1, count("customers", "id = 1 AND sales_rep_id = " + heir.getId()));
        List<AssignmentChange> history = service.historyOf(1);
        assertEquals(1, history.size());
        assertEquals("nghiviec", history.get(0).getFromName());
        assertTrue(history.get(0).getReason().contains("hồ sơ"));
    }

    @Test
    void createWithRepIsRecordedOnlyWhenManagerAssigns() throws Exception {
        Customer input = new Customer();
        input.setCode("DL9");
        input.setName("Đại lý mới");
        input.setGroupId(1L);
        input.setSalesRepId(heir.getId());

        long byAccountant = customers.create(input, accountant);
        assertEquals(1, count("customers", "id = " + byAccountant + " AND sales_rep_id IS NULL"));
        input.setCode("DL10");
        long byManager = customers.create(input, manager);
        assertEquals(1, count("customers", "id = " + byManager + " AND sales_rep_id = " + heir.getId()));
        assertEquals("Chưa phân công", service.historyOf(byManager).get(0).getFromLabel());
    }

    @Test
    void editingOtherFieldsWorksWhileRepIsLocked() throws Exception {
        sql("UPDATE users SET status = 'LOCKED' WHERE id = " + leaver.getId());
        Customer input = new Customer();
        input.setName("Đại lý 1 đổi tên");
        input.setGroupId(1L);
        input.setAreaId(30L);
        input.setSalesRepId(leaver.getId());   // giữ nguyên người phụ trách đã khoá: không bị từ chối

        customers.update(1, input, manager);

        assertEquals(1, count("customers", "id = 1 AND name = 'Đại lý 1 đổi tên'"));
        assertEquals(0, count("customer_assignments", "1 = 1"));
    }
}
