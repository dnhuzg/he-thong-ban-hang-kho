
package com.oms.service;

import com.oms.config.Db;
import com.oms.model.Role;
import com.oms.model.User;
import com.oms.support.DatabaseTest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InventoryAdjustmentServiceTest extends DatabaseTest {

    private final InventoryAdjustmentService service =
            new InventoryAdjustmentService();

    private User warehouseUser(String username) throws SQLException {
        User user = user(username, Role.WAREHOUSE);
        sql("INSERT INTO user_warehouses (user_id, warehouse_id) VALUES ("
                + user.getId() + ", 10)");
        return reload(user.getId());
    }

    private User manager(String username) throws SQLException {
        User user = user(username, Role.WH_MANAGER);
        sql("INSERT INTO user_warehouses (user_id, warehouse_id) VALUES ("
                + user.getId() + ", 10)");
        return reload(user.getId());
    }

    private void seedInventory() throws SQLException {
        sql("""
                INSERT INTO products
                    (id, sku, name, base_unit_id)
                VALUES
                    (201, 'SP201', 'Sản phẩm A', 1),
                    (202, 'SP202', 'Sản phẩm B', 1)
                """);

        sql("""
                INSERT INTO warehouse_inventory
                    (warehouse_id, product_id, on_hand, reserved)
                VALUES
                    (10, 201, 100, 10),
                    (10, 202, 50, 0)
                """);
    }

    private InventoryAdjustmentService.LineInput line(
            long productId,
            String reason,
            String detail,
            String quantity,
            String lossValue
    ) {
        return new InventoryAdjustmentService.LineInput(
                productId,
                reason,
                detail,
                new BigDecimal(quantity),
                new BigDecimal(lossValue)
        );
    }

    private BigDecimal stock(long productId) throws SQLException {
        try (Connection conn = Db.getConnection();
             var ps = conn.prepareStatement("""
                     SELECT on_hand
                     FROM warehouse_inventory
                     WHERE warehouse_id = 10 AND product_id = ?
                     """)) {
            ps.setLong(1, productId);

            try (var rs = ps.executeQuery()) {
                assertTrue(rs.next());
                return rs.getBigDecimal(1);
            }
        }
    }

    /**
     * AC1 + AC2:
     * Lập phiếu với ba lý do hỏng, mất, hết hạn.
     * Bắt buộc ghi giá trị tổn thất.
     * Tạo phiếu chưa được phép trừ tồn kho.
     */
    @Test
    void createAdjustmentForDamagedLostAndExpiredGoods()
            throws SQLException {

        seedInventory();
        User creator = warehouseUser("adj_creator_1");

        long id = service.create(
                10,
                "Kiểm tra tổn thất tại kho",
                List.of(
                        line(201, "DAMAGED",
                                "Vỡ bao bì", "2", "100000"),
                        line(202, "LOST",
                                "Thiếu khi kiểm tra", "3", "150000"),
                        line(201, "EXPIRED",
                                "Hết hạn sử dụng", "1", "50000")
                ),
                creator
        );

        var detail = service.get(id, creator);

        assertEquals("DRAFT", detail.adjustment().status());
        assertEquals(3, detail.lines().size());

        assertEquals(
                0,
                new BigDecimal("300000")
                        .compareTo(detail.adjustment().totalLossValue())
        );

        assertEquals(0, new BigDecimal("100")
                .compareTo(stock(201)));
        assertEquals(0, new BigDecimal("50")
                .compareTo(stock(202)));

        assertEquals(0L, count(
                "inventory_movements",
                "reference_type = 'INVENTORY_ADJUSTMENT'"
        ));
    }

    /**
     * AC2:
     * Không cho tạo phiếu thiếu lý do chi tiết
     * hoặc giá trị tổn thất.
     */
    @Test
    void rejectInvalidReasonAndLossValue()
            throws SQLException {

        User creator = warehouseUser("adj_creator_2");

        assertThrows(
                ServiceException.class,
                () -> service.create(
                        10,
                        null,
                        List.of(line(
                                201, "DAMAGED", " ",
                                "2", "100000"
                        )),
                        creator
                )
        );

        assertThrows(
                ServiceException.class,
                () -> service.create(
                        10,
                        null,
                        List.of(new InventoryAdjustmentService.LineInput(
                                201,
                                "LOST",
                                "Thiếu hàng",
                                new BigDecimal("2"),
                                null
                        )),
                        creator
                )
        );

        assertEquals(0L, count(
                "inventory_adjustments", "1 = 1"
        ));
    }

    /**
     * AC3:
     * Người lập không được tự duyệt phiếu,
     * kể cả khi có quyền Quản lý kho.
     */
    @Test
    void creatorCannotApproveOwnAdjustment()
            throws SQLException {

        seedInventory();

        User creator = manager("adj_manager_creator");

        long id = service.create(
                10,
                null,
                List.of(line(
                        201, "DAMAGED",
                        "Hàng bị vỡ", "5", "250000"
                )),
                creator
        );

        service.submit(id, creator);

        assertThrows(
                ForbiddenException.class,
                () -> service.approve(id, creator)
        );

        assertEquals(
                "PENDING",
                service.get(id, creator).adjustment().status()
        );

        assertEquals(0, new BigDecimal("100")
                .compareTo(stock(201)));
    }

    /**
     * AC3:
     * Nhân viên kho không được phê duyệt.
     */
    @Test
    void onlyWarehouseManagerCanApprove()
            throws SQLException {

        seedInventory();

        User creator = warehouseUser("adj_creator_4");
        User anotherWorker = warehouseUser("adj_worker_4");

        long id = service.create(
                10,
                null,
                List.of(line(
                        201, "LOST",
                        "Thất lạc khi kiểm kê", "2", "100000"
                )),
                creator
        );

        service.submit(id, creator);

        assertThrows(
                ForbiddenException.class,
                () -> service.approve(id, anotherWorker)
        );

        assertEquals("PENDING",
                service.get(id, creator).adjustment().status());
    }

    /**
     * AC3 + AC4:
     * Quản lý kho khác người lập được duyệt.
     * Khi duyệt mới trừ tồn và ghi thẻ kho.
     * Không được duyệt lại lần thứ hai.
     */
    @Test
    void approvalDeductsStockAndRecordsMovementOnce()
            throws SQLException {

        seedInventory();

        User creator = warehouseUser("adj_creator_5");
        User approver = manager("adj_manager_5");

        long id = service.create(
                10,
                "Điều chỉnh hàng hỏng",
                List.of(line(
                        201, "DAMAGED",
                        "Vỡ trong kho", "5", "250000"
                )),
                creator
        );

        service.submit(id, creator);
        service.approve(id, approver);

        var detail = service.get(id, approver);

        assertEquals("APPROVED", detail.adjustment().status());
        assertEquals(
                approver.getId(),
                detail.adjustment().approvedBy()
        );

        assertEquals(0, new BigDecimal("95")
                .compareTo(stock(201)));

        assertEquals(1L, count(
                "inventory_movements",
                "reference_type = 'INVENTORY_ADJUSTMENT'"
                        + " AND reference_id = " + id
                        + " AND movement_type = 'INVENTORY_ADJUSTMENT_LOSS'"
                        + " AND quantity = -5"
        ));

        assertThrows(
                ServiceException.class,
                () -> service.approve(id, approver)
        );

        assertEquals(0, new BigDecimal("95")
                .compareTo(stock(201)));

        assertEquals(1L, count(
                "inventory_movements",
                "reference_type = 'INVENTORY_ADJUSTMENT'"
                        + " AND reference_id = " + id
        ));
    }

    /**
     * AC4:
     * Nếu tồn kho không đủ, việc phê duyệt phải rollback.
     * Không ghi nhận biến động dở dang.
     */
    @Test
    void insufficientStockRollsBackApproval()
            throws SQLException {

        seedInventory();

        User creator = warehouseUser("adj_creator_6");
        User approver = manager("adj_manager_6");

        long id = service.create(
                10,
                null,
                List.of(
                        line(201, "DAMAGED",
                                "Hỏng bao bì", "5", "250000"),
                        line(202, "LOST",
                                "Mất hàng", "100", "5000000")
                ),
                creator
        );

        service.submit(id, creator);

        assertThrows(
                ServiceException.class,
                () -> service.approve(id, approver)
        );

        assertEquals("PENDING",
                service.get(id, creator).adjustment().status());

        assertEquals(0, new BigDecimal("100")
                .compareTo(stock(201)));

        assertEquals(0, new BigDecimal("50")
                .compareTo(stock(202)));

        assertEquals(0L, count(
                "inventory_movements",
                "reference_type = 'INVENTORY_ADJUSTMENT'"
                        + " AND reference_id = " + id
        ));
    }

    /**
     * Từ chối phiếu không được trừ tồn kho.
     */
    @Test
    void rejectionDoesNotChangeInventory()
            throws SQLException {

        seedInventory();

        User creator = warehouseUser("adj_creator_7");
        User approver = manager("adj_manager_7");

        long id = service.create(
                10,
                null,
                List.of(line(
                        201, "EXPIRED",
                        "Hết hạn sử dụng", "4", "200000"
                )),
                creator
        );

        service.submit(id, creator);
        service.reject(
                id,
                "Cần kiểm tra lại số lượng",
                approver
        );

        assertEquals(
                "REJECTED",
                service.get(id, creator).adjustment().status()
        );

        assertEquals(0, new BigDecimal("100")
                .compareTo(stock(201)));

        assertEquals(0L, count(
                "inventory_movements",
                "reference_type = 'INVENTORY_ADJUSTMENT'"
        ));
    }
}
