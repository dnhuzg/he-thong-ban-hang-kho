package com.oms.service;

import com.oms.config.Db;
import com.oms.dao.AuditLogDao;
import com.oms.dao.CategoryDao;
import com.oms.dao.LookupDao;
import com.oms.dao.ProductDao;
import com.oms.dao.ProductUnitDao;
import com.oms.model.NamedItem;
import com.oms.model.PageResult;
import com.oms.model.Product;
import com.oms.model.User;
import com.oms.security.PermissionPolicy;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Danh mục sản phẩm (S2-05). */
public class ProductService {

    public static final int PAGE_SIZE = 20;
    private static final Pattern SKU = Pattern.compile("^[A-Z0-9][A-Z0-9._-]{1,49}$");
    private static final String IMAGE_FOLDER = "products";

    private final ProductDao productDao = new ProductDao();
    private final ProductUnitDao productUnitDao = new ProductUnitDao();
    private final CategoryDao categoryDao = new CategoryDao();
    private final LookupDao lookupDao = new LookupDao();
    private final AuditLogDao auditDao = new AuditLogDao();
    private final FileStorage storage;

    public ProductService() {
        this(new FileStorage());
    }

    public ProductService(FileStorage storage) {
        this.storage = storage;
    }

    /** Lọc theo nhóm hàng tính cả nhóm con; giá vốn chỉ nạp khi người xem được phép. */
    public PageResult<Product> search(String keyword, long categoryId, Product.Status status, int page, User viewer)
            throws SQLException {
        try (Connection conn = Db.getConnection()) {
            CategoryTree tree = new CategoryTree(categoryDao.listAll(conn));
            List<Long> categoryIds = categoryId > 0 ? tree.selfAndDescendants(categoryId) : null;
            PageResult<Product> result = productDao.search(conn, keyword, categoryIds, status, Math.max(1, page),
                    PAGE_SIZE, PermissionPolicy.canViewCostPrice(viewer.getRoles()));
            result.getItems().forEach(p -> p.setCategoryPath(pathOf(tree, p.getCategoryId())));
            var conversions = productUnitDao.listByProducts(conn, result.getItems().stream().map(Product::getId).toList());
            result.getItems().forEach(p -> p.setConversions(conversions.getOrDefault(p.getId(), List.of())));
            return result;
        }
    }

    public Product get(long id, User viewer) throws SQLException {
        try (Connection conn = Db.getConnection()) {
            Product p = productDao.findById(conn, id, PermissionPolicy.canViewCostPrice(viewer.getRoles()));
            if (p == null) {
                throw new NotFoundException();
            }
            p.setCategoryPath(pathOf(new CategoryTree(categoryDao.listAll(conn)), p.getCategoryId()));
            return p;
        }
    }

    public CategoryTree categories() throws SQLException {
        try (Connection conn = Db.getConnection()) {
            return new CategoryTree(categoryDao.listAll(conn));
        }
    }

    public List<NamedItem> units() throws SQLException {
        try (Connection conn = Db.getConnection()) {
            return lookupDao.units(conn);
        }
    }

    public long create(ProductForm form, byte[] image, User actor) throws SQLException, IOException {
        String sku = normalizeSku(form.getSku());
        Product p = toProduct(form);
        p.setSku(sku);
        boolean costAllowed = PermissionPolicy.canViewCostPrice(actor.getRoles());
        p.setCostPrice(costAllowed ? parseCost(form.getCostPrice()) : null);
        BufferedImage picture = image == null || image.length == 0 ? null : Images.readJpegOrPng(image);

        long id = Db.inTransaction(conn -> {
            validateReferences(conn, p);
            if (productDao.existsSku(conn, sku)) {
                throw new ServiceException("Mã SKU \"" + sku + "\" đã tồn tại. Mỗi sản phẩm phải có mã SKU riêng.");
            }
            long newId = productDao.insert(conn, p);
            auditDao.log(conn, actor.getId(), "PRODUCT_CREATE", "PRODUCT", newId, sku, null, describe(conn, p));
            if (p.getCostPrice() != null) {
                auditDao.log(conn, actor.getId(), "PRODUCT_COST_UPDATE", "PRODUCT", newId, sku, null, money(p.getCostPrice()));
            }
            return newId;
        });
        if (picture != null) {
            replaceImage(id, picture, actor);
        }
        return id;
    }

