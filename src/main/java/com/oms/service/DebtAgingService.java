
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
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * S7-03: Sổ công nợ theo đại lý và phân tích tuổi nợ.
 *
 * Tuổi nợ tính từ ngày phát hành hóa đơn.
 * Quá hạn tính theo ngày đến hạn thanh toán.
 */
public class DebtAgingService {

    public static final String[] BUCKETS = {
            "0–30 ngày",
            "31–60 ngày",
            "61–90 ngày",
            "Trên 90 ngày"
    };

    public record Filter(
            LocalDate asOf,
            String keyword,
            Integer bucket,
            boolean overdueOnly
    ) {
        public LocalDate getAsOf() {
            return asOf;
        }

        public String getKeyword() {
            return keyword;
        }

        public Integer getBucket() {
            return bucket;
        }

        public boolean isOverdueOnly() {
            return overdueOnly;
        }
    }

    public static final class Row {

        private final String code;
        private final String name;

        private BigDecimal receivable = BigDecimal.ZERO;
        private BigDecimal paid = BigDecimal.ZERO;
        private BigDecimal overdue = BigDecimal.ZERO;

        private final BigDecimal[] buckets = {
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        };

        private int openInvoices;

        Row(String code, String name) {
            this.code = code;
            this.name = name;
        }

        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }

        public BigDecimal getReceivable() {
            return receivable;
        }

        public BigDecimal getPaid() {
            return paid;
        }

        public BigDecimal getOutstanding() {
            return receivable.subtract(paid);
        }

        public BigDecimal getOverdue() {
            return overdue;
        }

        public boolean isLate() {
            return overdue.signum() > 0;
        }

        public List<BigDecimal> getBuckets() {
            return List.of(buckets);
        }

