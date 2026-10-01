package com.oms.service;

import com.oms.model.DeliveryPoint;
import com.oms.model.Role;
import com.oms.model.User;
import com.oms.support.DatabaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** S3-04: nhiều điểm giao hàng cho một đại lý, đúng một điểm mặc định, quyền theo người phụ trách. */
class DeliveryPointServiceTest extends DatabaseTest {

    private final DeliveryPointService service = new DeliveryPointService();
    private User manager;
    private User rep;
    private User otherRep;
    private long customerId;

    @BeforeEach
    void setUp() throws Exception {
        manager = user("qlkd", Role.SALES_MANAGER);
        rep = user("nvkd", Role.SALES_REP);
        otherRep = user("nvkd2", Role.SALES_REP);
        sql("INSERT INTO customers (id, code, name, group_id, sales_rep_id) VALUES (900, 'DL900', 'Đại lý thử', 1, " + rep.getId() + ")");
        sql("INSERT INTO customers (id, code, name, group_id, sales_rep_id) VALUES (901, 'DL901', 'Đại lý khác', 1, " + otherRep.getId() + ")");
        customerId = 900;
    }

    @Test
    void firstPointBecomesDefaultAndOthersAreNot() throws Exception {
        long first = service.save(customerId, 0, point("12 Hàng Bài", "Chị Lan", "0901234567", false), manager);
        long second = service.save(customerId, 0, point("Kho Long Biên", "Anh Hùng", "0912345678", false), manager);

        List<DeliveryPoint> points = service.list(customerId, manager);
        assertEquals(2, points.size());
        assertEquals(first, points.get(0).getId());
        assertTrue(points.get(0).isDefaultPoint());
        assertFalse(points.get(1).isDefaultPoint());
        assertEquals(second, points.get(1).getId());
        assertEquals(1, count("customer_delivery_points", "customer_id = 900 AND is_default = TRUE"));
    }

    @Test
    void choosingDefaultMovesTheFlag() throws Exception {
        service.save(customerId, 0, point("A", "Lan", "0901234567", false), manager);
        long second = service.save(customerId, 0, point("B", "Hùng", "0912345678", true), manager);

        assertEquals(1, count("customer_delivery_points", "customer_id = 900 AND is_default = TRUE"));
        assertEquals(second, service.list(customerId, manager).get(0).getId());

        long third = service.save(customerId, 0, point("C", "Mai", "0923456789", false), manager);
        service.makeDefault(customerId, third, manager);
        assertEquals(third, service.list(customerId, manager).get(0).getId());
        assertEquals(1, count("customer_delivery_points", "customer_id = 900 AND is_default = TRUE"));
        assertEquals(1, count("audit_logs", "action = 'DELIVERY_POINT_DEFAULT'"));
    }

    @Test
    void deletingDefaultPromotesOldestRemaining() throws Exception {
        long first = service.save(customerId, 0, point("A", "Lan", "0901234567", false), manager);
        long second = service.save(customerId, 0, point("B", "Hùng", "0912345678", false), manager);

        service.delete(customerId, first, manager);

        List<DeliveryPoint> points = service.list(customerId, manager);
        assertEquals(1, points.size());
        assertEquals(second, points.get(0).getId());
        assertTrue(points.get(0).isDefaultPoint());
        service.delete(customerId, second, manager);
        assertEquals(0, service.list(customerId, manager).size());
        assertEquals(2, count("audit_logs", "action = 'DELIVERY_POINT_DELETE'"));
    }

    @Test
    void editKeepsDefaultAndLogsBeforeAfter() throws Exception {
        long id = service.save(customerId, 0, point("Cũ", "Lan", "0901234567", false), manager);

        service.save(customerId, id, point("Mới", "Lan", "+84 90 123 4567", false), manager);

        DeliveryPoint p = service.list(customerId, manager).get(0);
        assertEquals("Mới", p.getAddress());
        assertEquals("0901234567", p.getReceiverPhone());
        assertTrue(p.isDefaultPoint());   // bỏ tick mặc định ở điểm đang mặc định thì vẫn giữ mặc định
        assertEquals(2, count("audit_logs", "action = 'DELIVERY_POINT_SAVE'"));
        assertEquals(1, count("audit_logs", "action = 'DELIVERY_POINT_SAVE' AND old_value LIKE '%Cũ%' AND new_value LIKE '%Mới%'"));
    }

    @Test
    void validatesRequiredFields() throws Exception {
        assertThrows(ServiceException.class, () -> service.save(customerId, 0, point(" ", "Lan", "0901234567", false), manager));
        assertThrows(ServiceException.class, () -> service.save(customerId, 0, point("A", "", "0901234567", false), manager));
        assertThrows(ServiceException.class, () -> service.save(customerId, 0, point("A", "Lan", "", false), manager));
        assertThrows(ServiceException.class, () -> service.save(customerId, 0, point("A", "Lan", "123", false), manager));
        DeliveryPoint longNote = point("A", "Lan", "0901234567", false);
        longNote.setRouteNote("x".repeat(501));
        assertThrows(ServiceException.class, () -> service.save(customerId, 0, longNote, manager));
        assertEquals(0, count("customer_delivery_points", "1 = 1"));
    }

    @Test
    void salesRepManagesOnlyOwnCustomersPoints() throws Exception {
        long mine = service.save(customerId, 0, point("A", "Lan", "0901234567", false), rep);
        assertEquals(1, service.list(customerId, rep).size());

        assertThrows(CustomerService.NotFoundException.class, () -> service.list(901, rep));
        assertThrows(CustomerService.NotFoundException.class, () -> service.save(901, 0, point("B", "Hùng", "0912345678", false), rep));
        assertThrows(CustomerService.NotFoundException.class, () -> service.delete(customerId, mine, otherRep));
        assertEquals(1, count("customer_delivery_points", "customer_id = 900"));
    }

    @Test
    void customerRoleCannotUseIt() throws Exception {
        User account = user("daily", Role.CUSTOMER);
        assertThrows(ForbiddenException.class, () -> service.list(customerId, account));
        assertThrows(ForbiddenException.class, () -> service.save(customerId, 0, point("A", "Lan", "0901234567", false), account));
    }

    @Test
    void pointOfAnotherCustomerIsRejected() throws Exception {
        long other = service.save(901, 0, point("Khác", "Mai", "0923456789", false), manager);
        assertThrows(ServiceException.class, () -> service.delete(customerId, other, manager));
        assertThrows(ServiceException.class, () -> service.makeDefault(customerId, other, manager));
        assertThrows(ServiceException.class, () -> service.save(customerId, other, point("Sửa lén", "Mai", "0923456789", false), manager));
        assertEquals(1, count("customer_delivery_points", "customer_id = 901 AND address = 'Khác'"));
    }

    private static DeliveryPoint point(String address, String receiver, String phone, boolean makeDefault) {
        DeliveryPoint p = new DeliveryPoint();
        p.setAddress(address);
        p.setReceiverName(receiver);
        p.setReceiverPhone(phone);
        p.setDefaultPoint(makeDefault);
        return p;
    }
}
