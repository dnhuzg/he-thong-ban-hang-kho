
package com.oms.service;

import com.oms.model.Role;
import com.oms.model.User;
import com.oms.support.DatabaseTest;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DebtAgingServiceTest extends DatabaseTest {

    private final DebtAgingService service = new DebtAgingService();
    private User accountant;
    private final LocalDate today = LocalDate.now();

    @BeforeEach
    void fixture() throws Exception {
        accountant = user("ketoan", Role.ACCOUNTANT);

        sql("INSERT INTO customers(id,code,name,group_id) VALUES"
                + "(900,'DL1','Đại lý Một',1),"
                + "(901,'DL2','Đại lý Hai',1)");

        invoice(1, 900, "HD000001", 100000, 0, 10, -20);
        invoice(2, 900, "HD000002", 200000, 50000, 45, 15);
        invoice(3, 900, "HD000003", 300000, 0, 75, 45);
        invoice(4, 901, "HD000004", 400000, 0, 120, 90);
        invoice(5, 901, "HD000005", 50000, 50000, 5, -25);
    }

    private void invoice(
            long id,
            long customer,
            String code,
            long total,
            long paid,
            int ageDays,
            int overdueDays
    ) throws Exception {
        sql("INSERT INTO invoices(id,code,customer_id,total_amount,"
                + "paid_amount,issued_at,due_date,status) VALUES("
                + id + ",'"
                + code + "',"
                + customer + ","
                + total + ","
                + paid + ",'"
                + today.minusDays(ageDays)
                + " 09:00:00','"
                + today.minusDays(overdueDays)
                + "','"
                + (paid >= total ? "PAID"
                        : paid > 0 ? "PARTIAL" : "ISSUED")
                + "')");
    }

    private DebtAgingService.Report report(
            String keyword,
            Integer bucket,
            boolean overdue
    ) throws Exception {
        return service.report(
                new DebtAgingService.Filter(
                        today, keyword, bucket, overdue
                ),
                accountant
        );
    }

    private static void moneyEquals(
            String expected,
            BigDecimal actual
    ) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }

    @Test
    void splitsOutstandingIntoFourAgeBucketsWithFooterTotals()
            throws Exception {

        DebtAgingService.Report report = report("", null, false);

        assertEquals(2, report.rows().size());

        DebtAgingService.Row first = report.rows().stream()
                .filter(r -> "DL1".equals(r.getCode()))
                .findFirst()
                .orElseThrow();

        moneyEquals("600000", first.getReceivable());
        moneyEquals("50000", first.getPaid());
        moneyEquals("550000", first.getOutstanding());
        moneyEquals("100000", first.getBuckets().get(0));
        moneyEquals("150000", first.getBuckets().get(1));
        moneyEquals("300000", first.getBuckets().get(2));
        moneyEquals("450000", first.getOverdue());

        DebtAgingService.Row total = report.total();

        moneyEquals("950000", total.getOutstanding());
        moneyEquals("400000", total.getBuckets().get(3));

        assertEquals(
                0,
                total.getOutstanding().compareTo(
                        total.getBuckets().stream()
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                )
        );
    }

    @Test
    void filtersOverdueBucketAndKeyword() throws Exception {
        sql("INSERT INTO customers(id,code,name,group_id) "
                + "VALUES(902,'DL3','Đại lý Ba',1)");

        invoice(6, 902, "HD000006", 70000, 0, 3, -27);

        assertEquals(3, report("", null, false).rows().size());
        assertEquals(2, report("", null, true).rows().size());
        assertEquals(1, report("", 3, false).rows().size());

        assertEquals(
                "DL2",
                report("hd000004", null, false)
                        .rows().get(0).getCode()
        );
    }

    @Test
    void exportsExcelAndChecksPermissions() throws Exception {
        byte[] file = service.exportXlsx(
                new DebtAgingService.Filter(null, null, null, false),
                accountant
        );

        assertTrue(file.length > 1000);

        User rep = user("nvkd", Role.SALES_REP);

        assertThrows(
                ForbiddenException.class,
                () -> service.report(
                        new DebtAgingService.Filter(
                                null, null, null, false
                        ),
                        rep
                )
        );

        assertThrows(
                ServiceException.class,
                () -> service.report(
                        new DebtAgingService.Filter(
                                today.plusDays(1),
                                null,
                                null,
                                false
                        ),
                        accountant
                )
        );
    }

    @Test
    void rejectsInvalidBucket() {
        assertThrows(
                ServiceException.class,
                () -> service.report(
                        new DebtAgingService.Filter(
                                today, "", -1, false
                        ),
                        accountant
                )
        );

        assertThrows(
                ServiceException.class,
                () -> service.report(
                        new DebtAgingService.Filter(
                                today, "", 4, false
                        ),
                        accountant
                )
        );
    }

    @Test
    void historicalReportCountsOnlyPaymentsThroughAsOfDate()
            throws Exception {

        // Ngày đối chiếu: 20 ngày trước.
        // Hóa đơn HD000002 đã phát hành trước ngày đó.
        LocalDate asOf = today.minusDays(20);

        // Tạo hai phiếu thu: một trước và một sau ngày đối chiếu.
        sql("INSERT INTO payments "
                + "(id,code,customer_id,amount,method,paid_on,"
                + "collected_by,created_by) VALUES "
                + "(101,'PT00101',900,20000,'CASH','"
                + today.minusDays(25) + "',"
                + accountant.getId() + ","
                + accountant.getId() + "),"
                + "(102,'PT00102',900,30000,'CASH','"
                + today.minusDays(5) + "',"
                + accountant.getId() + ","
                + accountant.getId() + ")");

        sql("INSERT INTO payment_allocations "
                + "(payment_id,invoice_id,amount) VALUES "
                + "(101,2,20000),(102,2,30000)");

        DebtAgingService.Report historical = service.report(
                new DebtAgingService.Filter(
                        asOf, "HD000002", null, false
                ),
                accountant
        );

        assertEquals(1, historical.rows().size());

        DebtAgingService.Row row = historical.rows().get(0);

        moneyEquals("200000", row.getReceivable());
        moneyEquals("20000", row.getPaid());
        moneyEquals("180000", row.getOutstanding());

        // Ngày hiện tại sử dụng số đã thu trên hóa đơn.
        DebtAgingService.Row current =
                report("HD000002", null, false).rows().get(0);

        moneyEquals("50000", current.getPaid());
        moneyEquals("150000", current.getOutstanding());
    }

    @Test
    void exportedExcelIncludesCorrectTotals() throws Exception {
        byte[] file = service.exportXlsx(
                new DebtAgingService.Filter(
                        today, "", null, false
                ),
                accountant
        );

        try (Workbook workbook = new XSSFWorkbook(
                new ByteArrayInputStream(file)
        )) {
            var sheet = workbook.getSheet("Tuổi nợ");

            assertNotNull(sheet);
            assertEquals("Mã đại lý",
                    sheet.getRow(2).getCell(0).getStringCellValue());

            // Hai đại lý ở dòng 3, 4; tổng cộng ở dòng 5.
            var totalRow = sheet.getRow(5);

            assertEquals("Tổng cộng",
                    totalRow.getCell(1).getStringCellValue());

            assertEquals(
                    950000,
                    totalRow.getCell(4).getNumericCellValue(),
                    0.01
            );
        }
    }
}
