package com.oms.service;

import com.oms.config.Db;
import com.oms.dao.InventoryReportDao;
import com.oms.model.InventoryLotAgeRow;
import com.oms.model.InventoryReport;
import com.oms.model.InventorySkuAgeRow;
import com.oms.model.InventoryValueRow;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InventoryReportService {

    public static final int SLOW_MOVING_THRESHOLD_DAYS = 30;

    private final InventoryReportDao dao = new InventoryReportDao();

    public InventoryReport report(Long warehouseId, Long categoryId) throws SQLException {
        try (Connection conn = Db.getConnection()) {
            List<InventoryValueRow> valueRows = dao.findValueRows(conn, warehouseId, categoryId);
            List<InventorySkuAgeRow> skuRows = calculateAgeRows(
                    dao.findLotAges(conn, warehouseId, categoryId));
            return new InventoryReport(valueRows, skuRows, SLOW_MOVING_THRESHOLD_DAYS);
        }
    }

    private List<InventorySkuAgeRow> calculateAgeRows(List<InventoryLotAgeRow> lots) {
        LocalDate today = LocalDate.now();
        Map<Long, AgeAccumulator> grouped = new LinkedHashMap<>();

        for (InventoryLotAgeRow lot : lots) {
            long ageDays = Math.max(0, ChronoUnit.DAYS.between(lot.receivedAt().toLocalDate(), today));
            AgeAccumulator acc = grouped.computeIfAbsent(lot.productId(),
                    id -> new AgeAccumulator(lot.productId(), lot.sku(), lot.productName()));
            acc.quantity = acc.quantity.add(lot.quantity());
            acc.weightedAge = acc.weightedAge.add(
                    lot.quantity().multiply(BigDecimal.valueOf(ageDays)));
        }

        List<InventorySkuAgeRow> rows = new ArrayList<>();
        for (AgeAccumulator acc : grouped.values()) {
            BigDecimal average = acc.quantity.signum() == 0
                    ? BigDecimal.ZERO
                    : acc.weightedAge.divide(acc.quantity, 2, RoundingMode.HALF_UP);
            rows.add(new InventorySkuAgeRow(
                    acc.productId,
                    acc.sku,
                    acc.productName,
                    acc.quantity,
                    average,
                    average.compareTo(BigDecimal.valueOf(SLOW_MOVING_THRESHOLD_DAYS)) >= 0));
        }

        rows.sort(Comparator.comparing(InventorySkuAgeRow::averageAgeDays).reversed()
                .thenComparing(InventorySkuAgeRow::sku));
        return rows;
    }

    public byte[] exportXlsx(Long warehouseId, Long categoryId) throws SQLException {
        InventoryReport report = report(warehouseId, categoryId);
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle headerStyle = workbook.createCellStyle();
            Font bold = workbook.createFont();
            bold.setBold(true);
            headerStyle.setFont(bold);

            CellStyle numberStyle = workbook.createCellStyle();
            numberStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));

            CellStyle integerStyle = workbook.createCellStyle();
            integerStyle.setDataFormat(workbook.createDataFormat().getFormat("0.00"));

            Sheet valueSheet = workbook.createSheet("\u0047\u0069\u00E1\u0020\u0074\u0072\u1ECB\u0020\u0074\u1ED3\u006E");
            writeHeaders(valueSheet.createRow(0), headerStyle,
                    "Kho", "NhÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³m hÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â ng", "SÃƒÆ’Ã‚Â¡Ãƒâ€šÃ‚Â»ÃƒÂ¢Ã¢â€šÂ¬Ã‹Å“ lÃƒÆ’Ã¢â‚¬Â Ãƒâ€šÃ‚Â°ÃƒÆ’Ã‚Â¡Ãƒâ€šÃ‚Â»Ãƒâ€šÃ‚Â£ng tÃƒÆ’Ã‚Â¡Ãƒâ€šÃ‚Â»ÃƒÂ¢Ã¢â€šÂ¬Ã…â€œn", "GiÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡ trÃƒÆ’Ã‚Â¡Ãƒâ€šÃ‚Â»ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¹ tÃƒÆ’Ã‚Â¡Ãƒâ€šÃ‚Â»ÃƒÂ¢Ã¢â€šÂ¬Ã…â€œn");
            int rowIndex = 1;
            for (InventoryValueRow row : report.valueRows()) {
                Row excelRow = valueSheet.createRow(rowIndex++);
                textCell(excelRow, 0, row.warehouseName());
                textCell(excelRow, 1, row.categoryName());
                numericCell(excelRow, 2, row.quantity(), numberStyle);
                numericCell(excelRow, 3, row.stockValue(), numberStyle);
            }
            autoSize(valueSheet, 4);

            Sheet ageSheet = workbook.createSheet("\u004E\u0067\u00E0\u0079\u0020\u0074\u1ED3\u006E\u0020\u0053\u004B\u0055");
            writeHeaders(ageSheet.createRow(0), headerStyle,
                    "SKU", "TÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Âªn sÃƒÆ’Ã‚Â¡Ãƒâ€šÃ‚ÂºÃƒâ€šÃ‚Â£n phÃƒÆ’Ã‚Â¡Ãƒâ€šÃ‚ÂºÃƒâ€šÃ‚Â©m", "SÃƒÆ’Ã‚Â¡Ãƒâ€šÃ‚Â»ÃƒÂ¢Ã¢â€šÂ¬Ã‹Å“ lÃƒÆ’Ã¢â‚¬Â Ãƒâ€šÃ‚Â°ÃƒÆ’Ã‚Â¡Ãƒâ€šÃ‚Â»Ãƒâ€šÃ‚Â£ng tÃƒÆ’Ã‚Â¡Ãƒâ€šÃ‚Â»ÃƒÂ¢Ã¢â€šÂ¬Ã…â€œn", "NgÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â y tÃƒÆ’Ã‚Â¡Ãƒâ€šÃ‚Â»ÃƒÂ¢Ã¢â€šÂ¬Ã…â€œn bÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¬nh quÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢n", "ChÃƒÆ’Ã‚Â¡Ãƒâ€šÃ‚ÂºÃƒâ€šÃ‚Â­m luÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢n chuyÃƒÆ’Ã‚Â¡Ãƒâ€šÃ‚Â»Ãƒâ€ Ã¢â‚¬â„¢n");
            rowIndex = 1;
            for (InventorySkuAgeRow row : report.skuAgeRows()) {
                Row excelRow = ageSheet.createRow(rowIndex++);
                textCell(excelRow, 0, row.sku());
                textCell(excelRow, 1, row.productName());
                numericCell(excelRow, 2, row.quantity(), numberStyle);
                numericCell(excelRow, 3, row.averageAgeDays(), integerStyle);
                textCell(excelRow, 4, row.slowMoving() ? "CÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³" : "KhÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â´ng");
            }
            autoSize(ageSheet, 5);

            Sheet info = workbook.createSheet("\u0054\u0068\u00F4\u006E\u0067\u0020\u0074\u0069\u006E");
            info.createRow(0).createCell(0).setCellValue("NgÃƒÆ’Ã¢â‚¬Â Ãƒâ€šÃ‚Â°ÃƒÆ’Ã‚Â¡Ãƒâ€šÃ‚Â»Ãƒâ€šÃ‚Â¡ng chÃƒÆ’Ã‚Â¡Ãƒâ€šÃ‚ÂºÃƒâ€šÃ‚Â­m luÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢n chuyÃƒÆ’Ã‚Â¡Ãƒâ€šÃ‚Â»Ãƒâ€ Ã¢â‚¬â„¢n (ngÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â y)");
            info.createRow(0).createCell(1).setCellValue(report.slowMovingThresholdDays());
            info.createRow(1).createCell(0).setCellValue("NgÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â y lÃƒÆ’Ã‚Â¡Ãƒâ€šÃ‚ÂºÃƒâ€šÃ‚Â­p bÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡o cÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡o");
            info.createRow(1).createCell(1).setCellValue(LocalDate.now().toString());
            autoSize(info, 2);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("KhÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â´ng tÃƒÆ’Ã‚Â¡Ãƒâ€šÃ‚ÂºÃƒâ€šÃ‚Â¡o ÃƒÆ’Ã¢â‚¬Å¾ÃƒÂ¢Ã¢â€šÂ¬Ã‹Å“ÃƒÆ’Ã¢â‚¬Â Ãƒâ€šÃ‚Â°ÃƒÆ’Ã‚Â¡Ãƒâ€šÃ‚Â»Ãƒâ€šÃ‚Â£c tÃƒÆ’Ã‚Â¡Ãƒâ€šÃ‚Â»ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¡p Excel bÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡o cÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡o tÃƒÆ’Ã‚Â¡Ãƒâ€šÃ‚Â»ÃƒÂ¢Ã¢â€šÂ¬Ã…â€œn kho.", e);
        }
    }

    private static void writeHeaders(Row row, CellStyle style, String... headers) {
        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
    }

    private static void textCell(Row row, int column, String value) {
        row.createCell(column).setCellValue(value == null ? "" : value);
    }

    private static void numericCell(Row row, int column, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value == null ? 0d : value.doubleValue());
        cell.setCellStyle(style);
    }

    private static void autoSize(Sheet sheet, int columns) {
        for (int i = 0; i < columns; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private static final class AgeAccumulator {
        private final long productId;
        private final String sku;
        private final String productName;
        private BigDecimal quantity = BigDecimal.ZERO;
        private BigDecimal weightedAge = BigDecimal.ZERO;

        private AgeAccumulator(long productId, String sku, String productName) {
            this.productId = productId;
            this.sku = sku;
            this.productName = productName;
        }
    }
}