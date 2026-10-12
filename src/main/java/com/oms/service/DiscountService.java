package com.oms.service;

import com.oms.config.Db;
import com.oms.dao.AuditLogDao;
import com.oms.dao.CategoryDao;
import com.oms.dao.DiscountPolicyDao;
import com.oms.dao.ProductDao;
import com.oms.model.Category;
import com.oms.model.DiscountPolicy;
import com.oms.model.DiscountTier;
import com.oms.model.NamedItem;
import com.oms.model.Product;
import com.oms.model.User;
import com.oms.security.PermissionPolicy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Chính sách chiết khấu theo sản lượng (S3-01). Quy tắc tính (cũng ghi ở docs/chinh-sach-chiet-khau.md):
 * số lượng tính bằng đơn vị cơ sở, các dòng cùng sản phẩm trong một đơn được cộng lại; mỗi chính sách lấy bậc
 * cao nhất mà số lượng đạt tới; nhiều chính sách cùng áp dụng thì lấy chính sách cho mức giảm trên mỗi đơn vị
 * cao nhất (có lợi nhất cho khách), không cộng dồn.
 */
public class DiscountService {

    public static final int MAX_TIERS = 8;
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    /** Kết quả chiết khấu được chọn cho một sản phẩm; perUnit là số tiền giảm trên mỗi đơn vị cơ sở. */
    public record Applied(long policyId, String policyName, int tierMinQty, DiscountPolicy.Type type,
                          BigDecimal value, BigDecimal perUnit) {
    }

    private final DiscountPolicyDao policyDao = new DiscountPolicyDao();
    private final ProductDao productDao = new ProductDao();
    private final CategoryDao categoryDao = new CategoryDao();
    private final AuditLogDao auditDao = new AuditLogDao();

    public List<DiscountPolicy> list(User viewer) throws SQLException {
        requireManage(viewer);
        try (Connection conn = Db.getConnection()) {
            CategoryTree tree = new CategoryTree(categoryDao.listAll(conn));
            List<DiscountPolicy> policies = policyDao.list(conn);
            policies.forEach(p -> resolveCategory(p, tree));
            return policies;
        }
    }

    public DiscountPolicy get(long id, User viewer) throws SQLException {
        requireManage(viewer);
        try (Connection conn = Db.getConnection()) {
            DiscountPolicy p = policyDao.findById(conn, id);
            if (p == null) {
                throw new NotFoundException();
            }
            resolveCategory(p, new CategoryTree(categoryDao.listAll(conn)));
            return p;
        }
    }

    public List<NamedItem> productOptions() throws SQLException {
        try (Connection conn = Db.getConnection()) {
            return productDao.activeOptions(conn);
        }
    }

    public List<Category> categories() throws SQLException {
        try (Connection conn = Db.getConnection()) {
            return new CategoryTree(categoryDao.listAll(conn)).ordered();
        }
    }

    /**
     * Tạo (id = 0) hoặc sửa chính sách. tierRows là các hàng người dùng nhập, mỗi hàng {số lượng tối thiểu, giá trị};
     * hàng trống bị bỏ qua. Trả về id chính sách.
     */
    public long save(long id, DiscountPolicy input, List<String[]> tierRows, User actor) throws SQLException {
        requireManage(actor);
        DiscountPolicy p = validated(input, tierRows);
        return Db.inTransaction(conn -> {
            CategoryTree tree = new CategoryTree(categoryDao.listAll(conn));
            checkScope(conn, p, tree);
            if (id == 0) {
                long newId = policyDao.insert(conn, p);
                p.setId(newId);
                resolveCategory(p, tree);
                resolveProduct(conn, p);
                auditDao.log(conn, actor.getId(), "DISCOUNT_CREATE", "DISCOUNT_POLICY", newId, p.getName(), null, describe(p));
                return newId;
            }
            DiscountPolicy old = policyDao.findById(conn, id);
            if (old == null) {
                throw new NotFoundException();
            }
            resolveCategory(old, tree);
            p.setId(id);
            policyDao.update(conn, p);
            resolveCategory(p, tree);
            resolveProduct(conn, p);
            String before = describe(old);
            String after = describe(p);
            if (!before.equals(after)) {
                auditDao.log(conn, actor.getId(), "DISCOUNT_UPDATE", "DISCOUNT_POLICY", id, p.getName(), before, after);
            }
            return id;
        });
    }

    public void delete(long id, User actor) throws SQLException {
        requireManage(actor);
        Db.inTransaction(conn -> {
            DiscountPolicy old = policyDao.findById(conn, id);
            if (old == null) {
                throw new NotFoundException();
            }
            resolveCategory(old, new CategoryTree(categoryDao.listAll(conn)));
            policyDao.delete(conn, id);
            auditDao.log(conn, actor.getId(), "DISCOUNT_DELETE", "DISCOUNT_POLICY", id, old.getName(), describe(old), null);
            return null;
        });
    }

