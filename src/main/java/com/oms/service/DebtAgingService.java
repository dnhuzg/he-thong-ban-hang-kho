package com.oms.service;

import com.oms.config.Db;
import com.oms.dao.DebtAgingDao;
import com.oms.model.User;
import com.oms.security.PermissionPolicy;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Sổ công nợ theo đại lý (GHTK-69) và báo cáo rủi ro công nợ theo tuổi nợ (GHTK-84).
 * Số liệu báo cáo tuổi nợ/nhân viên/rủi ro được tổng hợp từ SQL, không dùng dữ liệu hardcode trên trình duyệt.
 */
public class DebtAgingService {

    public static final String[] BUCKETS = {"0–30 ngày", "31–60 ngày", "61–90 ngày", "Trên 90 ngày"};

    public record Filter(LocalDate asOf, String keyword, Integer bucket, boolean overdueOnly) {
        public LocalDate getAsOf() { return asOf; }
        public String getKeyword() { return keyword; }
        public Integer getBucket() { return bucket; }
        public boolean isOverdueOnly() { return overdueOnly; }
    }

    /** Một dòng của sổ: một đại lý, bốn nhóm tuổi chỉ cộng phần còn nợ. */
    public static final class Row {
        private final long customerId;
        private final String code;
        private final String name;
        private final Long salesRepId;
        private final String salesRepName;
        private final Set<Long> invoiceIds = new LinkedHashSet<>();
        private BigDecimal receivable = BigDecimal.ZERO;
        private BigDecimal paid = BigDecimal.ZERO;
        private BigDecimal overdue = BigDecimal.ZERO;
        private final BigDecimal[] buckets = {BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
        private final BigDecimal[] overdueBuckets = {BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
        private int openInvoices;

        Row(long customerId, String code, String name, Long salesRepId, String salesRepName) {
            this.customerId = customerId;
            this.code = code;
            this.name = name;
            this.salesRepId = salesRepId;
            this.salesRepName = normalizedSalesRepName(salesRepName);
        }

        public long getCustomerId() { return customerId; }
        public String getCode() { return code; }
        public String getName() { return name; }
        public Long getSalesRepId() { return salesRepId; }
        public String getSalesRepName() { return salesRepName; }
        public BigDecimal getReceivable() { return receivable; }
        public BigDecimal getPaid() { return paid; }
        public BigDecimal getOutstanding() { return receivable.subtract(paid); }
        public BigDecimal getOverdue() { return overdue; }
        public boolean isLate() { return overdue.signum() > 0; }
        public List<BigDecimal> getBuckets() { return List.of(buckets); }
        public List<BigDecimal> getOverdueBuckets() { return List.of(overdueBuckets); }
        public int getOpenInvoices() { return openInvoices; }

        void addInvoiceId(long invoiceId) { invoiceIds.add(invoiceId); }
        List<Long> invoiceIds() { return List.copyOf(invoiceIds); }
    }

    /** Tổng hợp SQL theo nhân viên đang phụ trách các đại lý. */
    public static final class OwnerSummary {
        private final Long salesRepId;
        private final String salesRepName;
        private final long overdueCustomerCount;
        private final BigDecimal overdueTotal;
        private final List<BigDecimal> ageBuckets;

        OwnerSummary(DebtAgingDao.OwnerAging row) {
            this.salesRepId = row.salesRepId();
            this.salesRepName = normalizedSalesRepName(row.salesRepName());
            this.overdueCustomerCount = row.overdueCustomers();
            this.overdueTotal = row.overdueTotal();
            this.ageBuckets = List.copyOf(row.getBuckets());
        }

        public Long getSalesRepId() { return salesRepId; }
        public String getSalesRepName() { return salesRepName; }
        public long getOverdueCustomerCount() { return overdueCustomerCount; }
        public BigDecimal getOverdueTotal() { return overdueTotal; }
        public List<BigDecimal> getAgeBuckets() { return ageBuckets; }
    }

    /** Đại lý có rủi ro cao nhất trong kết quả lọc, do DAO xếp hạng bằng số liệu SQL. */
    public static final class RiskCustomer {
        private final long customerId;
        private final String code;
        private final String name;
        private final String salesRepName;
        private final long overdueInvoiceCount;
        private final BigDecimal overdueTotal;
        private final List<BigDecimal> ageBuckets;
        private final long oldestOverdueDays;

        RiskCustomer(DebtAgingDao.RiskCustomer row) {
            this.customerId = row.customerId();
            this.code = row.customerCode();
            this.name = row.customerName();
            this.salesRepName = normalizedSalesRepName(row.salesRepName());
            this.overdueInvoiceCount = row.overdueInvoices();
            this.overdueTotal = row.overdueTotal();
            this.ageBuckets = List.copyOf(row.getBuckets());
            this.oldestOverdueDays = row.oldestOverdueDays();
        }

        public long getCustomerId() { return customerId; }
        public String getCode() { return code; }
        public String getName() { return name; }
        public String getSalesRepName() { return salesRepName; }
        public long getOverdueInvoiceCount() { return overdueInvoiceCount; }
        public BigDecimal getOverdueTotal() { return overdueTotal; }
        public List<BigDecimal> getAgeBuckets() { return ageBuckets; }
        public long getOldestOverdueDays() { return oldestOverdueDays; }
    }

    public record Report(Filter filter, List<Row> rows, Row total, List<BigDecimal> overdueAgeBuckets,
                         List<OwnerSummary> ownerSummaries, RiskCustomer highestRiskCustomer) {
        public Filter getFilter() { return filter; }
        public List<Row> getRows() { return rows; }
        public Row getTotal() { return total; }
        public String[] getBucketLabels() { return BUCKETS; }
        public List<BigDecimal> getOverdueAgeBuckets() { return overdueAgeBuckets; }
        public List<OwnerSummary> getOwnerSummaries() { return ownerSummaries; }
        public RiskCustomer getHighestRiskCustomer() { return highestRiskCustomer; }
    }

    public Report report(Filter filter, User viewer) throws SQLException {
        if (!PermissionPolicy.canManageCredit(viewer.getRoles())) {
            throw new ForbiddenException("Chỉ kế toán công nợ và quản lý kinh doanh được xem sổ công nợ.");
        }
        LocalDate asOf = filter.asOf() == null ? LocalDate.now() : filter.asOf();
        if (asOf.isAfter(LocalDate.now())) {
            throw new ServiceException("Ngày đối chiếu không được sau hôm nay.");
        }
        Filter applied = new Filter(asOf, filter.keyword() == null ? "" : filter.keyword().strip(), filter.bucket(),
                filter.overdueOnly());
        String keyword = applied.keyword().toLowerCase(Locale.ROOT);
        DebtAgingDao dao = new DebtAgingDao();
        List<DebtAgingDao.InvoiceLine> lines;
        try (Connection conn = Db.getConnection()) {
            lines = dao.lines(conn, asOf);
        }

        Map<Long, Row> byCustomer = new LinkedHashMap<>();
        for (DebtAgingDao.InvoiceLine line : lines) {
            if (!keyword.isEmpty() && !(line.customerCode() + " " + line.customerName() + " " + line.invoiceCode())
                    .toLowerCase(Locale.ROOT).contains(keyword)) {
                continue;
            }
            Row row = byCustomer.computeIfAbsent(line.customerId(), id -> new Row(id, line.customerCode(),
                    line.customerName(), line.salesRepId(), line.salesRepName()));
            row.addInvoiceId(line.invoiceId());
            row.receivable = row.receivable.add(line.total());
            row.paid = row.paid.add(line.paid());
            BigDecimal open = line.total().subtract(line.paid());
            if (open.signum() > 0) {
                row.openInvoices++;
                long age = ChronoUnit.DAYS.between(line.issuedOn(), asOf);
                int bucket = age <= 30 ? 0 : age <= 60 ? 1 : age <= 90 ? 2 : 3;
                row.buckets[bucket] = row.buckets[bucket].add(open);
                if (line.dueDate().isBefore(asOf)) {
                    row.overdue = row.overdue.add(open);
                    row.overdueBuckets[bucket] = row.overdueBuckets[bucket].add(open);
                }
            }
        }

        List<Row> rows = new ArrayList<>();
        Row total = new Row(0, "", "Tổng cộng", null, "");
        for (Row row : byCustomer.values()) {
            if (applied.overdueOnly() && !row.isLate()) continue;
            if (applied.bucket() != null && row.buckets[applied.bucket()].signum() == 0) continue;
            rows.add(row);
            total.receivable = total.receivable.add(row.receivable);
            total.paid = total.paid.add(row.paid);
            total.overdue = total.overdue.add(row.overdue);
            total.openInvoices += row.openInvoices;
            for (int i = 0; i < 4; i++) total.buckets[i] = total.buckets[i].add(row.buckets[i]);
        }

        List<Long> visibleInvoiceIds = rows.stream().flatMap(row -> row.invoiceIds().stream()).distinct().toList();
        DebtAgingDao.OverdueAgingTotals totals;
        List<OwnerSummary> owners = new ArrayList<>();
        RiskCustomer highestRisk;
        try (Connection conn = Db.getConnection()) {
            totals = dao.overdueAgingTotals(conn, asOf, visibleInvoiceIds);
            for (DebtAgingDao.OwnerAging owner : dao.overdueBySalesRep(conn, asOf, visibleInvoiceIds)) {
                owners.add(new OwnerSummary(owner));
            }
            DebtAgingDao.RiskCustomer risk = dao.highestRiskCustomer(conn, asOf, visibleInvoiceIds);
            highestRisk = risk == null ? null : new RiskCustomer(risk);
        }
        return new Report(applied, List.copyOf(rows), total, totals.getBuckets(), List.copyOf(owners), highestRisk);
    }

    public byte[] exportXlsx(Filter filter, User viewer) throws SQLException {
        Report report = report(filter, viewer);
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle header = workbook.createCellStyle();
            Font bold = workbook.createFont();
            bold.setBold(true);
            header.setFont(bold);
            CellStyle money = workbook.createCellStyle();
            money.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));
            CellStyle boldMoney = workbook.createCellStyle();
            boldMoney.cloneStyleFrom(money);
            boldMoney.setFont(bold);

