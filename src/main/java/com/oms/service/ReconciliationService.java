package com.oms.service;

import com.oms.config.Db;
import com.oms.dao.AuditLogDao;
import com.oms.dao.FinanceOverviewDao;
import com.oms.dao.ReconciliationDao;
import com.oms.model.Role;
import com.oms.model.User;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class ReconciliationService {

    public record PageData(FinanceOverviewDao.LedgerData ledger,
                           List<ReconciliationDao.Document> documents,
                           Long portalCustomerId) { }

    private final ReconciliationDao dao = new ReconciliationDao();
    private final FinanceOverviewDao finance = new FinanceOverviewDao();
    private final AuditLogDao audit = new AuditLogDao();

    public PageData page(User viewer, boolean portal) throws SQLException {
        try (Connection conn = Db.getConnection()) {
            if (portal && !viewer.hasRole(Role.CUSTOMER)) {
                throw new ForbiddenException("Chỉ tài khoản đại lý được xem biên bản trên cổng đại lý.");
            }
            if (!portal && !(viewer.hasRole(Role.ACCOUNTANT) || viewer.isAdmin())) {
                throw new ForbiddenException("Chỉ kế toán hoặc quản trị viên được xem danh sách biên bản.");
            }
            Long customerId = portal ? dao.portalCustomer(conn, viewer.getId()) : null;
            if (portal && customerId == null) {
                throw new ForbiddenException("Tài khoản chưa được gắn với đại lý.");
            }

            FinanceOverviewDao.LedgerData liveLedger = finance.ledger(conn);
            List<ReconciliationDao.Document> documents = dao.list(conn, customerId);
            List<FinanceOverviewDao.CustomerOption> customers = portal
                    ? liveLedger.customers().stream().filter(c -> c.id() == customerId).toList()
                    : liveLedger.customers();
            List<FinanceOverviewDao.LedgerEntry> entries = new ArrayList<>(portal
                    ? liveLedger.entries().stream().filter(e -> e.customerId() == customerId).toList()
                    : liveLedger.entries());

            // Biên bản mới dùng dòng chụp cố định. Các biên bản cũ (chưa có snapshot) vẫn dùng sổ hiện hành.
            for (ReconciliationDao.Document document : documents) {
                if (!document.detailsSnapshotted()) continue;
                entries.removeIf(e -> e.customerId() == document.customerId()
                        && !e.date().isBefore(document.start()) && !e.date().isAfter(document.end()));
                entries.addAll(dao.lines(conn, document.id(), document.customerId()));
            }
            entries.sort(Comparator.comparingLong(FinanceOverviewDao.LedgerEntry::customerId)
                    .thenComparing(FinanceOverviewDao.LedgerEntry::date)
                    .thenComparing(FinanceOverviewDao.LedgerEntry::reference));
            return new PageData(new FinanceOverviewDao.LedgerData(customers, List.copyOf(entries)),
                    documents, customerId);
        }
    }

    public long generate(long customerId, LocalDate start, LocalDate end, User actor) throws SQLException {
        if (!(actor.hasRole(Role.ACCOUNTANT) || actor.isAdmin())) {
            throw new ForbiddenException("Chỉ kế toán hoặc quản trị viên được lập biên bản đối chiếu.");
        }
        if (customerId <= 0 || start == null || end == null || start.isAfter(end)) {
            throw new ServiceException("Kỳ đối chiếu không hợp lệ.");
        }
        return Db.inTransaction(conn -> {
            FinanceOverviewDao.LedgerData ledger = finance.ledger(conn);
            if (ledger.customers().stream().noneMatch(c -> c.id() == customerId)) {
                throw new ServiceException("Đại lý không tồn tại.");
            }
            List<FinanceOverviewDao.LedgerEntry> periodLines = ledger.entries().stream()
                    .filter(e -> e.customerId() == customerId
                            && !e.date().isBefore(start) && !e.date().isAfter(end))
                    .sorted(Comparator.comparing(FinanceOverviewDao.LedgerEntry::date)
                            .thenComparing(FinanceOverviewDao.LedgerEntry::reference))
                    .toList();
            BigDecimal opening = ledger.entries().stream()
                    .filter(e -> e.customerId() == customerId && e.date().isBefore(start))
                    .map(e -> e.increase().subtract(e.decrease()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal increase = periodLines.stream().map(FinanceOverviewDao.LedgerEntry::increase)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal decrease = periodLines.stream().map(FinanceOverviewDao.LedgerEntry::decrease)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            String code = String.format("DCN%06d", dao.next(conn));
            long id;
            try {
                id = dao.insert(conn, code, customerId, start, end, opening, increase, decrease, actor.getId());
            } catch (SQLException ex) {
                if (isConstraintViolation(ex)) {
                    throw new ServiceException("Kỳ này đã có biên bản đối chiếu.");
                }
                throw ex;
            }
            dao.insertLines(conn, id, periodLines);
            audit.log(conn, actor.getId(), "RECONCILIATION_CREATE", "RECONCILIATION", id, code);
            return id;
        });
    }

    public void respond(long id, String status, String reason, User actor) throws SQLException {
        if (!actor.hasRole(Role.CUSTOMER)) {
            throw new ForbiddenException("Chỉ tài khoản đại lý được phản hồi biên bản.");
        }
        if (id <= 0 || status == null || !Set.of("ACCEPTED", "DISPUTED").contains(status)) {
            throw new ServiceException("Phản hồi không hợp lệ.");
        }
        String clean = reason == null ? "" : reason.strip();
        if ("DISPUTED".equals(status) && clean.length() < 10) {
            throw new ServiceException("Nêu chênh lệch ít nhất 10 ký tự.");
        }
        Db.inTransaction(conn -> {
            Long customerId = dao.portalCustomer(conn, actor.getId());
            if (customerId == null) throw new ForbiddenException("Tài khoản chưa được gắn với đại lý.");
            if (!dao.respond(conn, id, customerId, status, clean)) {
                throw new ServiceException("Biên bản không tồn tại, không thuộc đại lý này hoặc đã được phản hồi.");
            }
            audit.log(conn, actor.getId(), "RECONCILIATION_" + status, "RECONCILIATION", id, clean);
            return null;
        });
    }

    private static boolean isConstraintViolation(SQLException ex) {
        String state = ex.getSQLState();
        return state != null && state.startsWith("23");
    }
}
