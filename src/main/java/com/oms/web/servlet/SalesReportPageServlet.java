package com.oms.web.servlet;

import com.oms.dao.SalesReportDao;
import com.oms.model.Role;
import com.oms.model.User;
import com.oms.service.SalesReportService;
import com.oms.service.ServiceException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;

/** GHTK-81: báo cáo doanh số nhiều chiều từ hoá đơn đã phát hành, so cùng kỳ trước, xuất Excel. */
@WebServlet({"/reports/sales", "/reports/sales/export"})
public class SalesReportPageServlet extends BaseServlet {

    private final SalesReportService reportService = new SalesReportService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        User user = currentUser(req);
        if (user == null || !canView(user.getRoles())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        try {
            SalesReportService.Filter filter = SalesReportService.filter(param(req, "dimension"),
                    date(param(req, "from")), date(param(req, "to")));
            if ("/reports/sales/export".equals(req.getServletPath())) {
                byte[] file = reportService.exportXlsx(filter, user);
                resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                resp.setHeader("Content-Disposition", "attachment; filename=\"doanh-so-" + filter.from() + "-"
                        + filter.to() + ".xlsx\"");
                resp.setContentLength(file.length);
                resp.getOutputStream().write(file);
                return;
            }
            req.setAttribute("sales", reportService.report(filter, user));
            req.setAttribute("dimensions", SalesReportDao.Dimension.values());
            render(req, resp, "reports/sales-report");
        } catch (ServiceException e) {
            flash(req, e.getMessage());
            redirect(req, resp, "/reports/sales");
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private static LocalDate date(String value) {
        if (value.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new ServiceException("Ngày không hợp lệ.");
        }
    }

    private static boolean canView(Set<Role> roles) {
        return roles != null
                && (roles.contains(Role.SALES_MANAGER)
                    || roles.contains(Role.ADMIN));
    }
}
