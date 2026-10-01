package com.oms.dao;

import com.oms.model.PriceChange;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Lịch sử đổi giá của sản phẩm: chỉ có thêm và đọc, không có hàm sửa hay xoá. */
public class PriceHistoryDao {

    public void insert(Connection conn, long productId, long priceListId, PriceChange.Type type,
                       BigDecimal oldSale, BigDecimal oldFloor, BigDecimal newSale, BigDecimal newFloor,
                       LocalDate effectiveFrom, long changedBy) throws SQLException {
        String sql = "INSERT INTO price_history (product_id, price_list_id, change_type, old_sale_price, old_floor_price, "
                + "new_sale_price, new_floor_price, effective_from, changed_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, productId);
            ps.setLong(2, priceListId);
            ps.setString(3, type.name());
            setDecimal(ps, 4, oldSale);
            setDecimal(ps, 5, oldFloor);
            setDecimal(ps, 6, newSale);
            setDecimal(ps, 7, newFloor);
            ps.setDate(8, Date.valueOf(effectiveFrom));
            ps.setLong(9, changedBy);
            ps.executeUpdate();
        }
    }

    /** Mới nhất trước. */
    public List<PriceChange> listByProduct(Connection conn, long productId) throws SQLException {
        String sql = "SELECT h.change_type, pl.code, pl.version, g.name, h.old_sale_price, h.old_floor_price, "
                + "h.new_sale_price, h.new_floor_price, h.effective_from, u.full_name, h.changed_at "
                + "FROM price_history h JOIN price_lists pl ON pl.id = h.price_list_id "
                + "JOIN customer_groups g ON g.id = pl.customer_group_id JOIN users u ON u.id = h.changed_by "
                + "WHERE h.product_id = ? ORDER BY h.changed_at DESC, h.id DESC";
        List<PriceChange> changes = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PriceChange c = new PriceChange();
                    c.setType(PriceChange.Type.valueOf(rs.getString(1)));
                    c.setPriceListCode(rs.getString(2));
                    c.setPriceListVersion(rs.getInt(3));
                    c.setGroupName(rs.getString(4));
                    c.setOldSalePrice(rs.getBigDecimal(5));
                    c.setOldFloorPrice(rs.getBigDecimal(6));
                    c.setNewSalePrice(rs.getBigDecimal(7));
                    c.setNewFloorPrice(rs.getBigDecimal(8));
                    c.setEffectiveFrom(rs.getDate(9).toLocalDate());
                    c.setChangedByName(rs.getString(10));
                    Timestamp at = rs.getTimestamp(11);
                    c.setChangedAt(at == null ? null : at.toLocalDateTime());
                    changes.add(c);
                }
            }
        }
        return changes;
    }

    private static void setDecimal(PreparedStatement ps, int index, BigDecimal value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.DECIMAL);
        } else {
            ps.setBigDecimal(index, value);
        }
    }
}
