package com.oms.service;

import com.oms.config.Db;
import com.oms.dao.AuditLogDao;
import com.oms.dao.SupplierDao;
import com.oms.model.PageResult;
import com.oms.model.Supplier;
import com.oms.model.User;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Locale;
import java.util.regex.Pattern;

/** Danh mục nhà cung cấp (S2-09). */
public class SupplierService {

    public static final int PAGE_SIZE = 20;
    private static final Pattern CODE = Pattern.compile("^[A-Z0-9][A-Z0-9._-]{1,19}$");
    // Mã số thuế doanh nghiệp Việt Nam: 10 số, chi nhánh / đơn vị phụ thuộc thêm "-" và 3 số
    private static final Pattern TAX_CODE = Pattern.compile("^\\d{10}(-\\d{3})?$");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final SupplierDao supplierDao = new SupplierDao();
    private final AuditLogDao auditDao = new AuditLogDao();

    public PageResult<Supplier> search(String keyword, Supplier.Status status, int page) throws SQLException {
        try (Connection conn = Db.getConnection()) {
            return supplierDao.search(conn, keyword, status, Math.max(1, page), PAGE_SIZE);
        }
    }

    public Supplier get(long id) throws SQLException {
        try (Connection conn = Db.getConnection()) {
            Supplier s = supplierDao.findById(conn, id);
            if (s == null) {
                throw new NotFoundException();
            }
            return s;
        }
    }

    /** input.code chỉ dùng khi tạo mới; các trường còn lại được kiểm tra và chuẩn hoá tại đây. */
    public long create(Supplier input, User actor) throws SQLException {
        String code = normalizeCode(input.getCode());
        Supplier s = validated(input);
        s.setCode(code);
        return Db.inTransaction(conn -> {
            if (supplierDao.existsCode(conn, code)) {
                throw new ServiceException("Mã nhà cung cấp \"" + code + "\" đã tồn tại.");
            }
            checkTaxCode(conn, s.getTaxCode(), 0);
            long id = supplierDao.insert(conn, s);
            auditDao.log(conn, actor.getId(), "SUPPLIER_CREATE", "SUPPLIER", id, code, null, describe(s));
            return id;
        });
    }

    public void update(long id, Supplier input, User actor) throws SQLException {
        Supplier changes = validated(input);
        Db.inTransaction(conn -> {
            Supplier existing = supplierDao.findByIdForUpdate(conn, id);
            if (existing == null) {
                throw new NotFoundException();
            }
            checkTaxCode(conn, changes.getTaxCode(), id);
            changes.setId(id);
            changes.setCode(existing.getCode());
            supplierDao.update(conn, changes);
            String before = describe(existing);
            String after = describe(changes);
            if (!before.equals(after)) {
                auditDao.log(conn, actor.getId(), "SUPPLIER_UPDATE", "SUPPLIER", id, existing.getCode(), before, after);
            }
            return null;
        });
    }

    /** Chỉ xoá được khi chưa có phiếu nhập hay dữ liệu nào tham chiếu (khoá ngoại chặn); ngược lại ngừng giao dịch. */
    public void delete(long id, User actor) throws SQLException {
        try {
            Db.inTransaction(conn -> {
                Supplier existing = supplierDao.findByIdForUpdate(conn, id);
                if (existing == null) {
                    throw new NotFoundException();
                }
                auditDao.log(conn, actor.getId(), "SUPPLIER_DELETE", "SUPPLIER", id, existing.getCode(), describe(existing), null);
                supplierDao.delete(conn, id);
                return null;
            });
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new ServiceException("Nhà cung cấp đã có phiếu nhập hoặc dữ liệu liên quan nên không xoá được. "
                    + "Hãy chuyển sang \"Ngừng giao dịch\".");
        }
    }

    private void checkTaxCode(Connection conn, String taxCode, long excludeId) throws SQLException {
        if (taxCode != null && supplierDao.existsTaxCode(conn, taxCode, excludeId)) {
            throw new ServiceException("Mã số thuế " + taxCode + " đã được dùng cho nhà cung cấp khác.");
        }
    }

    private static Supplier validated(Supplier in) {
        Supplier s = new Supplier();
        s.setName(required(in.getName(), "tên nhà cung cấp", 255));
        s.setTaxCode(optional(in.getTaxCode(), 14));
        if (s.getTaxCode() != null && !TAX_CODE.matcher(s.getTaxCode()).matches()) {
            throw new ServiceException("Mã số thuế gồm 10 chữ số, chi nhánh thêm \"-\" và 3 số (ví dụ 0101234567-001).");
        }
        s.setContactName(optional(in.getContactName(), 150));
        s.setContactPhone(PhoneNumbers.normalize(in.getContactPhone()));
        s.setEmail(optional(in.getEmail(), 150));
        if (s.getEmail() != null && !EMAIL.matcher(s.getEmail()).matches()) {
            throw new ServiceException("Email không hợp lệ.");
        }
        s.setAddress(optional(in.getAddress(), 255));
        s.setPaymentTerms(optional(in.getPaymentTerms(), 255));
        s.setStatus(in.getStatus() == null ? Supplier.Status.ACTIVE : in.getStatus());
        return s;
    }

    static String normalizeCode(String raw) {
        String code = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if (!CODE.matcher(code).matches()) {
            throw new ServiceException("Mã nhà cung cấp gồm 2–20 ký tự: chữ không dấu, số và . _ - (tự chuyển thành chữ hoa).");
        }
        return code;
    }

    private static String required(String raw, String label, int max) {
        String v = raw == null ? "" : raw.trim();
        if (v.isEmpty()) {
            throw new ServiceException("Vui lòng nhập " + label + ".");
        }
        if (v.length() > max) {
            throw new ServiceException("Độ dài " + label + " tối đa " + max + " ký tự.");
        }
        return v;
    }

    private static String optional(String raw, int max) {
        String v = raw == null ? "" : raw.trim();
        if (v.length() > max) {
            throw new ServiceException("Có trường vượt quá " + max + " ký tự.");
        }
        return v.isEmpty() ? null : v;
    }

    private static String describe(Supplier s) {
        return "Tên: " + s.getName() + "\nMST: " + dash(s.getTaxCode()) + "\nLiên hệ: " + dash(s.getContactName())
                + " – " + dash(s.getContactPhone()) + "\nĐiều khoản thanh toán: " + dash(s.getPaymentTerms())
                + "\nTrạng thái: " + s.getStatus().getLabel();
    }

    private static String dash(String v) {
        return v == null ? "—" : v;
    }

    public static class NotFoundException extends ServiceException {
        public NotFoundException() {
            super("Không tìm thấy nhà cung cấp.");
        }
    }
}
