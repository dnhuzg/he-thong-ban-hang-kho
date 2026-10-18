package com.oms.web.servlet;

import com.oms.model.Role;
import com.oms.model.SalesTarget;
import com.oms.model.User;
import com.oms.service.ForbiddenException;
import com.oms.service.SalesTargetService;
import com.oms.service.ServiceException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

/** GHTK-79 / S8-04: QLKD đặt chỉ tiêu doanh số theo tháng; NVKD theo dõi tiến độ của mình. */
@WebServlet({
    "/sales/targets",
    "/sales/targets/create",
    "/sales/targets/update",
    "/sales/targets/my"
})
public class SalesTargetServlet extends BaseServlet {

    private final SalesTargetService service = new SalesTargetService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User actor = currentUser(req);
        try {
            if ("/sales/targets/my".equals(req.getServletPath())) {
                req.setAttribute("rows", service.mine(actor));
                render(req, resp, "sales/my-targets");
                return;
            }
            if (!actor.isAdmin() && !actor.hasRole(Role.SALES_MANAGER)) {
                redirect(req, resp, "/sales/targets/my");
                return;
            }
            req.setAttribute("board", service.board(month(param(req, "month")), actor));
            render(req, resp, "sales/targets");
        } catch (ForbiddenException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
        } catch (ServiceException e) {
            flash(req, e.getMessage());
            redirect(req, resp, "/sales/targets");
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User actor = currentUser(req);
        String month = param(req, "month");
        try {
            if ("/sales/targets/create".equals(req.getServletPath())) {
                SalesTarget target = new SalesTarget();
                target.setEmployeeId(longParam(req, "employeeId"));
                target.setAreaId(longParam(req, "areaId"));
                target.setTargetMonth(month(month));
                target.setTargetAmount(amount(param(req, "amount")));
                service.create(target, actor);
                flash(req, "Đã đặt chỉ tiêu.");
            } else {
                service.update(longParam(req, "id"), amount(param(req, "amount")), param(req, "reason"), actor);
                flash(req, "Đã cập nhật chỉ tiêu.");
            }
        } catch (ForbiddenException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        } catch (ServiceException e) {
            flash(req, e.getMessage());
        } catch (SQLIntegrityConstraintViolationException e) {
            flash(req, "Nhân viên đã có chỉ tiêu cho khu vực này trong tháng.");
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        redirect(req, resp, "/sales/targets" + (month.matches("\\d{4}-\\d{2}") ? "?month=" + month : ""));
    }

    /** Ô chọn tháng gửi yyyy-MM; để trống là tháng hiện tại. */
    private static LocalDate month(String value) {
        if (value.isEmpty()) {
            return LocalDate.now().withDayOfMonth(1);
        }
        try {
            return YearMonth.parse(value).atDay(1);
        } catch (DateTimeParseException e) {
            throw new ServiceException("Tháng không hợp lệ.");
        }
    }

    private static BigDecimal amount(String value) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            throw new ServiceException("Chỉ tiêu phải là số tiền.");
        }
    }
}