    /**
     * Chiết khấu có lợi nhất cho khách khi mua qtyBase đơn vị cơ sở của sản phẩm, đơn giá cơ sở unitPrice, vào ngày date.
     * Trả null nếu không có chính sách nào áp dụng hoặc chưa đạt bậc nào (đơn hàng EP-04 gọi).
     */
    public Applied best(Connection conn, long productId, BigDecimal qtyBase, BigDecimal unitPrice, LocalDate date)
            throws SQLException {
        Product product = productDao.findById(conn, productId, false);
        if (product == null || qtyBase.signum() <= 0 || unitPrice.signum() <= 0) {
            return null;
        }
        List<Long> categoryIds = selfAndAncestors(new CategoryTree(categoryDao.listAll(conn)), product.getCategoryId());
        Applied best = null;
        for (DiscountPolicy p : policyDao.candidates(conn, productId, categoryIds, date)) {
            DiscountTier tier = p.getTiers().stream()
                    .filter(t -> qtyBase.compareTo(BigDecimal.valueOf(t.getMinQty())) >= 0)
                    .max(Comparator.comparingInt(DiscountTier::getMinQty)).orElse(null);
            if (tier == null) {
                continue;
            }
            BigDecimal perUnit = p.getType() == DiscountPolicy.Type.PERCENT
                    ? unitPrice.multiply(tier.getValue()).divide(HUNDRED, 2, RoundingMode.HALF_UP)
                    : tier.getValue();
            perUnit = perUnit.min(unitPrice);
            // cùng mức giảm thì giữ chính sách có id nhỏ hơn (danh sách đã sắp theo id) để kết quả ổn định
            if (best == null || perUnit.compareTo(best.perUnit()) > 0) {
                best = new Applied(p.getId(), p.getName(), tier.getMinQty(), p.getType(), tier.getValue(), perUnit);
            }
        }
        return best;
    }

    private static List<Long> selfAndAncestors(CategoryTree tree, long categoryId) {
        List<Long> ids = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        Category c = tree.get(categoryId);
        while (c != null && seen.add(c.getId())) {
            ids.add(c.getId());
            c = c.getParentId() == null ? null : tree.get(c.getParentId());
        }
        return ids;
    }

    private DiscountPolicy validated(DiscountPolicy in, List<String[]> rows) {
        String name = in.getName() == null ? "" : in.getName().trim();
        if (name.isEmpty()) {
            throw new ServiceException("Vui lòng nhập tên chính sách.");
        }
        if (name.length() > 150) {
            throw new ServiceException("Tên chính sách tối đa 150 ký tự.");
        }
        if ((in.getProductId() == null) == (in.getCategoryId() == null)) {
            throw new ServiceException("Vui lòng chọn một sản phẩm hoặc một nhóm hàng để áp dụng chính sách.");
        }
        if (in.getValidFrom() != null && in.getValidTo() != null && in.getValidTo().isBefore(in.getValidFrom())) {
            throw new ServiceException("Ngày kết thúc phải sau hoặc bằng ngày bắt đầu.");
        }
        DiscountPolicy p = new DiscountPolicy();
        p.setName(name);
        p.setProductId(in.getProductId());
        p.setCategoryId(in.getCategoryId());
        p.setType(in.getType());
        p.setValidFrom(in.getValidFrom());
        p.setValidTo(in.getValidTo());
        p.setStatus(in.getStatus());
        p.setTiers(parseTiers(rows, in.getType()));
        return p;
    }

