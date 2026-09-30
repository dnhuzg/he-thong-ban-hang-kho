package com.oms.service;

import com.oms.config.Db;
import com.oms.dao.AuditLogDao;
import com.oms.dao.CategoryDao;
import com.oms.dao.LookupDao;
import com.oms.dao.ProductDao;
import com.oms.dao.ProductUnitDao;
import com.oms.model.Category;
import com.oms.model.ImportBatch;
import com.oms.model.ImportRow.Outcome;
import com.oms.model.NamedItem;
import com.oms.model.Product;
import com.oms.model.ProductImportRow;
import com.oms.model.User;
import com.oms.security.PermissionPolicy;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Nhập danh mục sản phẩm từ Excel (S2-08). Bước xem trước chưa ghi gì: đối chiếu từng dòng và báo lỗi;
 * bước nhập ghi các dòng hợp lệ trong một transaction. SKU đã có thì cập nhật thay vì tạo mới.
 */
public class ProductImportService {

    public static final int MAX_ROWS = 5000;

    private static final List<String> HEADERS = List.of("Mã SKU *", "Tên sản phẩm *", "Nhóm hàng *", "Đơn vị tính *",
            "Quy cách", "Giá vốn", "Trạng thái");
    private static final List<String> REQUIRED = List.of("Mã SKU", "Tên sản phẩm", "Nhóm hàng", "Đơn vị tính");

    private final ProductDao productDao = new ProductDao();
    private final ProductUnitDao productUnitDao = new ProductUnitDao();
    private final CategoryDao categoryDao = new CategoryDao();
    private final LookupDao lookupDao = new LookupDao();
    private final AuditLogDao auditDao = new AuditLogDao();

    public byte[] template() {
        return Excel.template(HEADERS,
                List.of(List.of("COCA-330", "Coca-Cola lon 330ml", "Đồ uống › Nước giải khát › Nước ngọt có ga", "Lon", "Thùng 24 lon", "7200", "Đang kinh doanh"),
                        List.of("MI-HAOHAO", "Mì Hảo Hảo tôm chua cay", "Thực phẩm khô", "Gói", "Thùng 30 gói", "3500", "Đang kinh doanh")),
                List.of("Cột có dấu * là bắt buộc. Giữ nguyên dòng tiêu đề; có thể đổi thứ tự cột.",
                        "Mã SKU: 2–50 ký tự chữ không dấu, số và . _ - . SKU đã có trong hệ thống thì sản phẩm được CẬP NHẬT (SKU không đổi).",
                        "Nhóm hàng: ghi đủ đường dẫn, ví dụ \"Đồ uống › Nước giải khát\", hoặc chỉ tên nếu tên đó không trùng ở nhóm khác.",
                        "Đơn vị tính: tên đơn vị đã có trong hệ thống (Lon, Thùng, Gói...). Đây là đơn vị cơ sở.",
                        "Giá vốn: chỉ Quản lý kinh doanh và Admin được nhập; để trống thì giữ nguyên giá vốn cũ.",
                        "Trạng thái: \"Đang kinh doanh\" hoặc \"Ngừng kinh doanh\"; để trống thì sản phẩm mới là đang kinh doanh, sản phẩm cũ giữ nguyên.",
                        "Tối đa " + MAX_ROWS + " dòng mỗi tệp."));
    }

    public ImportBatch<ProductImportRow> preview(byte[] data, String fileName, User actor) throws SQLException {
        Excel.Table table = Excel.read(data, MAX_ROWS, REQUIRED);
        boolean costAllowed = PermissionPolicy.canViewCostPrice(actor.getRoles());
        List<ProductImportRow> rows = new ArrayList<>();
        try (Connection conn = Db.getConnection()) {
            CategoryLookup categories = new CategoryLookup(categoryDao.listAll(conn));
            Map<String, NamedItem> units = new HashMap<>();
            for (NamedItem u : lookupDao.units(conn)) {
                units.put(u.getName().toLowerCase(Locale.ROOT), u);
            }
            Map<String, Product> existing = productDao.allBySku(conn);
            Set<String> seen = new HashSet<>();
            for (Excel.Line line : table.lines()) {
                rows.add(check(line, categories, units, existing, seen, costAllowed));
            }
        }
        ImportBatch<ProductImportRow> batch = new ImportBatch<>(fileName, rows);
        if (!costAllowed && table.has("giá vốn")) {
            batch.getNotes().add("Bạn không có quyền nhập giá vốn nên cột Giá vốn được bỏ qua.");
        }
        return batch;
    }