            Sheet sheet = workbook.createSheet("Tuổi nợ");
            org.apache.poi.ss.usermodel.Row title = sheet.createRow(0);
            title.createCell(0).setCellValue("Sổ công nợ theo đại lý — đối chiếu ngày " + report.filter().asOf());
            title.getCell(0).setCellStyle(header);
            String[] headers = {"Mã đại lý", "Đại lý", "Phải thu", "Đã thu", "Còn nợ", BUCKETS[0], BUCKETS[1], BUCKETS[2],
                    BUCKETS[3], "Quá hạn"};
            writeHeaders(sheet.createRow(2), headers, header);
            int index = 3;
            for (Row row : report.rows()) write(sheet.createRow(index++), row, money);
            write(sheet.createRow(index), report.total(), boldMoney);
            autoSize(sheet, headers.length);

            Sheet overdueSheet = workbook.createSheet("Tổng hợp quá hạn");
            overdueSheet.createRow(0).createCell(0).setCellValue(
                    "Tuổi hoá đơn của các khoản quá hạn — đối chiếu ngày " + report.filter().asOf());
            writeHeaders(overdueSheet.createRow(2), new String[]{"Nhóm tuổi nợ", "Số dư quá hạn"}, header);
            for (int i = 0; i < BUCKETS.length; i++) {
                org.apache.poi.ss.usermodel.Row row = overdueSheet.createRow(i + 3);
                row.createCell(0).setCellValue(BUCKETS[i]);
                setMoney(row.createCell(1), report.overdueAgeBuckets().get(i), money);
            }
            org.apache.poi.ss.usermodel.Row overdueTotal = overdueSheet.createRow(7);
            overdueTotal.createCell(0).setCellValue("Tổng quá hạn");
            setMoney(overdueTotal.createCell(1), report.total().getOverdue(), boldMoney);
            autoSize(overdueSheet, 2);

