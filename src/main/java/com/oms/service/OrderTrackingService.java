package com.oms.service;

import com.oms.config.Db;
import com.oms.dao.OrderDao;
import com.oms.dao.OrderTrackingDao;
import com.oms.dao.PortalAccountDao;
import com.oms.model.Order;
import com.oms.model.OrderLine;
import com.oms.model.User;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Đại lý theo dõi đơn của mình (S5-01): danh sách đơn đã gửi kèm trạng thái, ngày giao dự kiến, tổng tiền;
 * chi tiết từng dòng với số lượng đã xuất / đã giao lấy từ phiếu xuất thật, đánh dấu giao thiếu.
 */
public class OrderTrackingService {

    private static final int LIMIT = 200;

    /** Một dòng đơn kèm số đã xuất / đã giao (đơn vị cơ sở). */
    public static final class LineStatus {
        private final OrderLine line;
        private final BigDecimal shipped;
        private final BigDecimal delivered;
        private final boolean exported;

        LineStatus(OrderLine line, BigDecimal shipped, BigDecimal delivered, boolean exported) {
            this.line = line;
            this.shipped = shipped;
            this.delivered = delivered;
            this.exported = exported;
        }

        public OrderLine getLine() { return line; }
        public BigDecimal getShipped() { return plain(shipped); }
        public BigDecimal getDelivered() { return plain(delivered); }
        /** Đơn đã xuất kho mà dòng này xuất ít hơn số đặt. */
        public boolean isShortShipped() { return exported && shipped.compareTo(line.getBaseQuantity()) < 0; }
        public BigDecimal getMissing() { return plain(line.getBaseQuantity().subtract(shipped).max(BigDecimal.ZERO)); }
    }

    public record Detail(Order order, List<LineStatus> lines, boolean exported) {
        public Order getOrder() { return order; }
        public List<LineStatus> getLines() { return lines; }
        public boolean isExported() { return exported; }
        public boolean isShortShipped() { return lines.stream().anyMatch(LineStatus::isShortShipped); }
    }

    public List<Order> list(User viewer) throws SQLException {
        try (Connection conn = Db.getConnection()) {
            Long customerId = new PortalAccountDao().customerId(conn, viewer.getId());
            if (customerId == null) {
                throw new ForbiddenException("Tài khoản chưa được liên kết với đại lý nào.");
            }
            return new OrderDao().recentForCustomer(conn, customerId, LIMIT);
        }
    }

    public Detail detail(long orderId, User viewer) throws SQLException {
        // OrderService.get đã kiểm phạm vi: đại lý chỉ xem đơn của chính mình, đơn khác coi như không tồn tại
        Order order = new OrderService().get(orderId, viewer);
        if (order.getStatus() == Order.Status.DRAFT) {
            throw new OrderService.NotFoundException();
        }
        Map<Long, BigDecimal[]> byProduct;
        try (Connection conn = Db.getConnection()) {
            byProduct = new OrderTrackingDao().shipped(conn, orderId);
        }
        boolean exported = !byProduct.isEmpty();
        // Cùng sản phẩm có thể nằm ở nhiều dòng (khác đơn vị): chia số đã xuất / đã giao lần lượt theo dòng
        Map<Long, BigDecimal[]> left = new HashMap<>();
        byProduct.forEach((k, v) -> left.put(k, new BigDecimal[] {v[0], v[1]}));
        List<LineStatus> lines = new ArrayList<>();
        for (OrderLine line : order.getLines()) {
            BigDecimal[] remain = left.getOrDefault(line.getProductId(), new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO});
            BigDecimal shipped = remain[0].min(line.getBaseQuantity());
            BigDecimal delivered = remain[1].min(line.getBaseQuantity());
            remain[0] = remain[0].subtract(shipped);
            remain[1] = remain[1].subtract(delivered);
            lines.add(new LineStatus(line, shipped, delivered, exported));
        }
        return new Detail(order, lines, exported);
    }

    private static BigDecimal plain(BigDecimal value) {
        return new BigDecimal(value.stripTrailingZeros().toPlainString());
    }
}
