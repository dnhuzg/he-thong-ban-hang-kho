package com.oms.service;

import com.oms.model.Role;
import com.oms.model.User;
import com.oms.support.DatabaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StockAdjustmentServiceTest extends DatabaseTest {

    private final StockAdjustmentService service = new StockAdjustmentService();
    private User keeper;
    private User manager;
    private User otherManager;

    @BeforeEach
    void fixture() throws Exception {
        keeper = user("thukho", Role.WAREHOUSE);
        manager = user("qlkho", Role.WH_MANAGER);
        otherManager = user("qlkho2", Role.WH_MANAGER);
        for (User u : new User[] {keeper, manager, otherManager}) {
            sql("INSERT INTO user_warehouses VALUES(" + u.getId() + ",10)");
        }
        sql("INSERT INTO products(id,sku,name,base_unit_id,cost_price,status) VALUES(801,'NUOC','Nước suối',1,6000,'ACTIVE'),"
                + "(802,'SUA','Sữa hộp',1,9000,'ACTIVE')");
        sql("INSERT INTO warehouse_inventory(warehouse_id,product_id,on_hand,reserved) VALUES(10,801,100,20),(20,801,50,0),(10,802,30,0)");
    }

    private StockAdjustmentService.Form form(long warehouse, long product, Long lot, String qty, String reason) {
        return new StockAdjustmentService.Form(warehouse, product, lot, new BigDecimal(qty), reason, "Thùng móp vỡ khi bốc xếp");
    }

    @Test
    void createdAdjustmentWaitsForApprovalAndRecordsLossValue() throws Exception {
        long id = service.create(form(10, 801, null, "10", "DAMAGED"), keeper);

        assertEquals(1, count("stock_adjustments", "id=" + id + " AND code='DC000001' AND status='PENDING' AND loss_value=60000"));
        assertEquals(1, count("warehouse_inventory", "warehouse_id=10 AND product_id=801 AND on_hand=100"));
        assertEquals(1, count("audit_logs", "action='STOCK_ADJUSTMENT_CREATE'"));
    }

    @Test
    void onlyAnotherWarehouseManagerApprovesAndApprovalDeductsStockOntoStockCard() throws Exception {
        long id = service.create(form(10, 801, null, "10", "LOST"), manager);

        assertThrows(ForbiddenException.class, () -> service.approve(id, keeper));
        assertThrows(ServiceException.class, () -> service.approve(id, manager));      // người lập không tự duyệt
        service.approve(id, otherManager);

        assertEquals(1, count("warehouse_inventory", "warehouse_id=10 AND product_id=801 AND on_hand=90 AND reserved=20"));
        assertEquals(1, count("inventory_movements", "movement_type='ADJUSTMENT_OUT' AND reference_type='STOCK_ADJUSTMENT' AND quantity=10"));
        assertEquals(1, count("stock_adjustments", "id=" + id + " AND status='APPROVED' AND decided_by=" + otherManager.getId()));
        assertThrows(ServiceException.class, () -> service.approve(id, otherManager)); // không duyệt hai lần

        StockCardService.Card card = new StockCardService().card(
                new StockCardService.Filter(801L, 10L, LocalDate.now().minusDays(1), LocalDate.now()));
        StockCardService.Line line = card.lines().get(card.lines().size() - 1);
        assertEquals("Điều chỉnh giảm", line.type());
        assertEquals("DC000001", line.documentCode());
        assertEquals(0, new BigDecimal("10").compareTo(line.decrease()));
    }

    @Test
    void rejectionNeedsReasonAndLeavesStockUntouched() throws Exception {
        long id = service.create(form(10, 801, null, "5", "EXPIRED"), keeper);

        assertThrows(ServiceException.class, () -> service.reject(id, " ", manager));
        service.reject(id, "Chưa có biên bản huỷ", manager);

        assertEquals(1, count("stock_adjustments", "id=" + id + " AND status='REJECTED'"));
        assertEquals(1, count("warehouse_inventory", "warehouse_id=10 AND product_id=801 AND on_hand=100"));
        assertThrows(ServiceException.class, () -> service.approve(id, manager));
    }

    @Test
    void validatesReasonQuantityAvailableStockAndWarehouseScope() throws Exception {
        assertThrows(ServiceException.class, () -> service.create(form(10, 801, null, "5", "OTHER"), keeper));
        assertThrows(ServiceException.class, () -> service.create(form(10, 801, null, "0", "DAMAGED"), keeper));
        assertThrows(ServiceException.class, () -> service.create(form(10, 801, null, "81", "DAMAGED"), keeper)); // khả dụng 80
        assertThrows(ForbiddenException.class, () -> service.create(form(20, 801, null, "1", "DAMAGED"), keeper));
        assertThrows(ServiceException.class, () -> service.create(
                new StockAdjustmentService.Form(10, 801, null, BigDecimal.ONE, "DAMAGED", " "), keeper));
        assertThrows(ForbiddenException.class, () -> service.create(form(10, 801, null, "1", "DAMAGED"), user("ketoan", Role.ACCOUNTANT)));
    }

    @Test
    void trackedSkuMustNameALotAndApprovalDeductsThatLot() throws Exception {
        sql("UPDATE products SET track_lots=TRUE WHERE id=802");
        sql("INSERT INTO inventory_lots(id,warehouse_id,product_id,lot_no,manufactured_date,expiry_date,remaining_quantity,unit_cost,received_at) "
                + "VALUES(71,10,802,'S1','" + LocalDate.now().minusMonths(6) + "','" + LocalDate.now().minusDays(1) + "',30,9000,CURRENT_TIMESTAMP)");
        assertThrows(ServiceException.class, () -> service.create(form(10, 802, null, "4", "EXPIRED"), keeper));

        long id = service.create(form(10, 802, 71L, "4", "EXPIRED"), keeper);
        service.approve(id, manager);

        assertEquals(1, count("inventory_lots", "id=71 AND remaining_quantity=26"));
        assertEquals(1, count("warehouse_inventory", "warehouse_id=10 AND product_id=802 AND on_hand=26"));
    }
}
