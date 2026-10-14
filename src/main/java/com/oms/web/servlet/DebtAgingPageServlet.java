package com.oms.web.servlet;

import com.oms.service.DebtAgingService;
import com.oms.service.ForbiddenException;
import com.oms.service.ServiceException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** GHTK-69: sổ công nợ theo đại lý, phân tích tuổi nợ và xuất Excel theo bộ lọc đang áp dụng. */
@WebServlet({"/finance/debt-aging", "/finance/debt-aging/export"})
public class DebtAgingPageServlet extends BaseServlet {

    private final DebtAgingService agingService = new DebtAgingService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        DebtAgingService.Filter filter;
        try {
            filter = filter(req);
        } catch (ServiceException e) {
            flash(req, e.getMessage());
            redirect(req, resp, "/finance/debt-aging");
            return;
        }
        try {
            if ("/finance/debt-aging/export".equals(req.getServletPath())) {
                byte[] file = agingService.exportXlsx(filter, currentUser(req));
                LocalDate asOf = filter.asOf() == null ? LocalDate.now() : filter.asOf();
                resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                resp.setHeader("Content-Disposition", "attachment; filename=\"so-cong-no-" + asOf + ".xlsx\"");
                resp.setContentLength(file.length);
                resp.getOutputStream().write(file);
                return;
            }
            req.setAttribute("aging", agingService.report(filter, currentUser(req)));
            req.setAttribute("today", LocalDate.now().toString());
            render(req, resp, "finance/debt-aging");
        } catch (ForbiddenException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
        } catch (ServiceException e) {
            flash(req, e.getMessage());
            redirect(req, resp, "/finance/debt-aging");
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private static DebtAgingService.Filter filter(HttpServletRequest req) {
        LocalDate asOf = null;
        if (!param(req, "asOf").isEmpty()) {
            try {
                asOf = LocalDate.parse(param(req, "asOf"));
            } catch (DateTimeParseException e) {
                throw new ServiceException("Ngày đối chiếu không hợp lệ.");
            }
        }
        Integer bucket = null;
        String value = param(req, "bucket");
        if (value.matches("[0-3]")) {
            bucket = Integer.parseInt(value);
        }
        return new DebtAgingService.Filter(asOf, param(req, "q"), bucket, "1".equals(param(req, "overdue")));
    }
}
