package com.oms.service;

import com.oms.config.Db;
import com.oms.dao.AuditLogDao;
import com.oms.dao.LookupDao;
import com.oms.dao.ProductDao;
import com.oms.dao.ProductUnitDao;
import com.oms.model.NamedItem;
import com.oms.model.Product;
import com.oms.model.ProductUnit;
import com.oms.model.User;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Đơn vị tính quy đổi (S2-07). Quy tắc dùng cho đơn hàng / phiếu kho sau này:
 * gọi {@link #toBase} khi ghi dòng giao dịch và lưu cả số lượng quy đổi lẫn hệ số trả về (bản chụp),
 * nhờ vậy đổi hệ số về sau không làm sai các giao dịch đã ghi.
 */
public class UnitConversionService {

    private static final BigDecimal MAX_FACTOR = new BigDecimal("99999999.9999");

    /** Kết quả quy đổi một dòng: số lượng theo đơn vị đã chọn và bản chụp hệ số tại thời điểm ghi. */
    public record Converted(long unitId, BigDecimal quantity, BigDecimal factor, BigDecimal baseQuantity) {
    }

    private final ProductUnitDao productUnitDao = new ProductUnitDao();
    private final ProductDao productDao = new ProductDao();
    private final LookupDao lookupDao = new LookupDao();
    private final AuditLogDao auditDao = new AuditLogDao();

    public List<ProductUnit> list(long productId) throws SQLException {
        try (Connection conn = Db.getConnection()) {
            return productUnitDao.listByProduct(conn, productId);
        }
    }

    /**
     * Quy số lượng theo đơn vị bất kỳ của sản phẩm về đơn vị cơ sở, dùng trong transaction của nghiệp vụ gọi tới.
     * Đơn vị cơ sở có hệ số 1; đơn vị chưa khai báo quy đổi cho sản phẩm thì báo lỗi.
     */
    public Converted toBase(Connection conn, long productId, long unitId, BigDecimal quantity) throws SQLException {
        if (quantity == null || quantity.signum() <= 0) {
            throw new ServiceException("Số lượng phải lớn hơn 0.");
        }
        Product product = productDao.findById(conn, productId, false);
        if (product == null) {
            throw new ProductService.NotFoundException();
        }
        BigDecimal factor = unitId == product.getBaseUnitId() ? BigDecimal.ONE : productUnitDao.findFactor(conn, productId, unitId);
        if (factor == null) {
            throw new ServiceException("Sản phẩm " + product.getSku() + " chưa khai báo quy đổi cho đơn vị này.");
        }
        return new Converted(unitId, quantity, factor, quantity.multiply(factor).setScale(4, RoundingMode.HALF_UP));
    }

    /** Thêm mới hoặc sửa hệ số của một đơn vị quy đổi. */
    public void save(long productId, long unitId, String factorText, User actor) throws SQLException {
        BigDecimal factor = parseFactor(factorText);
        Db.inTransaction(conn -> {
            Product product = productDao.findByIdForUpdate(conn, productId);
            if (product == null) {
                throw new ProductService.NotFoundException();
            }
            if (unitId == product.getBaseUnitId()) {
                throw new ServiceException("Đơn vị cơ sở luôn có hệ số 1, không cần khai báo quy đổi.");
            }
            String unitName = lookupDao.units(conn).stream().filter(u -> u.getId() == unitId)
                    .map(NamedItem::getName).findFirst()
                    .orElseThrow(() -> new ServiceException("Vui lòng chọn đơn vị tính."));
            BigDecimal old = productUnitDao.findFactor(conn, productId, unitId);
            if (old == null) {
                productUnitDao.insert(conn, productId, unitId, factor);
            } else if (old.compareTo(factor) != 0) {
                productUnitDao.updateFactor(conn, productId, unitId, factor);
            } else {
                return null;
            }
            String base = product.getBaseUnitName();
            auditDao.log(conn, actor.getId(), "PRODUCT_UNIT_SAVE", "PRODUCT", productId, product.getSku(),
                    old == null ? null : "1 " + unitName + " = " + text(old) + " " + base,
                    "1 " + unitName + " = " + text(factor) + " " + base);
            return null;
        });
    }

    public void delete(long productId, long unitId, User actor) throws SQLException {
        Db.inTransaction(conn -> {
            Product product = productDao.findByIdForUpdate(conn, productId);
            if (product == null) {
                throw new ProductService.NotFoundException();
            }
            BigDecimal old = productUnitDao.findFactor(conn, productId, unitId);
            if (old == null) {
                throw new ServiceException("Đơn vị quy đổi không còn tồn tại. Vui lòng tải lại trang.");
            }
            productUnitDao.delete(conn, productId, unitId);
            String unitName = lookupDao.units(conn).stream().filter(u -> u.getId() == unitId)
                    .map(NamedItem::getName).findFirst().orElse("?");
            auditDao.log(conn, actor.getId(), "PRODUCT_UNIT_DELETE", "PRODUCT", productId, product.getSku(),
                    "1 " + unitName + " = " + text(old) + " " + product.getBaseUnitName(), null);
            return null;
        });
    }

    static BigDecimal parseFactor(String raw) {
        try {
            BigDecimal value = new BigDecimal(raw == null ? "" : raw.trim().replace(",", "."));
            if (value.signum() <= 0 || value.compareTo(MAX_FACTOR) > 0 || value.stripTrailingZeros().scale() > 4) {
                throw new NumberFormatException();
            }
            return value.setScale(4, RoundingMode.UNNECESSARY);
        } catch (NumberFormatException | ArithmeticException e) {
            throw new ServiceException("Hệ số quy đổi phải là số lớn hơn 0, tối đa 4 chữ số thập phân (ví dụ 24 hoặc 0,5).");
        }
    }

    private static String text(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
