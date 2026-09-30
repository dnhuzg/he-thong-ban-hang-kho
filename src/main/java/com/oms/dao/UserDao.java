package com.oms.dao;

import com.oms.model.NamedItem;
import com.oms.model.PageResult;
import com.oms.model.Role;
import com.oms.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UserDao {

    private static final String COLUMNS = "u.id, u.username, u.email, u.full_name, u.phone, u.password_hash, u.status, "
            + "u.must_change_password, u.failed_login_count, u.login_locked_until, u.session_version, u.lock_reason";

    public User findById(Connection conn, long id) throws SQLException {
        return findOne(conn, "SELECT " + COLUMNS + " FROM users u WHERE u.id = ?", id);
    }

    public User findByUsername(Connection conn, String username) throws SQLException {
        return findOne(conn, "SELECT " + COLUMNS + " FROM users u WHERE u.username = ?", username);
    }

    /** Khoá dòng người dùng để các thao tác bảo mật trên cùng tài khoản chạy tuần tự. */
    public User findByUsernameForUpdate(Connection conn, String username) throws SQLException {
        return findOne(conn, "SELECT " + COLUMNS + " FROM users u WHERE u.username = ? FOR UPDATE", username);
    }

    public User findByIdForUpdate(Connection conn, long id) throws SQLException {
        return findOne(conn, "SELECT " + COLUMNS + " FROM users u WHERE u.id = ? FOR UPDATE", id);
    }

    public User findByEmail(Connection conn, String email) throws SQLException {
        return findOne(conn, "SELECT " + COLUMNS + " FROM users u WHERE u.email = ?", email);
    }

    private User findOne(Connection conn, String sql, Object param) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                User user = map(rs);
                loadRelations(conn, user);
                return user;
            }
        }
    }

    public boolean existsUsername(Connection conn, String username) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM users WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** excludeId = id của chính user đang sửa (0 khi tạo mới). */
    public boolean existsEmail(Connection conn, String email, long excludeId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM users WHERE email = ? AND id <> ?")) {
            ps.setString(1, email);
            ps.setLong(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public long count(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM users");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        }
    }

    public Map<User.Status, Long> countByStatus(Connection conn) throws SQLException {
        Map<User.Status, Long> result = new EnumMap<>(User.Status.class);
        for (User.Status s : User.Status.values()) {
            result.put(s, 0L);
        }
        try (PreparedStatement ps = conn.prepareStatement("SELECT status, COUNT(*) FROM users GROUP BY status");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(User.Status.valueOf(rs.getString(1)), rs.getLong(2));
            }
        }
        return result;
    }

    /** Tìm theo tên, tài khoản, số điện thoại; lọc theo vai trò và trạng thái (S1-08). */
    public PageResult<User> search(Connection conn, String keyword, Role role, User.Status status, int page, int pageSize)
            throws SQLException {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (u.full_name LIKE ? OR u.username LIKE ? OR u.phone LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (role != null) {
            where.append(" AND EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_code = ?)");
            params.add(role.name());
        }
        if (status != null) {
            where.append(" AND u.status = ?");
            params.add(status.name());
        }

        long total;
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM users u" + where)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                total = rs.getLong(1);
            }
        }

        List<User> users = new ArrayList<>();
        String sql = "SELECT " + COLUMNS + " FROM users u" + where + " ORDER BY u.full_name, u.id LIMIT ? OFFSET ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, params);
            ps.setInt(params.size() + 1, pageSize);
            ps.setInt(params.size() + 2, (page - 1) * pageSize);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(map(rs));
                }
            }
        }
        for (User u : users) {
            loadRelations(conn, u);
        }
        return new PageResult<>(users, page, pageSize, total);
    }

    public long insert(Connection conn, User user) throws SQLException {
        String sql = "INSERT INTO users (username, email, full_name, phone, password_hash, status, must_change_password) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getFullName());
            ps.setString(4, user.getPhone());
            ps.setString(5, user.getPasswordHash());
            ps.setString(6, user.getStatus().name());
            ps.setBoolean(7, user.isMustChangePassword());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public void updateProfile(Connection conn, User user) throws SQLException {
        String sql = "UPDATE users SET email = ?, full_name = ?, phone = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getFullName());
            ps.setString(3, user.getPhone());
            ps.setLong(4, user.getId());
            ps.executeUpdate();
        }
    }

    public void replaceRoles(Connection conn, long userId, Set<Role> roles) throws SQLException {
        replaceLinks(conn, "user_roles", "role_code", userId, roles.stream().map(Role::name).toList());
    }

    public void replaceWarehouses(Connection conn, long userId, Collection<Long> ids) throws SQLException {
        replaceLinks(conn, "user_warehouses", "warehouse_id", userId, new ArrayList<>(ids));
    }

    public void replaceAreas(Connection conn, long userId, Collection<Long> ids) throws SQLException {
        replaceLinks(conn, "user_areas", "area_id", userId, new ArrayList<>(ids));
    }

    // table/column là hằng số trong code, không lấy từ input người dùng
    private void replaceLinks(Connection conn, String table, String column, long userId, List<?> values) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM " + table + " WHERE user_id = ?")) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        }
        if (values.isEmpty()) {
            return;
        }
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO " + table + " (user_id, " + column + ") VALUES (?, ?)")) {
            for (Object v : values) {
                ps.setLong(1, userId);
                ps.setObject(2, v);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public void recordLoginFailure(Connection conn, long userId, int failedCount, LocalDateTime lockedUntil) throws SQLException {
        String sql = "UPDATE users SET failed_login_count = ?, login_locked_until = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, failedCount);
            ps.setTimestamp(2, lockedUntil == null ? null : Timestamp.valueOf(lockedUntil));
            ps.setLong(3, userId);
            ps.executeUpdate();
        }
    }

    public void recordLoginSuccess(Connection conn, long userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE users SET failed_login_count = 0, login_locked_until = NULL WHERE id = ?")) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        }
    }

    /** Đổi mật khẩu và tăng session_version để thu hồi mọi phiên đang mở. Trả về session_version mới. */
    public int updatePassword(Connection conn, long userId, String passwordHash) throws SQLException {
        String sql = "UPDATE users SET password_hash = ?, must_change_password = 0, failed_login_count = 0, "
                + "login_locked_until = NULL, session_version = session_version + 1, "
                + "status = CASE WHEN status = 'PENDING' THEN 'ACTIVE' ELSE status END WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, passwordHash);
            ps.setLong(2, userId);
            ps.executeUpdate();
        }
        return findSessionVersion(conn, userId);
    }

    public void lock(Connection conn, long userId, String reason, long actorId) throws SQLException {
        String sql = "UPDATE users SET status = 'LOCKED', lock_reason = ?, locked_at = NOW(), locked_by = ?, "
                + "session_version = session_version + 1 WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, reason);
            ps.setLong(2, actorId);
            ps.setLong(3, userId);
            ps.executeUpdate();
        }
    }

    public void unlock(Connection conn, long userId) throws SQLException {
        String sql = "UPDATE users SET status = CASE WHEN must_change_password = 1 THEN 'PENDING' ELSE 'ACTIVE' END, lock_reason = NULL, "
                + "locked_at = NULL, locked_by = NULL, failed_login_count = 0, login_locked_until = NULL, "
                + "session_version = session_version + 1 WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        }
    }

    private int findSessionVersion(Connection conn, long userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT session_version FROM users WHERE id = ?")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private void loadRelations(Connection conn, User user) throws SQLException {
        Set<Role> roles = EnumSet.noneOf(Role.class);
        try (PreparedStatement ps = conn.prepareStatement("SELECT role_code FROM user_roles WHERE user_id = ?")) {
            ps.setLong(1, user.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    roles.add(Role.valueOf(rs.getString(1)));
                }
            }
        }
        user.setRoles(roles);
        user.setWarehouses(loadNamed(conn,
                "SELECT w.id, w.name FROM user_warehouses uw JOIN warehouses w ON w.id = uw.warehouse_id "
                        + "WHERE uw.user_id = ? ORDER BY w.name", user.getId()));
        user.setAreas(loadNamed(conn,
                "SELECT a.id, a.name FROM user_areas ua JOIN areas a ON a.id = ua.area_id "
                        + "WHERE ua.user_id = ? ORDER BY a.name", user.getId()));
    }

    private List<NamedItem> loadNamed(Connection conn, String sql, long userId) throws SQLException {
        List<NamedItem> items = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(new NamedItem(rs.getLong(1), rs.getString(2)));
                }
            }
        }
        return items;
    }

    private static void bind(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }

    private static User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setUsername(rs.getString("username"));
        u.setEmail(rs.getString("email"));
        u.setFullName(rs.getString("full_name"));
        u.setPhone(rs.getString("phone"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setStatus(User.Status.valueOf(rs.getString("status")));
        u.setMustChangePassword(rs.getBoolean("must_change_password"));
        u.setFailedLoginCount(rs.getInt("failed_login_count"));
        Timestamp lockedUntil = rs.getTimestamp("login_locked_until");
        u.setLoginLockedUntil(lockedUntil == null ? null : lockedUntil.toLocalDateTime());
        u.setSessionVersion(rs.getInt("session_version"));
        u.setLockReason(rs.getString("lock_reason"));
        return u;
    }
}
