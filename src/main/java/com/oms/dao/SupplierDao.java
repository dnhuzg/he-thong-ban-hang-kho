package com.oms.dao;

import com.oms.model.PageResult;
import com.oms.model.Supplier;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class SupplierDao {

    private static final String COLUMNS = "id, code, name, tax_code, contact_name, contact_phone, email, address, "
            + "payment_terms, status";

    /** Tìm theo mã, tên hoặc mã số thuế; lọc trạng thái. */
    public PageResult<Supplier> search(Connection conn, String keyword, Supplier.Status status, int page, int pageSize)
            throws SQLException {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (code LIKE ? OR name LIKE ? OR tax_code LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (status != null) {
            where.append(" AND status = ?");
            params.add(status.name());
        }
        long total;
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM suppliers" + where)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                total = rs.getLong(1);
            }
        }
        List<Supplier> items = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT " + COLUMNS + " FROM suppliers" + where
                + " ORDER BY name, id LIMIT ? OFFSET ?")) {
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

    public Supplier findById(Connection conn, long id) throws SQLException {
        return findOne(conn, "SELECT " + COLUMNS + " FROM suppliers WHERE id = ?", id);
    }

    public Supplier findByIdForUpdate(Connection conn, long id) throws SQLException {
        return findOne(conn, "SELECT " + COLUMNS + " FROM suppliers WHERE id = ? FOR UPDATE", id);
    }

    private Supplier findOne(Connection conn, String sql, long id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public boolean existsCode(Connection conn, String code) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM suppliers WHERE code = ?")) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Mã số thuế trùng với nhà cung cấp khác (excludeId = 0 khi tạo mới). */
    public boolean existsTaxCode(Connection conn, String taxCode, long excludeId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM suppliers WHERE tax_code = ? AND id <> ?")) {
            ps.setString(1, taxCode);
            ps.setLong(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public long insert(Connection conn, Supplier s) throws SQLException {
        String sql = "INSERT INTO suppliers (code, name, tax_code, contact_name, contact_phone, email, address, payment_terms, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, s.getCode());
            fill(ps, s, 2);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    /** Sửa mọi trường trừ mã (mã không đổi sau khi tạo). */
    public void update(Connection conn, Supplier s) throws SQLException {
        String sql = "UPDATE suppliers SET name = ?, tax_code = ?, contact_name = ?, contact_phone = ?, email = ?, "
                + "address = ?, payment_terms = ?, status = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            fill(ps, s, 1);
            ps.setLong(9, s.getId());
            ps.executeUpdate();
        }
    }

    public void delete(Connection conn, long id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM suppliers WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    private static void fill(PreparedStatement ps, Supplier s, int start) throws SQLException {
        ps.setString(start, s.getName());
        ps.setString(start + 1, s.getTaxCode());
        ps.setString(start + 2, s.getContactName());
        ps.setString(start + 3, s.getContactPhone());
        ps.setString(start + 4, s.getEmail());
        ps.setString(start + 5, s.getAddress());
        ps.setString(start + 6, s.getPaymentTerms());
        ps.setString(start + 7, s.getStatus().name());
    }

    private static Supplier map(ResultSet rs) throws SQLException {
        Supplier s = new Supplier();
        s.setId(rs.getLong("id"));
        s.setCode(rs.getString("code"));
        s.setName(rs.getString("name"));
        s.setTaxCode(rs.getString("tax_code"));
        s.setContactName(rs.getString("contact_name"));
        s.setContactPhone(rs.getString("contact_phone"));
        s.setEmail(rs.getString("email"));
        s.setAddress(rs.getString("address"));
        s.setPaymentTerms(rs.getString("payment_terms"));
        s.setStatus(Supplier.Status.valueOf(rs.getString("status")));
        return s;
    }

    private static void bind(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }
}
