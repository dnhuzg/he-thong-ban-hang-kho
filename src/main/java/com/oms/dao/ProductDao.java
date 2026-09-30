package com.oms.dao;

import com.oms.model.NamedItem;
import com.oms.model.PageResult;
import com.oms.model.Product;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProductDao {

    private static final String FROM = " FROM products p JOIN units u ON u.id = p.base_unit_id";
    private static final String COLUMNS = "p.id, p.sku, p.name, p.category_id, p.base_unit_id, u.name AS unit_name, "
            + "p.packing_spec, p.cost_price, p.image_path, p.status";

    /**
     * Tìm theo SKU / tên, lọc theo danh sách nhóm hàng (đã gồm nhóm con) và trạng thái.
     * withCost = false thì không đọc giá vốn ra khỏi DB.
     */
    public PageResult<Product> search(Connection conn, String keyword, List<Long> categoryIds, Product.Status status,
                                      int page, int pageSize, boolean withCost) throws SQLException {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (p.sku LIKE ? OR p.name LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            params.add(like);
            params.add(like);
        }
        if (categoryIds != null) {
            if (categoryIds.isEmpty()) {
                return new PageResult<>(List.of(), page, pageSize, 0);
            }
            where.append(" AND p.category_id IN (").append(String.join(",", Collections.nCopies(categoryIds.size(), "?"))).append(")");
            params.addAll(categoryIds);
        }
        if (status != null) {
            where.append(" AND p.status = ?");
            params.add(status.name());
        }
        long total;
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*)" + FROM + where)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                total = rs.getLong(1);
            }
        }
        List<Product> items = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT " + COLUMNS + FROM + where
                + " ORDER BY p.name, p.id LIMIT ? OFFSET ?")) {
            bind(ps, params);
            ps.setInt(params.size() + 1, pageSize);
            ps.setInt(params.size() + 2, (page - 1) * pageSize);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(map(rs, withCost));
                }
            }
        }
        return new PageResult<>(items, page, pageSize, total);
    }

    public Product findById(Connection conn, long id, boolean withCost) throws SQLException {
        return findOne(conn, "SELECT " + COLUMNS + FROM + " WHERE p.id = ?", id, withCost);
    }

    public Product findByIdForUpdate(Connection conn, long id) throws SQLException {
        return findOne(conn, "SELECT " + COLUMNS + FROM + " WHERE p.id = ? FOR UPDATE", id, true);
    }

    public Product findBySku(Connection conn, String sku, boolean withCost) throws SQLException {
        return findOne(conn, "SELECT " + COLUMNS + FROM + " WHERE p.sku = ?", sku, withCost);
    }

    private Product findOne(Connection conn, String sql, Object param, boolean withCost) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs, withCost) : null;
            }
        }
    }

    /** Sản phẩm đang kinh doanh dạng "SKU – Tên (đơn vị cơ sở)" cho các ô chọn (bảng giá, đơn hàng). */
    public List<NamedItem> activeOptions(Connection conn) throws SQLException {
        List<NamedItem> items = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT p.id, CONCAT(p.sku, ' – ', p.name, ' (', u.name, ')')"
                + FROM + " WHERE p.status = 'ACTIVE' ORDER BY p.sku");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                items.add(new NamedItem(rs.getLong(1), rs.getString(2)));
            }
        }
        return items;
    }

    public boolean existsSku(Connection conn, String sku) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM products WHERE sku = ?")) {
            ps.setString(1, sku);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public long insert(Connection conn, Product p) throws SQLException {
        String sql = "INSERT INTO products (sku, name, category_id, base_unit_id, packing_spec, cost_price, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getSku());
            ps.setString(2, p.getName());
            ps.setLong(3, p.getCategoryId());
            ps.setLong(4, p.getBaseUnitId());
            ps.setString(5, p.getPackingSpec());
            ps.setBigDecimal(6, p.getCostPrice());
            ps.setString(7, p.getStatus().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    /** Sửa thông tin chung; không đụng SKU, giá vốn và ảnh (có hàm riêng). */
    public void update(Connection conn, Product p) throws SQLException {
        String sql = "UPDATE products SET name = ?, category_id = ?, base_unit_id = ?, packing_spec = ?, status = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getName());
            ps.setLong(2, p.getCategoryId());
            ps.setLong(3, p.getBaseUnitId());
            ps.setString(4, p.getPackingSpec());
            ps.setString(5, p.getStatus().name());
            ps.setLong(6, p.getId());
            ps.executeUpdate();
        }
    }

    public void updateCost(Connection conn, long id, BigDecimal cost) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE products SET cost_price = ? WHERE id = ?")) {
            ps.setBigDecimal(1, cost);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    public void updateImage(Connection conn, long id, String imagePath) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE products SET image_path = ? WHERE id = ?")) {
            ps.setString(1, imagePath);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    public void updateCategory(Connection conn, long id, long categoryId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE products SET category_id = ? WHERE id = ?")) {
            ps.setLong(1, categoryId);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    public void delete(Connection conn, long id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM products WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    private static Product map(ResultSet rs, boolean withCost) throws SQLException {
        Product p = new Product();
        p.setId(rs.getLong("id"));
        p.setSku(rs.getString("sku"));
        p.setName(rs.getString("name"));
        p.setCategoryId(rs.getLong("category_id"));
        p.setBaseUnitId(rs.getLong("base_unit_id"));
        p.setBaseUnitName(rs.getString("unit_name"));
        p.setPackingSpec(rs.getString("packing_spec"));
        p.setCostPrice(withCost ? rs.getBigDecimal("cost_price") : null);
        p.setImagePath(rs.getString("image_path"));
        p.setStatus(Product.Status.valueOf(rs.getString("status")));
        return p;
    }

    private static void bind(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }
}
