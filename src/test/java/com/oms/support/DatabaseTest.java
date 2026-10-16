
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
                        delivery_date DATE, note VARCHAR(500), status VARCHAR(32) DEFAULT 'DRAFT',
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
            statement.execute("CREATE TABLE customer_receivables (id BIGINT AUTO_INCREMENT PRIMARY KEY, customer_id BIGINT REFERENCES customers(id), document_code VARCHAR(50) UNIQUE, issued_on DATE, outstanding DECIMAL(15,2))");
            statement.execute("CREATE TABLE order_credit_reservations (order_id BIGINT PRIMARY KEY REFERENCES orders(id), customer_id BIGINT REFERENCES customers(id), amount DECIMAL(15,2))");
            statement.execute("ALTER TABLE customers ADD serving_warehouse_id BIGINT REFERENCES warehouses(id)");
            statement.execute("CREATE TABLE warehouse_inventory(warehouse_id BIGINT REFERENCES warehouses(id),product_id BIGINT REFERENCES products(id),on_hand DECIMAL(14,4),reserved DECIMAL(14,4) DEFAULT 0,PRIMARY KEY(warehouse_id,product_id),CHECK(reserved>=0 AND reserved<=on_hand))");
            statement.execute("CREATE TABLE order_inventory_reservations(order_id BIGINT REFERENCES orders(id),warehouse_id BIGINT,product_id BIGINT,quantity DECIMAL(14,4),PRIMARY KEY(order_id,warehouse_id,product_id),FOREIGN KEY(warehouse_id,product_id) REFERENCES warehouse_inventory(warehouse_id,product_id))");

            statement.execute("ALTER TABLE products ADD track_lots BOOLEAN DEFAULT FALSE NOT NULL");
            statement.execute("ALTER TABLE products ADD expiry_warning_days INT DEFAULT 30 NOT NULL");
            statement.execute("CREATE TABLE inventory_lots (id BIGINT AUTO_INCREMENT PRIMARY KEY, warehouse_id BIGINT REFERENCES warehouses(id), product_id BIGINT REFERENCES products(id), lot_no VARCHAR(50), manufactured_date DATE, expiry_date DATE, remaining_quantity DECIMAL(14,4) CHECK(remaining_quantity>=0), unit_cost DECIMAL(15,2), received_at TIMESTAMP, source_type VARCHAR(30), source_id BIGINT, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            statement.execute("ALTER TABLE orders ADD warehouse_id BIGINT REFERENCES warehouses(id)");
            statement.execute("ALTER TABLE orders ADD approval_reason VARCHAR(1000)");
            statement.execute("ALTER TABLE order_lines ADD unit_price DECIMAL(15,2)");
            statement.execute("ALTER TABLE order_lines ADD gross_amount DECIMAL(18,2)");
            statement.execute("ALTER TABLE order_lines ADD discount_amount DECIMAL(18,2)");
            statement.execute("ALTER TABLE order_lines ADD discount_note VARCHAR(500)");
            statement.execute("CREATE TABLE order_status_history(id BIGINT AUTO_INCREMENT PRIMARY KEY,order_id BIGINT REFERENCES orders(id),from_status VARCHAR(32),to_status VARCHAR(32),action VARCHAR(30),opinion VARCHAR(500),actor_id BIGINT REFERENCES users(id),created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            statement.execute("CREATE TABLE customer_accounts(customer_id BIGINT PRIMARY KEY REFERENCES customers(id),user_id BIGINT UNIQUE REFERENCES users(id))");
            statement.execute("ALTER TABLE orders ADD customer_submitted BOOLEAN DEFAULT FALSE NOT NULL");
            statement.execute("""
                    CREATE TABLE shipments (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        code VARCHAR(30) UNIQUE,
                        warehouse_id BIGINT,
                        customer_id BIGINT,
                        status VARCHAR(30) DEFAULT 'READY',
                        recipient_name VARCHAR(200),
                        delivered_at TIMESTAMP,
                        proof_image_path VARCHAR(255),
                        failure_reason VARCHAR(500),
                        returned_at TIMESTAMP,
                        invoice_eligible_at TIMESTAMP,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)
                    )
                    """);
            statement.execute("""
                    CREATE TABLE shipment_items (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        shipment_id BIGINT,
                        product_id BIGINT,
                        unit_id BIGINT,
                        quantity DECIMAL(15,4),
                        FOREIGN KEY (shipment_id) REFERENCES shipments(id) ON DELETE CASCADE
                    )
                    """);
            statement.execute("""
                    CREATE TABLE inventory_movements (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        warehouse_id BIGINT,
                        product_id BIGINT,
                        unit_id BIGINT,
                        quantity DECIMAL(15,4),
                        movement_type VARCHAR(30),
                        reference_type VARCHAR(50),
                        reference_id BIGINT,
                        created_by BIGINT,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (warehouse_id) REFERENCES warehouses(id),
                        FOREIGN KEY (created_by) REFERENCES users(id)
                    )
                    """);
            statement.execute("""
                    CREATE TABLE goods_receipts (id BIGINT AUTO_INCREMENT PRIMARY KEY, code VARCHAR(20) UNIQUE,
                        supplier_id BIGINT REFERENCES suppliers(id), document_no VARCHAR(50), received_date DATE,
                        warehouse_id BIGINT REFERENCES warehouses(id), note VARCHAR(500), status VARCHAR(20) DEFAULT 'DRAFT',
                        created_by BIGINT REFERENCES users(id), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, confirmed_by BIGINT REFERENCES users(id),
                        confirmed_at TIMESTAMP, UNIQUE (supplier_id, document_no))
                    """);
            statement.execute("""
                    CREATE TABLE goods_receipt_lines (id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        receipt_id BIGINT REFERENCES goods_receipts(id) ON DELETE CASCADE, line_no INT,
                        product_id BIGINT REFERENCES products(id), unit_id BIGINT REFERENCES units(id),
                        quantity DECIMAL(14,4), factor DECIMAL(12,4), base_quantity DECIMAL(15,4),
                        lot_no VARCHAR(50), expiry_date DATE, UNIQUE (receipt_id, line_no))
                    """);
            statement.execute("""
                    CREATE TABLE picking_lists (id BIGINT AUTO_INCREMENT PRIMARY KEY, code VARCHAR(20) UNIQUE,
                        warehouse_id BIGINT REFERENCES warehouses(id), status VARCHAR(10) DEFAULT 'OPEN',
                        created_by BIGINT REFERENCES users(id), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        completed_by BIGINT REFERENCES users(id), completed_at TIMESTAMP)
                    """);
            statement.execute("""
                    CREATE TABLE picking_list_lines (id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        picking_list_id BIGINT REFERENCES picking_lists(id) ON DELETE CASCADE,
                        order_id BIGINT REFERENCES orders(id), order_line_id BIGINT UNIQUE REFERENCES order_lines(id),
                        product_id BIGINT REFERENCES products(id), required_quantity DECIMAL(14,4),
                        picked_quantity DECIMAL(14,4), short_reason VARCHAR(500), picked_by BIGINT REFERENCES users(id),
                        picked_at TIMESTAMP)
                    """);
            statement.execute("ALTER TABLE shipments ADD order_id BIGINT REFERENCES orders(id)");

            // GHTK-63 / S6-06: Bảng kiểm thử phân tuyến giao hàng.
            statement.execute("""
                    CREATE TABLE delivery_drivers (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        full_name VARCHAR(150) NOT NULL,
                        phone VARCHAR(20) NOT NULL,
                        license_no VARCHAR(50),
                        is_active BOOLEAN DEFAULT TRUE,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            statement.execute("""
                    CREATE TABLE delivery_vehicles (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        license_plate VARCHAR(30) NOT NULL UNIQUE,
                        vehicle_type VARCHAR(100),
                        is_active BOOLEAN DEFAULT TRUE,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            statement.execute("""
                    CREATE TABLE delivery_trips (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        code VARCHAR(30) NOT NULL UNIQUE,
                        warehouse_id BIGINT NOT NULL REFERENCES warehouses(id),
                        area_name VARCHAR(150) NOT NULL,
                        delivery_date DATE NOT NULL,
                        driver_id BIGINT REFERENCES delivery_drivers(id),
                        vehicle_id BIGINT REFERENCES delivery_vehicles(id),
                        status VARCHAR(20) DEFAULT 'PLANNED',
                        created_by BIGINT NOT NULL REFERENCES users(id),
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            statement.execute("""
                    CREATE TABLE delivery_trip_shipments (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        trip_id BIGINT NOT NULL REFERENCES delivery_trips(id) ON DELETE CASCADE,
                        shipment_id BIGINT NOT NULL UNIQUE REFERENCES shipments(id),
                        stop_order INT NOT NULL CHECK (stop_order > 0),
                        UNIQUE (trip_id, stop_order)
                    )
                    """);

            statement.execute("CREATE TABLE shipment_lot_allocations (id BIGINT AUTO_INCREMENT PRIMARY KEY, shipment_id BIGINT REFERENCES shipments(id), lot_id BIGINT REFERENCES inventory_lots(id), quantity DECIMAL(14,4) CHECK(quantity>0), override_reason VARCHAR(500), override_by BIGINT REFERENCES users(id), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, UNIQUE(shipment_id,lot_id))");
            statement.execute("ALTER TABLE goods_receipt_lines ADD manufactured_date DATE");
            statement.execute("""
                    CREATE TABLE order_backorders (id BIGINT AUTO_INCREMENT PRIMARY KEY, order_id BIGINT REFERENCES orders(id),
                        order_line_id BIGINT UNIQUE REFERENCES order_lines(id), product_id BIGINT REFERENCES products(id),
                        quantity DECIMAL(14,4), reason VARCHAR(500), shipment_id BIGINT REFERENCES shipments(id),
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)
                    """);
            statement.execute("INSERT INTO product_categories (id, parent_id, name) VALUES (1, NULL, 'Đồ uống'), (2, 1, 'Nước giải khát'), (3, 2, 'Nước ngọt có ga'), (4, NULL, 'Thực phẩm khô')");
            statement.execute("ALTER TABLE product_categories ALTER COLUMN id RESTART WITH 100");
            statement.execute("INSERT INTO units (id, code, name) VALUES (1, 'LON', 'Lon'), (2, 'THUNG', 'Thùng'), (3, 'GOI', 'Gói')");
            statement.execute("ALTER TABLE units ALTER COLUMN id RESTART WITH 100");

            // Phải thêm cột code trước khi INSERT dữ liệu kho.
            statement.execute("ALTER TABLE warehouses ADD code VARCHAR(20)");
            statement.execute("INSERT INTO warehouses (id, name, code) VALUES (10, 'Kho A', 'KHO-A'), (20, 'Kho B', 'KHO-B')");
            statement.execute("INSERT INTO areas VALUES (30, 'Địa bàn A')");

            statement.execute("""
                    CREATE TABLE invoices (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        code VARCHAR(30) UNIQUE,
                        customer_id BIGINT,
                        recipient_user_id BIGINT,
                        total_amount DECIMAL(15,2),
                        issued_at TIMESTAMP NOT NULL,
                        due_date DATE NOT NULL,
                        status VARCHAR(20) DEFAULT 'ISSUED',
                        paid_at TIMESTAMP
                    )
                    """);
            statement.execute("""
                    CREATE TABLE invoice_reminder_logs (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        invoice_id BIGINT,
                        reminder_date DATE,
                        reminder_type VARCHAR(20),
                        email_sent BOOLEAN DEFAULT FALSE,
                        in_app_sent BOOLEAN DEFAULT FALSE,
                        email_sent_at TIMESTAMP,
                        in_app_sent_at TIMESTAMP,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        UNIQUE(invoice_id, reminder_date)
                    )
                    """);
            statement.execute("""
                    CREATE TABLE notifications (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        recipient_user_id BIGINT,
                        invoice_id BIGINT,
                        reminder_date DATE,
                        notification_type VARCHAR(20),
                        title VARCHAR(200),
                        message VARCHAR(1000),
                        read_at TIMESTAMP,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        UNIQUE(recipient_user_id, invoice_id, reminder_date)
                    )
                    """);
            statement.execute("ALTER TABLE invoices ADD issued_by BIGINT REFERENCES users(id)");
            statement.execute("ALTER TABLE shipments ADD invoice_id BIGINT REFERENCES invoices(id)");
            statement.execute("CREATE TABLE document_sequences (name VARCHAR(30) PRIMARY KEY, last_number BIGINT DEFAULT 0)");
            statement.execute("INSERT INTO document_sequences VALUES ('INVOICE', 0), ('PAYMENT', 0)");
            statement.execute("ALTER TABLE invoices ADD paid_amount DECIMAL(15,2) DEFAULT 0 NOT NULL");
            statement.execute("ALTER TABLE invoices ADD credited_amount DECIMAL(15,2) DEFAULT 0 NOT NULL");
            statement.execute("""
                    CREATE TABLE payments (id BIGINT AUTO_INCREMENT PRIMARY KEY, code VARCHAR(20) UNIQUE,
                        customer_id BIGINT REFERENCES customers(id), amount DECIMAL(15,2), method VARCHAR(20), paid_on DATE,
                        collected_by BIGINT REFERENCES users(id), note VARCHAR(500), created_by BIGINT REFERENCES users(id),
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)
                    """);
            statement.execute("CREATE TABLE payment_allocations (id BIGINT AUTO_INCREMENT PRIMARY KEY, payment_id BIGINT REFERENCES payments(id), invoice_id BIGINT REFERENCES invoices(id), amount DECIMAL(15,2))");
            statement.execute("""
                    CREATE TABLE invoice_lines (id BIGINT AUTO_INCREMENT PRIMARY KEY, invoice_id BIGINT REFERENCES invoices(id),
                        shipment_id BIGINT REFERENCES shipments(id), product_id BIGINT REFERENCES products(id),
                        quantity DECIMAL(14,4), unit_price DECIMAL(15,2), amount DECIMAL(15,2))
                    """);
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
