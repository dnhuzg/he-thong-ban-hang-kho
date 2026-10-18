
package com.oms.service;

import com.oms.config.Db;
import com.oms.dao.AuditLogDao;
import com.oms.dao.CustomerDao;
import com.oms.dao.DeliveryPointDao;
import com.oms.dao.OrderDao;
import com.oms.dao.OrderWorkflowDao;
import com.oms.dao.ProductDao;
import com.oms.dao.ProductUnitDao;
import com.oms.dao.PortalAccountDao;
import com.oms.model.Customer;
import com.oms.model.DeliveryPoint;
import com.oms.model.DiscountPolicy;
import com.oms.model.NamedItem;
import com.oms.model.Order;
import com.oms.model.OrderLine;
import com.oms.model.OrderStats;
import com.oms.model.PageResult;
import com.oms.model.PriceListItem;
import com.oms.model.Product;
import com.oms.model.ProductUnit;
import com.oms.model.Role;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
public class OrderService {

    public static final int PAGE_SIZE = 10;
    public static final int MAX_LINES = 100;
    public static final int MAX_QUANTITY = 1_000_000;
    private static final int PRODUCT_RESULTS = 8;
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public record ProductChoice(
            long productId,
            String sku,
            String name,
            String baseUnitName,
            BigDecimal basePrice,
            List<NamedItem> units
    ) {
        public long getProductId() { return productId; }
        public String getSku() { return sku; }
        public String getName() { return name; }
        public String getBaseUnitName() { return baseUnitName; }
        public BigDecimal getBasePrice() { return basePrice; }
        public List<NamedItem> getUnits() { return units; }
    }
    public record ReorderResult(
            long newOrderId,
            String message
    ) {
    }
    private final OrderDao orderDao = new OrderDao();
    private final CustomerDao customerDao = new CustomerDao();
    private final DeliveryPointDao pointDao = new DeliveryPointDao();
    private final ProductDao productDao = new ProductDao();
    private final ProductUnitDao productUnitDao = new ProductUnitDao();
    private final UnitConversionService unitService =
            new UnitConversionService();
    private final PriceListService priceListService =
            new PriceListService();
    private final DiscountService discountService =
            new DiscountService();
    private final AuditLogDao auditDao = new AuditLogDao();
    public OrderStats stats(User viewer, LocalDate today)
            throws SQLException {
        requireOrders(viewer);
        Long own = PermissionPolicy.canSeeAllOrders(viewer.getRoles())
                ? null : viewer.getId();
        try (Connection conn = Db.getConnection()) {
            return orderDao.stats(conn, own, today);
        }
    }
    public PageResult<Order> search(
            String keyword, int page, User viewer
    ) throws SQLException {
        if (!PermissionPolicy.canTakeOrders(viewer.getRoles())) {
            throw new ForbiddenException(
                    "Danh sách này chỉ dành cho nhân viên kinh doanh."
            );
        }
        Long own = PermissionPolicy.canSeeAllOrders(viewer.getRoles())
                ? null : viewer.getId();
        try (Connection conn = Db.getConnection()) {
            PageResult<Order> result = orderDao.search(
                    conn, keyword, own, Math.max(1, page), PAGE_SIZE
            );
            for (Order order : result.getItems()) {
                order.setLines(orderDao.lines(conn, order.getId()));
                price(conn, order);
            }
            return result;
        }
    }
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
    public List<DeliveryPoint> deliveryPoints(long customerId)
            throws SQLException {
        try (Connection conn = Db.getConnection()) {
            return pointDao.listByCustomer(conn, customerId);
        }
    }
    public List<ProductChoice> searchProducts(
            Order order, String keyword
    ) throws SQLException {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        try (Connection conn = Db.getConnection()) {
            List<Product> found = productDao.search(
                    conn,
                    keyword,
                    null,
                    Product.Status.ACTIVE,
                    1,
                    PRODUCT_RESULTS,
                    false
            ).getItems();
            Map<Long, List<ProductUnit>> conversions =
                    productUnitDao.listByProducts(
                            conn,
                            found.stream().map(Product::getId).toList()
                    );
            List<ProductChoice> choices = new ArrayList<>();
            for (Product product : found) {
                PriceListItem currentPrice =
                        priceListService.effectivePrice(
                                conn,
                                order.getCustomerGroupId(),
                                product.getId(),
                                LocalDate.now()
                        );
                List<NamedItem> units = new ArrayList<>();
                units.add(new NamedItem(
                        product.getBaseUnitId(),
                        product.getBaseUnitName()
                ));
                for (ProductUnit unit : conversions.getOrDefault(
                        product.getId(), List.of())) {
                    units.add(new NamedItem(
                            unit.getUnitId(),
                            unit.getUnitName()
                                    + " (= "
                                    + unit.getFactorText()
                                    + " "
                                    + product.getBaseUnitName()
                                    + ")"
                    ));
                }
                choices.add(new ProductChoice(
                        product.getId(),
                        product.getSku(),
                        product.getName(),
                        product.getBaseUnitName(),
                        currentPrice == null
                                ? null
                                : currentPrice.getSalePrice(),
                        units
                ));
            }
            return choices;
        }
    }
    public Map<Long, List<NamedItem>> unitOptions(Order order)
            throws SQLException {
        Map<Long, List<NamedItem>> options = new HashMap<>();
        if (order.getLines().isEmpty()) {
            return options;
        }
        try (Connection conn = Db.getConnection()) {
            List<Long> productIds = order.getLines()
                    .stream()
                    .map(OrderLine::getProductId)
                    .distinct()
                    .toList();
            Map<Long, List<ProductUnit>> conversions =
                    productUnitDao.listByProducts(conn, productIds);
            for (long productId : productIds) {
                Product product =
                        productDao.findById(conn, productId, false);
                List<NamedItem> units = new ArrayList<>();
                units.add(new NamedItem(
                        product.getBaseUnitId(),
                        product.getBaseUnitName()
                ));
                for (ProductUnit unit :
                        conversions.getOrDefault(productId, List.of())) {
                    units.add(new NamedItem(
                            unit.getUnitId(),
                            unit.getUnitName()
                    ));
                }

                options.put(productId, units);
            }
        }
        return options;
    }
    public long create(long customerId, User actor)
            throws SQLException {

        requireOrders(actor);

        return Db.inTransaction(
                conn -> create(conn, customerId, actor)
        );
    }
    long create(Connection conn, long customerId, User actor)
            throws SQLException {
        Customer customer =
                customerDao.findByIdForUpdate(conn, customerId);
        if (customer == null
                || !canSeeCustomer(conn, actor, customer)) {
            throw new NotFoundException();
        }
        CustomerLockService.requireTradable(customer);
        new OrderCreditService().requireNotOverdue(conn, customer);
        Long point = null;
        for (DeliveryPoint p :
                pointDao.listByCustomer(conn, customerId)) {
            if (point == null || p.isDefaultPoint()) {
                point = p.getId();
            }
        }
        long id = orderDao.insert(
                conn,
                customerId,
                point,
                actor.getId()
        );
        String code = String.format("DH%06d", id);
        orderDao.setCode(conn, id, code);
        auditDao.log(
                conn,
                actor.getId(),
                "ORDER_CREATE",
                "ORDER",
                id,
                code,
                null,
                "Đại lý: "
                        + customer.getCode()
                        + " – "
                        + customer.getName()
        );
        return id;
    }
    public ReorderResult reorder(
            long oldOrderId,
            long[] selectedLineIds,
            User actor
    ) throws SQLException {
        if (actor == null || !actor.hasRole(Role.CUSTOMER)) {
            throw new ForbiddenException(
                    "Chỉ Đại lý được phép đặt lại đơn hàng của mình."
            );
        }
        if (selectedLineIds == null
                || selectedLineIds.length == 0) {
            throw new ServiceException(
                    "Vui lòng chọn ít nhất một dòng hàng để đặt lại."
            );
        }
        return Db.inTransaction(conn -> {
            Order oldOrder = orderDao.findById(
                    conn,
                    oldOrderId
            );
            if (oldOrder == null
                    || !new PortalAccountDao().owns(
                            conn,
                            oldOrder.getCustomerId(),
                            actor.getId()
                    )) {
                throw new NotFoundException();
            }
            if (oldOrder.getStatus() == Order.Status.DRAFT) {
                throw new ServiceException(
                        "Không thể đặt lại từ đơn hàng đang ở trạng thái Nháp."
                );
            }
            List<OrderLine> oldLines =
                    orderDao.lines(conn, oldOrderId);
            Set<Long> selectedIds = new LinkedHashSet<>();
            for (long lineId : selectedLineIds) {
                if (lineId <= 0) {
                    throw new ServiceException(
                            "Dòng hàng không hợp lệ."
                    );
                }
                boolean found = false;
                for (OrderLine line : oldLines) {
                    if (line.getId() == lineId) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw new NotFoundException();
                }
                selectedIds.add(lineId);
            }
            long newOrderId = create(
                    conn,
                    oldOrder.getCustomerId(),
                    actor
            );
            Order newOrder =
                    orderDao.findById(conn, newOrderId);
            int added = 0;
            List<String> skipped = new ArrayList<>();
            for (OrderLine oldLine : oldLines) {
                if (!selectedIds.contains(oldLine.getId())) {
                    continue;
                }
                Product product = productDao.findById(
                        conn,
                        oldLine.getProductId(),
                        false
                );
                if (product == null
                        || product.getStatus()
                        != Product.Status.ACTIVE) {

                    skipped.add(
                            oldLine.getSku()
                                    + " đã ngừng kinh doanh hoặc không tồn tại"
                    );
                    continue;
                }
                PriceListItem currentPrice =
                        priceListService.effectivePrice(
                                conn,
                                newOrder.getCustomerGroupId(),
                                oldLine.getProductId(),
                                LocalDate.now()
                        );
                if (currentPrice == null) {
                    skipped.add(
                            oldLine.getSku()
                                    + " chưa có giá hiện hành"
                    );
                    continue;
                }
                addLine(
                        conn,
                        newOrder,
                        oldLine.getProductId(),
                        oldLine.getUnitId(),
                        oldLine.getQuantity()
                );
                added++;
            }
            if (added == 0) {
                throw new ServiceException(
                        "Không có mặt hàng nào có thể đặt lại trong đơn này."
                );
            }
            orderDao.touch(conn, newOrderId);
            String newCode =
                    String.format("DH%06d", newOrderId);
            auditDao.log(
                    conn,
                    actor.getId(),
                    "ORDER_UPDATE",
                    "ORDER",
                    newOrderId,
                    newCode,
                    null,
                    "Đặt lại từ đơn "
                            + oldOrder.getCode()
                            + ", gồm "
                            + added
                            + " dòng hàng."
            );
            String message =
                    "Đã tạo đơn mới "
                            + newCode
                            + " từ đơn "
                            + oldOrder.getCode()
                            + ". Giá và chiết khấu áp dụng theo chính sách hiện hành.";
            if (!skipped.isEmpty()) {
                message += " Các mặt hàng bị bỏ qua: "
                        + String.join("; ", skipped)
                        + ".";
            }
            return new ReorderResult(
                    newOrderId,
                    message
            );
        });
    }
    public void saveHeader(
            long orderId,
            Long pointId,
            LocalDate deliveryDate,
            String rawNote,
            User actor
    ) throws SQLException {
        requireOrders(actor);
        String note = rawNote == null || rawNote.isBlank()
                ? null : rawNote.trim();
        if (note != null && note.length() > 500) {
            throw new ServiceException(
                    "Ghi chú tối đa 500 ký tự."
            );
        }
        if (deliveryDate != null
                && deliveryDate.isBefore(LocalDate.now())) {
            throw new ServiceException(
                    "Ngày giao mong muốn không được ở quá khứ."
            );
        }
        Db.inTransaction(conn -> {
            Order order = lockVisible(
                    conn,
                    orderId,
                    actor
            );
            if (pointId != null) {
                DeliveryPoint point =
                        pointDao.findById(conn, pointId);
                if (point == null
                        || point.getCustomerId()
                        != order.getCustomerId()) {
                    throw new ServiceException(
                            "Điểm giao hàng không thuộc đại lý của đơn này."
                    );
                }
            }
            String before = describeHeader(order);
            orderDao.updateHeader(
                    conn,
                    orderId,
                    pointId,
                    deliveryDate,
                    note
            );
            Order after =
                    orderDao.findById(conn, orderId);
            if (!before.equals(describeHeader(after))) {
                auditDao.log(
                        conn,
                        actor.getId(),
                        "ORDER_UPDATE",
                        "ORDER",
                        orderId,
                        order.getCode(),
                        before,
                        describeHeader(after)
                );
            }
            return null;
        });
    }
    public void addLine(
            long orderId,
            long productId,
            long unitId,
            String quantityText,
            User actor
    ) throws SQLException {
        requireOrders(actor);
        int quantity = parseQuantity(quantityText);
        Db.inTransaction(conn -> {
            Order order = lockVisible(
                    conn,
                    orderId,
                    actor
            );
            addLine(
                    conn,
                    order,
                    productId,
                    unitId,
                    quantity
            );
            orderDao.touch(conn, orderId);

            return null;
        });
    }
    void addLine(
            Connection conn,
            Order order,
            long productId,
            long unitId,
            int quantity
    ) throws SQLException {
        if (quantity < 1 || quantity > MAX_QUANTITY) {
            throw new ServiceException(
                    "Số lượng không hợp lệ."
            );
        }
        Product product =
                productDao.findById(conn, productId, false);
        if (product == null
                || product.getStatus()
                != Product.Status.ACTIVE) {
            throw new ServiceException(
                    "Sản phẩm không tồn tại hoặc đã ngừng kinh doanh."
            );
        }
        if (priceListService.effectivePrice(
                conn,
                order.getCustomerGroupId(),
                productId,
                LocalDate.now()
        ) == null) {
            throw new ServiceException(
                    "Sản phẩm "
                            + product.getSku()
                            + " chưa có giá trong bảng giá đang hiệu lực."
            );
        }
        OrderLine existing = orderDao.findLine(
                conn,
                order.getId(),
                productId,
                unitId
        );
        if (existing != null) {
            setQuantity(
                    conn,
                    existing,
                    unitId,
                    existing.getQuantity() + (long) quantity
            );
        } else {
            if (orderDao.lines(conn, order.getId()).size()
                    >= MAX_LINES) {
                throw new ServiceException(
                        "Một đơn có tối đa "
                                + MAX_LINES
                                + " dòng hàng."
                );
            }
            UnitConversionService.Converted converted =
                    unitService.toBase(
                            conn,
                            productId,
                            unitId,
                            BigDecimal.valueOf(quantity)
                    );
            orderDao.insertLine(
                    conn,
                    order.getId(),
                    productId,
                    unitId,
                    quantity,
                    converted.factor(),
                    converted.baseQuantity()
            );
        }
    }
    public void updateLine(
            long orderId,
            long lineId,
            long unitId,
            String quantityText,
            User actor
    ) throws SQLException {
        requireOrders(actor);
        int quantity = parseQuantity(quantityText);
        Db.inTransaction(conn -> {
            lockVisible(conn, orderId, actor);
            OrderLine line =
                    requireLine(conn, orderId, lineId);
            if (unitId != line.getUnitId()
                    && orderDao.findLine(
                            conn,
                            orderId,
                            line.getProductId(),
                            unitId
                    ) != null) {
                throw new ServiceException(
                        "Đơn đã có dòng "
                                + line.getSku()
                                + " với đơn vị này. Hãy sửa số lượng ở dòng đó."
                );
            }
            setQuantity(conn, line, unitId, quantity);
            orderDao.touch(conn, orderId);
            return null;
        });
    }
    public void updateLine(
            long orderId,
            long lineId,
            long unitId,
            String quantityText,
            String manualPriceText,
            User actor
    ) throws SQLException {
        requireOrders(actor);
        int quantity = parseQuantity(quantityText);
        BigDecimal manualBasePrice =
                Money.parse(
                        manualPriceText,
                        "Giá chỉnh thủ công"
                );
        if (manualBasePrice != null
                && !PermissionPolicy.canTakeOrders(
                        actor.getRoles()
                )) {
            throw new ForbiddenException(
                    "Chỉ Nhân viên kinh doanh và Quản lý kinh doanh được chỉnh giá dòng hàng."
            );
        }
        Db.inTransaction(conn -> {
            lockVisible(conn, orderId, actor);
            OrderLine line =
                    requireLine(conn, orderId, lineId);
            if (unitId != line.getUnitId()
                    && orderDao.findLine(
                            conn,
                            orderId,
                            line.getProductId(),
                            unitId
                    ) != null) {
                throw new ServiceException(
                        "Đơn đã có dòng "
                                + line.getSku()
                                + " với đơn vị này. Hãy sửa số lượng ở dòng đó."
                );
            }
            UnitConversionService.Converted converted =
                    unitService.toBase(
                            conn,
                            line.getProductId(),
                            unitId,
                            BigDecimal.valueOf(quantity)
                    );
            orderDao.updateLine(
                    conn,
                    line.getId(),
                    unitId,
                    quantity,
                    converted.factor(),
                    converted.baseQuantity(),
                    manualBasePrice
            );
            orderDao.touch(conn, orderId);
            return null;
        });
    }
    public void deleteLine(
            long orderId,
            long lineId,
            User actor
    ) throws SQLException {
        requireOrders(actor);
        Db.inTransaction(conn -> {
            lockVisible(conn, orderId, actor);
            requireLine(conn, orderId, lineId);
            orderDao.deleteLine(conn, lineId);
            orderDao.touch(conn, orderId);
            return null;
        });
    }
    public void delete(
            long orderId,
            User actor
    ) throws SQLException {
        requireOrders(actor);
        Db.inTransaction(conn -> {
            Order order =
                    lockVisible(conn, orderId, actor);
            if (new OrderWorkflowDao().hasHistory(
                    conn, orderId)) {
                throw new ServiceException(
                        "Đơn đã có lịch sử duyệt; hãy huỷ thay vì xoá."
                );
            }
            orderDao.delete(conn, orderId);
            auditDao.log(
                    conn,
                    actor.getId(),
                    "ORDER_DELETE",
                    "ORDER",
                    orderId,
                    order.getCode(),
                    "Đại lý: "
                            + order.getCustomerCode()
                            + " – "
                            + order.getCustomerName()
                            + "\nSố dòng hàng: "
                            + order.getLineCount(),
                    null
            );
            return null;
        });
    }
    private void setQuantity(
            Connection conn,
            OrderLine line,
            long unitId,
            long quantity
    ) throws SQLException {
        if (quantity > MAX_QUANTITY) {
            throw new ServiceException(
                    "Số lượng tối đa "
                            + MAX_QUANTITY
                            + " cho mỗi dòng."
            );
        }
        UnitConversionService.Converted converted =
                unitService.toBase(
                        conn,
                        line.getProductId(),
                        unitId,
                        BigDecimal.valueOf(quantity)
                );
        orderDao.updateLine(
                conn,
                line.getId(),
                unitId,
                (int) quantity,
                converted.factor(),
                converted.baseQuantity(),
                line.getManualBasePrice()
        );
    }
    private OrderLine requireLine(
            Connection conn,
            long orderId,
            long lineId
    ) throws SQLException {
        OrderLine line =
                orderDao.findLine(conn, orderId, lineId);
        if (line == null) {
            throw new ServiceException(
                    "Dòng hàng không còn tồn tại. Vui lòng tải lại trang."
            );
        }
        return line;
    }
    Order lockVisible(
            Connection conn,
            long orderId,
            User viewer
    ) throws SQLException {
        Order order =
                orderDao.findByIdForUpdate(conn, orderId);
        requireVisible(order, viewer);
        if (order.getStatus() != Order.Status.DRAFT) {
            throw new ServiceException(
                    "Chỉ sửa hoặc xoá được đơn Nháp."
            );
        }
        return order;
    }
    static void requireVisible(
            Order order,
            User viewer
    ) {
        boolean visible = order != null
                && (
                    PermissionPolicy.canSeeAllOrders(
                            viewer.getRoles()
                    )
                    || (
                        viewer.hasRole(Role.SALES_REP)
                        && order.getCustomerSalesRepId() != null
                        && order.getCustomerSalesRepId()
                            == viewer.getId()
                    )
                    || (
                        viewer.hasRole(Role.CUSTOMER)
                        && order.getCustomerAccountUserId() != null
                        && order.getCustomerAccountUserId()
                            == viewer.getId()
                    )
                );
        if (!visible) {
            throw new NotFoundException();
        }
    }
    static boolean canSeeCustomer(
            Connection conn,
            User viewer,
            Customer customer
    ) throws SQLException {
        return PermissionPolicy.canSeeAllOrders(
                viewer.getRoles()
        )
                || (
                    viewer.hasRole(Role.SALES_REP)
                    && customer.getSalesRepId() != null
                    && customer.getSalesRepId() == viewer.getId()
                )
                || (
                    viewer.hasRole(Role.CUSTOMER)
                    && new PortalAccountDao().owns(
                            conn,
                            customer.getId(),
                            viewer.getId()
                    )
                );
    }
    static void requireOrders(User user) {
        if (user == null
                || (
                    !PermissionPolicy.canTakeOrders(user.getRoles())
                    && !user.hasRole(Role.CUSTOMER)
                )) {
            throw new ForbiddenException(
                    "Chỉ Nhân viên kinh doanh, Quản lý kinh doanh hoặc Đại lý được lập đơn hàng."
            );
        }
    }
    private static int parseQuantity(String text) {
        String digits = text == null
                ? ""
                : text.trim().replace(".", "").replace(" ", "");
        try {
            int quantity = Integer.parseInt(digits);
            if (quantity < 1
                    || quantity > MAX_QUANTITY) {
                throw new NumberFormatException();
            }
            return quantity;
        } catch (NumberFormatException e) {
            throw new ServiceException(
                    "Số lượng phải là số nguyên từ 1 đến "
                            + MAX_QUANTITY
                            + "."
            );
        }
    }
    void price(
            Connection conn,
            Order order
    ) throws SQLException {
        if (order.getStatus() != Order.Status.DRAFT) {
            BigDecimal gross = BigDecimal.ZERO;
            BigDecimal discount = BigDecimal.ZERO;
            int unpriced = 0;
            for (OrderLine line : order.getLines()) {
                if (!line.isPriced()) {
                    unpriced++;
                    continue;
                }
                gross = gross.add(line.getGrossAmount());
                discount = discount.add(line.getDiscountAmount());
            }
            order.setGrossTotal(gross);
            order.setDiscountTotal(discount);
            order.setUnpricedLines(unpriced);
            return;
        }
        LocalDate today = LocalDate.now();
        Map<Long, BigDecimal> totalBase =
                new HashMap<>();
        for (OrderLine line : order.getLines()) {
            totalBase.merge(
                    line.getProductId(),
                    line.getBaseQuantity(),
                    BigDecimal::add
            );
        }
        Map<Long, PriceListItem> prices =
                new HashMap<>();
        Map<Long, DiscountService.Applied> discounts =
                new HashMap<>();
        for (Map.Entry<Long, BigDecimal> entry :
                totalBase.entrySet()) {
            PriceListItem currentPrice =
                    priceListService.effectivePrice(
                            conn,
                            order.getCustomerGroupId(),
                            entry.getKey(),
                            today
                    );
            if (currentPrice != null) {
                prices.put(
                        entry.getKey(),
                        currentPrice
                );
                DiscountService.Applied applied =
                        discountService.best(
                                conn,
                                entry.getKey(),
                                entry.getValue(),
                                currentPrice.getSalePrice(),
                                today
                        );
                if (applied != null) {
                    discounts.put(
                            entry.getKey(),
                            applied
                    );
                }
            }
        }
        BigDecimal gross = BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;
        int unpriced = 0;
        boolean requiresApproval = false;
        for (OrderLine line : order.getLines()) {
            PriceListItem currentPrice =
                    prices.get(line.getProductId());

            if (currentPrice == null) {
                unpriced++;
                continue;
            }
            BigDecimal effectiveBasePrice =
                    line.isManualPrice()
                            ? line.getManualBasePrice()
                            : currentPrice.getSalePrice();
            line.setUnitPrice(
                    effectiveBasePrice
                            .multiply(line.getFactor())
                            .setScale(2, RoundingMode.HALF_UP)
            );
            line.setGrossAmount(
                    line.getBaseQuantity()
                            .multiply(effectiveBasePrice)
                            .setScale(2, RoundingMode.HALF_UP)
            );
            DiscountService.Applied applied =
                    discounts.get(line.getProductId());
            if (applied == null) {
                line.setDiscountAmount(BigDecimal.ZERO);
                line.setDiscountNote(null);
            } else {
                line.setDiscountAmount(
                        line.getBaseQuantity()
                                .multiply(applied.perUnit())
                                .setScale(
                                        2,
                                        RoundingMode.HALF_UP
                                )
                );
                line.setDiscountNote(
                        applied.policyName()
                                + ": "
                                + DiscountPolicy.valueText(
                                        applied.type(),
                                        applied.value()
                                )
                                + " (mua từ "
                                + applied.tierMinQty()
                                + ")"
                );
            }
            BigDecimal floorTotal =
                    line.getBaseQuantity()
                            .multiply(currentPrice.getFloorPrice())
                            .setScale(
                                    2,
                                    RoundingMode.HALF_UP
                            );
            if (line.getNetAmount().compareTo(floorTotal) < 0) {
                requiresApproval = true;
            }
            gross = gross.add(line.getGrossAmount());
            discount = discount.add(line.getDiscountAmount());
        }
        order.setGrossTotal(gross);
        order.setDiscountTotal(discount);
        order.setUnpricedLines(unpriced);
        order.setRequiresApproval(requiresApproval);
    }
    private static String describeHeader(Order order) {
        return "Điểm giao: "
                + (
                    order.getDeliveryPointText() == null
                            ? "chưa chọn"
                            : order.getDeliveryPointText()
                )
                + "\nNgày giao mong muốn: "
                + (
                    order.getDeliveryDate() == null
                            ? "chưa chọn"
                            : order.getDeliveryDate().format(DATE)
                )
                + "\nGhi chú: "
                + (
                    order.getNote() == null
                            ? "—"
                            : order.getNote()
                );
    }
    public static class NotFoundException
            extends ServiceException {

        public NotFoundException() {
            super("Không tìm thấy đơn hàng.");
        }
    }
}