        public int getOpenInvoices() {
            return openInvoices;
        }
    }

    public record Report(
            Filter filter,
            List<Row> rows,
            Row total
    ) {
        public Filter getFilter() {
            return filter;
        }

        public List<Row> getRows() {
            return rows;
        }

        public Row getTotal() {
            return total;
        }

        public String[] getBucketLabels() {
            return BUCKETS.clone();
        }
    }

    public Report report(Filter filter, User viewer)
            throws SQLException {

        if (!PermissionPolicy.canManageCredit(viewer.getRoles())) {
            throw new ForbiddenException(
                    "Chỉ kế toán công nợ và quản lý kinh doanh được xem sổ công nợ."
            );
        }

        if (filter == null) {
            throw new ServiceException("Bộ lọc không hợp lệ.");
        }

        LocalDate asOf = filter.asOf() == null
                ? LocalDate.now()
                : filter.asOf();

        if (asOf.isAfter(LocalDate.now())) {
            throw new ServiceException(
                    "Ngày đối chiếu không được sau hôm nay."
            );
        }

        Integer bucket = filter.bucket();

        if (bucket != null && (bucket < 0 || bucket >= BUCKETS.length)) {
            throw new ServiceException(
                    "Nhóm tuổi nợ không hợp lệ."
            );
        }

        String searchKeyword = filter.keyword() == null
                ? ""
                : filter.keyword().strip();

        Filter applied = new Filter(
                asOf,
                searchKeyword,
                bucket,
                filter.overdueOnly()
        );

        String keyword = searchKeyword.toLowerCase(Locale.ROOT);

        List<DebtAgingDao.InvoiceLine> lines;

        try (Connection conn = Db.getConnection()) {
            lines = new DebtAgingDao().lines(conn, asOf);
        }

        Map<Long, Row> byCustomer = new LinkedHashMap<>();

        for (DebtAgingDao.InvoiceLine line : lines) {

            String searchable = (
                    line.customerCode() + " "
                            + line.customerName() + " "
                            + line.invoiceCode()
            ).toLowerCase(Locale.ROOT);

            if (!keyword.isEmpty() && !searchable.contains(keyword)) {
                continue;
            }

            Row row = byCustomer.computeIfAbsent(
                    line.customerId(),
                    id -> new Row(
                            line.customerCode(),
                            line.customerName()
                    )
            );

            BigDecimal invoiceTotal = line.total();
            BigDecimal invoicePaid = line.paid();

            if (invoiceTotal == null) {
                invoiceTotal = BigDecimal.ZERO;
            }

            if (invoicePaid == null) {
                invoicePaid = BigDecimal.ZERO;
            }

            row.receivable = row.receivable.add(invoiceTotal);
            row.paid = row.paid.add(invoicePaid);

            BigDecimal outstanding = invoiceTotal.subtract(invoicePaid);

            if (outstanding.signum() <= 0) {
                continue;
            }

            row.openInvoices++;

            long age = ChronoUnit.DAYS.between(
                    line.issuedOn(),
                    asOf
            );

            int bucketIndex;

            if (age <= 30) {
                bucketIndex = 0;
            } else if (age <= 60) {
                bucketIndex = 1;
            } else if (age <= 90) {
                bucketIndex = 2;
            } else {
                bucketIndex = 3;
            }

            row.buckets[bucketIndex] =
                    row.buckets[bucketIndex].add(outstanding);

            if (line.dueDate() != null
                    && line.dueDate().isBefore(asOf)) {

                row.overdue = row.overdue.add(outstanding);
            }
        }

        List<Row> rows = new ArrayList<>();
        Row total = new Row("", "Tổng cộng");

        for (Row row : byCustomer.values()) {

            if (applied.overdueOnly() && !row.isLate()) {
                continue;
            }

            if (applied.bucket() != null
                    && row.buckets[applied.bucket()].signum() == 0) {
                continue;
            }

            rows.add(row);

            total.receivable = total.receivable.add(row.receivable);
            total.paid = total.paid.add(row.paid);
            total.overdue = total.overdue.add(row.overdue);
            total.openInvoices += row.openInvoices;

            for (int i = 0; i < BUCKETS.length; i++) {
                total.buckets[i] =
                        total.buckets[i].add(row.buckets[i]);
            }
        }

        return new Report(applied, rows, total);
    }

    public byte[] exportXlsx(Filter filter, User viewer)
            throws SQLException {

        Report report = report(filter, viewer);

        try (
                Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()
        ) {
            CellStyle header = workbook.createCellStyle();

            Font bold = workbook.createFont();
            bold.setBold(true);
            header.setFont(bold);

            CellStyle money = workbook.createCellStyle();
            money.setDataFormat(
                    workbook.createDataFormat().getFormat("#,##0")
            );

            CellStyle boldMoney = workbook.createCellStyle();
            boldMoney.cloneStyleFrom(money);
            boldMoney.setFont(bold);

            Sheet sheet = workbook.createSheet("Tuổi nợ");

            org.apache.poi.ss.usermodel.Row title = sheet.createRow(0);

            title.createCell(0).setCellValue(
                    "Sổ công nợ theo đại lý — đối chiếu ngày "
                            + report.filter().asOf()
            );

            title.getCell(0).setCellStyle(header);

            String[] headers = {
                    "Mã đại lý",
                    "Đại lý",
                    "Phải thu",
                    "Đã thu",
                    "Còn nợ",
                    BUCKETS[0],
                    BUCKETS[1],
                    BUCKETS[2],
                    BUCKETS[3],
                    "Quá hạn"
            };

            org.apache.poi.ss.usermodel.Row head = sheet.createRow(2);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = head.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(header);
            }

            int index = 3;

            for (Row row : report.rows()) {
                write(sheet.createRow(index++), row, money);
            }

            write(sheet.createRow(index), report.total(), boldMoney);

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Không tạo được tệp Excel sổ công nợ",
                    e
            );
        }
    }

    private static void write(
            org.apache.poi.ss.usermodel.Row excel,
            Row row,
            CellStyle money
    ) {
        excel.createCell(0).setCellValue(row.getCode());
        excel.createCell(1).setCellValue(row.getName());

        BigDecimal[] values = {
                row.getReceivable(),
                row.getPaid(),
                row.getOutstanding(),
                row.buckets[0],
                row.buckets[1],
                row.buckets[2],
                row.buckets[3],
                row.getOverdue()
        };

        for (int i = 0; i < values.length; i++) {
            Cell cell = excel.createCell(i + 2);
            cell.setCellValue(values[i].doubleValue());
            cell.setCellStyle(money);
        }
    }
}
