package com.oms.service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Đọc / tạo tệp Excel cho các chức năng nhập hàng loạt (S2-01, S2-08). Cột được nhận theo tên tiêu đề
 * (không phân biệt hoa thường, bỏ dấu *), nên người dùng đổi thứ tự hay thêm cột ghi chú vẫn đọc được.
 */
public final class Excel {

    public static final long MAX_BYTES = 5L * 1024 * 1024;

    private Excel() {
    }

    /** Dòng dữ liệu: số dòng như Excel hiển thị và giá trị các ô theo tên cột đã chuẩn hoá. */
    public record Line(int number, Map<String, String> cells) {
        public String get(String column) {
            return cells.getOrDefault(column, "");
        }
    }

    public record Table(List<String> columns, List<Line> lines) {
        public boolean has(String column) {
            return columns.contains(column);
        }
    }

    /** Tên cột dùng để so khớp: "Mã SKU *" → "mã sku". */
    public static String columnKey(String header) {
        return header.replace("*", "").trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    /**
     * Đọc trang tính đầu tiên: dòng có dữ liệu đầu tiên là tiêu đề, bỏ qua dòng trống.
     * Tệp hỏng, sai định dạng, thiếu cột bắt buộc hoặc quá maxLines dòng thì báo lỗi cho cả tệp.
     */
    public static Table read(byte[] data, int maxLines, List<String> requiredColumns) {
        if (data == null || data.length == 0) {
            throw new ServiceException("Vui lòng chọn tệp Excel.");
        }
        if (data.length > MAX_BYTES) {
            throw new ServiceException("Tệp tối đa 5 MB. Hãy chia nhỏ tệp rồi nhập nhiều lần.");
        }
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(data))) {
            Sheet sheet = workbook.getSheetAt(0);
            List<String> columns = null;
            Map<Integer, String> keyByIndex = new HashMap<>();
            List<Line> lines = new ArrayList<>();
            for (Row row : sheet) {
                if (columns == null) {
                    if (isBlank(row)) {
                        continue;
                    }
                    columns = new ArrayList<>();
                    for (Cell cell : row) {
                        String key = columnKey(text(cell));
                        if (!key.isEmpty()) {
                            keyByIndex.put(cell.getColumnIndex(), key);
                            columns.add(key);
                        }
                    }
                    continue;
                }
                if (isBlank(row)) {
                    continue;
                }
                if (lines.size() == maxLines) {
                    throw new ServiceException("Tệp có hơn " + maxLines + " dòng dữ liệu. Hãy chia nhỏ tệp rồi nhập nhiều lần.");
                }
                Map<String, String> cells = new HashMap<>();
                keyByIndex.forEach((index, key) -> cells.put(key, text(row.getCell(index))));
                lines.add(new Line(row.getRowNum() + 1, cells));
            }
            if (columns == null) {
                throw new ServiceException("Tệp không có dữ liệu.");
            }
            for (String required : requiredColumns) {
                if (!columns.contains(columnKey(required))) {
                    throw new ServiceException("Tệp thiếu cột \"" + required + "\". Hãy dùng tệp mẫu.");
                }
            }
            return new Table(columns, lines);
        } catch (IOException | RuntimeException e) {
            if (e instanceof ServiceException se) {
                throw se;
            }
            throw new ServiceException("Không đọc được tệp. Hãy dùng tệp Excel (.xlsx) theo mẫu.");
        }
    }

    /**
     * Tệp mẫu: trang "Dữ liệu" có dòng tiêu đề (cột bắt buộc ghi dấu *) và các dòng ví dụ,
     * trang "Hướng dẫn" liệt kê quy tắc. Các cột để dạng chữ để Excel không tự bỏ số 0 đầu (SKU, số điện thoại).
     */
    public static byte[] template(List<String> headers, List<List<String>> examples, List<String> guide) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle textStyle = workbook.createCellStyle();
            textStyle.setDataFormat(workbook.createDataFormat().getFormat("@"));
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.cloneStyleFrom(textStyle);
            Font bold = workbook.createFont();
            bold.setBold(true);
            headerStyle.setFont(bold);
            headerStyle.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Sheet data = workbook.createSheet("Dữ liệu");
            Row header = data.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
                data.setDefaultColumnStyle(i, textStyle);
                int width = headers.get(i).length();
                for (List<String> example : examples) {
                    width = Math.max(width, example.get(i).length());
                }
                data.setColumnWidth(i, Math.min(60, width + 4) * 256);
            }
            for (int r = 0; r < examples.size(); r++) {
                Row row = data.createRow(r + 1);
                for (int i = 0; i < headers.size(); i++) {
                    Cell cell = row.createCell(i);
                    cell.setCellValue(examples.get(r).get(i));
                    cell.setCellStyle(textStyle);
                }
            }
            data.createFreezePane(0, 1);

            Sheet help = workbook.createSheet("Hướng dẫn");
            help.setColumnWidth(0, 120 * 256);
            for (int r = 0; r < guide.size(); r++) {
                help.createRow(r).createCell(0).setCellValue(guide.get(r));
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Không tạo được tệp mẫu", e);
        }
    }

    private static boolean isBlank(Row row) {
        for (Cell cell : row) {
            if (!text(cell).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** Giá trị ô dạng chữ: số không kèm ".0" hay dạng khoa học, ngày dạng yyyy-MM-dd, công thức lấy kết quả đã tính. */
    static String text(Cell cell) {
        if (cell == null) {
            return "";
        }
        CellType type = cell.getCellType() == CellType.FORMULA ? cell.getCachedFormulaResultType() : cell.getCellType();
        return switch (type) {
            case STRING -> cell.getStringCellValue().replace(' ', ' ').trim();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getLocalDateTimeCellValue().toLocalDate().toString()
                    : BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
            case BOOLEAN -> new DataFormatter().formatCellValue(cell);
            default -> "";
        };
    }
}
