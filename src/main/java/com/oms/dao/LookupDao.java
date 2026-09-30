package com.oms.dao;

import com.oms.model.NamedItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Danh mục dùng chung cho các ô chọn: kho, địa bàn, người dùng. */
public class LookupDao {

    public List<NamedItem> warehouses(Connection conn) throws SQLException {
        return list(conn, "SELECT id, name FROM warehouses ORDER BY name");
    }

    public List<NamedItem> areas(Connection conn) throws SQLException {
        return list(conn, "SELECT id, name FROM areas ORDER BY name");
    }

    /** Người dùng dạng "Họ tên (tài khoản)" cho các ô chọn lọc. */
    public List<NamedItem> users(Connection conn) throws SQLException {
        return list(conn, "SELECT id, CONCAT(full_name, ' (', username, ')') FROM users ORDER BY full_name, username");
    }

    private List<NamedItem> list(Connection conn, String sql) throws SQLException {
        List<NamedItem> items = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                items.add(new NamedItem(rs.getLong(1), rs.getString(2)));
            }
        }
        return items;
    }
}
