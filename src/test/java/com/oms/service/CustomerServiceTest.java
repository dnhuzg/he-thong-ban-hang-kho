package com.oms.service;

import com.oms.model.Customer;
import com.oms.model.PageResult;
import com.oms.model.Role;
import com.oms.model.User;
import com.oms.support.DatabaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** S3-03: hồ sơ đại lý — mã duy nhất, nhóm khách hàng bắt buộc, quyền sửa và phạm vi xem theo người phụ trách. */
class CustomerServiceTest extends DatabaseTest {

    private final CustomerService service = new CustomerService();
    private User manager;
    private User accountant;
    private User rep;

    @BeforeEach
    void setUp() throws Exception {
        manager = user("qlkd", Role.SALES_MANAGER);
        accountant = user("ketoan", Role.ACCOUNTANT);
        rep = user("nvkd", Role.SALES_REP);
    }

    @Test
    void createsCustomerWithAllProfileFieldsAndAudit() throws Exception {
        long id = service.create(input("dl100", "Đại lý Hồng Phúc", "0101234567", 1L, 30L, rep.getId()), manager);

        Customer c = service.get(id, manager);
        assertEquals("DL100", c.getCode());
        assertEquals("0101234567", c.getTaxCode());
        assertEquals("Đại lý cấp 1", c.getGroupName());
        assertEquals("Địa bàn A", c.getAreaName());
        assertEquals("nvkd", c.getSalesRepName());
        assertEquals(Customer.Status.ACTIVE, c.getStatus());
        assertEquals(1, count("audit_logs", "action = 'CUSTOMER_CREATE'"));
    }

    @Test
    void codeAndTaxCodeMustBeUnique() throws Exception {
        service.create(input("DL1", "Đại lý A", "0101234567", 1L, null, null), manager);

        assertThrows(ServiceException.class, () -> service.create(input("dl1", "Đại lý khác", null, 1L, null, null), manager));
        ServiceException e = assertThrows(ServiceException.class,
                () -> service.create(input("DL2", "Đại lý B", "0101234567", 1L, null, null), manager));
        assertTrue(e.getMessage().contains("Mã số thuế"));
        assertEquals(1, count("customers", "1 = 1"));
    }

    @Test
    void validatesRequiredFieldsAndFormats() throws Exception {
        assertThrows(ServiceException.class, () -> service.create(input("D", "Mã quá ngắn", null, 1L, null, null), manager));
        assertThrows(ServiceException.class, () -> service.create(input("DL3", "  ", null, 1L, null, null), manager));
        assertThrows(ServiceException.class, () -> service.create(input("DL3", "Sai MST", "123", 1L, null, null), manager));
        assertThrows(ServiceException.class, () -> service.create(input("DL3", "Thiếu nhóm", null, null, null, null), manager));
        assertThrows(ServiceException.class, () -> service.create(input("DL3", "Nhóm không có", null, 99L, null, null), manager));
        assertThrows(ServiceException.class, () -> service.create(input("DL3", "Khu vực lạ", null, 1L, 999L, null), manager));
        User warehouse = user("kho", Role.WAREHOUSE);
        assertThrows(ServiceException.class, () -> service.create(input("DL3", "Phụ trách là thủ kho", null, 1L, null, warehouse.getId()), manager));
        assertEquals(0, count("customers", "1 = 1"));
    }

    @Test
    void updateKeepsCodeAndLogsBeforeAfter() throws Exception {
        long id = service.create(input("DL1", "Đại lý A", null, 1L, null, null), accountant);

        service.update(id, input("KHAC", "Đại lý A mới", "0101234567", 2L, 30L, rep.getId()), accountant);

        Customer c = service.get(id, manager);
        assertEquals("DL1", c.getCode());
        assertEquals("Đại lý A mới", c.getName());
        assertEquals("Đại lý cấp 2", c.getGroupName());
        assertEquals(1, count("audit_logs", "action = 'CUSTOMER_UPDATE' AND old_value LIKE '%Đại lý A%' AND new_value LIKE '%Đại lý A mới%'"));
        service.update(id, input("DL1", "Đại lý A mới", "0101234567", 2L, 30L, rep.getId()), accountant);   // không đổi gì thì không ghi thêm
        assertEquals(1, count("audit_logs", "action = 'CUSTOMER_UPDATE'"));
    }

