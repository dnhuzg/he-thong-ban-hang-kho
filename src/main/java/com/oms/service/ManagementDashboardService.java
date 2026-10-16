package com.oms.service;

import com.oms.config.Db;
import com.oms.dao.ManagementDashboardDao;
import com.oms.dao.ManagementDashboardDao.OverdueCustomer;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Dashboard điều hành bán hàng và kho (S8-05): mọi số đọc từ DB trong một kết nối. */
public class ManagementDashboardService {

    private final ManagementDashboardDao dao = new ManagementDashboardDao();

    public record Dashboard(BigDecimal today, BigDecimal month, long pendingApproval, long pendingDispatch,
                            BigDecimal overdueTotal, long outOfStockSkus, Map<LocalDate, BigDecimal> daily,
                            Map<String, BigDecimal> areas, List<OverdueCustomer> overdue) {
        public BigDecimal getToday() { return today; }
        public BigDecimal getMonth() { return month; }
        public long getPendingApproval() { return pendingApproval; }
        public long getPendingDispatch() { return pendingDispatch; }
        public BigDecimal getOverdueTotal() { return overdueTotal; }
        public long getOutOfStockSkus() { return outOfStockSkus; }
        public List<OverdueCustomer> getOverdue() { return overdue; }

        /** Doanh số từng ngày trong tháng, phân tách bởi dấu phẩy để biểu đồ đọc qua data-*. */
        public String getDailyValues() {
            return daily.values().stream().map(BigDecimal::toPlainString).collect(Collectors.joining(","));
        }

        public String getAreaNames() {
            return String.join("|", areas.keySet());
        }

        public String getAreaValues() {
            return areas.values().stream().map(BigDecimal::toPlainString).collect(Collectors.joining(","));
        }
    }

    public Dashboard load(LocalDate today) throws SQLException {
        LocalDate monthStart = today.withDayOfMonth(1);
        try (Connection conn = Db.getConnection()) {
            List<OverdueCustomer> overdue = dao.overdueCustomers(conn, today);
            BigDecimal overdueTotal = overdue.stream().map(OverdueCustomer::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
            return new Dashboard(dao.revenue(conn, today, today), dao.revenue(conn, monthStart, today),
                    dao.countOrders(conn, "PENDING_APPROVAL"), dao.countOrders(conn, "APPROVED", "PICKING"),
                    overdueTotal, dao.outOfStockSkus(conn), dao.dailyRevenue(conn, monthStart, today),
                    dao.revenueByArea(conn, monthStart, today), overdue);
        }
    }
}
