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
import static org.junit.jupiter.api.Assertions.assertTrue;

class StockCardServiceTest extends DatabaseTest {

    private final StockCardService service = new StockCardService();
    private final LocalDate today = LocalDate.now();

    @BeforeEach
    void fixture() throws Exception {
        User keeper = user("kho", Role.WAREHOUSE);
        sql("INSERT INTO products(id,sku,name,category_id,base_unit_id) VALUES(500,'COCA','Coca lon',3,1)");
        sql("INSERT INTO product_units VALUES(500,2,24)");
        sql("INSERT INTO suppliers(id,code,name) VALUES(1,'NCC1','Nhà cung cấp')");
        sql("INSERT INTO goods_receipts(id,code,supplier_id,document_no,received_date,warehouse_id,status,created_by) VALUES(7,'PN000007',1,'HD-1','"
                + today.minusDays(40) + "',10,'CONFIRMED'," + keeper.getId() + ")");
        // 40 ngày trước nhập 2 thùng (48 lon); 10 ngày trước xuất 30 lon; hôm nay hàng giao thất bại trả về 5 lon
        movement(1, "GOODS_RECEIPT", "GOODS_RECEIPT", 7, 2, "2", 40, keeper);
        movement(2, "SHIPMENT_OUT", "SHIPMENT", 9, 1, "30", 10, keeper);
        movement(3, "SHIPMENT_RETURN", "SHIPMENT", 9, 1, "5", 0, keeper);
        // Tồn có sẵn 100 lon trước khi có bút toán: hiện tại 100 + 48 − 30 + 5 = 123
        sql("INSERT INTO warehouse_inventory VALUES(10,500,123,0)");
    }

    private void movement(long id, String type, String refType, long refId, long unit, String qty, int daysAgo, User actor)
            throws Exception {
        sql("INSERT INTO inventory_movements(id,warehouse_id,product_id,unit_id,quantity,movement_type,reference_type,reference_id,created_by,created_at) VALUES("
                + id + ",10,500," + unit + "," + qty + ",'" + type + "','" + refType + "'," + refId + "," + actor.getId()
                + ",'" + today.minusDays(daysAgo) + " 10:00:00')");
    }

    private StockCardService.Card card(LocalDate from, LocalDate to) throws Exception {
        return service.card(new StockCardService.Filter(500L, 10L, from, to));
    }

    @Test
    void balancesAfterEachMovementMatchCurrentStock() throws Exception {
        StockCardService.Card all = card(today.minusDays(60), today);

        assertEquals(3, all.lines().size());
        assertEquals(0, new BigDecimal("100").compareTo(all.opening()));
        assertEquals(0, new BigDecimal("48").compareTo(all.lines().get(0).increase()));
        assertEquals(0, new BigDecimal("148").compareTo(all.lines().get(0).balance()));
        assertEquals(0, new BigDecimal("30").compareTo(all.lines().get(1).decrease()));
        assertEquals(0, new BigDecimal("118").compareTo(all.lines().get(1).balance()));
        assertEquals(0, new BigDecimal("123").compareTo(all.closing()));
        assertEquals("PN000007", all.lines().get(0).documentCode());
        assertEquals("/inventory/receipts?id=7", all.lines().get(0).documentUrl());
    }

    @Test
    void periodFilterUsesOpeningFromEarlierMovements() throws Exception {
        StockCardService.Card recent = card(today.minusDays(15), today.minusDays(1));

        assertEquals(1, recent.lines().size());
        assertEquals(0, new BigDecimal("148").compareTo(recent.opening()));
        assertEquals(0, new BigDecimal("118").compareTo(recent.closing()));
        assertThrows(ServiceException.class, () -> card(today, today.minusDays(1)));
        assertTrue(service.card(new StockCardService.Filter(null, null, null, null)).lines().isEmpty());
    }
}