    /** Ghi các dòng hợp lệ trong một transaction; dòng lỗi được bỏ qua. Mỗi lô chỉ áp một lần. */
    public void apply(ImportBatch<ProductImportRow> batch, User actor) throws SQLException {
        if (batch.isApplied()) {
            throw new ServiceException("Tệp này đã được nhập rồi.");
        }
        boolean costAllowed = PermissionPolicy.canViewCostPrice(actor.getRoles());
        Db.inTransaction(conn -> {
            CategoryTree tree = new CategoryTree(categoryDao.listAll(conn));
            List<NamedItem> units = lookupDao.units(conn);
            for (ProductImportRow row : batch.getRows()) {
                if (!row.isValid()) {
                    row.setOutcome(Outcome.SKIPPED);
                    continue;
                }
                Product p = new Product();
                p.setSku(row.getSku());
                p.setName(row.getName());
                p.setCategoryId(row.getCategoryId());
                p.setBaseUnitId(row.getUnitId());
                p.setPackingSpec(row.getPackingSpec().isEmpty() ? null : row.getPackingSpec());
                if (row.isExisting()) {
                    update(conn, row, p, tree, units, actor, costAllowed);
                } else {
                    p.setStatus(row.getStatusValue() == null ? Product.Status.ACTIVE : row.getStatusValue());
                    p.setCostPrice(costAllowed ? row.getCostValue() : null);
                    if (productDao.existsSku(conn, p.getSku())) {
                        skip(row, "SKU vừa được tạo ở nơi khác. Hãy tải lại tệp để xem trước.");
                        continue;
                    }
                    long id = productDao.insert(conn, p);
                    auditDao.log(conn, actor.getId(), "PRODUCT_CREATE", "PRODUCT", id, p.getSku(), null,
                            ProductService.describe(tree, units, p));
                    if (p.getCostPrice() != null) {
                        auditDao.log(conn, actor.getId(), "PRODUCT_COST_UPDATE", "PRODUCT", id, p.getSku(), null,
                                ProductService.money(p.getCostPrice()));
                    }
                    row.setOutcome(Outcome.CREATED);
                }
            }
            auditDao.log(conn, actor.getId(), "PRODUCT_IMPORT", "PRODUCT", null, batch.getFileName(), null,
                    "Tạo mới: " + batch.getCreatedCount() + "\nCập nhật: " + batch.getUpdatedCount()
                            + "\nKhông thay đổi: " + batch.getUnchangedCount() + "\nBỏ qua: " + batch.getSkippedCount());
            return null;
        });
        batch.setApplied(true);
    }

    private void update(Connection conn, ProductImportRow row, Product p, CategoryTree tree, List<NamedItem> units,
                        User actor, boolean costAllowed) throws SQLException {
        Product old = productDao.findBySkuForUpdate(conn, p.getSku());
        if (old == null) {
            skip(row, "Sản phẩm không còn tồn tại. Hãy tải lại tệp để xem trước.");
            return;
        }
        if (p.getBaseUnitId() != old.getBaseUnitId() && productUnitDao.hasAny(conn, old.getId())) {
            skip(row, "Sản phẩm đã có đơn vị quy đổi nên không đổi được đơn vị cơ sở.");
            return;
        }
        p.setId(old.getId());
        p.setStatus(row.getStatusValue() == null ? old.getStatus() : row.getStatusValue());
        String before = ProductService.describe(tree, units, old);
        String after = ProductService.describe(tree, units, p);
        boolean changed = !before.equals(after);
        if (changed) {
            productDao.update(conn, p);
            auditDao.log(conn, actor.getId(), "PRODUCT_UPDATE", "PRODUCT", old.getId(), old.getSku(), before, after);
        }
        BigDecimal newCost = row.getCostValue();
        if (costAllowed && newCost != null && !ProductService.sameMoney(old.getCostPrice(), newCost)) {
            productDao.updateCost(conn, old.getId(), newCost);
            auditDao.log(conn, actor.getId(), "PRODUCT_COST_UPDATE", "PRODUCT", old.getId(), old.getSku(),
                    ProductService.money(old.getCostPrice()), ProductService.money(newCost));
            changed = true;
        }
        row.setOutcome(changed ? Outcome.UPDATED : Outcome.UNCHANGED);
    }

