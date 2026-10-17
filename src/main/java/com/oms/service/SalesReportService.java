package com.oms.service;

import com.oms.config.Db;
import com.oms.dao.SalesReportDao;
import com.oms.dao.SalesReportDao.Dimension;
import com.oms.model.User;
import com.oms.security.PermissionPolicy;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Báo cáo doanh số nhiều chiều (S8-06): doanh số và sản lượng theo hoá đơn đã phát hành, so với kỳ liền trước
 * cùng độ dài. Biên lợi nhuận gộp chỉ tính cho người được xem giá vốn (QLKD, Admin).
 */
public class SalesReportService {

    private static final int MAX_DAYS = 366;

    private final SalesReportDao dao = new SalesReportDao();

    public record Filter(Dimension dimension, LocalDate from, LocalDate to) {
        public Dimension getDimension() { return dimension; }
        public LocalDate getFrom() { return from; }
        public LocalDate getTo() { return to; }
    }

    public static final class Row {
        private final String label;
        private BigDecimal quantity = BigDecimal.ZERO;
        private BigDecimal revenue = BigDecimal.ZERO;
        private BigDecimal cost = BigDecimal.ZERO;
        private BigDecimal previousRevenue = BigDecimal.ZERO;

        Row(String label) {
            this.label = label;
        }

        public String getLabel() { return label; }
        public BigDecimal getQuantity() { return quantity; }
        public BigDecimal getRevenue() { return revenue; }
        public BigDecimal getPreviousRevenue() { return previousRevenue; }

        /** Null khi kỳ trước không có doanh số (không so được). */
        public BigDecimal getChangePercent() {
            return previousRevenue.signum() == 0 ? null
                    : revenue.subtract(previousRevenue).multiply(BigDecimal.valueOf(100))
                            .divide(previousRevenue, 1, RoundingMode.HALF_UP);
        }

        /** Null khi chưa có doanh số. */
        public BigDecimal getMarginPercent() {
            return revenue.signum() == 0 ? null
                    : revenue.subtract(cost).multiply(BigDecimal.valueOf(100)).divide(revenue, 1, RoundingMode.HALF_UP);
        }

        void add(SalesReportDao.Line line) {
            quantity = quantity.add(line.quantity());
            revenue = revenue.add(line.revenue());
            cost = cost.add(line.cost());
        }
    }

    public record Report(Filter filter, LocalDate previousFrom, LocalDate previousTo, List<Row> rows, Row total,
                         boolean showMargin) {
        public Filter getFilter() { return filter; }
        public LocalDate getPreviousFrom() { return previousFrom; }
        public LocalDate getPreviousTo() { return previousTo; }
        public List<Row> getRows() { return rows; }
        public Row getTotal() { return total; }
        public boolean isShowMargin() { return showMargin; }
    }

    /** Mặc định từ đầu tháng tới hôm nay, nhóm theo nhân viên. */
    public static Filter filter(String dimension, LocalDate from, LocalDate to) {
        Dimension dim;
        try {
            dim = dimension == null || dimension.isBlank() ? Dimension.EMPLOYEE : Dimension.valueOf(dimension);
        } catch (IllegalArgumentException e) {
            throw new ServiceException("Chiều phân tích không hợp lệ.");
        }
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.withDayOfMonth(1) : from;
        if (start.isAfter(end)) {
            throw new ServiceException("Ngày bắt đầu không được sau ngày kết thúc.");
        }
        if (ChronoUnit.DAYS.between(start, end) >= MAX_DAYS) {
            throw new ServiceException("Chỉ xem được tối đa " + MAX_DAYS + " ngày mỗi lần.");
        }
        return new Filter(dim, start, end);
    }

