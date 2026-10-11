package com.oms.service;

import com.oms.model.OrderStats;
import com.oms.model.Role;
import com.oms.model.User;
import com.oms.support.DatabaseTest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Số liệu đầu trang Đơn hàng: quản lý thấy mọi đơn, nhân viên kinh doanh chỉ thấy đơn của đại lý mình phụ trách. */
class OrderStatsTest extends DatabaseTest {

    private static final LocalDate TODAY = LocalDate.of(2027, 3, 15);

    @Test
    void countsWithinViewerScope() throws Exception {
        User manager = user("qlkd", Role.SALES_MANAGER);
        User rep = user("nvkd", Role.SALES_REP);
        sql("INSERT INTO customers (id, code, name, group_id, sales_rep_id) VALUES (700, 'A', 'Đại lý A', 1, " + rep.getId() + "), (701, 'B', 'Đại lý B', 1, NULL)");
        sql("INSERT INTO products (id, sku, name, category_id, base_unit_id) VALUES (500, 'COCA', 'Coca', 3, 1)");
        sql("INSERT INTO orders (id, code, customer_id, delivery_date, status, created_by, created_at) VALUES "
                + "(1, 'DH1', 700, DATE '2027-03-18', 'DRAFT', " + rep.getId() + ", TIMESTAMP '2027-03-10 08:00:00'), "
                + "(2, 'DH2', 700, DATE '2027-04-30', 'DRAFT', " + rep.getId() + ", TIMESTAMP '2027-02-10 08:00:00'), "
                + "(3, 'DH3', 701, NULL, 'DRAFT', " + manager.getId() + ", TIMESTAMP '2027-03-01 08:00:00')");
        sql("INSERT INTO order_lines (order_id, product_id, unit_id, quantity, factor) VALUES (1, 500, 1, 2, 1)");

        OrderStats all = new OrderService().stats(manager, TODAY);
        assertEquals(3, all.getTotal());
        assertEquals(2, all.getThisMonth());
        assertEquals(2, all.getCustomers());
        assertEquals(1, all.getDueSoon());
        assertEquals(2, all.getWithoutLines());

        OrderStats own = new OrderService().stats(rep, TODAY);
        assertEquals(2, own.getTotal());
        assertEquals(1, own.getCustomers());
        assertEquals(1, own.getWithoutLines());
    }
}
