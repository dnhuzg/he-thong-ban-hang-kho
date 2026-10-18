package com.oms.service;

import com.oms.config.Db;
import com.oms.dao.LookupDao;
import com.oms.dao.StockCardDao;
import com.oms.model.NamedItem;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Thẻ kho của một sản phẩm (S6-02): phát sinh nhập, xuất, trả về theo thời gian, mỗi dòng có tồn sau phát sinh
 * và liên kết chứng từ gốc; lọc theo kho và khoảng ngày. Tồn sau phát sinh tính lùi từ tồn hiện tại nên luôn khớp
 * sổ tồn, kể cả phần tồn có sẵn trước khi có bút toán. Quyền xem (module INVENTORY) do AccessRules kiểm.
 */
public class StockCardService {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public record Filter(Long productId, Long warehouseId, LocalDate from, LocalDate to) {
        public Long getProductId() { return productId; }
        public Long getWarehouseId() { return warehouseId; }
        public LocalDate getFrom() { return from; }
        public LocalDate getTo() { return to; }
    }

    /** Một dòng thẻ kho; increase / decrease theo đơn vị cơ sở. */
    public record Line(String time, String type, String documentCode, String documentUrl, String warehouseName,
                       BigDecimal increase, BigDecimal decrease, BigDecimal balance) {
        public String getTime() { return time; }
        public String getType() { return type; }
        public String getDocumentCode() { return documentCode; }
        public String getDocumentUrl() { return documentUrl; }
        public String getWarehouseName() { return warehouseName; }
        public BigDecimal getIncrease() { return increase; }
        public BigDecimal getDecrease() { return decrease; }
        public BigDecimal getBalance() { return balance; }
    }

    public record Card(Filter filter, List<NamedItem> products, List<NamedItem> warehouses, List<Line> lines,
                       BigDecimal opening, BigDecimal increase, BigDecimal decrease, BigDecimal closing) {
        public Filter getFilter() { return filter; }
        public List<NamedItem> getProducts() { return products; }
        public List<NamedItem> getWarehouses() { return warehouses; }
        public List<Line> getLines() { return lines; }
        public BigDecimal getOpening() { return opening; }
        public BigDecimal getIncrease() { return increase; }
        public BigDecimal getDecrease() { return decrease; }
        public BigDecimal getClosing() { return closing; }
        public boolean isSelected() { return filter.productId() != null; }
    }

    public Card card(Filter filter) throws SQLException {
        LocalDate to = filter.to() == null ? LocalDate.now() : filter.to();
        LocalDate from = filter.from() == null ? to.minusDays(30) : filter.from();
        if (from.isAfter(to)) {
            throw new ServiceException("Từ ngày phải trước hoặc bằng đến ngày.");
        }
        Filter applied = new Filter(filter.productId(), filter.warehouseId(), from, to);
        StockCardDao dao = new StockCardDao();
        try (Connection conn = Db.getConnection()) {
            List<NamedItem> products = dao.products(conn);
            List<NamedItem> warehouses = new LookupDao().warehouses(conn);
            if (applied.productId() == null) {
                return new Card(applied, products, warehouses, List.of(), BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO);
            }
            List<StockCardDao.Movement> movements = dao.movements(conn, applied.productId(), applied.warehouseId());
            // Tồn sau từng bút toán: đi lùi từ tồn hiện tại
            BigDecimal[] after = new BigDecimal[movements.size()];
            BigDecimal running = dao.onHand(conn, applied.productId(), applied.warehouseId());
            for (int i = movements.size() - 1; i >= 0; i--) {
                after[i] = running;
                running = running.subtract(signed(movements.get(i)));
            }
            BigDecimal opening = running;
            BigDecimal increase = BigDecimal.ZERO;
            BigDecimal decrease = BigDecimal.ZERO;
            List<Line> lines = new ArrayList<>();
            for (int i = 0; i < movements.size(); i++) {
                StockCardDao.Movement m = movements.get(i);
                LocalDate day = m.at().toLocalDate();
                if (day.isBefore(from)) {
                    opening = after[i];
                    continue;
                }
                if (day.isAfter(to)) {
                    break;
                }
                BigDecimal value = signed(m);
                BigDecimal in = value.signum() > 0 ? value : BigDecimal.ZERO;
                BigDecimal out = value.signum() < 0 ? value.negate() : BigDecimal.ZERO;
                increase = increase.add(in);
                decrease = decrease.add(out);
                lines.add(new Line(m.at().format(TIME), label(m.type()), m.documentCode(), url(m), m.warehouseName(),
                        plain(in), plain(out), plain(after[i])));
            }
            BigDecimal closing = opening.add(increase).subtract(decrease);
            return new Card(applied, products, warehouses, lines, plain(opening), plain(increase), plain(decrease),
                    plain(closing));
        }
    }

    private static BigDecimal signed(StockCardDao.Movement m) {
        return ("SHIPMENT_OUT".equals(m.type()) || "INVENTORY_ADJUSTMENT_LOSS".equals(m.type())) ? m.quantity().negate() : m.quantity();
    }

    private static String label(String type) {
        return switch (type) {
            case "GOODS_RECEIPT" -> "Nhập kho";
            case "SHIPMENT_OUT" -> "Xuất kho";
            case "SHIPMENT_RETURN" -> "Giao thất bại, trả về kho";
            case "INVENTORY_ADJUSTMENT_LOSS" -> "Điều chỉnh giảm";
            default -> type;
        };
    }

    private static String url(StockCardDao.Movement m) {
        return switch (m.referenceType()) {
            case "GOODS_RECEIPT" -> "/inventory/receipts?id=" + m.referenceId();
            case "SHIPMENT" -> "/shipments/confirm?id=" + m.referenceId();
            case "INVENTORY_ADJUSTMENT" -> "/inventory/adjustments/detail?id=" + m.referenceId();
            default -> null;
        };
    }

    private static BigDecimal plain(BigDecimal value) {
        return new BigDecimal(value.stripTrailingZeros().toPlainString());
    }
}
