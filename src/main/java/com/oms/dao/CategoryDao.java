package com.oms.dao;

import com.oms.model.Category;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
}
