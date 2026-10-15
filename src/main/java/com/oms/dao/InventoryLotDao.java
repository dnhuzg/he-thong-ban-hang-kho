package com.oms.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Lô dùng chung với phiếu nhập và báo cáo tồn; không đưa giá vốn vào dữ liệu màn lô. */
public class InventoryLotDao {
    public record Product(long id, String sku, String name, String unit, boolean tracked, int warningDays) {
        public long getId() { return id; }
        public String getSku() { return sku; }
        public String getName() { return name; }
        public String getUnit() { return unit; }
        public boolean isTracked() { return tracked; }
        public int getWarningDays() { return warningDays; }
    }

    public record Lot(long id, long productId, long warehouseId, String sku, String productName,
                      String warehouseName, String unit, String number, LocalDate manufactured,
                      LocalDate expiry, BigDecimal remaining, int warningDays, boolean tracked) {
        public long getId() { return id; }
        public long getProductId() { return productId; }
        public long getWarehouseId() { return warehouseId; }
        public String getSku() { return sku; }
        public String getProductName() { return productName; }
        public String getWarehouseName() { return warehouseName; }
        public String getUnit() { return unit; }
        public String getNumber() { return number; }
        public LocalDate getManufactured() { return manufactured; }
        public LocalDate getExpiry() { return expiry; }
        public String getManufacturedText() { return manufactured==null?"Chưa khai báo":manufactured.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")); }
        public String getExpiryText() { return expiry==null?"Chưa khai báo":expiry.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")); }
        public BigDecimal getRemaining() { return remaining.stripTrailingZeros(); }
        public boolean isTracked() { return tracked; }
        public String getStatus() {
            if (remaining.signum() == 0) return "empty";
            if (number == null || number.isBlank() || manufactured == null || expiry == null) return "incomplete";
            if (expiry.isBefore(LocalDate.now())) return "expired";
            return !expiry.isAfter(LocalDate.now().plusDays(warningDays)) ? "soon" : "valid";
        }
        public String getStatusLabel() {
            return switch (getStatus()) {
                case "empty" -> "Hết hàng";
                case "incomplete" -> "Thiếu thông tin lô";
                case "expired" -> "Đã quá hạn";
                case "soon" -> "Sắp hết hạn";
                default -> "Còn hạn";
            };
        }
        public String getTone() {
            return switch (getStatus()) { case "expired" -> "danger"; case "soon", "incomplete" -> "warning"; default -> "success"; };
        }
    }

    private static final String LOT_SELECT = "SELECT l.id,l.product_id,l.warehouse_id,p.sku,p.name,"
            + "(SELECT name FROM warehouses WHERE id=l.warehouse_id),(SELECT name FROM units WHERE id=p.base_unit_id),"
            + "l.lot_no,l.manufactured_date,l.expiry_date,l.remaining_quantity,p.expiry_warning_days,p.track_lots "
            + "FROM inventory_lots l JOIN products p ON p.id=l.product_id";

    public List<Product> products(Connection conn) throws SQLException {
        List<Product> list = new ArrayList<>();
        try (var ps = conn.prepareStatement("SELECT p.id,p.sku,p.name,u.name,p.track_lots,p.expiry_warning_days "
                + "FROM products p JOIN units u ON u.id=p.base_unit_id ORDER BY p.sku"); var rs = ps.executeQuery()) {
            while (rs.next()) list.add(new Product(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getBoolean(5), rs.getInt(6)));
        }
        return list;
    }

    public Product product(Connection conn, long id, boolean lock) throws SQLException {
        // Không khoá hàng units dùng chung giữa nhiều SKU để tránh deadlock khi xuất nhiều SKU.
        try (var ps = conn.prepareStatement("SELECT p.id,p.sku,p.name,(SELECT name FROM units WHERE id=p.base_unit_id),p.track_lots,p.expiry_warning_days "
                + "FROM products p WHERE p.id=?" + (lock ? " FOR UPDATE" : ""))) {
            ps.setLong(1, id);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? new Product(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getBoolean(5), rs.getInt(6)) : null;
            }
        }
    }

    public List<Lot> lots(Connection conn, Long product, Long warehouse, boolean lock) throws SQLException {
        List<Lot> list = new ArrayList<>();
        String sql = LOT_SELECT + " WHERE 1=1" + (product == null ? "" : " AND l.product_id=?")
                + (warehouse == null ? "" : " AND l.warehouse_id=?") + " ORDER BY l.expiry_date,l.id" + (lock ? " FOR UPDATE" : "");
        try (var ps = conn.prepareStatement(sql)) {
            int n = 1;
            if (product != null) ps.setLong(n++, product);
            if (warehouse != null) ps.setLong(n, warehouse);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) list.add(new Lot(rs.getLong(1),rs.getLong(2),rs.getLong(3),rs.getString(4),rs.getString(5),
                        rs.getString(6),rs.getString(7),rs.getString(8),date(rs.getDate(9)),date(rs.getDate(10)),
                        rs.getBigDecimal(11),rs.getInt(12),rs.getBoolean(13)));
            }
        }
        return list;
    }

    public Lot find(Connection conn, long id, boolean lock) throws SQLException {
        // Dùng bộ lọc ID để không khoá toàn bộ danh sách lô khi chỉ cập nhật một lô.
        try (var ps = conn.prepareStatement(LOT_SELECT + " WHERE l.id=?" + (lock ? " FOR UPDATE" : ""))) {
            ps.setLong(1,id);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? new Lot(rs.getLong(1),rs.getLong(2),rs.getLong(3),rs.getString(4),rs.getString(5),rs.getString(6),rs.getString(7),rs.getString(8),date(rs.getDate(9)),date(rs.getDate(10)),rs.getBigDecimal(11),rs.getInt(12),rs.getBoolean(13)) : null;
            }
        }
    }

    public void configure(Connection conn, long id, boolean enabled, int days) throws SQLException {
        try (var ps = conn.prepareStatement("UPDATE products SET track_lots=?,expiry_warning_days=? WHERE id=?")) {
            ps.setBoolean(1, enabled); ps.setInt(2, days); ps.setLong(3,id); ps.executeUpdate();
        }
    }

    public void metadata(Connection conn, long id, String number, LocalDate manufactured, LocalDate expiry) throws SQLException {
        try (var ps = conn.prepareStatement("UPDATE inventory_lots SET lot_no=?,manufactured_date=?,expiry_date=? WHERE id=?")) {
            ps.setString(1,number); ps.setDate(2,Date.valueOf(manufactured)); ps.setDate(3,Date.valueOf(expiry)); ps.setLong(4,id); ps.executeUpdate();
        }
    }

    public void allocate(Connection conn, long shipment, Lot lot, BigDecimal quantity, String reason, Long actor) throws SQLException {
        try (var ps = conn.prepareStatement("UPDATE inventory_lots SET remaining_quantity=remaining_quantity-? WHERE id=? AND remaining_quantity>=?")) {
            ps.setBigDecimal(1,quantity); ps.setLong(2,lot.id()); ps.setBigDecimal(3,quantity);
            if (ps.executeUpdate()!=1) throw new SQLException("Lượng tồn lô đã thay đổi.");
        }
        try (var ps = conn.prepareStatement("INSERT INTO shipment_lot_allocations(shipment_id,lot_id,quantity,override_reason,override_by) VALUES(?,?,?,?,?)")) {
            ps.setLong(1,shipment); ps.setLong(2,lot.id()); ps.setBigDecimal(3,quantity); ps.setString(4,reason);
            if (actor == null) ps.setNull(5,java.sql.Types.BIGINT); else ps.setLong(5,actor);
            ps.executeUpdate();
        }
    }

    public void returnShipment(Connection conn, long shipment) throws SQLException {
        try (var ps = conn.prepareStatement("SELECT lot_id,quantity FROM shipment_lot_allocations WHERE shipment_id=? ORDER BY lot_id FOR UPDATE")) {
            ps.setLong(1,shipment);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    try (var update = conn.prepareStatement("UPDATE inventory_lots SET remaining_quantity=remaining_quantity+? WHERE id=?")) {
                        update.setBigDecimal(1,rs.getBigDecimal(2)); update.setLong(2,rs.getLong(1)); update.executeUpdate();
                    }
                }
            }
        }
    }

    private static LocalDate date(Date date) { return date == null ? null : date.toLocalDate(); }
}
