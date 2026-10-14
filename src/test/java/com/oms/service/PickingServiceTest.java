package com.oms.service;

import com.oms.model.NamedItem;
import com.oms.model.Order;
import com.oms.model.PickingList;
import com.oms.model.Role;
import com.oms.model.User;
import com.oms.support.DatabaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PickingServiceTest extends DatabaseTest {

    protected final OrderService orders = new OrderService();
    protected final OrderWorkflowService workflow = new OrderWorkflowService();
    protected final PickingService picking = new PickingService();
    protected User rep;
    protected User keeper;
    protected long first;
    protected long second;

    @BeforeEach
    void fixture() throws Exception {
        rep = user("rep", Role.SALES_REP);
        keeper = user("kho", Role.WAREHOUSE);
        keeper.setWarehouses(List.of(new NamedItem(10, "Kho A")));
        sql("INSERT INTO customers(id,code,name,group_id,sales_rep_id,credit_limit,credit_days,serving_warehouse_id) "
                + "VALUES(900,'DL','Đại lý',1," + rep.getId() + ",10000000,30,10)");
        sql("INSERT INTO customer_delivery_points(id,customer_id,address,receiver_name,is_default) VALUES(800,900,'Kho A','Anh A',TRUE)");
        sql("INSERT INTO products(id,sku,name,category_id,base_unit_id) VALUES(500,'A','Hàng A',3,1),(501,'B','Hàng B',4,1)");
        sql("INSERT INTO price_lists(id,code,name,customer_group_id,valid_from) VALUES(100,'BG','Giá',1,'2020-01-01')");
        sql("INSERT INTO price_list_items VALUES(100,500,10000,9000),(100,501,5000,4000)");
        sql("INSERT INTO warehouse_inventory VALUES(10,500,100,0),(10,501,100,0)");
        first = orders.create(900, rep);
        orders.addLine(first, 500, 1, "5", rep);
        orders.addLine(first, 501, 1, "2", rep);
        workflow.submit(first, rep);
        second = orders.create(900, rep);
        orders.addLine(second, 500, 1, "3", rep);
        workflow.submit(second, rep);
    }

    @Test
    void mergesApprovedOrdersIntoOnePickingListAndMovesThemToPicking() throws Exception {
        assertEquals(2, picking.page(keeper).queue().size());

        long id = picking.create(List.of(second, first), keeper);

        PickingList list = picking.get(id, keeper);
        assertEquals(2, list.getOrderCount());
        assertEquals(3, list.getLineCount());
        assertEquals("PS" + String.format("%06d", id), list.getCode());
        // Xếp theo nhóm hàng: "Nước ngọt có ga" trước "Thực phẩm khô"
        assertEquals("A", list.getLines().get(0).getSku());
        assertEquals("B", list.getLines().get(2).getSku());
        assertEquals(Order.Status.PICKING, orders.get(first, rep).getStatus());
        assertEquals(Order.Status.PICKING, orders.get(second, rep).getStatus());
        assertTrue(picking.page(keeper).queue().isEmpty());
        assertEquals(1, count("audit_logs", "action = 'PICKING_LIST_CREATE'"));
    }

    @Test
    void shortPickNeedsReasonAndListCompletesOnlyWhenAllLinesPicked() throws Exception {
        long id = picking.create(List.of(first), keeper);
        List<PickingList.Line> lines = picking.get(id, keeper).getLines();
        long lineA = lines.get(0).getId();
        long lineB = lines.get(1).getId();

        assertThrows(ServiceException.class, () -> picking.pick(id, lineA, new BigDecimal("4"), " ", keeper));
        assertThrows(ServiceException.class, () -> picking.pick(id, lineA, new BigDecimal("6"), null, keeper));
        assertThrows(ServiceException.class, () -> picking.pick(id, lineA, new BigDecimal("-1"), "x", keeper));
        picking.pick(id, lineA, new BigDecimal("4"), "Kệ chỉ còn 4", keeper);
        assertThrows(ServiceException.class, () -> picking.complete(id, keeper));

        picking.pick(id, lineB, new BigDecimal("2"), "bỏ qua", keeper);
        PickingList list = picking.get(id, keeper);
        assertTrue(list.getLines().get(0).isShortPicked());
        assertEquals("Kệ chỉ còn 4", list.getLines().get(0).getShortReason());
        assertFalse(list.getLines().get(1).isShortPicked());
        assertEquals(null, list.getLines().get(1).getShortReason());

        picking.complete(id, keeper);
        assertFalse(picking.get(id, keeper).isOpen());
        assertThrows(ServiceException.class, () -> picking.pick(id, lineA, new BigDecimal("5"), null, keeper));
    }

    @Test
    void rejectsWrongOrdersAndOtherWarehousesAndRoles() throws Exception {
        User foreign = user("kho2", Role.WAREHOUSE);
        foreign.setWarehouses(List.of(new NamedItem(20, "Kho B")));
        long draft = orders.create(900, rep);

        assertThrows(ServiceException.class, () -> picking.create(List.of(), keeper));
        assertThrows(ServiceException.class, () -> picking.create(List.of(draft), keeper));
        assertThrows(ServiceException.class, () -> picking.create(List.of(first), foreign));
        assertThrows(ForbiddenException.class, () -> picking.page(rep));

        long id = picking.create(List.of(first), keeper);
        assertThrows(PickingService.NotFoundException.class, () -> picking.get(id, foreign));
        assertThrows(ServiceException.class, () -> picking.create(List.of(first), keeper));
        assertTrue(picking.page(foreign).recent().isEmpty());
    }
}
