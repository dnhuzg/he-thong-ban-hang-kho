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

    public List<NamedItem> units(Connection conn) throws SQLException {
        return list(conn, "SELECT id, name FROM units ORDER BY name");
    }

    /** Người dùng dạng "Họ tên (tài khoản)" cho các ô chọn lọc. */
    public List<NamedItem> users(Connection conn) throws SQLException {
        return list(conn, "SELECT id, CONCAT(full_name, ' (', username, ')') FROM users ORDER BY full_name, username");
    }

    public List<NamedItem> customerGroups(Connection conn) throws SQLException {
        return list(conn, "SELECT id, name FROM customer_groups ORDER BY id");
    }

    /** Người có thể làm phụ trách đại lý: Nhân viên kinh doanh và Quản lý kinh doanh còn hoạt động. */
    public List<NamedItem> salesReps(Connection conn) throws SQLException {
        return list(conn, "SELECT u.id, CONCAT(u.full_name, ' (', u.username, ')') FROM users u "
                + "WHERE u.status <> 'LOCKED' AND EXISTS (SELECT 1 FROM user_roles r WHERE r.user_id = u.id "
                + "AND r.role_code IN ('SALES_REP', 'SALES_MANAGER')) ORDER BY u.full_name, u.username");
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