    /**
     * Sửa sản phẩm. SKU không đổi sau khi tạo; giá vốn chỉ Quản lý kinh doanh (và Admin) sửa được —
     * người khác gửi kèm trường giá vốn cũng bị bỏ qua.
     */
    public void update(ProductForm form, byte[] image, boolean removeImage, User actor) throws SQLException, IOException {
        Product changes = toProduct(form);
        boolean costAllowed = PermissionPolicy.canViewCostPrice(actor.getRoles());
        BigDecimal newCost = costAllowed ? parseCost(form.getCostPrice()) : null;
        BufferedImage picture = image == null || image.length == 0 ? null : Images.readJpegOrPng(image);

        String oldImage = Db.inTransaction(conn -> {
            Product existing = productDao.findByIdForUpdate(conn, form.getId());
            if (existing == null) {
                throw new NotFoundException();
            }
            changes.setId(existing.getId());
            changes.setSku(existing.getSku());
            // Hệ số quy đổi tính theo đơn vị cơ sở cũ: đổi đơn vị cơ sở sẽ làm sai toàn bộ quy đổi (S2-07)
            if (changes.getBaseUnitId() != existing.getBaseUnitId() && productUnitDao.hasAny(conn, existing.getId())) {
                throw new ServiceException("Sản phẩm đã khai báo đơn vị quy đổi nên không đổi được đơn vị cơ sở. "
                        + "Hãy xoá các đơn vị quy đổi trước.");
            }
            validateReferences(conn, changes);
            String before = describe(conn, existing);
            productDao.update(conn, changes);
            String after = describe(conn, changes);
            if (!before.equals(after)) {
                auditDao.log(conn, actor.getId(), "PRODUCT_UPDATE", "PRODUCT", existing.getId(), existing.getSku(), before, after);
            }
            if (costAllowed && !sameMoney(existing.getCostPrice(), newCost)) {
                productDao.updateCost(conn, existing.getId(), newCost);
                auditDao.log(conn, actor.getId(), "PRODUCT_COST_UPDATE", "PRODUCT", existing.getId(), existing.getSku(),
                        money(existing.getCostPrice()), money(newCost));
            }
            if (removeImage && picture == null && existing.getImagePath() != null) {
                productDao.updateImage(conn, existing.getId(), null);
                return existing.getImagePath();
            }
            return null;
        });
        deleteImageFiles(oldImage);
        if (picture != null) {
            replaceImage(form.getId(), picture, actor);
        }
    }

    /** Chỉ xoá được khi chưa có dữ liệu nào tham chiếu (khoá ngoại chặn); ngược lại chỉ ngừng kinh doanh. */
    public void delete(long id, User actor) throws SQLException {
        String image;
        try {
            image = Db.inTransaction(conn -> {
                Product existing = productDao.findByIdForUpdate(conn, id);
                if (existing == null) {
                    throw new NotFoundException();
                }
                auditDao.log(conn, actor.getId(), "PRODUCT_DELETE", "PRODUCT", id, existing.getSku(), describe(conn, existing), null);
                productDao.delete(conn, id);
                return existing.getImagePath();
            });
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new ServiceException("Sản phẩm đã phát sinh giao dịch hoặc dữ liệu liên quan nên không xoá được. "
                    + "Hãy chuyển sang \"Ngừng kinh doanh\".");
        }
        deleteImageFiles(image);
    }

