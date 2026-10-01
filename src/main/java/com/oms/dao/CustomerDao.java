package com.oms.dao;

import com.oms.model.Customer;
import com.oms.model.PageResult;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Đại lý (EP-03): hồ sơ, tìm kiếm và cảnh báo bàn giao khi khoá nhân viên (S1-10). */
public class CustomerDao {

    private static final String SELECT = "SELECT c.id, c.code, c.name, c.phone, c.tax_code, c.group_id, g.name AS group_name, "
            + "c.area_id, a.name AS area_name, c.sales_rep_id, u.full_name AS rep_name, c.status, c.credit_limit, c.credit_days, c.lock_reason, c.locked_at, lb.full_name AS locked_by_name, c.created_at "
            + "FROM customers c LEFT JOIN customer_groups g ON g.id = c.group_id LEFT JOIN areas a ON a.id = c.area_id "
            + "LEFT JOIN users u ON u.id = c.sales_rep_id LEFT JOIN users lb ON lb.id = c.locked_by";

    /** Đại lý thuộc một nhân viên đã bị khoá: cần bàn giao cho người khác. */
    public record HandoverItem(String customerCode, String customerName, long repId, String repFullName, String repUsername) {
        public long getRepId() { return repId; }
        public String getCustomerCode() { return customerCode; }
        public String getCustomerName() { return customerName; }
        public String getRepFullName() { return repFullName; }
        public String getRepUsername() { return repUsername; }
    }

    /** Nhân viên đang phụ trách ít nhất một đại lý, kèm số đại lý; locked để đánh dấu người đã bị khoá. */
    public record RepLoad(long id, String name, boolean locked, long customers) {
        public long getId() { return id; }
        public String getName() { return name; }
        public boolean isLocked() { return locked; }
        public long getCustomers() { return customers; }
    }

