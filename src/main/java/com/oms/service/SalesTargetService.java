package com.oms.service;

import com.oms.config.Db;
import com.oms.dao.AuditLogDao;
import com.oms.dao.SalesReportDao;
import com.oms.dao.SalesTargetDao;
import com.oms.dao.UserDao;
import com.oms.model.NamedItem;
import com.oms.model.Role;
import com.oms.model.SalesTarget;
import com.oms.model.User;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class SalesTargetService {

    private final SalesTargetDao dao = new SalesTargetDao();
    private final UserDao userDao = new UserDao();
    private final AuditLogDao audit = new AuditLogDao();

    public record Progress(
            SalesTarget target,
            BigDecimal revenue,
            BigDecimal percentage) {}

    private void requireManager(User actor) {
        if (actor == null || actor.getRoles() == null
                || actor.getStatus() != User.Status.ACTIVE
                || !(actor.hasRole(Role.ADMIN)
                || actor.hasRole(Role.SALES_MANAGER))) {
            throw new ForbiddenException("Khong co quyen quan ly chi tieu.");
        }
    }

    private void validateEmployee(
            Connection conn, long employeeId, long areaId)
            throws SQLException {

        User employee = userDao.findById(conn, employeeId);

        if (employee == null
                || employee.getStatus() != User.Status.ACTIVE
                || !employee.hasRole(Role.SALES_REP)) {
            throw new ServiceException("Nhan vien khong hop le.");
        }

        boolean assigned = employee.getAreas().stream()
                .map(NamedItem::getId)
                .anyMatch(id -> id == areaId);

        if (!assigned) {
            throw new ServiceException(
                    "Nhan vien chua duoc phan cong khu vuc.");
        }
    }

    private LocalDate validateMonth(LocalDate month) {
        if (month == null || month.getDayOfMonth() != 1) {
            throw new ServiceException(
                    "Thang chi tieu phai la ngay dau thang.");
        }
        return month;
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() < 0
                || amount.scale() > 2
                || amount.precision() - amount.scale() > 16) {
            throw new ServiceException("Chi tieu khong hop le.");
        }
    }

    public long create(SalesTarget target, User actor)
            throws SQLException {
        requireManager(actor);
        if (target == null) {
            throw new ServiceException("Thieu du lieu chi tieu.");
        }

        LocalDate month = validateMonth(target.getTargetMonth());
        validateAmount(target.getTargetAmount());

        if (month.isBefore(LocalDate.now().withDayOfMonth(1))) {
            throw new ServiceException("Ky da qua, khong the tao moi.");
        }

        try (Connection conn = Db.getConnection()) {
            validateEmployee(conn, target.getEmployeeId(),
                    target.getAreaId());

            conn.setAutoCommit(false);
            try {
                target.setCreatedBy(actor.getId());
                long id = dao.create(conn, target);

                audit.log(conn, actor.getId(), "CREATE",
                        "SALES_TARGET", id,
                        "Tao chi tieu thang " + month,
                        null, target.getTargetAmount().toPlainString());

                conn.commit();
                return id;
            } catch (SQLException | RuntimeException ex) {
                conn.rollback();
                throw ex;
            }
        }
    }

    public void update(long id, BigDecimal amount,
                       String reason, User actor)
            throws SQLException {
        requireManager(actor);
        validateAmount(amount);

        try (Connection conn = Db.getConnection()) {
            conn.setAutoCommit(false);
            try {
                SalesTarget old = dao.findByIdForUpdate(conn, id);
                if (old == null) {
                    throw new ServiceException("Khong tim thay chi tieu.");
                }

                boolean expired = old.getTargetMonth().isBefore(
                        LocalDate.now().withDayOfMonth(1));

                if (expired && (reason == null
                        || reason.isBlank())) {
                    throw new ServiceException(
                            "Ky da khoa, phai nhap ly do sua.");
                }

                if (!dao.updateAmount(conn, id, amount)) {
                    throw new ServiceException("Cap nhat that bai.");
                }

                audit.log(conn, actor.getId(), "UPDATE",
                        "SALES_TARGET", id,
                        reason == null ? "Cap nhat chi tieu" : reason.trim(),
                        old.getTargetAmount().toPlainString(),
                        amount.toPlainString());

                conn.commit();
            } catch (SQLException | RuntimeException ex) {
                conn.rollback();
                throw ex;
            }
        }
    }

    public List<SalesTarget> listByMonth(
            LocalDate month, User actor) throws SQLException {
        requireManager(actor);
        validateMonth(month);
        try (Connection conn = Db.getConnection()) {
            return dao.findByMonth(conn, month);
        }
    }

    public List<SalesTarget> myTargets(User actor)
            throws SQLException {
        if (actor == null || actor.getRoles() == null
                || actor.getStatus() != User.Status.ACTIVE
                || !(actor.hasRole(Role.SALES_REP)
                || actor.hasRole(Role.SALES_MANAGER)
                || actor.hasRole(Role.ADMIN))) {
            throw new ForbiddenException("Khong du quyen xem chi tieu.");
        }

        try (Connection conn = Db.getConnection()) {
            return dao.findByEmployee(conn, actor.getId());
        }
    }

    public Progress progress(long targetId, User actor)
            throws SQLException {
        if (actor == null
                || actor.getStatus() != User.Status.ACTIVE
                || actor.getRoles() == null) {
            throw new ForbiddenException("Tai khoan khong hop le.");
        }

        boolean manager = actor.hasRole(Role.ADMIN)
                || actor.hasRole(Role.SALES_MANAGER);

        if (!manager && !actor.hasRole(Role.SALES_REP)) {
            throw new ForbiddenException("Khong du quyen.");
        }

        try (Connection conn = Db.getConnection()) {
            SalesTarget target = dao.findById(conn, targetId);

            if (target == null) {
                throw new ServiceException("Khong tim thay chi tieu.");
            }

            if (!manager && target.getEmployeeId() != actor.getId()) {
                throw new ForbiddenException(
                        "Chi duoc xem tien do cua minh.");
            }

            LocalDate from = target.getTargetMonth();
            LocalDate to = from.plusMonths(1).minusDays(1);
            LocalDate today = LocalDate.now();

            if (to.isAfter(today)) {
                to = today;
            }

            BigDecimal revenue = BigDecimal.ZERO;

            if (!to.isBefore(from)) {
                revenue = dao.revenueByEmployeeArea(
                        conn,
                        target.getEmployeeId(),
                        target.getAreaId(),
                        from,
                        to);
            }

            BigDecimal percentage =
                    target.getTargetAmount().signum() == 0
                    ? BigDecimal.ZERO
                    : revenue.multiply(BigDecimal.valueOf(100))
                    .divide(target.getTargetAmount(), 2,
                            RoundingMode.HALF_UP);

            return new Progress(target, revenue, percentage);
        }
    }
}