    private void replaceImage(long productId, BufferedImage picture, User actor) throws SQLException, IOException {
        String name = FileStorage.randomName();
        String path = storage.write(IMAGE_FOLDER, name + ".png", Images.squarePng(picture, 400));
        String thumb = storage.write(IMAGE_FOLDER, name + "-64.png", Images.squarePng(picture, 64));
        String old;
        try {
            old = Db.inTransaction(conn -> {
                Product existing = productDao.findByIdForUpdate(conn, productId);
                if (existing == null) {
                    throw new NotFoundException();
                }
                productDao.updateImage(conn, productId, path);
                auditDao.log(conn, actor.getId(), "PRODUCT_IMAGE_UPDATE", "PRODUCT", productId, existing.getSku(), null, null);
                return existing.getImagePath();
            });
        } catch (SQLException | RuntimeException e) {
            storage.deleteQuietly(path);
            storage.deleteQuietly(thumb);
            throw e;
        }
        deleteImageFiles(old);
    }

    private void deleteImageFiles(String path) {
        if (path != null) {
            storage.deleteQuietly(path);
            storage.deleteQuietly(path.replaceFirst("\\.png$", "-64.png"));
        }
    }

    private Product toProduct(ProductForm form) {
        String name = form.getName() == null ? "" : form.getName().trim();
        if (name.isEmpty()) {
            throw new ServiceException("Vui lòng nhập tên sản phẩm.");
        }
        if (name.length() > 255) {
            throw new ServiceException("Tên sản phẩm tối đa 255 ký tự.");
        }
        String packing = form.getPackingSpec() == null ? "" : form.getPackingSpec().trim();
        if (packing.length() > 255) {
            throw new ServiceException("Quy cách đóng gói tối đa 255 ký tự.");
        }
        Product p = new Product();
        p.setName(name);
        p.setCategoryId(form.getCategoryId());
        p.setBaseUnitId(form.getBaseUnitId());
        p.setPackingSpec(packing.isEmpty() ? null : packing);
        try {
            p.setStatus(Product.Status.valueOf(form.getStatus()));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ServiceException("Trạng thái sản phẩm không hợp lệ.");
        }
        return p;
    }

    private void validateReferences(Connection conn, Product p) throws SQLException {
        if (p.getCategoryId() <= 0 || new CategoryTree(categoryDao.listAll(conn)).get(p.getCategoryId()) == null) {
            throw new ServiceException("Vui lòng chọn nhóm hàng.");
        }
        if (lookupDao.units(conn).stream().noneMatch(u -> u.getId() == p.getBaseUnitId())) {
            throw new ServiceException("Vui lòng chọn đơn vị tính cơ sở.");
        }
    }

    static String normalizeSku(String raw) {
        String sku = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if (!SKU.matcher(sku).matches()) {
            throw new ServiceException("Mã SKU gồm 2–50 ký tự: chữ không dấu, số và . _ - (tự chuyển thành chữ hoa).");
        }
        return sku;
    }

    /** Giá vốn người dùng nhập; bỏ trống = chưa có giá vốn. */
    static BigDecimal parseCost(String raw) {
        return Money.parse(raw, "Giá vốn");
    }

    private String describe(Connection conn, Product p) throws SQLException {
        CategoryTree tree = new CategoryTree(categoryDao.listAll(conn));
        String unit = lookupDao.units(conn).stream().filter(u -> u.getId() == p.getBaseUnitId())
                .map(NamedItem::getName).findFirst().orElse("?");
        return "Tên: " + p.getName() + "\nNhóm hàng: " + pathOf(tree, p.getCategoryId())
                + "\nĐơn vị cơ sở: " + unit + "\nQuy cách: " + (p.getPackingSpec() == null ? "—" : p.getPackingSpec())
                + "\nTrạng thái: " + p.getStatus().getLabel();
    }

    private static String pathOf(CategoryTree tree, long categoryId) {
        var c = tree.get(categoryId);
        return c == null ? "?" : c.getPath();
    }

    private static boolean sameMoney(BigDecimal a, BigDecimal b) {
        return a == null ? b == null : b != null && a.compareTo(b) == 0;
    }

    private static String money(BigDecimal value) {
        return value == null ? "—" : value.toPlainString();
    }

    public static class NotFoundException extends ServiceException {
        public NotFoundException() {
            super("Không tìm thấy sản phẩm.");
        }
    }
}
