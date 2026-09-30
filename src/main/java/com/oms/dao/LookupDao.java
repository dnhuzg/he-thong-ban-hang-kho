package com.oms.dao;

import com.oms.model.NamedItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Danh mục kho và địa bàn. */
public class LookupDao {

    public List<NamedItem> warehouses(Connection conn) throws SQLException {
        return list(conn, "SELECT id, name FROM warehouses ORDER BY name");
    }

    public List<NamedItem> areas(Connection conn) throws SQLException {
        return list(conn, "SELECT id, name FROM areas ORDER BY name");
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
