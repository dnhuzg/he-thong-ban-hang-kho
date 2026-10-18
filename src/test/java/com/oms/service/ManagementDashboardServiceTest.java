package com.oms.service;

import com.oms.support.DatabaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ManagementDashboardServiceTest extends DatabaseTest {

    private final ManagementDashboardService service = new ManagementDashboardService();
    private final LocalDate today = LocalDate.of(2026, 10, 15);

    @BeforeEach
    void fixture() throws Exception {
        sql("INSERT INTO products(id,sku,name,status) VALUES(901,'A','Hàng A','ACTIVE'),(902,'B','Hàng B','ACTIVE'),"
                + "(903,'C','Hàng C','INACTIVE')");
        sql("INSERT INTO warehouse_inventory(warehouse_id,product_id,on_hand,reserved) VALUES(10,901,5,5),(20,901,8,0),"
                + "(10,902,9,1),(10,903,0,0)");
        sql("INSERT INTO customers(id,code,name,group_id,area_id) VALUES(901,'DL1','Đại lý Một',1,30),(902,'DL2','Đại lý Hai',1,NULL)");
        invoice(1, 901, "2026-10-15", "2026-11-14", 300000, 0, 0, "ISSUED");      // hôm nay
        invoice(2, 902, "2026-10-03", "2026-11-02", 200000, 0, 0, "ISSUED");      // đầu tháng
        invoice(3, 901, "2026-08-20", "2026-09-19", 500000, 100000, 50000, "PARTIAL"); // quá hạn 26 ngày, còn 350.000
        invoice(4, 902, "2026-09-01", "2026-10-01", 80000, 0, 0, "ISSUED");       // quá hạn 14 ngày
        invoice(5, 902, "2026-10-10", "2026-10-12", 999000, 0, 0, "CANCELLED");   // huỷ: không tính
        for (String status : new String[] {"PENDING_APPROVAL", "PENDING_APPROVAL", "APPROVED", "PICKING", "SHIPPED", "DRAFT"}) {
            sql("INSERT INTO orders(customer_id,status) VALUES(901,'" + status + "')");
        }
    }

    private void invoice(long id, long customer, String issued, String due, long total, long paid, long credited,
                         String status) throws Exception {
        sql("INSERT INTO invoices(id,code,customer_id,total_amount,paid_amount,credited_amount,issued_at,due_date,status) VALUES("
                + id + ",'HD" + id + "'," + customer + "," + total + "," + paid + "," + credited + ",'" + issued
                + " 09:00:00','" + due + "','" + status + "')");
    }

    private static void money(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), expected + " ≠ " + actual);
    }

    @Test
    void kpisComeFromInvoicesOrdersAndStock() throws Exception {
        ManagementDashboardService.Dashboard d = service.load(today);

        money("300000", d.today());
        money("500000", d.month());                 // hoá đơn huỷ và hoá đơn tháng trước không tính
        assertEquals(2, d.pendingApproval());
        assertEquals(2, d.pendingDispatch());        // đã duyệt + đang soạn
        assertEquals(1, d.outOfStockSkus());         // A hết khả dụng ở kho 10; C ngừng kinh doanh không tính
    }

    @Test
    void overdueListSortedByAmountAndNetOfPaymentsAndCredits() throws Exception {
        ManagementDashboardService.Dashboard d = service.load(today);

        assertEquals(2, d.overdue().size());
        assertEquals("DL1", d.overdue().get(0).code());
        money("350000", d.overdue().get(0).amount());
        assertEquals(26, d.overdue().get(0).days());
        assertEquals("Địa bàn A", d.overdue().get(0).area());
        assertEquals("Chưa có khu vực", d.overdue().get(1).area());
        money("430000", d.overdueTotal());
    }

    @Test
    void chartsCoverEveryDayOfTheMonthAndEachArea() throws Exception {
        ManagementDashboardService.Dashboard d = service.load(today);

        assertEquals(15, d.daily().size());
        money("200000", d.daily().get(LocalDate.of(2026, 10, 3)));
        money("0", d.daily().get(LocalDate.of(2026, 10, 4)));
        assertEquals("Địa bàn A|Chưa có khu vực", d.getAreaNames());
        assertEquals("300000.00,200000.00", d.getAreaValues());
    }
}
