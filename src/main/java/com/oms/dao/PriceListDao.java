package com.oms.dao;

import com.oms.model.NamedItem;
import com.oms.model.PriceList;
import com.oms.model.PriceListItem;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PriceListDao {

    private static final String COLUMNS = "pl.id, pl.code, pl.version, pl.name, pl.customer_group_id, g.name AS group_name, "
            + "pl.valid_from, pl.valid_to, pl.previous_id, pl.locked_at, "
            + "(SELECT COUNT(*) FROM price_list_items i WHERE i.price_list_id = pl.id) AS item_count, "
            + "(SELECT COUNT(*) FROM price_lists n WHERE n.previous_id = pl.id) AS newer_count";
    private static final String FROM = " FROM price_lists pl JOIN customer_groups g ON g.id = pl.customer_group_id";

    public List<NamedItem> customerGroups(Connection conn) throws SQLException {
        List<NamedItem> groups = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT id, name FROM customer_groups ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                groups.add(new NamedItem(rs.getLong(1), rs.getString(2)));
            }
        }
        return groups;
    }

    public List<PriceList> list(Connection conn, Long groupId) throws SQLException {
        String sql = "SELECT " + COLUMNS + FROM + (groupId == null ? "" : " WHERE pl.customer_group_id = ?")
                + " ORDER BY g.id, pl.code, pl.version DESC";
        List<PriceList> lists = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (groupId != null) {
                ps.setLong(1, groupId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lists.add(map(rs));
                }
            }
        }
        return lists;
    }

    public PriceList findById(Connection conn, long id) throws SQLException {
        return findOne(conn, "SELECT " + COLUMNS + FROM + " WHERE pl.id = ?", id);
    }

    public PriceList findByIdForUpdate(Connection conn, long id) throws SQLException {
        return findOne(conn, "SELECT " + COLUMNS + FROM + " WHERE pl.id = ? FOR UPDATE", id);
    }

    private PriceList findOne(Connection conn, String sql, long id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** Các phiên bản cùng mã, mới nhất trước. */
    public List<PriceList> versions(Connection conn, String code) throws SQLException {
        List<PriceList> lists = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT " + COLUMNS + FROM + " WHERE pl.code = ? ORDER BY pl.version DESC")) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lists.add(map(rs));
                }
            }
        }
        return lists;
    }

    public boolean existsCode(Connection conn, String code) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM price_lists WHERE code = ?")) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int maxVersion(Connection conn, String code) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT COALESCE(MAX(version), 0) FROM price_lists WHERE code = ?")) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** Bảng giá khác cùng nhóm có khoảng hiệu lực giao với [from, to] (to = null: không thời hạn). */
    public PriceList findOverlap(Connection conn, long groupId, LocalDate from, LocalDate to, List<Long> excludeIds)
            throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT " + COLUMNS + FROM
                + " WHERE pl.customer_group_id = ? AND (pl.valid_to IS NULL OR pl.valid_to >= ?)");
        if (to != null) {
            sql.append(" AND pl.valid_from <= ?");
        }
        for (int i = 0; i < excludeIds.size(); i++) {
            sql.append(" AND pl.id <> ?");
        }
        sql.append(" ORDER BY pl.valid_from LIMIT 1");
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int i = 1;
            ps.setLong(i++, groupId);
            ps.setDate(i++, Date.valueOf(from));
            if (to != null) {
                ps.setDate(i++, Date.valueOf(to));
            }
            for (Long id : excludeIds) {
                ps.setLong(i++, id);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public long insert(Connection conn, PriceList pl) throws SQLException {
        String sql = "INSERT INTO price_lists (code, version, name, customer_group_id, valid_from, valid_to, previous_id) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, pl.getCode());
            ps.setInt(2, pl.getVersion());
            ps.setString(3, pl.getName());
            ps.setLong(4, pl.getCustomerGroupId());
            ps.setDate(5, Date.valueOf(pl.getValidFrom()));
            ps.setDate(6, pl.getValidTo() == null ? null : Date.valueOf(pl.getValidTo()));
            if (pl.getPreviousId() == null) {
                ps.setNull(7, Types.BIGINT);
            } else {
                ps.setLong(7, pl.getPreviousId());
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public void updateHeader(Connection conn, PriceList pl) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE price_lists SET name = ?, customer_group_id = ?, valid_from = ?, valid_to = ? WHERE id = ?")) {
            ps.setString(1, pl.getName());
            ps.setLong(2, pl.getCustomerGroupId());
            ps.setDate(3, Date.valueOf(pl.getValidFrom()));
            ps.setDate(4, pl.getValidTo() == null ? null : Date.valueOf(pl.getValidTo()));
            ps.setLong(5, pl.getId());
            ps.executeUpdate();
        }
    }

    public void updateValidTo(Connection conn, long id, LocalDate validTo) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE price_lists SET valid_to = ? WHERE id = ?")) {
            ps.setDate(1, validTo == null ? null : Date.valueOf(validTo));
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    /** Khoá bảng giá khi phát sinh đơn đầu tiên (EP-04 gọi); đã khoá thì giữ nguyên thời điểm cũ. */
    public void lock(Connection conn, long id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE price_lists SET locked_at = CURRENT_TIMESTAMP WHERE id = ? AND locked_at IS NULL")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    public List<PriceListItem> items(Connection conn, long priceListId, boolean withCost) throws SQLException {
        String sql = "SELECT i.price_list_id, i.product_id, p.sku, p.name, u.name, i.sale_price, i.floor_price, p.cost_price "
                + "FROM price_list_items i JOIN products p ON p.id = i.product_id JOIN units u ON u.id = p.base_unit_id "
                + "WHERE i.price_list_id = ? ORDER BY p.name, p.id";
        List<PriceListItem> items = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, priceListId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PriceListItem it = new PriceListItem();
                    it.setPriceListId(rs.getLong(1));
                    it.setProductId(rs.getLong(2));
                    it.setSku(rs.getString(3));
                    it.setProductName(rs.getString(4));
                    it.setBaseUnitName(rs.getString(5));
                    it.setSalePrice(rs.getBigDecimal(6));
                    it.setFloorPrice(rs.getBigDecimal(7));
                    it.setCostPrice(withCost ? rs.getBigDecimal(8) : null);
                    items.add(it);
                }
            }
        }
        return items;
    }

    public PriceListItem findItem(Connection conn, long priceListId, long productId) throws SQLException {
        return items(conn, priceListId, false).stream().filter(i -> i.getProductId() == productId).findFirst().orElse(null);
    }

    public void upsertItem(Connection conn, long priceListId, long productId, BigDecimal sale, BigDecimal floor, boolean exists)
            throws SQLException {
        String sql = exists
                ? "UPDATE price_list_items SET sale_price = ?, floor_price = ? WHERE price_list_id = ? AND product_id = ?"
                : "INSERT INTO price_list_items (sale_price, floor_price, price_list_id, product_id) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, sale);
            ps.setBigDecimal(2, floor);
            ps.setLong(3, priceListId);
            ps.setLong(4, productId);
            ps.executeUpdate();
        }
    }

    public void deleteItem(Connection conn, long priceListId, long productId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM price_list_items WHERE price_list_id = ? AND product_id = ?")) {
            ps.setLong(1, priceListId);
            ps.setLong(2, productId);
            ps.executeUpdate();
        }
    }

    public void copyItems(Connection conn, long fromListId, long toListId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO price_list_items (price_list_id, product_id, sale_price, floor_price) "
                        + "SELECT ?, product_id, sale_price, floor_price FROM price_list_items WHERE price_list_id = ?")) {
            ps.setLong(1, toListId);
            ps.setLong(2, fromListId);
            ps.executeUpdate();
        }
    }

    public long countEffective(Connection conn, LocalDate date) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM price_lists WHERE valid_from <= ? AND (valid_to IS NULL OR valid_to >= ?)")) {
            ps.setDate(1, Date.valueOf(date));
            ps.setDate(2, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    /** Dòng giá áp dụng cho nhóm khách vào một ngày (đơn hàng EP-04 dùng); null nếu sản phẩm chưa có giá. */
    public PriceListItem effectiveItem(Connection conn, long groupId, long productId, LocalDate date) throws SQLException {
        String sql = "SELECT i.price_list_id, i.sale_price, i.floor_price FROM price_list_items i "
                + "JOIN price_lists pl ON pl.id = i.price_list_id WHERE pl.customer_group_id = ? AND i.product_id = ? "
                + "AND pl.valid_from <= ? AND (pl.valid_to IS NULL OR pl.valid_to >= ?) ORDER BY pl.valid_from DESC LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, groupId);
            ps.setLong(2, productId);
            ps.setDate(3, Date.valueOf(date));
            ps.setDate(4, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                PriceListItem it = new PriceListItem();
                it.setPriceListId(rs.getLong(1));
                it.setProductId(productId);
                it.setSalePrice(rs.getBigDecimal(2));
                it.setFloorPrice(rs.getBigDecimal(3));
                return it;
            }
        }
    }

    private static PriceList map(ResultSet rs) throws SQLException {
        PriceList pl = new PriceList();
        pl.setId(rs.getLong("id"));
        pl.setCode(rs.getString("code"));
        pl.setVersion(rs.getInt("version"));
        pl.setName(rs.getString("name"));
        pl.setCustomerGroupId(rs.getLong("customer_group_id"));
        pl.setCustomerGroupName(rs.getString("group_name"));
        pl.setValidFrom(rs.getDate("valid_from").toLocalDate());
        Date to = rs.getDate("valid_to");
        pl.setValidTo(to == null ? null : to.toLocalDate());
        long prev = rs.getLong("previous_id");
        pl.setPreviousId(rs.wasNull() ? null : prev);
        Timestamp locked = rs.getTimestamp("locked_at");
        pl.setLockedAt(locked == null ? null : locked.toLocalDateTime());
        pl.setItemCount(rs.getLong("item_count"));
        pl.setHasNewerVersion(rs.getLong("newer_count") > 0);
        return pl;
    }
}
