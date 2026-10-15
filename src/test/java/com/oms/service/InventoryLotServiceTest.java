package com.oms.service;

import com.oms.config.Db;
import com.oms.model.Order;
import com.oms.model.Role;
import com.oms.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class InventoryLotServiceTest extends PickingServiceTest {
    private final InventoryLotService lots = new InventoryLotService();
    private final ShipmentExportService exports = new ShipmentExportService();
    private User manager;

    @BeforeEach
    void lotFixture() throws Exception {
        sql("CREATE TABLE role_permissions(role_code VARCHAR(30),module_code VARCHAR(30),level VARCHAR(20),PRIMARY KEY(role_code,module_code))");
        sql("INSERT INTO role_permissions VALUES('WH_MANAGER','INVENTORY','F'),('WAREHOUSE','INVENTORY','R')");
        sql("INSERT INTO user_warehouses VALUES("+keeper.getId()+",10)");
        manager=user("whmanager",Role.WH_MANAGER);
        sql("INSERT INTO user_warehouses VALUES("+manager.getId()+",10)");
        manager=reload(manager.getId());
    }

    private void lot(long id, long product, int remaining, int expiryOffset) throws Exception {
        sql("INSERT INTO inventory_lots(id,warehouse_id,product_id,lot_no,manufactured_date,expiry_date,remaining_quantity,unit_cost,received_at) VALUES("
                +id+",10,"+product+",'L"+id+"','"+LocalDate.now().minusYears(1)+"','"+LocalDate.now().plusDays(expiryOffset)+"',"+remaining+",100,CURRENT_TIMESTAMP)");
    }

    private void ready() throws Exception {
        long list=picking.create(List.of(first),keeper);
        for (var line:picking.get(list,keeper).getLines()) picking.pick(list,line.getId(),line.getRequired(),null,keeper);
        picking.complete(list,keeper);
    }

    @Test void configIsPersistentValidatedAndCannotDisableToBypassExpiry() throws Exception {
        assertThrows(ServiceException.class,()->lots.configure(500,true,30,manager));
        lot(1,500,100,30);
        lots.configure(500,true,0,manager);
        assertEquals(1,count("products","id=500 AND track_lots=TRUE AND expiry_warning_days=0"));
        assertThrows(ServiceException.class,()->lots.configure(500,false,30,manager));
        assertThrows(ServiceException.class,()->lots.configure(500,true,366,manager));
        assertThrows(ForbiddenException.class,()->lots.configure(500,true,30,keeper));
        sql("UPDATE inventory_lots SET remaining_quantity=0 WHERE product_id=500");
        sql("UPDATE warehouse_inventory SET on_hand=0,reserved=0 WHERE product_id=500");
        lots.configure(500,false,365,manager);
        assertEquals(1,count("products","id=500 AND track_lots=FALSE"));
    }

    @Test void readFiltersAssignedWarehousesAndUsesCurrentPermissions() throws Exception {
        lot(1,500,100,10);
        sql("INSERT INTO inventory_lots(warehouse_id,product_id,remaining_quantity,unit_cost,received_at) VALUES(20,500,10,100,CURRENT_TIMESTAMP)");
        assertEquals(1,lots.page(null,null,"","",keeper).lots().size());
        assertEquals(1,lots.page(500L,10L,"soon","L1",keeper).soon());
        assertThrows(ForbiddenException.class,()->lots.page(null,20L,"","",keeper));
        assertFalse(lots.page(null,null,"","",keeper).canManage());
        sql("DELETE FROM role_permissions WHERE role_code='WAREHOUSE'");
        assertThrows(ForbiddenException.class,()->lots.page(null,null,"","",keeper));
    }

    @Test void metadataRequiresManagerAndCannotRewriteTrackedExpiry() throws Exception {
        sql("INSERT INTO inventory_lots(id,warehouse_id,product_id,remaining_quantity,unit_cost,received_at) VALUES(1,10,500,100,100,CURRENT_TIMESTAMP)");
        assertThrows(ServiceException.class,()->lots.configure(500,true,30,manager));
        assertThrows(ForbiddenException.class,()->lots.updateMetadata(1,"LO",LocalDate.now(),LocalDate.now(),keeper));
        lots.updateMetadata(1,"LO",LocalDate.now().minusMonths(1),LocalDate.now(),manager);
        lots.configure(500,true,30,manager);
        assertThrows(ServiceException.class,()->lots.updateMetadata(1,"LO",LocalDate.now(),LocalDate.now().plusYears(1),manager));
        assertEquals(1,count("audit_logs","action='LOT_METADATA_UPDATE'"));
        assertEquals(1,count("inventory_lots","remaining_quantity=100"));
    }

    @Test void expiredShipmentRollsBackEverythingAndCannotUseLifecycleShortcut() throws Exception {
        lot(1,500,100,-1); lots.configure(500,true,30,manager); ready();
        assertThrows(ServiceException.class,()->exports.export(first,keeper));
        assertEquals(Order.Status.PICKING,orders.get(first,rep).getStatus());
        assertEquals(1,count("warehouse_inventory","product_id=500 AND on_hand=100 AND reserved=8"));
        assertEquals(0,count("shipments","1=1"));
        assertEquals(0,count("shipment_lot_allocations","1=1"));
        assertThrows(ServiceException.class,()->new OrderLifecycleService().advance(first,Order.Status.SHIPPED,keeper));
        assertThrows(ForbiddenException.class,()->exports.export(first,keeper,true,"Kiểm thử"));
        assertThrows(ForbiddenException.class,()->exports.export(first,manager,true," "));
    }

    @Test void validLotsPreferredAndTodayIsStillValid() throws Exception {
        lot(1,500,95,-1); lot(2,500,5,0); lots.configure(500,true,0,manager); ready();
        long shipment=exports.export(first,keeper);
        assertEquals(1,count("inventory_lots","id=2 AND remaining_quantity=0"));
        assertEquals(1,count("inventory_lots","id=1 AND remaining_quantity=95"));
        assertEquals(1,count("shipment_lot_allocations","shipment_id="+shipment+" AND lot_id=2 AND override_by IS NULL"));
    }

    @Test void managerOverrideIsAuditedAndFailedDeliveryRestoresSameLotOnce() throws Exception {
        lot(1,500,100,-2); lots.configure(500,true,30,manager); ready();
        long shipment=exports.export(first,manager,true,"Phê duyệt theo biên bản kiểm tra");
        assertEquals(1,count("inventory_lots","id=1 AND remaining_quantity=95"));
        assertEquals(1,count("shipment_lot_allocations","override_by="+manager.getId()+" AND override_reason IS NOT NULL"));
        assertEquals(1,count("audit_logs","action='LOT_EXPIRY_OVERRIDE'"));
        new ShipmentService().confirmFailure(shipment,"Đại lý không nhận",keeper);
        assertEquals(1,count("inventory_lots","id=1 AND remaining_quantity=100"));
        assertThrows(ServiceException.class,()->new ShipmentService().confirmFailure(shipment,"Lặp",keeper));
        assertEquals(1,count("inventory_lots","id=1 AND remaining_quantity=100"));
    }

    @Test void cannotOverrideAfterRoleOrSessionWasRevoked() throws Exception {
        lot(1,500,100,-2); lots.configure(500,true,30,manager); ready();
        sql("DELETE FROM user_roles WHERE user_id="+manager.getId());
        assertThrows(ForbiddenException.class,()->exports.export(first,manager,true,"Giấy duyệt"));
        assertEquals(0,count("shipments","1=1"));
    }

    @Test void twoConcurrentExportsDoNotConsumeLotTwice() throws Exception {
        lot(1,500,100,60); lots.configure(500,true,30,manager); ready();
        var pool=Executors.newFixedThreadPool(2);
        try {
            var tasks=List.of(pool.submit(()->{try { exports.export(first,keeper); return true; } catch (ServiceException e) { return false; }}),
                    pool.submit(()->{try { exports.export(first,keeper); return true; } catch (ServiceException e) { return false; }}));
            int success=0;
            for (var task:tasks) if (task.get(15,TimeUnit.SECONDS)) success++;
            assertEquals(1,success);
            assertEquals(1,count("inventory_lots","id=1 AND remaining_quantity=95"));
            assertEquals(1,count("shipment_lot_allocations","lot_id=1"));
        } finally { pool.shutdownNow(); }
    }
}
