package com.oms.web.servlet;

import com.oms.service.AssignmentService;
import com.oms.service.ForbiddenException;
import com.oms.service.ServiceException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Chuyển giao đại lý hàng loạt khi nhân viên nghỉ (S3-06): chọn người cần chuyển, người nhận và các đại lý. */
@WebServlet({"/customers/transfer", "/customers/transfer/apply"})
public class CustomerTransferServlet extends BaseServlet {

    private final AssignmentService assignmentService = new AssignmentService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getServletPath().equals("/customers/transfer/apply")) {
            redirect(req, resp, "/customers/transfer");
            return;
        }
        long from = longParam(req, "from");
        long area = longParam(req, "area");
        try {
            req.setAttribute("reps", assignmentService.repsWithCustomers(currentUser(req)));
            req.setAttribute("targets", assignmentService.targetReps(currentUser(req)));
            req.setAttribute("areas", assignmentService.areas(currentUser(req)));
            req.setAttribute("recent", assignmentService.recent(currentUser(req)));
            req.setAttribute("selectedFrom", from);
            req.setAttribute("selectedArea", area);
            if (from > 0) {
                req.setAttribute("candidates", assignmentService.customersOf(from, area > 0 ? area : null, currentUser(req)));
            }
            render(req, resp, "customers/transfer");
        } catch (ForbiddenException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        long from = longParam(req, "from");
        long area = longParam(req, "area");
        try {
            int moved = assignmentService.transfer(from, longParam(req, "to"), ids(req), param(req, "reason"), currentUser(req));
            flash(req, "Đã chuyển giao " + moved + " đại lý.");
        } catch (ForbiddenException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        } catch (ServiceException e) {
            flash(req, e.getMessage());
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        redirect(req, resp, "/customers/transfer?from=" + from + (area > 0 ? "&area=" + area : ""));
    }

    private static List<Long> ids(HttpServletRequest req) {
        List<Long> ids = new ArrayList<>();
        String[] values = req.getParameterValues("customerId");
        if (values != null) {
            for (String v : values) {
                try {
                    ids.add(Long.parseLong(v.trim()));
                } catch (NumberFormatException ignored) {
                    // giá trị lạ từ form: bỏ qua, thiếu thì bị báo "chọn ít nhất một đại lý"
                }
            }
        }
        return ids;
    }
}
