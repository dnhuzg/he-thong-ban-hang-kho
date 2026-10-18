package com.oms.service;

import com.oms.dao.SalesReportDao.Dimension;
import com.oms.model.Role;
import com.oms.model.User;
import com.oms.support.DatabaseTest;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SalesReportServiceTest extends DatabaseTest {

    private final SalesReportService service = new SalesReportService();
    private final LocalDate to = LocalDate.of(2026, 10, 10);
    private final LocalDate from = LocalDate.of(2026, 10, 1);   // kỳ trước: 21/09 → 30/09
    private User manager;

    @BeforeEach
    void fixture() throws Exception {
        manager = user("qlkd", Role.SALES_MANAGER);
        User rep = user("nvkd_a", Role.SALES_REP);
        sql("INSERT INTO products(id,sku,name,category_id,base_unit_id,cost_price,status) VALUES"
                + "(901,'COCA','Coca lon',3,1,6000,'ACTIVE'),(902,'MI','Mì gói',4,3,3000,'ACTIVE')");
        sql("INSERT INTO customers(id,code,name,group_id,area_id,sales_rep_id) VALUES"
                + "(901,'DL1','Đại lý Một',1,30," + rep.getId() + "),(902,'DL2','Đại lý Hai',1,NULL,NULL)");
        invoice(1, 901, "2026-10-02", "ISSUED", 901, 100, 8000);   // kỳ này: DL1 Coca 800.000
        invoice(2, 902, "2026-10-10", "ISSUED", 902, 50, 4000);    // kỳ này, ngày cuối: DL2 Mì 200.000
        invoice(3, 901, "2026-09-25", "PAID", 901, 50, 8000);      // kỳ trước: DL1 400.000
        invoice(4, 902, "2026-10-05", "CANCELLED", 902, 999, 4000); // huỷ: không tính
        invoice(5, 901, "2026-10-11", "ISSUED", 901, 10, 8000);    // sau kỳ: không tính
    }

    private void invoice(long id, long customer, String day, String status, long product, int qty, int price)
            throws Exception {
        sql("INSERT INTO invoices(id,code,customer_id,total_amount,paid_amount,issued_at,due_date,status) VALUES(" + id
                + ",'HD" + id + "'," + customer + "," + qty * price + ",0,'" + day + " 09:00:00','" + day + "','" + status + "')");
        sql("INSERT INTO invoice_lines(invoice_id,product_id,quantity,unit_price,amount) VALUES(" + id + "," + product
                + "," + qty + "," + price + "," + qty * price + ")");
    }

    private SalesReportService.Report report(Dimension dimension, User viewer) throws Exception {
        return service.report(new SalesReportService.Filter(dimension, from, to), viewer);
    }

    private static void money(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), expected + " ≠ " + actual);
    }

    @Test
    void groupsByEmployeeAndComparesWithPreviousPeriodOfSameLength() throws Exception {
        SalesReportService.Report report = report(Dimension.EMPLOYEE, manager);

        assertEquals(LocalDate.of(2026, 9, 21), report.previousFrom());
        assertEquals(LocalDate.of(2026, 9, 30), report.previousTo());
        assertEquals(2, report.rows().size());
        SalesReportService.Row rep = report.rows().get(0);
        assertEquals("nvkd_a", rep.getLabel());
        money("800000", rep.getRevenue());
        money("100", rep.getQuantity());
        money("400000", rep.getPreviousRevenue());
        money("100.0", rep.getChangePercent());
        money("25.0", rep.getMarginPercent());          // (800.000 − 100 × 6.000) / 800.000
        SalesReportService.Row unassigned = report.rows().get(1);
        assertEquals("Chưa phân công", unassigned.getLabel());
        assertNull(unassigned.getChangePercent());       // kỳ trước không có doanh số
        money("1000000", report.total().getRevenue());   // không tính hoá đơn huỷ và hoá đơn ngoài kỳ
        money("400000", report.total().getPreviousRevenue());
    }

    @Test
    void groupsByRegionCustomerCategoryAndSku() throws Exception {
        assertEquals("Địa bàn A", report(Dimension.REGION, manager).rows().get(0).getLabel());
        assertEquals("Chưa có khu vực", report(Dimension.REGION, manager).rows().get(1).getLabel());
        assertEquals("DL1 – Đại lý Một", report(Dimension.CUSTOMER, manager).rows().get(0).getLabel());
        assertEquals("Nước ngọt có ga", report(Dimension.CATEGORY, manager).rows().get(0).getLabel());
        SalesReportService.Row mi = report(Dimension.SKU, manager).rows().stream()
                .filter(r -> r.getLabel().startsWith("MI")).findFirst().orElseThrow();
        money("200000", mi.getRevenue());
        money("25.0", mi.getMarginPercent());            // (200.000 − 50 × 3.000) / 200.000
    }

    @Test
    void hidesGrossMarginFromUsersWhoCannotSeeCostPrice() throws Exception {
        User accountant = user("ketoan", Role.ACCOUNTANT);
        SalesReportService.Report report = report(Dimension.SKU, accountant);

        assertFalse(report.showMargin());
        money("100.0", report.total().getMarginPercent()); // giá vốn bị bỏ ở service nên biên không lộ ra
        try (Workbook book = WorkbookFactory.create(new ByteArrayInputStream(
                service.exportXlsx(new SalesReportService.Filter(Dimension.SKU, from, to), accountant)))) {
            assertEquals(5, book.getSheetAt(0).getRow(2).getLastCellNum());
        }
    }

    @Test
    void exportsSameRowsAndTotalsAsTheScreen() throws Exception {
        try (Workbook book = WorkbookFactory.create(new ByteArrayInputStream(
                service.exportXlsx(new SalesReportService.Filter(Dimension.CUSTOMER, from, to), manager)))) {
            Sheet sheet = book.getSheetAt(0);
            assertEquals(6, sheet.getRow(2).getLastCellNum());
            assertEquals("DL1 – Đại lý Một", sheet.getRow(3).getCell(0).getStringCellValue());
            assertEquals("Tổng cộng", sheet.getRow(5).getCell(0).getStringCellValue());
            assertEquals(1000000, sheet.getRow(5).getCell(1).getNumericCellValue());
        }
    }

    @Test
    void rejectsInvalidFilters() {
        assertThrows(ServiceException.class, () -> SalesReportService.filter("XYZ", null, null));
        assertThrows(ServiceException.class, () -> SalesReportService.filter("SKU", to, from));
        assertThrows(ServiceException.class, () -> SalesReportService.filter("SKU", from.minusYears(2), to));
        SalesReportService.Filter defaults = SalesReportService.filter("", null, null);
        assertEquals(Dimension.EMPLOYEE, defaults.dimension());
        assertEquals(1, defaults.from().getDayOfMonth());
        assertTrue(!defaults.to().isBefore(defaults.from()));
    }
}