    @Test
    void onlyManagersAndAccountantsCanEdit() throws Exception {
        long id = service.create(input("DL1", "Đại lý A", null, 1L, null, rep.getId()), manager);

        assertThrows(ForbiddenException.class, () -> service.create(input("DL2", "Do NVKD tạo", null, 1L, null, null), rep));
        assertThrows(ForbiddenException.class, () -> service.update(id, input("DL1", "Sửa trái phép", null, 1L, null, null), rep));
        assertThrows(ForbiddenException.class, () -> service.delete(id, rep));
        assertEquals("Đại lý A", service.get(id, manager).getName());
    }

    @Test
    void salesRepOnlySeesOwnCustomers() throws Exception {
        User other = user("nvkd2", Role.SALES_REP);
        long mine = service.create(input("DL1", "Của tôi", null, 1L, null, rep.getId()), manager);
        long theirs = service.create(input("DL2", "Của người khác", null, 1L, null, other.getId()), manager);
        service.create(input("DL3", "Chưa phân công", null, 1L, null, null), manager);

        PageResult<Customer> own = service.search(null, null, null, null, null, 1, rep);
        assertEquals(1, own.getTotal());
        assertEquals("Của tôi", own.getItems().get(0).getName());
        assertEquals(3, service.search(null, null, null, null, null, 1, manager).getTotal());
        assertEquals(mine, service.get(mine, rep).getId());
        assertThrows(CustomerService.NotFoundException.class, () -> service.get(theirs, rep));
        // lọc theo người phụ trách khác không giúp nhân viên thấy đại lý của người khác
        assertEquals(0, service.search(null, null, null, other.getId(), null, 1, rep).getTotal());
    }

    @Test
    void customerAndWarehouseRolesCannotBrowse() throws Exception {
        User customer = user("daily", Role.CUSTOMER);
        User warehouse = user("kho", Role.WAREHOUSE);
        assertThrows(ForbiddenException.class, () -> service.search(null, null, null, null, null, 1, customer));
        assertThrows(ForbiddenException.class, () -> service.search(null, null, null, null, null, 1, warehouse));
    }

    @Test
    void deleteRemovesCustomerAndLogs() throws Exception {
        long id = service.create(input("DL1", "Đại lý A", null, 1L, null, null), manager);
        service.delete(id, manager);
        assertEquals(0, count("customers", "1 = 1"));
        assertEquals(1, count("audit_logs", "action = 'CUSTOMER_DELETE'"));
        assertThrows(CustomerService.NotFoundException.class, () -> service.delete(id, manager));
    }

    @Test
    void searchesByCodeNameOrPhoneAndFiltersCombine() throws Exception {
        Customer a = input("DL10", "Tạp hoá Minh Anh", null, 1L, 30L, rep.getId());
        a.setPhone("0901234567");
        service.create(a, manager);
        service.create(input("DL11", "Siêu thị Bình Dân", null, 2L, 30L, null), manager);
        long locked = service.create(input("DL12", "Đại lý Phú Quý", null, 1L, null, rep.getId()), manager);
        new CustomerLockService().lock(locked, "Nợ quá hạn", manager);

        assertEquals(1, service.search("DL10", null, null, null, null, 1, manager).getTotal());
        assertEquals(1, service.search("Bình Dân", null, null, null, null, 1, manager).getTotal());
        assertEquals(1, service.search("090 123 4567", null, null, null, null, 1, manager).getTotal());
        assertEquals(1, service.search("+84901234567", null, null, null, null, 1, manager).getTotal());
        assertEquals(0, service.search("0987654321", null, null, null, null, 1, manager).getTotal());
        assertEquals(2, service.search(null, 1L, null, null, null, 1, manager).getTotal());
        assertEquals(2, service.search(null, null, 30L, null, null, 1, manager).getTotal());
        assertEquals(1, service.search(null, 1L, 30L, null, null, 1, manager).getTotal());
        assertEquals(2, service.search(null, null, null, rep.getId(), null, 1, manager).getTotal());
        assertEquals(1, service.search(null, null, null, null, Customer.Status.LOCKED, 1, manager).getTotal());
        assertEquals(1, service.search(null, null, null, rep.getId(), Customer.Status.LOCKED, 1, manager).getTotal());
        assertEquals(2, service.search(null, null, null, null, null, 1, rep).getTotal());
        // Nhân viên kinh doanh không lọc vượt phạm vi đại lý của mình bằng cách chọn người phụ trách khác
        assertEquals(0, service.search(null, null, null, manager.getId(), null, 1, rep).getTotal());
    }

    private static Customer input(String code, String name, String tax, Long groupId, Long areaId, Long repId) {
        Customer c = new Customer();
        c.setCode(code);
        c.setName(name);
        c.setTaxCode(tax);
        c.setGroupId(groupId);
        c.setAreaId(areaId);
        c.setSalesRepId(repId);
        return c;
    }
}
