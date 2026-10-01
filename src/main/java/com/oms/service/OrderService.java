package com.oms.service;

import com.oms.config.Db;
import com.oms.dao.AuditLogDao;
import com.oms.dao.CustomerDao;
import com.oms.dao.DeliveryPointDao;
import com.oms.dao.OrderDao;
import com.oms.dao.ProductDao;
import com.oms.dao.ProductUnitDao;
import com.oms.model.Customer;
import com.oms.model.DeliveryPoint;
import com.oms.model.DiscountPolicy;
import com.oms.model.NamedItem;
import com.oms.model.Order;
import com.oms.model.OrderLine;
import com.oms.model.PageResult;
import com.oms.model.PriceListItem;
import com.oms.model.Product;
import com.oms.model.ProductUnit;
import com.oms.model.User;
import com.oms.security.PermissionPolicy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Đơn hàng nháp của đại lý (S3-09). Giá lấy từ bảng giá của nhóm khách hàng đang hiệu lực hôm nay, chiết khấu theo
 * {@link DiscountService#best} trên tổng số lượng (đơn vị cơ sở) của từng sản phẩm trong đơn. Đơn nháp luôn được
 * tính lại theo giá và chính sách hiện hành; chốt đơn, kiểm tồn, hạn mức công nợ và giá sàn thuộc Sprint 4 (EP-04).
 */
public class OrderService {

    public static final int PAGE_SIZE = 10;
    public static final int MAX_LINES = 100;
    public static final int MAX_QUANTITY = 1_000_000;
    private static final int PRODUCT_RESULTS = 8;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Sản phẩm tìm được khi thêm dòng hàng: giá theo đơn vị cơ sở (null nếu chưa có giá) và các đơn vị chọn được. */
    public record ProductChoice(long productId, String sku, String name, String baseUnitName, BigDecimal basePrice,
                                List<NamedItem> units) {
    }

    private final OrderDao orderDao = new OrderDao();
    private final CustomerDao customerDao = new CustomerDao();
    private final DeliveryPointDao pointDao = new DeliveryPointDao();
    private final ProductDao productDao = new ProductDao();
    private final ProductUnitDao productUnitDao = new ProductUnitDao();
    private final UnitConversionService unitService = new UnitConversionService();
    private final PriceListService priceListService = new PriceListService();
    private final DiscountService discountService = new DiscountService();
    private final AuditLogDao auditDao = new AuditLogDao();

    public PageResult<Order> search(String keyword, int page, User viewer) throws SQLException {
        requireOrders(viewer);
        Long own = PermissionPolicy.canSeeAllOrders(viewer.getRoles()) ? null : viewer.getId();
        try (Connection conn = Db.getConnection()) {
            PageResult<Order> result = orderDao.search(conn, keyword, own, Math.max(1, page), PAGE_SIZE);
            for (Order o : result.getItems()) {
                o.setLines(orderDao.lines(conn, o.getId()));
                price(conn, o);
            }
            return result;
        }
    }

    /** Đơn kèm các dòng và tổng tiền; NotFoundException nếu không có hoặc người xem không thấy đơn của đại lý đó. */
    public Order get(long id, User viewer) throws SQLException {
        requireOrders(viewer);
        try (Connection conn = Db.getConnection()) {
            Order order = orderDao.findById(conn, id);
            requireVisible(order, viewer);
            order.setLines(orderDao.lines(conn, id));
            price(conn, order);
            return order;
        }
    }

    public List<DeliveryPoint> deliveryPoints(long customerId) throws SQLException {
        try (Connection conn = Db.getConnection()) {
            return pointDao.listByCustomer(conn, customerId);
        }
    }

    /** Tìm sản phẩm đang kinh doanh theo mã hoặc tên để thêm dòng; kèm giá hiện hành của nhóm khách hàng trong đơn. */
    public List<ProductChoice> searchProducts(Order order, String keyword) throws SQLException {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        try (Connection conn = Db.getConnection()) {
            List<Product> found = productDao.search(conn, keyword, null, Product.Status.ACTIVE, 1, PRODUCT_RESULTS, false).getItems();
            Map<Long, List<ProductUnit>> conversions = productUnitDao.listByProducts(conn, found.stream().map(Product::getId).toList());
            List<ProductChoice> choices = new ArrayList<>();
            for (Product p : found) {
                PriceListItem price = priceListService.effectivePrice(conn, order.getCustomerGroupId(), p.getId(), LocalDate.now());
                List<NamedItem> units = new ArrayList<>();
                units.add(new NamedItem(p.getBaseUnitId(), p.getBaseUnitName()));
                for (ProductUnit pu : conversions.getOrDefault(p.getId(), List.of())) {
                    units.add(new NamedItem(pu.getUnitId(), pu.getUnitName() + " (= " + pu.getFactorText() + " " + p.getBaseUnitName() + ")"));
                }
                choices.add(new ProductChoice(p.getId(), p.getSku(), p.getName(), p.getBaseUnitName(),
                        price == null ? null : price.getSalePrice(), units));
            }
            return choices;
        }
    }

    /** Đơn vị chọn được (đơn vị cơ sở và các đơn vị quy đổi) của từng sản phẩm có dòng trong đơn, để đổi đơn vị ngay trên dòng. */
    public Map<Long, List<NamedItem>> unitOptions(Order order) throws SQLException {
        Map<Long, List<NamedItem>> options = new HashMap<>();
        if (order.getLines().isEmpty()) {
            return options;
        }
        try (Connection conn = Db.getConnection()) {
            List<Long> productIds = order.getLines().stream().map(OrderLine::getProductId).distinct().toList();
            Map<Long, List<ProductUnit>> conversions = productUnitDao.listByProducts(conn, productIds);
            for (long productId : productIds) {
                Product p = productDao.findById(conn, productId, false);
                List<NamedItem> units = new ArrayList<>();
                units.add(new NamedItem(p.getBaseUnitId(), p.getBaseUnitName()));
                for (ProductUnit pu : conversions.getOrDefault(productId, List.of())) {
                    units.add(new NamedItem(pu.getUnitId(), pu.getUnitName()));
                }
                options.put(productId, units);
            }
        }
        return options;
    }

    /** Tạo đơn nháp rỗng cho đại lý, chọn sẵn điểm giao mặc định. Đại lý bị khoá giao dịch thì không tạo được. */
    public long create(long customerId, User actor) throws SQLException {
        requireOrders(actor);
        return Db.inTransaction(conn -> {
            Customer customer = customerDao.findById(conn, customerId);
            if (customer == null || !canSeeCustomer(actor, customer)) {
                throw new NotFoundException();
            }
            CustomerLockService.requireTradable(customer);
            Long point = null;
            for (DeliveryPoint p : pointDao.listByCustomer(conn, customerId)) {
                if (point == null || p.isDefaultPoint()) {
                    point = p.getId();
                }
            }
            long id = orderDao.insert(conn, customerId, point, actor.getId());
            String code = String.format("DH%06d", id);
            orderDao.setCode(conn, id, code);
            auditDao.log(conn, actor.getId(), "ORDER_CREATE", "ORDER", id, code, null, "Đại lý: " + customer.getCode() + " – " + customer.getName());
            return id;
        });
    }

    /** Điểm giao (có thể để trống khi còn nháp) phải thuộc đại lý của đơn; ngày giao không được ở quá khứ. */
    public void saveHeader(long orderId, Long pointId, LocalDate deliveryDate, String rawNote, User actor) throws SQLException {
        requireOrders(actor);
        String note = rawNote == null || rawNote.isBlank() ? null : rawNote.trim();
        if (note != null && note.length() > 500) {
            throw new ServiceException("Ghi chú tối đa 500 ký tự.");
        }
        if (deliveryDate != null && deliveryDate.isBefore(LocalDate.now())) {
            throw new ServiceException("Ngày giao mong muốn không được ở quá khứ.");
        }
        Db.inTransaction(conn -> {
            Order order = lockVisible(conn, orderId, actor);
            if (pointId != null) {
                DeliveryPoint point = pointDao.findById(conn, pointId);
                if (point == null || point.getCustomerId() != order.getCustomerId()) {
                    throw new ServiceException("Điểm giao hàng không thuộc đại lý của đơn này.");
                }
            }
            String before = describeHeader(order);
            orderDao.updateHeader(conn, orderId, pointId, deliveryDate, note);
            Order after = orderDao.findById(conn, orderId);
            if (!before.equals(describeHeader(after))) {
                auditDao.log(conn, actor.getId(), "ORDER_UPDATE", "ORDER", orderId, order.getCode(), before, describeHeader(after));
            }
            return null;
        });
    }

    /** Thêm dòng; sản phẩm và đơn vị đã có trong đơn thì cộng dồn số lượng vào dòng đó. */
    public void addLine(long orderId, long productId, long unitId, String quantityText, User actor) throws SQLException {
        requireOrders(actor);
        int quantity = parseQuantity(quantityText);
        Db.inTransaction(conn -> {
            Order order = lockVisible(conn, orderId, actor);
            Product product = productDao.findById(conn, productId, false);
            if (product == null) {
                throw new ServiceException("Vui lòng chọn sản phẩm.");
            }
            if (product.getStatus() != Product.Status.ACTIVE) {
                throw new ServiceException("Sản phẩm " + product.getSku() + " đã ngừng kinh doanh, không thêm vào đơn được.");
            }
            if (priceListService.effectivePrice(conn, order.getCustomerGroupId(), productId, LocalDate.now()) == null) {
                throw new ServiceException("Sản phẩm " + product.getSku() + " chưa có giá trong bảng giá đang hiệu lực của nhóm "
                        + order.getCustomerGroupName() + " nên chưa thêm vào đơn được.");
            }
            OrderLine existing = orderDao.findLine(conn, orderId, productId, unitId);
            if (existing != null) {
                setQuantity(conn, existing, unitId, existing.getQuantity() + (long) quantity);
            } else {
                if (orderDao.lines(conn, orderId).size() >= MAX_LINES) {
                    throw new ServiceException("Một đơn có tối đa " + MAX_LINES + " dòng hàng.");
                }
                UnitConversionService.Converted c = unitService.toBase(conn, productId, unitId, BigDecimal.valueOf(quantity));
                orderDao.insertLine(conn, orderId, productId, unitId, quantity, c.factor(), c.baseQuantity());
            }
            orderDao.touch(conn, orderId);
            return null;
        });
    }

    /** Đổi đơn vị và / hoặc số lượng của một dòng. */
    public void updateLine(long orderId, long lineId, long unitId, String quantityText, User actor) throws SQLException {
        requireOrders(actor);
        int quantity = parseQuantity(quantityText);
        Db.inTransaction(conn -> {
            lockVisible(conn, orderId, actor);
            OrderLine line = requireLine(conn, orderId, lineId);
            if (unitId != line.getUnitId() && orderDao.findLine(conn, orderId, line.getProductId(), unitId) != null) {
                throw new ServiceException("Đơn đã có dòng " + line.getSku() + " với đơn vị này. Hãy sửa số lượng ở dòng đó.");
            }
            setQuantity(conn, line, unitId, quantity);
            orderDao.touch(conn, orderId);
            return null;
        });
    }

    public void deleteLine(long orderId, long lineId, User actor) throws SQLException {
        requireOrders(actor);
        Db.inTransaction(conn -> {
            lockVisible(conn, orderId, actor);
            requireLine(conn, orderId, lineId);
            orderDao.deleteLine(conn, lineId);
            orderDao.touch(conn, orderId);
            return null;
        });
    }

    public void delete(long orderId, User actor) throws SQLException {
        requireOrders(actor);
        Db.inTransaction(conn -> {
            Order order = lockVisible(conn, orderId, actor);
            orderDao.delete(conn, orderId);
            auditDao.log(conn, actor.getId(), "ORDER_DELETE", "ORDER", orderId, order.getCode(),
                    "Đại lý: " + order.getCustomerCode() + " – " + order.getCustomerName() + "\nSố dòng hàng: " + order.getLineCount(), null);
            return null;
        });
    }

    private void setQuantity(Connection conn, OrderLine line, long unitId, long quantity) throws SQLException {
        if (quantity > MAX_QUANTITY) {
            throw new ServiceException("Số lượng tối đa " + MAX_QUANTITY + " cho mỗi dòng.");
        }
        UnitConversionService.Converted c = unitService.toBase(conn, line.getProductId(), unitId, BigDecimal.valueOf(quantity));
        orderDao.updateLine(conn, line.getId(), unitId, (int) quantity, c.factor(), c.baseQuantity());
    }

    private OrderLine requireLine(Connection conn, long orderId, long lineId) throws SQLException {
        OrderLine line = orderDao.findLine(conn, orderId, lineId);
        if (line == null) {
            throw new ServiceException("Dòng hàng không còn tồn tại. Vui lòng tải lại trang.");
        }
        return line;
    }

    private Order lockVisible(Connection conn, long orderId, User viewer) throws SQLException {
        Order order = orderDao.findByIdForUpdate(conn, orderId);
        requireVisible(order, viewer);
        return order;
    }

    private static void requireVisible(Order order, User viewer) {
        boolean visible = order != null && (PermissionPolicy.canSeeAllOrders(viewer.getRoles())
                || (order.getCustomerSalesRepId() != null && order.getCustomerSalesRepId() == viewer.getId()));
        if (!visible) {
            throw new NotFoundException();
        }
    }

    private static boolean canSeeCustomer(User viewer, Customer c) {
        return PermissionPolicy.canSeeAllOrders(viewer.getRoles())
                || (c.getSalesRepId() != null && c.getSalesRepId() == viewer.getId());
    }

    private static void requireOrders(User user) {
        if (!PermissionPolicy.canTakeOrders(user.getRoles())) {
            throw new ForbiddenException("Chỉ Nhân viên kinh doanh và Quản lý kinh doanh được lập đơn hàng.");
        }
    }

    private static int parseQuantity(String text) {
        String digits = text == null ? "" : text.trim().replace(".", "").replace(" ", "");
        try {
            int qty = Integer.parseInt(digits);
            if (qty < 1 || qty > MAX_QUANTITY) {
                throw new NumberFormatException();
            }
            return qty;
        } catch (NumberFormatException e) {
            throw new ServiceException("Số lượng phải là số nguyên từ 1 đến " + MAX_QUANTITY + ".");
        }
    }

    /**
     * Tính tiền các dòng theo giá hiện hành: tiền hàng = số lượng cơ sở × giá cơ sở; chiết khấu = số lượng cơ sở × mức
     * giảm trên mỗi đơn vị của chính sách có lợi nhất, xét trên tổng số lượng cơ sở của sản phẩm đó trong đơn.
     * Dòng của sản phẩm chưa (hoặc không còn) có giá thì để trống tiền và không vào tổng.
     */
    private void price(Connection conn, Order order) throws SQLException {
        LocalDate today = LocalDate.now();
        Map<Long, BigDecimal> totalBase = new HashMap<>();
        for (OrderLine l : order.getLines()) {
            totalBase.merge(l.getProductId(), l.getBaseQuantity(), BigDecimal::add);
        }
        Map<Long, PriceListItem> prices = new HashMap<>();
        Map<Long, DiscountService.Applied> discounts = new HashMap<>();
        for (Map.Entry<Long, BigDecimal> e : totalBase.entrySet()) {
            PriceListItem price = priceListService.effectivePrice(conn, order.getCustomerGroupId(), e.getKey(), today);
            if (price != null) {
                prices.put(e.getKey(), price);
                DiscountService.Applied applied = discountService.best(conn, e.getKey(), e.getValue(), price.getSalePrice(), today);
                if (applied != null) {
                    discounts.put(e.getKey(), applied);
                }
            }
        }
        BigDecimal gross = BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;
        int unpriced = 0;
        for (OrderLine l : order.getLines()) {
            PriceListItem price = prices.get(l.getProductId());
            if (price == null) {
                unpriced++;
                continue;
            }
            l.setUnitPrice(price.getSalePrice().multiply(l.getFactor()).setScale(2, RoundingMode.HALF_UP));
            l.setGrossAmount(l.getBaseQuantity().multiply(price.getSalePrice()).setScale(2, RoundingMode.HALF_UP));
            DiscountService.Applied applied = discounts.get(l.getProductId());
            if (applied == null) {
                l.setDiscountAmount(BigDecimal.ZERO);
            } else {
                l.setDiscountAmount(l.getBaseQuantity().multiply(applied.perUnit()).setScale(2, RoundingMode.HALF_UP));
                l.setDiscountNote(applied.policyName() + ": " + DiscountPolicy.valueText(applied.type(), applied.value())
                        + " (mua từ " + applied.tierMinQty() + ")");
            }
            gross = gross.add(l.getGrossAmount());
            discount = discount.add(l.getDiscountAmount());
        }
        order.setGrossTotal(gross);
        order.setDiscountTotal(discount);
        order.setUnpricedLines(unpriced);
    }

    private static String describeHeader(Order o) {
        return "Điểm giao: " + (o.getDeliveryPointText() == null ? "chưa chọn" : o.getDeliveryPointText())
                + "\nNgày giao mong muốn: " + (o.getDeliveryDate() == null ? "chưa chọn" : o.getDeliveryDate().format(DATE))
                + "\nGhi chú: " + (o.getNote() == null ? "—" : o.getNote());
    }

    public static class NotFoundException extends ServiceException {
        public NotFoundException() {
            super("Không tìm thấy đơn hàng.");
        }
    }
}
