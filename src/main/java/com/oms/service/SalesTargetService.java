package com.oms.service;

import com.oms.config.Db;
import com.oms.dao.AuditLogDao;
import com.oms.dao.LookupDao;
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
import java.util.ArrayList;
import java.util.List;

public class SalesTargetService {

    private final SalesTargetDao dao = new SalesTargetDao();
    private final UserDao userDao = new UserDao();
    private final AuditLogDao audit = new AuditLogDao();

    public record Progress(
            SalesTarget target,
            BigDecimal revenue,
            BigDecimal percentage) {}

    /** Một dòng trên trang chỉ tiêu (getter kiểu bean để JSP đọc được). */
    public record Row(Progress progress, String employeeName, String areaName) {
        public SalesTarget getTarget() { return progress.target(); }
        public BigDecimal getRevenue() { return progress.revenue(); }
        public BigDecimal getPercentage() { return progress.percentage(); }
        public String getEmployeeName() { return employeeName; }
        public String getAreaName() { return areaName; }
        /** Nấc 10% cho thanh tiến độ (class meter__fill--N), vượt 100% vẫn vẽ đầy. */
        public int getStep() { return Math.min(100, progress.percentage().intValue() / 10 * 10); }
        /** Tháng đã qua thì sửa phải ghi lý do. */
        public boolean isLocked() { return progress.target().getTargetMonth().isBefore(LocalDate.now().withDayOfMonth(1)); }
    }

    public record Board(LocalDate month, List<Row> rows, List<NamedItem> salesReps, List<NamedItem> areas) {
        public LocalDate getMonth() { return month; }
        public String getMonthValue() { return month.toString().substring(0, 7); }
        public List<Row> getRows() { return rows; }
        public List<NamedItem> getSalesReps() { return salesReps; }
        public List<NamedItem> getAreas() { return areas; }
    }

    /** Trang của Quản lý kinh doanh: chỉ tiêu mọi nhân viên trong tháng kèm tiến độ. */
    public Board board(LocalDate month, User actor) throws SQLException {
        List<SalesTarget> targets = listByMonth(month, actor);
        try (Connection conn = Db.getConnection()) {
            LookupDao lookup = new LookupDao();
            List<NamedItem> reps = lookup.salesReps(conn);
            List<NamedItem> areas = lookup.areas(conn);
            return new Board(month, rows(conn, targets, reps, areas), reps, areas);
        }
    }

    /** Trang của nhân viên kinh doanh: chỉ tiêu của chính mình. */
    public List<Row> mine(User actor) throws SQLException {
        List<SalesTarget> targets = myTargets(actor);
        try (Connection conn = Db.getConnection()) {
            LookupDao lookup = new LookupDao();
            return rows(conn, targets, lookup.salesReps(conn), lookup.areas(conn));
        }
    }

    private List<Row> rows(Connection conn, List<SalesTarget> targets, List<NamedItem> reps, List<NamedItem> areas)
            throws SQLException {
        List<Row> rows = new ArrayList<>();
        for (SalesTarget target : targets) {
            rows.add(new Row(progress(conn, target), name(reps, target.getEmployeeId()), name(areas, target.getAreaId())));
        }
        return rows;
    }

    private static String name(List<NamedItem> items, long id) {
        return items.stream().filter(i -> i.getId() == id).map(NamedItem::getName).findFirst().orElse("#" + id);
    }

    private void requireManager(User actor) {
        if (actor == null || actor.getRoles() == null
                || actor.getStatus() != User.Status.ACTIVE
                || !(actor.hasRole(Role.ADMIN)
                || actor.hasRole(Role.SALES_MANAGER))) {
            throw new ForbiddenException("Chỉ Quản lý kinh doanh được quản lý chỉ tiêu.");
        }
    }

    private void validateEmployee(
            Connection conn, long employeeId, long areaId)
            throws SQLException {

        User employee = userDao.findById(conn, employeeId);

        if (employee == null
                || employee.getStatus() != User.Status.ACTIVE
                || !employee.hasRole(Role.SALES_REP)) {
            throw new ServiceException("Nhân viên kinh doanh không hợp lệ.");
        }

        boolean assigned = employee.getAreas().stream()
                .map(NamedItem::getId)
                .anyMatch(id -> id == areaId);

        if (!assigned) {
            throw new ServiceException(
                    "Nhân viên chưa được phân công khu vực này.");
        }
    }

    private LocalDate validateMonth(LocalDate month) {
        if (month == null || month.getDayOfMonth() != 1) {
            throw new ServiceException(
                    "Tháng chỉ tiêu không hợp lệ.");
        }
        return month;
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() < 0
                || amount.scale() > 2
                || amount.precision() - amount.scale() > 16) {
            throw new ServiceException("Chỉ tiêu phải là số tiền từ 0, tối đa 2 chữ số thập phân.");
        }
    }

    public long create(SalesTarget target, User actor)
            throws SQLException {
        requireManager(actor);
        if (target == null) {
            throw new ServiceException("Thiếu dữ liệu chỉ tiêu.");
        }

        LocalDate month = validateMonth(target.getTargetMonth());
        validateAmount(target.getTargetAmount());

        if (month.isBefore(LocalDate.now().withDayOfMonth(1))) {
            throw new ServiceException("Tháng đã qua, không tạo chỉ tiêu mới được.");
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
                        "Tạo chỉ tiêu tháng " + month,
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
                    throw new ServiceException("Không tìm thấy chỉ tiêu.");
                }

                boolean expired = old.getTargetMonth().isBefore(
                        LocalDate.now().withDayOfMonth(1));

                if (expired && (reason == null
                        || reason.isBlank())) {
                    throw new ServiceException(
                            "Tháng đã khoá sổ, phải nhập lý do khi sửa chỉ tiêu.");
                }

                if (!dao.updateAmount(conn, id, amount)) {
                    throw new ServiceException("Cập nhật chỉ tiêu thất bại.");
                }

                audit.log(conn, actor.getId(), "UPDATE",
                        "SALES_TARGET", id,
                        reason == null ? "Cập nhật chỉ tiêu" : reason.trim(),
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
            throw new ForbiddenException("Bạn không có quyền xem chỉ tiêu.");
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
            throw new ForbiddenException("Tài khoản không hợp lệ.");
        }

        boolean manager = actor.hasRole(Role.ADMIN)
                || actor.hasRole(Role.SALES_MANAGER);

        if (!manager && !actor.hasRole(Role.SALES_REP)) {
            throw new ForbiddenException("Bạn không có quyền xem chỉ tiêu.");
        }

        try (Connection conn = Db.getConnection()) {
            SalesTarget target = dao.findById(conn, targetId);

            if (target == null) {
                throw new ServiceException("Không tìm thấy chỉ tiêu.");
            }

            if (!manager && target.getEmployeeId() != actor.getId()) {
                throw new ForbiddenException(
                        "Chỉ được xem tiến độ của mình.");
            }

            return progress(conn, target);
        }
    }

    /** Doanh số = hoá đơn đã phát hành (không tính huỷ) của đại lý thuộc nhân viên và khu vực, tính tới hôm nay. */
    private Progress progress(Connection conn, SalesTarget target) throws SQLException {
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