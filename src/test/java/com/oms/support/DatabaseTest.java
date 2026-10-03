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
            statement.execute("CREATE TABLE customer_groups (id BIGINT AUTO_INCREMENT PRIMARY KEY, code VARCHAR(20) UNIQUE, name VARCHAR(100))");
            statement.execute("INSERT INTO customer_groups (id, code, name) VALUES (1, 'DL1', 'Đại lý cấp 1'), (2, 'DL2', 'Đại lý cấp 2'), (3, 'LE', 'Khách lẻ')");
            statement.execute("""
                    CREATE TABLE customers (id BIGINT AUTO_INCREMENT PRIMARY KEY, code VARCHAR(20) UNIQUE, name VARCHAR(200),
                        phone VARCHAR(20), tax_code VARCHAR(14) UNIQUE, area_id BIGINT REFERENCES areas(id),
                        group_id BIGINT REFERENCES customer_groups(id), sales_rep_id BIGINT REFERENCES users(id),
                        status VARCHAR(20) DEFAULT 'ACTIVE', credit_limit DECIMAL(15,2) DEFAULT 0 NOT NULL,
                        credit_days INT DEFAULT 0 NOT NULL, lock_reason VARCHAR(500), locked_at TIMESTAMP,
                        locked_by BIGINT REFERENCES users(id), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)
                    """);
            statement.execute("""
                    CREATE TABLE customer_lock_history (id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        customer_id BIGINT REFERENCES customers(id) ON DELETE CASCADE, action VARCHAR(10), reason VARCHAR(500),
                        changed_by BIGINT REFERENCES users(id), changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)
                    """);
            statement.execute("""
                    CREATE TABLE customer_assignments (id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        customer_id BIGINT REFERENCES customers(id) ON DELETE CASCADE, from_user_id BIGINT REFERENCES users(id),
                        to_user_id BIGINT REFERENCES users(id), reason VARCHAR(500), changed_by BIGINT REFERENCES users(id),
                        changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)
                    """);
            statement.execute("""
                    CREATE TABLE customer_credit_history (id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        customer_id BIGINT REFERENCES customers(id) ON DELETE CASCADE, old_limit DECIMAL(15,2),
                        new_limit DECIMAL(15,2), old_days INT, new_days INT, reason VARCHAR(500),
                        changed_by BIGINT REFERENCES users(id), changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)
                    """);
            statement.execute("""
                    CREATE TABLE customer_delivery_points (id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        customer_id BIGINT REFERENCES customers(id) ON DELETE CASCADE, address VARCHAR(255),
                        receiver_name VARCHAR(150), receiver_phone VARCHAR(20), route_note VARCHAR(500),
                        is_default BOOLEAN DEFAULT FALSE, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)
                    """);
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
            statement.execute("""
                    CREATE TABLE suppliers (id BIGINT AUTO_INCREMENT PRIMARY KEY, code VARCHAR(20) UNIQUE, name VARCHAR(255),
                        tax_code VARCHAR(14), contact_name VARCHAR(150), contact_phone VARCHAR(20), email VARCHAR(150),
                        address VARCHAR(255), payment_terms VARCHAR(255), status VARCHAR(20) DEFAULT 'ACTIVE',
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)
                    """);
            statement.execute("""
                    CREATE TABLE price_lists (id BIGINT AUTO_INCREMENT PRIMARY KEY, code VARCHAR(30), version INT DEFAULT 1,
                        name VARCHAR(150), customer_group_id BIGINT REFERENCES customer_groups(id), valid_from DATE, valid_to DATE,
                        previous_id BIGINT REFERENCES price_lists(id), locked_at TIMESTAMP,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        UNIQUE (code, version))
                    """);
            statement.execute("""
                    CREATE TABLE price_list_items (price_list_id BIGINT REFERENCES price_lists(id) ON DELETE CASCADE,
                        product_id BIGINT REFERENCES products(id), sale_price DECIMAL(15,2), floor_price DECIMAL(15,2),
                        PRIMARY KEY (price_list_id, product_id), CHECK (floor_price >= 0 AND floor_price <= sale_price))
                    """);
            statement.execute("""
                    CREATE TABLE price_history (id BIGINT AUTO_INCREMENT PRIMARY KEY, product_id BIGINT REFERENCES products(id),
                        price_list_id BIGINT REFERENCES price_lists(id), change_type VARCHAR(10),
                        old_sale_price DECIMAL(15,2), old_floor_price DECIMAL(15,2), new_sale_price DECIMAL(15,2),
                        new_floor_price DECIMAL(15,2), effective_from DATE, changed_by BIGINT REFERENCES users(id),
                        changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)
                    """);
            statement.execute("""
                    CREATE TABLE discount_policies (id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(150),
                        product_id BIGINT REFERENCES products(id) ON DELETE CASCADE,
                        category_id BIGINT REFERENCES product_categories(id) ON DELETE CASCADE,
                        discount_type VARCHAR(10), valid_from DATE, valid_to DATE, status VARCHAR(10) DEFAULT 'ACTIVE',
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)
                    """);
            statement.execute("""
                    CREATE TABLE discount_tiers (policy_id BIGINT REFERENCES discount_policies(id) ON DELETE CASCADE,
                        min_qty INT, discount_value DECIMAL(15,2), PRIMARY KEY (policy_id, min_qty))
                    """);
            statement.execute("""
                    CREATE TABLE orders (id BIGINT AUTO_INCREMENT PRIMARY KEY, code VARCHAR(20) UNIQUE,
                        customer_id BIGINT REFERENCES customers(id), delivery_point_id BIGINT REFERENCES customer_delivery_points(id),
                        delivery_date DATE, note VARCHAR(500), status VARCHAR(10) DEFAULT 'DRAFT',
                        created_by BIGINT REFERENCES users(id), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)
                    """);
            statement.execute("""
        CREATE TABLE order_lines (id BIGINT AUTO_INCREMENT PRIMARY KEY,
            order_id BIGINT REFERENCES orders(id) ON DELETE CASCADE, product_id BIGINT REFERENCES products(id),
            unit_id BIGINT REFERENCES units(id), quantity INT, factor DECIMAL(12,4), base_quantity DECIMAL(14,4),
            manual_base_price DECIMAL(15,2),
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, UNIQUE (order_id, product_id, unit_id))
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
