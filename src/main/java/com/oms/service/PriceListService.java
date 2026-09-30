package com.oms.service;

import com.oms.config.Db;
import com.oms.dao.AuditLogDao;
import com.oms.dao.PriceListDao;
import com.oms.dao.ProductDao;
import com.oms.model.NamedItem;
import com.oms.model.PriceList;
import com.oms.model.PriceListItem;
import com.oms.model.Product;
import com.oms.model.User;
import com.oms.security.PermissionPolicy;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Bảng giá theo nhóm khách hàng (S2-10). Các bảng giá của cùng nhóm không chồng thời gian hiệu lực,
 * nên mỗi ngày đơn hàng tra được đúng một giá. Bảng đã phát sinh đơn (khoá) hoặc đã có phiên bản mới hơn
 * thì không sửa, chỉ tạo phiên bản mới.
 */
public class PriceListService {

    private static final Pattern CODE = Pattern.compile("^[A-Z0-9][A-Z0-9._-]{1,29}$");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PriceListDao priceListDao = new PriceListDao();
    private final ProductDao productDao = new ProductDao();
    private final AuditLogDao auditDao = new AuditLogDao();

    public List<PriceList> list(Long groupId) throws SQLException {
        try (Connection conn = Db.getConnection()) {
            return priceListDao.list(conn, groupId);
        }
    }

    public List<NamedItem> customerGroups() throws SQLException {
        try (Connection conn = Db.getConnection()) {
            return priceListDao.customerGroups(conn);
        }
    }

    /** Bảng giá kèm các dòng; giá vốn (để tính biên lợi nhuận) chỉ nạp cho người được xem giá vốn. */
    public PriceList get(long id, User viewer) throws SQLException {
        try (Connection conn = Db.getConnection()) {
            PriceList pl = priceListDao.findById(conn, id);
            if (pl == null) {
                throw new NotFoundException();
            }
            pl.setItems(priceListDao.items(conn, id, PermissionPolicy.canViewCostPrice(viewer.getRoles())));
            return pl;
        }
    }

    public List<NamedItem> productOptions() throws SQLException {
        try (Connection conn = Db.getConnection()) {
            return productDao.activeOptions(conn);
        }
    }

    public List<PriceList> versions(String code) throws SQLException {
        try (Connection conn = Db.getConnection()) {
            return priceListDao.versions(conn, code);
        }
    }

    public long create(PriceList input, User actor) throws SQLException {
        String code = normalizeCode(input.getCode());
        PriceList pl = validatedHeader(input);
        pl.setCode(code);
        pl.setVersion(1);
        return Db.inTransaction(conn -> {
            requireGroup(conn, pl.getCustomerGroupId());
            if (priceListDao.existsCode(conn, code)) {
                throw new ServiceException("Mã bảng giá \"" + code + "\" đã tồn tại.");
            }
            checkOverlap(conn, pl, List.of());
            long id = priceListDao.insert(conn, pl);
            auditDao.log(conn, actor.getId(), "PRICE_LIST_CREATE", "PRICE_LIST", id, code, null, describe(conn, pl));
            return id;
        });
    }

    public void updateHeader(long id, PriceList input, User actor) throws SQLException {
        PriceList changes = validatedHeader(input);
        Db.inTransaction(conn -> {
            PriceList existing = requireEditable(conn, id);
            requireGroup(conn, changes.getCustomerGroupId());
            changes.setId(id);
            checkOverlap(conn, changes, List.of(id));
            String before = describe(conn, existing);
            priceListDao.updateHeader(conn, changes);
            String after = describe(conn, priceListDao.findById(conn, id));
            if (!before.equals(after)) {
                auditDao.log(conn, actor.getId(), "PRICE_LIST_UPDATE", "PRICE_LIST", id, existing.getCode(), before, after);
            }
            return null;
        });
    }

