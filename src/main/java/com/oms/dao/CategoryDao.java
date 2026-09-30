package com.oms.dao;

import com.oms.model.Category;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class CategoryDao {

    /** Mọi nhóm hàng kèm số sản phẩm trực tiếp thuộc nhóm (chưa tính nhóm con). */
    public List<Category> listAll(Connection conn) throws SQLException {
        List<Category> list = new ArrayList<>();
        String sql = "SELECT c.id, c.parent_id, c.name, "
                + "(SELECT COUNT(*) FROM products p WHERE p.category_id = c.id) AS product_count "
                + "FROM product_categories c";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Category c = new Category();
                c.setId(rs.getLong("id"));
                long parent = rs.getLong("parent_id");
                c.setParentId(rs.wasNull() ? null : parent);
                c.setName(rs.getString("name"));
                c.setProductCount(rs.getLong("product_count"));
                list.add(c);
            }
        }
        return list;
    }

    /** Tên trùng giữa các nhóm cùng cha (không phân biệt hoa thường), bỏ qua chính nhóm đang sửa. */
    public boolean existsSibling(Connection conn, Long parentId, String name, long excludeId) throws SQLException {
        String sql = "SELECT 1 FROM product_categories WHERE "
                + (parentId == null ? "parent_id IS NULL" : "parent_id = ?") + " AND LOWER(name) = LOWER(?) AND id <> ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int i = 1;
            if (parentId != null) {
                ps.setLong(i++, parentId);
            }
            ps.setString(i++, name);
            ps.setLong(i, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public long insert(Connection conn, Long parentId, String name) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO product_categories (parent_id, name) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            if (parentId == null) {
                ps.setNull(1, Types.BIGINT);
            } else {
                ps.setLong(1, parentId);
            }
            ps.setString(2, name);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public void rename(Connection conn, long id, String name) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE product_categories SET name = ? WHERE id = ?")) {
            ps.setString(1, name);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    public void delete(Connection conn, long id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM product_categories WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    /** Chuyển toàn bộ sản phẩm của một nhóm sang nhóm khác; trả về số sản phẩm đã chuyển. */
    public int moveProducts(Connection conn, long fromId, long toId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE products SET category_id = ? WHERE category_id = ?")) {
            ps.setLong(1, toId);
            ps.setLong(2, fromId);
            return ps.executeUpdate();
        }
    }

    /** Khoá các dòng nhóm hàng để thao tác sửa cây chạy tuần tự, không tạo nhóm con dưới nhóm vừa bị xoá. */
    public void lockAll(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM product_categories FOR UPDATE");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rs.getLong(1);
            }
        }
    }
}
