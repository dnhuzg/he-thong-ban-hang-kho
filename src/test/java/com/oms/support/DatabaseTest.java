package com.oms.support;

import com.oms.config.Db;
import com.oms.dao.UserDao;
import com.oms.model.Role;
import com.oms.model.User;
import com.oms.security.Passwords;
import org.junit.jupiter.api.BeforeEach;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.EnumSet;

public abstract class DatabaseTest {
    protected static final String PASSWORD = "Matkhau2026";
    private static final String HASH = Passwords.hash(PASSWORD);

    @BeforeEach
    void prepareDatabase() throws SQLException {
        try (Connection conn = Db.getConnection(); var statement = conn.createStatement()) {
            statement.execute("DROP ALL OBJECTS");
            statement.execute("""
                    CREATE TABLE users (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(50) UNIQUE,
                        email VARCHAR(150) UNIQUE, full_name VARCHAR(150), phone VARCHAR(20), avatar_path VARCHAR(255),
                        password_hash VARCHAR(100), status VARCHAR(20), must_change_password BOOLEAN,
                        failed_login_count INT DEFAULT 0, login_locked_until TIMESTAMP,
                        session_version INT DEFAULT 0, lock_reason VARCHAR(500),
                        locked_at TIMESTAMP, locked_by BIGINT)
                    """);
            statement.execute("CREATE TABLE user_roles (user_id BIGINT, role_code VARCHAR(30), PRIMARY KEY(user_id, role_code))");
            statement.execute("CREATE TABLE warehouses (id BIGINT PRIMARY KEY, name VARCHAR(150))");
            statement.execute("CREATE TABLE areas (id BIGINT PRIMARY KEY, name VARCHAR(150))");
            statement.execute("""
                    CREATE TABLE user_warehouses (user_id BIGINT, warehouse_id BIGINT,
                        PRIMARY KEY(user_id, warehouse_id), FOREIGN KEY(user_id) REFERENCES users(id),
                        FOREIGN KEY(warehouse_id) REFERENCES warehouses(id))
                    """);
            statement.execute("""
                    CREATE TABLE user_areas (user_id BIGINT, area_id BIGINT, PRIMARY KEY(user_id, area_id),
                        FOREIGN KEY(user_id) REFERENCES users(id), FOREIGN KEY(area_id) REFERENCES areas(id))
                    """);
            statement.execute("""
                    CREATE TABLE audit_logs (id BIGINT AUTO_INCREMENT PRIMARY KEY, actor_id BIGINT,
                        action VARCHAR(50), target_type VARCHAR(50), target_id BIGINT, detail VARCHAR(1000),
                        old_value CLOB, new_value CLOB, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)
                    """);
            statement.execute("CREATE TABLE customers (id BIGINT PRIMARY KEY, code VARCHAR(20), name VARCHAR(200), sales_rep_id BIGINT)");
            // Danh mục sản phẩm (S2-05, S2-06); dữ liệu mẫu: cây 3 cấp 1 → 2 → 3 và nhóm gốc 4
            statement.execute("CREATE TABLE product_categories (id BIGINT AUTO_INCREMENT PRIMARY KEY, parent_id BIGINT REFERENCES product_categories(id), name VARCHAR(150), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            statement.execute("CREATE TABLE units (id BIGINT AUTO_INCREMENT PRIMARY KEY, code VARCHAR(20) UNIQUE, name VARCHAR(50))");
            statement.execute("""
                    CREATE TABLE products (id BIGINT AUTO_INCREMENT PRIMARY KEY, sku VARCHAR(50) UNIQUE, name VARCHAR(255),
                        category_id BIGINT REFERENCES product_categories(id), base_unit_id BIGINT REFERENCES units(id),
                        packing_spec VARCHAR(255), cost_price DECIMAL(15,2), image_path VARCHAR(255),
                        status VARCHAR(20) DEFAULT 'ACTIVE', created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)
                    """);
            statement.execute("""
                    CREATE TABLE product_units (product_id BIGINT REFERENCES products(id) ON DELETE CASCADE,
                        unit_id BIGINT REFERENCES units(id), factor DECIMAL(12,4) CHECK (factor > 0),
                        PRIMARY KEY (product_id, unit_id))
                    """);
            statement.execute("INSERT INTO product_categories (id, parent_id, name) VALUES (1, NULL, 'Đồ uống'), (2, 1, 'Nước giải khát'), (3, 2, 'Nước ngọt có ga'), (4, NULL, 'Thực phẩm khô')");
            statement.execute("ALTER TABLE product_categories ALTER COLUMN id RESTART WITH 100");
            statement.execute("INSERT INTO units (id, code, name) VALUES (1, 'LON', 'Lon'), (2, 'THUNG', 'Thùng'), (3, 'GOI', 'Gói')");
            statement.execute("ALTER TABLE units ALTER COLUMN id RESTART WITH 100");
            statement.execute("INSERT INTO warehouses VALUES (10, 'Kho A'), (20, 'Kho B')");
            statement.execute("INSERT INTO areas VALUES (30, 'Địa bàn A')");
        }
    }

    protected User user(String username, Role... roles) throws SQLException {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.test");
        user.setFullName(username);
        user.setPasswordHash(HASH);
        user.setStatus(User.Status.ACTIVE);
        user.setMustChangePassword(false);
        try (Connection conn = Db.getConnection()) {
            UserDao dao = new UserDao();
            user.setId(dao.insert(conn, user));
            user.setRoles(roles.length == 0 ? EnumSet.noneOf(Role.class) : EnumSet.of(roles[0], roles));
            dao.replaceRoles(conn, user.getId(), user.getRoles());
        }
        return user;
    }

    protected User reload(long id) throws SQLException {
        try (Connection conn = Db.getConnection()) {
            return new UserDao().findById(conn, id);
        }
    }

    protected void sql(String statement) throws SQLException {
        try (Connection conn = Db.getConnection(); var ps = conn.createStatement()) {
            ps.execute(statement);
        }
    }

    protected long count(String table, String where) throws SQLException {
        try (Connection conn = Db.getConnection(); var ps = conn.createStatement();
             var rs = ps.executeQuery("SELECT COUNT(*) FROM " + table + " WHERE " + where)) {
            rs.next();
            return rs.getLong(1);
        }
    }
}
