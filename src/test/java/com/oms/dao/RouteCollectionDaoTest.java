package com.oms.dao;

import com.oms.config.Db;
import com.oms.model.Role;
import com.oms.model.User;
import com.oms.support.DatabaseTest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RouteCollectionDaoTest extends DatabaseTest {

    @Test
    void pageIncludesEveryCollectionSoReconciliationDoesNotDropOlderRecords() throws Exception {
        User rep = user("route-rep", Role.SALES_REP);
        try (Connection connection = Db.getConnection();
             PreparedStatement customer = connection.prepareStatement(
                     "INSERT INTO customers (id, code, name, sales_rep_id) VALUES (900, 'DL900', 'Đại lý kiểm thử', ?)");
             PreparedStatement payment = connection.prepareStatement(
                     "INSERT INTO payments (code, customer_id, amount, method, paid_on, collected_by, note, created_by) "
                             + "VALUES (?, 900, ?, 'CASH', ?, ?, 'GHTK-71 test', ?)", PreparedStatement.RETURN_GENERATED_KEYS);
             PreparedStatement collection = connection.prepareStatement(
                     "INSERT INTO route_collections (payment_id, receipt_path) VALUES (?, ?)")) {
            customer.setLong(1, rep.getId());
            customer.executeUpdate();

            for (int i = 1; i <= 105; i++) {
                payment.setString(1, "RC" + String.format("%06d", i));
                payment.setBigDecimal(2, new BigDecimal("100.00"));
                payment.setDate(3, java.sql.Date.valueOf(LocalDate.now().minusDays(i % 10)));
                payment.setLong(4, rep.getId());
                payment.setLong(5, rep.getId());
                payment.executeUpdate();
                try (var keys = payment.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new IllegalStateException("Không tạo được phiếu thu kiểm thử.");
                    }
                    collection.setLong(1, keys.getLong(1));
                    collection.setString(2, "receipts/test-" + i + ".png");
                    collection.executeUpdate();
                }
            }

            RouteCollectionDao.PageData page = new RouteCollectionDao().page(connection, rep.getId());
            assertEquals(105, page.records().size(), "Bảng đối chiếu phải lấy cả khoản thu cũ hơn 100 bản ghi gần nhất");
        }
    }
}
