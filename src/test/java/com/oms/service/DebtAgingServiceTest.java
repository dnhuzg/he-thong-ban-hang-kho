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

class DebtAgingServiceTest extends DatabaseTest {

    private final DebtAgingService service = new DebtAgingService();
    private User accountant;
    private final LocalDate today = LocalDate.now();

    @BeforeEach
    void fixture() throws Exception {
        accountant = user("ketoan", Role.ACCOUNTANT);
        sql("INSERT INTO customers(id,code,name,group_id) VALUES(900,'DL1','Đại lý Một',1),(901,'DL2','Đại lý Hai',1)");
        invoice(1, 900, "HD000001", 100000, 0, 10, -20);    // 0–30 ngày, chưa tới hạn
        invoice(2, 900, "HD000002", 200000, 50000, 45, 15); // 31–60 ngày, quá hạn
        invoice(3, 900, "HD000003", 300000, 0, 75, 45);    // 61–90 ngày, quá hạn
        invoice(4, 901, "HD000004", 400000, 0, 120, 90);   // trên 90 ngày, quá hạn
        invoice(5, 901, "HD000005", 50000, 50000, 5, -25); // đã thu đủ
    }

    /** Hoá đơn phát hành cách hôm nay ageDays ngày, hạn thanh toán cách hôm nay overdueDays ngày về trước. */
    private void invoice(long id, long customer, String code, long total, long paid, int ageDays, int overdueDays)
            throws Exception {
        sql("INSERT INTO invoices(id,code,customer_id,total_amount,paid_amount,issued_at,due_date,status) VALUES(" + id + ",'"
                + code + "'," + customer + "," + total + "," + paid + ",'" + today.minusDays(ageDays) + " 09:00:00','"
                + today.minusDays(overdueDays) + "','" + (paid >= total ? "PAID" : paid > 0 ? "PARTIAL" : "ISSUED") + "')");
    }

    private DebtAgingService.Report report(String keyword, Integer bucket, boolean overdue) throws Exception {
        return service.report(new DebtAgingService.Filter(today, keyword, bucket, overdue), accountant);
    }

    @Test
    void splitsOutstandingIntoFourAgeBucketsWithFooterTotals() throws Exception {
        DebtAgingService.Report report = report("", null, false);

        assertEquals(2, report.rows().size());
        DebtAgingService.Row first = report.rows().stream().filter(r -> "DL1".equals(r.getCode())).findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("600000").compareTo(first.getReceivable()));
        assertEquals(0, new BigDecimal("50000").compareTo(first.getPaid()));
        assertEquals(0, new BigDecimal("550000").compareTo(first.getOutstanding()));
        assertEquals(0, new BigDecimal("100000").compareTo(first.getBuckets().get(0)));
        assertEquals(0, new BigDecimal("150000").compareTo(first.getBuckets().get(1)));
        assertEquals(0, new BigDecimal("300000").compareTo(first.getBuckets().get(2)));
        assertEquals(0, new BigDecimal("450000").compareTo(first.getOverdue()));
        DebtAgingService.Row total = report.total();
        assertEquals(0, new BigDecimal("950000").compareTo(total.getOutstanding()));
        assertEquals(0, new BigDecimal("400000").compareTo(total.getBuckets().get(3)));
        assertEquals(0, total.getOutstanding().compareTo(
                total.getBuckets().stream().reduce(BigDecimal.ZERO, BigDecimal::add)));
    }

    @Test
    void filtersOverdueBucketAndKeyword() throws Exception {
        sql("INSERT INTO customers(id,code,name,group_id) VALUES(902,'DL3','Đại lý Ba',1)");
        invoice(6, 902, "HD000006", 70000, 0, 3, -27);

        assertEquals(3, report("", null, false).rows().size());
        assertEquals(2, report("", null, true).rows().size());
        assertEquals(1, report("", 3, false).rows().size());
        assertEquals("DL2", report("hd000004", null, false).rows().get(0).getCode());
    }

    @Test
    void exportsExcelAndChecksPermissions() throws Exception {
        byte[] file = service.exportXlsx(new DebtAgingService.Filter(null, null, null, false), accountant);
        assertTrue(file.length > 1000);

        User rep = user("nvkd", Role.SALES_REP);
        assertThrows(ForbiddenException.class, () -> service.report(new DebtAgingService.Filter(null, null, null, false), rep));
        assertThrows(ServiceException.class,
                () -> service.report(new DebtAgingService.Filter(today.plusDays(1), null, null, false), accountant));
    }
}
