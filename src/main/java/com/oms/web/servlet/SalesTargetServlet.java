package com.oms.web.servlet;

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
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@WebServlet({
    "/sales/targets",
    "/sales/targets/create",
    "/sales/targets/update",
    "/sales/targets/my",
    "/sales/targets/progress"
})
public class SalesTargetServlet extends BaseServlet {

    private final SalesTargetService service =
            new SalesTargetService();

    @Override
    protected void doGet(HttpServletRequest req,
                         HttpServletResponse resp)
            throws ServletException, IOException {

        try {
            User actor = currentUser(req);
            String path = req.getServletPath();

            switch (path) {
                case "/sales/targets" -> {
                    LocalDate month = month(param(req, "month"));
                    List<SalesTarget> targets =
                            service.listByMonth(month, actor);
                    sendTargets(resp, targets);
                }

                case "/sales/targets/my" -> {
                    List<SalesTarget> targets =
                            service.myTargets(actor);
                    sendTargets(resp, targets);
                }

                case "/sales/targets/progress" -> {
                    long id = positiveId(req, "id");
                    var result = service.progress(id, actor);

                    sendJson(resp, "{"
                            + "\"targetId\":" + result.target().getId()
                            + ",\"employeeId\":"
                            + result.target().getEmployeeId()
                            + ",\"areaId\":"
                            + result.target().getAreaId()
                            + ",\"month\":\""
                            + result.target().getTargetMonth()
                            + "\",\"targetAmount\":"
                            + result.target().getTargetAmount()
                            + ",\"revenue\":" + result.revenue()
                            + ",\"percentage\":"
                            + result.percentage()
                            + "}");
                }

                default -> resp.sendError(404);
            }

        } catch (ForbiddenException e) {
            resp.sendError(403);
        } catch (ServiceException e) {
            resp.sendError(400, e.getMessage());
        } catch (SQLException e) {
            if (e instanceof java.sql.SQLIntegrityConstraintViolationException
                    || "23000".equals(e.getSQLState())
                    || "23505".equals(e.getSQLState())) {
                resp.sendError(409, "Chi tieu da ton tai hoac du lieu vi pham rang buoc.");
                return;
            }
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req,
                          HttpServletResponse resp)
            throws ServletException, IOException {

        try {
            User actor = currentUser(req);
            String path = req.getServletPath();

            switch (path) {
                case "/sales/targets/create" -> {
                    SalesTarget target = new SalesTarget();

                    target.setEmployeeId(
                            positiveId(req, "employeeId"));
                    target.setAreaId(
                            positiveId(req, "areaId"));
                    target.setTargetMonth(
                            month(param(req, "month")));
                    target.setTargetAmount(
                            amount(param(req, "amount")));

                    long id = service.create(target, actor);

                    resp.setStatus(201);
                    sendJson(resp, "{\"success\":true,\"id\":"
                            + id + "}");
                }

                case "/sales/targets/update" -> {
                    long id = positiveId(req, "id");
                    BigDecimal value =
                            amount(param(req, "amount"));
                    String reason = param(req, "reason");

                    service.update(id, value, reason, actor);

                    sendJson(resp, "{\"success\":true}");
                }

                default -> resp.sendError(404);
            }

        } catch (ForbiddenException e) {
            resp.sendError(403);
        } catch (ServiceException e) {
            resp.sendError(400, e.getMessage());
        } catch (SQLException e) {
            if (e instanceof java.sql.SQLIntegrityConstraintViolationException
                    || "23000".equals(e.getSQLState())
                    || "23505".equals(e.getSQLState())) {
                resp.sendError(409, "Chi tieu da ton tai hoac du lieu vi pham rang buoc.");
                return;
            }
            throw new ServletException(e);
        }
    }

    private static long positiveId(
            HttpServletRequest req, String name) {
        try {
            long id = Long.parseLong(param(req, name));
            if (id <= 0) {
                throw new NumberFormatException();
            }
            return id;
        } catch (NumberFormatException e) {
            throw new ServiceException(
                    "ID khong hop le: " + name);
        }
    }

    private static LocalDate month(String value) {
        try {
            if (value.matches("\\d{4}-\\d{2}")) {
                value += "-01";
            }

            LocalDate date = LocalDate.parse(value);

            if (date.getDayOfMonth() != 1) {
                throw new ServiceException(
                        "Thang phai bat dau tu ngay 01.");
            }

            return date;
        } catch (DateTimeParseException e) {
            throw new ServiceException(
                    "Thang khong hop le. Dung yyyy-MM.");
        }
    }

    private static BigDecimal amount(String value) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            throw new ServiceException(
                    "So tien khong hop le.");
        }
    }

    private static void sendTargets(
            HttpServletResponse resp,
            List<SalesTarget> targets) throws IOException {

        StringBuilder json = new StringBuilder("[");

        for (SalesTarget t : targets) {
            if (json.length() > 1) {
                json.append(",");
            }

            json.append("{")
                .append("\"id\":").append(t.getId())
                .append(",\"employeeId\":")
                .append(t.getEmployeeId())
                .append(",\"areaId\":")
                .append(t.getAreaId())
                .append(",\"month\":\"")
                .append(t.getTargetMonth())
                .append("\",\"targetAmount\":")
                .append(t.getTargetAmount())
                .append("}");
        }

        json.append("]");
        sendJson(resp, json.toString());
    }

    private static void sendJson(
            HttpServletResponse resp, String json)
            throws IOException {

        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write(json);
    }
}