    public List<RepLoad> repsWithCustomers(Connection conn) throws SQLException {
        List<RepLoad> reps = new ArrayList<>();
        String sql = "SELECT u.id, CONCAT(u.full_name, ' (', u.username, ')'), u.status, COUNT(*) FROM customers c "
                + "JOIN users u ON u.id = c.sales_rep_id GROUP BY u.id, u.full_name, u.username, u.status ORDER BY u.full_name, u.username";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                reps.add(new RepLoad(rs.getLong(1), rs.getString(2), "LOCKED".equals(rs.getString(3)), rs.getLong(4)));
            }
        }
        return reps;
    }

    public void updateSalesRep(Connection conn, long id, Long repId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE customers SET sales_rep_id = ? WHERE id = ?")) {
            setLong(ps, 1, repId);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    /** Điều kiện tìm đại lý; trường null hoặc rỗng là không lọc. ownRepId chỉ dùng cho người chỉ xem đại lý của mình. */
    public record Filter(String keyword, Long groupId, Long areaId, Long salesRepId, Customer.Status status, Long ownRepId) {
    }

    /** Số điện thoại lưu dạng 0xxxxxxxxx; người dùng hay gõ có dấu cách, dấu chấm hoặc +84 nên đưa về cùng dạng trước khi so khớp. */
    private static String phoneDigits(String keyword) {
        String k = keyword.trim();
        if (!k.matches("[+\\d\\s.()-]+")) {
            return k;
        }
        k = k.replaceAll("[\\s.()-]", "");
        return k.startsWith("+84") ? "0" + k.substring(3) : k;
    }

    public PageResult<Customer> search(Connection conn, Filter f, int page, int pageSize) throws SQLException {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (f.keyword() != null && !f.keyword().isBlank()) {
            where.append(" AND (c.code LIKE ? OR c.name LIKE ? OR c.phone LIKE ?)");
            String like = "%" + f.keyword().trim() + "%";
            params.add(like);
            params.add(like);
            params.add("%" + phoneDigits(f.keyword()) + "%");
        }
        addEquals(where, params, "c.group_id", f.groupId());
        addEquals(where, params, "c.area_id", f.areaId());
        addEquals(where, params, "c.sales_rep_id", f.salesRepId());
        addEquals(where, params, "c.sales_rep_id", f.ownRepId());
        if (f.status() != null) {
            where.append(" AND c.status = ?");
            params.add(f.status().name());
        }
        long total;
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM customers c" + where)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                total = rs.getLong(1);
            }
        }
        List<Customer> items = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT + where + " ORDER BY c.name, c.id LIMIT ? OFFSET ?")) {
            bind(ps, params);
            ps.setInt(params.size() + 1, pageSize);
            ps.setInt(params.size() + 2, (page - 1) * pageSize);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(map(rs));
                }
            }
        }
        return new PageResult<>(items, page, pageSize, total);
    }

    public Customer findById(Connection conn, long id) throws SQLException {
        return findOne(conn, SELECT + " WHERE c.id = ?", id);
    }

    public Customer findByIdForUpdate(Connection conn, long id) throws SQLException {
        return findOne(conn, SELECT + " WHERE c.id = ? FOR UPDATE", id);
    }

    private Customer findOne(Connection conn, String sql, long id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public boolean existsCode(Connection conn, String code) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM customers WHERE code = ?")) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Mã số thuế trùng đại lý khác (excludeId = 0 khi tạo mới). */
    public boolean existsTaxCode(Connection conn, String taxCode, long excludeId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM customers WHERE tax_code = ? AND id <> ?")) {
            ps.setString(1, taxCode);
            ps.setLong(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public long insert(Connection conn, Customer c) throws SQLException {
        String sql = "INSERT INTO customers (code, name, phone, tax_code, group_id, area_id, sales_rep_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getCode());
            fill(ps, c, 2);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    /** Sửa mọi trường trừ mã (mã không đổi sau khi tạo) và trạng thái (đổi bằng khoá / mở khoá có lý do). */
    public void update(Connection conn, Customer c) throws SQLException {
        String sql = "UPDATE customers SET name = ?, phone = ?, tax_code = ?, group_id = ?, area_id = ?, sales_rep_id = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            fill(ps, c, 1);
            ps.setLong(7, c.getId());
            ps.executeUpdate();
        }
    }

    public void updateCredit(Connection conn, long id, BigDecimal limit, int days) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE customers SET credit_limit = ?, credit_days = ? WHERE id = ?")) {
            ps.setBigDecimal(1, limit);
            ps.setInt(2, days);
            ps.setLong(3, id);
            ps.executeUpdate();
        }
    }

    public void lock(Connection conn, long id, String reason, long userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE customers SET status = 'LOCKED', lock_reason = ?, locked_at = CURRENT_TIMESTAMP, locked_by = ? WHERE id = ?")) {
            ps.setString(1, reason);
            ps.setLong(2, userId);
            ps.setLong(3, id);
            ps.executeUpdate();
        }
    }

    public void unlock(Connection conn, long id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE customers SET status = 'ACTIVE', lock_reason = NULL, locked_at = NULL, locked_by = NULL WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    public void delete(Connection conn, long id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM customers WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    /** Số đại lý đang phụ trách theo từng nhân viên (chỉ trả về người có ít nhất một đại lý). */
    public Map<Long, Long> countBySalesReps(Connection conn, Collection<Long> userIds) throws SQLException {
        Map<Long, Long> counts = new HashMap<>();
        if (userIds.isEmpty()) {
            return counts;
        }
        String placeholders = String.join(",", Collections.nCopies(userIds.size(), "?"));
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT sales_rep_id, COUNT(*) FROM customers WHERE sales_rep_id IN (" + placeholders + ") GROUP BY sales_rep_id")) {
            int i = 1;
            for (Long id : userIds) {
                ps.setLong(i++, id);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    counts.put(rs.getLong(1), rs.getLong(2));
                }
            }
        }
        return counts;
    }

    public List<String> namesBySalesRep(Connection conn, long userId) throws SQLException {
        List<String> names = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT name FROM customers WHERE sales_rep_id = ? ORDER BY name")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    names.add(rs.getString(1));
                }
            }
        }
        return names;
    }

    public List<HandoverItem> findNeedingHandover(Connection conn) throws SQLException {
        List<HandoverItem> items = new ArrayList<>();
        String sql = "SELECT c.code, c.name, u.id, u.full_name, u.username FROM customers c "
                + "JOIN users u ON u.id = c.sales_rep_id WHERE u.status = 'LOCKED' ORDER BY u.full_name, c.name";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                items.add(new HandoverItem(rs.getString(1), rs.getString(2), rs.getLong(3), rs.getString(4), rs.getString(5)));
            }
        }
        return items;
    }

    private static void fill(PreparedStatement ps, Customer c, int from) throws SQLException {
        ps.setString(from, c.getName());
        ps.setString(from + 1, c.getPhone());
        ps.setString(from + 2, c.getTaxCode());
        setLong(ps, from + 3, c.getGroupId());
        setLong(ps, from + 4, c.getAreaId());
        setLong(ps, from + 5, c.getSalesRepId());
    }

    private static void setLong(PreparedStatement ps, int index, Long value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.BIGINT);
        } else {
            ps.setLong(index, value);
        }
    }

    private static void addEquals(StringBuilder where, List<Object> params, String column, Long value) {
        if (value != null) {
            where.append(" AND ").append(column).append(" = ?");
            params.add(value);
        }
    }

    private static void bind(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }

    private static Customer map(ResultSet rs) throws SQLException {
        Customer c = new Customer();
        c.setId(rs.getLong("id"));
        c.setCode(rs.getString("code"));
        c.setName(rs.getString("name"));
        c.setPhone(rs.getString("phone"));
        c.setTaxCode(rs.getString("tax_code"));
        c.setGroupId(longOrNull(rs, "group_id"));
        c.setGroupName(rs.getString("group_name"));
        c.setAreaId(longOrNull(rs, "area_id"));
        c.setAreaName(rs.getString("area_name"));
        c.setSalesRepId(longOrNull(rs, "sales_rep_id"));
        c.setSalesRepName(rs.getString("rep_name"));
        c.setStatus(Customer.Status.valueOf(rs.getString("status")));
        c.setCreditLimit(rs.getBigDecimal("credit_limit"));
        c.setCreditDays(rs.getInt("credit_days"));
        c.setLockReason(rs.getString("lock_reason"));
        Timestamp locked = rs.getTimestamp("locked_at");
        c.setLockedAt(locked == null ? null : locked.toLocalDateTime());
        c.setLockedByName(rs.getString("locked_by_name"));
        Timestamp created = rs.getTimestamp("created_at");
        c.setCreatedAt(created == null ? null : created.toLocalDateTime());
        return c;
    }

    private static Long longOrNull(ResultSet rs, String column) throws SQLException {
        long v = rs.getLong(column);
        return rs.wasNull() ? null : v;
    }
}
