package com.oms.service;

import com.oms.support.DatabaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StockLedgerServiceTest extends DatabaseTest {

    private final StockLedgerService service = new StockLedgerService();

    @BeforeEach
    void fixture() throws Exception {
        sql("INSERT INTO products(id,sku,name,category_id,base_unit_id) VALUES(500,'COCA','Coca lon',3,1),(501,'MI','Mì gói',4,3)");
        // Coca: kho A còn 20 khả dụng; kho B đã giữ chỗ hết. Mì: kho A hết hàng.
        sql("INSERT INTO warehouse_inventory VALUES(10,500,30,10),(20,500,5,5),(10,501,0,0)");
    }

    private StockLedgerService.Page page(String q, Long warehouse, Long category, String status) throws Exception {
        return service.page(new StockLedgerService.Filter(q, warehouse, category, status));
    }

    @Test
    void showsOnHandReservedAndAvailablePerSkuAndWarehouse() throws Exception {
        StockLedgerService.Page all = page("", null, null, null);

        assertEquals(3, all.lines().size());
        StockLedgerService.Line coca = all.lines().stream()
                .filter(l -> "COCA".equals(l.getSku()) && l.getWarehouseId() == 10).findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("30").compareTo(coca.getOnHand()));
        assertEquals(0, new BigDecimal("10").compareTo(coca.getReserved()));
        assertEquals(0, new BigDecimal("20").compareTo(coca.getAvailable()));
        assertEquals(1, all.availableCount());
        assertEquals(1, all.reservedOutCount());
        assertEquals(1, all.emptyCount());
    }

    @Test
    void filtersByWarehouseCategoryTreeStatusAndKeyword() throws Exception {
        assertEquals(2, page("", 10L, null, null).lines().size());
        // Nhóm 1 "Đồ uống" là cha của nhóm 3 nên lọc theo nhóm cha vẫn ra Coca
        assertEquals(2, page("", null, 1L, null).lines().size());
        assertEquals(1, page("", null, null, "RESERVED_OUT").lines().size());
        assertEquals(1, page("", null, null, "EMPTY").lines().size());
        assertEquals(1, page("Mì", null, null, null).lines().size());
        assertEquals(3, page("", null, null, "khong-hop-le").lines().size());
    }

    @Test
    void exportUsesFilterAndNamesFileWithWarehouseCodeAndDate() throws Exception {
        StockLedgerService.Export file = service.exportXlsx(new StockLedgerService.Filter("", 10L, null, null));
        assertEquals("ton-kho-KHO-A-" + LocalDate.now() + ".xlsx", file.fileName());
        assertTrue(file.content().length > 1000);
        assertEquals("ton-kho-tat-ca-kho-" + LocalDate.now() + ".xlsx",
                service.exportXlsx(new StockLedgerService.Filter(null, null, null, null)).fileName());
    }
}
