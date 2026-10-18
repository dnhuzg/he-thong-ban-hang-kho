package com.oms.service;

import com.oms.config.Db;
import com.oms.dao.AuditLogDao;
import com.oms.dao.LookupDao;
import com.oms.dao.StockLedgerDao;
import com.oms.model.Category;
import com.oms.model.NamedItem;
import com.oms.model.Role;
import com.oms.model.User;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Sổ tồn theo SKU và kho (S5-05): ba cột tồn thực tế, đang giữ chỗ, khả dụng; lọc theo kho, nhóm hàng
 * (gồm nhóm con), trạng thái tồn và tìm theo mã / tên. Xuất Excel (S8-08) theo đúng bộ lọc, tên tệp có mã kho và ngày.
 * Quyền xem (module INVENTORY) do AccessRules kiểm ở tầng web.
 */
public class StockLedgerService {

    public static final List<String> STATUSES = List.of("AVAILABLE", "RESERVED_OUT", "EMPTY", "LOW");
    private static final BigDecimal MAX_MINIMUM = new BigDecimal("1000000000");

    public record Filter(String keyword, Long warehouseId, Long categoryId, String status) {
        public String getKeyword() { return keyword; }
        public Long getWarehouseId() { return warehouseId; }
        public Long getCategoryId() { return categoryId; }
        public String getStatus() { return status; }
    }

    /** Một dòng tồn; khả dụng = thực tế − giữ chỗ. */
    public static final class Line {
        private final StockLedgerDao.Row row;

        Line(StockLedgerDao.Row row) {
            this.row = row;
        }

        public long getProductId() { return row.productId(); }
        public String getSku() { return row.sku(); }
        public String getProductName() { return row.productName(); }
        public String getCategoryName() { return row.categoryName(); }
        public String getBaseUnit() { return row.baseUnit(); }
        public long getWarehouseId() { return row.warehouseId(); }
        public String getWarehouseName() { return row.warehouseName(); }
        public BigDecimal getOnHand() { return plain(row.onHand()); }
        public BigDecimal getReserved() { return plain(row.reserved()); }
        public BigDecimal getAvailable() { return plain(row.onHand().subtract(row.reserved())); }
        public String getStatus() {
            return row.onHand().signum() <= 0 ? "EMPTY" : getAvailable().signum() <= 0 ? "RESERVED_OUT" : "AVAILABLE";
        }
        public BigDecimal getMinQuantity() { return row.minQuantity() == null ? null : plain(row.minQuantity()); }
        /** GHTK-57: chỉ cảnh báo khi đã khai báo ngưỡng; so với khả dụng vì phần giữ chỗ đã thuộc đơn khác. */
        public boolean isBelowMinimum() {
            return row.minQuantity() != null && row.onHand().subtract(row.reserved()).compareTo(row.minQuantity()) < 0;
        }
    }

    public record Page(Filter filter, List<Line> lines, List<NamedItem> warehouses, List<Category> categories,
                       long availableCount, long reservedOutCount, long emptyCount, long belowMinimumCount) {
        public Filter getFilter() { return filter; }
        public List<Line> getLines() { return lines; }
        public List<NamedItem> getWarehouses() { return warehouses; }
        public List<Category> getCategories() { return categories; }
        public long getAvailableCount() { return availableCount; }
        public long getReservedOutCount() { return reservedOutCount; }
        public long getEmptyCount() { return emptyCount; }
        public long getBelowMinimumCount() { return belowMinimumCount; }
    }

    private final ProductService products = new ProductService();

    public Page page(Filter filter) throws SQLException {
        Filter applied = normalize(filter);
        CategoryTree tree = products.categories();
        List<Long> categoryIds = applied.categoryId() == null ? null : tree.selfAndDescendants(applied.categoryId());
        try (Connection conn = Db.getConnection()) {
            List<Line> lines = new ArrayList<>();
            long available = 0;
            long reservedOut = 0;
            long empty = 0;
            long belowMinimum = 0;
            for (StockLedgerDao.Row row : new StockLedgerDao().rows(conn, applied.keyword(), applied.warehouseId(),
                    categoryIds, applied.status())) {
                Line line = new Line(row);
                lines.add(line);
                if (line.isBelowMinimum()) {
                    belowMinimum++;
                }
                switch (line.getStatus()) {
                    case "AVAILABLE" -> available++;
                    case "RESERVED_OUT" -> reservedOut++;
                    default -> empty++;
                }
            }
            return new Page(applied, lines, new LookupDao().warehouses(conn), tree.ordered(), available, reservedOut, empty,
                    belowMinimum);
        }
    }

