package com.oms.service;

import com.oms.model.Role;
import com.oms.model.Supplier;
import com.oms.model.User;
import com.oms.support.DatabaseTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** S2-09: mã, tên, MST, người liên hệ, điều khoản thanh toán; đã có phiếu nhập thì chỉ ngừng giao dịch. */
class SupplierServiceTest extends DatabaseTest {

    private final SupplierService service = new SupplierService();

    private static Supplier input(String code, String taxCode) {
        Supplier s = new Supplier();
        s.setCode(code);
        s.setName("Công ty TNHH Nước giải khát ABC");
        s.setTaxCode(taxCode);
        s.setContactName("Nguyễn Văn Hùng");
        s.setContactPhone("0903 123 456");
        s.setPaymentTerms("Công nợ 30 ngày");
        return s;
    }

    @Test
    void createsSupplierWithNormalizedCodeAndPhone() throws Exception {
        User keeper = user("kho01", Role.WAREHOUSE);
        long id = service.create(input(" ncc-abc ", "0101234567-001"), keeper);

        Supplier s = service.get(id);
        assertEquals("NCC-ABC", s.getCode());
        assertEquals("0903123456", s.getContactPhone());
        assertEquals("Công nợ 30 ngày", s.getPaymentTerms());
        assertEquals(Supplier.Status.ACTIVE, s.getStatus());
        assertEquals(1, count("audit_logs", "action = 'SUPPLIER_CREATE'"));
    }

    @Test
    void codeAndTaxCodeMustBeUniqueAndValid() throws Exception {
        User keeper = user("kho01", Role.WAREHOUSE);
        service.create(input("NCC-ABC", "0101234567"), keeper);
        assertThrows(ServiceException.class, () -> service.create(input("ncc-abc", null), keeper));
        ServiceException sameTax = assertThrows(ServiceException.class, () -> service.create(input("NCC-XYZ", "0101234567"), keeper));
        assertTrue(sameTax.getMessage().contains("đã được dùng"));
        assertThrows(ServiceException.class, () -> service.create(input("NCC-1", "12345"), keeper));
        assertThrows(ServiceException.class, () -> service.create(input("có dấu", null), keeper));
    }

    @Test
    void updateKeepsCodeAndLogsBeforeAfter() throws Exception {
        User keeper = user("kho01", Role.WAREHOUSE);
        long id = service.create(input("NCC-ABC", null), keeper);
        Supplier change = input("DOI-MA", null);
        change.setPaymentTerms("Công nợ 45 ngày");
        change.setStatus(Supplier.Status.INACTIVE);
        service.update(id, change, keeper);

        Supplier s = service.get(id);
        assertEquals("NCC-ABC", s.getCode());
        assertEquals(Supplier.Status.INACTIVE, s.getStatus());
        assertEquals(1, count("audit_logs", "action = 'SUPPLIER_UPDATE' AND new_value LIKE '%Công nợ 45 ngày%'"));
    }

    @Test
    void supplierWithReceiptsCannotBeDeleted() throws Exception {
        User keeper = user("kho01", Role.WAREHOUSE);
        long used = service.create(input("NCC-ABC", null), keeper);
        long unused = service.create(input("NCC-XYZ", null), keeper);
        // Mô phỏng phiếu nhập kho (S5-04) tham chiếu nhà cung cấp
        sql("CREATE TABLE fake_receipts (id BIGINT PRIMARY KEY, supplier_id BIGINT REFERENCES suppliers(id))");
        sql("INSERT INTO fake_receipts VALUES (1, " + used + ")");

        ServiceException e = assertThrows(ServiceException.class, () -> service.delete(used, keeper));
        assertTrue(e.getMessage().contains("Ngừng giao dịch"));
        assertEquals(1, count("suppliers", "id = " + used));
        service.delete(unused, keeper);
        assertEquals(0, count("suppliers", "id = " + unused));
    }
}