    /** Thêm hoặc sửa một dòng giá. Giá sàn không được vượt giá bán; bán dưới giá sàn sẽ phải qua duyệt (EP-04). */
    public void saveItem(long listId, long productId, String saleText, String floorText, User actor) throws SQLException {
        BigDecimal sale = Money.parse(saleText, "Giá bán");
        BigDecimal floor = Money.parse(floorText, "Giá sàn");
        if (sale == null || floor == null) {
            throw new ServiceException("Vui lòng nhập cả giá bán và giá sàn.");
        }
        if (floor.compareTo(sale) > 0) {
            throw new ServiceException("Giá sàn không được cao hơn giá bán.");
        }
        Db.inTransaction(conn -> {
            PriceList pl = requireEditable(conn, listId);
            Product product = productDao.findById(conn, productId, false);
            if (product == null) {
                throw new ServiceException("Vui lòng chọn sản phẩm.");
            }
            PriceListItem old = priceListDao.findItem(conn, listId, productId);
            if (old == null && product.getStatus() != Product.Status.ACTIVE) {
                throw new ServiceException("Sản phẩm " + product.getSku() + " đã ngừng kinh doanh, không thêm vào bảng giá.");
            }
            if (old != null && old.getSalePrice().compareTo(sale) == 0 && old.getFloorPrice().compareTo(floor) == 0) {
                return null;
            }
            priceListDao.upsertItem(conn, listId, productId, sale, floor, old != null);
            auditDao.log(conn, actor.getId(), "PRICE_ITEM_SAVE", "PRICE_LIST", listId, pl.getCode() + " / " + product.getSku(),
                    old == null ? null : prices(old.getSalePrice(), old.getFloorPrice()), prices(sale, floor));
            return null;
        });
    }

    public void deleteItem(long listId, long productId, User actor) throws SQLException {
        Db.inTransaction(conn -> {
            PriceList pl = requireEditable(conn, listId);
            PriceListItem old = priceListDao.findItem(conn, listId, productId);
            if (old == null) {
                throw new ServiceException("Dòng giá không còn tồn tại. Vui lòng tải lại trang.");
            }
            priceListDao.deleteItem(conn, listId, productId);
            auditDao.log(conn, actor.getId(), "PRICE_ITEM_DELETE", "PRICE_LIST", listId, pl.getCode() + " / " + old.getSku(),
                    prices(old.getSalePrice(), old.getFloorPrice()), null);
            return null;
        });
    }

    /**
     * Tạo phiên bản mới từ phiên bản mới nhất: chép toàn bộ dòng giá, hiệu lực từ ngày chọn; phiên bản cũ
     * kết thúc hôm trước đó. Đơn đã ghi vẫn tham chiếu phiên bản cũ nên giá của đơn không đổi.
     */
    public long newVersion(long sourceId, LocalDate validFrom, User actor) throws SQLException {
        if (validFrom == null) {
            throw new ServiceException("Vui lòng chọn ngày bắt đầu hiệu lực của phiên bản mới.");
        }
        return Db.inTransaction(conn -> {
            PriceList source = priceListDao.findByIdForUpdate(conn, sourceId);
            if (source == null) {
                throw new NotFoundException();
            }
            if (source.isHasNewerVersion()) {
                throw new ServiceException("Bảng giá này đã có phiên bản mới hơn. Hãy tạo phiên bản từ bản mới nhất.");
            }
            if (!validFrom.isAfter(source.getValidFrom())) {
                throw new ServiceException("Phiên bản mới phải bắt đầu sau ngày " + source.getValidFromText() + ".");
            }
            PriceList next = new PriceList();
            next.setCode(source.getCode());
            next.setVersion(priceListDao.maxVersion(conn, source.getCode()) + 1);
            next.setName(source.getName());
            next.setCustomerGroupId(source.getCustomerGroupId());
            next.setValidFrom(validFrom);
            boolean sourceStillRunning = source.getValidTo() == null || !source.getValidTo().isBefore(validFrom);
            next.setValidTo(sourceStillRunning ? source.getValidTo() : null);
            next.setPreviousId(source.getId());
            checkOverlap(conn, next, List.of(source.getId()));

            long id = priceListDao.insert(conn, next);
            priceListDao.copyItems(conn, source.getId(), id);
            if (sourceStillRunning) {
                priceListDao.updateValidTo(conn, source.getId(), validFrom.minusDays(1));
            }
            auditDao.log(conn, actor.getId(), "PRICE_LIST_NEW_VERSION", "PRICE_LIST", id, source.getCode(),
                    "Phiên bản " + source.getVersion() + ": " + source.getValidFromText() + " → " + source.getValidToText(),
                    "Phiên bản " + next.getVersion() + " từ " + validFrom.format(DATE)
                            + (sourceStillRunning ? "; phiên bản " + source.getVersion() + " kết thúc " + validFrom.minusDays(1).format(DATE) : ""));
            return id;
        });
    }

