package com.oms.web.servlet;

import com.oms.model.Role;
import com.oms.service.CustomerDebtLedgerService;
import com.oms.service.CustomerDebtLedgerService.CustomerOption;
import com.oms.service.CustomerDebtLedgerService.Entry;
import com.oms.service.CustomerDebtLedgerService.Report;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;

/** API dữ liệu thật và xuất file cho sổ chi tiết công nợ (S8-10 / GHTK-85). */
@WebServlet(urlPatterns = {"/api/finance/debt-ledger", "/api/finance/debt-ledger/export"})
public class CustomerDebtLedgerServlet extends BaseServlet {
    private final CustomerDebtLedgerService service = new CustomerDebtLedgerService();

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (currentUser(req) == null) { resp.sendError(HttpServletResponse.SC_UNAUTHORIZED); return; }
        if (!currentUser(req).hasRole(Role.ACCOUNTANT) && !currentUser(req).hasRole(Role.ADMIN)) { resp.sendError(HttpServletResponse.SC_FORBIDDEN); return; }
        resp.setHeader("Cache-Control", "no-store");
        try {
            if ("/api/finance/debt-ledger/export".equals(req.getServletPath())) {
                Report report = loadReport(req);
                String format = param(req, "format").toLowerCase(java.util.Locale.ROOT);
                byte[] data; String type; String ext;
                if ("pdf".equals(format)) { data=service.exportPdf(report); type="application/pdf"; ext="pdf"; }
                else if ("xlsx".equals(format)) { data=service.exportXlsx(report); type="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"; ext="xlsx"; }
                else { resp.sendError(HttpServletResponse.SC_BAD_REQUEST,"Định dạng xuất không hợp lệ."); return; }
                resp.setContentType(type); resp.setHeader("Content-Disposition", "attachment; filename=\"so-cong-no-" + report.customerCode() + "-" + report.from() + "-" + report.to() + "." + ext + "\"");
                resp.setContentLength(data.length); resp.getOutputStream().write(data); return;
            }
            if ("true".equalsIgnoreCase(param(req,"customers"))) { writeCustomers(resp); return; }
            writeReport(resp, loadReport(req));
        } catch (IllegalArgumentException e) { resp.sendError(HttpServletResponse.SC_BAD_REQUEST,e.getMessage()); }
        catch (SQLException e) { log("Không tải được sổ chi tiết công nợ",e); resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,"Không tải được sổ công nợ."); }
    }

    private Report loadReport(HttpServletRequest req) throws SQLException {
        long customerId; try { customerId=Long.parseLong(param(req,"customerId")); } catch(NumberFormatException e) { throw new IllegalArgumentException("Mã đại lý không hợp lệ."); }
        LocalDate from, to; try { from=LocalDate.parse(param(req,"from")); to=LocalDate.parse(param(req,"to")); }
        catch(Exception e) { throw new IllegalArgumentException("Vui lòng chọn ngày bắt đầu và ngày kết thúc hợp lệ."); }
        return service.report(customerId,from,to);
    }

    private void writeCustomers(HttpServletResponse resp) throws SQLException, IOException {
        resp.setContentType("application/json;charset=UTF-8"); StringBuilder out=new StringBuilder("{\"customers\":[");
        boolean first=true; for(CustomerOption c:service.customers()) { if(!first)out.append(',');first=false;out.append("{\"id\":").append(c.id()).append(",\"code\":\"").append(json(c.code())).append("\",\"name\":\"").append(json(c.name())).append("\"}"); }
        resp.getWriter().write(out.append("]}").toString());
    }

    private void writeReport(HttpServletResponse resp, Report r) throws IOException {
        resp.setContentType("application/json;charset=UTF-8"); StringBuilder out=new StringBuilder(2048);
        out.append("{\"customer\":{\"id\":").append(r.customerId()).append(",\"code\":\"").append(json(r.customerCode())).append("\",\"name\":\"").append(json(r.customerName())).append("\"},\"from\":\"").append(r.from()).append("\",\"to\":\"").append(r.to()).append("\",\"opening\":").append(r.opening()).append(",\"increase\":").append(r.increase()).append(",\"decrease\":").append(r.decrease()).append(",\"closing\":").append(r.closing()).append(",\"entries\":[");
        for(int i=0;i<r.entries().size();i++){ if(i>0)out.append(','); Entry e=r.entries().get(i); out.append("{\"date\":\"").append(e.date()).append("\",\"reference\":\"").append(json(e.reference())).append("\",\"type\":\"").append(json(e.type())).append("\",\"note\":\"").append(json(e.note())).append("\",\"increase\":").append(e.increase()).append(",\"decrease\":").append(e.decrease()).append(",\"balance\":").append(e.balance()).append('}'); }
        resp.getWriter().write(out.append("]}").toString());
    }
    private static String json(String value) { if(value==null)return ""; return value.replace("\\","\\\\").replace("\"","\\\"").replace("\r","\\r").replace("\n","\\n").replace("\t","\\t"); }
}
