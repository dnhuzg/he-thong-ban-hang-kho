package com.oms.dao;

import com.oms.model.ProductUnit;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductUnitDao {

    private static final String SELECT = "SELECT pu.product_id, pu.unit_id, u.name, pu.factor "
            + "FROM product_units pu JOIN units u ON u.id = pu.unit_id";

    public List<ProductUnit> listByProduct(Connection conn, long productId) throws SQLException {
        return listByProducts(conn, List.of(productId)).getOrDefault(productId, List.of());
    }

    /** Quy đổi của nhiều sản phẩm một lần (trang danh sách), sắp theo hệ số tăng dần. */
    public Map<Long, List<ProductUnit>> listByProducts(Connection conn, Collection<Long> productIds) throws SQLException {
        Map<Long, List<ProductUnit>> result = new HashMap<>();
        if (productIds.isEmpty()) {
            return result;
        }
        String sql = SELECT + " WHERE pu.product_id IN (" + String.join(",", Collections.nCopies(productIds.size(), "?"))
                + ") ORDER BY pu.product_id, pu.factor";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int i = 1;
            for (Long id : productIds) {
                ps.setLong(i++, id);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ProductUnit pu = new ProductUnit();
                    pu.setProductId(rs.getLong(1));
                    pu.setUnitId(rs.getLong(2));
                    pu.setUnitName(rs.getString(3));
                    pu.setFactor(rs.getBigDecimal(4));
                    result.computeIfAbsent(pu.getProductId(), k -> new ArrayList<>()).add(pu);
                }
            }
        }
        return result;
    }

    public BigDecimal findFactor(Connection conn, long productId, long unitId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT factor FROM product_units WHERE product_id = ? AND unit_id = ?")) {
            ps.setLong(1, productId);
            ps.setLong(2, unitId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : null;
            }
        }
    }

    public void insert(Connection conn, long productId, long unitId, BigDecimal factor) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO product_units (product_id, unit_id, factor) VALUES (?, ?, ?)")) {
            ps.setLong(1, productId);
            ps.setLong(2, unitId);
            ps.setBigDecimal(3, factor);
            ps.executeUpdate();
        }
    }

    public void updateFactor(Connection conn, long productId, long unitId, BigDecimal factor) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE product_units SET factor = ? WHERE product_id = ? AND unit_id = ?")) {
            ps.setBigDecimal(1, factor);
            ps.setLong(2, productId);
            ps.setLong(3, unitId);
            ps.executeUpdate();
        }
    }

    public void delete(Connection conn, long productId, long unitId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM product_units WHERE product_id = ? AND unit_id = ?")) {
            ps.setLong(1, productId);
            ps.setLong(2, unitId);
            ps.executeUpdate();
        }
    }

    public boolean hasAny(Connection conn, long productId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM product_units WHERE product_id = ?")) {
            ps.setLong(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