    private static List<DiscountTier> parseTiers(List<String[]> rows, DiscountPolicy.Type type) {
        List<DiscountTier> tiers = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        int n = 0;
        for (String[] row : rows) {
            String qtyText = row[0] == null ? "" : row[0].trim();
            String valueText = row[1] == null ? "" : row[1].trim();
            if (qtyText.isEmpty() && valueText.isEmpty()) {
                continue;
            }
            n++;
            if (qtyText.isEmpty() || valueText.isEmpty()) {
                throw new ServiceException("Bậc " + n + ": nhập đủ số lượng tối thiểu và mức chiết khấu.");
            }
            int qty = parseQty(qtyText, n);
            BigDecimal value = type == DiscountPolicy.Type.PERCENT ? parsePercent(valueText, n) : parseAmount(valueText, n);
            if (!seen.add(qty)) {
                throw new ServiceException("Bậc " + n + ": số lượng tối thiểu " + qty + " bị trùng với một bậc khác.");
            }
            tiers.add(new DiscountTier(qty, value));
        }
        if (tiers.isEmpty()) {
            throw new ServiceException("Vui lòng nhập ít nhất một bậc chiết khấu.");
        }
        if (tiers.size() > MAX_TIERS) {
            throw new ServiceException("Một chính sách có tối đa " + MAX_TIERS + " bậc.");
        }
        tiers.sort(Comparator.comparingInt(DiscountTier::getMinQty));
        // bậc cao hơn mà mức giảm không lớn hơn thì vô nghĩa và dễ nhập nhầm
        for (int i = 1; i < tiers.size(); i++) {
            if (tiers.get(i).getValue().compareTo(tiers.get(i - 1).getValue()) <= 0) {
                throw new ServiceException("Mua từ " + tiers.get(i).getMinQty() + " phải được chiết khấu cao hơn mua từ "
                        + tiers.get(i - 1).getMinQty() + ".");
            }
        }
        return tiers;
    }

    private static int parseQty(String text, int n) {
        String digits = text.replace(".", "").replace(" ", "");
        try {
            int qty = Integer.parseInt(digits);
            if (qty < 1) {
                throw new NumberFormatException();
            }
            return qty;
        } catch (NumberFormatException e) {
            throw new ServiceException("Bậc " + n + ": số lượng tối thiểu phải là số nguyên từ 1 (tính theo đơn vị cơ sở).");
        }
    }

    /** Phần trăm cho phép dấu phẩy thập phân kiểu Việt Nam ("2,5"); lớn hơn 0 và tối đa 100. */
    private static BigDecimal parsePercent(String text, int n) {
        try {
            BigDecimal value = new BigDecimal(text.replace(" ", "").replace(',', '.'));
            if (value.signum() <= 0 || value.compareTo(HUNDRED) > 0 || value.scale() > 2) {
                throw new NumberFormatException();
            }
            return value.setScale(2, RoundingMode.UNNECESSARY);
        } catch (NumberFormatException | ArithmeticException e) {
            throw new ServiceException("Bậc " + n + ": phần trăm chiết khấu phải lớn hơn 0 và tối đa 100 (tối đa 2 chữ số thập phân).");
        }
    }

    private static BigDecimal parseAmount(String text, int n) {
        BigDecimal value;
        try {
            value = Money.parse(text, "Bậc " + n + ": số tiền chiết khấu");
        } catch (ServiceException e) {
            throw new ServiceException("Bậc " + n + ": số tiền chiết khấu phải là số dương, tối đa 2 chữ số thập phân.");
        }
        if (value == null || value.signum() <= 0) {
            throw new ServiceException("Bậc " + n + ": số tiền chiết khấu phải lớn hơn 0.");
        }
        return value;
    }

    private void checkScope(Connection conn, DiscountPolicy p, CategoryTree tree) throws SQLException {
        if (p.getProductId() != null && productDao.findById(conn, p.getProductId(), false) == null) {
            throw new ServiceException("Sản phẩm đã chọn không còn tồn tại.");
        }
        if (p.getCategoryId() != null && tree.get(p.getCategoryId()) == null) {
            throw new ServiceException("Nhóm hàng đã chọn không còn tồn tại.");
        }
    }

    private static void resolveCategory(DiscountPolicy p, CategoryTree tree) {
        if (p.getCategoryId() != null) {
            Category c = tree.get(p.getCategoryId());
            p.setCategoryPath(c == null ? null : c.getPath().replace(CategoryTree.SEPARATOR, " • "));
        }
    }

    private void resolveProduct(Connection conn, DiscountPolicy p) throws SQLException {
        if (p.getProductId() != null) {
            Product product = productDao.findById(conn, p.getProductId(), false);
            p.setProductLabel(product.getSku() + " – " + product.getName());
        }
    }

    private static String describe(DiscountPolicy p) {
        return "Tên: " + p.getName() + "\nÁp dụng cho: " + p.getScopeKind() + " " + p.getScopeText()
                + "\nKiểu: " + p.getType().getLabel() + "\nCác bậc: " + p.getTiersText()
                + "\nHiệu lực: " + p.getValidityText() + "\nTrạng thái: " + p.getStatus().getLabel();
    }

    private static void requireManage(User user) {
        if (!PermissionPolicy.canManageDiscounts(user.getRoles())) {
            throw new ForbiddenException("Chỉ Quản lý kinh doanh được khai báo chính sách chiết khấu.");
        }
    }

    public static class NotFoundException extends ServiceException {
        public NotFoundException() {
            super("Không tìm thấy chính sách chiết khấu.");
        }
    }
}