    private static void skip(ProductImportRow row, String reason) {
        row.addError(reason);
        row.setOutcome(Outcome.SKIPPED);
    }

    private ProductImportRow check(Excel.Line line, CategoryLookup categories, Map<String, NamedItem> units,
                                   Map<String, Product> existing, Set<String> seen, boolean costAllowed) {
        ProductImportRow row = new ProductImportRow();
        row.setRowNumber(line.number());
        row.setSku(line.get("mã sku"));
        row.setName(line.get("tên sản phẩm"));
        row.setCategory(line.get("nhóm hàng"));
        row.setUnit(line.get("đơn vị tính"));
        row.setPackingSpec(line.get("quy cách"));
        row.setCostPrice(line.get("giá vốn"));
        row.setStatus(line.get("trạng thái"));

        try {
            row.setSku(ProductService.normalizeSku(row.getSku()));
            if (!seen.add(row.getSku())) {
                row.addError("SKU trùng với một dòng phía trên trong tệp");
            }
            row.setExisting(existing.containsKey(row.getSku()));
        } catch (ServiceException e) {
            row.addError(e.getMessage());
        }
        if (row.getName().isEmpty()) {
            row.addError("Thiếu tên sản phẩm");
        } else if (row.getName().length() > 255) {
            row.addError("Tên sản phẩm tối đa 255 ký tự");
        }
        if (row.getPackingSpec().length() > 255) {
            row.addError("Quy cách đóng gói tối đa 255 ký tự");
        }
        String categoryError = categories.resolve(row.getCategory(), row);
        if (categoryError != null) {
            row.addError(categoryError);
        }
        NamedItem unit = units.get(row.getUnit().toLowerCase(Locale.ROOT));
        if (row.getUnit().isEmpty()) {
            row.addError("Thiếu đơn vị tính");
        } else if (unit == null) {
            row.addError("Đơn vị tính \"" + row.getUnit() + "\" chưa có trong hệ thống");
        } else {
            row.setUnitId(unit.getId());
        }
        if (costAllowed) {
            try {
                row.setCostValue(Money.parse(row.getCostPrice(), "Giá vốn"));
            } catch (ServiceException e) {
                row.addError(e.getMessage());
            }
        }
        if (!row.getStatus().isEmpty()) {
            String s = row.getStatus().toLowerCase(Locale.ROOT);
            if (s.equals("đang kinh doanh") || s.equals("active")) {
                row.setStatusValue(Product.Status.ACTIVE);
            } else if (s.equals("ngừng kinh doanh") || s.equals("discontinued")) {
                row.setStatusValue(Product.Status.DISCONTINUED);
            } else {
                row.addError("Trạng thái phải là \"Đang kinh doanh\" hoặc \"Ngừng kinh doanh\"");
            }
        }
        return row;
    }

    /** Nhận nhóm hàng theo đường dẫn đầy đủ hoặc theo tên (khi tên không trùng ở nhóm khác). */
    private static final class CategoryLookup {
        private final Map<String, List<Category>> byKey = new HashMap<>();

        CategoryLookup(List<Category> flat) {
            for (Category c : new CategoryTree(flat).ordered()) {
                byKey.computeIfAbsent(key(c.getPath()), k -> new ArrayList<>()).add(c);
                byKey.computeIfAbsent(key(c.getName()), k -> new ArrayList<>()).add(c);
            }
        }

        String resolve(String text, ProductImportRow row) {
            if (text.isEmpty()) {
                return "Thiếu nhóm hàng";
            }
            List<Category> found = byKey.getOrDefault(key(text), List.of()).stream().distinct().toList();
            if (found.isEmpty()) {
                return "Nhóm hàng \"" + text + "\" chưa có trong hệ thống";
            }
            if (found.size() > 1) {
                return "Nhóm hàng \"" + text + "\" trùng tên ở nhiều nhánh, hãy ghi đủ đường dẫn (ví dụ \""
                        + found.get(0).getPath() + "\")";
            }
            row.setCategoryId(found.get(0).getId());
            return null;
        }

        private static String key(String s) {
            return s.replaceAll("\\s*[›>/]\\s*", "›").trim().toLowerCase(Locale.ROOT);
        }
    }
}