    /** Giá áp dụng cho nhóm khách vào ngày cho trước; null nếu sản phẩm chưa có trong bảng giá hiệu lực (EP-04 dùng). */
    public PriceListItem effectivePrice(Connection conn, long groupId, long productId, LocalDate date) throws SQLException {
        return priceListDao.effectiveItem(conn, groupId, productId, date);
    }

    /** Đơn hàng gọi khi lần đầu dùng bảng giá: từ đó bảng giá không sửa được nữa. */
    public void lockForOrder(Connection conn, long priceListId) throws SQLException {
        priceListDao.lock(conn, priceListId);
    }

    private PriceList requireEditable(Connection conn, long id) throws SQLException {
        PriceList pl = priceListDao.findByIdForUpdate(conn, id);
        if (pl == null) {
            throw new NotFoundException();
        }
        if (pl.isLocked()) {
            throw new ServiceException("Bảng giá đã phát sinh đơn hàng nên không sửa được. Hãy tạo phiên bản mới.");
        }
        if (pl.isHasNewerVersion()) {
            throw new ServiceException("Bảng giá đã có phiên bản mới hơn nên không sửa được bản cũ.");
        }
        return pl;
    }

    private void checkOverlap(Connection conn, PriceList pl, List<Long> exclude) throws SQLException {
        PriceList other = priceListDao.findOverlap(conn, pl.getCustomerGroupId(), pl.getValidFrom(), pl.getValidTo(), new ArrayList<>(exclude));
        if (other != null) {
            throw new ServiceException("Khoảng hiệu lực trùng với bảng giá " + other.getCode() + " (phiên bản " + other.getVersion()
                    + ", " + other.getValidFromText() + " → " + other.getValidToText() + ") của cùng nhóm khách hàng.");
        }
    }

    private void requireGroup(Connection conn, long groupId) throws SQLException {
        if (priceListDao.customerGroups(conn).stream().noneMatch(g -> g.getId() == groupId)) {
            throw new ServiceException("Vui lòng chọn nhóm khách hàng.");
        }
    }

    private static PriceList validatedHeader(PriceList in) {
        String name = in.getName() == null ? "" : in.getName().trim();
        if (name.isEmpty()) {
            throw new ServiceException("Vui lòng nhập tên bảng giá.");
        }
        if (name.length() > 150) {
            throw new ServiceException("Tên bảng giá tối đa 150 ký tự.");
        }
        if (in.getValidFrom() == null) {
            throw new ServiceException("Vui lòng chọn ngày bắt đầu hiệu lực.");
        }
        if (in.getValidTo() != null && in.getValidTo().isBefore(in.getValidFrom())) {
            throw new ServiceException("Ngày kết thúc phải sau hoặc bằng ngày bắt đầu.");
        }
        PriceList pl = new PriceList();
        pl.setName(name);
        pl.setCustomerGroupId(in.getCustomerGroupId());
        pl.setValidFrom(in.getValidFrom());
        pl.setValidTo(in.getValidTo());
        return pl;
    }

    static String normalizeCode(String raw) {
        String code = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if (!CODE.matcher(code).matches()) {
            throw new ServiceException("Mã bảng giá gồm 2–30 ký tự: chữ không dấu, số và . _ - (tự chuyển thành chữ hoa).");
        }
        return code;
    }

    private String describe(Connection conn, PriceList pl) throws SQLException {
        String group = priceListDao.customerGroups(conn).stream().filter(g -> g.getId() == pl.getCustomerGroupId())
                .map(NamedItem::getName).findFirst().orElse("?");
        return "Tên: " + pl.getName() + "\nNhóm khách hàng: " + group
                + "\nHiệu lực: " + pl.getValidFrom().format(DATE) + " → "
                + (pl.getValidTo() == null ? "Không thời hạn" : pl.getValidTo().format(DATE));
    }

    private static String prices(BigDecimal sale, BigDecimal floor) {
        return "Giá bán: " + Money.text(sale) + "\nGiá sàn: " + Money.text(floor);
    }

    public static class NotFoundException extends ServiceException {
        public NotFoundException() {
            super("Không tìm thấy bảng giá.");
        }
    }
}
