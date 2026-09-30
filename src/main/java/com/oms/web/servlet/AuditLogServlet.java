package com.oms.web.servlet;

import com.oms.dao.AuditLogDao;
import com.oms.model.AuditEntry;
import com.oms.service.AuditService;
import com.oms.service.ServiceException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Xem nhật ký thao tác, lọc theo người thực hiện, loại đối tượng, khoảng thời gian (S2-04). */
@WebServlet("/admin/audit-logs")
public class AuditLogServlet extends BaseServlet {

    private final AuditService auditService = new AuditService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        long actorId = longParam(req, "actor");
        String type = param(req, "type");
        String fromText = param(req, "from");
        String toText = param(req, "to");
        try {
            AuditLogDao.Filter filter = new AuditLogDao.Filter(
                    actorId > 0 ? actorId : null,
                    AuditEntry.TARGET_LABELS.containsKey(type) ? type : null,
                    parseDate(fromText), parseDate(toText));
            req.setAttribute("result", auditService.search(filter, intParam(req, "page", 1)));
        } catch (ServiceException e) {
            req.setAttribute("error", e.getMessage());
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        try {
            req.setAttribute("actors", auditService.actors());
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        req.setAttribute("targetTypes", AuditEntry.TARGET_LABELS);
        req.setAttribute("selectedActor", actorId > 0 ? actorId : 0);
        req.setAttribute("selectedType", type);
        req.setAttribute("from", fromText);
        req.setAttribute("to", toText);
        render(req, resp, "admin/audit-logs");
    }

    private static LocalDate parseDate(String text) {
        if (text.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new ServiceException("Ngày không hợp lệ: " + text);
        }
    }
}