    /** Quản lý kho khai báo cho kho mình phụ trách; Admin cho mọi kho. Để trống là bỏ ngưỡng. */
    public static boolean canSetMinimum(User user, long warehouseId) {
        return user.isAdmin() || (user.hasRole(Role.WH_MANAGER)
                && user.getWarehouses().stream().anyMatch(w -> w.getId() == warehouseId));
    }

    public void setMinimum(long warehouseId, long productId, String value, User actor) throws SQLException {
        if (!canSetMinimum(actor, warehouseId)) {
            throw new ForbiddenException("Chỉ Quản lý kho của kho này được khai báo tồn tối thiểu.");
        }
        BigDecimal minimum = null;
        if (value != null && !value.isBlank()) {
            try {
                minimum = new BigDecimal(value.strip().replace(",", "."));
            } catch (NumberFormatException e) {
                throw new ServiceException("Tồn tối thiểu phải là số.");
            }
            if (minimum.signum() < 0 || minimum.compareTo(MAX_MINIMUM) >= 0 || minimum.stripTrailingZeros().scale() > 4) {
                throw new ServiceException("Tồn tối thiểu phải từ 0, tối đa 4 chữ số thập phân.");
            }
        }
        BigDecimal applied = minimum;
        Db.inTransaction(conn -> {
            if (!new StockLedgerDao().setMinimum(conn, warehouseId, productId, applied)) {
                throw new ServiceException("SKU chưa có dòng tồn ở kho này.");
            }
            new AuditLogDao().log(conn, actor.getId(), "STOCK_MINIMUM_SET", "PRODUCT", productId,
                    "Kho #" + warehouseId + ": tồn tối thiểu " + (applied == null ? "bỏ ngưỡng" : applied.toPlainString()));
            return null;
        });
    }

    public record Export(String fileName, byte[] content) {
    }

    public Export exportXlsx(Filter filter) throws SQLException {
        Page page = page(filter);
        String warehouse = "tat-ca-kho";
        if (page.filter().warehouseId() != null) {
            try (Connection conn = Db.getConnection()) {
                String code = new StockLedgerDao().warehouseCode(conn, page.filter().warehouseId());
                warehouse = code == null ? warehouse : code;
            }
        }
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle header = workbook.createCellStyle();
            Font bold = workbook.createFont();
            bold.setBold(true);
            header.setFont(bold);
            CellStyle number = workbook.createCellStyle();
            number.setDataFormat(workbook.createDataFormat().getFormat("#,##0.####"));
            Sheet sheet = workbook.createSheet("Tồn kho");
            String[] headers = {"SKU", "Sản phẩm", "Nhóm hàng", "Kho", "ĐVT", "Tồn thực tế", "Đang giữ chỗ", "Khả dụng"};
            org.apache.poi.ss.usermodel.Row head = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = head.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(header);
            }
            int index = 1;
            for (Line line : page.lines()) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(index++);
                row.createCell(0).setCellValue(line.getSku());
                row.createCell(1).setCellValue(line.getProductName());
                row.createCell(2).setCellValue(line.getCategoryName());
                row.createCell(3).setCellValue(line.getWarehouseName());
                row.createCell(4).setCellValue(line.getBaseUnit());
                BigDecimal[] values = {line.getOnHand(), line.getReserved(), line.getAvailable()};
                for (int i = 0; i < values.length; i++) {
                    Cell cell = row.createCell(5 + i);
                    cell.setCellValue(values[i].doubleValue());
                    cell.setCellStyle(number);
                }
            }
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(out);
            return new Export("ton-kho-" + warehouse + "-" + LocalDate.now() + ".xlsx", out.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("Không tạo được tệp Excel tồn kho", e);
        }
    }

    private static Filter normalize(Filter filter) {
        String status = filter.status() != null && STATUSES.contains(filter.status()) ? filter.status() : null;
        return new Filter(filter.keyword() == null ? "" : filter.keyword().strip(), filter.warehouseId(),
                filter.categoryId(), status);
    }

    private static BigDecimal plain(BigDecimal value) {
        return new BigDecimal(value.stripTrailingZeros().toPlainString());
    }
}