            Sheet ownerSheet = workbook.createSheet("Theo nhân viên");
            ownerSheet.createRow(0).createCell(0).setCellValue("Công nợ quá hạn theo nhân viên phụ trách");
            String[] ownerHeaders = {"Nhân viên phụ trách", "Số đại lý quá hạn", BUCKETS[0], BUCKETS[1], BUCKETS[2],
                    BUCKETS[3], "Tổng quá hạn"};
            writeHeaders(ownerSheet.createRow(2), ownerHeaders, header);
            index = 3;
            for (OwnerSummary owner : report.ownerSummaries()) {
                org.apache.poi.ss.usermodel.Row row = ownerSheet.createRow(index++);
                row.createCell(0).setCellValue(owner.getSalesRepName());
                row.createCell(1).setCellValue(owner.getOverdueCustomerCount());
                for (int i = 0; i < 4; i++) setMoney(row.createCell(i + 2), owner.getAgeBuckets().get(i), money);
                setMoney(row.createCell(6), owner.getOverdueTotal(), money);
            }
            if (report.ownerSummaries().isEmpty()) ownerSheet.createRow(index).createCell(0).setCellValue("Không có khoản nợ quá hạn theo bộ lọc.");
            autoSize(ownerSheet, ownerHeaders.length);

            Sheet riskSheet = workbook.createSheet("Rủi ro cao nhất");
            riskSheet.createRow(0).createCell(0).setCellValue("Đại lý có rủi ro công nợ cao nhất trong bộ lọc");
            String[] riskHeaders = {"Mã đại lý", "Đại lý", "Nhân viên phụ trách", "Số hoá đơn quá hạn", "Tổng quá hạn",
                    BUCKETS[0], BUCKETS[1], BUCKETS[2], BUCKETS[3], "Ngày quá hạn lâu nhất"};
            writeHeaders(riskSheet.createRow(2), riskHeaders, header);
            if (report.highestRiskCustomer() == null) {
                riskSheet.createRow(3).createCell(0).setCellValue("Không có đại lý có công nợ quá hạn theo bộ lọc.");
            } else {
                RiskCustomer risk = report.highestRiskCustomer();
                org.apache.poi.ss.usermodel.Row row = riskSheet.createRow(3);
                row.createCell(0).setCellValue(risk.getCode());
                row.createCell(1).setCellValue(risk.getName());
                row.createCell(2).setCellValue(risk.getSalesRepName());
                row.createCell(3).setCellValue(risk.getOverdueInvoiceCount());
                setMoney(row.createCell(4), risk.getOverdueTotal(), money);
                for (int i = 0; i < 4; i++) setMoney(row.createCell(i + 5), risk.getAgeBuckets().get(i), money);
                row.createCell(9).setCellValue(risk.getOldestOverdueDays());
            }
            autoSize(riskSheet, riskHeaders.length);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Không tạo được tệp Excel sổ công nợ", e);
        }
    }

    private static void writeHeaders(org.apache.poi.ss.usermodel.Row row, String[] headers, CellStyle style) {
        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
    }

    private static void autoSize(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) sheet.autoSizeColumn(i);
    }

    private static void setMoney(Cell cell, BigDecimal value, CellStyle style) {
        cell.setCellValue(value == null ? 0 : value.doubleValue());
        cell.setCellStyle(style);
    }

    private static void write(org.apache.poi.ss.usermodel.Row excel, Row row, CellStyle money) {
        excel.createCell(0).setCellValue(row.getCode());
        excel.createCell(1).setCellValue(row.getName());
        BigDecimal[] values = {row.getReceivable(), row.getPaid(), row.getOutstanding(), row.buckets[0], row.buckets[1],
                row.buckets[2], row.buckets[3], row.getOverdue()};
        for (int i = 0; i < values.length; i++) setMoney(excel.createCell(i + 2), values[i], money);
    }

    private static String normalizedSalesRepName(String name) {
        return name == null || name.isBlank() ? "Chưa phân công" : name;
    }
}
