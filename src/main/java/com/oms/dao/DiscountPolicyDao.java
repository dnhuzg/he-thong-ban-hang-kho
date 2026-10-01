package com.oms.dao;

import com.oms.model.DiscountPolicy;
import com.oms.model.DiscountTier;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DiscountPolicyDao {

    private static final String SELECT = "SELECT d.id, d.name, d.product_id, CONCAT(p.sku, ' – ', p.name), d.category_id, "
            + "d.discount_type, d.valid_from, d.valid_to, d.status FROM discount_policies d "
            + "LEFT JOIN products p ON p.id = d.product_id";

    public List<DiscountPolicy> list(Connection conn) throws SQLException {
        return query(conn, SELECT + " ORDER BY d.status, d.name, d.id", List.of());
    }

    public DiscountPolicy findById(Connection conn, long id) throws SQLException {
        List<DiscountPolicy> found = query(conn, SELECT + " WHERE d.id = ?", List.of(id));
        return found.isEmpty() ? null : found.get(0);
    }

    /**
     * Chính sách đang bật và còn hiệu lực vào ngày date áp được cho sản phẩm: chính sách theo sản phẩm đó hoặc theo
     * một trong các nhóm hàng categoryIds (nhóm của sản phẩm và các nhóm cha).
     */
    public List<DiscountPolicy> candidates(Connection conn, long productId, List<Long> categoryIds, LocalDate date)
            throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT + " WHERE d.status = 'ACTIVE' "
                + "AND (d.valid_from IS NULL OR d.valid_from <= ?) AND (d.valid_to IS NULL OR d.valid_to >= ?) "
                + "AND (d.product_id = ?");
        List<Object> params = new ArrayList<>(List.of(Date.valueOf(date), Date.valueOf(date), productId));
        if (!categoryIds.isEmpty()) {
            sql.append(" OR d.category_id IN (").append("?,".repeat(categoryIds.size()), 0, categoryIds.size() * 2 - 1).append(")");
            params.addAll(categoryIds);
        }
        sql.append(") ORDER BY d.id");
        return query(conn, sql.toString(), params);
    }

    public long insert(Connection conn, DiscountPolicy p) throws SQLException {
        String sql = "INSERT INTO discount_policies (name, product_id, category_id, discount_type, valid_from, valid_to, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindFields(ps, p);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                long id = keys.getLong(1);
                replaceTiers(conn, id, p.getTiers());
                return id;
            }
        }
    }

    public void update(Connection conn, DiscountPolicy p) throws SQLException {
        String sql = "UPDATE discount_policies SET name = ?, product_id = ?, category_id = ?, discount_type = ?, "
                + "valid_from = ?, valid_to = ?, status = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bindFields(ps, p);
            ps.setLong(8, p.getId());
            ps.executeUpdate();
        }
        replaceTiers(conn, p.getId(), p.getTiers());
    }

    public void delete(Connection conn, long id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM discount_policies WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    private void replaceTiers(Connection conn, long policyId, List<DiscountTier> tiers) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM discount_tiers WHERE policy_id = ?")) {
            ps.setLong(1, policyId);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO discount_tiers (policy_id, min_qty, discount_value) VALUES (?, ?, ?)")) {
            for (DiscountTier t : tiers) {
                ps.setLong(1, policyId);
                ps.setInt(2, t.getMinQty());
                ps.setBigDecimal(3, t.getValue());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static void bindFields(PreparedStatement ps, DiscountPolicy p) throws SQLException {
        ps.setString(1, p.getName());
        setLong(ps, 2, p.getProductId());
        setLong(ps, 3, p.getCategoryId());
        ps.setString(4, p.getType().name());
        ps.setDate(5, p.getValidFrom() == null ? null : Date.valueOf(p.getValidFrom()));
        ps.setDate(6, p.getValidTo() == null ? null : Date.valueOf(p.getValidTo()));
        ps.setString(7, p.getStatus().name());
    }

    private static void setLong(PreparedStatement ps, int index, Long value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.BIGINT);
        } else {
            ps.setLong(index, value);
        }
    }

    private List<DiscountPolicy> query(Connection conn, String sql, List<Object> params) throws SQLException {
        List<DiscountPolicy> policies = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DiscountPolicy p = new DiscountPolicy();
                    p.setId(rs.getLong(1));
                    p.setName(rs.getString(2));
                    long productId = rs.getLong(3);
                    p.setProductId(rs.wasNull() ? null : productId);
                    p.setProductLabel(rs.getString(4));
                    long categoryId = rs.getLong(5);
                    p.setCategoryId(rs.wasNull() ? null : categoryId);
                    p.setType(DiscountPolicy.Type.valueOf(rs.getString(6)));
                    Date from = rs.getDate(7);
                    p.setValidFrom(from == null ? null : from.toLocalDate());
                    Date to = rs.getDate(8);
                    p.setValidTo(to == null ? null : to.toLocalDate());
                    p.setStatus(DiscountPolicy.Status.valueOf(rs.getString(9)));
                    policies.add(p);
                }
            }
        }
        attachTiers(conn, policies);
        return policies;
    }

    private void attachTiers(Connection conn, List<DiscountPolicy> policies) throws SQLException {
        if (policies.isEmpty()) {
            return;
        }
        Map<Long, DiscountPolicy> byId = new HashMap<>();
        policies.forEach(p -> byId.put(p.getId(), p));
        String sql = "SELECT policy_id, min_qty, discount_value FROM discount_tiers WHERE policy_id IN ("
                + "?,".repeat(policies.size()).replaceAll(",$", "") + ") ORDER BY policy_id, min_qty";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < policies.size(); i++) {
                ps.setLong(i + 1, policies.get(i).getId());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    byId.get(rs.getLong(1)).getTiers().add(new DiscountTier(rs.getInt(2), rs.getBigDecimal(3)));
                }
            }
        }
    }
}
