package com.oms.service;

import com.oms.config.Db;
import com.oms.dao.AuditLogDao;
import com.oms.dao.LookupDao;
import com.oms.model.AuditEntry;
import com.oms.model.NamedItem;
import com.oms.model.PageResult;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/** Tra cứu nhật ký thao tác (S2-04). */
public class AuditService {

    public static final int PAGE_SIZE = 20;

    private final AuditLogDao auditDao = new AuditLogDao();
    private final LookupDao lookupDao = new LookupDao();

    public PageResult<AuditEntry> search(AuditLogDao.Filter filter, int page) throws SQLException {
        if (filter.from() != null && filter.to() != null && filter.from().isAfter(filter.to())) {
            throw new ServiceException("\"Từ ngày\" phải trước hoặc bằng \"Đến ngày\".");
        }
        try (Connection conn = Db.getConnection()) {
            return auditDao.search(conn, filter, Math.max(1, page), PAGE_SIZE);
        }
    }

    /** Danh sách người dùng cho bộ lọc "Người thực hiện". */
    public List<NamedItem> actors() throws SQLException {
        try (Connection conn = Db.getConnection()) {
            return lookupDao.users(conn);
        }
    }
}