    public Report report(Filter filter, User viewer) throws SQLException {
        long days = ChronoUnit.DAYS.between(filter.from(), filter.to()) + 1;
        LocalDate previousTo = filter.from().minusDays(1);
        LocalDate previousFrom = previousTo.minusDays(days - 1);
        Map<String, Row> rows = new LinkedHashMap<>();
        Map<String, BigDecimal> previous = new HashMap<>();
        Map<String, String> previousLabels = new HashMap<>();
        try (Connection conn = Db.getConnection()) {
            for (SalesReportDao.Line line : dao.totals(conn, filter.dimension(), filter.from(), filter.to())) {
                rows.computeIfAbsent(line.key(), k -> new Row(line.label())).add(line);
            }
            for (SalesReportDao.Line line : dao.totals(conn, filter.dimension(), previousFrom, previousTo)) {
                previous.put(line.key(), line.revenue());
                previousLabels.put(line.key(), line.label());
            }
        }
        // Dòng chỉ có doanh số kỳ trước vẫn hiện, để thấy được mặt hàng / đại lý đã ngừng mua
        previous.forEach((key, value) -> rows.computeIfAbsent(key, k -> new Row(previousLabels.get(k))).previousRevenue = value);
        Row total = new Row("Tổng cộng");
        for (Row row : rows.values()) {
            total.quantity = total.quantity.add(row.quantity);
            total.revenue = total.revenue.add(row.revenue);
            total.cost = total.cost.add(row.cost);
            total.previousRevenue = total.previousRevenue.add(row.previousRevenue);
        }
        boolean showMargin = PermissionPolicy.canViewCostPrice(viewer.getRoles());
        if (!showMargin) {
            rows.values().forEach(r -> r.cost = BigDecimal.ZERO);
            total.cost = BigDecimal.ZERO;
        }
        return new Report(filter, previousFrom, previousTo, new ArrayList<>(rows.values()), total, showMargin);
    }

    public byte[] exportXlsx(Filter filter, User viewer) throws SQLException {
        Report report = report(filter, viewer);
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle header = workbook.createCellStyle();
            Font bold = workbook.createFont();
            bold.setBold(true);
            header.setFont(bold);
            CellStyle number = workbook.createCellStyle();
            number.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));
            CellStyle percent = workbook.createCellStyle();
            percent.setDataFormat(workbook.createDataFormat().getFormat("0.0"));

            Sheet sheet = workbook.createSheet("Doanh số");
            sheet.createRow(0).createCell(0).setCellValue("Doanh số theo " + filter.dimension().getLabel().toLowerCase()
                    + " từ " + filter.from() + " đến " + filter.to() + " (cùng kỳ trước: " + report.previousFrom()
                    + " đến " + report.previousTo() + ")");
            sheet.getRow(0).getCell(0).setCellStyle(header);
            List<String> headers = new ArrayList<>(List.of(filter.dimension().getLabel(), "Doanh số", "Sản lượng",
                    "Doanh số cùng kỳ", "Chênh lệch (%)"));
            if (report.showMargin()) {
                headers.add("Biên LN gộp (%)");
            }
            org.apache.poi.ss.usermodel.Row head = sheet.createRow(2);
            for (int i = 0; i < headers.size(); i++) {
                head.createCell(i).setCellValue(headers.get(i));
                head.getCell(i).setCellStyle(header);
            }
            int index = 3;
            for (Row row : report.rows()) {
                write(sheet.createRow(index++), row, report.showMargin(), number, percent);
            }
            write(sheet.createRow(index), report.total(), report.showMargin(), number, percent);
            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Không tạo được tệp Excel", e);
        }
    }

    private static void write(org.apache.poi.ss.usermodel.Row target, Row row, boolean showMargin, CellStyle number,
                              CellStyle percent) {
        target.createCell(0).setCellValue(row.getLabel());
        BigDecimal[] values = {row.getRevenue(), row.getQuantity(), row.getPreviousRevenue()};
        for (int i = 0; i < values.length; i++) {
            target.createCell(i + 1).setCellValue(values[i].doubleValue());
            target.getCell(i + 1).setCellStyle(number);
        }
        setPercent(target, 4, row.getChangePercent(), percent);
        if (showMargin) {
            setPercent(target, 5, row.getMarginPercent(), percent);
        }
    }

    private static void setPercent(org.apache.poi.ss.usermodel.Row target, int column, BigDecimal value, CellStyle style) {
        if (value != null) {
            target.createCell(column).setCellValue(value.doubleValue());
            target.getCell(column).setCellStyle(style);
        }
    }
}
