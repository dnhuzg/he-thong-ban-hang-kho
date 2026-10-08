package com.oms.service;

import com.oms.model.Customer;
import com.oms.model.CustomerStats;
import com.oms.model.Role;
import com.oms.model.User;
import com.oms.support.DatabaseTest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Số liệu đầu trang Đại lý: quản lý thấy toàn bộ, nhân viên kinh doanh chỉ thấy đại lý mình phụ trách. */
class CustomerStatsTest extends DatabaseTest {

    @Test
    void countsWithinViewerScope() throws Exception {
        User manager = user("qlkd", Role.SALES_MANAGER);
        User rep = user("nvkd", Role.SALES_REP);
        sql("INSERT INTO customers (code, name, group_id, sales_rep_id, status, credit_limit, credit_days) VALUES "
                + "('A', 'Đại lý A', 1, " + rep.getId() + ", 'ACTIVE', 5000000, 30), "
                + "('B', 'Đại lý B', 1, " + rep.getId() + ", 'LOCKED', 0, 0), "
                + "('C', 'Đại lý C', 1, NULL, 'ACTIVE', 2000000, 15)");

        CustomerStats all = new CustomerService().stats(manager);
        assertEquals(3, all.getTotal());
        assertEquals(2, all.getActive());
        assertEquals(1, all.getLocked());
        assertEquals(1, all.getUnassigned());
        assertEquals(2, all.getWithCredit());
        assertEquals(1, all.getPayNow());
        assertEquals(0, new BigDecimal("7000000").compareTo(all.getTotalCreditLimit()));

        CustomerStats own = new CustomerService().stats(rep);
        assertEquals(2, own.getTotal());
        assertEquals(0, own.getUnassigned());
        assertEquals(0, new BigDecimal("5000000").compareTo(own.getTotalCreditLimit()));
    }

    @Test
    void initialsComeFromTheLastTwoWordsOfTheName() {
        Customer c = new Customer();
        c.setName("Cửa hàng Hoàng Long");
        assertEquals("HL", c.getInitials());
        c.setName("Vinmart");
        assertEquals("V", c.getInitials());
    }
}
